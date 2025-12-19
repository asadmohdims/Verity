package com.verity.core.replay.ledger

/**
 * SimpleLedgerReplayEngine
 *
 * PURPOSE
 * -------
 * Minimal, deterministic implementation of LedgerReplayEngine.
 *
 * Replays financial events from scratch and derives
 * customer balances and ledger entries.
 *
 * CONSTRAINTS
 * -----------
 * • Pure function: no side effects
 * • Deterministic: same input yields same output
 * • No platform, database, or UI dependencies
 * • Financial meaning derived strictly from eventType
 */
class SimpleLedgerReplayEngine : LedgerReplayEngine {

    override fun replay(events: List<ReplayInput>): LedgerState {
        if (events.isEmpty()) {
            return LedgerState.empty()
        }

        val balances = mutableMapOf<String, Long>()
        val entries = mutableListOf<LedgerEntry>()

        for (event in events) {
            val customerId = event.customerId
            val currentBalance = balances[customerId] ?: 0L

            when (event.eventType) {
                "InvoiceFinalized" -> {
                    val newBalance = currentBalance + event.amount
                    balances[customerId] = newBalance

                    entries += LedgerEntry(
                        eventId = event.eventId,
                        customerId = customerId,
                        amount = event.amount,
                        occurredAt = event.occurredAt,
                        type = EntryType.DEBIT
                    )
                }

                "PaymentRecorded" -> {
                    val newBalance = currentBalance - event.amount
                    balances[customerId] = newBalance

                    entries += LedgerEntry(
                        eventId = event.eventId,
                        customerId = customerId,
                        amount = event.amount,
                        occurredAt = event.occurredAt,
                        type = EntryType.CREDIT
                    )
                }

                else -> {
                    // Non-financial events are intentionally ignored by the ledger.
                }
            }
        }

        return LedgerState(
            balancesByCustomer = balances.toMap(),
            entries = entries.toList()
        )
    }
}