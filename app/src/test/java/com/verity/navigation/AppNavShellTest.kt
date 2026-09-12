package com.verity.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import com.verity.core.document.model.InvoiceDocumentModel
import com.verity.core.theme.VerityBaseTypography
import com.verity.core.theme.VerityTheme
import com.verity.feature.home.DocumentSummary
import com.verity.feature.home.HomeDataSource
import com.verity.feature.home.HomeViewModel
import com.verity.feature.invoice.autocomplete.CustomerAutocompleteDataSource
import com.verity.feature.invoice.autocomplete.CustomerAutocompleteItem
import com.verity.feature.invoice.draft.InvoiceDraftStore
import com.verity.feature.invoice.draft.InvoiceDraftUiState
import com.verity.feature.invoice.finalize.InvoiceFinalizer
import com.verity.feature.invoice.ui.InvoiceWorkspaceViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Clock

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

    private fun setContentWithShell() {
        val homeViewModel = HomeViewModel(
            homeDataSource = NoopHomeDataSource(),
            clock = Clock.systemUTC()
        )
        val workspaceViewModel = InvoiceWorkspaceViewModel(
            draftStore = InvoiceDraftStore(),
            customerAutocompleteDataSource = NoopCustomerAutocompleteDataSource(),
            invoiceFinalizer = NoopInvoiceFinalizer()
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
                    invoiceWorkspaceViewModel = workspaceViewModel
                )
            }
        }
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

        composeTestRule
            .onNodeWithText("Every invoice and challan you finalize will be listed and searchable here soon.")
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
}
