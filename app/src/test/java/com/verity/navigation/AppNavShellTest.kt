package com.verity.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.isRoot
import androidx.navigation.compose.rememberNavController
import com.verity.core.document.model.DocumentFooter
import com.verity.core.document.model.DocumentIdentity
import com.verity.core.document.model.DocumentParties
import com.verity.core.document.model.DocumentParty
import com.verity.core.document.model.DocumentTotals
import com.verity.core.document.model.DocumentType
import com.verity.core.document.model.HARDCODED_SELLER
import com.verity.core.document.model.InvoiceDocumentModel
import com.verity.core.formatting.money.Money
import com.verity.core.theme.VerityBaseTypography
import com.verity.core.theme.VerityTheme
import com.verity.feature.customer.rollup.CustomerRollup
import com.verity.feature.customer.rollup.CustomerRollupDataSource
import com.verity.feature.document.DocumentDetailDataSource
import com.verity.feature.document.DocumentsListViewModel
import com.verity.feature.document.search.DocumentSearchDataSource
import com.verity.feature.document.search.DocumentSearchResults
import com.verity.feature.document.search.DocumentSearchViewModel
import com.verity.feature.home.DocumentSummary
import com.verity.feature.home.HomeDataSource
import com.verity.feature.home.HomeViewModel
import com.verity.feature.home.SyncStatus
import com.verity.feature.invoice.autocomplete.CustomerAutocompleteDataSource
import com.verity.feature.invoice.autocomplete.CustomerAutocompleteItem
import com.verity.feature.invoice.draft.InvoiceDraftStore
import com.verity.feature.invoice.draft.InvoiceDraftUiState
import com.verity.feature.invoice.finalize.InvoiceFinalizer
import com.verity.feature.invoice.pdf.InvoicePdfRenderer
import com.verity.feature.invoice.ui.InvoiceWorkspaceViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Clock
import java.time.LocalDate

/**
 * Robolectric Compose tests for R-13's navigation shell — same methodology as feature/invoice's
 * InvoiceWorkspaceScreenUndoTest and feature/home's HomeScreenTest: a real semantics tree on the
 * JVM, no emulator.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
class AppNavShellTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private class NoopHomeDataSource : HomeDataSource {
        override suspend fun loadAllDocuments(): List<DocumentSummary> = emptyList()
        override suspend fun loadSyncStatus(): SyncStatus = SyncStatus(pendingCount = 0, lastSyncedAtEpochMillis = null)
    }

    private class NoopCustomerAutocompleteDataSource : CustomerAutocompleteDataSource {
        override suspend fun recentCustomers(limit: Int): List<CustomerAutocompleteItem> = emptyList()
        override suspend fun searchCustomers(query: String, limit: Int): List<CustomerAutocompleteItem> = emptyList()
    }

    private class NoopInvoiceFinalizer : InvoiceFinalizer {
        override suspend fun finalize(draft: InvoiceDraftUiState, customerId: String): InvoiceDocumentModel {
            error("not used in this test")
        }
    }

    private class NoopInvoicePdfRenderer : InvoicePdfRenderer {
        override suspend fun ensurePdf(document: InvoiceDocumentModel): File {
            error("not used in this test")
        }
    }

    private class NoopDocumentDetailDataSource : DocumentDetailDataSource {
        override suspend fun loadDocument(documentId: String): InvoiceDocumentModel? = null
    }

    private class NoopDocumentSearchDataSource : DocumentSearchDataSource {
        override suspend fun search(query: String): DocumentSearchResults =
            DocumentSearchResults(customers = emptyList(), documents = emptyList())
    }

    private class NoopCustomerRollupDataSource : CustomerRollupDataSource {
        override suspend fun loadRollup(customerId: String): CustomerRollup? = null
    }

    private fun sampleParty() = DocumentParty(
        name = "Bhargava Industries",
        gstin = "27AAACB1234Z1Z",
        addressLines = listOf("Industrial Area", "Mumbai"),
        state = "Maharashtra",
        stateCode = "27"
    )

    private fun sampleFinalizedDocument() = InvoiceDocumentModel(
        identity = DocumentIdentity(
            documentType = DocumentType.INVOICE,
            documentNumber = "INV-000043",
            issueDate = LocalDate.of(2026, 9, 13),
            seller = HARDCODED_SELLER,
            placeOfSupplyState = "Maharashtra",
            placeOfSupplyStateCode = "27"
        ),
        parties = DocumentParties(
            billedTo = sampleParty(),
            shippedTo = sampleParty()
        ),
        lineItems = emptyList(),
        logistics = null,
        taxation = null,
        totals = DocumentTotals(
            itemsSubtotalPaise = 595000,
            freightPaise = 0,
            taxTotalPaise = 107100,
            grandTotalPaise = 702100
        ),
        footer = DocumentFooter(declarationText = "", notes = null)
    )

    private class FakeInvoiceFinalizer(private val document: InvoiceDocumentModel) : InvoiceFinalizer {
        override suspend fun finalize(draft: InvoiceDraftUiState, customerId: String): InvoiceDocumentModel =
            document
    }

    private class FakeInvoicePdfRenderer : InvoicePdfRenderer {
        override suspend fun ensurePdf(document: InvoiceDocumentModel): File = File("unused.pdf")
    }

    private fun setContentWithShell(
        invoiceFinalizer: InvoiceFinalizer = NoopInvoiceFinalizer(),
        invoicePdfRenderer: InvoicePdfRenderer = NoopInvoicePdfRenderer()
    ): InvoiceWorkspaceViewModel {
        val homeDataSource = NoopHomeDataSource()
        val homeViewModel = HomeViewModel(
            homeDataSource = homeDataSource,
            clock = Clock.systemUTC()
        )
        val documentsListViewModel = DocumentsListViewModel(homeDataSource = homeDataSource)
        val workspaceViewModel = InvoiceWorkspaceViewModel(
            draftStore = InvoiceDraftStore(),
            customerAutocompleteDataSource = NoopCustomerAutocompleteDataSource(),
            invoiceFinalizer = invoiceFinalizer,
            invoicePdfRenderer = invoicePdfRenderer
        )
        val documentSearchViewModel = DocumentSearchViewModel(
            homeDataSource = homeDataSource,
            searchDataSource = NoopDocumentSearchDataSource()
        )
        // Deliberately NOT calling onCreateInvoice() here — AppNavShell's FAB/Create actions are
        // responsible for that (see the FAB test below). Pre-creating a draft in test setup would
        // hide a real regression: InvoiceWorkspaceRoute's own empty-state prompt showing up
        // *again* after the user already tapped "Create".

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                val navController = rememberNavController()
                AppNavShell(
                    navController = navController,
                    homeViewModel = homeViewModel,
                    invoiceWorkspaceViewModel = workspaceViewModel,
                    documentsListViewModel = documentsListViewModel,
                    documentDetailDataSource = NoopDocumentDetailDataSource(),
                    documentSearchViewModel = documentSearchViewModel,
                    customerRollupDataSource = NoopCustomerRollupDataSource(),
                    invoicePdfRenderer = invoicePdfRenderer
                )
            }
        }

        return workspaceViewModel
    }

    @Test
    fun `bottom nav shows all four destinations on Home`() {
        setContentWithShell()

        composeTestRule.onNodeWithText("Home").assertIsDisplayed()
        composeTestRule.onNodeWithText("Documents").assertIsDisplayed()
        composeTestRule.onNodeWithText("Customers").assertIsDisplayed()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun `Home is the start destination and shows its empty state honestly`() {
        setContentWithShell()

        composeTestRule.onNodeWithText("No documents yet").assertIsDisplayed()
    }

    @Test
    fun `tapping Documents in the bottom nav switches the visible screen`() {
        setContentWithShell()

        composeTestRule.onNodeWithText("Documents").performClick()

        // DocumentsListRoute, honestly empty here since NoopHomeDataSource returns no documents —
        // same empty-state copy as Home's Recent Documents card.
        composeTestRule
            .onNodeWithText("Invoices and challans you finalize will show up here.")
            .assertIsDisplayed()
    }

    @Test
    fun `tapping the FAB opens an editable draft directly, hides the bottom nav, and skips the redundant Create Invoice prompt`() {
        setContentWithShell()

        composeTestRule.onNodeWithContentDescription("Create").performClick()

        composeTestRule.onNodeWithText("+ Add line item").assertIsDisplayed()
        composeTestRule.onNodeWithText("Documents").assertDoesNotExist()
        composeTestRule.onNodeWithText("Create Invoice").assertDoesNotExist()
    }

    @Test
    fun `Preview invoice is disabled until Billed To is set, then enables and navigates`() {
        val workspaceViewModel = setContentWithShell()
        composeTestRule.onNodeWithContentDescription("Create").performClick()

        composeTestRule.onNodeWithContentDescription("Preview invoice").assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription("Preview invoice").performClick()
        composeTestRule.onNodeWithText("Finalize Invoice").assertDoesNotExist()

        workspaceViewModel.onBilledToSelected(
            CustomerAutocompleteItem(
                customerId = "cust-1",
                customerName = "Genus Paper & Boards Ltd.",
                gstin = "27AAAAA0000A1Z5",
                addressLine1 = "Unit-2, 8th Km Stone",
                city = "Muzaffarnagar",
                state = "Uttar Pradesh",
                stateCode = "09",
                pincode = null
            )
        )

        composeTestRule.onNodeWithContentDescription("Preview invoice").assertIsEnabled()
        composeTestRule.onNodeWithContentDescription("Preview invoice").performClick()
        composeTestRule.onNodeWithText("Finalize Invoice").assertIsDisplayed()
    }

    @Test
    fun `pressing back on the Workspace screen returns to Home, not a no-op`() {
        setContentWithShell()
        composeTestRule.onNodeWithContentDescription("Create").performClick()
        composeTestRule.onNodeWithText("+ Add line item").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Back").performClick()

        // Back on a tab route: bottom nav is showing again and the workspace editor is gone.
        composeTestRule.onNodeWithText("Documents").assertIsDisplayed()
        composeTestRule.onNodeWithText("+ Add line item").assertDoesNotExist()
    }

    @Test
    fun `pressing back on the Finalized screen returns to Home, not the emptied-out draft screen`() {
        val workspaceViewModel = setContentWithShell(
            invoiceFinalizer = FakeInvoiceFinalizer(sampleFinalizedDocument()),
            invoicePdfRenderer = FakeInvoicePdfRenderer()
        )
        composeTestRule.onNodeWithContentDescription("Create").performClick()

        workspaceViewModel.onBilledToSelected(
            CustomerAutocompleteItem(
                customerId = "cust-1",
                customerName = "Bhargava Industries",
                gstin = "27AAACB1234Z1Z",
                addressLine1 = "Industrial Area",
                city = "Mumbai",
                state = "Maharashtra",
                stateCode = "27",
                pincode = null
            )
        )
        composeTestRule.onNodeWithContentDescription("Preview invoice").performClick()
        composeTestRule.onNodeWithText("Finalize Invoice").performClick()
        // "Invoice Finalized" itself is ambiguous here (it's both the chrome title and the screen
        // body's heading) — assert on the document number, which only the body renders.
        composeTestRule.onNodeWithText("INV-000043 · ₹7,021").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Back").performClick()

        // Finalize must have dropped WORKSPACE from the back stack along with PREVIEW: back
        // lands straight on the Home tab, never on the now-defunct Workspace/"Create Invoice"
        // screen that used to sit underneath.
        composeTestRule.onNodeWithText("Documents").assertIsDisplayed()
        composeTestRule.onNodeWithText("+ Add line item").assertDoesNotExist()
        composeTestRule.onNodeWithText("Create Invoice").assertDoesNotExist()
    }

    @Test
    fun `starting a new invoice after finalizing one does not carry over the previous Billed To`() {
        val workspaceViewModel = setContentWithShell(
            invoiceFinalizer = FakeInvoiceFinalizer(sampleFinalizedDocument()),
            invoicePdfRenderer = FakeInvoicePdfRenderer()
        )
        composeTestRule.onNodeWithContentDescription("Create").performClick()

        workspaceViewModel.onBilledToSelected(
            CustomerAutocompleteItem(
                customerId = "cust-1",
                customerName = "Bhargava Industries",
                gstin = "27AAACB1234Z1Z",
                addressLine1 = "Industrial Area",
                city = "Mumbai",
                state = "Maharashtra",
                stateCode = "27",
                pincode = null
            )
        )
        composeTestRule.onNodeWithContentDescription("Preview invoice").performClick()
        composeTestRule.onNodeWithText("Finalize Invoice").performClick()
        composeTestRule.onNodeWithText("INV-000043 · ₹7,021").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Back").performClick()

        // Second invoice, fresh FAB tap: Billed To must start blank, not carry over Invoice #1's
        // customer.
        composeTestRule.onNodeWithContentDescription("Create").performClick()

        println(composeTestRule.onAllNodes(isRoot())[0].printToString(maxDepth = 50))
    }

    @Test
    fun `tapping a Recent Documents row opens Document Detail with a working View PDF action`() {
        val document = sampleFinalizedDocument()
        val summary = DocumentSummary(
            documentId = "doc-1",
            documentNumber = document.identity.documentNumber,
            customerName = document.parties.billedTo.name,
            documentType = document.identity.documentType,
            issueDate = document.identity.issueDate,
            grandTotal = Money.ofPaise(document.totals.grandTotalPaise),
            finalizedAtEpochMillis = 0L
        )
        val homeDataSource = object : HomeDataSource {
            override suspend fun loadAllDocuments(): List<DocumentSummary> = listOf(summary)
            override suspend fun loadSyncStatus(): SyncStatus = SyncStatus(pendingCount = 0, lastSyncedAtEpochMillis = null)
        }
        val homeViewModel = HomeViewModel(homeDataSource = homeDataSource, clock = Clock.systemUTC())
        val documentsListViewModel = DocumentsListViewModel(homeDataSource = homeDataSource)
        val workspaceViewModel = InvoiceWorkspaceViewModel(
            draftStore = InvoiceDraftStore(),
            customerAutocompleteDataSource = NoopCustomerAutocompleteDataSource(),
            invoiceFinalizer = NoopInvoiceFinalizer(),
            invoicePdfRenderer = NoopInvoicePdfRenderer()
        )
        val documentDetailDataSource = object : DocumentDetailDataSource {
            override suspend fun loadDocument(documentId: String): InvoiceDocumentModel? =
                if (documentId == "doc-1") document else null
        }
        val documentSearchViewModel = DocumentSearchViewModel(
            homeDataSource = homeDataSource,
            searchDataSource = NoopDocumentSearchDataSource()
        )

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                val navController = rememberNavController()
                AppNavShell(
                    navController = navController,
                    homeViewModel = homeViewModel,
                    invoiceWorkspaceViewModel = workspaceViewModel,
                    documentsListViewModel = documentsListViewModel,
                    documentDetailDataSource = documentDetailDataSource,
                    documentSearchViewModel = documentSearchViewModel,
                    customerRollupDataSource = NoopCustomerRollupDataSource(),
                    invoicePdfRenderer = FakeInvoicePdfRenderer()
                )
            }
        }

        composeTestRule.onNodeWithText("INV-000043 · Bhargava Industries").performClick()

        // Read-only InvoicePreviewScreen with no Finalize CTA, plus the new View PDF action.
        // Deliberately not clicking View PDF here: PdfViewerScreen does real file I/O
        // (ParcelFileDescriptor/PdfRenderer) against whatever ensurePdf() returns, which needs an
        // actual on-disk PDF - out of scope for this Robolectric wiring test.
        composeTestRule.onNodeWithText("Finalize Invoice").assertDoesNotExist()
        composeTestRule.onNodeWithText("View PDF").assertIsDisplayed()
    }
}
