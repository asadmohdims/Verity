package com.verity.feature.document

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

    @Test
    fun `tapping a document row fires onDocumentClick with its documentId`() {
        var clickedDocumentId: String? = null

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                DocumentsListScreen(
                    state = DocumentsListUiState(isLoading = false, documents = listOf(document)),
                    onDocumentClick = { clickedDocumentId = it }
                )
            }
        }

        composeTestRule.onNodeWithText("INV-000001 · Test Buyer").performClick()

        assertEquals("doc-1", clickedDocumentId)
    }
}
