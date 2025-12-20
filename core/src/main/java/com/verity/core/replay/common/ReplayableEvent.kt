package com.verity.core.replay.common

/**
 * ReplayableEvent
 *
 * PURPOSE
 * -------
 * Defines a platform-agnostic envelope for domain events that can be
 * replayed by core engines (ledger, document index, etc.).
 *
 * This abstraction deliberately decouples core replay logic from
 * persistence technologies (e.g., Room) and platform concerns.
 *
 * INTENT
 * ------
 * • Allow core to interpret events without depending on platform modules
 * • Preserve deterministic replay semantics across environments
 * • Enable JVM-only testing of replay logic
 *
 * CONSTRAINTS
 * -----------
 * • Immutable contract
 * • No Android or persistence dependencies
 * • Implemented/adapted by platform layer
 */
interface ReplayableEvent {

    /**
     * Canonical event name (e.g., InvoiceFinalized, PaymentRecorded).
     */
    val eventType: String

    /**
     * Version of the event payload schema.
     */
    val eventVersion: Int

    /**
     * Business time when the event occurred.
     */
    val occurredAt: Long

    /**
     * Globally unique identifier of the event.
     */
    val eventId: String

    /**
     * Identifier of the organization to which this event belongs.
     */
    val orgId: String

    /**
     * Serialized event payload (typically JSON).
     */
    val payload: String
}
