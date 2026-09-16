package com.verity.feature.invoice.ui

import com.verity.core.document.model.InvoiceDocumentModel
import com.verity.core.document.model.SellerDetails
import com.verity.feature.invoice.projection.DraftToInvoiceDocument
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

import com.verity.core.ui.chrome.WorkspaceChromeSpec
import com.verity.core.ui.molecules.VerityChromeMode
import com.verity.core.ui.molecules.VerityNavIcon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verity.feature.invoice.autocomplete.CustomerAutocompleteDataSource
import com.verity.feature.invoice.autocomplete.CustomerAutocompleteItem
import com.verity.feature.invoice.finalize.DocumentNumberPreviewDataSource
import com.verity.feature.invoice.finalize.InvoiceFinalizer
import com.verity.feature.invoice.finalize.JobWorkLinkage
import com.verity.feature.invoice.pdf.InvoicePdfRenderer
import com.verity.feature.invoice.draft.DraftAddress
import com.verity.feature.invoice.draft.DraftInboundChallanReference
import com.verity.feature.invoice.draft.DraftJobWorkChallanLink
import com.verity.feature.invoice.draft.DraftLineItem
import com.verity.feature.invoice.draft.DraftTransportDetails
import com.verity.feature.invoice.draft.DraftDocumentType
import com.verity.feature.invoice.draft.InvoiceDraftStore
import com.verity.feature.invoice.draft.InvoiceDraftUiState
import com.verity.feature.referencelist.ReferenceListDataSource
import com.verity.feature.referencelist.ReferenceListKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

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
 * - No PDF drawing logic (delegates to InvoicePdfRenderer; this class only decides *when* to
 *   call it - right after finalize succeeds - not how a PDF is drawn)
 * - No navigation
 */

class InvoiceWorkspaceViewModel(
    private val draftStore: InvoiceDraftStore,
    private val customerAutocompleteDataSource: CustomerAutocompleteDataSource,
    private val invoiceFinalizer: InvoiceFinalizer,
    private val invoicePdfRenderer: InvoicePdfRenderer,
    private val referenceListDataSource: ReferenceListDataSource,
    private val documentNumberPreviewDataSource: DocumentNumberPreviewDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(draftStore.currentDraft)
    val uiState: StateFlow<InvoiceDraftUiState> = _uiState.asStateFlow()

    // Transporter Name / HSN Code / Unit suggestions (Settings-managed reference lists — see
    // ReferenceListDataSource). Loaded once, up front: all three lists are small (a business's
    // own curated values), so there's no need for the per-keystroke query round trip Customer
    // autocomplete uses — the Screen filters this already-loaded list in memory as the user types.
    private val _transporterNameSuggestions = MutableStateFlow<List<String>>(emptyList())
    val transporterNameSuggestions: StateFlow<List<String>> = _transporterNameSuggestions.asStateFlow()

    private val _hsnCodeSuggestions = MutableStateFlow<List<String>>(emptyList())
    val hsnCodeSuggestions: StateFlow<List<String>> = _hsnCodeSuggestions.asStateFlow()

    private val _unitSuggestions = MutableStateFlow<List<String>>(emptyList())
    val unitSuggestions: StateFlow<List<String>> = _unitSuggestions.asStateFlow()

    init {
        refreshReferenceListSuggestions()
    }

    /**
     * Re-loads all three suggestion lists. Also called from onCreateInvoice() — this ViewModel
     * is tab-persistent (constructed once at the composition root, not per-navigation), so a
     * value added in Settings after launch would otherwise never show up here until app restart.
     * Public so AppNavShell's goToWorkspace()/goToWorkspaceForCustomer() can also call this
     * unconditionally, on *every* entry into Workspace — not just a genuinely new draft
     * (onCreateInvoice() only runs there when hasActiveDraft is false). Without that, resuming an
     * in-progress draft after adding a reference-list value while away (e.g. switching to Settings
     * mid-draft to add a missing HSN code, then tapping the FAB to return) left these lists stale
     * for the rest of that draft — found 2026-09-16.
     */
    fun refreshReferenceListSuggestions() {
        viewModelScope.launch {
            _transporterNameSuggestions.value =
                referenceListDataSource.getAll(ReferenceListKind.TRANSPORTER_NAME).map { it.value }
        }
        viewModelScope.launch {
            _hsnCodeSuggestions.value =
                referenceListDataSource.getAll(ReferenceListKind.HSN_CODE).map { it.value }
        }
        viewModelScope.launch {
            _unitSuggestions.value =
                referenceListDataSource.getAll(ReferenceListKind.UNIT).map { it.value }
        }
    }

    private val _hasActiveDraft = MutableStateFlow(false)
    val hasActiveDraft: StateFlow<Boolean> = _hasActiveDraft.asStateFlow()

    private val _isFinalizing = MutableStateFlow(false)
    val isFinalizing: StateFlow<Boolean> = _isFinalizing.asStateFlow()

    private val _finalizedDocument = MutableStateFlow<InvoiceDocumentModel?>(null)
    val finalizedDocument: StateFlow<InvoiceDocumentModel?> = _finalizedDocument.asStateFlow()

    // ------------------------------------------------------------
    // Predicted document number — see DocumentNumberPreviewDataSource. A non-binding "this will
    // likely be numbered X" hint, refreshed whenever the draft's own document type changes
    // (onCreateInvoice/onDocumentTypeChanged). Superseded by draft.jobWorkChallanLink's real
    // reserved number when one exists (see previewDocument below and InvoiceWorkspaceScreen).
    // ------------------------------------------------------------

    private val _predictedDocumentNumber = MutableStateFlow<String?>(null)
    val predictedDocumentNumber: StateFlow<String?> = _predictedDocumentNumber.asStateFlow()

    private fun refreshPredictedDocumentNumber(documentType: DraftDocumentType) {
        viewModelScope.launch {
            _predictedDocumentNumber.value =
                documentNumberPreviewDataSource.peekNextNumber(documentType)
        }
    }

    // ------------------------------------------------------------
    // Preview (D2) — Draft → Document projection
    // ------------------------------------------------------------

    val previewDocument: StateFlow<InvoiceDocumentModel?> =
        combine(uiState, predictedDocumentNumber) { draft, predicted ->
                if (draft.billedTo == null) {
                    null
                } else {
                    DraftToInvoiceDocument.project(
                        draft = draft,
                        // A job-work continuation Invoice already has a real, reserved number —
                        // prefer it over the mere prediction. Otherwise fall back to the
                        // non-binding peek, or an honest placeholder while that peek is still
                        // loading (it's a suspend Room read, not available synchronously).
                        documentNumber = draft.jobWorkChallanLink?.reservedInvoiceNumber
                            ?: predicted
                            ?: "Assigned at finalize",
                        seller = SellerDetails(
                            name = "Preview Seller",
                            gstin = null,
                            addressLine1 = "",
                            addressLine2 = null,
                            city = "",
                            state = "",
                            stateCode = "",
                            pincode = ""
                        )
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
            actions = emptyList(),
            chromeMode = VerityChromeMode.Workspace
        )
    )

    val chromeSpec: StateFlow<WorkspaceChromeSpec> = _chromeSpec.asStateFlow()

    /**
     * Title tracks the actual selected Document Type ("Invoice"/"Challan") rather than a fixed
     * "Invoice" — previously hardcoded, so switching the dropdown to Challan left the top bar
     * saying "Invoice" the whole time. Subtitle stays "Draft" except in the two job-work cases
     * (a Challan drafted with "Challan + Invoice", or the continuation Invoice opened from one),
     * both of which read "Job Work" — matching the "Job Work" section shown on the draft itself.
     */
    private fun buildDraftChromeSpec(draft: InvoiceDraftUiState): WorkspaceChromeSpec {
        val title = when (draft.documentType) {
            DraftDocumentType.INVOICE -> "Invoice"
            DraftDocumentType.CHALLAN -> "Challan"
        }
        val isJobWork = draft.jobWorkChallanLink != null ||
            (draft.documentType == DraftDocumentType.CHALLAN && draft.isJobWorkFlow)

        return WorkspaceChromeSpec(
            title = title,
            subtitle = if (isJobWork) "Job Work" else "Draft",
            navigationIcon = VerityNavIcon.Back(
                onClick = { /* handled at root */ },
                contentDescription = "Back"
            ),
            actions = emptyList()
        )
    }

    fun onCreateInvoice(prefillBilledTo: DraftAddress? = null) {
        // Clear whatever the previous invoice left behind: its draft data (deferred here from
        // finalize, not reset there - see onFinalizeInvoice) and its finalize result, so this
        // is a genuinely fresh draft and the next Preview visit doesn't navigate straight to
        // "finalized" for a document that belongs to the previous invoice.
        draftStore.reset()
        _finalizedDocument.value = null
        _hasActiveDraft.value = true

        // "New Invoice" from a Customer Detail screen arrives with the customer already known -
        // prefill billed-to the same way a manual autocomplete pick does (shipped-to already
        // defaults to billed-to unless overridden, so no separate call is needed for it).
        if (prefillBilledTo != null) {
            draftStore.setBilledTo(prefillBilledTo)
        }
        _uiState.value = draftStore.currentDraft
        refreshReferenceListSuggestions()

        _chromeSpec.value = buildDraftChromeSpec(draftStore.currentDraft)
        refreshPredictedDocumentNumber(draftStore.currentDraft.documentType)
    }

    fun onDiscardDraft() {
        // Previously left the old draft's data sitting in the store — the next "Create Invoice"
        // would resurrect it. reset() actually clears it.
        draftStore.reset()
        _hasActiveDraft.value = false
        _uiState.value = draftStore.currentDraft
        _chromeSpec.value = emptyWorkspaceChromeSpec()
        _predictedDocumentNumber.value = null
    }

    private fun emptyWorkspaceChromeSpec(): WorkspaceChromeSpec =
        WorkspaceChromeSpec(
            title = "Verity",
            subtitle = null,
            navigationIcon = VerityNavIcon.None,
            actions = emptyList(),
            chromeMode = VerityChromeMode.Workspace
        )

    /**
     * Finalizes the current draft into a permanent, numbered document.
     *
     * Guards against double-tap (two rapid taps would otherwise create two real documents with
     * consecutive numbers — nothing at the DB layer stops that on its own). Requires a billed-to
     * customer with a resolved customerId (i.e. selected via autocomplete, not hand-typed).
     */
    /**
     * Returns the local PDF file for the currently finalized document, generating it first if
     * the eager attempt in onFinalizeInvoice() didn't leave one behind (e.g. it failed). Safe to
     * call every time the PDF viewer opens - ensurePdf() is idempotent.
     */
    suspend fun ensureFinalizedPdf(): File {
        val document = requireNotNull(finalizedDocument.value) {
            "No finalized document to render a PDF for"
        }
        return invoicePdfRenderer.ensurePdf(document)
    }

    fun onFinalizeInvoice() {
        if (_isFinalizing.value) return

        val draft = draftStore.currentDraft
        val customerId = draft.billedTo?.customerId
        requireNotNull(customerId) { "Cannot finalize without a selected billed-to customer" }

        // Which half of a "Challan + Invoice" pair (if either) this finalize is — see
        // InvoiceFinalizer.JobWorkLinkage. Both cases were declared upfront, never guessed here:
        // isJobWorkFlow was set by the Document Type dropdown's "Challan + Invoice" item, and
        // jobWorkChallanLink was set by onContinueToJobWorkInvoice() when this Invoice draft was
        // opened.
        val jobWorkLinkage = when {
            draft.documentType == DraftDocumentType.CHALLAN && draft.isJobWorkFlow ->
                JobWorkLinkage.ReserveLinkedInvoiceNumber
            draft.documentType == DraftDocumentType.INVOICE && draft.jobWorkChallanLink != null ->
                JobWorkLinkage.UseReservedNumber(
                    documentNumber = draft.jobWorkChallanLink.reservedInvoiceNumber,
                    linkedChallanDocumentNumber = draft.jobWorkChallanLink.challanDocumentNumber,
                    linkedChallanDate = draft.jobWorkChallanLink.challanDate
                )
            else -> JobWorkLinkage.None
        }

        viewModelScope.launch {
            _isFinalizing.value = true
            val document = invoiceFinalizer.finalize(draft, customerId, jobWorkLinkage)

            // Best-effort: the document number is already committed at this point, which is the
            // truly irreversible part of finalize. If PDF generation fails here (e.g. disk full),
            // finalize still succeeds and navigation still proceeds - ensurePdf() is idempotent
            // and gets called again defensively when the user opens the PDF viewer, so a failed
            // attempt here self-heals on next view without any dedicated retry UI.
            //
            // generateFreshPdf(), not ensurePdf(): this document was just created by the finalize
            // call above, so it's guaranteed not to exist on disk or in Storage yet - ensurePdf()'s
            // Storage-download check would be a real network round trip guaranteed to fail here,
            // adding latency to every finalize for no benefit (confirmed live on-device).
            runCatching { invoicePdfRenderer.generateFreshPdf(document) }

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

    /**
     * Deletion happens on the full-screen line-item entry surface, which pops back to Workspace
     * immediately after — so the "Undo" snackbar can't be shown from there. This is a one-shot
     * event (StateFlow, not SharedFlow: Workspace's recomposition after the pop-back is guaranteed
     * to observe whatever value is current, so nothing is missed) that Workspace consumes via
     * onLineItemDeletedEventConsumed() once its snackbar has been shown.
     */
    data class DeletedLineItem(val index: Int, val item: DraftLineItem)

    private val _lineItemDeleted = MutableStateFlow<DeletedLineItem?>(null)
    val lineItemDeleted: StateFlow<DeletedLineItem?> = _lineItemDeleted.asStateFlow()

    fun onRemoveLineItem(index: Int) {
        val removed = draftStore.currentDraft.lineItems[index]
        draftStore.removeLineItem(index)
        _uiState.value = draftStore.currentDraft
        _lineItemDeleted.value = DeletedLineItem(index, removed)
    }

    fun onLineItemDeletedEventConsumed() {
        _lineItemDeleted.value = null
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
        // Chrome title ("Invoice"/"Challan") and the predicted number both depend on which type
        // is selected — previously left stale here, so switching the dropdown kept showing the
        // old type's title and number until the next screen visit.
        _chromeSpec.value = buildDraftChromeSpec(draftStore.currentDraft)
        refreshPredictedDocumentNumber(documentType)
    }

    // ------------------------------------------------------------
    // Atom 7 — Job Work (Challan + Invoice)
    // ------------------------------------------------------------

    /** Set by the Document Type dropdown's "Challan + Invoice" item, right after it sets the
     *  type itself to CHALLAN — see InvoiceWorkspaceScreen. */
    fun onJobWorkFlowChanged(enabled: Boolean) {
        draftStore.setJobWorkFlow(enabled)
        _uiState.value = draftStore.currentDraft
        // Subtitle ("Draft" vs "Job Work") depends on this flag too.
        _chromeSpec.value = buildDraftChromeSpec(draftStore.currentDraft)
    }

    /** "Received vide Challan No X dated Y" — available on any Challan, not gated by job work. */
    fun onInboundChallanReferenceChanged(reference: DraftInboundChallanReference?) {
        draftStore.setInboundChallanReference(reference)
        _uiState.value = draftStore.currentDraft
    }

    fun onIssueDateChanged(date: LocalDate) {
        draftStore.setIssueDate(date)
        _uiState.value = draftStore.currentDraft
    }

    fun onSelfNotesChanged(notes: String) {
        draftStore.setSelfNotes(notes)
        _uiState.value = draftStore.currentDraft
    }

    /**
     * Opens a fresh Invoice draft continuing the job-work Challan that was just finalized,
     * carrying over the same customer and a reference back to that Challan — but deliberately
     * NOT its line items/goods value, since only the job-work service gets billed on the
     * Invoice (see CLAUDE.md's "Challan → Invoice (job work)" domain model section).
     *
     * Called from the Challan's "Challan Finalized" confirmation screen's "Continue to Invoice"
     * button, so draftStore still holds that Challan's draft state (onFinalizeInvoice()
     * deliberately doesn't reset it - see its comment) - this is where that reset finally
     * happens instead.
     *
     * Deliberately NOT nulling _finalizedDocument here, for the same reason onFinalizeInvoice()
     * doesn't reset the draft eagerly (see its comment): the "finalized" route this was called
     * from is still composed when this runs (navigation to WORKSPACE hasn't happened yet), and
     * its requireNotNull(finalizedDocument) races the null against that recomposition - crashed
     * with "Finalized route entered without a finalized document" when the null landed first.
     * onCreateInvoice() already nulls it unconditionally on the next new-document flow, and
     * finalizing this Invoice overwrites it with the new document anyway, so nothing here is
     * left stale in practice.
     */
    fun onContinueToJobWorkInvoice() {
        val challan = requireNotNull(_finalizedDocument.value) {
            "No finalized Challan to continue from"
        }
        val jobWorkLink = requireNotNull(challan.jobWorkLink) {
            "Challan has no reserved Invoice link"
        }
        val carriedBilledTo = requireNotNull(draftStore.currentDraft.billedTo) {
            "Finalized Challan draft has no billedTo to carry forward"
        }
        val carriedShippedTo = draftStore.currentDraft.shippedTo

        draftStore.reset()
        draftStore.setDocumentType(DraftDocumentType.INVOICE)
        draftStore.setBilledTo(carriedBilledTo)
        carriedShippedTo?.let { draftStore.setShippedToOverride(it) }
        draftStore.setJobWorkChallanLink(
            DraftJobWorkChallanLink(
                reservedInvoiceNumber = jobWorkLink.linkedDocumentNumber,
                challanDocumentNumber = challan.identity.documentNumber,
                challanDate = challan.identity.issueDate
            )
        )

        _hasActiveDraft.value = true
        _uiState.value = draftStore.currentDraft
        refreshReferenceListSuggestions()

        _chromeSpec.value = buildDraftChromeSpec(draftStore.currentDraft)
        // This draft already has a real reserved number (draftStore.jobWorkChallanLink, set
        // above) — previewDocument prefers that over a prediction, so there's nothing to peek.
        _predictedDocumentNumber.value = null
    }
}