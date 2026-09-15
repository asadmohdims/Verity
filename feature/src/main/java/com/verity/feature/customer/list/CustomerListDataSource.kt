package com.verity.feature.customer.list

import com.verity.core.formatting.money.Money

/**
 * CustomerListDataSource
 *
 * Port for the Customers tab. Active customers only (CustomerDao.getActiveCustomers already
 * filters this) — a deactivated customer stays reachable via Customer Detail (search, or a
 * document's own customer link) but drops out of this list, same rule CustomerDao's own doc
 * comment states.
 */
interface CustomerListDataSource {
    suspend fun loadCustomers(): List<CustomerListItem>
}

data class CustomerListItem(
    val customerId: String,
    val customerName: String,
    val city: String,
    val state: String,
    val balanceDue: Money
) {
    val isSettled: Boolean get() = balanceDue.raw == 0L
}
