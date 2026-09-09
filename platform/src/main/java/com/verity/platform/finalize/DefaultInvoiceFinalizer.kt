package com.verity.platform.finalize

import androidx.room.withTransaction
import com.verity.core.document.model.HARDCODED_SELLER
import com.verity.core.document.model.InvoiceDocumentModel
import com.verity.feature.invoice.finalize.InvoiceFinalizer
import com.verity.feature.invoice.projection.DraftToInvoiceDocument
import com.verity.invoice.draft.DraftDocumentType
import com.verity.invoice.draft.InvoiceDraftUiState
import com.verity.platform.database.PlatformDatabase
import com.verity.platform.database.entities.DocumentEntity
import com.verity.platform.database.entities.LedgerEntryEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Clock
import java.util.UUID

/** Single-org for now — see CLAUDE.md, multi-org is a real future goal kept cheap, not urgent. */
const val DEFAULT_ORG_ID = "default-org"

/**
 * DefaultInvoiceFinalizer
 *
 * Assigns the next sequence number, projects the draft into an InvoiceDocumentModel via the
 * existing (feature-owned) DraftToInvoiceDocument, and persists both the document and its
 * ledger entry in one transaction.
 *
 * Numbering has no collision detection or reconciliation by design — see CLAUDE.md's Invoice
 * numbering decision. The sequence read happens inside the transaction purely to avoid a
 * pointless-to-leave-open double-tap race, not to enforce uniqueness (there is deliberately no
 * DB constraint for that).
 */
class DefaultInvoiceFinalizer(
    private val database: PlatformDatabase,
    private val clock: Clock
) : InvoiceFinalizer {

    private val json = Json { ignoreUnknownKeys = false }

    override suspend fun finalize(draft: InvoiceDraftUiState, customerId: String): InvoiceDocumentModel {
        require(draft.documentType == DraftDocumentType.INVOICE) {
            "Only Invoice finalize is supported in this milestone"
        }

        return database.withTransaction {
            val nextSequence = (database.documentDao().getMaxSequenceNumber(DEFAULT_ORG_ID, "INVOICE") ?: 0L) + 1
            val documentNumber = "INV-" + nextSequence.toString().padStart(6, '0')

            val document = DraftToInvoiceDocument.project(
                draft = draft,
                documentNumber = documentNumber,
                seller = HARDCODED_SELLER,
                clock = clock
            )

            val now = System.currentTimeMillis()
            val documentId = UUID.randomUUID().toString()

            database.documentDao().insert(
                DocumentEntity(
                    documentId = documentId,
                    orgId = DEFAULT_ORG_ID,
                    documentType = "INVOICE",
                    sequenceNumber = nextSequence,
                    documentNumber = documentNumber,
                    customerId = customerId,
                    customerName = document.parties.billedTo.name,
                    issueDateEpochDay = document.identity.issueDate.toEpochDay(),
                    grandTotalPaise = document.totals.grandTotalPaise,
                    linkedDocumentId = null,
                    payloadJson = json.encodeToString(document),
                    finalizedAt = now
                )
            )

            database.ledgerEntryDao().insert(
                LedgerEntryEntity(
                    entryId = UUID.randomUUID().toString(),
                    orgId = DEFAULT_ORG_ID,
                    customerId = customerId,
                    documentId = documentId,
                    amountPaise = document.totals.grandTotalPaise,
                    occurredAt = now,
                    createdAt = now
                )
            )

            document
        }
    }
}
