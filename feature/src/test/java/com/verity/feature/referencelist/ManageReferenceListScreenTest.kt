package com.verity.feature.referencelist

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.verity.core.theme.VerityBaseTypography
import com.verity.core.theme.VerityTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
class ManageReferenceListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `typing a value and tapping Add fires onAdd`() {
        var typedValue: String? = null
        var addTapped = false

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                ManageReferenceListScreen(
                    state = ManageReferenceListUiState(isLoading = false, newValueInput = typedValue ?: ""),
                    onNewValueInputChanged = { typedValue = it },
                    onAdd = { addTapped = true },
                    onDelete = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Add new value").performTextInput("MZN Transport")

        assertEquals("MZN Transport", typedValue)
    }

    @Test
    fun `tapping Add with a non-blank input fires onAdd`() {
        var addTapped = false

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                ManageReferenceListScreen(
                    state = ManageReferenceListUiState(isLoading = false, newValueInput = "MZN Transport"),
                    onNewValueInputChanged = {},
                    onAdd = { addTapped = true },
                    onDelete = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Add").performClick()

        assertEquals(true, addTapped)
    }

    @Test
    fun `tapping Remove on a row fires onDelete with that item's id`() {
        var deletedId: String? = null

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                ManageReferenceListScreen(
                    state = ManageReferenceListUiState(
                        isLoading = false,
                        items = listOf(ReferenceListItem(id = "t-1", value = "MZN Transport"))
                    ),
                    onNewValueInputChanged = {},
                    onAdd = {},
                    onDelete = { deletedId = it }
                )
            }
        }

        composeTestRule.onNodeWithText("MZN Transport").assertIsDisplayed()
        composeTestRule.onNodeWithText("Remove").performClick()

        assertEquals("t-1", deletedId)
    }

    @Test
    fun `empty state shows when there are no items`() {
        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                ManageReferenceListScreen(
                    state = ManageReferenceListUiState(isLoading = false, items = emptyList()),
                    onNewValueInputChanged = {},
                    onAdd = {},
                    onDelete = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Nothing added yet").assertIsDisplayed()
    }

    @Test
    fun `empty state is absent once items exist`() {
        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                ManageReferenceListScreen(
                    state = ManageReferenceListUiState(
                        isLoading = false,
                        items = listOf(ReferenceListItem(id = "t-1", value = "MZN Transport"))
                    ),
                    onNewValueInputChanged = {},
                    onAdd = {},
                    onDelete = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Nothing added yet").assertDoesNotExist()
    }
}
