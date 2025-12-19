package com.verity.core.replay.ledger

/**
 * LedgerState
 *
 * PURPOSE
 * -------
 * Represents the derived financial truth after replaying
 * all relevant financial domain events.
 *
 * This state is fully rebuildable from the event history and
 * must never be persisted as a source of truth.
 *
 * INTENT
 * ------
 * • Hold customer-level balances
 * • Hold a chronological ledger view for audit and validation
 *
 * CONSTRAINTS
 * -----------
 * • Immutable
 * • Derived only via replay
 * • No business logic
 */
data class LedgerState(

    /**
     * Net balance per customer.
     *
     * Positive  -> customer owes money
     * Zero      -> settled
     * Negative  -> customer credit (advance)
     */
    val balancesByCustomer: Map<String, Long>,

    /**
     * Chronological ledger entries derived from events.
     *
     * Used for audit, debugging, and replay verification.
     */
    val entries: List<LedgerEntry>
) {

    companion object {

        /**
         * Empty ledger state before any events are applied.
         */
        fun empty(): LedgerState =
            LedgerState(
                balancesByCustomer = emptyMap(),
                entries = emptyList()
            )
    }
}