package com.verity.platform.finalize

import androidx.room.withTransaction
import com.verity.core.document.model.DocumentJobWorkLink
import com.verity.core.document.model.HARDCODED_SELLER
import com.verity.core.document.model.InvoiceDocumentModel
import com.verity.core.document.search.buildSearchIndexText
import com.verity.feature.invoice.finalize.InvoiceFinalizer
import com.verity.feature.invoice.finalize.JobWorkLinkage
import com.verity.feature.invoice.projection.DraftToInvoiceDocument
import com.verity.feature.invoice.draft.DraftDocumentType
import com.verity.feature.invoice.draft.InvoiceDraftUiState
import com.verity.platform.database.PlatformDatabase
import com.verity.platform.database.entities.DocumentEntity
import com.verity.platform.database.entities.LedgerEntryEntity
import com.verity.platform.sync.FirebaseSyncClient
import com.verity.platform.sync.InvoiceNumberAllocator
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Clock
import java.time.LocalDate
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
 *
 * Job work ("Challan + Invoice", see [JobWorkLinkage]): a Challan finalize that reserves a linked
 * Invoice number allocates *two* numbers before the transaction opens instead of one. They're
 * independent counters (this Challan's own sequence, and the Invoice's), so the two allocate()
 * calls run concurrently rather than one after another - no reason to pay for two network round
 * trips back to back when neither depends on the other's result. If the transaction then fails,
 * both are burned rather than just one — the same accepted risk category as above, just
 * occasionally two-wide instead of one-wide.
 */
class DefaultInvoiceFinalizer(
    private val database: PlatformDatabase,
    private val clock: Clock,
    private val numberAllocator: InvoiceNumberAllocator,
    private val syncClient: FirebaseSyncClient
) : InvoiceFinalizer {

    private val json = Json { ignoreUnknownKeys = false }

    override suspend fun finalize(
        draft: InvoiceDraftUiState,
        customerId: String,
        jobWorkLinkage: JobWorkLinkage
    ): InvoiceDocumentModel {
        val documentTypeColumn = when (draft.documentType) {
            DraftDocumentType.INVOICE -> "INVOICE"
            DraftDocumentType.CHALLAN -> "CHALLAN"
        }
        val numberPrefix = when (draft.documentType) {
            DraftDocumentType.INVOICE -> "INV-"
            DraftDocumentType.CHALLAN -> "CH-"
        }

        // Reusing a reserved number (UseReservedNumber) only ever applies to the Invoice half of
        // a job-work pair; reserving one (ReserveLinkedInvoiceNumber) only to the Challan half —
        // both declared upfront by the "Challan + Invoice" flow, never mixed up by the caller.
        if (jobWorkLinkage is JobWorkLinkage.UseReservedNumber) {
            require(draft.documentType == DraftDocumentType.INVOICE) {
                "JobWorkLinkage.UseReservedNumber only applies to an Invoice finalize"
            }
        }
        if (jobWorkLinkage is JobWorkLinkage.ReserveLinkedInvoiceNumber) {
            require(draft.documentType == DraftDocumentType.CHALLAN) {
                "JobWorkLinkage.ReserveLinkedInvoiceNumber only applies to a Challan finalize"
            }
        }

        val nextSequence: Long
        val documentNumber: String
        // Reserved BEFORE opening the transaction, same reasoning as the allocate() call above —
        // it can hit the network (up to its own timeout via numberAllocator), and this is a
        // second real number being consumed on top of this document's own.
        val reservedInvoiceNumber: String?
        if (jobWorkLinkage is JobWorkLinkage.UseReservedNumber) {
            documentNumber = jobWorkLinkage.documentNumber
            nextSequence = documentNumber.substringAfterLast('-').toLong()
            reservedInvoiceNumber = null
        } else if (jobWorkLinkage is JobWorkLinkage.ReserveLinkedInvoiceNumber) {
            // Two independent counters, neither depending on the other's result - run both
            // allocate() calls concurrently instead of back to back (see class doc comment).
            val (ownSequence, invoiceSequence) = coroutineScope {
                val ownSequenceDeferred = async { numberAllocator.allocate(DEFAULT_ORG_ID, documentTypeColumn) }
                val invoiceSequenceDeferred = async { numberAllocator.allocate(DEFAULT_ORG_ID, "INVOICE") }
                ownSequenceDeferred.await() to invoiceSequenceDeferred.await()
            }
            nextSequence = ownSequence
            documentNumber = numberPrefix + nextSequence.toString().padStart(6, '0')
            reservedInvoiceNumber = "INV-" + invoiceSequence.toString().padStart(6, '0')
        } else {
            nextSequence = numberAllocator.allocate(DEFAULT_ORG_ID, documentTypeColumn)
            documentNumber = numberPrefix + nextSequence.toString().padStart(6, '0')
            reservedInvoiceNumber = null
        }

        // Cheap local read, also done before opening the transaction: resolves the job-work
        // Invoice's linked Challan (known only by number on the draft) to its real documentId.
        val linkedChallanEntity: DocumentEntity? =
            (jobWorkLinkage as? JobWorkLinkage.UseReservedNumber)?.let { linkage ->
                requireNotNull(
                    database.documentDao()
                        .findByDocumentNumber(DEFAULT_ORG_ID, linkage.linkedChallanDocumentNumber)
                ) {
                    "Linked Challan ${linkage.linkedChallanDocumentNumber} not found — cannot finalize job-work Invoice"
                }
            }

        lateinit var documentEntity: DocumentEntity
        var ledgerEntryEntity: LedgerEntryEntity? = null

        val document = database.withTransaction {
            var document = DraftToInvoiceDocument.project(
                draft = draft,
                documentNumber = documentNumber,
                seller = HARDCODED_SELLER,
                clock = clock
            )

            if (reservedInvoiceNumber != null) {
                document = document.copy(
                    jobWorkLink = DocumentJobWorkLink(
                        linkedDocumentNumber = reservedInvoiceNumber,
                        linkedDocumentDate = LocalDate.now(clock),
                        linkedDocumentId = null
                    )
                )
            }
            if (linkedChallanEntity != null) {
                document = document.copy(
                    jobWorkLink = document.jobWorkLink?.copy(linkedDocumentId = linkedChallanEntity.documentId)
                )
            }

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
                linkedDocumentId = linkedChallanEntity?.documentId,
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
