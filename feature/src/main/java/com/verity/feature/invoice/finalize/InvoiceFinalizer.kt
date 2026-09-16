package com.verity.feature.invoice.finalize

import com.verity.core.document.model.InvoiceDocumentModel
import com.verity.feature.invoice.draft.InvoiceDraftUiState
import java.time.LocalDate

/**
 * InvoiceFinalizer
 *
 * Port for turning a draft into a permanent, numbered document. Feature defines this interface;
 * platform implements it (same direction as CustomerAutocompleteDataSource) because assigning a
 * number and persisting the result requires database access, which only platform may touch.
 */
interface InvoiceFinalizer {

    /**
     * @param customerId the billed-to party's customer id (see DraftAddress.customerId) —
     *   who owes the money, used for the ledger entry and the persisted document's customerId.
     * @param jobWorkLinkage the "Challan + Invoice" job-work cross-document numbering behavior
     *   for this finalize call. Defaults to [JobWorkLinkage.None] — an ordinary, unlinked
     *   Invoice or Challan finalize.
     * @throws IllegalArgumentException if the draft is not a finalizable Invoice, or if
     *   [jobWorkLinkage] doesn't match the draft's own documentType (see [JobWorkLinkage]).
     */
    suspend fun finalize(
        draft: InvoiceDraftUiState,
        customerId: String,
        jobWorkLinkage: JobWorkLinkage = JobWorkLinkage.None
    ): InvoiceDocumentModel
}

/**
 * JobWorkLinkage
 *
 * Cross-document numbering behavior for a "Challan + Invoice" job-work finalize. A Challan links
 * to at most one Invoice, always declared upfront (never decided later) — see CLAUDE.md's
 * "Challan → Invoice (job work)" domain model section.
 */
sealed interface JobWorkLinkage {

    /** Ordinary finalize — no job-work linkage. Valid for any draft documentType. */
    data object None : JobWorkLinkage

    /**
     * A job-work Challan finalize (draft.documentType must be CHALLAN): also atomically
     * allocate-and-consume the next Invoice number, baking it into the Challan's own
     * [com.verity.core.document.model.DocumentJobWorkLink] before persisting. That reserved
     * number is a real, consumed number from that moment on — if the user never finalizes the
     * continuation Invoice, it's a permanent gap in the Invoice sequence (accepted trade-off).
     */
    data object ReserveLinkedInvoiceNumber : JobWorkLinkage

    /**
     * An Invoice finalize continuing a prior job-work Challan (draft.documentType must be
     * INVOICE): reuse [documentNumber] — already allocated at that Challan's finalize time —
     * instead of allocating a new one, and resolve [linkedChallanDocumentNumber] to a real
     * documentId to write into DocumentEntity.linkedDocumentId and this Invoice's own
     * DocumentJobWorkLink.
     *
     * @throws IllegalArgumentException if no document with [linkedChallanDocumentNumber] exists.
     */
    data class UseReservedNumber(
        val documentNumber: String,
        val linkedChallanDocumentNumber: String,
        val linkedChallanDate: LocalDate
    ) : JobWorkLinkage
}
