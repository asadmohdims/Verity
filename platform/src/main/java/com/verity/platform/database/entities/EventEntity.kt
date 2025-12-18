package com.verity.platform.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * EventEntity
 *
 * PURPOSE
 * -------
 * Represents an immutable, append-only record of a domain event.
 * This table is the primary source of truth for all financial and
 * domain history in Verity.
 *
 * INTENT
 * ------
 * • Persist every domain event exactly once
 * • Enable deterministic replay of system state
 * • Support auditability, recovery, and sync
 *
 * CONSTRAINTS
 * -----------
 * • Events must never be updated or deleted
 * • Corrections are represented only via new events
 * • This entity contains no business logic
 * • Interpretation of payload is owned by the domain layer
 */
@Entity(
    tableName = "events"
)
data class EventEntity(

    /**
     * Globally unique identifier for the event.
     *
     * Used to guarantee immutability, idempotent writes,
     * and safe replay (duplicate inserts are ignored).
     */
    @PrimaryKey
    val eventId: String,

    /**
     * Identifier of the organization to which this event belongs.
     *
     * Enables multi-organization support while allowing single-org
     * usage as a constant value in early versions.
     */
    val orgId: String,

    /**
     * Canonical event name.
     *
     * Examples:
     *  - InvoiceFinalized
     *  - PaymentRecorded
     *  - DocumentsLinked
     *
     * Used by the replay engine to dispatch to the correct handler.
     */
    val eventType: String,

    /**
     * Version of the event schema / semantic meaning.
     *
     * Allows safe evolution of events without rewriting history.
     * Replay logic must branch based on this version when needed.
     */
    val eventVersion: Int,

    /**
     * Business time when the event actually occurred.
     *
     * This represents domain truth (not persistence time) and is
     * used for deterministic ordering during replay.
     */
    val occurredAt: Long,

    /**
     * System time when the event was recorded locally.
     *
     * Used for diagnostics, sync ordering, and debugging.
     * Must not be used to infer business meaning.
     */
    val recordedAt: Long,

    /**
     * Serialized event payload (typically JSON).
     *
     * The structure and interpretation of this payload are owned
     * entirely by the domain layer and versioned via eventVersion.
     */
    val payload: String,

    /**
     * Source of the event.
     *
     * Typical values:
     *  - "local"  (user action on device)
     *  - "sync"   (replayed from cloud)
     *  - "import" (bulk or migration import)
     */
    val source: String,

    /**
     * Optional correlation identifier.
     *
     * Used to logically group related events that are part of the
     * same user or system workflow (e.g., invoice creation followed
     * by document linking).
     */
    val correlationId: String?
)