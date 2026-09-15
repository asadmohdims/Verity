package com.verity.platform.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds the `reference_list_items` table backing the manually-curated Transporter Name / HSN Code
 * lists (see ReferenceListEntity). A brand-new, empty table — no existing rows to backfill, unlike
 * Migration1To2/Migration2To3.
 */
object Migration3To4 : Migration(3, 4) {

    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS reference_list_items (
                id TEXT NOT NULL PRIMARY KEY,
                kind TEXT NOT NULL,
                value TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_reference_list_items_kind_value " +
                "ON reference_list_items (kind, value)"
        )
    }
}
