package com.verity.platform.document

import com.verity.core.document.model.DocumentType
import com.verity.core.document.model.InvoiceDocumentModel
import com.verity.core.formatting.money.Money
import com.verity.feature.document.search.CustomerSearchResult
import com.verity.feature.document.search.DocumentMatchOutcome
import com.verity.feature.document.search.DocumentSearchDataSource
import com.verity.feature.document.search.DocumentSearchRanking
import com.verity.feature.document.search.DocumentSearchResult
import com.verity.feature.document.search.DocumentSearchResults
import com.verity.platform.database.PlatformDatabase
import com.verity.platform.database.entities.DocumentEntity
import kotlinx.serialization.json.Json
import java.time.LocalDate

private const val MAX_CUSTOMER_MATCHES = 2

/**
 * DefaultDocumentSearchDataSource
 *
 * platform's implementation of DocumentSearchDataSource: `DocumentDao.search()` narrows candidates
 * via a plain SQL LIKE over the flattened searchIndexText column (fast — no JSON decode yet, see
 * that query's doc comment), then only the narrowed set gets its payloadJson decoded, to run
 * DocumentSearchRanking.match() and produce a snippet. Customer matches reuse
 * `getActiveCustomers()` the same way DefaultCustomerAutocompleteDataSource already does — a
 * small table, in-memory filtering is the established pattern there.
 *
 * Multi-word queries are ANDed (every token must appear somewhere, order-independent — see
 * DocumentSearchRanking's doc comment). Room's `search()` query only takes one substring, so it's
 * called with just the longest token (the most selective single LIKE, and a safe prefilter since
 * any real match must contain it too); the remaining tokens are checked against the already-
 * fetched `searchIndexText` column in Kotlin before paying for a JSON decode.
 */
class DefaultDocumentSearchDataSource(
    private val database: PlatformDatabase
) : DocumentSearchDataSource {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun search(query: String): DocumentSearchResults {
        val tokens = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) {
            return DocumentSearchResults(customers = emptyList(), documents = emptyList())
        }

        val customers = database.customerDao()
            .getActiveCustomers()
            .asSequence()
            .filter { customer ->
                // Same fields DefaultCustomerAutocompleteDataSource already matches on, flattened
                // so a query like "garg mumbai" can match name and city as separate tokens.
                val searchableText = listOf(customer.customerName, customer.gstin, customer.city, customer.state)
                    .joinToString(" ") { it.lowercase() }
                tokens.all { searchableText.contains(it) }
            }
            .take(MAX_CUSTOMER_MATCHES)
            .map { CustomerSearchResult(it.customerId, it.customerName, it.gstin) }
            .toList()

        val prefilterToken = tokens.maxByOrNull { it.length } ?: return DocumentSearchResults(customers, emptyList())

        val documents = database.documentDao()
            .search(prefilterToken)
            .filter { entity -> tokens.all { entity.searchIndexText.contains(it) } }
            .map { entity -> entity to decode(entity) }
            .map { (entity, document) ->
                val outcome = DocumentSearchRanking.match(
                    documentNumber = entity.documentNumber,
                    customerName = entity.customerName,
                    document = document,
                    tokens = tokens
                )
                entity to outcome
            }
            .sortedWith(
                compareBy<Pair<DocumentEntity, DocumentMatchOutcome>> { it.second.priority }
                    .thenByDescending { it.first.finalizedAt }
            )
            .map { (entity, outcome) -> entity.toSearchResult(outcome) }

        return DocumentSearchResults(customers = customers, documents = documents)
    }

    private fun decode(entity: DocumentEntity): InvoiceDocumentModel =
        json.decodeFromString(InvoiceDocumentModel.serializer(), entity.payloadJson)

    private fun DocumentEntity.toSearchResult(
        outcome: DocumentMatchOutcome
    ): DocumentSearchResult = DocumentSearchResult(
        documentId = documentId,
        documentNumber = documentNumber,
        customerName = customerName,
        documentType = DocumentType.valueOf(documentType),
        issueDate = LocalDate.ofEpochDay(issueDateEpochDay),
        grandTotal = Money.ofPaise(grandTotalPaise),
        matchKind = outcome.matchKind,
        matchedSnippet = outcome.snippet
    )
}
