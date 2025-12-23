package com.verity.invoice.draft

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
        _currentDraft = _currentDraft.copy(billedTo = null)
    }
}