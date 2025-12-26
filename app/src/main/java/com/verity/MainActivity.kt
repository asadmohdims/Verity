package com.verity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.LaunchedEffect
import java.util.UUID
import com.verity.platform.database.entities.CustomerEntity
import com.verity.core.ui.primitives.VerityTextField
import com.verity.core.ui.primitives.VerityTextFieldRole
import com.verity.core.ui.primitives.VeritySuggestion

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
        val database = remember { PlatformDatabaseFactory.create(context) }
        val customerDao = remember { database.customerDao() }

        LaunchedEffect(Unit) {
            // DEBUG‑ONLY seed data for autocomplete testing
            if (customerDao.count() == 0) {
                customerDao.insert(
                    CustomerEntity(
                        customerId = UUID.randomUUID().toString(),
                        customerName = "Bhargava Industries",
                        phone = null,
                        gstin = "27AAACB1234Z1Z",
                        city = "Pune",
                        state = "Maharashtra",
                        stateCode = "27",
                        isActive = true,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                customerDao.insert(
                    CustomerEntity(
                        customerId = UUID.randomUUID().toString(),
                        customerName = "Apex Engineering",
                        phone = null,
                        gstin = "29AABCA9999Q1Z",
                        city = "Bengaluru",
                        state = "Karnataka",
                        stateCode = "29",
                        isActive = true,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }

        val viewModel: InvoiceWorkspaceViewModel = viewModel(
            factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    // Reuse DB / DAO created in composable scope
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
            draft = viewModel.uiState.collectAsState().value,
            viewModel = viewModel
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

        // --- DEBUG · VerityTextField Sandbox ---
        VeritySpacer(size = VeritySpace.Large)

        VeritySection(title = "DEBUG · VerityTextField Sandbox") {

            var editing by remember { mutableStateOf(false) }
            var query by remember { mutableStateOf("") }

            val suggestions = listOf(
                VeritySuggestion(
                    id = "1",
                    primary = "Bhargava Industries",
                    secondary = "Pune · Maharashtra"
                ),
                VeritySuggestion(
                    id = "2",
                    primary = "Apex Engineering",
                    secondary = "Bengaluru · Karnataka"
                ),
                VeritySuggestion(
                    id = "3",
                    primary = "Sunrise Tools Pvt Ltd",
                    secondary = "Mumbai · Maharashtra"
                ),
                VeritySuggestion(
                    id = "4",
                    primary = "Nova Tech Solutions",
                    secondary = "Hyderabad · Telangana"
                ),
                VeritySuggestion(
                    id = "5",
                    primary = "Kaveri Hydraulics",
                    secondary = "Coimbatore · Tamil Nadu"
                ),
                VeritySuggestion(
                    id = "6",
                    primary = "Zenith Industrial Works",
                    secondary = "Ahmedabad · Gujarat"
                )
            ).filter {
                query.isNotBlank() && it.primary.contains(query, ignoreCase = true)
            }

            VerityTextField(
                role = VerityTextFieldRole.Basic,
                label = "Basic Text Field",
                placeholder = "Select customer",
                value = query,
                onValueChange = { query = it },
                editing = editing,
                onEnterEdit = { editing = true },
                onExitEdit = { editing = false },
                suggestions = suggestions,
                onSelectSuggestion = {
                    query = it.primary
                    editing = false
                },
                modifier = Modifier.fillMaxWidth()
            )

            VeritySpacer(size = VeritySpace.Medium)

            VerityTextField(
                role = VerityTextFieldRole.SelectionSearch,
                label = "Selection Search",
                placeholder = "Select customer",
                value = query,
                onValueChange = { query = it },
                editing = editing,
                onEnterEdit = { editing = true },
                onExitEdit = { editing = false },
                suggestions = suggestions,
                onSelectSuggestion = {
                    query = it.primary
                    editing = false
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        // --- END DEBUG SANDBOX ---
    }
}