package com.verity.platform.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * LedgerEntryEntity
 *
 * Append-only financial ledger row, written transactionally alongside document finalization.
 * Rows are never updated or deleted. Customer balance is SUM(amountPaise) per customer — a plain
 * query, not a replay engine (see CLAUDE.md's Data & sync architecture for why).
 */
@Entity(tableName = "ledger_entries")
data class LedgerEntryEntity(
    @PrimaryKey
    val entryId: String,

    val orgId: String,

    val customerId: String,

    /** The document (invoice/payment) that generated this entry. */
    val documentId: String,

    /** Positive = debit (customer owes more). */
    val amountPaise: Long,

    val occurredAt: Long,

    val createdAt: Long,

    /** Same meaning and lifecycle as DocumentEntity.syncedToCloud — see that doc comment. */
    val syncedToCloud: Boolean = false
)
