package com.verity.platform.database.seed

/**
 * CustomerSeedDto
 *
 * PURPOSE
 * -------
 * Represents a single customer record loaded from
 * the JSON seed file during development / alpha.
 *
 * NOTES
 * -----
 * - This is NOT a database entity
 * - This is NOT a domain model
 * - This exists only at import time
 */
data class CustomerSeedDto(
    val customerName: String,
    val gstin: String,
    val addressLine1: String,
    val city: String,
    val state: String,
    val stateCode: String,
    val pincode: String?
)