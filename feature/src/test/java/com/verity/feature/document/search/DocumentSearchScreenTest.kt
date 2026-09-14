package com.verity.feature.document.search


import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
 * DocumentSearchScreenTest
 *
 * Pure-renderer test, same style as DocumentsListScreenTest: state in, callback out — the
 * debounce/query-flow behavior lives in DocumentSearchViewModel and is covered separately by
 * DocumentSearchViewModelTest.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
class DocumentSearchScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val recentDocument = DocumentSummary(
        documentId = "doc-recent",
        documentNumber = "INV-000001",
        customerName = "Test Buyer",
        documentType = DocumentType.INVOICE,
        issueDate = LocalDate.of(2026, 9, 14),
        grandTotal = Money.ofPaise(10_000),
        finalizedAtEpochMillis = 0L
    )

    private val documentMatch = DocumentSearchResult(
        documentId = "doc-1",
        documentNumber = "INV-000045",
        customerName = "Acme Traders",
        documentType = DocumentType.INVOICE,
        issueDate = LocalDate.of(2026, 9, 14),
        grandTotal = Money.ofPaise(4_576_000),
        matchKind = DocumentMatchKind.LINE_ITEM,
        matchedSnippet = SnippetMatch(text = "Portland Cement", matchRange = 9 until 15)
    )

    private val customerMatch = CustomerSearchResult(
        customerId = "cust-1",
        customerName = "Acme Traders",
        gstin = "27AAACB1234Z1Z"
    )

    @Test
    fun `blank query shows the Recent section and clicking a row fires onDocumentClick`() {
        var clicked: String? = null

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                DocumentSearchScreen(
                    state = DocumentSearchUiState(query = "", recentDocuments = listOf(recentDocument)),
                    onQueryChanged = {},
                    onDocumentClick = { clicked = it },
                    onCustomerClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Recent").assertIsDisplayed()
        composeTestRule.onNodeWithText("INV-000001 · Test Buyer").performClick()

        assertEquals("doc-recent", clicked)
    }

    @Test
    fun `a document match shows its highlighted snippet and fires onDocumentClick`() {
        var clicked: String? = null

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                DocumentSearchScreen(
                    state = DocumentSearchUiState(
                        query = "cement",
                        results = DocumentSearchResults(customers = emptyList(), documents = listOf(documentMatch))
                    ),
                    onQueryChanged = {},
                    onDocumentClick = { clicked = it },
                    onCustomerClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Portland Cement").assertIsDisplayed()
        composeTestRule.onNodeWithText("INV-000045 · Acme Traders").performClick()

        assertEquals("doc-1", clicked)
    }

    @Test
    fun `a customer match sits in its own group and fires onCustomerClick`() {
        var clicked: String? = null

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                DocumentSearchScreen(
                    state = DocumentSearchUiState(
                        query = "acme",
                        results = DocumentSearchResults(customers = listOf(customerMatch), documents = emptyList())
                    ),
                    onQueryChanged = {},
                    onDocumentClick = {},
                    onCustomerClick = { clicked = it }
                )
            }
        }

        composeTestRule.onNodeWithText("Customers").assertIsDisplayed()
        composeTestRule.onNodeWithText("Acme Traders").performClick()

        assertEquals("cust-1", clicked)
    }

    @Test
    fun `no matches shows the empty state with the query quoted`() {
        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                DocumentSearchScreen(
                    state = DocumentSearchUiState(query = "xyz-nomatch"),
                    onQueryChanged = {},
                    onDocumentClick = {},
                    onCustomerClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("No documents match \"xyz-nomatch\"").assertIsDisplayed()
    }
}
