package com.verity.core.replay.document

/**
 * DocumentIndexState
 *
 * PURPOSE
 * -------
 * Holds the derived, in-memory representation of all indexed
 * business documents during replay.
 *
 * This state is fully rebuildable from immutable domain events
 * and must never be persisted as a source of truth.
 *
 * INTENT
 * ------
 * • Provide a pure container for document index rows
 * • Enable deterministic replay and testing
 * • Support copy-on-write updates during replay
 *
 * CONSTRAINTS
 * -----------
 * • Immutable
 * • No business logic
 * • No persistence or cursor metadata
 */
data class DocumentIndexState(

    /**
     * Indexed documents keyed by documentId.
     */
    val documentsById: Map<String, DocumentIndexRow>
) {

    companion object {

        /**
         * Empty document index state before any events are applied.
         */
        fun empty(): DocumentIndexState =
            DocumentIndexState(
                documentsById = emptyMap()
            )
    }
}