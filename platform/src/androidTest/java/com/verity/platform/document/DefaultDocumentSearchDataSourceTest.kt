package com.verity.platform.document

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.verity.feature.document.search.DocumentMatchKind
import com.verity.feature.invoice.draft.DraftAddress
import com.verity.feature.invoice.draft.DraftDocumentType
import com.verity.feature.invoice.draft.DraftLineItem
import com.verity.feature.invoice.draft.DraftTransportDetails
import com.verity.feature.invoice.draft.InvoiceDraftUiState
import com.verity.platform.database.PlatformDatabase
import com.verity.platform.database.entities.CustomerEntity
import com.verity.platform.finalize.DefaultInvoiceFinalizer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

/**
 * DefaultDocumentSearchDataSourceTest
 *
 * Verifies search against real finalized rows (via DefaultInvoiceFinalizer, same as the other
 * platform DB tests) rather than hand-built payloadJson: document-number matches rank first,
 * line-item matches carry a snippet, and customer matches surface in the customers group.
 */
@RunWith(AndroidJUnit4::class)
class DefaultDocumentSearchDataSourceTest {

    private lateinit var database: PlatformDatabase
    private lateinit var finalizer: DefaultInvoiceFinalizer
    private lateinit var dataSource: DefaultDocumentSearchDataSource
    private val clock: Clock = Clock.fixed(Instant.parse("2026-09-14T00:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PlatformDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        finalizer = DefaultInvoiceFinalizer(database, clock)
        dataSource = DefaultDocumentSearchDataSource(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun search_finds_a_document_by_its_own_number() = runBlocking {
        val customerId = UUID.randomUUID().toString()
        val finalized = finalizer.finalize(testDraft("Portland Cement"), customerId)

        val results = dataSource.search(finalized.identity.documentNumber)

        assertEquals(1, results.documents.size)
        assertEquals(DocumentMatchKind.DOCUMENT_NUMBER, results.documents.single().matchKind)
    }

    @Test
    fun search_finds_a_document_by_a_line_item_description_with_a_snippet() = runBlocking {
        val customerId = UUID.randomUUID().toString()
        finalizer.finalize(testDraft("Portland Cement"), customerId)

        val results = dataSource.search("cement")

        assertEquals(1, results.documents.size)
        val result = results.documents.single()
        assertEquals(DocumentMatchKind.LINE_ITEM, result.matchKind)
        assertTrue(result.matchedSnippet!!.text.lowercase().contains("cement"))
    }

    @Test
    fun search_finds_a_document_by_vehicle_number() = runBlocking {
        val customerId = UUID.randomUUID().toString()
        finalizer.finalize(testDraft("Steel Rods", vehicleNumber = "MH12AB3456"), customerId)

        val results = dataSource.search("mh12ab3456")

        assertEquals(1, results.documents.size)
        assertEquals(DocumentMatchKind.TRANSPORT, results.documents.single().matchKind)
    }

    @Test
    fun search_surfaces_a_matching_active_customer_in_the_customers_group() = runBlocking {
        val customerId = UUID.randomUUID().toString()
        database.customerDao().insert(
            CustomerEntity(
                customerId = customerId,
                customerName = "Acme Traders",
                phone = null,
                gstin = "27AAACB1234Z1Z",
                addressLine1 = "12 Market Road",
                city = "Mumbai",
                state = "Maharashtra",
                stateCode = "27",
                pincode = "400001",
                isActive = true,
                updatedAt = 0L
            )
        )

        val results = dataSource.search("acme")

        assertEquals(1, results.customers.size)
        assertEquals(customerId, results.customers.single().customerId)
    }

    @Test
    fun search_returns_nothing_for_an_unmatched_query() = runBlocking {
        finalizer.finalize(testDraft("Portland Cement"), UUID.randomUUID().toString())

        val results = dataSource.search("nonexistent-query-xyz")

        assertTrue(results.documents.isEmpty())
        assertTrue(results.customers.isEmpty())
    }

    @Test
    fun search_with_two_tokens_requires_both_to_match_ANDed_across_different_fields() = runBlocking {
        finalizer.finalize(testDraft("Pipes", customerName = "Garg Duplex"), UUID.randomUUID().toString())
        finalizer.finalize(testDraft("Pipes", customerName = "Acme Traders"), UUID.randomUUID().toString())
        finalizer.finalize(testDraft("Cement", customerName = "Garg Duplex"), UUID.randomUUID().toString())

        val results = dataSource.search("pipes garg")

        assertEquals(1, results.documents.size)
        assertEquals("Garg Duplex", results.documents.single().customerName)
    }

    @Test
    fun search_with_two_tokens_is_order_independent() = runBlocking {
        finalizer.finalize(testDraft("Pipes", customerName = "Garg Duplex"), UUID.randomUUID().toString())
        finalizer.finalize(testDraft("Pipes", customerName = "Acme Traders"), UUID.randomUUID().toString())

        val results = dataSource.search("garg pipes")

        assertEquals(1, results.documents.size)
        assertEquals("Garg Duplex", results.documents.single().customerName)
    }

    @Test
    fun search_prefers_a_line_item_snippet_even_when_a_different_token_wins_on_customer_priority() = runBlocking {
        finalizer.finalize(testDraft("Pipes", customerName = "Garg Duplex"), UUID.randomUUID().toString())

        val results = dataSource.search("pipes garg")

        val result = results.documents.single()
        // "garg" matches the customer name, which alone would rank as CUSTOMER with no snippet —
        // but "pipes" also matches a line item, and that's more useful to highlight since the
        // customer name is already shown in the result row regardless.
        assertEquals(DocumentMatchKind.CUSTOMER, result.matchKind)
        assertTrue(result.matchedSnippet!!.text.lowercase().contains("pipes"))
    }

    private fun testDraft(
        itemDescription: String,
        vehicleNumber: String? = null,
        customerName: String = "Test Buyer"
    ): InvoiceDraftUiState =
        InvoiceDraftUiState(
            documentType = DraftDocumentType.INVOICE,
            billedTo = DraftAddress(
                name = customerName,
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
                    description = itemDescription,
                    hsnCode = "1001",
                    quantity = 10,
                    unit = "PCS",
                    ratePaise = 1_000
                )
            ),
            transportDetails = if (vehicleNumber != null) {
                DraftTransportDetails(vehicleNumber = vehicleNumber)
            } else {
                null
            }
        )
}
