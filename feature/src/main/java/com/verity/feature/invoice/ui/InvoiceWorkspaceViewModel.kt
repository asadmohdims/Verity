package com.verity.feature.invoice.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verity.feature.invoice.autocomplete.CustomerAutocompleteDataSource
import com.verity.feature.invoice.autocomplete.CustomerAutocompleteItem
import com.verity.invoice.draft.DraftAddress
import com.verity.invoice.draft.InvoiceDraftStore
import com.verity.invoice.draft.InvoiceDraftUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * InvoiceWorkspaceViewModel
 *
 * Screen-level controller for the Invoice Workspace.
 *
 * Responsibilities:
 * - Own the in-memory invoice draft via InvoiceDraftStore
 * - Expose immutable UI state for observation
 * - Accept UI intents and delegate mutations to the draft store
 *
 * Non-responsibilities:
 * - No UI logic
 * - No persistence
 * - No PDF generation
 * - No navigation
 */
class InvoiceWorkspaceViewModel(
    private val draftStore: InvoiceDraftStore,
    private val customerAutocompleteDataSource: CustomerAutocompleteDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(draftStore.currentDraft)
    val uiState: StateFlow<InvoiceDraftUiState> = _uiState.asStateFlow()

    // ------------------------------------------------------------
    // Atom 1 — Billed To (read-only autocomplete)
    // ------------------------------------------------------------

    private val _recentCustomers = MutableStateFlow<List<CustomerAutocompleteItem>>(emptyList())
    val recentCustomers: StateFlow<List<CustomerAutocompleteItem>> = _recentCustomers.asStateFlow()

    private val _searchResults = MutableStateFlow<List<CustomerAutocompleteItem>>(emptyList())
    val searchResults: StateFlow<List<CustomerAutocompleteItem>> = _searchResults.asStateFlow()

    // ------------------------------------------------------------
    // Atom 1 — Billed To
    // ------------------------------------------------------------

    fun onBilledToFocused() {
        viewModelScope.launch {
            _recentCustomers.value = customerAutocompleteDataSource.recentCustomers()
        }
    }

    fun onBilledToQueryChanged(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            _searchResults.value = customerAutocompleteDataSource.searchCustomers(query)
        }
    }

    fun onBilledToSelected(address: DraftAddress) {
        draftStore.setBilledTo(address)
        _uiState.value = draftStore.currentDraft
    }

    fun onBilledToCleared() {
        draftStore.clearBilledTo()
        _uiState.value = draftStore.currentDraft
    }
}