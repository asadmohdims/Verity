package com.verity.core.document.search

import com.verity.core.document.model.DocumentFooter
import com.verity.core.document.model.DocumentIdentity
import com.verity.core.document.model.DocumentLineItem
import com.verity.core.document.model.DocumentLogistics
import com.verity.core.document.model.DocumentParties
import com.verity.core.document.model.DocumentParty
import com.verity.core.document.model.DocumentTotals
import com.verity.core.document.model.DocumentType
import com.verity.core.document.model.InvoiceDocumentModel
import com.verity.core.document.model.SellerDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DocumentSearchIndexTextTest {

    @Test
    fun `flattens document number, party, line item, and logistics fields`() {
        val text = buildSearchIndexText(testDocument())

        assertTrue(text.contains("inv-000001"))
        assertTrue(text.contains("acme traders"))
        assertTrue(text.contains("27aaacb1234z1z"))
        assertTrue(text.contains("portland cement"))
        assertTrue(text.contains("2523"))
        assertTrue(text.contains("mh12ab3456"))
        assertTrue(text.contains("lr-9081"))
        assertTrue(text.contains("eway-771122"))
    }

    @Test
    fun `includes the grand total as a plain rupee string, not grouped`() {
        val text = buildSearchIndexText(testDocument())

        assertTrue(text.contains("45760"))
        assertTrue(!text.contains("45,760"))
    }

    @Test
    fun `is lowercased regardless of source casing`() {
        val text = buildSearchIndexText(testDocument())

        assertEquals(text, text.lowercase())
    }

    @Test
    fun `tolerates absent logistics`() {
        val document = testDocument().copy(logistics = null)

        val text = buildSearchIndexText(document)

        assertTrue(text.contains("acme traders"))
    }

    private fun testDocument(): InvoiceDocumentModel = InvoiceDocumentModel(
        identity = DocumentIdentity(
            documentType = DocumentType.INVOICE,
            documentNumber = "INV-000001",
            issueDate = LocalDate.of(2026, 9, 14),
            seller = SellerDetails(
                name = "Unitech Machineries",
                gstin = "27AAACU1234Z1Z",
                addressLine1 = "Plot 1",
                addressLine2 = null,
                city = "Mumbai",
                state = "Maharashtra",
                stateCode = "27",
                pincode = "400001"
            ),
            placeOfSupplyState = "Maharashtra",
            placeOfSupplyStateCode = "27"
        ),
        parties = DocumentParties(
            billedTo = DocumentParty(
                name = "Acme Traders",
                gstin = "27AAACB1234Z1Z",
                addressLines = listOf("12 Market Road", "Andheri"),
                state = "Maharashtra",
                stateCode = "27"
            ),
            shippedTo = DocumentParty(
                name = "Acme Traders",
                gstin = "27AAACB1234Z1Z",
                addressLines = listOf("12 Market Road", "Andheri"),
                state = "Maharashtra",
                stateCode = "27"
            )
        ),
        lineItems = listOf(
            DocumentLineItem(
                description = "Portland Cement",
                hsnCode = "2523",
                quantity = 50,
                unit = "BAG",
                ratePaise = 40_000,
                amountPaise = 2_000_000
            )
        ),
        logistics = DocumentLogistics(
            transporterName = "Speed Carriers",
            vehicleNumber = "MH12AB3456",
            supplyDate = LocalDate.of(2026, 9, 14),
            grOrLrNumber = "LR-9081",
            freightPaise = 50_000,
            notes = null,
            ewayBillNumber = "EWAY-771122"
        ),
        taxation = null,
        totals = DocumentTotals(
            itemsSubtotalPaise = 2_000_000,
            freightPaise = 50_000,
            taxTotalPaise = 526_000,
            grandTotalPaise = 4_576_000
        ),
        footer = DocumentFooter(
            declarationText = "We declare that this invoice shows the actual price.",
            notes = null
        )
    )
}
