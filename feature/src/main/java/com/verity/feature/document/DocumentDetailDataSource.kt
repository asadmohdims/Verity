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
}
