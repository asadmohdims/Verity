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

    private fun recalculate(
        draft: InvoiceDraftUiState
    ): InvoiceDraftUiState {
        val itemsTotal = draft.lineItems.sumOf { it.amount }
        val freight = draft.transportDetails?.freightAmount ?: 0.0
        val subtotal = itemsTotal + freight

        return draft.copy(
            summary = DraftSummary(
                subtotal = subtotal,
                taxTotal = 0.0,
                grandTotal = subtotal
            )
        )
    }
}