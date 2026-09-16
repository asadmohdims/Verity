package com.verity.core.document.search

import com.verity.core.document.model.InvoiceDocumentModel

/**
 * Flattens every field worth matching a search query against into one lowercase, space-joined
 * string, computed once at finalize time and stored alongside the immutable DocumentEntity row
 * (documents are insert-only — this never needs recomputing).
 *
 * Kept deliberately as a plain substring-searchable blob rather than per-field columns: the
 * matched-field detection needed for search result snippets re-decodes the JSON payload for the
 * (small) set of rows this text already narrowed down, so this string only has to answer "does
 * anything in this document mention X", not "which field".
 */
fun buildSearchIndexText(document: InvoiceDocumentModel): String {
    val parts = buildList {
        add(document.identity.documentNumber)
        add(document.identity.placeOfSupplyState)

        add(document.parties.billedTo.name)
        add(document.parties.billedTo.gstin)
        addAll(document.parties.billedTo.addressLines)
        add(document.parties.shippedTo.name)
        add(document.parties.shippedTo.gstin)
        addAll(document.parties.shippedTo.addressLines)

        document.lineItems.forEach { item ->
            add(item.description)
            add(item.hsnCode)
        }

        document.logistics?.let { logistics ->
            logistics.transporterName?.let { add(it) }
            logistics.vehicleNumber?.let { add(it) }
            logistics.grOrLrNumber?.let { add(it) }
            logistics.ewayBillNumber?.let { add(it) }
            logistics.notes?.let { add(it) }
        }

        document.inboundChallanReference?.let { add(it.challanNumber) }
        document.jobWorkLink?.let { add(it.linkedDocumentNumber) }

        // Plain rupee string (no grouping/decimals) so typing an amount like "45000" matches —
        // deliberately not Money.format()'s grouped "45,000" form, which would make substring
        // matching depend on where the user's cursor lands relative to a comma.
        add((document.totals.grandTotalPaise / 100).toString())
    }

    return parts
        .filter { it.isNotBlank() }
        .joinToString(separator = " ") { it.trim() }
        .lowercase()
}
