package com.verity.platform.database.seed

import com.verity.platform.database.entities.CustomerEntity
import java.util.UUID

/**
 * Maps a fixture seed record into a real, persistable CustomerEntity.
 * Seed data is an execution aid for alpha/dev, not domain truth.
 */
fun CustomerSeedDto.toEntity(): CustomerEntity =
    CustomerEntity(
        customerId = UUID.randomUUID().toString(),
        customerName = customerName,
        phone = null,
        gstin = gstin,
        addressLine1 = addressLine1,
        city = city,
        state = state,
        stateCode = stateCode,
        pincode = pincode,
        isActive = true,
        updatedAt = System.currentTimeMillis()
    )
