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
import com.verity.platform.database.entities.DocumentEntity
import com.verity.platform.database.entities.LedgerEntryEntity
import com.verity.platform.sync.DefaultInvoiceNumberAllocator
import com.verity.platform.sync.FirebaseSyncClient
import kotlinx.coroutines.runBlocking
import java.io.File
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
 * Verifies sequence numbering (independently per document type), persisted payload
 * round-tripping, and the ledger entry written alongside Invoice finalization (but not Challan,
 * which isn't a billing event) — against a real Room instance, not mocks (same pattern the
 * deleted EventDaoTest used for the old event store).
 *
 * The number allocator's online counter always fails here (FailingOnlineCounterSource), so every
 * test exercises the local-fallback numbering path — deliberately, since that's the path that
 * must keep working offline regardless of cloud config. See InvoiceNumberAllocatorTest for the
 * online/timeout/fallback branching itself. The sync client is a recording fake (see below) so
 * tests can assert finalize() pushes without ever touching real network — see
 * FirebaseSyncClient's doc comment for why that push is fire-and-forget and must never block
 * finalize's return.
 */
@RunWith(AndroidJUnit4::class)
class DefaultInvoiceFinalizerTest {

    private lateinit var database: PlatformDatabase
    private lateinit var finalizer: DefaultInvoiceFinalizer
    private lateinit var syncClient: RecordingFirebaseSyncClient
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

        syncClient = RecordingFirebaseSyncClient()
        finalizer = DefaultInvoiceFinalizer(
            database = database,
            clock = clock,
            numberAllocator = DefaultInvoiceNumberAllocator(
                onlineCounterSource = { _, _ -> error("test: online counter always fails") },
                documentDao = database.documentDao()
            ),
            syncClient = syncClient
        )
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

    @Test
    fun finalize_challan_gets_its_own_ch_prefixed_number_and_no_ledger_entry() = runBlocking {
        val customerId = UUID.randomUUID().toString()
        val document = finalizer.finalize(testDraft().copy(documentType = DraftDocumentType.CHALLAN), customerId)

        assertEquals("CH-000001", document.identity.documentNumber)

        val rows = database.documentDao().getAll()
        assertEquals(1, rows.size)
        assertEquals("CHALLAN", rows.single().documentType)

        val balance = database.ledgerEntryDao().getBalanceForCustomer(DEFAULT_ORG_ID, customerId)
        assertEquals(null, balance)
    }

    @Test
    fun invoice_and_challan_sequences_are_independent() = runBlocking {
        val customerId = UUID.randomUUID().toString()
        val draft = testDraft()

        val firstInvoice = finalizer.finalize(draft, customerId)
        val firstChallan = finalizer.finalize(draft.copy(documentType = DraftDocumentType.CHALLAN), customerId)
        val secondInvoice = finalizer.finalize(draft, customerId)

        assertEquals("INV-000001", firstInvoice.identity.documentNumber)
        assertEquals("CH-000001", firstChallan.identity.documentNumber)
        assertEquals("INV-000002", secondInvoice.identity.documentNumber)

        val balance = database.ledgerEntryDao().getBalanceForCustomer(DEFAULT_ORG_ID, customerId)
        assertEquals(firstInvoice.totals.grandTotalPaise + secondInvoice.totals.grandTotalPaise, balance)
    }

    @Test
    fun finalize_pushes_the_document_and_ledger_entry_after_committing_locally() = runBlocking {
        val customerId = UUID.randomUUID().toString()
        val document = finalizer.finalize(testDraft(), customerId)

        // FirebaseSyncClient.pushDocument/pushLedgerEntry are non-suspend by design (see that
        // interface's doc comment) specifically so finalize() can never accidentally await them -
        // this test confirms the wiring calls them with the right data, not the non-blocking
        // guarantee itself, which the type signature already enforces at compile time.
        assertEquals(1, syncClient.pushedDocuments.size)
        assertEquals(document.identity.documentNumber, syncClient.pushedDocuments.single().documentNumber)
        assertEquals(1, syncClient.pushedLedgerEntries.size)
        assertEquals(document.totals.grandTotalPaise, syncClient.pushedLedgerEntries.single().amountPaise)
    }

    @Test
    fun finalize_challan_pushes_the_document_but_no_ledger_entry() = runBlocking {
        val customerId = UUID.randomUUID().toString()
        finalizer.finalize(testDraft().copy(documentType = DraftDocumentType.CHALLAN), customerId)

        assertEquals(1, syncClient.pushedDocuments.size)
        assertEquals(0, syncClient.pushedLedgerEntries.size)
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

    private class RecordingFirebaseSyncClient : FirebaseSyncClient {
        val pushedDocuments = mutableListOf<DocumentEntity>()
        val pushedLedgerEntries = mutableListOf<LedgerEntryEntity>()

        override fun pushDocument(document: DocumentEntity) {
            pushedDocuments += document
        }

        override fun pushLedgerEntry(entry: LedgerEntryEntity) {
            pushedLedgerEntries += entry
        }

        override fun pushPdf(orgId: String, documentNumber: String, file: File) = Unit

        override suspend fun downloadPdf(orgId: String, documentNumber: String, destination: File) = false
    }
}
