package com.verity.platform.sync

import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.verity.platform.database.dao.DocumentDao
import com.verity.platform.database.dao.LedgerEntryDao
import com.verity.platform.database.entities.DocumentEntity
import com.verity.platform.database.entities.LedgerEntryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/**
 * FirebaseSyncClient
 *
 * Push side of Phase 1 cloud sync: mirrors local writes to Firestore/Storage, fire-and-forget.
 * Never awaited by a caller — relies entirely on the Firestore Android SDK's own built-in
 * offline persistence and durable write queue for retry, rather than a hand-rolled outbox/
 * WorkManager (see the cloud-sync plan's Context section for why that machinery, needed for
 * Supabase, is redundant here). Re-implementing retry on top of an SDK that already retries
 * would be two queues fighting for the same job — a single-ownership violation.
 */
interface FirebaseSyncClient {
    fun pushDocument(document: DocumentEntity)
    fun pushLedgerEntry(entry: LedgerEntryEntity)
    fun pushPdf(orgId: String, documentNumber: String, file: File)
    suspend fun downloadPdf(orgId: String, documentNumber: String, destination: File): Boolean
}

class DefaultFirebaseSyncClient(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val documentDao: DocumentDao,
    private val ledgerEntryDao: LedgerEntryDao,
    private val syncStatusStore: SyncStatusStore,
    private val downloadTimeoutMillis: Long = 5_000
) : FirebaseSyncClient {

    // Fire-and-forget pushes need somewhere to run their post-success Room update (a suspend
    // call) without blocking the caller — this class owns that background lifecycle itself
    // rather than requiring every caller to plumb a scope through.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun pushDocument(document: DocumentEntity) {
        firestore.collection(documentsCollection(document.orgId))
            .document(document.documentId)
            .set(document.toFirestoreFields())
            .addOnSuccessListener {
                scope.launch { documentDao.markSyncedToCloud(document.documentId) }
                syncStatusStore.recordSyncSuccess(System.currentTimeMillis())
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "pushDocument failed for ${document.documentId} — SDK will retry.", e)
            }
    }

    override fun pushLedgerEntry(entry: LedgerEntryEntity) {
        firestore.collection(ledgerEntriesCollection(entry.orgId))
            .document(entry.entryId)
            .set(entry.toFirestoreFields())
            .addOnSuccessListener {
                scope.launch { ledgerEntryDao.markSyncedToCloud(entry.entryId) }
                syncStatusStore.recordSyncSuccess(System.currentTimeMillis())
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "pushLedgerEntry failed for ${entry.entryId} — SDK will retry.", e)
            }
    }

    override fun pushPdf(orgId: String, documentNumber: String, file: File) {
        storage.reference.child(pdfPath(orgId, documentNumber))
            .putFile(Uri.fromFile(file))
            .addOnFailureListener { e ->
                Log.w(TAG, "pushPdf failed for $documentNumber — will retry next time it's viewed.", e)
            }
    }

    override suspend fun downloadPdf(orgId: String, documentNumber: String, destination: File): Boolean {
        // Bug fixed 2026-09-15: this had no timeout, so ensurePdf() (which tries this before
        // ever regenerating - see DefaultInvoicePdfRenderer) would hang indefinitely while
        // offline, since a brand-new document always has no local file yet, making this the
        // *first* thing every offline finalize hit. Caught on a real device in airplane mode -
        // exactly the guarantee (finalize never blocks on network, Principle 6) this whole
        // architecture exists to protect, broken by one missing timeout. Same
        // withTimeoutOrNull-then-fall-through pattern as InvoiceNumberAllocator, which already
        // had this protection.
        val downloaded = withTimeoutOrNull(downloadTimeoutMillis) {
            try {
                storage.reference.child(pdfPath(orgId, documentNumber)).getFile(destination).await()
                true
            } catch (e: Exception) {
                // Expected and silent whenever there's genuinely no cloud copy yet (a very
                // recent, still-offline finalize) — ensurePdf()'s caller falls through to
                // regeneration.
                false
            }
        }
        return downloaded ?: false
    }

    private companion object {
        const val TAG = "FirebaseSyncClient"
    }
}

private fun documentsCollection(orgId: String) = "orgs/$orgId/documents"
private fun ledgerEntriesCollection(orgId: String) = "orgs/$orgId/ledgerEntries"
private fun pdfPath(orgId: String, documentNumber: String) = "orgs/$orgId/pdfs/$documentNumber.pdf"

private fun DocumentEntity.toFirestoreFields(): Map<String, Any?> = mapOf(
    "documentId" to documentId,
    "orgId" to orgId,
    "documentType" to documentType,
    "sequenceNumber" to sequenceNumber,
    "documentNumber" to documentNumber,
    "customerId" to customerId,
    "customerName" to customerName,
    "issueDateEpochDay" to issueDateEpochDay,
    "grandTotalPaise" to grandTotalPaise,
    "linkedDocumentId" to linkedDocumentId,
    "payloadJson" to payloadJson,
    "finalizedAt" to finalizedAt,
    "searchIndexText" to searchIndexText
)

private fun LedgerEntryEntity.toFirestoreFields(): Map<String, Any?> = mapOf(
    "entryId" to entryId,
    "orgId" to orgId,
    "customerId" to customerId,
    "documentId" to documentId,
    "amountPaise" to amountPaise,
    "occurredAt" to occurredAt,
    "createdAt" to createdAt
)
