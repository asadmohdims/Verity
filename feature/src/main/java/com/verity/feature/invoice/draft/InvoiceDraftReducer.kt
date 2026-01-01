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
    /**
     * GST rates expressed as whole-number percentages.
     *
     * IMPORTANT:
     * - Represented as Long to preserve deterministic, integer-only math.
     * - Avoids floating-point rounding and replay instability.
     * - Draft-level only; finalization may introduce jurisdictional variants.
     */
    private const val GST_RATE_TOTAL_PERCENT: Long = 18L
    private const val GST_RATE_HALF_PERCENT: Long = 9L

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

    /**
     * TEMPORARY (Draft Spine v1)
     *
     * This function exists to support incremental editing flows
     * in the current Invoice Workspace implementation.
     *
     * IMPORTANT:
     * - Monetary amounts are NOT authoritative here.
     * - All financial totals MUST be derived in `recalculate(...)`.
     * - This function will become redundant once line-item editing
     *   is unified to submit full DraftLineItem updates only.
     *
     * Do NOT add business logic or financial derivation here.
     */
    fun updateLineItemQuantity(
        draft: InvoiceDraftUiState,
        index: Int,
        quantity: Long
    ): InvoiceDraftUiState {
        val updatedItems = draft.lineItems.mapIndexed { i, item ->
            if (i == index) {
                item.copy(quantity = quantity)
            } else {
                item
            }
        }
        return recalculate(draft.copy(lineItems = updatedItems))
    }

    /**
     * TEMPORARY (Draft Spine v1)
     *
     * This function exists to support incremental editing flows
     * in the current Invoice Workspace implementation.
     *
     * IMPORTANT:
     * - Monetary amounts are NOT authoritative here.
     * - All financial totals MUST be derived in `recalculate(...)`.
     * - This function will become redundant once line-item editing
     *   is unified to submit full DraftLineItem updates only.
     *
     * Do NOT add business logic or financial derivation here.
     */
    fun updateLineItemRate(
        draft: InvoiceDraftUiState,
        index: Int,
        ratePaise: Long
    ): InvoiceDraftUiState {
        val updatedItems = draft.lineItems.mapIndexed { i, item ->
            if (i == index) {
                item.copy(ratePaise = ratePaise)
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

        val itemsSubtotalPaise: Long =
            draft.lineItems.sumOf { it.quantity * it.ratePaise }
        val freightPaise: Long =
            draft.transportDetails?.freightPaise ?: 0L
        val taxableSubtotalPaise: Long =
            itemsSubtotalPaise + freightPaise

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
                            ratePercent = GST_RATE_HALF_PERCENT,
                            amountPaise = (taxableSubtotalPaise * GST_RATE_HALF_PERCENT) / 100
                        ),
                        sgst = DraftTaxComponent(
                            ratePercent = GST_RATE_HALF_PERCENT,
                            amountPaise = (taxableSubtotalPaise * GST_RATE_HALF_PERCENT) / 100
                        )
                    )
                } else {
                    DraftTaxBreakdown(
                        mode = DraftTaxMode.INTER_STATE,
                        igst = DraftTaxComponent(
                            ratePercent = GST_RATE_TOTAL_PERCENT,
                            amountPaise = (taxableSubtotalPaise * GST_RATE_TOTAL_PERCENT) / 100
                        )
                    )
                }
            }

        val taxTotalPaise: Long =
            taxBreakdown?.let {
                (it.cgst?.amountPaise ?: 0L) +
                (it.sgst?.amountPaise ?: 0L) +
                (it.igst?.amountPaise ?: 0L)
            } ?: 0L

        val grandTotalPaise = taxableSubtotalPaise + taxTotalPaise

        return draft.copy(
            summary = DraftSummary(
                subtotalPaise = itemsSubtotalPaise, // items ONLY
                tax = taxBreakdown,
                taxTotalPaise = taxTotalPaise,
                grandTotalPaise = grandTotalPaise
            )
        )
    }
}