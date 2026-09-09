package com.verity.platform.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.verity.platform.database.dao.CustomerDao
import com.verity.platform.database.dao.DocumentDao
import com.verity.platform.database.dao.LedgerEntryDao
import com.verity.platform.database.entities.CustomerEntity
import com.verity.platform.database.entities.DocumentEntity
import com.verity.platform.database.entities.LedgerEntryEntity

/**
 * PlatformDatabase
 *
 * Authoritative local database for Verity platform infrastructure. Local-first: this is the
 * source of truth; cloud (once it exists) is a sync target, not the other way around.
 *
 * exportSchema = false: no real installs of this app exist anywhere yet, so there is no
 * migration history worth protecting. Revisit once there's a live user base.
 */
@Database(
    entities = [
        CustomerEntity::class,
        DocumentEntity::class,
        LedgerEntryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PlatformDatabase : RoomDatabase() {

    abstract fun customerDao(): CustomerDao

    abstract fun documentDao(): DocumentDao

    abstract fun ledgerEntryDao(): LedgerEntryDao
}
