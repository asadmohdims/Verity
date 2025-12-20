package com.verity.platform.database.projection

import com.verity.platform.database.dao.EventDao
import com.verity.platform.database.dao.LedgerBalanceDao
import com.verity.platform.database.entities.LedgerBalanceEntity
import com.verity.platform.database.mapping.FinancialEventMapper
import com.verity.platform.database.mapping.FinancialMappingResult
import com.verity.core.replay.ledger.LedgerReplayEngine
import com.verity.core.replay.ledger.ReplayInput

/**
 * LedgerProjectionWriter
 *
 * PURPOSE
 * -------
 * Coordinates incremental replay of financial events into the
 * ledger balance projection.
 *
 * This component is the sole orchestrator that:
 * • Reads immutable events
 * • Maps them through financial semantics
 * • Replays deterministic state
 * • Persists the resulting projection atomically
 *
 * INTENT
 * ------
 * • Keep ledger_balance in sync with domain events
 * • Avoid full replay on every invocation
 * • Fail loudly on invalid financial history
 *
 * CONSTRAINTS
 * -----------
 * • This class must not contain business logic
 * • It must not be invoked from UI layers directly
 * • All writes must be atomic and replay-driven
 * • Cursor must advance monotonically
 */
class LedgerProjectionWriter(
    private val eventDao: EventDao,
    private val ledgerBalanceDao: LedgerBalanceDao,
    private val financialEventMapper: FinancialEventMapper,
    private val replayEngine: LedgerReplayEngine,
    private val now: () -> Long
) {

    /**
     * Runs incremental ledger projection replay.
     *
     * Safe to call multiple times; if no new financial events
     * exist after the current cursor, this method is a no-op.
     *
     * @throws IllegalStateException if invalid financial history is detected
     */
    suspend fun runIncremental(orgId: String) {
        val currentCursor = readGlobalCursor()

        val events = eventDao.getEventsAfter(
            orgId = orgId,
            occurredAfter = currentCursor.lastAppliedOccurredAt
        )

        if (events.isEmpty()) return

        val replayInputs = mutableListOf<ReplayInput>()
        var maxOccurredAt = currentCursor.lastAppliedOccurredAt
        var maxEventId = currentCursor.lastAppliedEventId

        for (event in events) {
            when (val result = financialEventMapper.map(event)) {
                is FinancialMappingResult.NonFinancial -> Unit
                is FinancialMappingResult.Invalid -> {
                    throw IllegalStateException(
                        "Invalid financial event encountered: ${result.reason}"
                    )
                }
                is FinancialMappingResult.Financial -> {
                    replayInputs += result.replayInput

                    if (
                        event.occurredAt > maxOccurredAt ||
                        (event.occurredAt == maxOccurredAt && event.eventId > maxEventId)
                    ) {
                        maxOccurredAt = event.occurredAt
                        maxEventId = event.eventId
                    }
                }
            }
        }

        if (replayInputs.isEmpty()) return

        val updatedState = replayEngine.replay(replayInputs)

        val updatedRows = updatedState.balancesByCustomer.map { (customerId, balance) ->
            LedgerBalanceEntity(
                customerId = customerId,
                balance = balance,
                lastAppliedOccurredAt = maxOccurredAt,
                lastAppliedEventId = maxEventId,
                updatedAt = now()
            )
        }

        ledgerBalanceDao.replaceAll(updatedRows)
    }

    private suspend fun readGlobalCursor(): Cursor {
        val anyRow = ledgerBalanceDao.getAllBalances().firstOrNull()

        return if (anyRow == null) {
            Cursor(0L, "")
        } else {
            Cursor(
                lastAppliedOccurredAt = anyRow.lastAppliedOccurredAt,
                lastAppliedEventId = anyRow.lastAppliedEventId
            )
        }
    }

    private data class Cursor(
        val lastAppliedOccurredAt: Long,
        val lastAppliedEventId: String
    )
}
