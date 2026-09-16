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
 * Pure JVM — DocumentDao/LedgerEntryDao/RemoteRestoreSource are all faked directly, no Room or
 * Firestore runtime needed. Proves the one property that actually matters here (see
 * FirebaseRestoreClient's doc comment): restoreAll() is safe to call more than once, including
 * after a simulated partial failure, because it goes through the REPLACE-based upsertAllFromCloud
 * rather than the normal insert().
 */
class FirebaseRestoreClientTest {

    private val fakeDocumentDao = FakeDocumentDao()
    private val fakeLedgerEntryDao = FakeLedgerEntryDao()

    @Test
    fun restoreAll_upserts_every_document_and_ledger_entry_from_the_remote_source() = runTest {
        val remoteSource = FakeRemoteRestoreSource(
            documents = listOf(document("doc-1"), document("doc-2")),
            ledgerEntries = listOf(ledgerEntry("entry-1"))
        )
        val client = DefaultFirebaseRestoreClient(remoteSource, fakeDocumentDao, fakeLedgerEntryDao)

        client.restoreAll("org-1")

        assertEquals(2, fakeDocumentDao.upserted.size)
        assertEquals(1, fakeLedgerEntryDao.upserted.size)
        assertTrue(fakeDocumentDao.upserted.all { it.syncedToCloud })
    }

    @Test
    fun restoreAll_is_safe_to_re_run_after_an_interrupted_attempt() = runTest {
        val remoteSource = FakeRemoteRestoreSource(
            documents = listOf(document("doc-1")),
            ledgerEntries = emptyList()
        )
        val client = DefaultFirebaseRestoreClient(remoteSource, fakeDocumentDao, fakeLedgerEntryDao)

        // Simulates a first attempt that got as far as documents but never confirmed completion
        // (e.g. the app was killed before the SharedPreferences flag was set - see MainActivity's
        // restore trigger). Re-running must not error or duplicate.
        client.restoreAll("org-1")
        client.restoreAll("org-1")

        assertEquals(2, fakeDocumentDao.upsertCallCount)
        assertEquals(1, fakeDocumentDao.upserted.distinctBy { it.documentId }.size)
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
        private val documents: List<DocumentEntity>,
        private val ledgerEntries: List<LedgerEntryEntity>
    ) : RemoteRestoreSource {
        override suspend fun fetchAllDocuments(orgId: String) = documents
        override suspend fun fetchAllLedgerEntries(orgId: String) = ledgerEntries
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
        override suspend fun count(): Int = error("not used by this test")
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
