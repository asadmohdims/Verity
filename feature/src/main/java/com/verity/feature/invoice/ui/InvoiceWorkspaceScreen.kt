package com.verity.feature.invoice.ui

import androidx.compose.ui.tooling.preview.Preview
import com.verity.core.theme.VerityTheme
import com.verity.core.theme.VerityBaseTypography
import com.verity.invoice.draft.DraftCustomer
import com.verity.invoice.draft.DraftLineItem
import com.verity.invoice.draft.DraftAdditionalDetails
import com.verity.invoice.draft.DraftSummary

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.verity.core.ui.molecules.VerityHeader
import com.verity.core.ui.molecules.VeritySection
import com.verity.core.ui.primitives.VeritySpacer
import com.verity.core.ui.primitives.VeritySpace
import com.verity.core.ui.primitives.VerityText
import com.verity.core.ui.primitives.VerityTextStyle
import com.verity.invoice.draft.InvoiceDraftUiState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.verity.core.ui.primitives.VeritySurface
import com.verity.core.ui.primitives.VeritySurfaceType
import com.verity.core.ui.primitives.dp
import com.verity.core.ui.molecules.VerityListItem
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.TextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import com.verity.feature.invoice.autocomplete.CustomerAutocompleteItem

import com.verity.invoice.draft.InvoiceDraftStore
import com.verity.feature.invoice.autocomplete.CustomerAutocompleteDataSource

/**
 * InvoiceWorkspaceScreen
 *
 * PURPOSE
 * -------
 * Primary working surface for invoice / challan creation.
 *
 * This screen represents the user's daily workspace and is the
 * default landing screen of the application.
 *
 * DELIVERY PHASE
 * --------------
 * D1A — UI-only, read-only skeleton.
 *
 * This implementation intentionally:
 * • Renders draft state only
 * • Does NOT allow editing
 * • Does NOT emit commands
 * • Does NOT invoke replay or persistence
 *
 * NON-GOALS (EXPLICIT)
 * -------------------
 * • No validation
 * • No inputs
 * • No buttons with behavior
 * • No preview / finalize flows
 *
 * This screen exists purely to validate:
 * • Structural hierarchy
 * • Visual rhythm
 * • Section composition
 */
@Composable
fun InvoiceWorkspaceScreen(
    draft: InvoiceDraftUiState,
    viewModel: InvoiceWorkspaceViewModel
) {
    val billedToQuery by viewModel.billedToQuery.collectAsState()
    val billedToSuggestions by viewModel.billedToSuggestions.collectAsState()
    val isBilledToSearching by viewModel.isBilledToSearching.collectAsState()

    val shippedToQuery by viewModel.shippedToQuery.collectAsState()
    val shippedToSuggestions by viewModel.shippedToSuggestions.collectAsState()
    val isShippedToSearching by viewModel.isShippedToSearching.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {

        // ─────────────────────────────────────────────
        // Screen Header
        // ─────────────────────────────────────────────

        VerityHeader(
            title = "Invoice Workspace",
            subtitle = "Invoice",
            trailing = {
                VerityText(
                    text = "Search",
                    style = VerityTextStyle.Label
                )
            }
        )

        VeritySpacer(size = VeritySpace.Large)

        VerityText(
            text = "Document Type: Invoice",
            style = VerityTextStyle.Caption,
            modifier = Modifier.padding(horizontal = VeritySpace.Small.dp)
        )

        VeritySpacer(size = VeritySpace.Small)

        // ─────────────────────────────────────────────
        // Parties Section (formerly Customer Details)
        // ─────────────────────────────────────────────

        VeritySurface(
            type = VeritySurfaceType.Base,
            modifier = Modifier.padding(horizontal = VeritySpace.Small.dp)
        ) {
            VeritySection {

                androidx.compose.foundation.layout.Row {

                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        VerityText(
                            text = "Billed To",
                            style = VerityTextStyle.Label
                        )

                        VeritySpacer(size = VeritySpace.ExtraSmall)

                        if (draft.billedTo == null) {

                            TextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = billedToQuery,
                                onValueChange = { viewModel.onBilledToQueryChanged(it) },
                                placeholder = { VerityText(text = "Search customer", style = VerityTextStyle.Body) },
                                singleLine = true
                            )

                            DropdownMenu(
                                expanded = billedToSuggestions.isNotEmpty(),
                                onDismissRequest = { /* suggestions hide automatically on clear */ },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                billedToSuggestions.forEach { item ->
                                    DropdownMenuItem(
                                        text = {
                                            VerityText(
                                                text = item.customerName,
                                                style = VerityTextStyle.Body
                                            )
                                        },
                                        onClick = {
                                            viewModel.onBilledToSelected(item)
                                        }
                                    )
                                }
                            }

                        } else {

                            VerityText(
                                text = draft.billedTo.name,
                                style = VerityTextStyle.Body
                            )

                            VeritySpacer(size = VeritySpace.ExtraSmall)

                            VerityText(
                                text = "Change",
                                style = VerityTextStyle.Caption,
                                modifier = Modifier
                                    .padding(top = VeritySpace.ExtraSmall.dp)
                                    .then(Modifier)
                                    .clickable { viewModel.onBilledToCleared() }
                            )
                        }
                    }

                    VeritySpacer(size = VeritySpace.Large, horizontal = true)

                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        VerityText(
                            text = "Shipped To",
                            style = VerityTextStyle.Label
                        )

                        VeritySpacer(size = VeritySpace.ExtraSmall)

                        if (draft.shippedTo != null) {

                            VerityText(
                                text = draft.shippedTo.name,
                                style = VerityTextStyle.Body
                            )

                            VeritySpacer(size = VeritySpace.ExtraSmall)

                            VerityText(
                                text = "Change",
                                style = VerityTextStyle.Caption,
                                modifier = Modifier
                                    .padding(top = VeritySpace.ExtraSmall.dp)
                                    .clickable { viewModel.onShippedToCleared() }
                            )

                        } else {

                            TextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = shippedToQuery,
                                onValueChange = { viewModel.onShippedToQueryChanged(it) },
                                placeholder = {
                                    VerityText(
                                        text = draft.billedTo?.name ?: "Search customer",
                                        style = VerityTextStyle.Body
                                    )
                                },
                                singleLine = true
                            )

                            DropdownMenu(
                                expanded = shippedToSuggestions.isNotEmpty(),
                                onDismissRequest = { /* auto-hide */ },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                shippedToSuggestions.forEach { item ->
                                    DropdownMenuItem(
                                        text = {
                                            VerityText(
                                                text = item.customerName,
                                                style = VerityTextStyle.Body
                                            )
                                        },
                                        onClick = {
                                            viewModel.onShippedToSelected(item)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        VeritySpacer(size = VeritySpace.Medium)

        // ─────────────────────────────────────────────
        // Line Items Section
        // ─────────────────────────────────────────────

        VeritySection(title = "Line Items") {
            var isAddingLineItem by remember { mutableStateOf(false) }
            var editingIndex by remember { mutableStateOf<Int?>(null) }

            if (draft.lineItems.isEmpty() && !isAddingLineItem) {
                VerityText(
                    text = "No line items added",
                    style = VerityTextStyle.Caption
                )

                VeritySpacer(size = VeritySpace.Small)

                VerityText(
                    text = "+ Add line item",
                    style = VerityTextStyle.Label,
                    modifier = Modifier.clickable { isAddingLineItem = true }
                )
            } else {
                draft.lineItems.forEachIndexed { index, item ->
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                editingIndex = index
                                isAddingLineItem = false
                            }
                    ) {
                        VerityListItem(
                            title = item.description,
                            subtitle =
                                "HSN ${item.hsnCode} · ${item.quantity} ${item.unit} × ${item.rate}  |  Amount: ${item.amount}"
                        )
                    }

                    VeritySpacer(size = VeritySpace.Small)

                    if (editingIndex == index) {
                        VeritySpacer(size = VeritySpace.Small)

                        var description by remember { mutableStateOf(item.description) }
                        var hsn by remember { mutableStateOf(item.hsnCode) }
                        var quantity by remember { mutableStateOf(item.quantity.toString()) }
                        var unit by remember { mutableStateOf(item.unit) }
                        var rate by remember { mutableStateOf(item.rate.toString()) }

                        val qtyValue = quantity.toDoubleOrNull() ?: 0.0
                        val rateValue = rate.toDoubleOrNull() ?: 0.0
                        val amount = qtyValue * rateValue

                        VeritySurface(type = VeritySurfaceType.Sunken) {
                            Column(modifier = Modifier.padding(VeritySpace.Small.dp)) {

                                TextField(
                                    modifier = Modifier.fillMaxWidth(),
                                    value = description,
                                    onValueChange = { description = it },
                                    singleLine = true
                                )

                                VeritySpacer(size = VeritySpace.ExtraSmall)

                                TextField(
                                    modifier = Modifier.fillMaxWidth(),
                                    value = hsn,
                                    onValueChange = { hsn = it },
                                    singleLine = true
                                )

                                VeritySpacer(size = VeritySpace.ExtraSmall)

                                TextField(
                                    modifier = Modifier.fillMaxWidth(),
                                    value = quantity,
                                    onValueChange = { quantity = it },
                                    singleLine = true
                                )

                                VeritySpacer(size = VeritySpace.ExtraSmall)

                                TextField(
                                    modifier = Modifier.fillMaxWidth(),
                                    value = unit,
                                    onValueChange = { unit = it },
                                    singleLine = true
                                )

                                VeritySpacer(size = VeritySpace.ExtraSmall)

                                TextField(
                                    modifier = Modifier.fillMaxWidth(),
                                    value = rate,
                                    onValueChange = { rate = it },
                                    singleLine = true
                                )

                                VeritySpacer(size = VeritySpace.Small)

                                VerityText(
                                    text = "Amount: $amount",
                                    style = VerityTextStyle.Body
                                )

                                VeritySpacer(size = VeritySpace.Small)

                                androidx.compose.foundation.layout.Row {
                                    VerityText(
                                        text = "Save",
                                        style = VerityTextStyle.Label,
                                        modifier = Modifier.clickable {
                                            viewModel.onUpdateLineItem(
                                                index,
                                                DraftLineItem(
                                                    description = description,
                                                    hsnCode = hsn,
                                                    quantity = qtyValue,
                                                    unit = unit,
                                                    rate = rateValue,
                                                    amount = amount
                                                )
                                            )
                                            editingIndex = null
                                        }
                                    )

                                    VeritySpacer(size = VeritySpace.Large, horizontal = true)

                                    VerityText(
                                        text = "Delete",
                                        style = VerityTextStyle.Label,
                                        modifier = Modifier.clickable {
                                            viewModel.onRemoveLineItem(index)
                                            editingIndex = null
                                        }
                                    )

                                    VeritySpacer(size = VeritySpace.Large, horizontal = true)

                                    VerityText(
                                        text = "Cancel",
                                        style = VerityTextStyle.Caption,
                                        modifier = Modifier.clickable {
                                            editingIndex = null
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (!isAddingLineItem && editingIndex == null) {
                    VerityText(
                        text = "+ Add another line item",
                        style = VerityTextStyle.Label,
                        modifier = Modifier.clickable { isAddingLineItem = true }
                    )
                }
            }

            if (isAddingLineItem && editingIndex == null) {
                VeritySpacer(size = VeritySpace.Small)

                var description by remember { mutableStateOf("") }
                var hsn by remember { mutableStateOf("") }
                var quantity by remember { mutableStateOf("") }
                var unit by remember { mutableStateOf("") }
                var rate by remember { mutableStateOf("") }

                val qtyValue = quantity.toDoubleOrNull() ?: 0.0
                val rateValue = rate.toDoubleOrNull() ?: 0.0
                val amount = qtyValue * rateValue

                VeritySurface(type = VeritySurfaceType.Sunken) {
                    Column(
                        modifier = Modifier.padding(VeritySpace.Small.dp)
                    ) {

                        TextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = description,
                            onValueChange = { description = it },
                            placeholder = { VerityText("Description", VerityTextStyle.Body) },
                            singleLine = true
                        )

                        VeritySpacer(size = VeritySpace.ExtraSmall)

                        TextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = hsn,
                            onValueChange = { hsn = it },
                            placeholder = { VerityText("HSN Code", VerityTextStyle.Body) },
                            singleLine = true
                        )

                        VeritySpacer(size = VeritySpace.ExtraSmall)

                        TextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = quantity,
                            onValueChange = { quantity = it },
                            placeholder = { VerityText("Quantity", VerityTextStyle.Body) },
                            singleLine = true
                        )

                        VeritySpacer(size = VeritySpace.ExtraSmall)

                        TextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = unit,
                            onValueChange = { unit = it },
                            placeholder = { VerityText("Unit", VerityTextStyle.Body) },
                            singleLine = true
                        )

                        VeritySpacer(size = VeritySpace.ExtraSmall)

                        TextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = rate,
                            onValueChange = { rate = it },
                            placeholder = { VerityText("Rate", VerityTextStyle.Body) },
                            singleLine = true
                        )

                        VeritySpacer(size = VeritySpace.Small)

                        VerityText(
                            text = "Amount: $amount",
                            style = VerityTextStyle.Body
                        )

                        VeritySpacer(size = VeritySpace.Small)

                        androidx.compose.foundation.layout.Row {
                            VerityText(
                                text = "Add",
                                style = VerityTextStyle.Label,
                                modifier = Modifier
                                    .clickable {
                                        viewModel.onAddLineItem(
                                            DraftLineItem(
                                                description = description,
                                                hsnCode = hsn,
                                                quantity = qtyValue,
                                                unit = unit,
                                                rate = rateValue,
                                                amount = amount
                                            )
                                        )
                                        isAddingLineItem = false
                                    }
                            )

                            VeritySpacer(size = VeritySpace.Large, horizontal = true)

                            VerityText(
                                text = "Cancel",
                                style = VerityTextStyle.Caption,
                                modifier = Modifier
                                    .clickable {
                                        isAddingLineItem = false
                                    }
                            )
                        }
                    }
                }
            }
        }

        VeritySpacer(size = VeritySpace.Large)

        // ─────────────────────────────────────────────
        // Logistics Section (formerly Additional Details)
        // ─────────────────────────────────────────────

        VeritySection(title = "Logistics") {

            VerityText(
                text = "Transporter: —",
                style = VerityTextStyle.Caption
            )

            VerityText(
                text = "Vehicle No: —",
                style = VerityTextStyle.Caption
            )

            VerityText(
                text = "Supply Date: —",
                style = VerityTextStyle.Caption
            )

            VeritySpacer(size = VeritySpace.Small)

            VerityText(
                text = "Linked Challan: —",
                style = VerityTextStyle.Caption
            )
        }

        VeritySpacer(size = VeritySpace.Large)

        // ─────────────────────────────────────────────
        // Summary Section
        // ─────────────────────────────────────────────

        VeritySurface(
            type = VeritySurfaceType.Sunken,
            modifier = Modifier.padding(horizontal = VeritySpace.Small.dp)
        ) {
            VeritySection(title = "Summary") {
                VerityText(
                    text = "Subtotal: ${draft.summary.subtotal}",
                    style = VerityTextStyle.Body
                )

                VeritySpacer(size = VeritySpace.ExtraSmall)

                VerityText(
                    text = "Freight: ${draft.additionalDetails.freightAmount ?: 0}",
                    style = VerityTextStyle.Body
                )

                VeritySpacer(size = VeritySpace.ExtraSmall)

                VerityText(
                    text = "Tax: ${draft.summary.taxTotal}",
                    style = VerityTextStyle.Body
                )

                VeritySpacer(size = VeritySpace.Small)

                VerityText(
                    text = "Grand Total: ${draft.summary.grandTotal}",
                    style = VerityTextStyle.Title
                )
            }
        }
    }
}

@Preview(
    name = "Invoice Workspace — Light",
    showBackground = true
)
@Composable
private fun InvoiceWorkspacePreviewLight() {
    VerityTheme(
        darkTheme = false,
        typography = VerityBaseTypography
    ) {
        VeritySurface(
            type = VeritySurfaceType.Base,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .padding(VeritySpace.Medium.dp)
            ) {
                InvoiceWorkspaceScreen(
                    draft = previewInvoiceDraft(),
                    viewModel = previewInvoiceWorkspaceViewModel()
                )
            }
        }
    }
}

@Preview(
    name = "Invoice Workspace — Dark",
    showBackground = true
)
@Composable
private fun InvoiceWorkspacePreviewDark() {
    VerityTheme(
        darkTheme = true,
        typography = VerityBaseTypography
    ) {
        VeritySurface(
            type = VeritySurfaceType.Base,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .padding(VeritySpace.Medium.dp)
            ) {
                InvoiceWorkspaceScreen(
                    draft = previewInvoiceDraft(),
                    viewModel = previewInvoiceWorkspaceViewModel()
                )
            }
        }
    }
}

@Composable
private fun previewInvoiceWorkspaceViewModel(): InvoiceWorkspaceViewModel {
    return InvoiceWorkspaceViewModel(
        draftStore = previewDraftStore(),
        customerAutocompleteDataSource = previewCustomerAutocompleteDataSource()
    )
}

@Composable
private fun previewDraftStore(): InvoiceDraftStore {
    return InvoiceDraftStore(initialDraft = previewInvoiceDraft())
}

@Composable
private fun previewCustomerAutocompleteDataSource(): CustomerAutocompleteDataSource {
    return object : CustomerAutocompleteDataSource {

        override suspend fun recentCustomers(
            limit: Int
        ): List<CustomerAutocompleteItem> {
            return emptyList()
        }

        override suspend fun searchCustomers(
            query: String,
            limit: Int
        ): List<CustomerAutocompleteItem> {
            return emptyList()
        }
    }
}

@Composable
private fun previewInvoiceDraft(): InvoiceDraftUiState =
    InvoiceDraftUiState(
        customer = DraftCustomer(
            displayName = "Bhargava Industries",
            gstin = "27AAACB1234Z1Z"
        ),
        lineItems = listOf(
            DraftLineItem(
                description = "Metl Sheet",
                hsnCode = "7208",
                quantity = 10.0,
                unit = "PCS",
                rate = 320.0,
                amount = 3200.0
            ),
            DraftLineItem(
                description = "Cold Rolled Coil",
                hsnCode = "7209",
                quantity = 5.0,
                unit = "KG",
                rate = 450.0,
                amount = 2250.0
            )
        ),
        additionalDetails = DraftAdditionalDetails(
            freightAmount = 500.0
        ),
        summary = DraftSummary(
            subtotal = 5950.0,
            taxTotal = 1071.0,
            grandTotal = 7021.0
        )
    )