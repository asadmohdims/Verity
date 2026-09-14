package com.verity.feature.invoice.preview

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.verity.core.document.model.DocumentFooter
import com.verity.core.document.model.DocumentIdentity
import com.verity.core.document.model.DocumentParties
import com.verity.core.document.model.DocumentParty
import com.verity.core.document.model.DocumentTotals
import com.verity.core.document.model.DocumentType
import com.verity.core.document.model.HARDCODED_SELLER
import com.verity.core.document.model.InvoiceDocumentModel
import com.verity.core.theme.VerityBaseTypography
import com.verity.core.theme.VerityTheme
import org.junit.Assert.assertTrue
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
class InvoiceFinalizedScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun sampleParty() = DocumentParty(
        name = "Bhargava Industries",
        gstin = "27AAACB1234Z1Z",
        addressLines = listOf("Industrial Area", "Mumbai"),
        state = "Maharashtra",
        stateCode = "27"
    )

    private fun sampleDocument() = InvoiceDocumentModel(
        identity = DocumentIdentity(
            documentType = DocumentType.INVOICE,
            documentNumber = "INV-000043",
            issueDate = LocalDate.of(2026, 9, 13),
            seller = HARDCODED_SELLER
        ),
        parties = DocumentParties(
            billedTo = sampleParty(),
            shippedTo = sampleParty()
        ),
        lineItems = emptyList(),
        logistics = null,
        taxation = null,
        totals = DocumentTotals(
            itemsSubtotalPaise = 595000,
            freightPaise = 0,
            taxTotalPaise = 107100,
            grandTotalPaise = 702100
        ),
        footer = DocumentFooter(declarationText = "", notes = null)
    )

    @Test
    fun `shows the title and document number with grand total`() {
        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                InvoiceFinalizedScreen(document = sampleDocument(), onViewDocument = {})
            }
        }

        composeTestRule.onNodeWithText("Invoice Finalized").assertIsDisplayed()
        composeTestRule.onNodeWithText("INV-000043 · ₹7,021").assertIsDisplayed()
    }

    @Test
    fun `tapping View Document invokes the callback`() {
        var tapped = false

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                InvoiceFinalizedScreen(document = sampleDocument(), onViewDocument = { tapped = true })
            }
        }

        composeTestRule.onNodeWithText("View Document").performClick()

        assertTrue("Expected onViewDocument to be invoked", tapped)
    }
}
