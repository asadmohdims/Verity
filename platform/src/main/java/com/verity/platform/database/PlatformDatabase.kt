package com.verity.platform.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.verity.platform.database.dao.EventDao
import com.verity.platform.database.entities.EventEntity

/**
 * PlatformDatabase
 *
 * PURPOSE
 * -------
 * Authoritative local database for Verity platform infrastructure.
 *
 * This database anchors event truth and schema versioning.
 * It is the sole container for Room entities and DAOs.
 *
 * CONSTRAINTS
 * -----------
 * • Platform-owned only (no UI, no feature imports)
 * • Offline-first: local DB is the source of truth
 * • Versioned explicitly; migrations are mandatory on change
 * • No business logic, replay, or projections live here
 */
@Database(
    entities = [
        EventEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class PlatformDatabase : RoomDatabase() {

    /**
     * Provides insert-only access to the domain event store.
     *
     * Append-only semantics are enforced at the DAO and schema level.
     */
    abstract fun eventDao(): EventDao
}