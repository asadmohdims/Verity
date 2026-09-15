package com.verity.feature.invoice.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.verity.core.document.model.InvoiceDocumentModel
import com.verity.core.theme.VerityBaseTypography
import com.verity.core.theme.VerityTheme
import com.verity.feature.invoice.autocomplete.CustomerAutocompleteDataSource
import com.verity.feature.invoice.autocomplete.CustomerAutocompleteItem
import com.verity.feature.invoice.draft.InvoiceDraftStore
import com.verity.feature.invoice.draft.InvoiceDraftUiState
import com.verity.feature.invoice.finalize.InvoiceFinalizer
import com.verity.feature.invoice.pdf.InvoicePdfRenderer
import com.verity.feature.referencelist.ReferenceListDataSource
import com.verity.feature.referencelist.ReferenceListItem
import com.verity.feature.referencelist.ReferenceListKind
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Regression test for Billed To's EditBlock pattern (read-only label+value once a customer is
 * selected -> tap to reopen the search field -> selecting a suggestion commits and collapses
 * again) — see InvoiceWorkspaceScreen.kt's PartyField and the plan this implements.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
class InvoiceWorkspacePartyFieldTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val bhargavaIndustries = CustomerAutocompleteItem(
        customerId = "cust-1",
        customerName = "Bhargava Industries",
        gstin = "27AAACB1234Z1Z",
        addressLine1 = "Industrial Area",
        city = "Mumbai",
        state = "Maharashtra",
        stateCode = "27",
        pincode = "400001"
    )

    private class FakeCustomerAutocompleteDataSource(
        private val items: List<CustomerAutocompleteItem>
    ) : CustomerAutocompleteDataSource {
        override suspend fun recentCustomers(limit: Int): List<CustomerAutocompleteItem> = emptyList()
        override suspend fun searchCustomers(query: String, limit: Int): List<CustomerAutocompleteItem> =
            items.filter { it.customerName.contains(query, ignoreCase = true) }
    }

    private class NoopInvoiceFinalizer : InvoiceFinalizer {
        override suspend fun finalize(draft: InvoiceDraftUiState, customerId: String): InvoiceDocumentModel {
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

    private fun buildViewModel(): InvoiceWorkspaceViewModel {
        val viewModel = InvoiceWorkspaceViewModel(
            draftStore = InvoiceDraftStore(),
            customerAutocompleteDataSource = FakeCustomerAutocompleteDataSource(listOf(bhargavaIndustries)),
            invoiceFinalizer = NoopInvoiceFinalizer(),
            invoicePdfRenderer = NoopInvoicePdfRenderer(),
            referenceListDataSource = NoopReferenceListDataSource()
        )
        viewModel.onCreateInvoice()
        return viewModel
    }

    @Test
    fun `selecting a billed-to customer collapses the field to a read-only row`() {
        val viewModel = buildViewModel()

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                InvoiceWorkspaceRoute(viewModel = viewModel)
            }
        }

        // Not yet selected: the collapsed EditBlock action prompt is showing, not a read-only row.
        composeTestRule.onNodeWithText("+ Add billed-to party").assertIsDisplayed()

        composeTestRule.onNodeWithText("+ Add billed-to party").performClick()
        // Material3's OutlinedTextField only shows its placeholder once focused — target the
        // field by its always-present label instead of the "Search customer" placeholder text.
        composeTestRule.onNodeWithText("Billed To").performTextInput("Bhargava")
        composeTestRule.onNodeWithText("Bhargava Industries").performClick()

        // Selecting the suggestion is the commit — no separate Save tap — and the field collapses
        // back to a read-only label+value row. Shipped To has no override yet, so it now defaults
        // to Billed To's value too (see InvoiceDraftUiState.effectiveShippedTo) — a second, equally
        // real "Bhargava Industries" row, not a duplicate to dedupe away.
        composeTestRule.onAllNodesWithText("Bhargava Industries").assertCountEquals(2)
        composeTestRule.onNodeWithText("+ Add billed-to party").assertDoesNotExist()
    }

    @Test
    fun `tapping the collapsed read-only row reopens the search field`() {
        val viewModel = buildViewModel()

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                InvoiceWorkspaceRoute(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText("+ Add billed-to party").performClick()
        composeTestRule.onNodeWithText("Billed To").performTextInput("Bhargava")
        composeTestRule.onNodeWithText("Bhargava Industries").performClick()

        // Tap the now-collapsed read-only row to reopen the search field. Billed To and Shipped
        // To both show "Bhargava Industries" now (Shipped To defaults to Billed To until
        // explicitly overridden) — index 0 is Billed To's row, declared first in the layout.
        composeTestRule.onAllNodesWithText("Bhargava Industries")[0].performClick()

        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun `starting a new invoice does not carry the previous Billed To into the search field`() {
        val viewModel = buildViewModel()

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                InvoiceWorkspaceRoute(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText("+ Add billed-to party").performClick()
        composeTestRule.onNodeWithText("Billed To").performTextInput("Bhargava")
        composeTestRule.onNodeWithText("Bhargava Industries").performClick()

        // Simulates the FAB starting a fresh invoice after finalize (see AppNavShell.goToWorkspace
        // / InvoiceWorkspaceViewModel.onFinalizeInvoice, which clears hasActiveDraft so the next
        // FAB tap calls this).
        viewModel.onCreateInvoice()

        // Billed To reads as unset again...
        composeTestRule.onNodeWithText("+ Add billed-to party").assertIsDisplayed()

        // ...and, critically, reopening it shows a genuinely blank search field rather than the
        // previous invoice's customer already typed in. The bug: onSelectSuggestion() used to call
        // onBilledToQueryChanged(name) right after onBilledToSelected() had already cleared that
        // same query back to "" — re-populating it (and, via that query, the suggestions list)
        // with the just-selected customer for no UI reason (the field was about to collapse to
        // the read-only row anyway, which doesn't read billedToQuery at all). onCreateInvoice()
        // never reset that separate autocomplete-UI state, so it silently carried into the next
        // invoice's fresh Billed To field, looking auto-populated the moment it was opened.
        composeTestRule.onNodeWithText("+ Add billed-to party").performClick()
        composeTestRule.onNodeWithText("Bhargava Industries").assertDoesNotExist()
    }
}
