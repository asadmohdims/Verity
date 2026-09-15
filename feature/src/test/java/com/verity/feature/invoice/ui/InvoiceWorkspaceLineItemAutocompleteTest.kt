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
 * Regression coverage for the "dropdown" behavior on HSN Code / Unit: tapping into the field
 * alone (no typing) should reveal the full curated list, matching the request to have these
 * feel like a picker for a short, closed-ish list — see VerityTextField's
 * expandSuggestionsOnFocus parameter and CLAUDE.md's "never a popup/dropdown menu" rule this
 * stays inline to satisfy.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
class InvoiceWorkspaceLineItemAutocompleteTest {

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

    private class NoopInvoicePdfRenderer : InvoicePdfRenderer {
        override suspend fun ensurePdf(document: InvoiceDocumentModel): java.io.File {
            error("not used in this test")
        }
    }

    private class FakeReferenceListDataSource(
        private val itemsByKind: Map<ReferenceListKind, List<String>>
    ) : ReferenceListDataSource {
        override suspend fun getAll(kind: ReferenceListKind): List<ReferenceListItem> =
            (itemsByKind[kind] ?: emptyList()).mapIndexed { index, value ->
                ReferenceListItem(id = "$kind-$index", value = value)
            }

        override suspend fun add(kind: ReferenceListKind, value: String) {}
        override suspend fun delete(kind: ReferenceListKind, id: String) {}
    }

    private fun buildViewModel(): InvoiceWorkspaceViewModel {
        val viewModel = InvoiceWorkspaceViewModel(
            draftStore = InvoiceDraftStore(),
            customerAutocompleteDataSource = NoopCustomerAutocompleteDataSource(),
            invoiceFinalizer = NoopInvoiceFinalizer(),
            invoicePdfRenderer = NoopInvoicePdfRenderer(),
            referenceListDataSource = FakeReferenceListDataSource(
                mapOf(
                    ReferenceListKind.HSN_CODE to listOf("7208", "7209"),
                    ReferenceListKind.UNIT to listOf("PCS", "KG")
                )
            )
        )
        viewModel.onCreateInvoice()
        return viewModel
    }

    @Test
    fun `tapping HSN Code without typing reveals the curated list`() {
        val viewModel = buildViewModel()

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                InvoiceWorkspaceRoute(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText("+ Add line item").performClick()
        composeTestRule.onNodeWithText("HSN Code").performScrollTo().performClick()

        composeTestRule.onNodeWithText("7208").assertIsDisplayed()
        composeTestRule.onNodeWithText("7209").assertIsDisplayed()
    }

    @Test
    fun `selecting an HSN suggestion fills the field and hides the panel`() {
        val viewModel = buildViewModel()

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                InvoiceWorkspaceRoute(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText("+ Add line item").performClick()
        composeTestRule.onNodeWithText("HSN Code").performScrollTo().performClick()
        composeTestRule.onNodeWithText("7208").performClick()

        composeTestRule.onNodeWithText("7209").assertDoesNotExist()
    }

    @Test
    fun `tapping Unit without typing reveals the curated list`() {
        val viewModel = buildViewModel()

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                InvoiceWorkspaceRoute(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText("+ Add line item").performClick()
        composeTestRule.onNodeWithText("Unit").performScrollTo().performClick()

        composeTestRule.onNodeWithText("PCS").assertIsDisplayed()
        composeTestRule.onNodeWithText("KG").assertIsDisplayed()
    }
}
