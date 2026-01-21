package com.verity.invoice.draft

import java.time.LocalDate

/**
 * UI-only invoice draft state.
 *
 * This model is NOT authoritative.
 * It exists only while the user is editing.
 */

/**
 * NOTE:
 * This draft model is consumed ONLY by:
 * - Invoice Workspace UI
 * - Draft reducer
 *
 * It must NEVER be consumed directly by:
 * - Preview rendering
 * - Finalization logic
 * - Persistence or domain events
 */
data class InvoiceDraftUiState(
    val documentType: DraftDocumentType = DraftDocumentType.INVOICE,
    val customer: DraftCustomer? = null,
    val billedTo: DraftAddress? = null,
    val shippedTo: DraftAddress? = null,
    val supplyDate: LocalDate? = null,
    val reverseCharge: Boolean = false,
    val lineItems: List<DraftLineItem> = emptyList(),
    val transportDetails: DraftTransportDetails? = null,
    val summary: DraftSummary = DraftSummary()
) {
    /**
     * Effective shipped address for display and read-only consumption.
     *
     * Business rule:
     * - Shipped To mirrors Billed To by default
     * - A user-set shippedTo acts as an explicit override
     *
     * IMPORTANT:
     * - This is a derived value (no mutation)
     * - Do NOT persist or finalize using this field
     */
    val effectiveShippedTo: DraftAddress?
        get() = shippedTo ?: billedTo
}

/* ---------- Supporting Draft Types ---------- */

enum class DraftDocumentType {
    INVOICE,
    CHALLAN
}

enum class DraftTaxMode {
    INTRA_STATE, // CGST + SGST
    INTER_STATE  // IGST
}

/**
 * DraftTaxComponent
 *
 * Draft-level tax component.
 *
 * - ratePercent is a whole-number percentage (Long) to preserve
 *   deterministic, integer-only math.
 * - amountPaise is the derived monetary value in paise.
 */
data class DraftTaxComponent(
    val ratePercent: Long,
    val amountPaise: Long
)

data class DraftTaxBreakdown(
    val mode: DraftTaxMode,
    val cgst: DraftTaxComponent? = null,
    val sgst: DraftTaxComponent? = null,
    val igst: DraftTaxComponent? = null
)

data class DraftCustomer(
    val displayName: String,
    val gstin: String?
)

data class DraftAddress(
    val name: String,
    val gstin: String?,
    val addressLine1: String,
    val addressLine2: String? = null,
    val city: String,
    val state: String,
    val stateCode: String,
    val pincode: String
)

/**
 * DraftLineItem
 *
 * Represents a single invoice line item in draft state.
 *
 * NOTES:
 * - This is UI-only workflow state, not domain truth.
 * - Monetary values are represented in paise (Long) to align with
 *   Money, projections, and persistence.
 * - Line amount is intentionally NOT stored; it is always derived
 *   as (quantity × ratePaise).
 */
data class DraftLineItem(
    val description: String,
    val hsnCode: String,
    val quantity: Long,
    val unit: String,
    val ratePaise: Long
)

/**
 * DraftTransportDetails
 *
 * Draft-only logistics metadata.
 *
 * NOTE:
 * - freightPaise is monetary and therefore represented in paise (Long).
 * - Draft-only; finalization rules apply later.
 */
data class DraftTransportDetails(
    val transporterName: String? = null,
    val vehicleNumber: String? = null,
    val supplyDate: LocalDate? = null,
    val grOrLrNumber: String? = null,
    val freightPaise: Long? = null,
    val notes: String? = null
)

/**
 * DraftSummary
 *
 * Derived financial summary for the draft invoice.
 *
 * All monetary values are represented in paise (Long) to ensure
 * deterministic behavior and consistency with domain projections.
 */
data class DraftSummary(
    val subtotalPaise: Long = 0L,
    val tax: DraftTaxBreakdown? = null,
    val taxTotalPaise: Long = 0L,
    val grandTotalPaise: Long = 0L
)