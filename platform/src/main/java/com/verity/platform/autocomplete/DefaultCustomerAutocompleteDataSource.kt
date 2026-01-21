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

    /**
     * Maps a CustomerEntity to an invoice-ready CustomerAutocompleteItem.
     *
     * This is a GUARANTEE boundary.
     * Missing data here indicates a Customer data integrity bug.
     */
    private fun CustomerEntity.toAutocompleteItem(): CustomerAutocompleteItem {

        require(customerName.isNotBlank()) {
            "CustomerEntity.customerName must not be blank"
        }

        require(!gstin.isNullOrBlank()) {
            "CustomerEntity.gstin must be present for invoice generation"
        }

        require(!addressLine1.isNullOrBlank()) {
            "CustomerEntity.addressLine1 must be present for invoice generation"
        }

        require(!city.isNullOrBlank()) {
            "CustomerEntity.city must be present for invoice generation"
        }

        require(!state.isNullOrBlank()) {
            "CustomerEntity.state must be present for invoice generation"
        }

        require(!stateCode.isNullOrBlank()) {
            "CustomerEntity.stateCode must be present for invoice generation"
        }

        return CustomerAutocompleteItem(
            customerId = customerId,
            customerName = customerName,
            gstin = gstin!!,
            addressLine1 = addressLine1!!,
            city = city!!,
            state = state!!,
            stateCode = stateCode!!
        )
    }
}