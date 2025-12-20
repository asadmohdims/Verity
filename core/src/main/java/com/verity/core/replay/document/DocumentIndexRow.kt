package com.verity.core.replay.document

/**
 * DocumentIndexRow
 *
 * PURPOSE
 * -------
 * Represents a single indexed business document within the
 * in-memory document index state.
 *
 * This row mirrors the document index projection schema without
 * any persistence or replay cursor metadata.
 *
 * CONSTRAINTS
 * -----------
 * • Immutable
 * • Derived only via replay
 * • Free of persistence and UI concerns
 */
data class DocumentIndexRow(

    /**
     * Globally unique identifier of the document.
     */
    val documentId: String,

    /**
     * Type of the document (e.g., INVOICE, CHALLAN).
     */
    val documentType: String,

    /**
     * Human-readable document number.
     */
    val documentNumber: String,

    /**
     * Identifier of the associated customer.
     */
    val customerId: String,

    /**
     * Snapshot of the customer name at the time of document
     * finalization or issuance.
     */
    val customerName: String,

    /**
     * Business date of the document.
     */
    val documentDate: Long,

    /**
     * Final total amount of the document.
     */
    val totalAmount: Long,

    /**
     * Current lifecycle status of the document.
     */
    val status: String,

    /**
     * Identifier of the organization to which this document belongs.
     */
    val orgId: String
)