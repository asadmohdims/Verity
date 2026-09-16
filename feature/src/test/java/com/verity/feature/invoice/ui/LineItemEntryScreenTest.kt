package com.verity.feature.invoice.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.verity.core.theme.VerityBaseTypography
import com.verity.core.theme.VerityTheme
import com.verity.feature.invoice.draft.DraftLineItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * LineItemEntryScreen is the full-screen surface that replaced the old inline VerityEditBlock for
 * adding/editing a line item (see the screen's own header comment for the keyboard-hiding-fields
 * bug that motivated the move). It's a pure composable — state hoisted entirely through
 * onAdd/onSave/onDelete/onDone — so these tests drive it directly with plain lambdas, no
 * InvoiceWorkspaceViewModel/InvoiceDraftStore needed.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
class LineItemEntryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        isEditMode: Boolean,
        existingItem: DraftLineItem? = null,
        onAdd: (DraftLineItem) -> Unit = { error("onAdd not expected in this test") },
        onSave: (DraftLineItem) -> Unit = { error("onSave not expected in this test") },
        onDelete: () -> Unit = { error("onDelete not expected in this test") },
        onDone: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                LineItemEntryScreen(
                    isEditMode = isEditMode,
                    existingItem = existingItem,
                    hsnCodeSuggestions = emptyList(),
                    unitSuggestions = emptyList(),
                    onAdd = onAdd,
                    onSave = onSave,
                    onDelete = onDelete,
                    onDone = onDone
                )
            }
        }
    }

    @Test
    fun `Save and Add Another commits the item, clears the form, and stays on screen`() {
        val addedItems = mutableListOf<DraftLineItem>()
        var doneCalled = false

        setContent(
            isEditMode = false,
            onAdd = { addedItems.add(it) },
            onDone = { doneCalled = true }
        )

        composeTestRule.onNodeWithText("Description").performTextInput("Test Item")
        composeTestRule.onNodeWithText("Quantity (optional)").performTextInput("10")
        composeTestRule.onNodeWithText("Rate").performTextInput("100")
        composeTestRule.onNodeWithText("Save & Add Another").performScrollTo().performClick()

        assertEquals(1, addedItems.size)
        assertEquals("Test Item", addedItems.single().description)
        assertEquals(10L, addedItems.single().quantity)
        assertFalse("Save & Add Another must not leave the screen", doneCalled)

        // Fields reset so the next item can be typed straight away.
        composeTestRule.onNodeWithText("Test Item").assertDoesNotExist()
    }

    @Test
    fun `Save and Close commits the item and leaves the screen`() {
        val addedItems = mutableListOf<DraftLineItem>()
        var doneCalled = false

        setContent(
            isEditMode = false,
            onAdd = { addedItems.add(it) },
            onDone = { doneCalled = true }
        )

        composeTestRule.onNodeWithText("Description").performTextInput("Test Item")
        composeTestRule.onNodeWithText("Rate").performTextInput("100")
        composeTestRule.onNodeWithText("Save & Close").performScrollTo().performClick()

        assertEquals(1, addedItems.size)
        assertTrue(doneCalled)
    }

    @Test
    fun `Edit mode prefills existing values and Save commits the update`() {
        val existing = DraftLineItem(
            description = "Old Description",
            hsnCode = "1234",
            quantity = 5,
            unit = "pcs",
            ratePaise = 5000
        )
        var savedItem: DraftLineItem? = null
        var doneCalled = false

        setContent(
            isEditMode = true,
            existingItem = existing,
            onSave = { savedItem = it },
            onDone = { doneCalled = true }
        )

        composeTestRule.onNodeWithText("Old Description").assertIsDisplayed()

        composeTestRule.onNodeWithText("Save").performScrollTo().performClick()

        assertEquals("Old Description", savedItem?.description)
        assertEquals(5L, savedItem?.quantity)
        assertTrue(doneCalled)
    }

    @Test
    fun `Delete in Edit mode invokes onDelete`() {
        val existing = DraftLineItem(
            description = "Old Description",
            hsnCode = "1234",
            quantity = 5,
            unit = "pcs",
            ratePaise = 5000
        )
        var deleteCalled = false

        setContent(
            isEditMode = true,
            existingItem = existing,
            onDelete = { deleteCalled = true }
        )

        composeTestRule.onNodeWithText("Delete").performScrollTo().performClick()

        assertTrue(deleteCalled)
    }
}
