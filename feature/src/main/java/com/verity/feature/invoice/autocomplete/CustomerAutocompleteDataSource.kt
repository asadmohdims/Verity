package com.verity.feature.invoice.autocomplete

/**
 * CustomerAutocompleteDataSource
 *
 * Read-only data source for customer autocomplete backed by
 * CRUD-managed customer identity data.
 *
 * Responsibilities:
 * - Provide recently used customers for empty-query focus
 * - Provide query-based customer search results
 * - Read from authoritative Customer CRUD storage
 *
 * Non-responsibilities:
 * - No draft mutation
 * - No UI state
 * - No caching
 * - No persistence
 * - No event sourcing or projection logic
 */
interface CustomerAutocompleteDataSource {

    /**
     * Returns recently used customers in deterministic order.
     * Used when the autocomplete field is focused with an empty query.
     */
    suspend fun recentCustomers(
        limit: Int = 10
    ): List<CustomerAutocompleteItem>

    /**
     * Returns customers matching the given query.
     */
    suspend fun searchCustomers(
        query: String,
        limit: Int = 20
    ): List<CustomerAutocompleteItem>
}

/**
 * Lightweight read-model for customer autocomplete rows.
 *
 * This model is derived from CustomerEntity and is optimized
 * for fast, read-only display in autocomplete UIs.
 *
 * It must not be treated as a domain or persistence model.
 */
data class CustomerAutocompleteItem(
    val customerId: String,
    val customerName: String,
    val gstin: String?,
    val city: String?,
    val state: String?,
    // GST state code as 2-digit numeric string (e.g. "27")
    val stateCode: String?
)