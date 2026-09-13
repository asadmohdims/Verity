package com.verity.feature.home

import com.verity.core.formatting.money.Money
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Aggregated, display-ready shape of the Home dashboard.
 */
data class HomeDashboardData(
    val thisMonthTotal: Money,
    val thisMonthDocumentCount: Int,
    val thisMonthLabel: String,
    val recentDocuments: List<DocumentSummary>
)

private val monthLabelFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

/**
 * HomeDashboardCalculator
 *
 * Pure aggregation over already-loaded documents: no persistence, no coroutines, no Android
 * dependency — everything this needs is already true from HomeDataSource.loadAllDocuments().
 *
 * "This month" is scoped to the calendar month of [referenceDate], compared against each
 * document's issueDate (the business date, not finalizedAt) — matching what a user means by
 * "this month's invoicing". "Recent" is ordered by finalizedAtEpochMillis, since that's the one
 * field that can never be backdated.
 */
object HomeDashboardCalculator {

    const val DEFAULT_RECENT_LIMIT = 5

    fun buildDashboard(
        documents: List<DocumentSummary>,
        referenceDate: LocalDate,
        recentLimit: Int = DEFAULT_RECENT_LIMIT
    ): HomeDashboardData {
        val referenceMonth = YearMonth.from(referenceDate)

        val thisMonthDocuments = documents.filter { YearMonth.from(it.issueDate) == referenceMonth }
        val thisMonthTotalPaise = thisMonthDocuments.sumOf { it.grandTotal.raw }

        val recentDocuments = documents
            .sortedByDescending { it.finalizedAtEpochMillis }
            .take(recentLimit)

        return HomeDashboardData(
            thisMonthTotal = Money.ofPaise(thisMonthTotalPaise),
            thisMonthDocumentCount = thisMonthDocuments.size,
            thisMonthLabel = monthLabelFormatter.format(referenceMonth),
            recentDocuments = recentDocuments
        )
    }
}
