package com.verity.feature.invoice.projection

import com.verity.core.document.model.*
import com.verity.feature.invoice.draft.InvoiceDraftUiState
import com.verity.feature.invoice.draft.DraftAddress
import com.verity.feature.invoice.draft.DraftLineItem
import com.verity.feature.invoice.draft.DraftTaxBreakdown
import com.verity.feature.invoice.draft.DraftTaxMode
import java.time.Clock
import java.time.LocalDate

/**
 * DraftToInvoiceDocument
 *
 * Pure projection from feature draft state to canonical document model.
 *
 * IMPORTANT:
 * - Lives in feature because draft is a feature workflow.
 * - Produces core.document.model.InvoiceDocumentModel.
 * - Side-effect free, deterministic.
 * - Used for BOTH preview and finalization.
 */
object DraftToInvoiceDocument {

    fun project(
        draft: InvoiceDraftUiState,
        documentNumber: String,
        seller: SellerDetails,
        clock: Clock
    ): InvoiceDocumentModel {

        val billedTo = requireNotNull(draft.billedTo) {
            "Cannot generate document without Billed To"
        }

        val shippedTo = draft.effectiveShippedTo ?: billedTo

        return InvoiceDocumentModel(
            identity = DocumentIdentity(
                documentType = draft.documentType.toDocumentType(),
                documentNumber = documentNumber,
                issueDate = LocalDate.now(clock),
                seller = seller,
                // Place of Supply for goods is the delivery state - i.e. Shipped To, which
                // already falls back to Billed To above when shipping wasn't specified separately.
                placeOfSupplyState = shippedTo.state,
                placeOfSupplyStateCode = shippedTo.stateCode,
                reverseChargeApplicable = draft.reverseCharge
            ),
            parties = DocumentParties(
                billedTo = billedTo.toDocumentParty(),
                shippedTo = shippedTo.toDocumentParty()
            ),
            lineItems = draft.lineItems.map { it.toDocumentLineItem() },
            logistics = draft.transportDetails?.toDocumentLogistics(),
            taxation = draft.summary.tax?.toDocumentTaxation(),
            totals = DocumentTotals(
                itemsSubtotalPaise = draft.summary.subtotalPaise,
                freightPaise = draft.transportDetails?.freightPaise ?: 0L,
                taxTotalPaise = draft.summary.taxTotalPaise,
                grandTotalPaise = draft.summary.grandTotalPaise
            ),
            footer = DocumentFooter(
                // Matches the approved PDF design's exact wording (Main.dc.html) - printed next
                // to the signature block.
                declarationText = "Certified that the particulars given above are true and correct.",
                notes = draft.transportDetails?.notes
            )
        )
    }
}

/* ---------- Mapping helpers ---------- */

private fun DraftAddress.toDocumentParty(): DocumentParty {
    require(name.isNotBlank()) { "Customer name is required" }
    require(state.isNotBlank()) { "Customer state is required" }
    require(stateCode.isNotBlank()) { "Customer state code is required" }

    val gstin = requireNotNull(gstin) {
        "GSTIN is required to generate an invoice document"
    }

    return DocumentParty(
        name = name,
        gstin = gstin,
        addressLines = listOfNotNull(
            addressLine1,
            addressLine2,
            city
        ),
        state = state,
        stateCode = stateCode
    )
}

private fun DraftLineItem.toDocumentLineItem(): DocumentLineItem =
    DocumentLineItem(
        description = description,
        hsnCode = hsnCode,
        quantity = quantity,
        unit = unit,
        ratePaise = ratePaise,
        amountPaise = quantity * ratePaise
    )

private fun DraftTaxBreakdown.toDocumentTaxation(): DocumentTaxation =
    DocumentTaxation(
        mode = when (mode) {
            DraftTaxMode.INTRA_STATE -> DocumentTaxMode.INTRA_STATE
            DraftTaxMode.INTER_STATE -> DocumentTaxMode.INTER_STATE
        },
        cgst = cgst?.let { DocumentTaxComponent(it.ratePercent, it.amountPaise) },
        sgst = sgst?.let { DocumentTaxComponent(it.ratePercent, it.amountPaise) },
        igst = igst?.let { DocumentTaxComponent(it.ratePercent, it.amountPaise) }
    )

private fun com.verity.feature.invoice.draft.DraftDocumentType.toDocumentType(): DocumentType =
    when (this) {
        com.verity.feature.invoice.draft.DraftDocumentType.INVOICE -> DocumentType.INVOICE
        com.verity.feature.invoice.draft.DraftDocumentType.CHALLAN -> DocumentType.CHALLAN
    }

private fun com.verity.feature.invoice.draft.DraftTransportDetails.toDocumentLogistics(): DocumentLogistics =
    DocumentLogistics(
        transporterName = transporterName,
        vehicleNumber = vehicleNumber,
        supplyDate = supplyDate,
        grOrLrNumber = grOrLrNumber,
        freightPaise = freightPaise,
        notes = notes,
        ewayBillNumber = ewayBillNumber
    )