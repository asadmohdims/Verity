package com.verity.platform.customer

import com.verity.feature.customer.edit.CustomerEditDataSource
import com.verity.feature.customer.edit.CustomerFormData
import com.verity.platform.database.PlatformDatabase
import com.verity.platform.database.entities.CustomerEntity
import com.verity.platform.sync.FirebaseSyncClient

/**
 * DefaultCustomerEditDataSource
 *
 * platform's implementation of CustomerEditDataSource. save() always writes isActive = true —
 * the only way to deactivate is the dedicated deactivate() call (CustomerDao.deactivate), never a
 * side effect of an unrelated field edit. Both save() and deactivate() push the resulting row to
 * Firestore (fire-and-forget, same pattern as DefaultInvoiceFinalizer) — see CLAUDE.md's Data &
 * sync architecture and the customerId-divergence fix this was added for.
 */
class DefaultCustomerEditDataSource(
    private val database: PlatformDatabase,
    private val syncClient: FirebaseSyncClient
) : CustomerEditDataSource {

    override suspend fun loadCustomer(customerId: String): CustomerFormData? {
        val entity = database.customerDao().getById(customerId) ?: return null
        return CustomerFormData(
            customerId = entity.customerId,
            customerName = entity.customerName,
            gstin = entity.gstin,
            phone = entity.phone,
            addressLine1 = entity.addressLine1,
            city = entity.city,
            state = entity.state,
            stateCode = entity.stateCode,
            pincode = entity.pincode,
            updatedAtEpochMillis = entity.updatedAt,
            notes = entity.notes
        )
    }

    override suspend fun save(form: CustomerFormData) {
        val entity = CustomerEntity(
            customerId = form.customerId,
            customerName = form.customerName,
            phone = form.phone,
            gstin = form.gstin,
            addressLine1 = form.addressLine1,
            city = form.city,
            state = form.state,
            stateCode = form.stateCode,
            pincode = form.pincode,
            isActive = true,
            updatedAt = form.updatedAtEpochMillis,
            notes = form.notes
        )
        database.customerDao().insert(entity)
        syncClient.pushCustomer(entity)
    }

    override suspend fun deactivate(customerId: String) {
        database.customerDao().deactivate(customerId)
        database.customerDao().getById(customerId)?.let { syncClient.pushCustomer(it) }
    }
}
