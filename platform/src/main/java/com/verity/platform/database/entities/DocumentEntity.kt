package com.verity.platform.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * DocumentEntity
 *
 * A finalized Invoice or Challan. One table for both document types (matches
 * InvoiceDocumentModel's own DocumentType unification) rather than parallel schemas.
 *
 * Immutable once written: rows are never updated, only inserted. `payloadJson` holds the full
 * serialized InvoiceDocumentModel so the document can be re-rendered exactly as finalized,
 * regardless of later changes to seller/customer data elsewhere. `sequenceNumber` and
 * `documentNumber` are assigned once at finalize time and never change afterward.
 */
@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey
    val documentId: String,

    val orgId: String,

    /** "INVOICE" or "CHALLAN". */
    val documentType: String,

    /** Pure integer, scoped per org+documentType. Drives numbering; never parsed from a string. */
    val sequenceNumber: Long,

    /** Formatted display string assigned once at finalize time, e.g. "INV-000001". */
    val documentNumber: String,

    val customerId: String,

    /** Denormalized snapshot for display without joining against customers. */
    val customerName: String,

    val issueDateEpochDay: Long,

    val grandTotalPaise: Long,

    /** Challan<->Invoice linkage. Not written or read in Milestone 1. */
    val linkedDocumentId: String?,

    /** Full serialized InvoiceDocumentModel. */
    val payloadJson: String,

    val finalizedAt: Long
)
