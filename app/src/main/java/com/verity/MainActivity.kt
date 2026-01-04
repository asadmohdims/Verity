package com.verity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.verity.core.ui.molecules.VerityTopBarAction
import com.verity.core.ui.icons.VerityIcons
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.verity.core.theme.VerityBaseTypography
import com.verity.core.theme.VerityTheme
import com.verity.core.ui.molecules.VerityTopAppBar
import com.verity.core.ui.molecules.VerityChromeMode
import com.verity.core.ui.molecules.VerityNavIcon
import com.verity.core.ui.primitives.*
import androidx.core.view.WindowInsetsControllerCompat

/**
 * MainActivity
 *
 * Minimal application entry point.
 *
 * Responsibilities:
 * - Act as the composition root
 * - Provide a simple landing screen
 * - Route into Invoice Workspace (temporarily)
 *
 * Non-responsibilities:
 * - No ViewModel wiring
 * - No navigation framework
 * - No persistence or business logic
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val isDarkTheme = false
            val view = androidx.compose.ui.platform.LocalView.current
            androidx.compose.runtime.SideEffect {
                val controller = WindowInsetsControllerCompat(window, view)
                controller.isAppearanceLightStatusBars = !isDarkTheme
                controller.isAppearanceLightNavigationBars = !isDarkTheme
            }
            VerityTheme(
                darkTheme = isDarkTheme,
                typography = VerityBaseTypography
            ) {
                var chromeMode by remember { mutableStateOf<VerityChromeMode>(VerityChromeMode.Brand) }

                // --- Stress-test state variables ---
                var testCase by remember { mutableStateOf(0) }

                val stressTitles = listOf(
                    "Invoice",
                    "Invoice for ACME",
                    "Invoice for ACME Manufacturing Pvt Ltd",
                    "Invoice for ACME Manufacturing Pvt Ltd – Bangalore Unit"
                )

                val stressSubtitles = listOf(
                    null,
                    "Draft",
                    "Draft · Not Final",
                    "Draft · Pending Tax Validation"
                )
                val stressActions: List<List<VerityTopBarAction>> = listOf(
                    emptyList<VerityTopBarAction>(),
                    listOf(
                        VerityTopBarAction.Icon(
                            icon = VerityIcons.Search,
                            contentDescription = "Search",
                            onClick = {}
                        )
                    ),
                    listOf(
                        VerityTopBarAction.Icon(
                            icon = VerityIcons.Search,
                            contentDescription = "Search",
                            onClick = {}
                        ),
                        VerityTopBarAction.Icon(
                            icon = VerityIcons.Overflow,
                            contentDescription = "More",
                            onClick = {}
                        )
                    ),
                    listOf(
                        VerityTopBarAction.Overflow(
                            items = emptyList()
                        )
                    )
                )
                // --- End stress-test state variables ---

                val goToInvoiceWorkspace = {
                    chromeMode = VerityChromeMode.Workspace
                }

                Scaffold(
                    topBar = {
                        VerityTopAppBar(
                            chromeMode = chromeMode,
                            title = when (chromeMode) {
                                VerityChromeMode.Brand -> "Verity"
                                else -> stressTitles[testCase % stressTitles.size]
                            },
                            subtitle = when (chromeMode) {
                                VerityChromeMode.Workspace ->
                                    stressSubtitles[testCase % stressSubtitles.size]
                                else -> null
                            },
                            navigationIcon = when (chromeMode) {
                                VerityChromeMode.Brand -> VerityNavIcon.None
                                else -> VerityNavIcon.Back(
                                    onClick = {
                                        // no-op for chrome testing
                                    },
                                    contentDescription = "Back"
                                )
                            },
                            actions = stressActions[testCase % stressActions.size],
                        )
                    }
                ) { innerPadding ->
                    VeritySurface(
                        type = VeritySurfaceType.Base,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {

                            VerityText(
                                text = "DEBUG · Chrome Mode Tester",
                                style = VerityTextStyle.Caption
                            )

                            // --- Stress-test controls ---
                            VerityText(
                                text = "DEBUG · Stress Test Case ${testCase + 1}",
                                style = VerityTextStyle.Caption
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                VerityButton(
                                    label = "Next Case",
                                    role = VerityButtonRole.Secondary,
                                    state = VerityButtonState.Enabled,
                                    onClick = { testCase++ }
                                )

                                VerityButton(
                                    label = "Prev Case",
                                    role = VerityButtonRole.Secondary,
                                    state = VerityButtonState.Enabled,
                                    onClick = { if (testCase > 0) testCase-- }
                                )
                            }
                            // --- End stress-test controls ---

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                VerityButton(
                                    label = "Brand",
                                    role = VerityButtonRole.Secondary,
                                    state = VerityButtonState.Enabled,
                                    onClick = { chromeMode = VerityChromeMode.Brand }
                                )

                                VerityButton(
                                    label = "Workspace",
                                    role = VerityButtonRole.Secondary,
                                    state = VerityButtonState.Enabled,
                                    onClick = { chromeMode = VerityChromeMode.Workspace }
                                )

                                VerityButton(
                                    label = "Support",
                                    role = VerityButtonRole.Secondary,
                                    state = VerityButtonState.Enabled,
                                    onClick = { chromeMode = VerityChromeMode.Support }
                                )
                            }

                            VeritySpacer(size = VeritySpace.Medium)

                            VerityButton(
                                label = "Create Invoice",
                                role = VerityButtonRole.Primary,
                                state = VerityButtonState.Enabled,
                                onClick = goToInvoiceWorkspace
                            )
                        }
                    }
                }
            }
        }
    }
}
