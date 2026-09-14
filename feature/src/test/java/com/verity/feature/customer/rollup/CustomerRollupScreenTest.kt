package com.verity.feature.customer.rollup


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

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
class CustomerRollupScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val documents = listOf(
        DocumentSummary(
            documentId = "doc-1",
            documentNumber = "INV-000001",
            customerName = "Acme Traders",
            documentType = DocumentType.INVOICE,
            issueDate = LocalDate.of(2026, 9, 1),
            grandTotal = Money.ofPaise(1_000_00),
            finalizedAtEpochMillis = 100
        ),
        DocumentSummary(
            documentId = "doc-2",
            documentNumber = "INV-000002",
            customerName = "Acme Traders",
            documentType = DocumentType.INVOICE,
            issueDate = LocalDate.of(2026, 9, 10),
            grandTotal = Money.ofPaise(2_000_00),
            finalizedAtEpochMillis = 200
        )
    )

    @Test
    fun `header shows the document count and summed running total`() {
        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                CustomerRollupScreen(
                    state = CustomerRollupUiState(
                        isLoading = false,
                        rollup = CustomerRollup(customerId = "cust-1", customerName = "Acme Traders", documents = documents)
                    ),
                    onDocumentClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Acme Traders").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 documents · ₹3,000").assertIsDisplayed()
    }

    @Test
    fun `tapping a document row fires onDocumentClick with its documentId`() {
        var clicked: String? = null

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                CustomerRollupScreen(
                    state = CustomerRollupUiState(
                        isLoading = false,
                        rollup = CustomerRollup(customerId = "cust-1", customerName = "Acme Traders", documents = documents)
                    ),
                    onDocumentClick = { clicked = it }
                )
            }
        }

        composeTestRule.onNodeWithText("INV-000002 · Acme Traders").performClick()

        assertEquals("doc-2", clicked)
    }

    @Test
    fun `zero documents shows the empty state, not a crash`() {
        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                CustomerRollupScreen(
                    state = CustomerRollupUiState(
                        isLoading = false,
                        rollup = CustomerRollup(customerId = "cust-1", customerName = "New Customer", documents = emptyList())
                    ),
                    onDocumentClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("0 documents · ₹0").assertIsDisplayed()
        composeTestRule.onNodeWithText("No documents yet for this customer.").assertIsDisplayed()
    }

    @Test
    fun `an unknown customer shows the not-found state`() {
        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                CustomerRollupScreen(
                    state = CustomerRollupUiState(isLoading = false, rollup = null),
                    onDocumentClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Customer not found").assertIsDisplayed()
    }
}
