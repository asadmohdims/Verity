package com.verity.feature.invoice.ui

import com.verity.core.ui.primitives.VerityTextField
import com.verity.core.ui.primitives.VerityTextFieldRole
import com.verity.core.ui.primitives.VeritySuggestion
import com.verity.core.ui.molecules.VerityEditBlock
import com.verity.core.ui.molecules.VerityEditMode

import androidx.compose.ui.tooling.preview.Preview
import com.verity.core.theme.VerityTheme
import com.verity.core.theme.VerityBaseTypography
import com.verity.invoice.draft.DraftCustomer
import com.verity.invoice.draft.DraftLineItem
import com.verity.invoice.draft.DraftTransportDetails
import com.verity.invoice.draft.DraftSummary

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.verity.core.ui.molecules.VerityHeader
import com.verity.core.ui.molecules.VeritySection
import com.verity.core.ui.primitives.VeritySpacer
import com.verity.core.ui.molecules.VerityInvoiceLineItemRow
import com.verity.core.ui.primitives.VeritySpace
import com.verity.core.ui.primitives.VerityText
import com.verity.core.ui.primitives.VerityTextStyle
import com.verity.core.formatting.money.Money
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

private fun CustomerAutocompleteItem.toVeritySuggestion(): VeritySuggestion {
    val secondaryText =
        listOfNotNull(city, state).joinToString(", ").ifBlank { null }

    return VeritySuggestion(
        id = customerId,
        primary = customerName,
        secondary = secondaryText
    )
}

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
            subtitle = draft.documentType.name.lowercase().replaceFirstChar { it.uppercase() },
            trailing = {
                VerityText(
                    text = "Search",
                    style = VerityTextStyle.Label
                )
            }
        )

        VeritySpacer(size = VeritySpace.Large)

        VeritySurface(
            type = VeritySurfaceType.Base,
            modifier = Modifier.padding(horizontal = VeritySpace.Small.dp)
        ) {
            VeritySection {

                var isDocTypeMenuOpen by remember { mutableStateOf(false) }

                VerityText(
                    text = "Document Type",
                    style = VerityTextStyle.Label
                )

                VeritySpacer(size = VeritySpace.ExtraSmall)

                VerityText(
                    text = draft.documentType.name.lowercase()
                        .replaceFirstChar { it.uppercase() },
                    style = VerityTextStyle.Body,
                    modifier = Modifier
                        .clickable { isDocTypeMenuOpen = true }
                )

                DropdownMenu(
                    expanded = isDocTypeMenuOpen,
                    onDismissRequest = { isDocTypeMenuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { VerityText("Invoice", VerityTextStyle.Body) },
                        onClick = {
                            viewModel.onDocumentTypeChanged(
                                com.verity.invoice.draft.DraftDocumentType.INVOICE
                            )
                            isDocTypeMenuOpen = false
                        }
                    )
                    DropdownMenuItem(
                        text = { VerityText("Challan", VerityTextStyle.Body) },
                        onClick = {
                            viewModel.onDocumentTypeChanged(
                                com.verity.invoice.draft.DraftDocumentType.CHALLAN
                            )
                            isDocTypeMenuOpen = false
                        }
                    )
                }
            }
        }

        VeritySpacer(size = VeritySpace.Small)

        // ─────────────────────────────────────────────
        // Parties Section (formerly Customer Details)
        // ─────────────────────────────────────────────
        VeritySurface(
            type = VeritySurfaceType.Base,
            modifier = Modifier.padding(horizontal = VeritySpace.Small.dp)
        ) {
            VeritySection(title = "Parties") {
                androidx.compose.foundation.layout.Row {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        VerityTextField(
                            role = VerityTextFieldRole.SelectionSearch,
                            label = "Billed To",
                            placeholder = "Search customer",
                            value = billedToQuery,
                            onValueChange = { newValue ->
                                if (draft.billedTo != null && newValue.isBlank()) {
                                    viewModel.onBilledToCleared()
                                }
                                viewModel.onBilledToQueryChanged(newValue)
                            },
                            editing = true,
                            onEnterEdit = null,
                            onExitEdit = null,
                            suggestions = billedToSuggestions.map { it.toVeritySuggestion() },
                            onSelectSuggestion = { suggestion ->
                                val original =
                                    billedToSuggestions.first { it.customerId == suggestion.id }
                                viewModel.onBilledToSelected(original)
                                viewModel.onBilledToQueryChanged(original.customerName)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    VeritySpacer(size = VeritySpace.Large, horizontal = true)
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        VerityTextField(
                            role = VerityTextFieldRole.SelectionSearch,
                            label = "Shipped To",
                            placeholder = draft.billedTo?.name ?: "Search customer",
                            value = shippedToQuery,
                            onValueChange = { newValue ->
                                if (draft.shippedTo != null && newValue.isBlank()) {
                                    viewModel.onShippedToCleared()
                                }
                                viewModel.onShippedToQueryChanged(newValue)
                            },
                            editing = true,
                            onEnterEdit = null,
                            onExitEdit = null,
                            suggestions = shippedToSuggestions.map { it.toVeritySuggestion() },
                            onSelectSuggestion = { suggestion ->
                                val original =
                                    shippedToSuggestions.first { it.customerId == suggestion.id }
                                viewModel.onShippedToSelected(original)
                                viewModel.onShippedToQueryChanged(original.customerName)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
        VeritySpacer(size = VeritySpace.Medium)

        // ─────────────────────────────────────────────
        // Line Items Section
        // ─────────────────────────────────────────────
        VeritySurface(
            type = VeritySurfaceType.Base,
            modifier = Modifier.padding(horizontal = VeritySpace.Small.dp)
        ) {
            VeritySection(title = "Line Items") {
                var isAddingLineItem by remember { mutableStateOf(false) }
                var editingLineItemIndex by remember { mutableStateOf<Int?>(null) }

                var itemDescription by remember { mutableStateOf("") }
                var itemHsn by remember { mutableStateOf("") }
                var itemQuantity by remember { mutableStateOf("") }
                var itemUnit by remember { mutableStateOf("") }
                var itemRate by remember { mutableStateOf("") }

                if (!draft.lineItems.isEmpty()) {
                    draft.lineItems.forEachIndexed { index, item ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    editingLineItemIndex = index
                                    isAddingLineItem = true

                                    itemDescription = item.description
                                    itemHsn = item.hsnCode
                                    itemQuantity = item.quantity.toString()
                                    itemUnit = item.unit
                                    // Set itemRate as rupees string from paise
                                    itemRate = (item.ratePaise / 100.0).toString()
                                }
                        ) {
                            val amountPaise = (item.quantity * item.ratePaise).toLong()
                            VerityInvoiceLineItemRow(
                                description = item.description,
                                quantity = item.quantity,
                                rate = Money.ofPaise(item.ratePaise),
                                amount = Money.ofPaise(amountPaise),
                                hsnCode = item.hsnCode
                            )
                        }

                        VeritySpacer(size = VeritySpace.Small)
                    }
                }

                VeritySpacer(size = VeritySpace.Small)

                VerityEditBlock(
                    title = null,
                    mode =
                        if (editingLineItemIndex == null)
                            VerityEditMode.Add
                        else
                            VerityEditMode.Edit,
                    expanded = isAddingLineItem,
                    collapsedActionLabel = "Add line item",
                    onCollapsedAction = {
                        editingLineItemIndex = null
                        isAddingLineItem = true
                    },
                    onAdd = {
                        val parsedQuantity = itemQuantity.toLongOrNull() ?: 0L
                        val parsedRateRupees = itemRate.toDoubleOrNull() ?: 0.0
                        val ratePaise = (parsedRateRupees * 100).toLong()
                        viewModel.onAddLineItem(
                            DraftLineItem(
                                description = itemDescription,
                                hsnCode = itemHsn,
                                quantity = parsedQuantity,
                                unit = itemUnit,
                                ratePaise = ratePaise
                            )
                        )

                        editingLineItemIndex = null
                        isAddingLineItem = false
                        itemDescription = ""
                        itemHsn = ""
                        itemQuantity = ""
                        itemUnit = ""
                        itemRate = ""
                    },
                    onSave = {
                        val parsedQuantity = itemQuantity.toLongOrNull() ?: 0L
                        val parsedRateRupees = itemRate.toDoubleOrNull() ?: 0.0
                        val ratePaise = (parsedRateRupees * 100).toLong()
                        viewModel.onUpdateLineItem(
                            index = editingLineItemIndex!!,
                            item = DraftLineItem(
                                description = itemDescription,
                                hsnCode = itemHsn,
                                quantity = parsedQuantity,
                                unit = itemUnit,
                                ratePaise = ratePaise
                            )
                        )

                        editingLineItemIndex = null
                        isAddingLineItem = false
                        itemDescription = ""
                        itemHsn = ""
                        itemQuantity = ""
                        itemUnit = ""
                        itemRate = ""
                    },
                    onDelete = {
                        val index = editingLineItemIndex
                        if (index != null) {
                            viewModel.onRemoveLineItem(index)
                        }

                        editingLineItemIndex = null
                        isAddingLineItem = false
                        itemDescription = ""
                        itemHsn = ""
                        itemQuantity = ""
                        itemUnit = ""
                        itemRate = ""
                    },
                    onCancel = {
                        editingLineItemIndex = null
                        isAddingLineItem = false
                        itemDescription = ""
                        itemHsn = ""
                        itemQuantity = ""
                        itemUnit = ""
                        itemRate = ""
                    }
                ) {
                    VerityTextField(
                        role = VerityTextFieldRole.Basic,
                        label = "Description",
                        value = itemDescription,
                        onValueChange = { itemDescription = it },
                        editing = true,
                        onEnterEdit = null,
                        onExitEdit = null,
                        suggestions = emptyList(),
                        onSelectSuggestion = null
                    )

                    VeritySpacer(size = VeritySpace.Small)

                    VerityTextField(
                        role = VerityTextFieldRole.Basic,
                        label = "HSN Code",
                        value = itemHsn,
                        onValueChange = { itemHsn = it },
                        editing = true,
                        onEnterEdit = null,
                        onExitEdit = null,
                        suggestions = emptyList(),
                        onSelectSuggestion = null
                    )

                    VeritySpacer(size = VeritySpace.Small)

                    VerityTextField(
                        role = VerityTextFieldRole.Basic,
                        label = "Quantity",
                        value = itemQuantity,
                        onValueChange = { itemQuantity = it },
                        editing = true,
                        onEnterEdit = null,
                        onExitEdit = null,
                        suggestions = emptyList(),
                        onSelectSuggestion = null
                    )

                    VeritySpacer(size = VeritySpace.Small)

                    VerityTextField(
                        role = VerityTextFieldRole.Basic,
                        label = "Unit",
                        value = itemUnit,
                        onValueChange = { itemUnit = it },
                        editing = true,
                        onEnterEdit = null,
                        onExitEdit = null,
                        suggestions = emptyList(),
                        onSelectSuggestion = null
                    )

                    VeritySpacer(size = VeritySpace.Small)

                    VerityTextField(
                        role = VerityTextFieldRole.Basic,
                        label = "Rate",
                        value = itemRate,
                        onValueChange = { itemRate = it },
                        editing = true,
                        onEnterEdit = null,
                        onExitEdit = null,
                        suggestions = emptyList(),
                        onSelectSuggestion = null
                    )
                }
            }
        }

        VeritySpacer(size = VeritySpace.Medium)

        // ─────────────────────────────────────────────
        // Transportation Section
        // ─────────────────────────────────────────────
        VeritySurface(
            type = VeritySurfaceType.Base,
            modifier = Modifier.padding(horizontal = VeritySpace.Small.dp)
        ) {
            VeritySection(title = "Transportation Mode") {

                var isEditingTransport by remember { mutableStateOf(false) }
                var transporterName by remember { mutableStateOf("") }
                var vehicleNumber by remember { mutableStateOf("") }
                var grOrLrNumber by remember { mutableStateOf("") }
                var freightPaise by remember { mutableStateOf("") }

                if (draft.transportDetails == null) {
                    VerityText(
                        text = "No transportation details",
                        style = VerityTextStyle.Caption,
                        modifier = Modifier.clickable {
                            isEditingTransport = true
                        }
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                isEditingTransport = true
                                transporterName = draft.transportDetails.transporterName ?: ""
                                vehicleNumber = draft.transportDetails.vehicleNumber ?: ""
                                grOrLrNumber = draft.transportDetails.grOrLrNumber ?: ""
                                freightPaise = draft.transportDetails.freightPaise?.let { (it / 100.0).toString() } ?: ""
                            }
                    ) {
                        VerityListItem(
                            title = "Transportation",
                            subtitle =
                                listOfNotNull(
                                    draft.transportDetails.transporterName,
                                    draft.transportDetails.vehicleNumber,
                                    draft.transportDetails.grOrLrNumber
                                ).joinToString(" · ")
                        )

                        draft.transportDetails.freightPaise?.let {
                            VeritySpacer(size = VeritySpace.ExtraSmall)
                            val freightRupees = "%.2f".format(it / 100.0)
                            VerityText(
                                text = "Freight: $freightRupees",
                                style = VerityTextStyle.Body
                            )
                        }
                    }
                }

                VeritySpacer(size = VeritySpace.Small)

                VerityEditBlock(
                    title = null,
                    mode =
                        if (draft.transportDetails == null)
                            VerityEditMode.Add
                        else
                            VerityEditMode.Edit,
                    expanded = isEditingTransport,
                    collapsedActionLabel =
                        if (draft.transportDetails == null)
                            "Add transportation details"
                        else
                            null,
                    onCollapsedAction = {
                        isEditingTransport = true
                    },
                    onAdd = {
                        val parsedFreightRupees = freightPaise.toDoubleOrNull() ?: 0.0
                        val freightPaiseLong = (parsedFreightRupees * 100).toLong()
                        viewModel.onTransportDetailsChanged(
                            DraftTransportDetails(
                                transporterName = transporterName,
                                vehicleNumber = vehicleNumber,
                                grOrLrNumber = grOrLrNumber,
                                freightPaise = freightPaiseLong
                            )
                        )

                        isEditingTransport = false
                        transporterName = ""
                        vehicleNumber = ""
                        grOrLrNumber = ""
                        freightPaise = ""
                    },
                    onSave = {
                        val parsedFreightRupees = freightPaise.toDoubleOrNull() ?: 0.0
                        val freightPaiseLong = (parsedFreightRupees * 100).toLong()
                        viewModel.onTransportDetailsChanged(
                            DraftTransportDetails(
                                transporterName = transporterName,
                                vehicleNumber = vehicleNumber,
                                grOrLrNumber = grOrLrNumber,
                                freightPaise = freightPaiseLong
                            )
                        )

                        isEditingTransport = false
                        transporterName = ""
                        vehicleNumber = ""
                        grOrLrNumber = ""
                        freightPaise = ""
                    },
                    onCancel = {
                        isEditingTransport = false
                        transporterName = ""
                        vehicleNumber = ""
                        grOrLrNumber = ""
                        freightPaise = ""
                    }
                ) {
                    VerityTextField(
                        role = VerityTextFieldRole.Basic,
                        label = "Transporter Name",
                        value = transporterName,
                        onValueChange = { transporterName = it },
                        editing = true,
                        onEnterEdit = null,
                        onExitEdit = null,
                        suggestions = emptyList(),
                        onSelectSuggestion = null
                    )

                    VeritySpacer(size = VeritySpace.Small)

                    VerityTextField(
                        role = VerityTextFieldRole.Basic,
                        label = "Vehicle Number",
                        value = vehicleNumber,
                        onValueChange = { vehicleNumber = it },
                        editing = true,
                        onEnterEdit = null,
                        onExitEdit = null,
                        suggestions = emptyList(),
                        onSelectSuggestion = null
                    )

                    VeritySpacer(size = VeritySpace.Small)

                    VerityTextField(
                        role = VerityTextFieldRole.Basic,
                        label = "GR / LR Number",
                        value = grOrLrNumber,
                        onValueChange = { grOrLrNumber = it },
                        editing = true,
                        onEnterEdit = null,
                        onExitEdit = null,
                        suggestions = emptyList(),
                        onSelectSuggestion = null
                    )

                    VeritySpacer(size = VeritySpace.Small)

                    VerityTextField(
                        role = VerityTextFieldRole.Basic,
                        label = "Freight Amount",
                        value = freightPaise,
                        onValueChange = { freightPaise = it },
                        editing = true,
                        onEnterEdit = null,
                        onExitEdit = null,
                        suggestions = emptyList(),
                        onSelectSuggestion = null
                    )
                }
            }
        }

        VeritySpacer(size = VeritySpace.Medium)

        // ─────────────────────────────────────────────
        // Summary Section
        // ─────────────────────────────────────────────
        VeritySurface(
            type = VeritySurfaceType.Assist,
            modifier = Modifier.padding(horizontal = VeritySpace.Small.dp)
        ) {
            VeritySection(title = "Summary") {
                VerityText(
                    text = "Subtotal: ${"%.2f".format(draft.summary.subtotalPaise / 100.0)}",
                    style = VerityTextStyle.Body
                )

                VeritySpacer(size = VeritySpace.ExtraSmall)

                VerityText(
                    text = "Freight: ${"%.2f".format((draft.transportDetails?.freightPaise ?: 0L) / 100.0)}",
                    style = VerityTextStyle.Body
                )

                draft.summary.tax?.let { tax ->
                    VeritySpacer(size = VeritySpace.Small)
                    when (tax.mode) {
                        com.verity.invoice.draft.DraftTaxMode.INTRA_STATE -> {
                            tax.cgst?.let {
                                VerityText(
                                    text = "CGST (${it.ratePercent}%): ${"%.2f".format(it.amountPaise / 100.0)}",
                                    style = VerityTextStyle.Body
                                )
                            }
                            tax.sgst?.let {
                                VerityText(
                                    text = "SGST (${it.ratePercent}%): ${"%.2f".format(it.amountPaise / 100.0)}",
                                    style = VerityTextStyle.Body
                                )
                            }
                        }
                        com.verity.invoice.draft.DraftTaxMode.INTER_STATE -> {
                            tax.igst?.let {
                                VerityText(
                                    text = "IGST (${it.ratePercent}%): ${"%.2f".format(it.amountPaise / 100.0)}",
                                    style = VerityTextStyle.Body
                                )
                            }
                        }
                    }
                    VeritySpacer(size = VeritySpace.ExtraSmall)
                    VerityText(
                        text = "Tax Total: ${"%.2f".format(draft.summary.taxTotalPaise / 100.0)}",
                        style = VerityTextStyle.Body
                    )
                }

                VeritySpacer(size = VeritySpace.Small)

                VeritySpacer(size = VeritySpace.Small)
                VerityText(
                    text = "Grand Total: ${"%.2f".format(draft.summary.grandTotalPaise / 100.0)}",
                    style = VerityTextStyle.Title
                )
            }
        }
    } // closes Column
} // closes InvoiceWorkspaceScreen

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
                description = "Metal Sheet",
                hsnCode = "7208",
                quantity = 10,
                unit = "PCS",
                ratePaise = 32000
            ),
            DraftLineItem(
                description = "Cold Rolled Coil",
                hsnCode = "7209",
                quantity = 5,
                unit = "KG",
                ratePaise = 45000
            )
        ),
        transportDetails = DraftTransportDetails(
            freightPaise = 50000
        ),
        summary = DraftSummary(
            subtotalPaise = 595000,
            tax = com.verity.invoice.draft.DraftTaxBreakdown(
                mode = com.verity.invoice.draft.DraftTaxMode.INTRA_STATE,
                cgst = com.verity.invoice.draft.DraftTaxComponent(
                    ratePercent = 9,
                    amountPaise = 53550
                ),
                sgst = com.verity.invoice.draft.DraftTaxComponent(
                    ratePercent = 9,
                    amountPaise = 53550
                )
            ),
            taxTotalPaise = 107100,
            grandTotalPaise = 702100
        )
    )