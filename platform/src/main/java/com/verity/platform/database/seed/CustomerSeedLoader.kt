package com.verity.platform.database.seed

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

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

    fun load(context: Context): List<CustomerSeedDto> {
        val assetManager = context.assets

        assetManager.open("customers.json").use { inputStream ->
            InputStreamReader(inputStream).use { reader ->
                val listType = object : TypeToken<List<CustomerSeedDto>>() {}.type
                return Gson().fromJson(reader, listType)
            }
        }
    }
}