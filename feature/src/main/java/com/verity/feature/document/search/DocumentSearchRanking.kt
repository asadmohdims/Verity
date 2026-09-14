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
 *
 * Multi-word queries (e.g. "pipes garg") are ANDed: a candidate must satisfy every token
 * somewhere, but the tokens don't have to hit the same field — one might match a line item,
 * another a customer name. Priority still follows the single cascade above (does *any* token
 * reach this level), but the snippet shown to the user is chosen independently: a line-item
 * match is preferred whenever any token hits one, even if a different token already won a
 * higher-priority level — customer name is already visible in the result row regardless, so
 * highlighting the item is more useful than showing no snippet at all.
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
        tokens: List<String>
    ): DocumentMatchOutcome {
        require(tokens.isNotEmpty()) { "tokens must not be empty" }

        val lineItemSnippet = findLineItemSnippet(document, tokens)
        val transportSnippet = findTransportSnippet(document, tokens)
        val bestSnippet = lineItemSnippet ?: transportSnippet

        val lowerDocumentNumber = documentNumber.lowercase()
        if (tokens.any { lowerDocumentNumber.contains(it) }) {
            return DocumentMatchOutcome(priority = 0, matchKind = DocumentMatchKind.DOCUMENT_NUMBER, snippet = bestSnippet)
        }

        val partyFields = listOf(
            customerName,
            document.parties.billedTo.name,
            document.parties.billedTo.gstin,
            document.parties.shippedTo.name,
            document.parties.shippedTo.gstin
        ).map { it.lowercase() }
        if (tokens.any { token -> partyFields.any { it.contains(token) } }) {
            return DocumentMatchOutcome(priority = 1, matchKind = DocumentMatchKind.CUSTOMER, snippet = bestSnippet)
        }

        if (lineItemSnippet != null) {
            return DocumentMatchOutcome(priority = 2, matchKind = DocumentMatchKind.LINE_ITEM, snippet = lineItemSnippet)
        }

        if (transportSnippet != null) {
            return DocumentMatchOutcome(priority = 3, matchKind = DocumentMatchKind.TRANSPORT, snippet = transportSnippet)
        }

        return DocumentMatchOutcome(priority = 4, matchKind = DocumentMatchKind.OTHER, snippet = null)
    }

    /** First line item (in document order) where any token matches its description or HSN code. */
    private fun findLineItemSnippet(document: InvoiceDocumentModel, tokens: List<String>): SnippetMatch? {
        for (item in document.lineItems) {
            for (token in tokens) {
                (buildSnippet(item.description, token) ?: buildSnippet(item.hsnCode, token))?.let { return it }
            }
        }
        return null
    }

    /** First transport field where any token matches. */
    private fun findTransportSnippet(document: InvoiceDocumentModel, tokens: List<String>): SnippetMatch? {
        val logistics = document.logistics ?: return null
        val fields = listOfNotNull(
            logistics.transporterName,
            logistics.vehicleNumber,
            logistics.grOrLrNumber,
            logistics.ewayBillNumber
        )
        for (field in fields) {
            for (token in tokens) {
                buildSnippet(field, token)?.let { return it }
            }
        }
        return null
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
