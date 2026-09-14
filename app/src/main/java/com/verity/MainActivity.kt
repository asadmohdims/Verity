package com.verity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.rememberNavController
import com.verity.core.theme.VerityBaseTypography
import com.verity.core.theme.VerityTheme
import com.verity.feature.home.HomeViewModel
import com.verity.feature.invoice.draft.InvoiceDraftStore
import com.verity.feature.invoice.draft.InvoiceDraftUiState
import com.verity.feature.invoice.ui.InvoiceWorkspaceViewModel
import com.verity.navigation.AppNavShell
import com.verity.platform.autocomplete.DefaultCustomerAutocompleteDataSource
import com.verity.platform.database.PlatformDatabaseFactory
import com.verity.platform.database.seed.CustomerSeedLoader
import com.verity.platform.database.seed.toEntity
import com.verity.platform.finalize.DefaultInvoiceFinalizer
import com.verity.platform.home.DefaultHomeDataSource
import com.verity.platform.pdf.DefaultInvoicePdfRenderer
import java.time.Clock

/**
 * MainActivity
 *
 * Composition root. Constructs the object graph (database, ViewModels) and hands off to
 * AppNavShell for the actual UI tree (chrome, navigation, screens) — no business logic, no
 * navigation logic lives here.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val isDarkTheme = false
            val navController = rememberNavController()
            val view = LocalView.current
            SideEffect {
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
                    ),
                    invoicePdfRenderer = DefaultInvoicePdfRenderer(context = context)
                )
            }

            val homeViewModel = remember {
                HomeViewModel(
                    homeDataSource = DefaultHomeDataSource(database = database),
                    clock = Clock.systemDefaultZone()
                )
            }

            VerityTheme(
                darkTheme = isDarkTheme,
                typography = VerityBaseTypography
            ) {
                AppNavShell(
                    navController = navController,
                    homeViewModel = homeViewModel,
                    invoiceWorkspaceViewModel = invoiceWorkspaceViewModel
                )
            }
        }
    }
}
