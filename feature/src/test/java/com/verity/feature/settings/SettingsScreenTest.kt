package com.verity.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.verity.core.theme.ThemeMode
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
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `tapping a theme segment fires onThemeModeSelected with that mode`() {
        var selected: ThemeMode? = null

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                SettingsScreen(
                    state = SettingsUiState(themeMode = ThemeMode.SYSTEM, appVersionLabel = "1.0"),
                    onThemeModeSelected = { selected = it },
                    onManageTransporterNames = {},
                    onManageHsnCodes = {},
                    onManageUnits = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Dark").performClick()

        assertEquals(ThemeMode.DARK, selected)
    }

    @Test
    fun `sync status row shows the label when present`() {
        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                SettingsScreen(
                    state = SettingsUiState(syncStatusLabel = "3 pending", appVersionLabel = "1.0"),
                    onThemeModeSelected = {},
                    onManageTransporterNames = {},
                    onManageHsnCodes = {},
                    onManageUnits = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Sync status").assertIsDisplayed()
        composeTestRule.onNodeWithText("3 pending").assertIsDisplayed()
    }

    @Test
    fun `sync status row is absent when there is no label yet`() {
        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                SettingsScreen(
                    state = SettingsUiState(syncStatusLabel = null, appVersionLabel = "1.0"),
                    onThemeModeSelected = {},
                    onManageTransporterNames = {},
                    onManageHsnCodes = {},
                    onManageUnits = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Sync status").assertDoesNotExist()
    }

    @Test
    fun `version row shows the app version label`() {
        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                SettingsScreen(
                    state = SettingsUiState(appVersionLabel = "1.0 (Milestone 1)"),
                    onThemeModeSelected = {},
                    onManageTransporterNames = {},
                    onManageHsnCodes = {},
                    onManageUnits = {}
                )
            }
        }

        composeTestRule.onNodeWithText("1.0 (Milestone 1)").assertIsDisplayed()
    }

    @Test
    fun `Business Profile row is visible but shows the real seller name`() {
        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                SettingsScreen(
                    state = SettingsUiState(appVersionLabel = "1.0"),
                    onThemeModeSelected = {},
                    onManageTransporterNames = {},
                    onManageHsnCodes = {},
                    onManageUnits = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Business Profile").assertIsDisplayed()
        composeTestRule.onNodeWithText("UNITECH MACHINERIES").assertIsDisplayed()
    }

    @Test
    fun `tapping Transporter Names fires onManageTransporterNames`() {
        var tapped = false

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                SettingsScreen(
                    state = SettingsUiState(appVersionLabel = "1.0"),
                    onThemeModeSelected = {},
                    onManageTransporterNames = { tapped = true },
                    onManageHsnCodes = {},
                    onManageUnits = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Transporter Names").performClick()

        assertEquals(true, tapped)
    }

    @Test
    fun `tapping HSN Codes fires onManageHsnCodes`() {
        var tapped = false

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                SettingsScreen(
                    state = SettingsUiState(appVersionLabel = "1.0"),
                    onThemeModeSelected = {},
                    onManageTransporterNames = {},
                    onManageHsnCodes = { tapped = true },
                    onManageUnits = {}
                )
            }
        }

        composeTestRule.onNodeWithText("HSN Codes").performClick()

        assertEquals(true, tapped)
    }

    @Test
    fun `tapping Units fires onManageUnits`() {
        var tapped = false

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                SettingsScreen(
                    state = SettingsUiState(appVersionLabel = "1.0"),
                    onThemeModeSelected = {},
                    onManageTransporterNames = {},
                    onManageHsnCodes = {},
                    onManageUnits = { tapped = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Units").performClick()

        assertEquals(true, tapped)
    }
}
