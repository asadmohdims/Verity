package com.verity.core.replay.document

/**
 * SimpleDocumentIndexReplayEngine
 *
 * PURPOSE
 * -------
 * Default deterministic implementation of DocumentIndexReplayEngine.
 *
 * Applies document index mutations sequentially to derive the
 * final in-memory DocumentIndexState.
 *
 * CONSTRAINTS
 * -----------
 * • Pure computation only
 * • Throws on invalid mutation sequences
 * • No partial state on failure
 */
class SimpleDocumentIndexReplayEngine : DocumentIndexReplayEngine {

    override fun replay(mutations: List<DocumentIndexMutation>): DocumentIndexState {
        var state = DocumentIndexState.empty()

        for (mutation in mutations) {
            state = applyMutation(state, mutation)
        }

        return state
    }

    private fun applyMutation(
        state: DocumentIndexState,
        mutation: DocumentIndexMutation
    ): DocumentIndexState {

        return when (mutation) {
            is DocumentIndexMutation.UpsertDocument -> {
                val row = DocumentIndexRow(
                    documentId = mutation.documentId,
                    documentType = mutation.documentType,
                    documentNumber = mutation.documentNumber,
                    customerId = mutation.customerId,
                    customerName = mutation.customerName,
                    documentDate = mutation.documentDate,
                    totalAmount = mutation.totalAmount,
                    status = mutation.status,
                    orgId = mutation.orgId
                )

                state.copy(
                    documentsById = state.documentsById + (mutation.documentId to row)
                )
            }

            is DocumentIndexMutation.UpdateStatus -> {
                val existing = state.documentsById[mutation.documentId]
                    ?: throw IllegalStateException(
                        "Cannot update status for non-existent documentId=${mutation.documentId}"
                    )

                val updated = existing.copy(
                    status = mutation.status
                )

                state.copy(
                    documentsById = state.documentsById + (mutation.documentId to updated)
                )
            }
        }
    }
}
