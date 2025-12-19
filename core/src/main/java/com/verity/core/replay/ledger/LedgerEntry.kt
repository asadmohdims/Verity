package com.verity.core.replay.ledger

/**
 * LedgerEntry
 *
 * PURPOSE
 * -------
 * Represents a single financial effect derived from a domain event.
 *
 * CONSTRAINTS
 * -----------
 * • Immutable
 * • Derived from exactly one event
 * • No business logic
 */
data class LedgerEntry(
    val eventId: String,
    val customerId: String,
    val amount: Long,
    val occurredAt: Long,
    val type: EntryType
)

enum class EntryType {
    DEBIT,   // Invoice, charge
    CREDIT   // Payment
}