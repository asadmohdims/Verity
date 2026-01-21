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
 * CustomerAutocompleteItem
 *
 * Invoice-ready, authoritative customer snapshot.
 *
 * IMPORTANT INVARIANTS
 * --------------------
 * • Selecting a customer MUST yield all information required to:
 *   - Render Invoice Preview
 *   - Finalize an Invoice without additional fetches
 *
 * • All fields here are expected to be populated from
 *   authoritative Customer CRUD storage.
 *
 * • Missing or blank fields represent a DATA INTEGRITY BUG,
 *   not a workflow or UI state.
 *
 * NON-GOALS
 * ---------
 * • This is NOT a domain entity
 * • This is NOT persisted
 * • This is NOT editable in the Invoice Workspace
 */
data class CustomerAutocompleteItem(
    val customerId: String,
    val customerName: String,

    // Tax identity (mandatory for invoice)
    val gstin: String,

    // Address (invoice-relevant)
    val addressLine1: String,
    val city: String,
    val state: String,

    // GST state code as 2-digit numeric string (e.g. "27")
    val stateCode: String
)