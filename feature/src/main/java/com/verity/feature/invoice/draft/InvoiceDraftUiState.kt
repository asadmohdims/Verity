package com.verity.invoice.draft

import java.time.LocalDate

/**
 * UI-only invoice draft state.
 *
 * This model is NOT authoritative.
 * It exists only while the user is editing.
 */
data class InvoiceDraftUiState(
    val documentType: DraftDocumentType = DraftDocumentType.INVOICE,
    val customer: DraftCustomer? = null,
    val billedTo: DraftAddress? = null,
    val shippedTo: DraftAddress? = null,
    val supplyDate: LocalDate? = null,
    val reverseCharge: Boolean = false,
    val lineItems: List<DraftLineItem> = emptyList(),
    val additionalDetails: DraftAdditionalDetails = DraftAdditionalDetails(),
    val transportDetails: DraftTransportDetails? = null,
    val summary: DraftSummary = DraftSummary()
)

/* ---------- Supporting Draft Types ---------- */

enum class DraftDocumentType {
    INVOICE,
    CHALLAN
}

data class DraftCustomer(
    val displayName: String,
    val gstin: String?
)

data class DraftAddress(
    val name: String,
    val addressLine1: String,
    val addressLine2: String? = null,
    val city: String,
    val state: String,
    val stateCode: String,
    val pincode: String
)

data class DraftLineItem(
    val description: String,
    val hsnCode: String,
    val quantity: Double,
    val unit: String,
    val rate: Double,
    val amount: Double        // UI-calculated convenience value
)

data class DraftAdditionalDetails(
    val freightAmount: Double? = null,
    val notes: String? = null
)

data class DraftTransportDetails(
    val transporterName: String? = null,
    val vehicleNumber: String? = null,
    val grOrLrNumber: String? = null,
    val transportMode: String? = null
)

data class DraftSummary(
    val subtotal: Double = 0.0,
    val taxTotal: Double = 0.0,
    val grandTotal: Double = 0.0
)