package com.verity.platform.finalize

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.verity.core.document.model.InvoiceDocumentModel
import com.verity.feature.invoice.draft.DraftAddress
import com.verity.feature.invoice.draft.DraftDocumentType
import com.verity.feature.invoice.draft.DraftLineItem
import com.verity.feature.invoice.draft.InvoiceDraftUiState
import com.verity.platform.database.PlatformDatabase
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

/**
 * DefaultInvoiceFinalizerTest
 *
 * Verifies sequence numbering, persisted payload round-tripping, and the ledger entry written
 * alongside finalization — against a real Room instance, not mocks (same pattern the deleted
 * EventDaoTest used for the old event store).
 */
@RunWith(AndroidJUnit4::class)
class DefaultInvoiceFinalizerTest {

    private lateinit var database: PlatformDatabase
    private lateinit var finalizer: DefaultInvoiceFinalizer
    private val json = Json { ignoreUnknownKeys = false }
    private val clock: Clock = Clock.fixed(Instant.parse("2026-09-09T00:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PlatformDatabase::class.java
        )
            .allowMainThreadQueries() // instrumentation tests only
            .build()

        finalizer = DefaultInvoiceFinalizer(database, clock)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun finalize_assigns_sequence_one_and_persists_a_round_trippable_document() = runBlocking {
        val customerId = UUID.randomUUID().toString()
        val document = finalizer.finalize(testDraft(), customerId)

        assertEquals("INV-000001", document.identity.documentNumber)

        val rows = database.documentDao().getAll()
        assertEquals(1, rows.size)
        assertEquals(1L, rows.single().sequenceNumber)
        assertEquals(customerId, rows.single().customerId)
        assertEquals(document.totals.grandTotalPaise, rows.single().grandTotalPaise)

        val roundTripped = json.decodeFromString<InvoiceDocumentModel>(rows.single().payloadJson)
        assertEquals(document, roundTripped)

        val balance = database.ledgerEntryDao().getBalanceForCustomer(DEFAULT_ORG_ID, customerId)
        assertEquals(document.totals.grandTotalPaise, balance)
    }

    @Test
    fun finalize_assigns_consecutive_numbers_across_calls() = runBlocking {
        val customerId = UUID.randomUUID().toString()
        val draft = testDraft()

        val first = finalizer.finalize(draft, customerId)
        val second = finalizer.finalize(draft, customerId)

        assertEquals("INV-000001", first.identity.documentNumber)
        assertEquals("INV-000002", second.identity.documentNumber)

        val balance = database.ledgerEntryDao().getBalanceForCustomer(DEFAULT_ORG_ID, customerId)
        assertEquals(first.totals.grandTotalPaise + second.totals.grandTotalPaise, balance)
    }

    private fun testDraft(): InvoiceDraftUiState =
        InvoiceDraftUiState(
            documentType = DraftDocumentType.INVOICE,
            billedTo = DraftAddress(
                name = "Test Buyer",
                gstin = "27AAACB1234Z1Z",
                addressLine1 = "Test Address",
                city = "Mumbai",
                state = "Maharashtra",
                stateCode = "27",
                pincode = "400001",
                customerId = UUID.randomUUID().toString()
            ),
            lineItems = listOf(
                DraftLineItem(
                    description = "Item A",
                    hsnCode = "1001",
                    quantity = 10,
                    unit = "PCS",
                    ratePaise = 1_000
                )
            )
        )
}
