package com.verity.invoice.draft

/**
 * Pure reducer functions for InvoiceDraftUiState.
 *
 * These functions:
 * - Are side-effect free
 * - Do NOT enforce domain rules
 * - Do NOT persist anything
 */
object InvoiceDraftReducer {

    private const val ASSUMED_SELLER_STATE_CODE = "27"
    private const val GST_RATE_TOTAL = 18.0
    private const val GST_RATE_HALF = 9.0

    fun setCustomer(
        draft: InvoiceDraftUiState,
        customer: DraftCustomer,
        billedTo: DraftAddress,
        shippedTo: DraftAddress = billedTo
    ): InvoiceDraftUiState {
        return draft.copy(
            customer = customer,
            billedTo = billedTo,
            shippedTo = shippedTo
        )
    }

    fun setBilledTo(
        draft: InvoiceDraftUiState,
        billedTo: DraftAddress
    ): InvoiceDraftUiState {
        return draft.copy(billedTo = billedTo)
    }

    fun setShippedToOverride(
        draft: InvoiceDraftUiState,
        shippedTo: DraftAddress
    ): InvoiceDraftUiState {
        return draft.copy(shippedTo = shippedTo)
    }


    fun addLineItem(
        draft: InvoiceDraftUiState,
        item: DraftLineItem
    ): InvoiceDraftUiState {
        val updatedItems = draft.lineItems + item
        return recalculate(draft.copy(lineItems = updatedItems))
    }

    fun updateLineItemQuantity(
        draft: InvoiceDraftUiState,
        index: Int,
        quantity: Double
    ): InvoiceDraftUiState {
        val updatedItems = draft.lineItems.mapIndexed { i, item ->
            if (i == index) {
                val newAmount = quantity * item.rate
                item.copy(quantity = quantity, amount = newAmount)
            } else {
                item
            }
        }
        return recalculate(draft.copy(lineItems = updatedItems))
    }

    fun updateLineItemRate(
        draft: InvoiceDraftUiState,
        index: Int,
        rate: Double
    ): InvoiceDraftUiState {
        val updatedItems = draft.lineItems.mapIndexed { i, item ->
            if (i == index) {
                val newAmount = item.quantity * rate
                item.copy(rate = rate, amount = newAmount)
            } else {
                item
            }
        }
        return recalculate(draft.copy(lineItems = updatedItems))
    }

    fun updateLineItem(
        draft: InvoiceDraftUiState,
        index: Int,
        item: DraftLineItem
    ): InvoiceDraftUiState {
        val updatedItems = draft.lineItems.mapIndexed { i, existing ->
            if (i == index) item else existing
        }
        return recalculate(draft.copy(lineItems = updatedItems))
    }

    fun removeLineItem(
        draft: InvoiceDraftUiState,
        index: Int
    ): InvoiceDraftUiState {
        val updatedItems = draft.lineItems.filterIndexed { i, _ -> i != index }
        return recalculate(draft.copy(lineItems = updatedItems))
    }

    fun setTransportDetails(
        draft: InvoiceDraftUiState,
        details: DraftTransportDetails?
    ): InvoiceDraftUiState {
        return recalculate(
            draft.copy(
                transportDetails = details
            )
        )
    }

    fun setDocumentType(
        draft: InvoiceDraftUiState,
        documentType: DraftDocumentType
    ): InvoiceDraftUiState {
        // No behavior change yet.
        // Tax behavior will branch on documentType in Atom 6.2.
        return recalculate(
            draft.copy(documentType = documentType)
        )
    }

    private fun recalculate(
        draft: InvoiceDraftUiState
    ): InvoiceDraftUiState {

        val itemsTotal = draft.lineItems.sumOf { it.amount }
        val freight = draft.transportDetails?.freightAmount ?: 0.0
        val subtotal = itemsTotal + freight

        // -----------------------------
        // Draft Tax Calculation (Atom 6.2)
        // -----------------------------
        // TODO (FINALIZATION):
        // Remove hardcoded ASSUMED_SELLER_STATE_CODE.
        // Seller GST state must come from Organization Profile / GSTIN
        // once Draft → Final boundary is introduced.

        val buyerStateCode = draft.billedTo?.stateCode.orEmpty()

        val taxBreakdown =
            if (draft.documentType == DraftDocumentType.CHALLAN) {
                null
            } else if (buyerStateCode.isBlank()) {
                null
            } else {
                val isIntraState = buyerStateCode == ASSUMED_SELLER_STATE_CODE

                if (isIntraState) {
                    DraftTaxBreakdown(
                        mode = DraftTaxMode.INTRA_STATE,
                        cgst = DraftTaxComponent(
                            ratePercent = GST_RATE_HALF,
                            amount = subtotal * GST_RATE_HALF / 100
                        ),
                        sgst = DraftTaxComponent(
                            ratePercent = GST_RATE_HALF,
                            amount = subtotal * GST_RATE_HALF / 100
                        )
                    )
                } else {
                    DraftTaxBreakdown(
                        mode = DraftTaxMode.INTER_STATE,
                        igst = DraftTaxComponent(
                            ratePercent = GST_RATE_TOTAL,
                            amount = subtotal * GST_RATE_TOTAL / 100
                        )
                    )
                }
            }

        val taxTotal =
            taxBreakdown?.let {
                (it.cgst?.amount ?: 0.0) +
                (it.sgst?.amount ?: 0.0) +
                (it.igst?.amount ?: 0.0)
            } ?: 0.0

        val grandTotal = subtotal + taxTotal

        return draft.copy(
            summary = DraftSummary(
                subtotal = subtotal,
                tax = taxBreakdown,
                taxTotal = taxTotal,
                grandTotal = grandTotal
            )
        )
    }
}