package com.verity.platform.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.verity.core.document.model.InvoiceDocumentModel
import com.verity.core.document.search.buildSearchIndexText
import kotlinx.serialization.json.Json

/**
 * Adds `documents.searchIndexText` (the flattened, lowercase text a document search query is
 * matched against — see DocumentSearchIndexText.kt) and backfills it for every row that already
 * exists on-device, so invoices finalized before this migration remain fully searchable.
 *
 * A real Migration, not fallbackToDestructiveMigration: real finalized invoices already live in
 * this app's local database, and destructively resetting the schema would delete them.
 */
object Migration1To2 : Migration(1, 2) {

    private val json = Json { ignoreUnknownKeys = true }

    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE documents ADD COLUMN searchIndexText TEXT NOT NULL DEFAULT ''")

        val cursor = db.query("SELECT documentId, payloadJson FROM documents")
        val backfill = cursor.use {
            val idIndex = it.getColumnIndexOrThrow("documentId")
            val payloadIndex = it.getColumnIndexOrThrow("payloadJson")

            buildList {
                while (it.moveToNext()) {
                    val documentId = it.getString(idIndex)
                    val payloadJson = it.getString(payloadIndex)
                    val document = json.decodeFromString(InvoiceDocumentModel.serializer(), payloadJson)
                    add(documentId to buildSearchIndexText(document))
                }
            }
        }

        backfill.forEach { (documentId, searchIndexText) ->
            db.execSQL(
                "UPDATE documents SET searchIndexText = ? WHERE documentId = ?",
                arrayOf(searchIndexText, documentId)
            )
        }
    }
}
