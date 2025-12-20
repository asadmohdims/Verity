package com.verity.platform.replay

import com.verity.core.replay.common.ReplayableEvent
import com.verity.platform.database.entities.EventEntity

/**
 * PlatformReplayableEvent
 *
 * PURPOSE
 * -------
 * Adapts a platform-owned EventEntity (Room persistence model)
 * into a core-level ReplayableEvent for deterministic replay.
 *
 * This class is the ONLY allowed bridge between the platform
 * layer and core replay logic.
 *
 * INTENT
 * ------
 * • Preserve strict dependency direction (platform → core)
 * • Keep core free of Room / Android dependencies
 * • Enable JVM-only testing of core replay logic
 *
 * CONSTRAINTS
 * -----------
 * • Immutable view over EventEntity
 * • No transformation or inference
 * • One-to-one field mapping
 */
class PlatformReplayableEvent(
    private val entity: EventEntity
) : ReplayableEvent {

    override val eventType: String
        get() = entity.eventType

    override val eventVersion: Int
        get() = entity.eventVersion

    override val occurredAt: Long
        get() = entity.occurredAt

    override val eventId: String
        get() = entity.eventId

    override val orgId: String
        get() = entity.orgId

    override val payload: String
        get() = entity.payload
}
