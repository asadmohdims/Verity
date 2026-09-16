package com.verity.feature.invoice.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.verity.core.document.model.InvoiceDocumentModel
import com.verity.core.theme.VerityBaseTypography
import com.verity.core.theme.VerityTheme
import com.verity.feature.invoice.autocomplete.CustomerAutocompleteDataSource
import com.verity.feature.invoice.autocomplete.CustomerAutocompleteItem
import com.verity.feature.invoice.draft.DraftDocumentType
import com.verity.feature.invoice.draft.InvoiceDraftStore
import com.verity.feature.invoice.draft.InvoiceDraftUiState
import com.verity.feature.invoice.finalize.DocumentNumberPreviewDataSource
import com.verity.feature.invoice.finalize.InvoiceFinalizer
import com.verity.feature.invoice.finalize.JobWorkLinkage
import com.verity.feature.invoice.pdf.InvoicePdfRenderer
import com.verity.feature.referencelist.ReferenceListDataSource
import com.verity.feature.referencelist.ReferenceListItem
import com.verity.feature.referencelist.ReferenceListKind
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Covers the new "Challan + Invoice" Document Type option: selecting it reveals the Job Work
 * section (the always-available "Received Vide Challan" reference block, plus a placeholder for
 * the auto-assigned Invoice number), and saving that block updates its read-only summary. The
 * cross-document finalize/continue flow itself (reserving a real Invoice number, opening the
 * pre-filled continuation draft, locking Document Type on it) is covered by
 * DefaultInvoiceFinalizerTest's real-Room job-work tests and needs an on-device pass per this
 * project's testing standards - it isn't re-derived here with a fake async finalize, since that
 * would mostly be testing coroutine plumbing this file doesn't otherwise need.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
class InvoiceWorkspaceJobWorkFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private class NoopCustomerAutocompleteDataSource : CustomerAutocompleteDataSource {
        override suspend fun recentCustomers(limit: Int): List<CustomerAutocompleteItem> = emptyList()
        override suspend fun searchCustomers(query: String, limit: Int): List<CustomerAutocompleteItem> = emptyList()
    }

    private class NoopInvoiceFinalizer : InvoiceFinalizer {
        override suspend fun finalize(
            draft: InvoiceDraftUiState,
            customerId: String,
            jobWorkLinkage: JobWorkLinkage
        ): InvoiceDocumentModel {
            error("not used in this test")
        }
    }

    private class NoopInvoicePdfRenderer : InvoicePdfRenderer {
        override suspend fun ensurePdf(document: InvoiceDocumentModel): java.io.File {
            error("not used in this test")
        }
    }

    private class NoopReferenceListDataSource : ReferenceListDataSource {
        override suspend fun getAll(kind: ReferenceListKind): List<ReferenceListItem> = emptyList()
        override suspend fun add(kind: ReferenceListKind, value: String) {}
        override suspend fun delete(kind: ReferenceListKind, id: String) {}
    }

    private class NoopDocumentNumberPreviewDataSource : DocumentNumberPreviewDataSource {
        override suspend fun peekNextNumber(documentType: DraftDocumentType): String = "INV-000001"
    }

    private fun buildViewModel(): InvoiceWorkspaceViewModel {
        val viewModel = InvoiceWorkspaceViewModel(
            draftStore = InvoiceDraftStore(),
            customerAutocompleteDataSource = NoopCustomerAutocompleteDataSource(),
            invoiceFinalizer = NoopInvoiceFinalizer(),
            invoicePdfRenderer = NoopInvoicePdfRenderer(),
            referenceListDataSource = NoopReferenceListDataSource(),
            documentNumberPreviewDataSource = NoopDocumentNumberPreviewDataSource()
        )
        viewModel.onCreateInvoice()
        return viewModel
    }

    @Test
    fun `selecting Challan plus Invoice reveals the Job Work section`() {
        val viewModel = buildViewModel()

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                InvoiceWorkspaceRoute(viewModel = viewModel, onAddLineItem = {}, onEditLineItem = {})
            }
        }

        // Opens the Document Type dropdown (the closed field currently reads "Invoice").
        composeTestRule.onNodeWithText("Invoice").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Challan + Invoice").performClick()

        composeTestRule.onNodeWithText("Challan + Invoice").assertIsDisplayed()
        // VerityEditBlock's collapsed action label is always rendered as "+ <label>".
        composeTestRule.onNodeWithText("+ Add received-vide-challan reference").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("(assigned automatically at finalize)").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `saving the Received Vide Challan block shows a read-only summary row`() {
        val viewModel = buildViewModel()
        viewModel.onDocumentTypeChanged(DraftDocumentType.CHALLAN)

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                InvoiceWorkspaceRoute(viewModel = viewModel, onAddLineItem = {}, onEditLineItem = {})
            }
        }

        composeTestRule.onNodeWithText("+ Add received-vide-challan reference").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Received Vide Challan No.").performScrollTo().performTextInput("CUST-CH-0042")
        composeTestRule.onNodeWithText("Add").performScrollTo().performClick()

        composeTestRule.onNodeWithText("CUST-CH-0042", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `plain Challan selection does not show the job work placeholder row`() {
        val viewModel = buildViewModel()

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                InvoiceWorkspaceRoute(viewModel = viewModel, onAddLineItem = {}, onEditLineItem = {})
            }
        }

        composeTestRule.onNodeWithText("Invoice").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Challan").performClick()

        composeTestRule.onNodeWithText("(assigned automatically at finalize)").assertDoesNotExist()
        // The always-available inbound-reference block should still be there for a plain Challan.
        assertTrue(
            composeTestRule.onAllNodesWithText("+ Add received-vide-challan reference").fetchSemanticsNodes().isNotEmpty()
        )
    }
}
