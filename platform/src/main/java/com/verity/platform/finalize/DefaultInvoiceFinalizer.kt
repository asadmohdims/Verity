package com.verity.platform.finalize

import androidx.room.withTransaction
import com.verity.core.document.model.HARDCODED_SELLER
import com.verity.core.document.model.InvoiceDocumentModel
import com.verity.core.document.search.buildSearchIndexText
import com.verity.feature.invoice.finalize.InvoiceFinalizer
import com.verity.feature.invoice.projection.DraftToInvoiceDocument
import com.verity.feature.invoice.draft.DraftDocumentType
import com.verity.feature.invoice.draft.InvoiceDraftUiState
import com.verity.platform.database.PlatformDatabase
import com.verity.platform.database.entities.DocumentEntity
import com.verity.platform.database.entities.LedgerEntryEntity
import com.verity.platform.sync.FirebaseSyncClient
import com.verity.platform.sync.InvoiceNumberAllocator
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Clock
import java.util.UUID

/** Single-org for now — see CLAUDE.md, multi-org is a real future goal kept cheap, not urgent. */
const val DEFAULT_ORG_ID = "default-org"

/**
 * DefaultInvoiceFinalizer
 *
 * Assigns the next sequence number (independently per document type — "INV-" / "CH-" each have
 * their own count), projects the draft into an InvoiceDocumentModel via the existing
 * (feature-owned) DraftToInvoiceDocument, and persists the document — plus a ledger entry, for
 * Invoice only, since a Challan is a delivery document rather than a billing event — in one
 * transaction.
 *
 * Numbering: atomic online (a Firestore transaction), local-fallback offline — see
 * InvoiceNumberAllocator's doc comment and the cloud-sync plan's "Atomic invoice numbering"
 * section for why (a shared login can mean two physical devices finalizing at once) and why full
 * elimination of the collision risk isn't attempted (would require every finalize to round-trip
 * the network, breaking the never-blocked-on-network guarantee).
 *
 * numberAllocator.allocate() runs *before* the Room transaction opens, not inside it — it can
 * take real network time (up to its timeout), and holding a SQLite transaction open for that
 * long would block other concurrent DB work for no reason. Only the fast local inserts are
 * transactional; the deliberate risk this reopens (two on-device finalize calls racing between
 * allocate() and the DB write) is the same "pointless-to-leave-open double-tap race" the original
 * local-only version already accepted, not a new one.
 */
class DefaultInvoiceFinalizer(
    private val database: PlatformDatabase,
    private val clock: Clock,
    private val numberAllocator: InvoiceNumberAllocator,
    private val syncClient: FirebaseSyncClient
) : InvoiceFinalizer {

    private val json = Json { ignoreUnknownKeys = false }

    override suspend fun finalize(draft: InvoiceDraftUiState, customerId: String): InvoiceDocumentModel {
        val documentTypeColumn = when (draft.documentType) {
            DraftDocumentType.INVOICE -> "INVOICE"
            DraftDocumentType.CHALLAN -> "CHALLAN"
        }
        val numberPrefix = when (draft.documentType) {
            DraftDocumentType.INVOICE -> "INV-"
            DraftDocumentType.CHALLAN -> "CH-"
        }

        val nextSequence = numberAllocator.allocate(DEFAULT_ORG_ID, documentTypeColumn)
        val documentNumber = numberPrefix + nextSequence.toString().padStart(6, '0')

        lateinit var documentEntity: DocumentEntity
        var ledgerEntryEntity: LedgerEntryEntity? = null

        val document = database.withTransaction {
            val document = DraftToInvoiceDocument.project(
                draft = draft,
                documentNumber = documentNumber,
                seller = HARDCODED_SELLER,
                clock = clock
            )

            val now = System.currentTimeMillis()
            val documentId = UUID.randomUUID().toString()

            documentEntity = DocumentEntity(
                documentId = documentId,
                orgId = DEFAULT_ORG_ID,
                documentType = documentTypeColumn,
                sequenceNumber = nextSequence,
                documentNumber = documentNumber,
                customerId = customerId,
                customerName = document.parties.billedTo.name,
                issueDateEpochDay = document.identity.issueDate.toEpochDay(),
                grandTotalPaise = document.totals.grandTotalPaise,
                linkedDocumentId = null,
                payloadJson = json.encodeToString(document),
                finalizedAt = now,
                searchIndexText = buildSearchIndexText(document)
            )
            database.documentDao().insert(documentEntity)

            // Only Invoice finalization is a billing event - a Challan is a delivery document,
            // not a receivable, so it doesn't get a ledger entry.
            if (draft.documentType == DraftDocumentType.INVOICE) {
                ledgerEntryEntity = LedgerEntryEntity(
                    entryId = UUID.randomUUID().toString(),
                    orgId = DEFAULT_ORG_ID,
                    customerId = customerId,
                    documentId = documentId,
                    amountPaise = document.totals.grandTotalPaise,
                    occurredAt = now,
                    createdAt = now
                )
                database.ledgerEntryDao().insert(ledgerEntryEntity!!)
            }

            document
        }

        // Fire-and-forget, after the local transaction has committed - never blocks finalize's
        // return on the network (Principle 6). See FirebaseSyncClient's doc comment.
        syncClient.pushDocument(documentEntity)
        ledgerEntryEntity?.let { syncClient.pushLedgerEntry(it) }

        return document
    }
}
