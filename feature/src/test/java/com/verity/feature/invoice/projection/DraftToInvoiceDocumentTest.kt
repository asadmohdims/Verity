package com.verity.feature.invoice.projection

import com.verity.core.document.model.SellerDetails
import com.verity.feature.invoice.draft.DraftAddress
import com.verity.feature.invoice.draft.DraftLineItem
import com.verity.feature.invoice.draft.DraftTransportDetails
import com.verity.feature.invoice.draft.InvoiceDraftUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class DraftToInvoiceDocumentTest {

    @Test
    fun `place of supply defaults to billed-to state when shipped-to is not set separately`() {
        val document = DraftToInvoiceDocument.project(
            draft = baseDraft(),
            documentNumber = "INV-000001",
            seller = testSeller(),
            clock = fixedClock()
        )

        assertEquals("Maharashtra", document.identity.placeOfSupplyState)
        assertEquals("27", document.identity.placeOfSupplyStateCode)
    }

    @Test
    fun `place of supply uses shipped-to state when it genuinely differs from billed-to`() {
        val draft = baseDraft().copy(
            shippedTo = testBilledTo().copy(
                name = "Warehouse Unit II",
                state = "Uttar Pradesh",
                stateCode = "09"
            )
        )

        val document = DraftToInvoiceDocument.project(
            draft = draft,
            documentNumber = "INV-000002",
            seller = testSeller(),
            clock = fixedClock()
        )

        assertEquals("Uttar Pradesh", document.identity.placeOfSupplyState)
        assertEquals("09", document.identity.placeOfSupplyStateCode)
    }

    @Test
    fun `e-way bill number maps through from transport details when present`() {
        val draft = baseDraft().copy(
            transportDetails = DraftTransportDetails(ewayBillNumber = "3412 1099 8877")
        )

        val document = DraftToInvoiceDocument.project(
            draft = draft,
            documentNumber = "INV-000003",
            seller = testSeller(),
            clock = fixedClock()
        )

        assertEquals("3412 1099 8877", document.logistics?.ewayBillNumber)
    }

    @Test
    fun `supply date maps through from transport details when present`() {
        val draft = baseDraft().copy(
            transportDetails = DraftTransportDetails(supplyDate = java.time.LocalDate.of(2026, 9, 20))
        )

        val document = DraftToInvoiceDocument.project(
            draft = draft,
            documentNumber = "INV-000006",
            seller = testSeller(),
            clock = fixedClock()
        )

        assertEquals(java.time.LocalDate.of(2026, 9, 20), document.logistics?.supplyDate)
    }

    @Test
    fun `a line item with no quantity has a null quantity and an amount equal to its rate`() {
        val draft = baseDraft().copy(
            lineItems = listOf(
                DraftLineItem(
                    description = "Fabrication job work",
                    hsnCode = "9988",
                    quantity = null,
                    unit = "",
                    ratePaise = 7_500
                )
            )
        )

        val document = DraftToInvoiceDocument.project(
            draft = draft,
            documentNumber = "INV-000007",
            seller = testSeller(),
            clock = fixedClock()
        )

        val item = document.lineItems.single()
        assertNull(item.quantity)
        assertEquals(7_500L, item.amountPaise)
    }

    @Test
    fun `logistics is null when no transport details were entered`() {
        val document = DraftToInvoiceDocument.project(
            draft = baseDraft(),
            documentNumber = "INV-000004",
            seller = testSeller(),
            clock = fixedClock()
        )

        assertNull(document.logistics)
    }

    @Test
    fun `reverse charge flag maps through from the draft`() {
        val draft = baseDraft().copy(reverseCharge = true)

        val document = DraftToInvoiceDocument.project(
            draft = draft,
            documentNumber = "INV-000005",
            seller = testSeller(),
            clock = fixedClock()
        )

        assertTrue(document.identity.reverseChargeApplicable)
    }

    private fun baseDraft(): InvoiceDraftUiState =
        InvoiceDraftUiState(
            billedTo = testBilledTo(),
            lineItems = listOf(
                DraftLineItem(
                    description = "Item A",
                    hsnCode = "1001",
                    quantity = 1,
                    unit = "PCS",
                    ratePaise = 1_000
                )
            )
        )

    private fun testBilledTo(): DraftAddress =
        DraftAddress(
            name = "Test Buyer",
            addressLine1 = "Test Address",
            city = "Mumbai",
            state = "Maharashtra",
            stateCode = "27",
            gstin = "27AAACB1234Z1Z",
            pincode = "400001"
        )

    private fun testSeller(): SellerDetails =
        SellerDetails(
            name = "Test Seller",
            gstin = "27AAAAA0000A1Z5",
            addressLine1 = "Seller Address",
            addressLine2 = null,
            city = "Mumbai",
            state = "Maharashtra",
            stateCode = "27",
            pincode = "400001"
        )

    private fun fixedClock(): Clock =
        Clock.fixed(Instant.parse("2026-09-14T00:00:00Z"), ZoneOffset.UTC)
}
