package com.verity.feature.invoice.draft

import org.junit.Assert.assertEquals
import org.junit.Test
import com.verity.feature.invoice.draft.InvoiceDraftReducer
import com.verity.feature.invoice.draft.InvoiceDraftUiState
import com.verity.feature.invoice.draft.DraftLineItem
import com.verity.feature.invoice.draft.DraftTransportDetails
import com.verity.feature.invoice.draft.DraftAddress
import com.verity.feature.invoice.draft.DraftDocumentType
import com.verity.feature.invoice.draft.DraftTaxMode

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
    fun `a line item with no quantity contributes its rate once to the subtotal`() {
        val draft = InvoiceDraftUiState(
            billedTo = testBilledTo(),
            lineItems = listOf(
                DraftLineItem(
                    description = "Fabrication job work",
                    hsnCode = "9988",
                    quantity = null,
                    unit = "",
                    ratePaise = 7_500
                ),
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

        assertEquals(7_500 + 10 * 1_000, result.summary.subtotalPaise)
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

    @Test
    fun `insertLineItemAt restores a removed item at its original index`() {
        val itemA = DraftLineItem(description = "Item A", hsnCode = "1001", quantity = 10, unit = "PCS", ratePaise = 1_000)
        val itemB = DraftLineItem(description = "Item B", hsnCode = "1002", quantity = 5, unit = "PCS", ratePaise = 2_000)
        val itemC = DraftLineItem(description = "Item C", hsnCode = "1003", quantity = 1, unit = "PCS", ratePaise = 3_000)

        val draft = InvoiceDraftUiState(
            billedTo = testBilledTo(),
            lineItems = listOf(itemA, itemB, itemC)
        )

        val afterRemoval = InvoiceDraftReducer.removeLineItem(draft, index = 1)
        assertEquals(listOf(itemA, itemC), afterRemoval.lineItems)

        val afterUndo = InvoiceDraftReducer.insertLineItemAt(afterRemoval, index = 1, item = itemB)
        assertEquals(listOf(itemA, itemB, itemC), afterUndo.lineItems)
    }

    @Test
    fun `insertLineItemAt clamps an out-of-range index to the end of the list`() {
        val itemA = DraftLineItem(description = "Item A", hsnCode = "1001", quantity = 10, unit = "PCS", ratePaise = 1_000)
        val itemB = DraftLineItem(description = "Item B", hsnCode = "1002", quantity = 5, unit = "PCS", ratePaise = 2_000)

        val draft = InvoiceDraftUiState(billedTo = testBilledTo(), lineItems = listOf(itemA))

        val result = InvoiceDraftReducer.insertLineItemAt(draft, index = 99, item = itemB)

        assertEquals(listOf(itemA, itemB), result.lineItems)
    }

    @Test
    fun `reset returns an empty draft regardless of prior state`() {
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
            ),
            transportDetails = DraftTransportDetails(freightPaise = 5_000)
        )

        val result = InvoiceDraftReducer.reset()

        assertEquals(InvoiceDraftUiState(), result)
        assertEquals(0, result.lineItems.size)
        assertEquals(null, result.billedTo)
        assertEquals(null, result.transportDetails)
    }

    @Test
    fun `inter-state invoice applies IGST when buyer state code differs from seller`() {
        val draft = InvoiceDraftUiState(
            billedTo = testBilledTo(stateCode = "27", gstin = "27AAACB1234Z1Z"),
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

        assertEquals(DraftTaxMode.INTER_STATE, tax.mode)
        assertEquals(18L, tax.igst!!.ratePercent)
        assertEquals(1_800, tax.igst!!.amountPaise)
    }

    // Regression test for a real-world bug: a buyer with the seller's own state code (09,
    // Uttar Pradesh) was being taxed as inter-state (IGST) because the reducer compared against
    // a stale hardcoded seller state ("27") instead of the seller's real GSTIN state.
    @Test
    fun `buyer sharing the seller's state code applies CGST and SGST, not IGST`() {
        val draft = InvoiceDraftUiState(
            billedTo = testBilledTo(stateCode = "09", gstin = "09AAACB1234Z1Z"),
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
        assertEquals(900, tax.cgst!!.amountPaise)
        assertEquals(900, tax.sgst!!.amountPaise)
    }

    @Test
    fun `setJobWorkFlow toggles independently of other draft fields`() {
        val draft = InvoiceDraftUiState(
            documentType = DraftDocumentType.CHALLAN,
            billedTo = testBilledTo(),
            lineItems = listOf(
                DraftLineItem(description = "Item A", hsnCode = "1001", quantity = 10, unit = "PCS", ratePaise = 1_000)
            )
        )

        val enabled = InvoiceDraftReducer.setJobWorkFlow(draft, true)
        assertEquals(true, enabled.isJobWorkFlow)
        assertEquals(draft.lineItems, enabled.lineItems)
        assertEquals(draft.billedTo, enabled.billedTo)

        val disabled = InvoiceDraftReducer.setJobWorkFlow(enabled, false)
        assertEquals(false, disabled.isJobWorkFlow)
    }

    @Test
    fun `setDocumentType clears isJobWorkFlow and inboundChallanReference when leaving Challan`() {
        val draft = InvoiceDraftReducer.setInboundChallanReference(
            InvoiceDraftReducer.setJobWorkFlow(
                InvoiceDraftUiState(documentType = DraftDocumentType.CHALLAN, billedTo = testBilledTo()),
                true
            ),
            DraftInboundChallanReference(challanNumber = "CUST-CH-0042")
        )
        assertEquals(true, draft.isJobWorkFlow)

        val result = InvoiceDraftReducer.setDocumentType(draft, DraftDocumentType.INVOICE)

        assertEquals(false, result.isJobWorkFlow)
        assertEquals(null, result.inboundChallanReference)
    }

    @Test
    fun `setDocumentType always resets isJobWorkFlow even when staying on Challan`() {
        // The "Challan + Invoice" dropdown item calls setDocumentType(CHALLAN) then a follow-up
        // setJobWorkFlow(true) - plain "Challan" only calls the first, so switching from
        // "Challan + Invoice" to plain "Challan" must not leave isJobWorkFlow stuck true.
        val draft = InvoiceDraftReducer.setJobWorkFlow(
            InvoiceDraftUiState(documentType = DraftDocumentType.CHALLAN, billedTo = testBilledTo()),
            true
        )

        val result = InvoiceDraftReducer.setDocumentType(draft, DraftDocumentType.CHALLAN)

        assertEquals(false, result.isJobWorkFlow)
    }

    @Test
    fun `setDocumentType preserves inboundChallanReference when staying on Challan`() {
        val draft = InvoiceDraftReducer.setInboundChallanReference(
            InvoiceDraftUiState(documentType = DraftDocumentType.CHALLAN, billedTo = testBilledTo()),
            DraftInboundChallanReference(challanNumber = "CUST-CH-0042")
        )

        val result = InvoiceDraftReducer.setDocumentType(draft, DraftDocumentType.CHALLAN)

        assertEquals("CUST-CH-0042", result.inboundChallanReference?.challanNumber)
    }

    @Test
    fun `a job-work Challan draft still has tax nulled out`() {
        val draft = InvoiceDraftReducer.setJobWorkFlow(
            InvoiceDraftUiState(
                documentType = DraftDocumentType.CHALLAN,
                billedTo = testBilledTo(),
                lineItems = listOf(
                    DraftLineItem(description = "Item A", hsnCode = "1001", quantity = 10, unit = "PCS", ratePaise = 1_000)
                )
            ),
            true
        )

        val result = InvoiceDraftReducer.updateLineItem(draft, index = 0, item = draft.lineItems.first())

        assertEquals(null, result.summary.tax)
        assertEquals(0L, result.summary.taxTotalPaise)
    }

    @Test
    fun `setJobWorkChallanLink attaches the carried-forward Challan reference`() {
        val draft = InvoiceDraftUiState(documentType = DraftDocumentType.INVOICE, billedTo = testBilledTo())
        val link = DraftJobWorkChallanLink(
            reservedInvoiceNumber = "INV-000043",
            challanDocumentNumber = "CH-000012",
            challanDate = java.time.LocalDate.of(2026, 9, 16)
        )

        val result = InvoiceDraftReducer.setJobWorkChallanLink(draft, link)

        assertEquals(link, result.jobWorkChallanLink)
    }

    private fun testBilledTo(
        stateCode: String = "09",
        gstin: String = "09AAACB1234Z1Z"
    ): DraftAddress =
        DraftAddress(
            name = "Test Buyer",
            addressLine1 = "Test Address",
            city = "Muzaffarnagar",
            state = "Uttar Pradesh",
            stateCode = stateCode,
            gstin = gstin,
            pincode = "251001"
        )
}