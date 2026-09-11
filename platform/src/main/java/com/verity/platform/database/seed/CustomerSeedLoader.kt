package com.verity.platform.database.seed

import android.content.Context
import kotlinx.serialization.json.Json

/**
 * CustomerSeedLoader
 *
 * PURPOSE
 * -------
 * Loads customer seed data from assets/customers.json
 * and parses it into CustomerSeedDto objects.
 *
 * CONSTRAINTS
 * -----------
 * - Development / alpha only
 * - No database access
 * - No side effects
 */
object CustomerSeedLoader {

    private val json = Json { ignoreUnknownKeys = true }

    fun load(context: Context): List<CustomerSeedDto> {
        val text = context.assets.open("customers.json").bufferedReader().use { it.readText() }
        return json.decodeFromString<List<CustomerSeedDto>>(text)
    }
}