package com.verity.platform.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * LedgerBalanceEntity
 *
 * PURPOSE
 * -------
 * Represents the current financial balance per customer as a
 * deterministic projection derived from immutable domain events.
 *
 * This table exists purely as a performance cache and may be
 * dropped and rebuilt at any time via replay.
 *
 * INTENT
 * ------
 * • Provide fast access to customer balances for UI and queries
 * • Avoid replaying the full event history on every read
 * • Act as a stable, explainable projection of financial truth
 *
 * CONSTRAINTS
 * -----------
 * • This table is NOT a source of truth
 * • Rows must never be mutated directly by UI or business logic
 * • All values must be derived exclusively from event replay
 * • Loss or corruption of this table must not cause data loss
 */
@Entity(
    tableName = "ledger_balance"
)
data class LedgerBalanceEntity(
    /**
     * Identifier of the customer whose balance this row represents.
     *
     * Exactly one row must exist per customer.
     */
    @PrimaryKey
    val customerId: String,

    /**
     * Deterministic ledger balance for the customer.
     *
     * Semantics:
     *  - Positive value → amount owed by the customer
     *  - Negative value → customer credit / advance
     *
     * This value is always derived from replaying
     * InvoiceFinalized and PaymentRecorded events.
     */
    val balance: Long,

    /**
     * Global replay cursor — primary ordering key.
     *
     * Represents the maximum occurredAt timestamp that has
     * been successfully applied to this projection.
     *
     * Used to determine which events must be replayed next.
     */
    val lastAppliedOccurredAt: Long,

    /**
     * Global replay cursor — deterministic tie-breaker.
     *
     * Used when multiple events share the same occurredAt
     * timestamp to guarantee stable replay ordering.
     */
    val lastAppliedEventId: String,

    /**
     * System time when this projection row was last updated.
     *
     * Used strictly for diagnostics and debugging.
     * Must not be used to infer business meaning.
     */
    val updatedAt: Long
)