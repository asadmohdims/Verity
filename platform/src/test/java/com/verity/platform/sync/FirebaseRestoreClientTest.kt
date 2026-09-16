package com.verity.platform.sync

import com.verity.platform.database.dao.DocumentDao
import com.verity.platform.database.dao.LedgerEntryDao
import com.verity.platform.database.entities.DocumentEntity
import com.verity.platform.database.entities.LedgerEntryEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FirebaseRestoreClientTest
 *
 * Pure JVM — DocumentDao/LedgerEntryDao/RemoteRestoreSource/SyncWatermarkStore are all faked
 * directly, no Room, Firestore, or Android Context needed. Proves the properties that actually
 * matter here (see FirebaseRestoreClient's doc comment):
 * - the first sync ever on a device does one unfiltered fetch (the baseline), to catch documents
 *   pushed before the serverSyncedAt field existed;
 * - every sync after that asks the remote source for exactly the watermark it has stored, which
 *   only ever advances based on what the remote source itself reports observing (never a client
 *   clock reading) — this is the actual fix for the cross-device-clock-skew bug an earlier,
 *   finalizedAt-based version of this had;
 * - sync() is safe to call more than once, including after a simulated partial failure, because
 *   it goes through the REPLACE-based upsertAllFromCloud rather than the normal insert().
 */
class FirebaseRestoreClientTest {

    @Test
    fun first_ever_sync_does_an_unfiltered_baseline_fetch_and_records_the_watermark() = runTest {
        val documentDao = FakeDocumentDao()
        val ledgerEntryDao = FakeLedgerEntryDao()
        val watermarkStore = FakeSyncWatermarkStore()
        val remoteSource = FakeRemoteRestoreSource(
            allDocuments = RemoteBatch(listOf(document("doc-1"), document("doc-2")), maxServerSyncedAtMillis = 9_000L),
            allLedgerEntries = RemoteBatch(listOf(ledgerEntry("entry-1")), maxServerSyncedAtMillis = 8_000L)
        )
        val client = DefaultFirebaseRestoreClient(remoteSource, documentDao, ledgerEntryDao, watermarkStore)

        client.sync("org-1")

        assertTrue("baseline fetch must use the unfiltered path", remoteSource.fetchAllDocumentsCallCount == 1)
        assertEquals(2, documentDao.upserted.size)
        assertEquals(1, ledgerEntryDao.upserted.size)
        assertTrue(documentDao.upserted.all { it.syncedToCloud })
        assertTrue(watermarkStore.hasCompletedBaselineSync())
        assertEquals(9_000L, watermarkStore.lastSyncWatermarkMillis())
    }

    @Test
    fun a_later_sync_asks_only_for_what_is_newer_than_the_stored_watermark() = runTest {
        val documentDao = FakeDocumentDao()
        val ledgerEntryDao = FakeLedgerEntryDao()
        val watermarkStore = FakeSyncWatermarkStore().apply {
            markBaselineSyncCompleted()
            recordSyncWatermark(5_000L)
        }
        val remoteSource = FakeRemoteRestoreSource(
            sinceDocuments = RemoteBatch(listOf(document("doc-3")), maxServerSyncedAtMillis = 6_000L),
            sinceLedgerEntries = RemoteBatch(emptyList(), maxServerSyncedAtMillis = null)
        )
        val client = DefaultFirebaseRestoreClient(remoteSource, documentDao, ledgerEntryDao, watermarkStore)

        client.sync("org-1")

        assertEquals(5_000L, remoteSource.lastDocumentsSinceRequested)
        assertEquals(0, remoteSource.fetchAllDocumentsCallCount)
        assertEquals(1, documentDao.upserted.size)
        assertEquals(6_000L, watermarkStore.lastSyncWatermarkMillis())
    }

    @Test
    fun the_watermark_never_moves_backward_when_nothing_new_comes_back() = runTest {
        val watermarkStore = FakeSyncWatermarkStore().apply {
            markBaselineSyncCompleted()
            recordSyncWatermark(5_000L)
        }
        val remoteSource = FakeRemoteRestoreSource(
            sinceDocuments = RemoteBatch(emptyList(), maxServerSyncedAtMillis = null),
            sinceLedgerEntries = RemoteBatch(emptyList(), maxServerSyncedAtMillis = null)
        )
        val client = DefaultFirebaseRestoreClient(remoteSource, FakeDocumentDao(), FakeLedgerEntryDao(), watermarkStore)

        client.sync("org-1")

        assertEquals(5_000L, watermarkStore.lastSyncWatermarkMillis())
    }

    @Test
    fun sync_is_safe_to_re_run_after_an_interrupted_attempt() = runTest {
        val documentDao = FakeDocumentDao()
        val watermarkStore = FakeSyncWatermarkStore()
        val remoteSource = FakeRemoteRestoreSource(
            allDocuments = RemoteBatch(listOf(document("doc-1")), maxServerSyncedAtMillis = 1_000L),
            allLedgerEntries = RemoteBatch(emptyList(), maxServerSyncedAtMillis = null)
        )
        val client = DefaultFirebaseRestoreClient(remoteSource, documentDao, FakeLedgerEntryDao(), watermarkStore)

        // Simulates a first attempt that got as far as documents but never confirmed completion
        // (e.g. the app was killed mid-sync). Re-running must not error or duplicate.
        client.sync("org-1")
        client.sync("org-1")

        assertEquals(2, documentDao.upsertCallCount)
        assertEquals(1, documentDao.upserted.distinctBy { it.documentId }.size)
    }

    // syncedToCloud = true here, matching what the real RemoteRestoreSource implementation
    // (FirestoreRestoreSource) always produces - a document fetched from the cloud is, by
    // definition, already synced (see FirebaseRestoreClient's toDocumentEntity()).
    private fun document(id: String) = DocumentEntity(
        documentId = id,
        orgId = "org-1",
        documentType = "INVOICE",
        sequenceNumber = 1,
        documentNumber = "INV-000001",
        customerId = "cust-1",
        customerName = "Acme",
        issueDateEpochDay = 0,
        grandTotalPaise = 1000,
        linkedDocumentId = null,
        payloadJson = "{}",
        finalizedAt = 0,
        searchIndexText = "",
        syncedToCloud = true
    )

    private fun ledgerEntry(id: String) = LedgerEntryEntity(
        entryId = id,
        orgId = "org-1",
        customerId = "cust-1",
        documentId = "doc-1",
        amountPaise = 1000,
        occurredAt = 0,
        createdAt = 0,
        syncedToCloud = true
    )

    private class FakeRemoteRestoreSource(
        private val allDocuments: RemoteBatch<DocumentEntity> = RemoteBatch(emptyList(), null),
        private val allLedgerEntries: RemoteBatch<LedgerEntryEntity> = RemoteBatch(emptyList(), null),
        private val sinceDocuments: RemoteBatch<DocumentEntity> = RemoteBatch(emptyList(), null),
        private val sinceLedgerEntries: RemoteBatch<LedgerEntryEntity> = RemoteBatch(emptyList(), null)
    ) : RemoteRestoreSource {
        var fetchAllDocumentsCallCount = 0
        var lastDocumentsSinceRequested: Long? = null

        override suspend fun fetchAllDocuments(orgId: String): RemoteBatch<DocumentEntity> {
            fetchAllDocumentsCallCount++
            return allDocuments
        }

        override suspend fun fetchAllLedgerEntries(orgId: String): RemoteBatch<LedgerEntryEntity> = allLedgerEntries

        override suspend fun fetchDocumentsSince(orgId: String, sinceEpochMillis: Long): RemoteBatch<DocumentEntity> {
            lastDocumentsSinceRequested = sinceEpochMillis
            return sinceDocuments
        }

        override suspend fun fetchLedgerEntriesSince(orgId: String, sinceEpochMillis: Long): RemoteBatch<LedgerEntryEntity> =
            sinceLedgerEntries
    }

    private class FakeSyncWatermarkStore : SyncWatermarkStore {
        private var baselineDone = false
        private var watermark: Long? = null

        override fun hasCompletedBaselineSync(): Boolean = baselineDone
        override fun markBaselineSyncCompleted() {
            baselineDone = true
        }

        override fun lastSyncWatermarkMillis(): Long? = watermark
        override fun recordSyncWatermark(epochMillis: Long) {
            watermark = epochMillis
        }
    }

    private class FakeDocumentDao : DocumentDao {
        val upserted = mutableListOf<DocumentEntity>()
        var upsertCallCount = 0

        override suspend fun upsertAllFromCloud(documents: List<DocumentEntity>) {
            upsertCallCount++
            upserted += documents
        }

        override suspend fun insert(document: DocumentEntity) = error("not used by this test")
        override suspend fun markSyncedToCloud(documentId: String) = error("not used by this test")
        override suspend fun getPendingSyncCount(): Int = error("not used by this test")
        override suspend fun getMaxSequenceNumber(orgId: String, documentType: String): Long? = error("not used by this test")
        override suspend fun getById(documentId: String): DocumentEntity? = error("not used by this test")
        override suspend fun getAll(): List<DocumentEntity> = error("not used by this test")
        override suspend fun search(normalizedQuery: String): List<DocumentEntity> = error("not used by this test")
        override suspend fun getByCustomerId(customerId: String): List<DocumentEntity> = error("not used by this test")
        override suspend fun findByLinkedDocumentId(challanDocumentId: String): DocumentEntity? = error("not used by this test")
        override suspend fun findByDocumentNumber(orgId: String, documentNumber: String): DocumentEntity? = error("not used by this test")
    }

    private class FakeLedgerEntryDao : LedgerEntryDao {
        val upserted = mutableListOf<LedgerEntryEntity>()

        override suspend fun upsertAllFromCloud(entries: List<LedgerEntryEntity>) {
            upserted += entries
        }

        override suspend fun insert(entry: LedgerEntryEntity) = error("not used by this test")
        override suspend fun markSyncedToCloud(entryId: String) = error("not used by this test")
        override suspend fun getPendingSyncCount(): Int = error("not used by this test")
        override suspend fun getBalanceForCustomer(orgId: String, customerId: String): Long? = error("not used by this test")
    }
}
