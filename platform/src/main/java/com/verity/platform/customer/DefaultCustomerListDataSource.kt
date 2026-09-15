package com.verity.platform.customer

import com.verity.core.formatting.money.Money
import com.verity.feature.customer.list.CustomerListDataSource
import com.verity.feature.customer.list.CustomerListItem
import com.verity.platform.database.PlatformDatabase
import com.verity.platform.database.entities.CustomerEntity
import com.verity.platform.finalize.DEFAULT_ORG_ID

/**
 * DefaultCustomerListDataSource
 *
 * platform's implementation of CustomerListDataSource. One balance query per active customer —
 * fine at this app's realistic single-business volume (a few dozen to low hundreds of customers),
 * same reasoning DocumentDao's search query uses for a plain scan over an indexed lookup.
 */
class DefaultCustomerListDataSource(
    private val database: PlatformDatabase
) : CustomerListDataSource {

    override suspend fun loadCustomers(): List<CustomerListItem> =
        database.customerDao().getActiveCustomers().map { it.toListItem() }

    private suspend fun CustomerEntity.toListItem(): CustomerListItem {
        val balancePaise = database.ledgerEntryDao()
            .getBalanceForCustomer(orgId = DEFAULT_ORG_ID, customerId = customerId) ?: 0L

        return CustomerListItem(
            customerId = customerId,
            customerName = customerName,
            city = city,
            state = state,
            balanceDue = Money.ofPaise(balancePaise)
        )
    }
}
