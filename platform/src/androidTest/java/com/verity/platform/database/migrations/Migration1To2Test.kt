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
 * Migration1To2Test
 *
 * Proves a real pre-v2 database (built from the exported v1 schema, holding an actual finalized
 * document row) survives Migration1To2 with `searchIndexText` correctly backfilled — the one
 * genuinely load-bearing test in the Documents-search work, since this protects real on-device
 * invoice data from ever being silently reset (see PlatformDatabase's exportSchema doc comment).
 */
@RunWith(AndroidJUnit4::class)
class Migration1To2Test {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PlatformDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate1To2_backfills_searchIndexText_for_an_existing_row() {
        val documentId = "doc-1"
        val payloadJson = SAMPLE_PAYLOAD_JSON

        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                """
                INSERT INTO documents (
                    documentId, orgId, documentType, sequenceNumber, documentNumber,
                    customerId, customerName, issueDateEpochDay, grandTotalPaise,
                    linkedDocumentId, payloadJson, finalizedAt
                ) VALUES (
                    '$documentId', 'default-org', 'INVOICE', 1, 'INV-000001',
                    'cust-1', 'Acme Traders', 20000, 4576000,
                    NULL, '${payloadJson.replace("'", "''")}', 1700000000000
                )
                """.trimIndent()
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 2, true, Migration1To2)

        val cursor = migrated.query("SELECT searchIndexText FROM documents WHERE documentId = '$documentId'")
        cursor.use {
            assertEquals(true, it.moveToFirst())
            val searchIndexText = it.getString(it.getColumnIndexOrThrow("searchIndexText"))
            assertEquals(true, searchIndexText.contains("acme traders"))
            assertEquals(true, searchIndexText.contains("portland cement"))
            assertEquals(true, searchIndexText.contains("mh12ab3456"))
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"

        // Minimal but real InvoiceDocumentModel JSON — matches the shape encoded by
        // DefaultInvoiceFinalizer, trimmed to only what buildSearchIndexText reads.
        const val SAMPLE_PAYLOAD_JSON = """
        {
          "identity": {
            "documentType": "INVOICE",
            "documentNumber": "INV-000001",
            "issueDate": "2026-09-14",
            "seller": {
              "name": "Unitech Machineries",
              "gstin": "27AAACU1234Z1Z",
              "addressLine1": "Plot 1",
              "addressLine2": null,
              "city": "Mumbai",
              "state": "Maharashtra",
              "stateCode": "27",
              "pincode": "400001"
            },
            "placeOfSupplyState": "Maharashtra",
            "placeOfSupplyStateCode": "27",
            "reverseChargeApplicable": false
          },
          "parties": {
            "billedTo": {
              "name": "Acme Traders",
              "gstin": "27AAACB1234Z1Z",
              "addressLines": ["12 Market Road"],
              "state": "Maharashtra",
              "stateCode": "27"
            },
            "shippedTo": {
              "name": "Acme Traders",
              "gstin": "27AAACB1234Z1Z",
              "addressLines": ["12 Market Road"],
              "state": "Maharashtra",
              "stateCode": "27"
            }
          },
          "lineItems": [
            {
              "description": "Portland Cement",
              "hsnCode": "2523",
              "quantity": 50,
              "unit": "BAG",
              "ratePaise": 40000,
              "amountPaise": 2000000
            }
          ],
          "logistics": {
            "transporterName": "Speed Carriers",
            "vehicleNumber": "MH12AB3456",
            "supplyDate": "2026-09-14",
            "grOrLrNumber": "LR-9081",
            "freightPaise": 50000,
            "notes": null,
            "ewayBillNumber": "EWAY-771122"
          },
          "taxation": null,
          "totals": {
            "itemsSubtotalPaise": 2000000,
            "freightPaise": 50000,
            "taxTotalPaise": 526000,
            "grandTotalPaise": 4576000
          },
          "footer": {
            "declarationText": "We declare that this invoice shows the actual price.",
            "notes": null
          }
        }
        """
    }
}
