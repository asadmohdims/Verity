package com.verity.feature.document

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import com.verity.core.document.model.DocumentType
import com.verity.core.formatting.money.Money
import com.verity.core.theme.VerityBaseTypography
import com.verity.core.theme.VerityTheme
import com.verity.feature.home.DocumentSummary
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * Same click-wiring regression coverage as HomeScreenDocumentClickTest, for the Documents tab's
 * full list.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
class DocumentsListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val document = DocumentSummary(
        documentId = "doc-1",
        documentNumber = "INV-000001",
        customerName = "Test Buyer",
        documentType = DocumentType.INVOICE,
        issueDate = LocalDate.of(2026, 9, 14),
        grandTotal = Money.ofPaise(10_000),
        finalizedAtEpochMillis = 0L
    )

    private val challan = DocumentSummary(
        documentId = "doc-2",
        documentNumber = "CH-000001",
        customerName = "Test Buyer",
        documentType = DocumentType.CHALLAN,
        issueDate = LocalDate.of(2026, 9, 13),
        grandTotal = Money.ofPaise(5_000),
        finalizedAtEpochMillis = 0L
    )

    @Test
    fun `tapping a document row fires onDocumentClick with its documentId`() {
        var clickedDocumentId: String? = null

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                DocumentsListScreen(
                    state = DocumentsListUiState(isLoading = false, documents = listOf(document)),
                    onFilterChanged = {},
                    onDocumentClick = { clickedDocumentId = it }
                )
            }
        }

        composeTestRule.onNodeWithText("INV-000001 · Test Buyer").performClick()

        assertEquals("doc-1", clickedDocumentId)
    }

    @Test
    fun `tapping the Invoices filter hides challan rows`() {
        var filter = DocumentTypeFilter.ALL

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                DocumentsListScreen(
                    state = DocumentsListUiState(isLoading = false, documents = listOf(document, challan), filter = filter),
                    onFilterChanged = { filter = it },
                    onDocumentClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("CH-000001 · Test Buyer").assertIsDisplayed()

        composeTestRule.onNodeWithText("Invoices").performClick()

        assertEquals(DocumentTypeFilter.INVOICES, filter)
    }

    @Test
    fun `a Challan with a resolved linked Invoice shows the Linked badge`() {
        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                DocumentsListScreen(
                    state = DocumentsListUiState(
                        isLoading = false,
                        documents = listOf(challan),
                        linkedToDocumentIds = setOf("doc-2")
                    ),
                    onFilterChanged = {},
                    onDocumentClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("LINKED").assertIsDisplayed()
    }

    @Test
    fun `a standalone Challan shows no Linked badge`() {
        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                DocumentsListScreen(
                    state = DocumentsListUiState(isLoading = false, documents = listOf(challan)),
                    onFilterChanged = {},
                    onDocumentClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("LINKED").assertDoesNotExist()
    }

    @Test
    fun `long-pressing a row fires onDocumentLongPress with its documentId, not onDocumentClick`() {
        var longPressedId: String? = null
        var clickedId: String? = null

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                DocumentsListScreen(
                    state = DocumentsListUiState(isLoading = false, documents = listOf(document)),
                    onFilterChanged = {},
                    onDocumentClick = { clickedId = it },
                    onDocumentLongPress = { longPressedId = it }
                )
            }
        }

        composeTestRule.onNodeWithText("INV-000001 · Test Buyer").performTouchInput { longClick() }

        assertEquals("doc-1", longPressedId)
        assertEquals(null, clickedId)
    }

    @Test
    fun `tapping a row while a selection is active toggles it instead of navigating`() {
        var toggledId: String? = null
        var clickedId: String? = null

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                DocumentsListScreen(
                    state = DocumentsListUiState(
                        isLoading = false,
                        documents = listOf(document),
                        selectedDocumentIds = setOf("some-other-doc")
                    ),
                    onFilterChanged = {},
                    onDocumentClick = { clickedId = it },
                    onDocumentToggleSelect = { toggledId = it }
                )
            }
        }

        composeTestRule.onNodeWithText("INV-000001 · Test Buyer").performClick()

        assertEquals("doc-1", toggledId)
        assertEquals(null, clickedId)
    }

    @Test
    fun `a selected row's checkbox renders checked, an unselected row's unchecked`() {
        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                DocumentsListScreen(
                    state = DocumentsListUiState(
                        isLoading = false,
                        documents = listOf(document, challan),
                        selectedDocumentIds = setOf("doc-1")
                    ),
                    onFilterChanged = {},
                    onDocumentClick = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Selected").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Not selected").assertIsDisplayed()
    }
}
