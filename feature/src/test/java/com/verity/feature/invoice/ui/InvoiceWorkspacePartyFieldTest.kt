package com.verity.feature.invoice.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
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

    private fun buildViewModel(): InvoiceWorkspaceViewModel {
        val viewModel = InvoiceWorkspaceViewModel(
            draftStore = InvoiceDraftStore(),
            customerAutocompleteDataSource = FakeCustomerAutocompleteDataSource(listOf(bhargavaIndustries)),
            invoiceFinalizer = NoopInvoiceFinalizer()
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
        // back to a read-only label+value row.
        composeTestRule.onNodeWithText("Bhargava Industries").assertIsDisplayed()
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

        // Tap the now-collapsed read-only row to reopen the search field.
        composeTestRule.onNodeWithText("Bhargava Industries").performClick()

        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }
}
