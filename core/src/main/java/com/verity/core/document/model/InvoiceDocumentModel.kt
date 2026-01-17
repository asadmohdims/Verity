package com.verity.core.document.model

import java.time.LocalDate

/**
 * InvoiceDocumentModel
 *
 * Canonical, immutable representation of an invoice document.
 *
 * This model:
 * - Is authoritative for preview, PDF generation, and printing
 * - Is independent of draft UI state
 * - Contains no mutability or side effects
 * - Is safe to snapshot, cache, or persist after finalization
 */
data class InvoiceDocumentModel(
    val identity: DocumentIdentity,
    val parties: DocumentParties,
    val lineItems: List<DocumentLineItem>,
    val logistics: DocumentLogistics?,
    val taxation: DocumentTaxation?,
    val totals: DocumentTotals,
    val footer: DocumentFooter
)

/* ---------- Identity ---------- */

data class DocumentIdentity(
    val documentType: DocumentType,
    val documentNumber: String,
    val issueDate: LocalDate,
    val seller: SellerDetails
)

enum class DocumentType {
    INVOICE,
    CHALLAN
}

/**
 * SellerDetails
 *
 * NOTE:
 * This is intentionally provisional.
 * Seller/Organization modeling will be finalized separately.
 */
data class SellerDetails(
    val name: String,
    val gstin: String?,
    val addressLine1: String,
    val addressLine2: String?,
    val city: String,
    val state: String,
    val stateCode: String,
    val pincode: String
)

/* ---------- Parties ---------- */

/**
 * DocumentParty
 *
 * Snapshot of customer information as it appears on the document.
 * This must NOT reference CustomerEntity.
 */
data class DocumentParty(
    val name: String,
    val gstin: String,
    val addressLines: List<String>,
    val state: String,
    val stateCode: String
)

data class DocumentParties(
    val billedTo: DocumentParty,
    val shippedTo: DocumentParty
)

/* ---------- Line Items ---------- */

data class DocumentLineItem(
    val description: String,
    val hsnCode: String,
    val quantity: Long,
    val unit: String,
    val ratePaise: Long,
    val amountPaise: Long
)

/* ---------- Logistics ---------- */

data class DocumentLogistics(
    val transporterName: String?,
    val vehicleNumber: String?,
    val supplyDate: LocalDate?,
    val grOrLrNumber: String?,
    val freightPaise: Long?,
    val notes: String?
)

/* ---------- Taxation ---------- */

data class DocumentTaxation(
    val mode: DocumentTaxMode,
    val cgst: DocumentTaxComponent?,
    val sgst: DocumentTaxComponent?,
    val igst: DocumentTaxComponent?
)

enum class DocumentTaxMode {
    INTRA_STATE,
    INTER_STATE
}

data class DocumentTaxComponent(
    val ratePercent: Long,
    val amountPaise: Long
)

/* ---------- Totals ---------- */

data class DocumentTotals(
    val itemsSubtotalPaise: Long,
    val freightPaise: Long,
    val taxTotalPaise: Long,
    val grandTotalPaise: Long
)

/* ---------- Footer ---------- */

data class DocumentFooter(
    val declarationText: String,
    val notes: String?
)