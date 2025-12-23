package com.verity.platform.database

import android.content.Context
import androidx.room.Room

/**
 * PlatformDatabaseFactory
 *
 * PURPOSE
 * -------
 * Explicit construction point for PlatformDatabase.
 *
 * This factory is platform-owned and UI-agnostic.
 * It deliberately avoids global singletons and DI frameworks.
 *
 * NOTE
 * ----
 * Database name is hardcoded for now by explicit decision.
 */
object PlatformDatabaseFactory {

    private const val DATABASE_NAME = "verity_platform.db"

    fun create(context: Context): PlatformDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            PlatformDatabase::class.java,
            DATABASE_NAME
        ).build()
    }
}