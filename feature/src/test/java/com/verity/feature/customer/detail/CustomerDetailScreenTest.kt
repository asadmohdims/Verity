package com.verity.feature.customer.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
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
class CustomerDetailScreenTest {

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

    private fun sampleDetail(documents: List<DocumentSummary>, balanceDuePaise: Long = 3_000_00) = CustomerDetail(
        customerId = "cust-1",
        customerName = "Acme Traders",
        phone = "9999999999",
        gstin = "27AAACB1234Z1Z",
        addressLine1 = "Industrial Area",
        city = "Mumbai",
        state = "Maharashtra",
        stateCode = "27",
        pincode = "400001",
        notes = null,
        balanceDue = Money.ofPaise(balanceDuePaise),
        documents = documents
    )

    @Test
    fun `profile header and balance hero show the customer's real data`() {
        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                CustomerDetailScreen(
                    state = CustomerDetailUiState(isLoading = false, detail = sampleDetail(documents)),
                    onDocumentClick = {},
                    onNewInvoice = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Acme Traders").assertIsDisplayed()
        composeTestRule.onNodeWithText("₹3,000").assertIsDisplayed()
        composeTestRule.onNodeWithText("Across 2 documents").assertIsDisplayed()
    }

    @Test
    fun `Record Payment quick action is disabled - no Payment entity exists yet`() {
        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                CustomerDetailScreen(
                    state = CustomerDetailUiState(isLoading = false, detail = sampleDetail(documents)),
                    onDocumentClick = {},
                    onNewInvoice = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Record Payment").assertIsNotEnabled()
    }

    @Test
    fun `tapping New Invoice fires onNewInvoice`() {
        var tapped = false

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                CustomerDetailScreen(
                    state = CustomerDetailUiState(isLoading = false, detail = sampleDetail(documents)),
                    onDocumentClick = {},
                    onNewInvoice = { tapped = true }
                )
            }
        }

        composeTestRule.onNodeWithText("New Invoice").performClick()

        assertEquals(true, tapped)
    }

    @Test
    fun `tapping a document row fires onDocumentClick with its documentId`() {
        var clicked: String? = null

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                CustomerDetailScreen(
                    state = CustomerDetailUiState(isLoading = false, detail = sampleDetail(documents)),
                    onDocumentClick = { clicked = it },
                    onNewInvoice = {}
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
                CustomerDetailScreen(
                    state = CustomerDetailUiState(
                        isLoading = false,
                        detail = sampleDetail(documents = emptyList(), balanceDuePaise = 0)
                    ),
                    onDocumentClick = {},
                    onNewInvoice = {}
                )
            }
        }

        composeTestRule.onNodeWithText("No documents yet for this customer.").assertIsDisplayed()
    }

    @Test
    fun `an unknown customer shows the not-found state`() {
        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                CustomerDetailScreen(
                    state = CustomerDetailUiState(isLoading = false, detail = null),
                    onDocumentClick = {},
                    onNewInvoice = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Customer not found").assertIsDisplayed()
    }
}
