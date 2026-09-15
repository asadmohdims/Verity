package com.verity.feature.customer.edit

/**
 * CustomerEditDataSource
 *
 * Port for the Add/Edit Customer form. `save` covers both create and edit-by-id (CustomerDao's
 * own insert is REPLACE-on-conflict — see CLAUDE.md's UX direction section) so there is
 * deliberately no separate create()/update() split here.
 */
interface CustomerEditDataSource {
    suspend fun loadCustomer(customerId: String): CustomerFormData?
    suspend fun save(form: CustomerFormData)
    suspend fun deactivate(customerId: String)
}

data class CustomerFormData(
    val customerId: String,
    val customerName: String,
    val gstin: String,
    val phone: String?,
    val addressLine1: String,
    val city: String,
    val state: String,
    val stateCode: String,
    val pincode: String?,
    val updatedAtEpochMillis: Long
)
