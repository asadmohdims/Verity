package com.verity.core.replay.document

/**
 * DocumentIndexMutation
 *
 * PURPOSE
 * -------
 * Represents a validated, deterministic mutation intent for the
 * Document Index replay engine.
 *
 * Instances of this type are produced exclusively by the
 * DocumentIndexEventMapper after successful validation of
 * domain events. The replay engine consumes these mutations to
 * derive the current DocumentIndexState.
 *
 * INTENT
 * ------
 * • Separate event parsing from replay logic
 * • Encode only what replay needs (no raw payloads)
 * • Guarantee deterministic, idempotent application
 *
 * CONSTRAINTS
 * -----------
 * • Immutable
 * • Projection-specific (must not be reused by other domains)
 * • Free of persistence and UI concerns
 */
sealed class DocumentIndexMutation {

    /**
     * Identifier of the document affected by this mutation.
     */
    abstract val documentId: String

    /**
     * Type of the document (e.g., INVOICE, CHALLAN).
     */
    abstract val documentType: String

    /**
     * Business time when the originating event occurred.
     *
     * Used for deterministic ordering during replay.
     */
    abstract val occurredAt: Long

    /**
     * Globally unique identifier of the originating event.
     *
     * Used as a stable tie-breaker when multiple events share
     * the same occurredAt timestamp.
     */
    abstract val eventId: String

    /**
     * UpsertDocument
     *
     * Represents insertion or replacement of a document index row.
     * Used for document creation events such as InvoiceFinalized
     * and ChallanIssued.
     */
    data class UpsertDocument(
        override val documentId: String,
        override val documentType: String,
        val documentNumber: String,
        val customerId: String,
        val customerName: String,
        val documentDate: Long,
        val totalAmount: Long,
        val status: String,
        val orgId: String,
        override val occurredAt: Long,
        override val eventId: String
    ) : DocumentIndexMutation()

    /**
     * UpdateStatus
     *
     * Represents a lifecycle status change for an existing document.
     * Used for cancellation events such as InvoiceCancelled and
     * ChallanCancelled.
     */
    data class UpdateStatus(
        override val documentId: String,
        override val documentType: String,
        val status: String,
        override val occurredAt: Long,
        override val eventId: String
    ) : DocumentIndexMutation()
}
