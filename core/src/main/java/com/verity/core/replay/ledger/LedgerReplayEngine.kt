package com.verity.core.replay.ledger

/**
 * LedgerReplayEngine
 *
 * PURPOSE
 * -------
 * Computes derived financial truth by replaying a sequence
 * of financial domain events.
 *
 * The engine is pure: given the same inputs, it must always
 * produce the same LedgerState.
 *
 * CONSTRAINTS
 * -----------
 * • No side effects
 * • No dependency on storage, platform, or UI
 * • Deterministic and repeatable
 */
interface LedgerReplayEngine {

    /**
     * Replays the given events from scratch and returns
     * the resulting LedgerState.
     *
     * @param events financial events ordered by occurredAt
     * @return derived ledger state
     */
    fun replay(events: List<ReplayInput>): LedgerState
}