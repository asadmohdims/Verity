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
 * Migration2To3Test
 *
 * Proves a real pre-v3 database (built from the exported v2 schema, holding an actual finalized
 * document and ledger entry) survives Migration2To3 with syncedToCloud correctly defaulted to
 * false for every pre-existing row — same pattern as Migration1To2Test, protecting real on-device
 * invoice data from ever being silently reset.
 */
@RunWith(AndroidJUnit4::class)
class Migration2To3Test {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PlatformDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate2To3_defaults_syncedToCloud_to_false_for_existing_rows() {
        helper.createDatabase(TEST_DB, 2).apply {
            execSQL(
                """
                INSERT INTO documents (
                    documentId, orgId, documentType, sequenceNumber, documentNumber,
                    customerId, customerName, issueDateEpochDay, grandTotalPaise,
                    linkedDocumentId, payloadJson, finalizedAt, searchIndexText
                ) VALUES (
                    'doc-1', 'default-org', 'INVOICE', 1, 'INV-000001',
                    'cust-1', 'Acme Traders', 20000, 4576000,
                    NULL, '{}', 1700000000000, 'acme traders'
                )
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO ledger_entries (
                    entryId, orgId, customerId, documentId, amountPaise, occurredAt, createdAt
                ) VALUES (
                    'entry-1', 'default-org', 'cust-1', 'doc-1', 4576000, 1700000000000, 1700000000000
                )
                """.trimIndent()
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 3, true, Migration2To3)

        migrated.query("SELECT syncedToCloud FROM documents WHERE documentId = 'doc-1'").use {
            assertEquals(true, it.moveToFirst())
            assertEquals(0, it.getInt(it.getColumnIndexOrThrow("syncedToCloud")))
        }
        migrated.query("SELECT syncedToCloud FROM ledger_entries WHERE entryId = 'entry-1'").use {
            assertEquals(true, it.moveToFirst())
            assertEquals(0, it.getInt(it.getColumnIndexOrThrow("syncedToCloud")))
        }
    }

    private companion object {
        const val TEST_DB = "migration-2-3-test"
    }
}
