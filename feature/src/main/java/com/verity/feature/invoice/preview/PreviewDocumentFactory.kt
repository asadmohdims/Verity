package com.verity.feature.invoice.preview

import com.verity.core.document.model.*
import java.time.LocalDate

object PreviewDocumentFactory {

    fun create(): InvoiceDocumentModel {
        return InvoiceDocumentModel(
            identity = DocumentIdentity(
                documentType = DocumentType.INVOICE,
                documentNumber = "PREVIEW-001",
                issueDate = LocalDate.now(),
                seller = SellerDetails(
                    name = "Verity",
                    gstin = null,
                    addressLine1 = "",
                    addressLine2 = null,
                    city = "",
                    state = "",
                    stateCode = "",
                    pincode = ""
                )
            ),
            parties = DocumentParties(
                billedTo = DocumentParty(
                    name = "Preview Customer",
                    gstin = "27AAACB1234Z1Z",
                    addressLines = listOf("Preview Address"),
                    state = "Maharashtra",
                    stateCode = "27"
                ),
                shippedTo = DocumentParty(
                    name = "Preview Customer",
                    gstin = "27AAACB1234Z1Z",
                    addressLines = listOf("Preview Address"),
                    state = "Maharashtra",
                    stateCode = "27"
                )
            ),
            lineItems = emptyList(),
            logistics = null,
            taxation = null,
            totals = DocumentTotals(
                itemsSubtotalPaise = 0L,
                freightPaise = 0L,
                taxTotalPaise = 0L,
                grandTotalPaise = 0L
            ),
            footer = DocumentFooter(
                declarationText = "This is a preview document",
                notes = null
            )
        )
    }
}