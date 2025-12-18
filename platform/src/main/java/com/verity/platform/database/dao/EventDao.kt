package com.verity.platform.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.verity.platform.database.entities.EventEntity

/**
 * EventDao
 *
 * PURPOSE
 * -------
 * Defines the sole persistence contract for domain events.
 * Enforces append-only semantics and replay-safe access patterns.
 *
 * CONSTRAINTS
 * -----------
 * • Insert-only: events may never be updated or deleted
 * • Idempotent writes via primary key (eventId)
 * • Deterministic ordering for replay
 * • Organization-scoped access only
 */
@Dao
interface EventDao {

    /**
     * Inserts a single domain event.
     *
     * Duplicate inserts (same eventId) are ignored to guarantee
     * idempotency during retries, sync replays, or imports.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEvent(event: EventEntity)

    /**
     * Inserts multiple domain events atomically.
     *
     * Used during sync replay or bulk import.
     * Duplicate events are ignored individually.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEvents(events: List<EventEntity>)

    /**
     * Returns all events for an organization in deterministic
     * business-time order for full replay.
     *
     * Ordering is strictly by occurredAt, with eventId as a
     * stable tie-breaker.
     */
    @Query(
        """
        SELECT * FROM events
        WHERE orgId = :orgId
        ORDER BY occurredAt ASC, eventId ASC
        """
    )
    suspend fun getAllEventsForOrg(orgId: String): List<EventEntity>

    /**
     * Returns events occurring after the given business timestamp.
     *
     * Used for incremental replay and projection catch-up.
     * Must never skip or reorder events.
     */
    @Query(
        """
        SELECT * FROM events
        WHERE orgId = :orgId
          AND occurredAt > :occurredAfter
        ORDER BY occurredAt ASC, eventId ASC
        """
    )
    suspend fun getEventsAfter(
        orgId: String,
        occurredAfter: Long
    ): List<EventEntity>
}