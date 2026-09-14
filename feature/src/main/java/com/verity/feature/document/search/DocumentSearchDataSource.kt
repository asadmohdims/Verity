package com.verity.feature.document.search

import com.verity.core.document.model.DocumentType
import com.verity.core.formatting.money.Money
import java.time.LocalDate

/**
 * DocumentSearchDataSource
 *
 * Port for the Documents search screen. platform implements it (same direction as
 * CustomerAutocompleteDataSource/HomeDataSource) because ranking requires decoding each
 * candidate's persisted JSON payload, which only platform may touch. The "recent documents"
 * empty-query state deliberately does NOT live here — DocumentSearchViewModel reuses HomeDataSource
 * for that, since it's the exact same already-sorted list Home/Documents-tab already show.
 */
interface DocumentSearchDataSource {
    suspend fun search(query: String): DocumentSearchResults
}

/**
 * Zero to two customer matches (for the top "Customers" group, tap → customer rollup) alongside
 * ranked document matches.
 */
data class DocumentSearchResults(
    val customers: List<CustomerSearchResult>,
    val documents: List<DocumentSearchResult>
)

data class CustomerSearchResult(
    val customerId: String,
    val customerName: String,
    val gstin: String?
)

data class DocumentSearchResult(
    val documentId: String,
    val documentNumber: String,
    val customerName: String,
    val documentType: DocumentType,
    val issueDate: LocalDate,
    val grandTotal: Money,
    val matchKind: DocumentMatchKind,
    /** Non-null only for a body match (line item/transport field) — what the result row highlights. */
    val matchedSnippet: SnippetMatch?
)
