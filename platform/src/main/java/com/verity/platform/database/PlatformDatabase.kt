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
 * exportSchema = true (changed from false 2026-09-14, alongside the searchIndexText column):
 * real invoices already exist in this app's on-device database, so from here on every schema
 * change needs a real Migration, not fallbackToDestructiveMigration — schema JSON per version is
 * exported to schemas/ (see platform/build.gradle.kts's ksp block) for MigrationTestHelper to
 * build real pre-migration databases against in tests.
 *
 * v3 (2026-09-15) adds syncedToCloud to documents/ledger_entries for cloud sync — see
 * Migration2To3 and CLAUDE.md's Data & sync architecture section.
 */
@Database(
    entities = [
        CustomerEntity::class,
        DocumentEntity::class,
        LedgerEntryEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class PlatformDatabase : RoomDatabase() {

    abstract fun customerDao(): CustomerDao

    abstract fun documentDao(): DocumentDao

    abstract fun ledgerEntryDao(): LedgerEntryDao
}
