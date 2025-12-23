package com.verity.platform.autocomplete

import com.verity.feature.invoice.autocomplete.CustomerAutocompleteDataSource
import com.verity.feature.invoice.autocomplete.CustomerAutocompleteItem
import com.verity.platform.database.dao.CustomerDao
import com.verity.platform.database.entities.CustomerEntity

/**
 * DefaultCustomerAutocompleteDataSource
 *
 * Platform-owned implementation of CustomerAutocompleteDataSource.
 *
 * Reads from CRUD-managed customer storage via CustomerDao and performs
 * in-memory filtering for autocomplete use-cases.
 */
class DefaultCustomerAutocompleteDataSource(
    private val customerDao: CustomerDao
) : CustomerAutocompleteDataSource {

    override suspend fun recentCustomers(limit: Int): List<CustomerAutocompleteItem> {
        return customerDao
            .getActiveCustomers()
            .take(limit)
            .map { it.toAutocompleteItem() }
    }

    override suspend fun searchCustomers(
        query: String,
        limit: Int
    ): List<CustomerAutocompleteItem> {
        val normalizedQuery = query.trim().lowercase()

        if (normalizedQuery.isEmpty()) {
            return recentCustomers(limit)
        }

        return customerDao
            .getActiveCustomers()
            .asSequence()
            .filter { customer ->
                customer.customerName.lowercase().contains(normalizedQuery) ||
                        (customer.gstin?.lowercase()?.contains(normalizedQuery) ?: false) ||
                        (customer.city?.lowercase()?.contains(normalizedQuery) ?: false) ||
                        (customer.state?.lowercase()?.contains(normalizedQuery) ?: false)
            }
            .take(limit)
            .map { it.toAutocompleteItem() }
            .toList()
    }

    private fun CustomerEntity.toAutocompleteItem(): CustomerAutocompleteItem {
        return CustomerAutocompleteItem(
            customerId = customerId,
            customerName = customerName,
            gstin = gstin,
            city = city,
            state = state
        )
    }
}