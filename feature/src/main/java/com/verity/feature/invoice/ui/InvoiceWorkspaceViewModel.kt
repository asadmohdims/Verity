package com.verity.feature.invoice.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verity.feature.invoice.autocomplete.CustomerAutocompleteDataSource
import com.verity.feature.invoice.autocomplete.CustomerAutocompleteItem
import com.verity.invoice.draft.DraftAddress
import com.verity.invoice.draft.DraftLineItem
import com.verity.invoice.draft.DraftTransportDetails
import com.verity.invoice.draft.DraftDocumentType
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

    // Autocomplete UI state (Atom 1 contract)
    private val _billedToQuery = MutableStateFlow("")
    val billedToQuery: StateFlow<String> = _billedToQuery.asStateFlow()

    private val _billedToSuggestions =
        MutableStateFlow<List<CustomerAutocompleteItem>>(emptyList())
    val billedToSuggestions: StateFlow<List<CustomerAutocompleteItem>> =
        _billedToSuggestions.asStateFlow()

    private val _isBilledToSearching = MutableStateFlow(false)
    val isBilledToSearching: StateFlow<Boolean> =
        _isBilledToSearching.asStateFlow()

    private val _shippedToQuery = MutableStateFlow("")
    val shippedToQuery: StateFlow<String> = _shippedToQuery.asStateFlow()

    private val _shippedToSuggestions =
        MutableStateFlow<List<CustomerAutocompleteItem>>(emptyList())
    val shippedToSuggestions: StateFlow<List<CustomerAutocompleteItem>> =
        _shippedToSuggestions.asStateFlow()

    private val _isShippedToSearching = MutableStateFlow(false)
    val isShippedToSearching: StateFlow<Boolean> =
        _isShippedToSearching.asStateFlow()

    // ------------------------------------------------------------
    // Atom 1 — Billed To (read-only autocomplete)
    // ------------------------------------------------------------

    // ------------------------------------------------------------
    // Atom 1 — Billed To
    // ------------------------------------------------------------

    fun onBilledToQueryChanged(query: String) {
        _billedToQuery.value = query

        if (query.isBlank()) {
            _billedToSuggestions.value = emptyList()
            _isBilledToSearching.value = false
            return
        }

        viewModelScope.launch {
            _isBilledToSearching.value = true
            _billedToSuggestions.value =
                customerAutocompleteDataSource.searchCustomers(query)
            _isBilledToSearching.value = false
        }
    }

    fun onBilledToSelected(address: DraftAddress) {
        draftStore.setBilledTo(address)
        _uiState.value = draftStore.currentDraft

        // Clear autocomplete UI state after selection
        _billedToQuery.value = ""
        _billedToSuggestions.value = emptyList()
        _isBilledToSearching.value = false
    }

    /**
     * Adapter for autocomplete selection.
     *
     * Maps read-model (CustomerAutocompleteItem) to draft-model (DraftAddress)
     * at the ViewModel boundary, then delegates to the existing mutation path.
     */
    fun onBilledToSelected(item: CustomerAutocompleteItem) {
        val address = DraftAddress(
            name = item.customerName,
            addressLine1 = "",
            city = item.city ?: "",
            state = item.state ?: "",
            stateCode = item.stateCode ?: "",
            pincode = ""
        )

        onBilledToSelected(address)
    }

    fun onBilledToCleared() {
        draftStore.clearBilledTo()
        _uiState.value = draftStore.currentDraft

        // Reset autocomplete UI state
        _billedToQuery.value = ""
        _billedToSuggestions.value = emptyList()
        _isBilledToSearching.value = false
    }

    // ------------------------------------------------------------
    // Atom 2 — Shipped To (override autocomplete)
    // ------------------------------------------------------------

    fun onShippedToQueryChanged(query: String) {
        _shippedToQuery.value = query

        if (query.isBlank()) {
            _shippedToSuggestions.value = emptyList()
            _isShippedToSearching.value = false
            return
        }

        viewModelScope.launch {
            _isShippedToSearching.value = true
            _shippedToSuggestions.value =
                customerAutocompleteDataSource.searchCustomers(query)
            _isShippedToSearching.value = false
        }
    }

    fun onShippedToSelected(address: DraftAddress) {
        draftStore.setShippedToOverride(address)
        _uiState.value = draftStore.currentDraft

        _shippedToQuery.value = ""
        _shippedToSuggestions.value = emptyList()
        _isShippedToSearching.value = false
    }

    fun onShippedToSelected(item: CustomerAutocompleteItem) {
        val address = DraftAddress(
            name = item.customerName,
            addressLine1 = "",
            city = item.city ?: "",
            state = item.state ?: "",
            stateCode = item.stateCode ?: "",
            pincode = ""
        )

        onShippedToSelected(address)
    }

    fun onShippedToCleared() {
        draftStore.clearShippedToOverride()
        _uiState.value = draftStore.currentDraft

        _shippedToQuery.value = ""
        _shippedToSuggestions.value = emptyList()
        _isShippedToSearching.value = false
    }

    // ------------------------------------------------------------
    // Atom 3 — Line Items
    // ------------------------------------------------------------

    fun onAddLineItem(item: DraftLineItem) {
        draftStore.addLineItem(item)
        _uiState.value = draftStore.currentDraft
    }

    // ------------------------------------------------------------
    // Atom 4 — Line Items (Remove)
    // ------------------------------------------------------------

    fun onRemoveLineItem(index: Int) {
        draftStore.removeLineItem(index)
        _uiState.value = draftStore.currentDraft
    }

    // ------------------------------------------------------------
    // Atom 4 — Line Items (Update)
    // ------------------------------------------------------------

    fun onUpdateLineItem(
        index: Int,
        item: DraftLineItem
    ) {
        draftStore.updateLineItem(index, item)
        _uiState.value = draftStore.currentDraft
    }

    // ------------------------------------------------------------
    // Atom 5 — Transportation Mode
    // ------------------------------------------------------------

    fun onTransportDetailsChanged(details: DraftTransportDetails?) {
        draftStore.setTransportDetails(details)
        _uiState.value = draftStore.currentDraft
    }

    // ------------------------------------------------------------
    // Atom 6.1 — Document Type
    // ------------------------------------------------------------

    fun onDocumentTypeChanged(documentType: DraftDocumentType) {
        draftStore.setDocumentType(documentType)
        _uiState.value = draftStore.currentDraft
    }
}