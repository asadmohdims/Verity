package com.verity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.verity.invoice.draft.InvoiceDraftUiState
import com.verity.platform.autocomplete.DefaultCustomerAutocompleteDataSource
import com.verity.platform.database.PlatformDatabaseFactory
import com.verity.platform.database.seed.CustomerSeedLoader
import com.verity.platform.database.seed.toEntity
import com.verity.platform.finalize.DefaultInvoiceFinalizer

import com.verity.feature.invoice.preview.InvoicePreviewScreen

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.verity.core.ui.molecules.VerityNavIcon
import com.verity.core.ui.molecules.VerityChromeMode
import com.verity.core.ui.chrome.WorkspaceChromeSpec
import androidx.navigation.compose.currentBackStackEntryAsState
import java.time.Clock

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
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            val view = androidx.compose.ui.platform.LocalView.current
            androidx.compose.runtime.SideEffect {
                val controller = WindowInsetsControllerCompat(window, view)
                controller.isAppearanceLightStatusBars = !isDarkTheme
                controller.isAppearanceLightNavigationBars = !isDarkTheme
            }

            val context = LocalContext.current
            val database = remember { PlatformDatabaseFactory.create(context) }

            // One-time seed bootstrap: populate customers from the fixture asset on first run.
            LaunchedEffect(Unit) {
                if (database.customerDao().count() == 0) {
                    val seedCustomers = CustomerSeedLoader.load(context).map { it.toEntity() }
                    database.customerDao().upsertAll(seedCustomers)
                }
            }

            // Construct InvoiceWorkspaceViewModel for the feature route.
            val invoiceWorkspaceViewModel = remember {
                InvoiceWorkspaceViewModel(
                    draftStore = InvoiceDraftStore(
                        initialDraft = InvoiceDraftUiState()
                    ),
                    customerAutocompleteDataSource = DefaultCustomerAutocompleteDataSource(
                        customerDao = database.customerDao()
                    ),
                    invoiceFinalizer = DefaultInvoiceFinalizer(
                        database = database,
                        clock = Clock.systemDefaultZone()
                    )
                )
            }
            VerityTheme(
                darkTheme = isDarkTheme,
                typography = VerityBaseTypography
            ) {

                val workspaceChromeSpec by invoiceWorkspaceViewModel
                    .chromeSpec
                    .collectAsState()

                val chromeSpecWithNavigation = remember(workspaceChromeSpec) {
                    workspaceChromeSpec.copy(
                        actions = workspaceChromeSpec.actions.map { action ->
                            if (
                                action is com.verity.core.ui.molecules.VerityTopBarAction.Icon &&
                                action.contentDescription == "Preview invoice"
                            ) {
                                action.copy(
                                    onClick = {
                                        if (invoiceWorkspaceViewModel.previewDocument.value != null) {
                                            navController.navigate("preview")
                                        }
                                    }
                                )
                            } else {
                                action
                            }
                        }
                    )
                }

                val effectiveChromeSpec = when (currentRoute) {
                    "preview" -> WorkspaceChromeSpec(
                        title = "Invoice Preview",
                        subtitle = null,
                        navigationIcon = VerityNavIcon.Back(
                            onClick = { navController.popBackStack() },
                            contentDescription = "Back"
                        ),
                        actions = emptyList(),
                        chromeMode = VerityChromeMode.Support
                    )
                    "finalized" -> WorkspaceChromeSpec(
                        title = "Invoice Finalized",
                        subtitle = null,
                        navigationIcon = VerityNavIcon.Back(
                            onClick = { navController.popBackStack() },
                            contentDescription = "Back"
                        ),
                        actions = emptyList(),
                        chromeMode = VerityChromeMode.Support
                    )
                    else -> chromeSpecWithNavigation
                }

                Scaffold(
                    topBar = {
                        VerityTopAppBar(
                            title = effectiveChromeSpec.title,
                            subtitle = effectiveChromeSpec.subtitle,
                            navigationIcon = effectiveChromeSpec.navigationIcon,
                            actions = effectiveChromeSpec.actions,
                            chromeMode = effectiveChromeSpec.chromeMode
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

                            composable("preview") {
                                val previewDocument by invoiceWorkspaceViewModel
                                    .previewDocument
                                    .collectAsState()
                                val finalizedDocument by invoiceWorkspaceViewModel
                                    .finalizedDocument
                                    .collectAsState()
                                val isFinalizing by invoiceWorkspaceViewModel
                                    .isFinalizing
                                    .collectAsState()

                                // Finalize completes asynchronously in the ViewModel; once it
                                // publishes a result, move forward to the finalized screen and
                                // drop "preview" from the back stack (the draft it showed no
                                // longer exists — back must not be able to return to it).
                                LaunchedEffect(finalizedDocument) {
                                    if (finalizedDocument != null) {
                                        navController.navigate("finalized") {
                                            popUpTo("preview") { inclusive = true }
                                        }
                                    }
                                }

                                requireNotNull(previewDocument) {
                                    "Preview route entered without an active draft"
                                }

                                InvoicePreviewScreen(
                                    document = previewDocument!!,
                                    onBack = { navController.popBackStack() },
                                    onFinalize = { invoiceWorkspaceViewModel.onFinalizeInvoice() },
                                    isFinalizing = isFinalizing
                                )
                            }

                            composable("finalized") {
                                val finalizedDocument by invoiceWorkspaceViewModel
                                    .finalizedDocument
                                    .collectAsState()

                                requireNotNull(finalizedDocument) {
                                    "Finalized route entered without a finalized document"
                                }

                                InvoicePreviewScreen(
                                    document = finalizedDocument!!,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
