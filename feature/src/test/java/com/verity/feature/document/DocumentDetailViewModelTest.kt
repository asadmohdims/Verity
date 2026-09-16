package com.verity.feature.document

import com.verity.core.document.model.DocumentFooter
import com.verity.core.document.model.DocumentIdentity
import com.verity.core.document.model.DocumentParties
import com.verity.core.document.model.DocumentParty
import com.verity.core.document.model.DocumentTotals
import com.verity.core.document.model.DocumentType
import com.verity.core.document.model.InvoiceDocumentModel
import com.verity.core.document.model.SellerDetails
import com.verity.feature.invoice.pdf.InvoicePdfRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.LocalDate

/**
 * DocumentDetailViewModelTest
 *
 * Verifies the ViewModel loads the right document by id from its DocumentDetailDataSource, and
 * that ensurePdf() delegates to InvoicePdfRenderer with the loaded document — same fake-based
 * style as InvoiceWorkspaceScreenUndoTest's fakes, but as a plain JVM unit test (no Compose/
 * Android dependency needed here).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DocumentDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeDocumentDetailDataSource(
        private val documents: Map<String, InvoiceDocumentModel>,
        private val linkedDocumentIds: Map<String, String> = emptyMap()
    ) : DocumentDetailDataSource {
        override suspend fun loadDocument(documentId: String): InvoiceDocumentModel? =
            documents[documentId]
        override suspend fun findLinkedDocumentId(documentId: String): String? =
            linkedDocumentIds[documentId]
    }

    private class FakeInvoicePdfRenderer : InvoicePdfRenderer {
        var lastRenderedDocument: InvoiceDocumentModel? = null
        val file = File("fake.pdf")

        override suspend fun ensurePdf(document: InvoiceDocumentModel): File {
            lastRenderedDocument = document
            return file
        }
    }

    private fun testDocument(documentNumber: String): InvoiceDocumentModel {
        val party = DocumentParty(
            name = "Test Buyer",
            gstin = "27AAACB1234Z1Z",
            addressLines = listOf("Test Address"),
            state = "Maharashtra",
            stateCode = "27"
        )

        return InvoiceDocumentModel(
            identity = DocumentIdentity(
                documentType = DocumentType.INVOICE,
                documentNumber = documentNumber,
                issueDate = LocalDate.of(2026, 9, 14),
                seller = SellerDetails(
                    name = "Test Seller",
                    gstin = null,
                    addressLine1 = "",
                    addressLine2 = null,
                    city = "",
                    state = "",
                    stateCode = "27",
                    pincode = ""
                ),
                placeOfSupplyState = "Maharashtra",
                placeOfSupplyStateCode = "27"
            ),
            parties = DocumentParties(billedTo = party, shippedTo = party),
            lineItems = emptyList(),
            logistics = null,
            taxation = null,
            totals = DocumentTotals(
                itemsSubtotalPaise = 0,
                freightPaise = 0,
                taxTotalPaise = 0,
                grandTotalPaise = 0
            ),
            footer = DocumentFooter(declarationText = "Test declaration", notes = null)
        )
    }

    @Test
    fun loads_the_document_matching_the_given_id() = runTest(dispatcher) {
        val document = testDocument("INV-000001")
        val dataSource = FakeDocumentDetailDataSource(mapOf("doc-1" to document))

        val viewModel = DocumentDetailViewModel(
            documentId = "doc-1",
            dataSource = dataSource,
            invoicePdfRenderer = FakeInvoicePdfRenderer()
        )

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(document, viewModel.document.value)
    }

    @Test
    fun exposes_null_when_no_document_matches_the_id() = runTest(dispatcher) {
        val dataSource = FakeDocumentDetailDataSource(emptyMap())

        val viewModel = DocumentDetailViewModel(
            documentId = "missing",
            dataSource = dataSource,
            invoicePdfRenderer = FakeInvoicePdfRenderer()
        )

        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.document.value)
    }

    @Test
    fun ensurePdf_delegates_to_the_renderer_with_the_loaded_document() = runTest(dispatcher) {
        val document = testDocument("INV-000001")
        val dataSource = FakeDocumentDetailDataSource(mapOf("doc-1" to document))
        val renderer = FakeInvoicePdfRenderer()

        val viewModel = DocumentDetailViewModel(
            documentId = "doc-1",
            dataSource = dataSource,
            invoicePdfRenderer = renderer
        )

        dispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.ensurePdf()

        assertEquals(renderer.file, result)
        assertEquals(document, renderer.lastRenderedDocument)
    }
}
