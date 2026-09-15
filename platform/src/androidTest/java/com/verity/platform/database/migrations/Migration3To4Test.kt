package com.verity.platform.database.migrations

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.verity.platform.database.PlatformDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migration3To4Test
 *
 * Proves a real pre-v4 database (built from the exported v3 schema, holding real finalized
 * documents/ledger entries) survives Migration3To4 — the new reference_list_items table exists
 * afterward and accepts a row per kind, with the unique (kind, value) index enforced. No existing
 * rows to protect here (this is a brand-new, empty table), unlike Migration1To2/Migration2To3.
 */
@RunWith(AndroidJUnit4::class)
class Migration3To4Test {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PlatformDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate3To4_creates_reference_list_items_and_accepts_a_row() {
        helper.createDatabase(TEST_DB, 3).apply {
            execSQL(
                """
                INSERT INTO documents (
                    documentId, orgId, documentType, sequenceNumber, documentNumber,
                    customerId, customerName, issueDateEpochDay, grandTotalPaise,
                    linkedDocumentId, payloadJson, finalizedAt, searchIndexText, syncedToCloud
                ) VALUES (
                    'doc-1', 'default-org', 'INVOICE', 1, 'INV-000001',
                    'cust-1', 'Acme Traders', 20000, 4576000,
                    NULL, '{}', 1700000000000, 'acme traders', 0
                )
                """.trimIndent()
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 4, true, Migration3To4)

        migrated.execSQL(
            "INSERT INTO reference_list_items (id, kind, value, createdAt) " +
                "VALUES ('t-1', 'TRANSPORTER_NAME', 'MZN Transport', 1700000000000)"
        )

        migrated.query("SELECT value FROM reference_list_items WHERE id = 't-1'").use {
            assertEquals(true, it.moveToFirst())
            assertEquals("MZN Transport", it.getString(it.getColumnIndexOrThrow("value")))
        }

        // The pre-existing document row must survive the migration untouched.
        migrated.query("SELECT documentNumber FROM documents WHERE documentId = 'doc-1'").use {
            assertEquals(true, it.moveToFirst())
            assertEquals("INV-000001", it.getString(it.getColumnIndexOrThrow("documentNumber")))
        }
    }

    private companion object {
        const val TEST_DB = "migration-3-4-test"
    }
}
