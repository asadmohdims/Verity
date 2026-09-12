package com.verity.feature.invoice.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
 * Regression test for the delete/Undo flow. Runs the real Compose semantics tree on the JVM via
 * Robolectric, so performClick() drives the actual click handlers — no emulator, no guessed
 * screen coordinates.
 *
 * qualifiers sets a normal phone-sized window — Robolectric's unconfigured default is a legacy
 * ~320x470dp screen, which pushes this form's lower fields/buttons below the fold and makes
 * performClick() land on the wrong node. performScrollTo() before each click on those elements
 * guards against the same class of failure regardless of window size.
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
        override suspend fun finalize(draft: InvoiceDraftUiState, customerId: String): InvoiceDocumentModel {
            error("not used in this test")
        }
    }

    private fun buildViewModel(): InvoiceWorkspaceViewModel {
        val viewModel = InvoiceWorkspaceViewModel(
            draftStore = InvoiceDraftStore(),
            customerAutocompleteDataSource = NoopCustomerAutocompleteDataSource(),
            invoiceFinalizer = NoopInvoiceFinalizer()
        )
        viewModel.onCreateInvoice()
        return viewModel
    }

    @Test
    fun `tapping Undo on the delete snackbar restores the removed line item`() {
        val viewModel = buildViewModel()

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                InvoiceWorkspaceRoute(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText("+ Add line item").performClick()
        composeTestRule.onNodeWithText("Description").performTextInput("Test Item")
        composeTestRule.onNodeWithText("Quantity").performTextInput("10")
        composeTestRule.onNodeWithText("Rate").performTextInput("100")
        composeTestRule.onNodeWithText("Add").performScrollTo().performClick()

        composeTestRule.onNodeWithText("Test Item").assertIsDisplayed()

        composeTestRule.onNodeWithText("Test Item").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Delete").performScrollTo().performClick()

        composeTestRule.onNodeWithText("Test Item").assertDoesNotExist()

        composeTestRule.onNodeWithText("Undo").performClick()

        composeTestRule.onNodeWithText("Test Item").performScrollTo().assertIsDisplayed()
    }
}
