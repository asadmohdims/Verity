package com.verity.feature.customer.rollup

import com.verity.core.formatting.money.Money
import com.verity.feature.home.DocumentSummary

/**
 * CustomerRollupDataSource
 *
 * Port for the minimal, read-only customer document rollup reached from a Documents search
 * customer match. Deliberately not the full Customer Detail/CRUD screen (edit, deactivate,
 * profile fields) — that stays Phase 2 of the UX roadmap; this only answers "what has this
 * customer been billed, and when".
 */
interface CustomerRollupDataSource {
    suspend fun loadRollup(customerId: String): CustomerRollup?
}

data class CustomerRollup(
    val customerId: String,
    val customerName: String,
    val documents: List<DocumentSummary>
) {
    val documentCount: Int get() = documents.size
    val runningTotal: Money get() = Money.ofPaise(documents.sumOf { it.grandTotal.raw })
}
