package com.verity.feature.document

import com.verity.core.document.model.DocumentType
import com.verity.core.formatting.money.Money
import com.verity.feature.home.DocumentSummary
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Pure-function coverage for DocumentsListUiState.visibleDocuments — no ViewModel/coroutines
 * needed since filtering is deterministic derived state, per CLAUDE.md's testing standards.
 */
class DocumentsListUiStateTest {

    private fun document(id: String, type: DocumentType) = DocumentSummary(
        documentId = id,
        documentNumber = if (type == DocumentType.INVOICE) "INV-$id" else "CH-$id",
        customerName = "Test Buyer",
        documentType = type,
        issueDate = LocalDate.of(2026, 9, 14),
        grandTotal = Money.ofPaise(10_000),
        finalizedAtEpochMillis = 0L
    )

    private val invoice = document("1", DocumentType.INVOICE)
    private val challan = document("2", DocumentType.CHALLAN)

    @Test
    fun `ALL filter shows every document`() {
        val state = DocumentsListUiState(documents = listOf(invoice, challan), filter = DocumentTypeFilter.ALL)

        assertEquals(listOf(invoice, challan), state.visibleDocuments)
    }

    @Test
    fun `INVOICES filter hides challans`() {
        val state = DocumentsListUiState(documents = listOf(invoice, challan), filter = DocumentTypeFilter.INVOICES)

        assertEquals(listOf(invoice), state.visibleDocuments)
    }

    @Test
    fun `CHALLANS filter hides invoices`() {
        val state = DocumentsListUiState(documents = listOf(invoice, challan), filter = DocumentTypeFilter.CHALLANS)

        assertEquals(listOf(challan), state.visibleDocuments)
    }
}
