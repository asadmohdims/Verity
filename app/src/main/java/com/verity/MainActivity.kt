package com.verity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.verity.core.theme.VerityBaseTypography
import com.verity.core.theme.VerityTheme
import com.verity.core.ui.molecules.VerityTopAppBar
import com.verity.core.ui.primitives.*
import androidx.core.view.WindowInsetsControllerCompat
import com.verity.platform.chrome.WorkspaceChromeViewModel
import com.verity.feature.invoice.ui.InvoiceWorkspaceRoute
import com.verity.feature.invoice.ui.InvoiceWorkspaceViewModel
import com.verity.invoice.draft.InvoiceDraftStore
import com.verity.feature.invoice.autocomplete.CustomerAutocompleteDataSource
import com.verity.feature.invoice.autocomplete.CustomerAutocompleteItem
import com.verity.invoice.draft.InvoiceDraftUiState

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

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
 * - No feature-level orchestration
 * - No business or domain logic
 * - No UI chrome semantics
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val isDarkTheme = false
            val navController = rememberNavController()
            val view = androidx.compose.ui.platform.LocalView.current
            androidx.compose.runtime.SideEffect {
                val controller = WindowInsetsControllerCompat(window, view)
                controller.isAppearanceLightStatusBars = !isDarkTheme
                controller.isAppearanceLightNavigationBars = !isDarkTheme
            }
            // Construct InvoiceWorkspaceViewModel for the feature route.
            val invoiceWorkspaceViewModel = remember {
                InvoiceWorkspaceViewModel(
                    draftStore = InvoiceDraftStore(
                        initialDraft = InvoiceDraftUiState()
                    ),
                    customerAutocompleteDataSource = object : CustomerAutocompleteDataSource {
                        override suspend fun recentCustomers(limit: Int): List<CustomerAutocompleteItem> =
                            emptyList()

                        override suspend fun searchCustomers(
                            query: String,
                            limit: Int
                        ): List<CustomerAutocompleteItem> =
                            emptyList()
                    }
                )
            }
            VerityTheme(
                darkTheme = isDarkTheme,
                typography = VerityBaseTypography
            ) {

                val workspaceChromeSpec by invoiceWorkspaceViewModel
                    .chromeSpec
                    .collectAsState()

                Scaffold(
                    topBar = {
                        VerityTopAppBar(
                            title = workspaceChromeSpec.title,
                            subtitle = workspaceChromeSpec.subtitle,
                            navigationIcon = workspaceChromeSpec.navigationIcon,
                            actions = workspaceChromeSpec.actions,
                            chromeMode = workspaceChromeSpec.chromeMode
                        )
                    }
                ) { innerPadding ->
                    VeritySurface(
                        type = VeritySurfaceType.Base,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = "workspace"
                        ) {
                            composable("workspace") {
                                InvoiceWorkspaceRoute(
                                    viewModel = invoiceWorkspaceViewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
