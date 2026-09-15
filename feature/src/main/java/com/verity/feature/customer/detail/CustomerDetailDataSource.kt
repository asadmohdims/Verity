package com.verity.feature.customer.detail

import com.verity.core.formatting.money.Money
import com.verity.feature.home.DocumentSummary

/**
 * CustomerDetailDataSource
 *
 * Port for the Customer Detail screen — reached both from a Documents search customer match and
 * from the Customers List. Profile fields (phone/GSTIN/address) plus the actual outstanding
 * balance (from the ledger, not a sum of document totals — see CustomerDetail's balanceDue doc
 * comment) alongside the document history this screen used to show alone as the read-only
 * "rollup".
 */
interface CustomerDetailDataSource {
    suspend fun loadDetail(customerId: String): CustomerDetail?
}

data class CustomerDetail(
    val customerId: String,
    val customerName: String,
    val phone: String?,
    val gstin: String,
    val addressLine1: String,
    val city: String,
    val state: String,
    val stateCode: String,
    val pincode: String?,
    /**
     * SUM(ledger_entries) for this customer — the real outstanding amount, per CLAUDE.md's Data &
     * sync architecture. Deliberately not documents.sumOf { grandTotal } (the old rollup's
     * runningTotal): that would double-count a Challan's goods value, which never creates a ledger
     * entry because a Challan isn't a billing event.
     */
    val balanceDue: Money,
    val documents: List<DocumentSummary>
) {
    val documentCount: Int get() = documents.size
}
