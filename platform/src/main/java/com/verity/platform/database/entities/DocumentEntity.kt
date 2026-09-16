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

    val finalizedAt: Long,

    /**
     * Flattened, lowercase, space-joined text of every field worth matching a document search
     * against (see core's buildSearchIndexText) — customer/party name+GSTIN, line item
     * description+HSN, vehicle/GR-LR/e-way bill/transporter, grand total. Computed once at
     * finalize time, same as every other column here; added in schema v2 (see
     * platform/database/migrations/Migration1To2.kt) alongside a one-time backfill for rows
     * finalized before this column existed.
     */
    val searchIndexText: String,

    /**
     * Flips to true once FirebaseSyncClient's push succeeds (see platform/sync/). False here is
     * not HelloCredit's dead isSynced boolean: it's actually read by Home ("N pending"), and
     * retry is real because it's delegated to Firestore's own persistent offline queue rather
     * than reimplemented. Added in schema v3 — see Migration2To3.
     */
    val syncedToCloud: Boolean = false,

    /**
     * Private note for the document owner's own review (e.g. "customer paid partly in cash") —
     * never part of payloadJson, never reaches InvoicePdfRenderer, never printed. The one other
     * genuinely mutable column on this otherwise insert-only table besides syncedToCloud: unlike
     * the rest of this row, a note isn't a financial/legal fact, so Principle 5's immutability
     * rule doesn't apply to it — it stays editable from Document Detail after finalize. Added in
     * schema v5 — see Migration4To5.
     */
    val selfNotes: String? = null
)
