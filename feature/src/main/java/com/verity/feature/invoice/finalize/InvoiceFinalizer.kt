package com.verity.feature.invoice.finalize

import com.verity.core.document.model.InvoiceDocumentModel
import com.verity.invoice.draft.InvoiceDraftUiState

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
     * @throws IllegalArgumentException if the draft is not a finalizable Invoice
     */
    suspend fun finalize(draft: InvoiceDraftUiState, customerId: String): InvoiceDocumentModel
}
