package com.verity.feature.invoice.preview

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.verity.core.document.model.DocumentFooter
import com.verity.core.document.model.DocumentIdentity
import com.verity.core.document.model.DocumentParties
import com.verity.core.document.model.DocumentParty
import com.verity.core.document.model.DocumentTaxation
import com.verity.core.document.model.DocumentTaxMode
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
class InvoicePreviewScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun sampleParty() = DocumentParty(
        name = "Bhargava Industries",
        gstin = "27AAACB1234Z1Z",
        addressLines = listOf("Industrial Area", "Mumbai"),
        state = "Maharashtra",
        stateCode = "27"
    )

    private fun sampleDocument(
        documentType: DocumentType,
        taxation: DocumentTaxation?
    ) = InvoiceDocumentModel(
        identity = DocumentIdentity(
            documentType = documentType,
            documentNumber = "INV-000043",
            issueDate = LocalDate.of(2026, 9, 13),
            seller = HARDCODED_SELLER,
            placeOfSupplyState = "Maharashtra",
            placeOfSupplyStateCode = "27"
        ),
        parties = DocumentParties(
            billedTo = sampleParty(),
            shippedTo = sampleParty()
        ),
        lineItems = emptyList(),
        logistics = null,
        taxation = taxation,
        totals = DocumentTotals(
            itemsSubtotalPaise = 595000,
            freightPaise = 0,
            taxTotalPaise = if (taxation != null) 107100 else 0,
            grandTotalPaise = if (taxation != null) 702100 else 595000
        ),
        footer = DocumentFooter(declarationText = "", notes = null)
    )

    @Test
    fun `shows Finalize Invoice for an invoice document with a tax row`() {
        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                InvoicePreviewScreen(
                    document = sampleDocument(
                        documentType = DocumentType.INVOICE,
                        taxation = DocumentTaxation(mode = DocumentTaxMode.INTRA_STATE, cgst = null, sgst = null, igst = null)
                    ),
                    onBack = {},
                    onFinalize = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Finalize Invoice").assertIsDisplayed()
        composeTestRule.onNodeWithText("CGST + SGST").assertIsDisplayed()
    }

    @Test
    fun `shows Finalize Challan and no tax row for a challan document`() {
        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                InvoicePreviewScreen(
                    document = sampleDocument(documentType = DocumentType.CHALLAN, taxation = null),
                    onBack = {},
                    onFinalize = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Finalize Challan").assertIsDisplayed()
        composeTestRule.onNodeWithText("CGST + SGST").assertDoesNotExist()
        composeTestRule.onNodeWithText("IGST").assertDoesNotExist()
    }

    @Test
    fun `tapping Finalize invokes the callback`() {
        var tapped = false

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                InvoicePreviewScreen(
                    document = sampleDocument(documentType = DocumentType.INVOICE, taxation = null),
                    onBack = {},
                    onFinalize = { tapped = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Finalize Invoice").performClick()

        assertTrue("Expected onFinalize to be invoked", tapped)
    }
}
