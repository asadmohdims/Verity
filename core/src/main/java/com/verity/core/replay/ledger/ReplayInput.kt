package com.verity.core.replay.ledger

/**
 * ReplayInput
 *
 * PURPOSE
 * -------
 * Minimal, domain-safe representation of a financial event
 * used by the ledger replay engine.
 *
 * This type intentionally hides storage and transport details
 * (Room, JSON, sync metadata).
 *
 * CONSTRAINTS
 * -----------
 * • Immutable
 * • Must be derivable from EventEntity
 * • Must not depend on database or platform code
 */
data class ReplayInput(
    val eventId: String,
    val eventType: String,
    val customerId: String,
    val amount: Long,
    val occurredAt: Long
)