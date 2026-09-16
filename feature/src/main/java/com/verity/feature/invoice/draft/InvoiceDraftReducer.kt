package com.verity.feature.invoice.draft

import com.verity.core.document.model.HARDCODED_SELLER
import java.time.LocalDate

/**
 * Pure reducer functions for InvoiceDraftUiState.
 *
 * These functions:
 * - Are side-effect free
 * - Do NOT enforce domain rules
 * - Do NOT persist anything
 */

/**
 * PREVIEW SAFETY NOTE
 * -------------------
 * All derived financial values here are:
 * - Draft-level only
 * - Non-authoritative
 *
 * Preview MUST consume InvoiceDocumentModel,
 * never this reducer output directly.
 */
object InvoiceDraftReducer {

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
        quantity: Long?
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

    /**
     * Re-inserts a line item at a specific index — used to restore an item removed by
     * [removeLineItem] when the user taps Undo on the deletion snackbar. [index] is clamped to
     * the current list's bounds so an out-of-range index (e.g. the list shrank in the meantime)
     * degrades to an append rather than throwing.
     */
    fun insertLineItemAt(
        draft: InvoiceDraftUiState,
        index: Int,
        item: DraftLineItem
    ): InvoiceDraftUiState {
        val safeIndex = index.coerceIn(0, draft.lineItems.size)
        val updatedItems = draft.lineItems.toMutableList().apply {
            add(safeIndex, item)
        }
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
        // isJobWorkFlow always resets here — plain "Invoice"/"Challan" dropdown items call only
        // this, while "Challan + Invoice" calls this THEN a follow-up setJobWorkFlow(true), so
        // switching between any two of the three items always lands in the right state.
        // inboundChallanReference is Challan-only and clears when leaving Challan.
        return recalculate(
            draft.copy(
                documentType = documentType,
                isJobWorkFlow = false,
                inboundChallanReference = if (documentType == DraftDocumentType.CHALLAN) draft.inboundChallanReference else null
            )
        )
    }

    fun setJobWorkFlow(
        draft: InvoiceDraftUiState,
        enabled: Boolean
    ): InvoiceDraftUiState {
        return draft.copy(isJobWorkFlow = enabled)
    }

    fun setInboundChallanReference(
        draft: InvoiceDraftUiState,
        reference: DraftInboundChallanReference?
    ): InvoiceDraftUiState {
        return draft.copy(inboundChallanReference = reference)
    }

    fun setJobWorkChallanLink(
        draft: InvoiceDraftUiState,
        link: DraftJobWorkChallanLink?
    ): InvoiceDraftUiState {
        return draft.copy(jobWorkChallanLink = link)
    }

    fun setIssueDate(
        draft: InvoiceDraftUiState,
        date: LocalDate
    ): InvoiceDraftUiState {
        return draft.copy(issueDate = date)
    }

    /**
     * Returns a fresh, empty draft.
     *
     * Used after finalize (the just-finalized draft must not resurface as the next draft) and
     * after discard (fixes a prior bug where discarding left the old draft's data in the store).
     */
    fun reset(): InvoiceDraftUiState = InvoiceDraftUiState()

    private fun recalculate(
        draft: InvoiceDraftUiState
    ): InvoiceDraftUiState {

        val itemsSubtotalPaise: Long =
            draft.lineItems.sumOf { (it.quantity ?: 1L) * it.ratePaise }
        val freightPaise: Long =
            draft.transportDetails?.freightPaise ?: 0L
        val taxableSubtotalPaise: Long =
            itemsSubtotalPaise + freightPaise

        // -----------------------------
        // Draft Tax Calculation (Atom 6.2)
        // -----------------------------
        // Seller GST state comes from HARDCODED_SELLER (the same seller identity already used
        // at finalization/PDF) rather than a separate constant here, so there's only one place
        // that can drift from the seller's real GSTIN state.

        val buyerStateCode = draft.billedTo?.stateCode.orEmpty()

        val taxBreakdown =
            if (draft.documentType == DraftDocumentType.CHALLAN) {
                null
            } else if (buyerStateCode.isBlank()) {
                null
            } else {
                val isIntraState = buyerStateCode == HARDCODED_SELLER.stateCode

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