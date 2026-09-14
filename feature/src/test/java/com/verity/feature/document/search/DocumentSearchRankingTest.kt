package com.verity.feature.document.search

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
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DocumentSearchRankingTest {

    @Test
    fun `a document number match outranks everything else and carries no snippet`() {
        val outcome = DocumentSearchRanking.match(
            documentNumber = "INV-000045",
            customerName = "Acme Traders",
            document = testDocument(),
            normalizedQuery = "inv-000045"
        )

        assertEquals(0, outcome.priority)
        assertEquals(DocumentMatchKind.DOCUMENT_NUMBER, outcome.matchKind)
        assertNull(outcome.snippet)
    }

    @Test
    fun `a customer name match ranks above line item content and carries no snippet`() {
        val outcome = DocumentSearchRanking.match(
            documentNumber = "INV-000045",
            customerName = "Acme Traders",
            document = testDocument(),
            normalizedQuery = "acme"
        )

        assertEquals(1, outcome.priority)
        assertEquals(DocumentMatchKind.CUSTOMER, outcome.matchKind)
        assertNull(outcome.snippet)
    }

    @Test
    fun `a party GSTIN match counts as a customer match`() {
        val outcome = DocumentSearchRanking.match(
            documentNumber = "INV-000045",
            customerName = "Acme Traders",
            document = testDocument(),
            normalizedQuery = "27aaacb1234z1z"
        )

        assertEquals(DocumentMatchKind.CUSTOMER, outcome.matchKind)
    }

    @Test
    fun `a line item description match produces a highlighted snippet`() {
        val outcome = DocumentSearchRanking.match(
            documentNumber = "INV-000045",
            customerName = "Acme Traders",
            document = testDocument(),
            normalizedQuery = "cement"
        )

        assertEquals(2, outcome.priority)
        assertEquals(DocumentMatchKind.LINE_ITEM, outcome.matchKind)
        val snippet = requireNotNull(outcome.snippet)
        assertEquals(
            "cement",
            snippet.text.substring(snippet.matchRange.first, snippet.matchRange.last + 1).lowercase()
        )
    }

    @Test
    fun `a vehicle number match is a transport match, ranked below line items`() {
        val outcome = DocumentSearchRanking.match(
            documentNumber = "INV-000045",
            customerName = "Acme Traders",
            document = testDocument(),
            normalizedQuery = "mh12ab3456"
        )

        assertEquals(3, outcome.priority)
        assertEquals(DocumentMatchKind.TRANSPORT, outcome.matchKind)
        assertEquals("MH12AB3456", outcome.snippet?.text)
    }

    @Test
    fun `a query that matches nothing checked falls back to OTHER with no snippet`() {
        val outcome = DocumentSearchRanking.match(
            documentNumber = "INV-000045",
            customerName = "Acme Traders",
            document = testDocument(),
            normalizedQuery = "maharashtra"
        )

        assertEquals(4, outcome.priority)
        assertEquals(DocumentMatchKind.OTHER, outcome.matchKind)
        assertNull(outcome.snippet)
    }

    @Test
    fun `buildSnippet trims long text with an ellipsis on the truncated side`() {
        val longText = "A".repeat(50) + "needle" + "B".repeat(50)

        val snippet = requireNotNull(DocumentSearchRanking.buildSnippet(longText, "needle", contextChars = 5))

        assertEquals("…AAAAAneedleBBBBB…", snippet.text)
    }

    @Test
    fun `buildSnippet returns null when the query is absent`() {
        assertNull(DocumentSearchRanking.buildSnippet("Portland Cement", "steel"))
    }

    private fun testDocument(): InvoiceDocumentModel = InvoiceDocumentModel(
        identity = DocumentIdentity(
            documentType = DocumentType.INVOICE,
            documentNumber = "INV-000045",
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
                addressLines = listOf("12 Market Road"),
                state = "Maharashtra",
                stateCode = "27"
            ),
            shippedTo = DocumentParty(
                name = "Acme Traders",
                gstin = "27AAACB1234Z1Z",
                addressLines = listOf("12 Market Road"),
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
