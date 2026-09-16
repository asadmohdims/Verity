package com.verity.feature.customer.edit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.verity.core.theme.VerityBaseTypography
import com.verity.core.theme.VerityTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
class AddEditCustomerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `Save is disabled until required fields are filled`() {
        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                AddEditCustomerScreen(
                    state = AddEditCustomerUiState(),
                    onCustomerNameChange = {},
                    onGstinChange = {},
                    onPhoneChange = {},
                    onAddressLine1Change = {},
                    onCityChange = {},
                    onStateChange = {},
                    onStateCodeChange = {},
                    onPincodeChange = {},
                    onNotesChange = {},
                    onSave = {},
                    onDeactivate = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun `Save is enabled once all required fields are filled`() {
        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                AddEditCustomerScreen(
                    state = AddEditCustomerUiState(
                        customerName = "Ramesh Textiles",
                        gstin = "24AAACR5678Q1Z2",
                        addressLine1 = "Ring Road Industrial Estate",
                        city = "Surat",
                        state = "Gujarat",
                        stateCode = "24"
                    ),
                    onCustomerNameChange = {},
                    onGstinChange = {},
                    onPhoneChange = {},
                    onAddressLine1Change = {},
                    onCityChange = {},
                    onStateChange = {},
                    onStateCodeChange = {},
                    onPincodeChange = {},
                    onNotesChange = {},
                    onSave = {},
                    onDeactivate = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Save").assertIsEnabled()
    }

    @Test
    fun `tapping Save fires onSave`() {
        var saved = false

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                AddEditCustomerScreen(
                    state = AddEditCustomerUiState(
                        customerName = "Ramesh Textiles",
                        gstin = "24AAACR5678Q1Z2",
                        addressLine1 = "Ring Road Industrial Estate",
                        city = "Surat",
                        state = "Gujarat",
                        stateCode = "24"
                    ),
                    onCustomerNameChange = {},
                    onGstinChange = {},
                    onPhoneChange = {},
                    onAddressLine1Change = {},
                    onCityChange = {},
                    onStateChange = {},
                    onStateCodeChange = {},
                    onPincodeChange = {},
                    onNotesChange = {},
                    onSave = { saved = true },
                    onDeactivate = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Save").performClick()

        assertEquals(true, saved)
    }

    @Test
    fun `Deactivate row only appears in Edit mode`() {
        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                AddEditCustomerScreen(
                    state = AddEditCustomerUiState(isEditMode = false),
                    onCustomerNameChange = {},
                    onGstinChange = {},
                    onPhoneChange = {},
                    onAddressLine1Change = {},
                    onCityChange = {},
                    onStateChange = {},
                    onStateCodeChange = {},
                    onPincodeChange = {},
                    onNotesChange = {},
                    onSave = {},
                    onDeactivate = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Deactivate customer").assertDoesNotExist()
    }

    @Test
    fun `checking Deactivate in Edit mode fires onDeactivate`() {
        var deactivated = false

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                AddEditCustomerScreen(
                    state = AddEditCustomerUiState(isEditMode = true),
                    onCustomerNameChange = {},
                    onGstinChange = {},
                    onPhoneChange = {},
                    onAddressLine1Change = {},
                    onCityChange = {},
                    onStateChange = {},
                    onStateCodeChange = {},
                    onPincodeChange = {},
                    onNotesChange = {},
                    onSave = {},
                    onDeactivate = { deactivated = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Deactivate customer").assertIsDisplayed()
        composeTestRule.onNodeWithTag("deactivate-customer-checkbox").performClick()

        assertEquals(true, deactivated)
    }
}
