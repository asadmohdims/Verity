package com.verity.feature.invoice.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.verity.core.theme.VerityBaseTypography
import com.verity.core.theme.VerityTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Regression coverage for the "dropdown" behavior on HSN Code / Unit within LineItemEntryScreen
 * (the full-screen line-item add/edit surface): tapping into the field alone (no typing) should
 * reveal the full curated list — see VerityTextField's expandSuggestionsOnFocus parameter and
 * CLAUDE.md's "never a popup/dropdown menu" rule this stays inline to satisfy. Moved here from the
 * old InvoiceWorkspaceLineItemAutocompleteTest when line-item add/edit moved off Workspace's
 * inline VerityEditBlock onto its own screen — see LineItemEntryScreen's header comment for why.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
class LineItemEntryScreenAutocompleteTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent() {
        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                LineItemEntryScreen(
                    isEditMode = false,
                    existingItem = null,
                    hsnCodeSuggestions = listOf("7208", "7209"),
                    unitSuggestions = listOf("PCS", "KG"),
                    onAdd = {},
                    onSave = {},
                    onDelete = {},
                    onDone = {}
                )
            }
        }
    }

    @Test
    fun `tapping HSN Code without typing reveals the curated list`() {
        setContent()

        composeTestRule.onNodeWithText("HSN Code").performScrollTo().performClick()

        composeTestRule.onNodeWithText("7208").assertIsDisplayed()
        composeTestRule.onNodeWithText("7209").assertIsDisplayed()
    }

    @Test
    fun `selecting an HSN suggestion fills the field and hides the panel`() {
        setContent()

        composeTestRule.onNodeWithText("HSN Code").performScrollTo().performClick()
        composeTestRule.onNodeWithText("7208").performClick()

        composeTestRule.onNodeWithText("7209").assertDoesNotExist()
    }

    @Test
    fun `tapping Unit without typing reveals the curated list`() {
        setContent()

        composeTestRule.onNodeWithText("Unit").performScrollTo().performClick()

        composeTestRule.onNodeWithText("PCS").assertIsDisplayed()
        composeTestRule.onNodeWithText("KG").assertIsDisplayed()
    }
}
