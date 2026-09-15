package com.verity.platform.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds `syncedToCloud` to both `documents` and `ledger_entries`, defaulting every existing row
 * to false (0) — correct even for rows finalized before cloud sync existed, since none of them
 * have been pushed to Firestore yet. See DocumentEntity/LedgerEntryEntity's doc comments and
 * FirebaseSyncClient (platform/sync/) for how this column gets flipped to true.
 *
 * A real Migration, not fallbackToDestructiveMigration — same reasoning as Migration1To2: real
 * finalized invoices already live in this app's local database.
 */
object Migration2To3 : Migration(2, 3) {

    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE documents ADD COLUMN syncedToCloud INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE ledger_entries ADD COLUMN syncedToCloud INTEGER NOT NULL DEFAULT 0")
    }
}
