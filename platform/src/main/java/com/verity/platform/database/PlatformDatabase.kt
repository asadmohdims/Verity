package com.verity.platform.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.verity.platform.database.dao.CustomerDao
import com.verity.platform.database.dao.DocumentDao
import com.verity.platform.database.dao.LedgerEntryDao
import com.verity.platform.database.dao.ReferenceListDao
import com.verity.platform.database.entities.CustomerEntity
import com.verity.platform.database.entities.DocumentEntity
import com.verity.platform.database.entities.LedgerEntryEntity
import com.verity.platform.database.entities.ReferenceListEntity

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
 *
 * v4 (2026-09-15) adds reference_list_items, backing the manually-curated Transporter Name / HSN
 * Code lists — see Migration3To4 and ReferenceListEntity.
 *
 * v5 (2026-09-16) adds customers.notes and documents.selfNotes — see Migration4To5.
 *
 * v6 (2026-09-16) adds customers.orgId and customers.syncedToCloud — customers now sync to
 * Firestore like documents/ledger entries already did. See Migration5To6.
 */
@Database(
    entities = [
        CustomerEntity::class,
        DocumentEntity::class,
        LedgerEntryEntity::class,
        ReferenceListEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class PlatformDatabase : RoomDatabase() {

    abstract fun customerDao(): CustomerDao

    abstract fun documentDao(): DocumentDao

    abstract fun ledgerEntryDao(): LedgerEntryDao

    abstract fun referenceListDao(): ReferenceListDao
}
