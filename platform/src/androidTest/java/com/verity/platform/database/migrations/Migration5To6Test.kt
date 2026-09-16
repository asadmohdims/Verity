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
 * Migration5To6Test
 *
 * Proves a real pre-v6 database (a customer row already on disk, pre-dating cloud sync) survives
 * Migration5To6 with orgId defaulted to 'default-org' and syncedToCloud defaulted to 0 (false) for
 * that existing row — the exact signal CustomerDao.getUnsyncedCustomers() needs to push a device's
 * pre-existing customers to the cloud for the first time. Same "verify for real" discipline as
 * Migration4To5Test.
 */
@RunWith(AndroidJUnit4::class)
class Migration5To6Test {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PlatformDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate5To6_defaults_orgId_and_syncedToCloud_for_an_existing_customer_row() {
        helper.createDatabase(TEST_DB, 5).apply {
            execSQL(
                """
                INSERT INTO customers (
                    customerId, customerName, phone, gstin, addressLine1, city, state,
                    stateCode, pincode, isActive, updatedAt, notes
                ) VALUES (
                    'cust-1', 'Acme Traders', NULL, '27AAACB1234Z1Z', '12 Market Road', 'Mumbai',
                    'Maharashtra', '27', NULL, 1, 1700000000000, NULL
                )
                """.trimIndent()
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 6, true, Migration5To6)

        migrated.query("SELECT orgId, syncedToCloud FROM customers WHERE customerId = 'cust-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("default-org", cursor.getString(cursor.getColumnIndexOrThrow("orgId")))
            assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("syncedToCloud")))
        }
    }

    private companion object {
        const val TEST_DB = "migration-5-to-6-test"
    }
}
