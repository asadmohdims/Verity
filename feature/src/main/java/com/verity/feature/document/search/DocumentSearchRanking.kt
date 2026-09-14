package com.verity.feature.document.search

import com.verity.core.document.model.InvoiceDocumentModel

/**
 * DocumentSearchRanking
 *
 * Pure matching/ranking logic for document search, deliberately kept free of Room/Android so it
 * can be JVM-unit-tested directly (same rationale as the Draft Reducers — see CLAUDE.md). The
 * platform-owned data source does the IO (SQL narrowing, JSON decode) and calls [match] once per
 * candidate row it already knows contains the query somewhere.
 *
 * Priority order (lower number = ranked higher): an exact/substring hit on the document's own
 * number is the strongest signal a user typed exactly what they were looking for; a customer/
 * party match is next; then the document's actual content (line items, transport fields); a
 * fallback of "matched somewhere in the flattened index but not one of the fields checked above"
 * (place of supply, notes, amount) ranks lowest but is still a real match.
 */
enum class DocumentMatchKind {
    DOCUMENT_NUMBER,
    CUSTOMER,
    LINE_ITEM,
    TRANSPORT,
    OTHER
}

/** A short excerpt of a matched field, plus the offset range of the actual match within it, for highlighting. */
data class SnippetMatch(
    val text: String,
    val matchRange: IntRange
)

data class DocumentMatchOutcome(
    val priority: Int,
    val matchKind: DocumentMatchKind,
    val snippet: SnippetMatch?
)

object DocumentSearchRanking {

    fun match(
        documentNumber: String,
        customerName: String,
        document: InvoiceDocumentModel,
        normalizedQuery: String
    ): DocumentMatchOutcome {
        require(normalizedQuery.isNotBlank()) { "normalizedQuery must not be blank" }

        if (documentNumber.lowercase().contains(normalizedQuery)) {
            return DocumentMatchOutcome(priority = 0, matchKind = DocumentMatchKind.DOCUMENT_NUMBER, snippet = null)
        }

        val partyFields = listOf(
            customerName,
            document.parties.billedTo.name,
            document.parties.billedTo.gstin,
            document.parties.shippedTo.name,
            document.parties.shippedTo.gstin
        )
        if (partyFields.any { it.lowercase().contains(normalizedQuery) }) {
            return DocumentMatchOutcome(priority = 1, matchKind = DocumentMatchKind.CUSTOMER, snippet = null)
        }

        for (item in document.lineItems) {
            (buildSnippet(item.description, normalizedQuery) ?: buildSnippet(item.hsnCode, normalizedQuery))?.let {
                return DocumentMatchOutcome(priority = 2, matchKind = DocumentMatchKind.LINE_ITEM, snippet = it)
            }
        }

        document.logistics?.let { logistics ->
            listOfNotNull(
                logistics.transporterName,
                logistics.vehicleNumber,
                logistics.grOrLrNumber,
                logistics.ewayBillNumber
            ).forEach { field ->
                buildSnippet(field, normalizedQuery)?.let {
                    return DocumentMatchOutcome(priority = 3, matchKind = DocumentMatchKind.TRANSPORT, snippet = it)
                }
            }
        }

        return DocumentMatchOutcome(priority = 4, matchKind = DocumentMatchKind.OTHER, snippet = null)
    }

    /**
     * Builds a short excerpt of [text] centered on the first occurrence of [normalizedQuery]
     * (case-insensitive), with an ellipsis on whichever side was truncated — null if it doesn't
     * occur at all.
     */
    fun buildSnippet(text: String, normalizedQuery: String, contextChars: Int = 24): SnippetMatch? {
        val matchIndex = text.lowercase().indexOf(normalizedQuery)
        if (matchIndex < 0) return null

        val start = (matchIndex - contextChars).coerceAtLeast(0)
        val end = (matchIndex + normalizedQuery.length + contextChars).coerceAtMost(text.length)

        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < text.length) "…" else ""
        val snippetText = prefix + text.substring(start, end) + suffix

        val rangeStart = (matchIndex - start) + prefix.length
        val rangeEnd = rangeStart + normalizedQuery.length

        return SnippetMatch(text = snippetText, matchRange = rangeStart until rangeEnd)
    }
}
