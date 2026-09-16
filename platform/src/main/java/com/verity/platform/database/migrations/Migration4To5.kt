package com.verity.platform.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds `customers.notes` (free-form CRM-style notes) and `documents.selfNotes` (private,
 * never-printed per-document notes — see DocumentEntity's doc comment). Both are simple nullable
 * TEXT columns on existing tables; existing rows get NULL, same shape as Migration1To2's
 * searchIndexText backfill would have needed if that column weren't derivable — these two aren't
 * derivable from anything, so NULL (i.e. "no note yet") is the correct value for every
 * pre-existing row, not a placeholder that needs a follow-up backfill.
 */
object Migration4To5 : Migration(4, 5) {

    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE customers ADD COLUMN notes TEXT")
        db.execSQL("ALTER TABLE documents ADD COLUMN selfNotes TEXT")
    }
}
