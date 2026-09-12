package com.verity.feature.home

import com.verity.core.document.model.DocumentType
import com.verity.core.formatting.money.Money
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

private fun summary(
    documentId: String,
    documentNumber: String = documentId,
    customerName: String = "Customer",
    documentType: DocumentType = DocumentType.INVOICE,
    issueDate: LocalDate,
    grandTotalRupees: Long = 100,
    finalizedAtEpochMillis: Long
): DocumentSummary = DocumentSummary(
    documentId = documentId,
    documentNumber = documentNumber,
    customerName = customerName,
    documentType = documentType,
    issueDate = issueDate,
    grandTotal = Money.ofRupees(grandTotalRupees),
    finalizedAtEpochMillis = finalizedAtEpochMillis
)

class HomeDashboardCalculatorTest {

    private val september15 = LocalDate.of(2026, 9, 15)

    @Test
    fun `empty document list produces a zeroed dashboard`() {
        val dashboard = HomeDashboardCalculator.buildDashboard(
            documents = emptyList(),
            referenceDate = september15
        )

        assertEquals(Money.ofPaise(0), dashboard.thisMonthTotal)
        assertEquals(0, dashboard.thisMonthDocumentCount)
        assertEquals(emptyList<DocumentSummary>(), dashboard.recentDocuments)
    }

    @Test
    fun `only documents in the reference month are counted toward this month's total`() {
        val inSeptember = summary(
            documentId = "1",
            issueDate = LocalDate.of(2026, 9, 1),
            grandTotalRupees = 500,
            finalizedAtEpochMillis = 1
        )
        val alsoInSeptember = summary(
            documentId = "2",
            issueDate = LocalDate.of(2026, 9, 30),
            grandTotalRupees = 300,
            finalizedAtEpochMillis = 2
        )
        val inAugust = summary(
            documentId = "3",
            issueDate = LocalDate.of(2026, 8, 31),
            grandTotalRupees = 999,
            finalizedAtEpochMillis = 3
        )
        val inOctober = summary(
            documentId = "4",
            issueDate = LocalDate.of(2026, 10, 1),
            grandTotalRupees = 999,
            finalizedAtEpochMillis = 4
        )

        val dashboard = HomeDashboardCalculator.buildDashboard(
            documents = listOf(inSeptember, alsoInSeptember, inAugust, inOctober),
            referenceDate = september15
        )

        assertEquals(Money.ofRupees(800), dashboard.thisMonthTotal)
        assertEquals(2, dashboard.thisMonthDocumentCount)
    }

    @Test
    fun `recent documents are ordered by finalizedAt descending, not issueDate`() {
        val finalizedFirst = summary(
            documentId = "old",
            issueDate = LocalDate.of(2026, 9, 20),
            finalizedAtEpochMillis = 100
        )
        val finalizedLast = summary(
            documentId = "new",
            issueDate = LocalDate.of(2026, 9, 1),
            finalizedAtEpochMillis = 200
        )

        val dashboard = HomeDashboardCalculator.buildDashboard(
            documents = listOf(finalizedFirst, finalizedLast),
            referenceDate = september15
        )

        assertEquals(listOf("new", "old"), dashboard.recentDocuments.map { it.documentId })
    }

    @Test
    fun `recent documents are capped at the requested limit`() {
        val documents = (1..10).map {
            summary(
                documentId = it.toString(),
                issueDate = september15,
                finalizedAtEpochMillis = it.toLong()
            )
        }

        val dashboard = HomeDashboardCalculator.buildDashboard(
            documents = documents,
            referenceDate = september15,
            recentLimit = 3
        )

        assertEquals(3, dashboard.recentDocuments.size)
        assertEquals(listOf("10", "9", "8"), dashboard.recentDocuments.map { it.documentId })
    }
}
