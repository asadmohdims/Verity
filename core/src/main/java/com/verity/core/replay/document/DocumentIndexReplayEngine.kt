package com.verity.core.replay.document

/**
 * DocumentIndexReplayEngine
 *
 * PURPOSE
 * -------
 * Applies validated document index mutation intents to derive
 * the current DocumentIndexState.
 *
 * The replay engine is pure: given the same ordered mutations,
 * it must always produce the same state.
 *
 * CONSTRAINTS
 * -----------
 * • No side effects
 * • No persistence or platform dependencies
 * • Deterministic and repeatable
 */
interface DocumentIndexReplayEngine {

    /**
     * Replays the given document index mutations from scratch and
     * returns the resulting DocumentIndexState.
     *
     * @param mutations ordered list of validated mutation intents
     * @return derived document index state
     */
    fun replay(
        mutations: List<DocumentIndexMutation>
    ): DocumentIndexState
}