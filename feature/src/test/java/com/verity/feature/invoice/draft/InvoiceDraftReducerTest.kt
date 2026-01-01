package com.verity.feature.invoice.draft

import org.junit.Assert.assertEquals
import org.junit.Test
import com.verity.invoice.draft.InvoiceDraftReducer
import com.verity.invoice.draft.InvoiceDraftUiState
import com.verity.invoice.draft.DraftLineItem
import com.verity.invoice.draft.DraftTransportDetails
import com.verity.invoice.draft.DraftAddress
import com.verity.invoice.draft.DraftDocumentType
import com.verity.invoice.draft.DraftTaxMode

class InvoiceDraftReducerTest {

    @Test
    fun `items subtotal equals sum of quantity multiplied by rate`() {
        val draft = InvoiceDraftUiState(
            billedTo = testBilledTo(),
            lineItems = listOf(
                DraftLineItem(
                    description = "Item A",
                    hsnCode = "1001",
                    quantity = 10,
                    unit = "PCS",
                    ratePaise = 1_000
                ),
                DraftLineItem(
                    description = "Item B",
                    hsnCode = "1002",
                    quantity = 5,
                    unit = "PCS",
                    ratePaise = 2_000
                )
            )
        )

        val result = InvoiceDraftReducer.addLineItem(
            draft,
            DraftLineItem(
                description = "Item C",
                hsnCode = "1003",
                quantity = 1,
                unit = "PCS",
                ratePaise = 0
            )
        )

        assertEquals(
            10 * 1_000 + 5 * 2_000,
            result.summary.subtotalPaise
        )
    }

    @Test
    fun `freight is added exactly once and not included in items subtotal`() {
        val draft = InvoiceDraftUiState(
            billedTo = testBilledTo(),
            lineItems = listOf(
                DraftLineItem(
                    description = "Item A",
                    hsnCode = "1001",
                    quantity = 10,
                    unit = "PCS",
                    ratePaise = 1_000
                )
            ),
            transportDetails = DraftTransportDetails(
                freightPaise = 5_000
            )
        )

        val result = InvoiceDraftReducer.updateLineItem(
            draft,
            index = 0,
            item = draft.lineItems.first()
        )

        // Items subtotal must exclude freight
        assertEquals(10 * 1_000, result.summary.subtotalPaise)

        // Taxable subtotal = items + freight = 15,000
        // CGST 9% + SGST 9% = 18% of 15,000 = 2,700
        assertEquals(2_700, result.summary.taxTotalPaise)

        // Grand total = 15,000 + 2,700 = 17,700
        assertEquals(17_700, result.summary.grandTotalPaise)
    }

    @Test
    fun `intra-state invoice applies CGST and SGST`() {
        val draft = InvoiceDraftUiState(
            billedTo = testBilledTo(),
            lineItems = listOf(
                DraftLineItem(
                    description = "Item A",
                    hsnCode = "1001",
                    quantity = 10,
                    unit = "PCS",
                    ratePaise = 1_000
                )
            )
        )

        val result = InvoiceDraftReducer.updateLineItem(
            draft,
            index = 0,
            item = draft.lineItems.first()
        )

        val tax = result.summary.tax!!

        assertEquals(DraftTaxMode.INTRA_STATE, tax.mode)
        assertEquals(9L, tax.cgst!!.ratePercent)
        assertEquals(9L, tax.sgst!!.ratePercent)
        assertEquals(900, tax.cgst!!.amountPaise)
        assertEquals(900, tax.sgst!!.amountPaise)
    }

    @Test
    fun `challan document type does not apply tax`() {
        val draft = InvoiceDraftUiState(
            documentType = DraftDocumentType.CHALLAN,
            billedTo = testBilledTo(),
            lineItems = listOf(
                DraftLineItem(
                    description = "Item A",
                    hsnCode = "1001",
                    quantity = 10,
                    unit = "PCS",
                    ratePaise = 1_000
                )
            )
        )

        val result = InvoiceDraftReducer.updateLineItem(
            draft,
            index = 0,
            item = draft.lineItems.first()
        )

        assertEquals(null, result.summary.tax)
        assertEquals(0L, result.summary.taxTotalPaise)
    }

    private fun testBilledTo(): DraftAddress =
        DraftAddress(
            name = "Test Buyer",
            addressLine1 = "Test Address",
            city = "Mumbai",
            state = "Maharashtra",
            stateCode = "27",
            pincode = "400001"
        )
}