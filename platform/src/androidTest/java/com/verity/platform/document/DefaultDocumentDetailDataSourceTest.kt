package com.verity.platform.document

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.verity.feature.invoice.draft.DraftAddress
import com.verity.feature.invoice.draft.DraftDocumentType
import com.verity.feature.invoice.draft.DraftLineItem
import com.verity.feature.invoice.draft.InvoiceDraftUiState
import com.verity.platform.database.PlatformDatabase
import com.verity.platform.finalize.DefaultInvoiceFinalizer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

/**
 * DefaultDocumentDetailDataSourceTest
 *
 * Verifies loadDocument reads a real finalized row back through Room and deserializes it into
 * the exact InvoiceDocumentModel it was finalized from — against a real in-memory Room instance,
 * not mocks, same pattern as DefaultInvoiceFinalizerTest (whose finalizer is reused here to
 * produce a genuine persisted row rather than hand-constructing payloadJson).
 */
@RunWith(AndroidJUnit4::class)
class DefaultDocumentDetailDataSourceTest {

    private lateinit var database: PlatformDatabase
    private lateinit var finalizer: DefaultInvoiceFinalizer
    private lateinit var dataSource: DefaultDocumentDetailDataSource
    private val clock: Clock = Clock.fixed(Instant.parse("2026-09-14T00:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PlatformDatabase::class.java
        )
            .allowMainThreadQueries() // instrumentation tests only
            .build()

        finalizer = DefaultInvoiceFinalizer(database, clock)
        dataSource = DefaultDocumentDetailDataSource(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun loadDocument_returns_the_exact_document_a_row_was_finalized_from() = runBlocking {
        val customerId = UUID.randomUUID().toString()
        val finalized = finalizer.finalize(testDraft(), customerId)
        val documentId = database.documentDao().getAll().single().documentId

        val loaded = dataSource.loadDocument(documentId)

        assertEquals(finalized, loaded)
    }

    @Test
    fun loadDocument_returns_null_for_an_unknown_id() = runBlocking {
        val loaded = dataSource.loadDocument(UUID.randomUUID().toString())

        assertNull(loaded)
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
