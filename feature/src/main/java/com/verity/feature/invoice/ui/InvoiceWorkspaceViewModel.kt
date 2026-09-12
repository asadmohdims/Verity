package com.verity.feature.invoice.ui

import com.verity.core.document.model.InvoiceDocumentModel
import com.verity.core.document.model.SellerDetails
import com.verity.feature.invoice.projection.DraftToInvoiceDocument
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock

import com.verity.core.ui.chrome.WorkspaceChromeSpec
import com.verity.core.ui.molecules.VerityChromeMode
import com.verity.core.ui.molecules.VerityNavIcon
import com.verity.core.ui.molecules.VerityTopBarAction
import com.verity.core.ui.icons.VerityIcons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verity.feature.invoice.autocomplete.CustomerAutocompleteDataSource
import com.verity.feature.invoice.autocomplete.CustomerAutocompleteItem
import com.verity.feature.invoice.finalize.InvoiceFinalizer
import com.verity.feature.invoice.draft.DraftAddress
import com.verity.feature.invoice.draft.DraftLineItem
import com.verity.feature.invoice.draft.DraftTransportDetails
import com.verity.feature.invoice.draft.DraftDocumentType
import com.verity.feature.invoice.draft.InvoiceDraftStore
import com.verity.feature.invoice.draft.InvoiceDraftUiState
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
    private val customerAutocompleteDataSource: CustomerAutocompleteDataSource,
    private val invoiceFinalizer: InvoiceFinalizer
) : ViewModel() {

    private val _uiState = MutableStateFlow(draftStore.currentDraft)
    val uiState: StateFlow<InvoiceDraftUiState> = _uiState.asStateFlow()

    private val _hasActiveDraft = MutableStateFlow(false)
    val hasActiveDraft: StateFlow<Boolean> = _hasActiveDraft.asStateFlow()

    private val _isFinalizing = MutableStateFlow(false)
    val isFinalizing: StateFlow<Boolean> = _isFinalizing.asStateFlow()

    private val _finalizedDocument = MutableStateFlow<InvoiceDocumentModel?>(null)
    val finalizedDocument: StateFlow<InvoiceDocumentModel?> = _finalizedDocument.asStateFlow()

    // ------------------------------------------------------------
    // Preview (D2) — Draft → Document projection
    // ------------------------------------------------------------

    val previewDocument: StateFlow<InvoiceDocumentModel?> =
        uiState
            .map { draft ->
                if (draft.billedTo == null) {
                    null
                } else {
                    DraftToInvoiceDocument.project(
                        draft = draft,
                        documentNumber = "PREVIEW",
                        seller = SellerDetails(
                            name = "Preview Seller",
                            gstin = null,
                            addressLine1 = "",
                            addressLine2 = null,
                            city = "",
                            state = "",
                            stateCode = "",
                            pincode = ""
                        ),
                        clock = Clock.systemDefaultZone()
                    )
                }
            }
            .stateIn(
                // Eagerly, not WhileSubscribed: the chrome's Preview action checks
                // `.value` directly (not via collection) to decide whether to navigate, before
                // the "preview" route (the only place that ever collects this flow) exists to
                // start it. WhileSubscribed left `.value` stuck at null forever on a fresh
                // session — nothing had ever subscribed yet, so it never computed.
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = null
            )

    /**
     * Workspace chrome specification.
     *
     * This exposes the *semantic intent* for application chrome
     * when the Invoice Workspace is active.
     *
     * Responsibilities:
     * - Declare title, subtitle, navigation affordance, and actions
     * - Reflect workspace state (draft, search, etc.) over time
     *
     * Non-responsibilities:
     * - Does not execute navigation
     * - Does not render UI
     * - Does not know about Scaffold or VerityTopAppBar
     *
     * Ownership:
     * - Produced here (workspace domain)
     * - Consumed by root UI frame (MainActivity / NavHost)
     */
    private val _chromeSpec = MutableStateFlow(
        WorkspaceChromeSpec(
            title = "Verity",
            subtitle = null,
            navigationIcon = VerityNavIcon.None,
            actions = listOf(
                VerityTopBarAction.Icon(
                    icon = VerityIcons.Preview,
                    contentDescription = "Preview invoice",
                    onClick = { /* handled at root */ }
                ),
                VerityTopBarAction.Icon(
                    icon = VerityIcons.Search,
                    contentDescription = "Search",
                    onClick = { /* handled at root */ }
                )
            ),
            chromeMode = VerityChromeMode.Workspace
        )
    )

    val chromeSpec: StateFlow<WorkspaceChromeSpec> = _chromeSpec.asStateFlow()

    fun onCreateInvoice() {
        // Clear whatever the previous invoice left behind: its draft data (deferred here from
        // finalize, not reset there - see onFinalizeInvoice) and its finalize result, so this
        // is a genuinely fresh draft and the next Preview visit doesn't navigate straight to
        // "finalized" for a document that belongs to the previous invoice.
        draftStore.reset()
        _finalizedDocument.value = null
        _hasActiveDraft.value = true
        _uiState.value = draftStore.currentDraft

        _chromeSpec.value = WorkspaceChromeSpec(
            title = "Invoice",
            subtitle = "Draft",
            navigationIcon = VerityNavIcon.Back(
                onClick = { /* handled at root */ },
                contentDescription = "Back"
            ),
            actions = listOf(
                VerityTopBarAction.Icon(
                    icon = VerityIcons.Preview,
                    contentDescription = "Preview invoice",
                    onClick = { /* handled at root */ }
                ),
                VerityTopBarAction.Icon(
                    icon = VerityIcons.Search,
                    contentDescription = "Search",
                    onClick = { /* handled at root */ }
                )
            )
        )
    }

    fun onDiscardDraft() {
        // Previously left the old draft's data sitting in the store — the next "Create Invoice"
        // would resurrect it. reset() actually clears it.
        draftStore.reset()
        _hasActiveDraft.value = false
        _uiState.value = draftStore.currentDraft
        _chromeSpec.value = emptyWorkspaceChromeSpec()
    }

    private fun emptyWorkspaceChromeSpec(): WorkspaceChromeSpec =
        WorkspaceChromeSpec(
            title = "Verity",
            subtitle = null,
            navigationIcon = VerityNavIcon.None,
            actions = listOf(
                VerityTopBarAction.Icon(
                    icon = VerityIcons.Search,
                    contentDescription = "Search",
                    onClick = { /* handled at root */ }
                )
            ),
            chromeMode = VerityChromeMode.Workspace
        )

    /**
     * Finalizes the current draft into a permanent, numbered document.
     *
     * Guards against double-tap (two rapid taps would otherwise create two real documents with
     * consecutive numbers — nothing at the DB layer stops that on its own). Requires a billed-to
     * customer with a resolved customerId (i.e. selected via autocomplete, not hand-typed).
     */
    fun onFinalizeInvoice() {
        if (_isFinalizing.value) return

        val customerId = draftStore.currentDraft.billedTo?.customerId
        requireNotNull(customerId) { "Cannot finalize without a selected billed-to customer" }

        viewModelScope.launch {
            _isFinalizing.value = true
            val document = invoiceFinalizer.finalize(draftStore.currentDraft, customerId)
            _finalizedDocument.value = document

            // Deliberately NOT resetting draftStore/_uiState here: the "preview" route this
            // finalize was triggered from is still composed (its LaunchedEffect hasn't yet
            // navigated to "finalized") and requires previewDocument to stay non-null until it
            // does. Resetting here raced the navigation and crashed with "Preview route entered
            // without an active draft" - the draft's billedTo went null (via reset) in the same
            // recomposition pass that was supposed to navigate away first. The draft is instead
            // cleared in onCreateInvoice(), when the next invoice actually starts.
            _hasActiveDraft.value = false
            _isFinalizing.value = false

            // Safe to touch chrome here (unlike draftStore/_uiState above): it doesn't feed
            // previewDocument, so it can't race the "preview" -> "finalized" navigation.
            _chromeSpec.value = emptyWorkspaceChromeSpec()
        }
    }

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
            gstin = item.gstin,
            addressLine1 = item.addressLine1,
            city = item.city,
            state = item.state,
            stateCode = item.stateCode,
            pincode = item.pincode,
            customerId = item.customerId
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
            gstin = item.gstin,
            addressLine1 = item.addressLine1,
            city = item.city,
            state = item.state,
            stateCode = item.stateCode,
            pincode = item.pincode,
            customerId = item.customerId
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

    fun onInsertLineItemAt(index: Int, item: DraftLineItem) {
        draftStore.insertLineItemAt(index, item)
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