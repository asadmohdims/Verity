package com.verity.platform.database.migrations

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.verity.platform.database.PlatformDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migration4To5Test
 *
 * Proves a real pre-v5 database (customers and documents rows already on disk) survives
 * Migration4To5 with both new columns present and NULL for existing rows — same "verify for
 * real, not by guessing SQL" discipline as Migration1To2Test.
 */
@RunWith(AndroidJUnit4::class)
class Migration4To5Test {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PlatformDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate4To5_adds_notes_and_selfNotes_as_null_for_existing_rows() {
        helper.createDatabase(TEST_DB, 4).apply {
            execSQL(
                """
                INSERT INTO customers (
                    customerId, customerName, phone, gstin, addressLine1, city, state,
                    stateCode, pincode, isActive, updatedAt
                ) VALUES (
                    'cust-1', 'Acme Traders', NULL, '27AAACB1234Z1Z', '12 Market Road', 'Mumbai',
                    'Maharashtra', '27', NULL, 1, 1700000000000
                )
                """.trimIndent()
            )
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

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 5, true, Migration4To5)

        migrated.query("SELECT notes FROM customers WHERE customerId = 'cust-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("notes")))
        }

        migrated.query("SELECT selfNotes FROM documents WHERE documentId = 'doc-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("selfNotes")))
        }

        // The columns are genuinely writable post-migration, not just present.
        migrated.execSQL("UPDATE customers SET notes = 'Discount agreed' WHERE customerId = 'cust-1'")
        migrated.execSQL("UPDATE documents SET selfNotes = 'Paid in cash' WHERE documentId = 'doc-1'")

        migrated.query("SELECT notes FROM customers WHERE customerId = 'cust-1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("Discount agreed", cursor.getString(cursor.getColumnIndexOrThrow("notes")))
        }
        migrated.query("SELECT selfNotes FROM documents WHERE documentId = 'doc-1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("Paid in cash", cursor.getString(cursor.getColumnIndexOrThrow("selfNotes")))
        }
    }

    private companion object {
        const val TEST_DB = "migration-4-to-5-test"
    }
}
