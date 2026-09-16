package com.verity.feature.invoice.draft

import java.time.LocalDate

/**
 * InvoiceDraftStore
 *
 * Owns the in-memory invoice draft and is the single mutation gate.
 * Platform-agnostic. No UI, no lifecycle, no persistence.
 */
class InvoiceDraftStore(
    initialDraft: InvoiceDraftUiState = InvoiceDraftUiState()
) {

    private var _currentDraft: InvoiceDraftUiState = initialDraft

    val currentDraft: InvoiceDraftUiState
        get() = _currentDraft

    // ------------------------------------------------------------
    // Atom 1 — Billed To
    // ------------------------------------------------------------

    fun setBilledTo(address: DraftAddress) {
        _currentDraft = InvoiceDraftReducer.setBilledTo(
            draft = _currentDraft,
            billedTo = address
        )
    }

    fun clearBilledTo() {
        _currentDraft = _currentDraft.copy(billedTo = null, shippedTo = null)
    }

    // ------------------------------------------------------------
    // Atom 2 — Shipped To (Override)
    // ------------------------------------------------------------

    fun setShippedToOverride(address: DraftAddress) {
        _currentDraft = InvoiceDraftReducer.setShippedToOverride(
            draft = _currentDraft,
            shippedTo = address
        )
    }

    fun clearShippedToOverride() {
        _currentDraft = _currentDraft.copy(shippedTo = null)
    }

    // ------------------------------------------------------------
    // Atom 3 — Line Items
    // ------------------------------------------------------------

    fun addLineItem(item: DraftLineItem) {
        _currentDraft = InvoiceDraftReducer.addLineItem(
            draft = _currentDraft,
            item = item
        )
    }

    fun removeLineItem(index: Int) {
        _currentDraft = InvoiceDraftReducer.removeLineItem(
            draft = _currentDraft,
            index = index
        )
    }

    fun insertLineItemAt(index: Int, item: DraftLineItem) {
        _currentDraft = InvoiceDraftReducer.insertLineItemAt(
            draft = _currentDraft,
            index = index,
            item = item
        )
    }

    fun updateLineItem(
        index: Int,
        item: DraftLineItem
    ) {
        _currentDraft = InvoiceDraftReducer.updateLineItem(
            draft = _currentDraft,
            index = index,
            item = item
        )
    }

    // ------------------------------------------------------------
    // Atom 5 — Logistics (Freight)
    // ------------------------------------------------------------

    fun setTransportDetails(details: DraftTransportDetails?) {
        _currentDraft = InvoiceDraftReducer.setTransportDetails(
            draft = _currentDraft,
            details = details
        )
    }

    // ------------------------------------------------------------
    // Atom 6.1 — Document Type
    // ------------------------------------------------------------

    fun setDocumentType(documentType: DraftDocumentType) {
        _currentDraft = InvoiceDraftReducer.setDocumentType(
            draft = _currentDraft,
            documentType = documentType
        )
    }

    /** Clears the draft back to empty. See InvoiceDraftReducer.reset(). */
    fun reset() {
        _currentDraft = InvoiceDraftReducer.reset()
    }

    // ------------------------------------------------------------
    // Issue Date — defaults to today (InvoiceDraftUiState), overridden here to backdate.
    // ------------------------------------------------------------

    fun setIssueDate(date: LocalDate) {
        _currentDraft = InvoiceDraftReducer.setIssueDate(
            draft = _currentDraft,
            date = date
        )
    }

    fun setSelfNotes(notes: String) {
        _currentDraft = InvoiceDraftReducer.setSelfNotes(
            draft = _currentDraft,
            notes = notes
        )
    }

    // ------------------------------------------------------------
    // Atom 7 — Job Work (Challan + Invoice)
    // ------------------------------------------------------------

    fun setJobWorkFlow(enabled: Boolean) {
        _currentDraft = InvoiceDraftReducer.setJobWorkFlow(
            draft = _currentDraft,
            enabled = enabled
        )
    }

    fun setInboundChallanReference(reference: DraftInboundChallanReference?) {
        _currentDraft = InvoiceDraftReducer.setInboundChallanReference(
            draft = _currentDraft,
            reference = reference
        )
    }

    fun setJobWorkChallanLink(link: DraftJobWorkChallanLink?) {
        _currentDraft = InvoiceDraftReducer.setJobWorkChallanLink(
            draft = _currentDraft,
            link = link
        )
    }
}