package com.verity.platform.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * DocumentIndexEntity
 *
 * PURPOSE
 * -------
 * Represents a searchable, query-optimized index of finalized
 * business documents (Invoices and Challans).
 *
 * This table is a deterministic, rebuildable projection derived
 * exclusively from immutable domain events. It exists purely
 * for read performance and navigation support.
 *
 * INTENT
 * ------
 * • Provide fast listing of documents without event replay
 * • Support deliberate search and filtering workflows
 * • Act as a stable navigation index into document detail
 *
 * CONSTRAINTS
 * -----------
 * • This table is NOT a source of truth
 * • Rows must never be mutated directly by UI or business logic
 * • All rows are derived only via replay of domain events
 * • Rows must never be deleted; status reflects lifecycle
 * • Loss or corruption of this table must not cause data loss
 */
@Entity(
    tableName = "document_index"
)
data class DocumentIndexEntity(

    /**
     * Globally unique identifier of the document.
     *
     * Maps to:
     *  - invoiceId for invoices
     *  - challanId for challans
     */
    @PrimaryKey
    val documentId: String,

    /**
     * Type of the indexed document.
     *
     * Allowed values:
     *  - INVOICE
     *  - CHALLAN
     */
    val documentType: String,

    /**
     * Human-readable document number.
     *
     * Examples:
     *  - INV-2025-0012
     *  - CH-2025-0007
     */
    val documentNumber: String,

    /**
     * Identifier of the customer associated with this document.
     */
    val customerId: String,

    /**
     * Snapshot of the customer name at the time of document
     * finalization or issuance.
     *
     * This value is intentionally denormalized to support
     * fast search and preserve historical accuracy.
     */
    val customerName: String,

    /**
     * Business date of the document.
     *
     * For invoices: invoice date
     * For challans: challan issue date
     */
    val documentDate: Long,

    /**
     * Final total amount of the document.
     *
     * For invoices: total payable amount
     * For challans: typically zero or informational
     */
    val totalAmount: Long,

    /**
     * Current lifecycle status of the document.
     *
     * Allowed values:
     *  - FINALIZED  (invoice)
     *  - ISSUED     (challan)
     *  - CANCELLED
     */
    val status: String,

    /**
     * Identifier of the organization to which this document belongs.
     */
    val orgId: String,

    /**
     * Global replay cursor — primary ordering key.
     *
     * Represents the maximum occurredAt timestamp that has been
     * applied to this projection.
     */
    val lastAppliedOccurredAt: Long,

    /**
     * Global replay cursor — deterministic tie-breaker.
     *
     * Used when multiple events share the same occurredAt value.
     */
    val lastAppliedEventId: String,

    /**
     * System time when this projection row was last updated.
     *
     * Used strictly for diagnostics and debugging.
     */
    val updatedAt: Long
)