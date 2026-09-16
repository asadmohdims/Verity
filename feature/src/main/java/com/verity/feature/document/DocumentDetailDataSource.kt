package com.verity.feature.document

import com.verity.core.document.model.InvoiceDocumentModel

/**
 * DocumentDetailDataSource
 *
 * Port for loading one finalized document's full payload by id, for the Document Detail screen.
 * Feature defines this interface; platform implements it (same direction as HomeDataSource /
 * CustomerAutocompleteDataSource / InvoiceFinalizer) because reading persisted documents requires
 * database access, which only platform may touch.
 *
 * Deliberately returns the full InvoiceDocumentModel, unlike HomeDataSource's DocumentSummary —
 * a detail screen needs the whole document (line items, parties, taxation), not just the
 * denormalized list-row projection.
 */
interface DocumentDetailDataSource {
    suspend fun loadDocument(documentId: String): InvoiceDocumentModel?

    /**
     * Reverse lookup for a job-work Challan's "Linked Invoice" — the Invoice (if any) whose
     * own linkedDocumentId points back at [documentId]. Null both when this document has no
     * job-work link, and when it does but that Invoice was never finalized (the reserved number
     * can be burned with no Invoice ever created — see InvoiceFinalizer.JobWorkLinkage).
     */
    suspend fun findLinkedDocumentId(documentId: String): String?
}
