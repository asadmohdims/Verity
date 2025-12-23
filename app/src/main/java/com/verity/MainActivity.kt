package com.verity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import com.verity.core.theme.VerityBaseTypography
import com.verity.core.theme.VerityTheme
import com.verity.core.ui.molecules.VeritySection
import com.verity.core.ui.primitives.VeritySpace
import com.verity.core.ui.primitives.VeritySpacer
import com.verity.core.ui.primitives.VeritySurface
import com.verity.core.ui.primitives.VeritySurfaceType
import com.verity.core.ui.primitives.VerityText
import com.verity.core.ui.primitives.VerityTextStyle
import com.verity.feature.invoice.ui.InvoiceWorkspaceScreen
import com.verity.feature.invoice.ui.InvoiceWorkspaceViewModel
import com.verity.invoice.draft.InvoiceDraftStore
import com.verity.invoice.draft.InvoiceDraftUiState
import com.verity.platform.autocomplete.DefaultCustomerAutocompleteDataSource
import com.verity.platform.database.PlatformDatabaseFactory

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
            VerityTheme(
                darkTheme = false,
                typography = VerityBaseTypography
            ) {
                VeritySurface(
                    type = VeritySurfaceType.Base,
                    modifier = Modifier.fillMaxSize()
                ) {
                    MainEntry()
                }
            }
        }
    }
}

@Composable
private fun MainEntry() {
    var showInvoiceWorkspace by remember { mutableStateOf(false) }

    if (showInvoiceWorkspace) {
        val context = androidx.compose.ui.platform.LocalContext.current

        val viewModel: InvoiceWorkspaceViewModel = viewModel(
            factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val database = PlatformDatabaseFactory.create(context)
                    val customerDao = database.customerDao()
                    val autocompleteDataSource =
                        DefaultCustomerAutocompleteDataSource(customerDao)

                    @Suppress("UNCHECKED_CAST")
                    return InvoiceWorkspaceViewModel(
                        draftStore = InvoiceDraftStore(),
                        customerAutocompleteDataSource = autocompleteDataSource
                    ) as T
                }
            }
        )

        InvoiceWorkspaceScreen(
            draft = viewModel.uiState.collectAsState().value
        )
    } else {
        LandingScreen(
            onGoToInvoice = { showInvoiceWorkspace = true }
        )
    }
}

@Composable
private fun LandingScreen(
    onGoToInvoice: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        VeritySection(title = "Verity") {

            VerityText(
                text = "Create Invoice",
                style = VerityTextStyle.Title,
                modifier = Modifier.clickable { onGoToInvoice() }
            )

            VeritySpacer(size = VeritySpace.Medium)

            VerityText(
                text = "Search (coming soon)",
                style = VerityTextStyle.Caption
            )

            VerityText(
                text = "Customers (coming soon)",
                style = VerityTextStyle.Caption
            )
        }
    }
}