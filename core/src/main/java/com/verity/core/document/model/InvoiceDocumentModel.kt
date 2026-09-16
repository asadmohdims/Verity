package com.verity.core.document.model

import com.verity.core.serialization.LocalDateIsoSerializer
import kotlinx.serialization.Serializable
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
 *
 * Serializable so a finalized document can be stored as-is (see CLAUDE.md's Data & sync
 * architecture — a finalized document's persisted payload is this model, unchanged).
 */

/**
 * FUTURE:
 * - Used as input to PDF generation
 * - Used as input to finalization event emission
 *
 * NOT YET:
 * - No numbering authority
 * - No lifecycle semantics
 */
@Serializable
data class InvoiceDocumentModel(
    val identity: DocumentIdentity,
    val parties: DocumentParties,
    val lineItems: List<DocumentLineItem>,
    val logistics: DocumentLogistics?,
    /** Available on any Challan — documents the customer's own inbound delivery challan that
     *  arrived with the goods. Not gated behind job work; general goods-provenance reference. */
    val inboundChallanReference: DocumentInboundChallanReference? = null,
    /** Present on a job-work Challan and its continuation Invoice — see [DocumentJobWorkLink]. */
    val jobWorkLink: DocumentJobWorkLink? = null,
    val taxation: DocumentTaxation?,
    val totals: DocumentTotals,
    val footer: DocumentFooter
)

/**
 * "Received vide Challan No X dated Y" — the customer's own delivery challan that accompanied
 * goods sent in for job work. This document is external to Verity and is never itself a Verity
 * row; this is purely a printed reference on our own Challan.
 */
@Serializable
data class DocumentInboundChallanReference(
    val challanNumber: String,
    @Serializable(with = LocalDateIsoSerializer::class)
    val challanDate: LocalDate? = null
)

/**
 * Job-work cross-reference between a Challan and its continuation Invoice. Meaning depends on
 * which document it's attached to (inferable from that document's own identity.documentType,
 * so no separate "which type does this point at" field is needed):
 * - On the Challan: "this will be billed on Invoice X dated Y" — linkedDocumentId is always null,
 *   since that Invoice has no real row yet at Challan-finalize time (its number is reserved, not
 *   yet issued).
 * - On the Invoice: "this continues Challan X dated Y" — linkedDocumentId is the real Challan
 *   documentId, mirroring DocumentEntity.linkedDocumentId on this same row.
 */
@Serializable
data class DocumentJobWorkLink(
    val linkedDocumentNumber: String,
    @Serializable(with = LocalDateIsoSerializer::class)
    val linkedDocumentDate: LocalDate,
    val linkedDocumentId: String? = null
)

/* ---------- Identity ---------- */

@Serializable
data class DocumentIdentity(
    val documentType: DocumentType,
    val documentNumber: String,
    @Serializable(with = LocalDateIsoSerializer::class)
    val issueDate: LocalDate,
    val seller: SellerDetails,
    /**
     * GST Rule 46(f) mandatory field: the state (and code) the supply is legally considered to
     * occur in. For goods, this is the delivery state — i.e. Shipped To, falling back to
     * Billed To when they're the same (the common case). Computed at document-build time, never
     * user-entered.
     */
    val placeOfSupplyState: String,
    val placeOfSupplyStateCode: String,
    /**
     * GST Rule 46 also requires stating whether tax is payable on reverse charge. Mapped from the
     * pre-existing (previously unwired) InvoiceDraftUiState.reverseCharge - see
     * DraftToInvoiceDocument. Defaults false; there's still no workspace UI control to set it
     * true, so it's only reachable today by constructing a draft directly (e.g. in a test).
     */
    val reverseChargeApplicable: Boolean = false
)

@Serializable
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
@Serializable
data class SellerDetails(
    val name: String,
    val gstin: String?,
    val addressLine1: String,
    val addressLine2: String?,
    val city: String,
    val state: String,
    val stateCode: String,
    val pincode: String,
    val tagline: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val bankName: String? = null,
    val bankAccountNumber: String? = null,
    val bankIfsc: String? = null,
    val msmeOrUamNumber: String? = null,
    /** Static per-business boilerplate (jurisdiction, returns policy, etc.), not per-document. */
    val termsAndConditions: List<String>? = null
)

/* ---------- Parties ---------- */

/**
 * DocumentParty
 *
 * Snapshot of customer information as it appears on the document.
 * This must NOT reference CustomerEntity.
 */
@Serializable
data class DocumentParty(
    val name: String,
    val gstin: String,
    val addressLines: List<String>,
    val state: String,
    val stateCode: String
)

@Serializable
data class DocumentParties(
    val billedTo: DocumentParty,
    val shippedTo: DocumentParty
)

/* ---------- Line Items ---------- */

@Serializable
data class DocumentLineItem(
    val description: String,
    val hsnCode: String,
    /**
     * Null for line items with nothing to count or check against delivery (e.g. a job-work
     * service) — genuinely absent, not defaulted to 1. [amountPaise] is unaffected: it's always
     * `ratePaise` in that case, since there's only one unit of the work being billed.
     */
    val quantity: Long?,
    val unit: String,
    val ratePaise: Long,
    val amountPaise: Long
)

/* ---------- Logistics ---------- */

@Serializable
data class DocumentLogistics(
    val transporterName: String?,
    val vehicleNumber: String?,
    @Serializable(with = LocalDateIsoSerializer::class)
    val supplyDate: LocalDate?,
    val grOrLrNumber: String?,
    val freightPaise: Long?,
    val notes: String?,
    /** Reference only — no validation, generation, or e-way-bill-portal integration. */
    val ewayBillNumber: String? = null
)

/* ---------- Taxation ---------- */

@Serializable
data class DocumentTaxation(
    val mode: DocumentTaxMode,
    val cgst: DocumentTaxComponent?,
    val sgst: DocumentTaxComponent?,
    val igst: DocumentTaxComponent?
)

@Serializable
enum class DocumentTaxMode {
    INTRA_STATE,
    INTER_STATE
}

@Serializable
data class DocumentTaxComponent(
    val ratePercent: Long,
    val amountPaise: Long
)

/* ---------- Totals ---------- */

@Serializable
data class DocumentTotals(
    val itemsSubtotalPaise: Long,
    val freightPaise: Long,
    val taxTotalPaise: Long,
    val grandTotalPaise: Long
)

/* ---------- Footer ---------- */

@Serializable
data class DocumentFooter(
    val declarationText: String,
    val notes: String?
)
