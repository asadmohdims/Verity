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
                val newAmount = (quantity * item.rate).toLong()
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
        rate: Long
    ): InvoiceDraftUiState {
        val updatedItems = draft.lineItems.mapIndexed { i, item ->
            if (i == index) {
                val newAmount = (item.quantity * rate).toLong()
                item.copy(rate = rate, amount = newAmount)
            } else {
                item
            }
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

    fun setFreight(
        draft: InvoiceDraftUiState,
        freightAmount: Long?
    ): InvoiceDraftUiState {
        val updatedDetails = draft.additionalDetails.copy(
            freightAmount = freightAmount
        )
        return recalculate(draft.copy(additionalDetails = updatedDetails))
    }

    private fun recalculate(
        draft: InvoiceDraftUiState
    ): InvoiceDraftUiState {
        val itemsTotal = draft.lineItems.sumOf { it.amount }
        val freight = draft.additionalDetails.freightAmount ?: 0L
        val subtotal = itemsTotal + freight

        return draft.copy(
            summary = DraftSummary(
                subtotal = subtotal,
                taxTotal = 0L,
                grandTotal = subtotal
            )
        )
    }
}