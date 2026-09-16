package com.verity.feature.invoice.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.verity.core.document.model.InvoiceDocumentModel
import com.verity.core.theme.VerityBaseTypography
import com.verity.core.theme.VerityTheme
import com.verity.feature.invoice.autocomplete.CustomerAutocompleteDataSource
import com.verity.feature.invoice.autocomplete.CustomerAutocompleteItem
import com.verity.feature.invoice.draft.DraftDocumentType
import com.verity.feature.invoice.draft.DraftLineItem
import com.verity.feature.invoice.draft.InvoiceDraftStore
import com.verity.feature.invoice.draft.InvoiceDraftUiState
import com.verity.feature.invoice.finalize.DocumentNumberPreviewDataSource
import com.verity.feature.invoice.finalize.InvoiceFinalizer
import com.verity.feature.invoice.finalize.JobWorkLinkage
import com.verity.feature.invoice.pdf.InvoicePdfRenderer
import com.verity.feature.referencelist.ReferenceListDataSource
import com.verity.feature.referencelist.ReferenceListItem
import com.verity.feature.referencelist.ReferenceListKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Regression coverage for the delete/Undo flow, updated for the full-screen line-item entry
 * surface (LineItemEntryScreen) that replaced the old inline VerityEditBlock — see
 * LineItemEntryScreen's header comment for why that moved. Add/Edit/Delete field interactions now
 * live in LineItemEntryScreenTest, since that logic moved to its own pure composable; this file
 * covers what's still Workspace's job: delegating "add"/"tap a row" to navigation instead of
 * opening inline UI, and showing the Undo snackbar for a deletion that happened on a screen that's
 * since popped back (InvoiceWorkspaceViewModel.lineItemDeleted).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
class InvoiceWorkspaceScreenUndoTest {

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

    private val testItem = DraftLineItem(
        description = "Test Item",
        hsnCode = "1234",
        quantity = 10,
        unit = "pcs",
        ratePaise = 10000
    )

    @Test
    fun `deleting a line item shows an Undo snackbar, and Undo restores it`() {
        val viewModel = buildViewModel()
        viewModel.onAddLineItem(testItem)

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                InvoiceWorkspaceRoute(
                    viewModel = viewModel,
                    onAddLineItem = {},
                    onEditLineItem = {},
                    canPreview = true,
                    onPreview = {},
                    onDiscard = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Test Item").assertIsDisplayed()

        // Deletion itself now happens on LineItemEntryScreen (see LineItemEntryScreenTest), which
        // pops back to Workspace right after — this simulates exactly that: the ViewModel call
        // without the entry screen's UI, since Workspace's job is only to react to the event.
        viewModel.onRemoveLineItem(0)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Test Item").assertDoesNotExist()
        composeTestRule.onNodeWithText("Undo").performClick()

        composeTestRule.onNodeWithText("Test Item").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `tapping Add line item and an existing row delegates to navigation instead of opening inline UI`() {
        val viewModel = buildViewModel()
        viewModel.onAddLineItem(testItem)

        var addRequested = false
        var editRequestedIndex: Int? = null

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                InvoiceWorkspaceRoute(
                    viewModel = viewModel,
                    onAddLineItem = { addRequested = true },
                    onEditLineItem = { index -> editRequestedIndex = index },
                    canPreview = true,
                    onPreview = {},
                    onDiscard = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Test Item").performScrollTo().performClick()
        assertEquals(0, editRequestedIndex)

        composeTestRule.onNodeWithText("+ Add line item").performScrollTo().performClick()
        assertTrue(addRequested)
    }
}
