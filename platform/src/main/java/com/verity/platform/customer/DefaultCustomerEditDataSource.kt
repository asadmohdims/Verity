package com.verity.platform.customer

import com.verity.feature.customer.edit.CustomerEditDataSource
import com.verity.feature.customer.edit.CustomerFormData
import com.verity.platform.database.PlatformDatabase
import com.verity.platform.database.entities.CustomerEntity

/**
 * DefaultCustomerEditDataSource
 *
 * platform's implementation of CustomerEditDataSource. save() always writes isActive = true —
 * the only way to deactivate is the dedicated deactivate() call (CustomerDao.deactivate), never a
 * side effect of an unrelated field edit.
 */
class DefaultCustomerEditDataSource(
    private val database: PlatformDatabase
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
            updatedAtEpochMillis = entity.updatedAt
        )
    }

    override suspend fun save(form: CustomerFormData) {
        database.customerDao().insert(
            CustomerEntity(
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
                updatedAt = form.updatedAtEpochMillis
            )
        )
    }

    override suspend fun deactivate(customerId: String) {
        database.customerDao().deactivate(customerId)
    }
}
