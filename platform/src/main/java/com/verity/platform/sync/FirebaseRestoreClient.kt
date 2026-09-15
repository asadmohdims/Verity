package com.verity.platform.sync

import com.google.firebase.firestore.FirebaseFirestore
import com.verity.platform.database.dao.DocumentDao
import com.verity.platform.database.dao.LedgerEntryDao
import com.verity.platform.database.entities.DocumentEntity
import com.verity.platform.database.entities.LedgerEntryEntity
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * FirebaseRestoreClient
 *
 * New-device / reinstall restore — a one-time bulk pull, never a live subscription (see the
 * cloud-sync plan: cloud is backup/restore, not multi-device live sync). Only ever called by
 * MainActivity when local Room has no documents yet, so this is always a bulk-load into an
 * empty database, never a merge into live local state — no conflict logic needed.
 */
interface FirebaseRestoreClient {
    suspend fun restoreAll(orgId: String)
}

/**
 * Read side, kept behind its own interface so restoreAll's REPLACE-based safety (the actual
 * thing worth unit-testing — see the cloud-sync plan's verification section) can be tested
 * against a fake, without a real Firestore instance.
 */
interface RemoteRestoreSource {
    suspend fun fetchAllDocuments(orgId: String): List<DocumentEntity>
    suspend fun fetchAllLedgerEntries(orgId: String): List<LedgerEntryEntity>
}

class DefaultFirebaseRestoreClient(
    private val remoteSource: RemoteRestoreSource,
    private val documentDao: DocumentDao,
    private val ledgerEntryDao: LedgerEntryDao,
    private val timeoutMillis: Long = 15_000
) : FirebaseRestoreClient {

    /**
     * Timeout-guarded (added 2026-09-15, same fix as FirebaseSyncClient.downloadPdf and
     * FirebaseAuthGate.ensureSignedIn - found by testing offline on a real device). This runs
     * from MainActivity's startup LaunchedEffect, not the finalize path, so it was never the
     * cause of the offline-finalize hang, but it's the same unguarded-network-await bug class -
     * without this, a restore attempted while offline would hang indefinitely instead of failing
     * fast and letting the next app launch retry it. withTimeout (throws), not withTimeoutOrNull:
     * there's no partial-success value to fall back to here, so a timeout should surface as a
     * normal failure to the caller's existing runCatching, not be silently swallowed into "did
     * nothing."
     */
    override suspend fun restoreAll(orgId: String) = withTimeout(timeoutMillis) {
        // REPLACE-based inserts (not the normal ABORT-on-duplicate insert()) — makes this safe
        // to re-run in full if a previous attempt was interrupted partway, rather than needing
        // separate resume/pagination bookkeeping.
        documentDao.upsertAllFromCloud(remoteSource.fetchAllDocuments(orgId))
        ledgerEntryDao.upsertAllFromCloud(remoteSource.fetchAllLedgerEntries(orgId))
    }
}

class FirestoreRestoreSource(private val firestore: FirebaseFirestore) : RemoteRestoreSource {

    override suspend fun fetchAllDocuments(orgId: String): List<DocumentEntity> {
        val snapshot = firestore.collection("orgs/$orgId/documents").get().await()
        return snapshot.documents.mapNotNull { it.toDocumentEntity(orgId) }
    }

    override suspend fun fetchAllLedgerEntries(orgId: String): List<LedgerEntryEntity> {
        val snapshot = firestore.collection("orgs/$orgId/ledgerEntries").get().await()
        return snapshot.documents.mapNotNull { it.toLedgerEntryEntity(orgId) }
    }
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
