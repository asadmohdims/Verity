package com.verity.feature.invoice.finalize

import com.verity.feature.invoice.draft.DraftDocumentType

/**
 * Non-binding preview of the next document number a finalize would likely assign right now — a
 * plain read, not a reservation. Only finalize() itself (via InvoiceNumberAllocator) actually
 * allocates a number; this exists purely to give the user a "this will probably be numbered X"
 * hint while still drafting. It can be wrong — another device finalizing first, or the online
 * counter having moved ahead of what this device has synced — and that's an accepted trade-off,
 * not a bug: see InvoiceNumberAllocator's own doc comment for the same category of residual risk.
 */
interface DocumentNumberPreviewDataSource {
    suspend fun peekNextNumber(documentType: DraftDocumentType): String
}
