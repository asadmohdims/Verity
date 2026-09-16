package com.verity.platform.sync

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.verity.platform.database.dao.CustomerDao
import com.verity.platform.database.dao.DocumentDao
import com.verity.platform.database.dao.LedgerEntryDao
import com.verity.platform.database.entities.CustomerEntity
import com.verity.platform.database.entities.DocumentEntity
import com.verity.platform.database.entities.LedgerEntryEntity
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * FirebaseRestoreClient
 *
 * Cross-device catch-up: run once at every app launch (see MainActivity), not a live
 * subscription. Originally this only ever bulk-restored a genuinely empty device (new install /
 * reinstall) — cloud was framed as backup/restore for a single primary device, never a live
 * multi-device sync target. That premise stopped holding once this app was actually put on two
 * devices used for the same business at the same time: a document finalized on device A would
 * never appear on device B until B was wiped and restored, which is not something anyone would
 * ever do to a working device.
 *
 * sync() replaces that one-time bulk restore with an incremental pull, filtered on
 * `serverSyncedAt` — a Firestore-server-assigned write timestamp (FieldValue.serverTimestamp(),
 * see FirebaseSyncClient), never a client clock reading. That distinction matters: an earlier
 * version of this filtered on the document's own `finalizedAt` (each device's local clock at
 * finalize time) and had a real bug — if this device's clock ran even slightly ahead of another
 * device's, or this device simply created something with a later local timestamp, its watermark
 * could already exceed an older cross-device document's finalizedAt, permanently hiding it no
 * matter how many times sync ran again. A server-assigned timestamp is comparable across devices
 * regardless of clock skew, which a client timestamp fundamentally is not.
 *
 * One-time unfiltered baseline (SyncWatermarkStore.hasCompletedBaselineSync): documents pushed
 * before serverSyncedAt existed have no value for that field at all, and Firestore's range
 * filters simply exclude documents missing the filtered field — so a filtered query would never
 * see them. The first sync on any device (including ones with years of existing local data) does
 * one unfiltered fetch of everything to catch those up, then every sync after that filters on the
 * watermark it recorded.
 *
 * Deliberately not a live Firestore listener (addSnapshotListener): the user explicitly didn't
 * want continuous polling/an open connection, just "catch up on launch". Because the incremental
 * query only ever asks for what's new since the last real watermark, it stays cheap even if
 * called more often than once per cold launch (e.g. on resume too) — the cost is proportional to
 * what changed, not to total historical volume.
 */
interface FirebaseRestoreClient {
    suspend fun sync(orgId: String)
}

/** One page of pulled rows plus the highest serverSyncedAt actually observed among them, if any. */
data class RemoteBatch<T>(val items: List<T>, val maxServerSyncedAtMillis: Long?)

/**
 * Read side, kept behind its own interface so sync()'s REPLACE-based safety and watermark
 * arithmetic (the actual things worth unit-testing — see the cloud-sync plan's verification
 * section) can be tested against a fake, without a real Firestore instance.
 */
interface RemoteRestoreSource {
    suspend fun fetchAllDocuments(orgId: String): RemoteBatch<DocumentEntity>
    suspend fun fetchAllLedgerEntries(orgId: String): RemoteBatch<LedgerEntryEntity>
    suspend fun fetchAllCustomers(orgId: String): RemoteBatch<CustomerEntity>
    suspend fun fetchDocumentsSince(orgId: String, sinceEpochMillis: Long): RemoteBatch<DocumentEntity>
    suspend fun fetchLedgerEntriesSince(orgId: String, sinceEpochMillis: Long): RemoteBatch<LedgerEntryEntity>
    suspend fun fetchCustomersSince(orgId: String, sinceEpochMillis: Long): RemoteBatch<CustomerEntity>
}

class DefaultFirebaseRestoreClient(
    private val remoteSource: RemoteRestoreSource,
    private val documentDao: DocumentDao,
    private val ledgerEntryDao: LedgerEntryDao,
    private val customerDao: CustomerDao,
    private val watermarkStore: SyncWatermarkStore,
    private val timeoutMillis: Long = 15_000
) : FirebaseRestoreClient {

    /**
     * Timeout-guarded (added 2026-09-15, same fix as FirebaseSyncClient.downloadPdf and
     * FirebaseAuthGate.ensureSignedIn - found by testing offline on a real device). This runs
     * from MainActivity's startup LaunchedEffect, not the finalize path, so it was never the
     * cause of the offline-finalize hang, but it's the same unguarded-network-await bug class -
     * without this, a sync attempted while offline would hang indefinitely instead of failing
     * fast and letting the next app launch retry it. withTimeout (throws), not withTimeoutOrNull:
     * there's no partial-success value to fall back to here, so a timeout should surface as a
     * normal failure to the caller's existing runCatching, not be silently swallowed into "did
     * nothing."
     */
    override suspend fun sync(orgId: String) = withTimeout(timeoutMillis) {
        if (!watermarkStore.hasCompletedBaselineSync()) {
            val documents = remoteSource.fetchAllDocuments(orgId)
            val ledgerEntries = remoteSource.fetchAllLedgerEntries(orgId)
            val customers = remoteSource.fetchAllCustomers(orgId)
            upsertAndAdvanceWatermark(documents, ledgerEntries, customers, currentWatermark = 0L)
            watermarkStore.markBaselineSyncCompleted()
        } else {
            val since = watermarkStore.lastSyncWatermarkMillis() ?: 0L
            val documents = remoteSource.fetchDocumentsSince(orgId, since)
            val ledgerEntries = remoteSource.fetchLedgerEntriesSince(orgId, since)
            val customers = remoteSource.fetchCustomersSince(orgId, since)
            upsertAndAdvanceWatermark(documents, ledgerEntries, customers, currentWatermark = since)
        }
    }

    private suspend fun upsertAndAdvanceWatermark(
        documents: RemoteBatch<DocumentEntity>,
        ledgerEntries: RemoteBatch<LedgerEntryEntity>,
        customers: RemoteBatch<CustomerEntity>,
        currentWatermark: Long
    ) {
        // REPLACE-based inserts (not the normal ABORT-on-duplicate insert()) — makes this safe
        // to re-run in full if a previous attempt was interrupted partway, rather than needing
        // separate resume/pagination bookkeeping.
        documentDao.upsertAllFromCloud(documents.items)
        ledgerEntryDao.upsertAllFromCloud(ledgerEntries.items)
        customerDao.upsertAllFromCloud(customers.items)

        val newWatermark = listOfNotNull(
            currentWatermark,
            documents.maxServerSyncedAtMillis,
            ledgerEntries.maxServerSyncedAtMillis,
            customers.maxServerSyncedAtMillis
        ).max()
        if (newWatermark > currentWatermark) {
            watermarkStore.recordSyncWatermark(newWatermark)
        }
    }
}

class FirestoreRestoreSource(private val firestore: FirebaseFirestore) : RemoteRestoreSource {

    override suspend fun fetchAllDocuments(orgId: String): RemoteBatch<DocumentEntity> {
        val snapshot = firestore.collection(documentsCollection(orgId)).get().await()
        return snapshot.toDocumentBatch(orgId)
    }

    override suspend fun fetchAllLedgerEntries(orgId: String): RemoteBatch<LedgerEntryEntity> {
        val snapshot = firestore.collection(ledgerEntriesCollection(orgId)).get().await()
        return snapshot.toLedgerEntryBatch(orgId)
    }

    override suspend fun fetchAllCustomers(orgId: String): RemoteBatch<CustomerEntity> {
        val snapshot = firestore.collection(customersCollection(orgId)).get().await()
        return snapshot.toCustomerBatch(orgId)
    }

    override suspend fun fetchDocumentsSince(orgId: String, sinceEpochMillis: Long): RemoteBatch<DocumentEntity> {
        val snapshot = firestore.collection(documentsCollection(orgId))
            // >=, not > : two devices writing in the same millisecond is astronomically unlikely
            // but not impossible, and a strict > would let a same-timestamp document fall
            // permanently below the watermark once *any* document with that exact serverSyncedAt
            // is locally known. The one already-known row this re-fetches every call is a
            // harmless no-op via upsertAllFromCloud's REPLACE.
            .whereGreaterThanOrEqualTo("serverSyncedAt", timestampFromEpochMilli(sinceEpochMillis))
            .get()
            .await()
        return snapshot.toDocumentBatch(orgId)
    }

    override suspend fun fetchLedgerEntriesSince(orgId: String, sinceEpochMillis: Long): RemoteBatch<LedgerEntryEntity> {
        val snapshot = firestore.collection(ledgerEntriesCollection(orgId))
            .whereGreaterThanOrEqualTo("serverSyncedAt", timestampFromEpochMilli(sinceEpochMillis))
            .get()
            .await()
        return snapshot.toLedgerEntryBatch(orgId)
    }

    override suspend fun fetchCustomersSince(orgId: String, sinceEpochMillis: Long): RemoteBatch<CustomerEntity> {
        val snapshot = firestore.collection(customersCollection(orgId))
            .whereGreaterThanOrEqualTo("serverSyncedAt", timestampFromEpochMilli(sinceEpochMillis))
            .get()
            .await()
        return snapshot.toCustomerBatch(orgId)
    }
}

private fun documentsCollection(orgId: String) = "orgs/$orgId/documents"
private fun ledgerEntriesCollection(orgId: String) = "orgs/$orgId/ledgerEntries"
private fun customersCollection(orgId: String) = "orgs/$orgId/customers"

private fun Timestamp.toEpochMilli(): Long = seconds * 1_000 + nanoseconds / 1_000_000
private fun timestampFromEpochMilli(epochMillis: Long): Timestamp =
    Timestamp(epochMillis / 1_000, ((epochMillis % 1_000) * 1_000_000).toInt())

private fun com.google.firebase.firestore.QuerySnapshot.toDocumentBatch(orgId: String): RemoteBatch<DocumentEntity> {
    val items = documents.mapNotNull { it.toDocumentEntity(orgId) }
    val maxTimestamp = documents.mapNotNull { it.getTimestamp("serverSyncedAt")?.toEpochMilli() }.maxOrNull()
    return RemoteBatch(items, maxTimestamp)
}

private fun com.google.firebase.firestore.QuerySnapshot.toLedgerEntryBatch(orgId: String): RemoteBatch<LedgerEntryEntity> {
    val items = documents.mapNotNull { it.toLedgerEntryEntity(orgId) }
    val maxTimestamp = documents.mapNotNull { it.getTimestamp("serverSyncedAt")?.toEpochMilli() }.maxOrNull()
    return RemoteBatch(items, maxTimestamp)
}

private fun com.google.firebase.firestore.QuerySnapshot.toCustomerBatch(orgId: String): RemoteBatch<CustomerEntity> {
    val items = documents.mapNotNull { it.toCustomerEntity(orgId) }
    val maxTimestamp = documents.mapNotNull { it.getTimestamp("serverSyncedAt")?.toEpochMilli() }.maxOrNull()
    return RemoteBatch(items, maxTimestamp)
}

private fun com.google.firebase.firestore.DocumentSnapshot.toDocumentEntity(orgId: String): DocumentEntity? {
    val documentId = getString("documentId") ?: return null
    return DocumentEntity(
        documentId = documentId,
        orgId = orgId,
        documentType = getString("documentType") ?: return null,
        sequenceNumber = getLong("sequenceNumber") ?: return null,
        documentNumber = getString("documentNumber") ?: return null,
        customerId = getString("customerId") ?: return null,
        customerName = getString("customerName") ?: "",
        issueDateEpochDay = getLong("issueDateEpochDay") ?: return null,
        grandTotalPaise = getLong("grandTotalPaise") ?: return null,
        linkedDocumentId = getString("linkedDocumentId"),
        payloadJson = getString("payloadJson") ?: return null,
        finalizedAt = getLong("finalizedAt") ?: return null,
        searchIndexText = getString("searchIndexText") ?: "",
        selfNotes = getString("selfNotes"),
        // Restored rows are, by definition, already in the cloud - never re-pushed.
        syncedToCloud = true
    )
}

private fun com.google.firebase.firestore.DocumentSnapshot.toLedgerEntryEntity(orgId: String): LedgerEntryEntity? {
    val entryId = getString("entryId") ?: return null
    return LedgerEntryEntity(
        entryId = entryId,
        orgId = orgId,
        customerId = getString("customerId") ?: return null,
        documentId = getString("documentId") ?: return null,
        amountPaise = getLong("amountPaise") ?: return null,
        occurredAt = getLong("occurredAt") ?: return null,
        createdAt = getLong("createdAt") ?: return null,
        syncedToCloud = true
    )
}

private fun com.google.firebase.firestore.DocumentSnapshot.toCustomerEntity(orgId: String): CustomerEntity? {
    val customerId = getString("customerId") ?: return null
    return CustomerEntity(
        customerId = customerId,
        orgId = orgId,
        customerName = getString("customerName") ?: return null,
        phone = getString("phone"),
        gstin = getString("gstin") ?: return null,
        addressLine1 = getString("addressLine1") ?: return null,
        city = getString("city") ?: return null,
        state = getString("state") ?: return null,
        stateCode = getString("stateCode") ?: return null,
        pincode = getString("pincode"),
        isActive = getBoolean("isActive") ?: true,
        updatedAt = getLong("updatedAt") ?: return null,
        notes = getString("notes"),
        // Restored rows are, by definition, already in the cloud - never re-pushed.
        syncedToCloud = true
    )
}
