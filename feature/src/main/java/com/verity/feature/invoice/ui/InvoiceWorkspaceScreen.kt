package com.verity.feature.invoice.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.verity.core.document.model.InvoiceDocumentModel
import com.verity.core.formatting.date.DocumentDate
import com.verity.core.formatting.money.Money
import com.verity.core.formatting.money.formatPaiseAsRupeesInput
import com.verity.core.formatting.money.parseRupeesInputToPaise
import com.verity.core.theme.VerityBaseTypography
import com.verity.core.theme.VerityTheme
import com.verity.core.ui.icons.VerityIconGlyph
import com.verity.core.ui.icons.VerityIcons
import com.verity.core.ui.molecules.VerityEditBlock
import com.verity.core.ui.molecules.VerityEditMode
import com.verity.core.ui.molecules.VerityDateField
import com.verity.core.ui.molecules.VerityHeader
import com.verity.core.ui.molecules.VerityInvoiceLineItemRow
import com.verity.core.ui.molecules.VerityInvoiceSummary
import com.verity.core.ui.molecules.VeritySection
import com.verity.core.ui.molecules.VerityTransportSummaryRow
import com.verity.core.ui.molecules.VeritySnackbar
import com.verity.core.ui.primitives.VerityDivider
import com.verity.core.ui.primitives.VerityDividerStrength
import com.verity.core.ui.primitives.VeritySpace
import com.verity.core.ui.primitives.VeritySpacer
import com.verity.core.ui.primitives.VeritySuggestion
import com.verity.core.ui.primitives.VeritySurface
import com.verity.core.ui.primitives.VeritySurfaceType
import com.verity.core.ui.primitives.VerityButton
import com.verity.core.ui.primitives.VerityButtonRole
import com.verity.core.ui.primitives.VerityButtonState
import com.verity.core.ui.primitives.VerityText
import com.verity.core.ui.primitives.VerityTextField
import com.verity.core.ui.primitives.VerityTextFieldRole
import com.verity.core.ui.primitives.VerityTextStyle
import com.verity.core.ui.primitives.dp
import com.verity.feature.invoice.autocomplete.CustomerAutocompleteDataSource
import com.verity.feature.invoice.autocomplete.CustomerAutocompleteItem
import com.verity.feature.referencelist.ReferenceListDataSource
import com.verity.feature.referencelist.ReferenceListItem
import com.verity.feature.referencelist.ReferenceListKind
import com.verity.feature.invoice.draft.DraftAddress
import com.verity.feature.invoice.draft.DraftCustomer
import com.verity.feature.invoice.draft.DraftDocumentType
import com.verity.feature.invoice.draft.DraftInboundChallanReference
import com.verity.feature.invoice.draft.DraftLineItem
import com.verity.feature.invoice.draft.DraftSummary
import com.verity.feature.invoice.draft.DraftTaxBreakdown
import com.verity.feature.invoice.draft.DraftTaxComponent
import com.verity.feature.invoice.draft.DraftTaxMode
import com.verity.feature.invoice.draft.DraftTransportDetails
import com.verity.feature.invoice.draft.InvoiceDraftStore
import com.verity.feature.invoice.draft.InvoiceDraftUiState
import com.verity.feature.invoice.finalize.DocumentNumberPreviewDataSource
import com.verity.feature.invoice.finalize.InvoiceFinalizer
import com.verity.feature.invoice.finalize.JobWorkLinkage
import com.verity.feature.invoice.pdf.InvoicePdfRenderer
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
 * Filters a Settings-managed reference list (Transporter Name / HSN Code) down to entries
 * matching [query], client-side — both lists are small enough (a business's own curated values)
 * that this doesn't need a query round trip the way Customer autocomplete's does. A blank query
 * shows the whole list, same as tapping into an empty field to browse what's available.
 */
internal fun List<String>.toMatchingSuggestions(query: String): List<VeritySuggestion> =
    filter { query.isBlank() || it.contains(query, ignoreCase = true) }
        .map { VeritySuggestion(id = it, primary = it) }

/**
 * Wires focus + an explicit scroll-into-view for one field in a keyboard Next/Done chain.
 * `VerityTextField`/`OutlinedTextField` is supposed to auto-scroll itself into view on focus
 * change even without this, but that didn't hold up on-device once focus moved between fields
 * with the keyboard already open (found 2026-09-15, Transportation Mode: the field a `Next` tap
 * landed on wasn't guaranteed visible) — so this makes the scroll explicit instead of trusting
 * that implicit behavior, consistent with this app's "Focus/IME ownership: explicit... never
 * inferred" rule. Shared with LineItemEntryScreen, which chains fields the same way, and (2026-09-16)
 * InvoicePreviewScreen's Notes to Self field.
 *
 * This relies on the screen's root scrollable applying `Modifier.imePadding()` *before*
 * `Modifier.verticalScroll(...)` in its chain (imePadding shrinking the space the scroll container
 * itself is measured against, not just padding added inside an already-full-height scroll region)
 * — see LineItemEntryScreen's root Column for the working order. Get that order backwards and
 * `bringIntoView()` silently becomes a no-op: the scrollable's own viewport bounds never shrink, so
 * it always computes the focused field as "already visible," even though the keyboard is genuinely
 * covering it on screen (found on-device 2026-09-16, InvoicePreviewScreen: manual scroll worked
 * fine — content did grow taller — but the automatic scroll-into-view never fired, because the
 * order there was `verticalScroll().imePadding()`, the reverse of this).
 */
@Composable
internal fun rememberFocusScrollModifier(
    focusRequester: FocusRequester,
    coroutineScope: CoroutineScope
): Modifier {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    return Modifier
        .focusRequester(focusRequester)
        .bringIntoViewRequester(bringIntoViewRequester)
        .onFocusChanged { focusState ->
            if (focusState.isFocused) {
                coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
            }
        }
}

/**
 * InvoiceWorkspaceScreen
 *
 * Primary working surface for invoice / challan creation — the user's daily workspace and the
 * default landing screen of the application.
 *
 * Owns the full draft editing flow (Parties, Line Items, Transportation, Summary) via
 * VerityEditBlock sections, and emits intents to the InvoiceWorkspaceViewModel. Preview and
 * finalize are driven from the app-level NavHost (see MainActivity), not from this screen.
 */
@Composable
fun InvoiceWorkspaceRoute(
    viewModel: InvoiceWorkspaceViewModel,
    onAddLineItem: () -> Unit,
    onEditLineItem: (Int) -> Unit,
    canPreview: Boolean,
    onPreview: () -> Unit,
    onDiscard: () -> Unit
) {
    // The route is only ever entered via AppNavShell.goToWorkspace(), which guarantees a draft
    // exists (onCreateInvoice() runs first if none is active) before navigating here, and the
    // route is fully removed from the back stack at finalize — so there is no longer a state
    // where WORKSPACE is composed without an active draft. The old empty-state "Create Invoice"
    // prompt this used to fall back to was redundant with the FAB and is removed.
    val draft by viewModel.uiState.collectAsState()

    InvoiceWorkspaceScreen(
        draft = draft,
        viewModel = viewModel,
        onAddLineItem = onAddLineItem,
        onEditLineItem = onEditLineItem,
        canPreview = canPreview,
        onPreview = onPreview,
        onDiscard = onDiscard
    )
}

@Composable
fun InvoiceWorkspaceScreen(
    draft: InvoiceDraftUiState,
    viewModel: InvoiceWorkspaceViewModel,
    onAddLineItem: () -> Unit,
    onEditLineItem: (Int) -> Unit,
    canPreview: Boolean,
    onPreview: () -> Unit,
    onDiscard: () -> Unit
) {
    val billedToQuery by viewModel.billedToQuery.collectAsState()
    val billedToSuggestions by viewModel.billedToSuggestions.collectAsState()

    val shippedToQuery by viewModel.shippedToQuery.collectAsState()
    val shippedToSuggestions by viewModel.shippedToSuggestions.collectAsState()

    val transporterNameSuggestions by viewModel.transporterNameSuggestions.collectAsState()
    val predictedDocumentNumber by viewModel.predictedDocumentNumber.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showDiscardConfirmDialog by remember { mutableStateOf(false) }

    // Line item deletion now happens on a separate full-screen surface (LineItemEntryScreen),
    // which pops back here immediately — so the "Undo" snackbar it used to show inline has to be
    // shown from here instead, once the deletion event arrives. See
    // InvoiceWorkspaceViewModel.lineItemDeleted for why this is a StateFlow, not a one-shot event.
    val lineItemDeleted by viewModel.lineItemDeleted.collectAsState()
    LaunchedEffect(lineItemDeleted) {
        val deleted = lineItemDeleted ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = "Line item deleted",
            actionLabel = "Undo",
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.onInsertLineItemAt(deleted.index, deleted.item)
        }
        viewModel.onLineItemDeletedEventConsumed()
    }

    val workspaceScrollState = rememberScrollState()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(workspaceScrollState)
        ) {

        VeritySpacer(size = VeritySpace.Large)

        // ─────────────────────────────────────────────
        // Document Type + Parties — one merged section, matching InvoiceWorkspace.dc.html's
        // single `.section` (no separate "Parties" header).
        // ─────────────────────────────────────────────
        VeritySurface(
            type = VeritySurfaceType.Base,
            modifier = Modifier.padding(horizontal = VeritySpace.Small.dp)
        ) {
            VeritySection {

                var isDocTypeMenuOpen by remember { mutableStateOf(false) }
                var isEditingBilledTo by remember { mutableStateOf(false) }
                var isEditingShippedTo by remember { mutableStateOf(false) }

                VerityText(
                    text = "Document Type",
                    style = VerityTextStyle.Label
                )

                VeritySpacer(size = VeritySpace.ExtraSmall)

                // A continuation Invoice draft (opened via onContinueToJobWorkInvoice()) has
                // already burned its reserved number against a specific Challan — switching its
                // type away would orphan that reservation, so the control is locked.
                val isDocumentTypeLocked = draft.jobWorkChallanLink != null
                val documentTypeLabel = if (draft.documentType == DraftDocumentType.CHALLAN && draft.isJobWorkFlow) {
                    "Job Work"
                } else {
                    draft.documentType.name.lowercase().replaceFirstChar { it.uppercase() }
                }

                // Bordered select field, matching `.selectfield` — a plain clickable Text (no
                // border/chevron) gave no visual affordance this was a selector.
                //
                // The trigger Row and its DropdownMenu are wrapped in their own tight Box:
                // DropdownMenu anchors itself to its *enclosing layout's* bounds, not to any
                // specific sibling — left as a direct child of this whole section's Column (as it
                // was before), that enclosing layout was the entire section, so the menu opened
                // anchored to the section's full height (visually: below Billed To/Shipped To)
                // instead of right under this field. A Box containing only the trigger shrinks to
                // the trigger's own bounds, which is what the popup then anchors to.
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .border(
                                width = 1.dp,
                                color = VerityTheme.colors.borders.subtle,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .let { rowModifier ->
                                if (isDocumentTypeLocked) rowModifier
                                else rowModifier.clickable { isDocTypeMenuOpen = true }
                            }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VerityText(
                            text = documentTypeLabel,
                            style = VerityTextStyle.Body
                        )
                        if (!isDocumentTypeLocked) {
                            VerityIconGlyph(
                                icon = VerityIcons.ChevronDown,
                                contentDescription = null
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = isDocTypeMenuOpen,
                        onDismissRequest = { isDocTypeMenuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { VerityText("Invoice", VerityTextStyle.Body) },
                            onClick = {
                                viewModel.onDocumentTypeChanged(
                                    DraftDocumentType.INVOICE
                                )
                                isDocTypeMenuOpen = false
                            }
                        )
                        DropdownMenuItem(
                            text = { VerityText("Challan", VerityTextStyle.Body) },
                            onClick = {
                                viewModel.onDocumentTypeChanged(
                                    DraftDocumentType.CHALLAN
                                )
                                isDocTypeMenuOpen = false
                            }
                        )
                        DropdownMenuItem(
                            text = { VerityText("Job Work", VerityTextStyle.Body) },
                            onClick = {
                                viewModel.onDocumentTypeChanged(
                                    DraftDocumentType.CHALLAN
                                )
                                viewModel.onJobWorkFlowChanged(true)
                                isDocTypeMenuOpen = false
                            }
                        )
                    }
                }

                if (isDocumentTypeLocked) {
                    VeritySpacer(size = VeritySpace.ExtraSmall)
                    VerityText(
                        text = "Locked — continues Challan ${draft.jobWorkChallanLink?.challanDocumentNumber}",
                        style = VerityTextStyle.Caption
                    )
                }

                // ─────────────────────────────────────────────
                // Document number — prominent, not buried below Line Items. A job-work
                // continuation Invoice already has a real, reserved number (jobWorkChallanLink,
                // set when "Continue to Invoice" opened this draft); every other draft gets a
                // non-binding "likely" prediction instead (DocumentNumberPreviewDataSource) — a
                // delight, not a guarantee, since the real number is only assigned at finalize.
                // ─────────────────────────────────────────────
                val jobWorkLink = draft.jobWorkChallanLink
                val numberNoun = when (draft.documentType) {
                    DraftDocumentType.INVOICE -> "Invoice"
                    DraftDocumentType.CHALLAN -> "Challan"
                }
                if (jobWorkLink != null) {
                    VeritySpacer(size = VeritySpace.Small)
                    VeritySurface(type = VeritySurfaceType.Assist) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(VeritySpace.Small.dp)
                        ) {
                            VerityText(text = "Reserved Invoice No.", style = VerityTextStyle.Label)
                            VeritySpacer(size = VeritySpace.ExtraSmall)
                            VerityText(text = jobWorkLink.reservedInvoiceNumber, style = VerityTextStyle.Display)
                            VeritySpacer(size = VeritySpace.ExtraSmall)
                            VerityText(
                                text = "Continues Challan ${jobWorkLink.challanDocumentNumber} · " +
                                    DocumentDate.format(jobWorkLink.challanDate),
                                style = VerityTextStyle.Caption
                            )
                        }
                    }
                } else {
                    val predicted = predictedDocumentNumber
                    if (predicted != null) {
                        VeritySpacer(size = VeritySpace.Small)
                        VeritySurface(type = VeritySurfaceType.Assist) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(VeritySpace.Small.dp)
                            ) {
                                VerityText(text = "Likely $numberNoun No.", style = VerityTextStyle.Label)
                                VeritySpacer(size = VeritySpace.ExtraSmall)
                                VerityText(text = predicted, style = VerityTextStyle.Display)
                            }
                        }
                    }
                }

                VeritySpacer(size = VeritySpace.Medium)

                // Defaults to today (InvoiceDraftUiState.issueDate) — capped at today so the
                // picker can only backdate, never post-date, matching GST practice of never
                // issuing a document ahead of the day it's actually raised.
                VerityDateField(
                    label = "$numberNoun Date",
                    value = draft.issueDate,
                    onValueChange = { date -> date?.let(viewModel::onIssueDateChanged) },
                    formatter = { DocumentDate.format(it) },
                    maxDate = LocalDate.now()
                )

                VeritySpacer(size = VeritySpace.Medium)

                Row {
                    Column(modifier = Modifier.weight(1f)) {
                        PartyField(
                            label = "Billed To",
                            addLabel = "Add billed-to party",
                            value = draft.billedTo?.name,
                            isEditing = isEditingBilledTo,
                            onStartEditing = { isEditingBilledTo = true },
                            onCancelEditing = { isEditingBilledTo = false }
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
                                    // onBilledToSelected() already clears billedToQuery back to "".
                                    // Re-setting it to the selected name here was pointless (the
                                    // block collapses to the read-only view on the next line, which
                                    // renders draft.billedTo, not billedToQuery) and left that name
                                    // stuck in billedToQuery — which onCreateInvoice() never resets
                                    // — so the *next* invoice's Billed To search box opened
                                    // pre-filled with the previous invoice's customer.
                                    viewModel.onBilledToSelected(original)
                                    isEditingBilledTo = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    VeritySpacer(size = VeritySpace.Large, horizontal = true)
                    Column(modifier = Modifier.weight(1f)) {
                        PartyField(
                            label = "Shipped To",
                            addLabel = "Add shipped-to party",
                            value = draft.effectiveShippedTo?.name,
                            isEditing = isEditingShippedTo,
                            onStartEditing = { isEditingShippedTo = true },
                            onCancelEditing = { isEditingShippedTo = false }
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
                                    // See the matching comment in the Billed To handler above —
                                    // same stale-leftover-query bug, same fix.
                                    viewModel.onShippedToSelected(original)
                                    isEditingShippedTo = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        VerityDivider(
            strength = VerityDividerStrength.Divider,
            modifier = Modifier.padding(vertical = VeritySpace.Medium.dp)
        )

        // ─────────────────────────────────────────────
        // Job Work — "Received Vide Challan" (any Challan) + auto reference rows
        // ─────────────────────────────────────────────
        if (draft.documentType == DraftDocumentType.CHALLAN || draft.jobWorkChallanLink != null) {
            VeritySurface(
                type = VeritySurfaceType.Base,
                modifier = Modifier.padding(horizontal = VeritySpace.Small.dp)
            ) {
                VeritySection(title = "Job Work") {

                    if (draft.documentType == DraftDocumentType.CHALLAN) {
                        var isEditingInboundReference by remember { mutableStateOf(false) }
                        var inboundChallanNumber by remember { mutableStateOf("") }
                        var inboundChallanDate by remember { mutableStateOf<LocalDate?>(null) }

                        if (draft.inboundChallanReference != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        isEditingInboundReference = true
                                        inboundChallanNumber = draft.inboundChallanReference.challanNumber
                                        inboundChallanDate = draft.inboundChallanReference.challanDate
                                    },
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                VerityText(text = "Received Vide Challan No.", style = VerityTextStyle.Label)
                                VerityText(
                                    text = draft.inboundChallanReference.challanDate?.let {
                                        "${draft.inboundChallanReference.challanNumber} · ${DocumentDate.format(it)}"
                                    } ?: draft.inboundChallanReference.challanNumber,
                                    style = VerityTextStyle.Body
                                )
                            }

                            VeritySpacer(size = VeritySpace.Small)
                        }

                        VerityEditBlock(
                            title = null,
                            mode = if (draft.inboundChallanReference == null) VerityEditMode.Add else VerityEditMode.Edit,
                            expanded = isEditingInboundReference,
                            collapsedActionLabel = if (draft.inboundChallanReference == null) "Add received-vide-challan reference" else null,
                            onCollapsedAction = { isEditingInboundReference = true },
                            onAdd = {
                                viewModel.onInboundChallanReferenceChanged(
                                    DraftInboundChallanReference(
                                        challanNumber = inboundChallanNumber,
                                        challanDate = inboundChallanDate
                                    )
                                )
                                isEditingInboundReference = false
                                inboundChallanNumber = ""
                                inboundChallanDate = null
                            },
                            onSave = {
                                viewModel.onInboundChallanReferenceChanged(
                                    DraftInboundChallanReference(
                                        challanNumber = inboundChallanNumber,
                                        challanDate = inboundChallanDate
                                    )
                                )
                                isEditingInboundReference = false
                                inboundChallanNumber = ""
                                inboundChallanDate = null
                            },
                            onCancel = {
                                isEditingInboundReference = false
                                inboundChallanNumber = ""
                                inboundChallanDate = null
                            }
                        ) {
                            VerityTextField(
                                role = VerityTextFieldRole.Basic,
                                label = "Received Vide Challan No.",
                                value = inboundChallanNumber,
                                onValueChange = { inboundChallanNumber = it },
                                editing = true,
                                onEnterEdit = null,
                                onExitEdit = null,
                                suggestions = emptyList(),
                                onSelectSuggestion = null,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                            )

                            VeritySpacer(size = VeritySpace.Small)

                            VerityDateField(
                                label = "Dated",
                                value = inboundChallanDate,
                                onValueChange = { inboundChallanDate = it },
                                formatter = { DocumentDate.format(it) }
                            )
                        }

                        if (draft.isJobWorkFlow) {
                            VeritySpacer(size = VeritySpace.Small)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                VerityText(text = "Ref: Invoice No.", style = VerityTextStyle.Label)
                                VerityText(
                                    text = "(assigned automatically at finalize)",
                                    style = VerityTextStyle.Caption
                                )
                            }
                        }
                    }

                    // The reserved Invoice number itself is now shown prominently at the top of
                    // the screen (see the banner right below Document Type), not buried here —
                    // this row keeps just the Challan reference for detail.
                    draft.jobWorkChallanLink?.let { link ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            VerityText(text = "Ref: Challan No.", style = VerityTextStyle.Label)
                            VerityText(
                                text = "${link.challanDocumentNumber} · ${DocumentDate.format(link.challanDate)}",
                                style = VerityTextStyle.Body
                            )
                        }
                    }
                }
            }

            VeritySpacer(size = VeritySpace.Medium)
        }

        // ─────────────────────────────────────────────
        // Line Items Section
        // ─────────────────────────────────────────────
        VeritySurface(
            type = VeritySurfaceType.Base,
            modifier = Modifier.padding(horizontal = VeritySpace.Small.dp)
        ) {
            VeritySection(title = "Line Items") {
                // Add/Edit now happens on a dedicated full-screen surface (LineItemEntryScreen),
                // not inline here — a growing stack of add/edit fields sharing this screen's one
                // long scroll region is exactly what let the keyboard cover fields on a long
                // invoice with no way to bring them back into view. See LineItemEntryScreen's
                // header comment for the full reasoning.
                if (!draft.lineItems.isEmpty()) {
                    draft.lineItems.forEachIndexed { index, item ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEditLineItem(index) }
                        ) {
                            val amountPaise = (item.quantity ?: 1L) * item.ratePaise
                            VerityInvoiceLineItemRow(
                                description = item.description,
                                quantity = item.quantity,
                                unit = item.unit,
                                rate = Money.ofPaise(item.ratePaise),
                                amount = Money.ofPaise(amountPaise),
                                hsnCode = item.hsnCode
                            )
                        }

                        VeritySpacer(size = VeritySpace.Small)
                    }
                }

                VerityEditBlock(
                    title = null,
                    mode = VerityEditMode.Add,
                    expanded = false,
                    collapsedActionLabel = "Add line item",
                    onCollapsedAction = onAddLineItem,
                    content = {}
                )
            }
        }

        VerityDivider(
            strength = VerityDividerStrength.Divider,
            modifier = Modifier.padding(vertical = VeritySpace.Medium.dp)
        )

        // ─────────────────────────────────────────────
        // Transportation Section
        // ─────────────────────────────────────────────
        // onGloballyPositioned + the LaunchedEffect below scroll this section's heading to the
        // top of the screen the moment it expands — reported on-device: with imePadding() alone,
        // the block still opened wherever it happened to sit on the page, so only the first field
        // was ever guaranteed visible and everything past it needed a manual scroll. Field-to-field
        // movement uses ImeAction.Next + FocusRequesters (below) too, but NOT Compose's own
        // built-in scroll-to-focused-field — that turned out not to fire reliably once focus moved
        // between fields with the keyboard already open (found on-device, this exact chain), so
        // each field explicitly requests its own scroll via rememberFocusScrollModifier instead of
        // trusting the implicit behavior.
        var transportSectionTop by remember { mutableStateOf(0f) }

        VeritySurface(
            type = VeritySurfaceType.Base,
            modifier = Modifier
                .padding(horizontal = VeritySpace.Small.dp)
                .onGloballyPositioned { coordinates ->
                    transportSectionTop = coordinates.positionInParent().y
                }
        ) {
            VeritySection(title = "Transportation Mode") {

                var isEditingTransport by remember { mutableStateOf(false) }
                var transporterName by remember { mutableStateOf("") }
                var vehicleNumber by remember { mutableStateOf("") }
                var grOrLrNumber by remember { mutableStateOf("") }
                var supplyDate by remember { mutableStateOf<LocalDate?>(null) }
                var freightPaise by remember { mutableStateOf("") }
                var ewayBillNumber by remember { mutableStateOf("") }

                val transporterNameFocus = remember { FocusRequester() }
                val vehicleNumberFocus = remember { FocusRequester() }
                val grOrLrNumberFocus = remember { FocusRequester() }
                val freightFocus = remember { FocusRequester() }
                val ewayBillFocus = remember { FocusRequester() }

                LaunchedEffect(isEditingTransport) {
                    if (isEditingTransport) {
                        workspaceScrollState.animateScrollTo(transportSectionTop.toInt())
                    }
                }

                if (draft.transportDetails != null) {
                    VerityTransportSummaryRow(
                        transporterName = draft.transportDetails.transporterName ?: "",
                        vehicleNumber = draft.transportDetails.vehicleNumber,
                        grOrLrNumber = draft.transportDetails.grOrLrNumber,
                        freight = draft.transportDetails.freightPaise?.let { Money.ofPaise(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                isEditingTransport = true
                                transporterName = draft.transportDetails.transporterName ?: ""
                                vehicleNumber = draft.transportDetails.vehicleNumber ?: ""
                                grOrLrNumber = draft.transportDetails.grOrLrNumber ?: ""
                                supplyDate = draft.transportDetails.supplyDate
                                freightPaise =
                                    draft.transportDetails.freightPaise?.let { formatPaiseAsRupeesInput(it) } ?: ""
                                ewayBillNumber = draft.transportDetails.ewayBillNumber ?: ""
                            }
                    )

                    VeritySpacer(size = VeritySpace.Small)
                }

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
                        // Only reached starting a fresh Add (see collapsedActionLabel above) —
                        // editing existing transport details loads its own saved supplyDate via
                        // VerityTransportSummaryRow's onClick instead. Defaults to the document's
                        // own date since goods are typically dispatched the same day it's raised;
                        // still freely editable from there.
                        supplyDate = draft.issueDate
                    },
                    onAdd = {
                        val freightPaiseLong = parseRupeesInputToPaise(freightPaise)
                        viewModel.onTransportDetailsChanged(
                            DraftTransportDetails(
                                transporterName = transporterName,
                                vehicleNumber = vehicleNumber,
                                grOrLrNumber = grOrLrNumber,
                                supplyDate = supplyDate,
                                freightPaise = freightPaiseLong,
                                ewayBillNumber = ewayBillNumber.ifBlank { null }
                            )
                        )

                        isEditingTransport = false
                        transporterName = ""
                        vehicleNumber = ""
                        grOrLrNumber = ""
                        supplyDate = null
                        freightPaise = ""
                        ewayBillNumber = ""
                    },
                    onSave = {
                        val freightPaiseLong = parseRupeesInputToPaise(freightPaise)
                        viewModel.onTransportDetailsChanged(
                            DraftTransportDetails(
                                transporterName = transporterName,
                                vehicleNumber = vehicleNumber,
                                grOrLrNumber = grOrLrNumber,
                                supplyDate = supplyDate,
                                freightPaise = freightPaiseLong,
                                ewayBillNumber = ewayBillNumber.ifBlank { null }
                            )
                        )

                        isEditingTransport = false
                        transporterName = ""
                        vehicleNumber = ""
                        grOrLrNumber = ""
                        supplyDate = null
                        freightPaise = ""
                        ewayBillNumber = ""
                    },
                    onCancel = {
                        isEditingTransport = false
                        transporterName = ""
                        vehicleNumber = ""
                        grOrLrNumber = ""
                        supplyDate = null
                        freightPaise = ""
                        ewayBillNumber = ""
                    }
                ) {
                    VerityTextField(
                        role = VerityTextFieldRole.SelectionSearch,
                        label = "Transporter Name",
                        value = transporterName,
                        onValueChange = { transporterName = it },
                        editing = true,
                        onEnterEdit = null,
                        onExitEdit = null,
                        suggestions = transporterNameSuggestions.toMatchingSuggestions(transporterName),
                        onSelectSuggestion = { transporterName = it.primary },
                        fieldModifier = rememberFocusScrollModifier(transporterNameFocus, coroutineScope),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { vehicleNumberFocus.requestFocus() })
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
                        onSelectSuggestion = null,
                        fieldModifier = rememberFocusScrollModifier(vehicleNumberFocus, coroutineScope),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { grOrLrNumberFocus.requestFocus() })
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
                        onSelectSuggestion = null,
                        fieldModifier = rememberFocusScrollModifier(grOrLrNumberFocus, coroutineScope),
                        // Supply Date is a tap-to-open picker (VerityDateField), not a keyboard
                        // field, so it can't take part in the Next chain — skip straight to Freight.
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { freightFocus.requestFocus() })
                    )

                    VeritySpacer(size = VeritySpace.Small)

                    VerityDateField(
                        label = "Supply Date",
                        value = supplyDate,
                        onValueChange = { supplyDate = it },
                        formatter = { DocumentDate.format(it) }
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
                        onSelectSuggestion = null,
                        fieldModifier = rememberFocusScrollModifier(freightFocus, coroutineScope),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { ewayBillFocus.requestFocus() })
                    )

                    VeritySpacer(size = VeritySpace.Small)

                    VerityTextField(
                        role = VerityTextFieldRole.Basic,
                        label = "E-Way Bill Number",
                        value = ewayBillNumber,
                        onValueChange = { ewayBillNumber = it },
                        editing = true,
                        onEnterEdit = null,
                        onExitEdit = null,
                        suggestions = emptyList(),
                        onSelectSuggestion = null,
                        fieldModifier = rememberFocusScrollModifier(ewayBillFocus, coroutineScope),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                    )
                }
            }
        }

        VeritySpacer(size = VeritySpace.Medium)

        // ─────────────────────────────────────────────
        // Summary Section
        // ─────────────────────────────────────────────
        VeritySection(
            title = "Summary",
            modifier = Modifier.padding(horizontal = VeritySpace.Small.dp)
        ) {
            VerityInvoiceSummary(
                itemsSubtotal = Money.ofPaise(draft.summary.subtotalPaise),
                freight = draft.transportDetails?.freightPaise?.let { Money.ofPaise(it) },
                taxableSubtotal =
                    Money.ofPaise(
                        draft.summary.subtotalPaise +
                            (draft.transportDetails?.freightPaise ?: 0L)
                    ),
                cgst =
                    draft.summary.tax?.cgst?.let {
                        it.ratePercent.toInt() to Money.ofPaise(it.amountPaise)
                    },
                sgst =
                    draft.summary.tax?.sgst?.let {
                        it.ratePercent.toInt() to Money.ofPaise(it.amountPaise)
                    },
                igst =
                    draft.summary.tax?.igst?.let {
                        it.ratePercent.toInt() to Money.ofPaise(it.amountPaise)
                    },
                totalAfterTax = Money.ofPaise(draft.summary.grandTotalPaise)
            )
        }

        VeritySpacer(size = VeritySpace.Medium)

        // ─────────────────────────────────────────────
        // Notes to self — private, never printed on the PDF (see
        // DraftToInvoiceDocument.project(), which never reads draft.selfNotes). A single
        // free-text field, so no VerityEditBlock wrapper: that pattern exists to solve
        // multi-field keyboard-coverage problems this doesn't have.
        // ─────────────────────────────────────────────
        VeritySection(
            title = "Notes to Self (private — not printed)",
            modifier = Modifier.padding(horizontal = VeritySpace.Small.dp)
        ) {
            VerityTextField(
                role = VerityTextFieldRole.Basic,
                label = "Notes",
                placeholder = "e.g. sold this part at this rate, follow up next week",
                value = draft.selfNotes,
                onValueChange = viewModel::onSelfNotesChanged,
                editing = true,
                onEnterEdit = null,
                onExitEdit = null,
                suggestions = emptyList(),
                onSelectSuggestion = null,
                singleLine = false
            )
        }

        VeritySpacer(size = VeritySpace.Medium)

        // ─────────────────────────────────────────────
        // Discard / Preview — the draft's primary actions, placed as the final row of the
        // scrollable content rather than a persistent Scaffold-level bottom bar. Discussed and
        // decided with the user: Summary (immediately above) is always the last section
        // regardless of document type or job-work flow, so this row is never more than a short
        // scroll away from wherever the user was last editing — a pinned bar would permanently
        // cost screen space and sit somewhere a thumb could tap it by accident.
        // ─────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VeritySpace.Small.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VerityButton(
                label = "Discard",
                role = VerityButtonRole.Destructive,
                onClick = { showDiscardConfirmDialog = true },
                modifier = Modifier.weight(1f)
            )
            VerityButton(
                label = "Preview",
                role = VerityButtonRole.Primary,
                state = if (canPreview) VerityButtonState.Enabled else VerityButtonState.Disabled,
                onClick = onPreview,
                modifier = Modifier.weight(1f)
            )
        }

        VeritySpacer(size = VeritySpace.Large)
        } // closes Column

        // Declared after (on top of) the scrollable Column, matching Scaffold's own convention
        // of keeping the snackbar as the topmost layer: as a sibling declared BEFORE the Column,
        // the Column's fillMaxSize() + verticalScroll() intercepted every tap in that screen
        // region — including on the snackbar's own action button — before it could reach the
        // SnackbarHost underneath, even in the empty space where nothing was visibly drawn.
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 8.dp)
        ) { data ->
            VeritySnackbar(snackbarData = data)
        }
    } // closes Box

    // First AlertDialog in the app — a one-off confirmation, not worth a reusable VerityDialog
    // wrapper for a single call site. onDiscardDraft() has no undo path (unlike line-item
    // delete's snackbar above), so a multi-section draft representing real work isn't lost to a
    // stray tap.
    if (showDiscardConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmDialog = false },
            containerColor = VerityTheme.colors.surface.base,
            titleContentColor = VerityTheme.colors.text.primary,
            textContentColor = VerityTheme.colors.text.muted,
            title = { VerityText(text = "Discard this draft?", style = VerityTextStyle.Title) },
            text = { VerityText(text = "This can't be undone.", style = VerityTextStyle.Body) },
            confirmButton = {
                VerityButton(
                    label = "Discard",
                    role = VerityButtonRole.Destructive,
                    onClick = {
                        showDiscardConfirmDialog = false
                        onDiscard()
                    }
                )
            },
            dismissButton = {
                VerityButton(
                    label = "Cancel",
                    role = VerityButtonRole.Secondary,
                    onClick = { showDiscardConfirmDialog = false }
                )
            }
        )
    }
} // closes InvoiceWorkspaceScreen

/**
 * Billed To / Shipped To — EditBlock pattern (read-only label+value once set → tap to reopen the
 * search field → selecting a customer commits and collapses again), matching InvoiceWorkspace.dc.html's
 * `row2` grid and CLAUDE.md's own stated EditBlock convention, consistent with how Line Items and
 * Transportation below already work. [content] is the SelectionSearch field; selecting a
 * suggestion is what commits — there is no separate "Save" tap, so [VerityEditBlock] is used with
 * `onAdd = null`, which renders only a "Cancel" button while expanded.
 */
@Composable
private fun PartyField(
    label: String,
    addLabel: String,
    value: String?,
    isEditing: Boolean,
    onStartEditing: () -> Unit,
    onCancelEditing: () -> Unit,
    content: @Composable () -> Unit
) {
    if (value != null && !isEditing) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onStartEditing)
        ) {
            VerityText(text = label, style = VerityTextStyle.Label)
            VeritySpacer(size = VeritySpace.ExtraSmall)
            VerityText(text = value, style = VerityTextStyle.Body)
        }
    }

    VerityEditBlock(
        mode = VerityEditMode.Add,
        expanded = isEditing,
        collapsedActionLabel = if (value == null) addLabel else null,
        onCollapsedAction = onStartEditing,
        onAdd = null,
        onCancel = onCancelEditing,
        content = content
    )
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
                    viewModel = previewInvoiceWorkspaceViewModel(),
                    onAddLineItem = {},
                    onEditLineItem = {},
                    canPreview = true,
                    onPreview = {},
                    onDiscard = {}
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
                    viewModel = previewInvoiceWorkspaceViewModel(),
                    onAddLineItem = {},
                    onEditLineItem = {},
                    canPreview = true,
                    onPreview = {},
                    onDiscard = {}
                )
            }
        }
    }
}

@Composable
private fun previewInvoiceWorkspaceViewModel(): InvoiceWorkspaceViewModel {
    // remember, not a bare constructor call: lint (ViewModelConstructorInComposable) flags
    // constructing a ViewModel directly in a composable body because it would otherwise be
    // rebuilt on every recomposition. There's no real ViewModelStore in a @Preview to hand this
    // to instead, so remember is the correct fix here, not a suppression.
    return remember {
        InvoiceWorkspaceViewModel(
            draftStore = previewDraftStore(),
            customerAutocompleteDataSource = previewCustomerAutocompleteDataSource(),
            invoiceFinalizer = previewInvoiceFinalizer(),
            invoicePdfRenderer = previewInvoicePdfRenderer(),
            referenceListDataSource = previewReferenceListDataSource(),
            documentNumberPreviewDataSource = previewDocumentNumberPreviewDataSource()
        )
    }
}

private fun previewDocumentNumberPreviewDataSource(): DocumentNumberPreviewDataSource {
    return object : DocumentNumberPreviewDataSource {
        override suspend fun peekNextNumber(documentType: DraftDocumentType): String = "INV-000001"
    }
}

private fun previewInvoiceFinalizer(): InvoiceFinalizer {
    return object : InvoiceFinalizer {
        override suspend fun finalize(
            draft: InvoiceDraftUiState,
            customerId: String,
            jobWorkLinkage: JobWorkLinkage
        ): InvoiceDocumentModel {
            error("Finalize is not available in @Preview")
        }
    }
}

private fun previewInvoicePdfRenderer(): InvoicePdfRenderer {
    return object : InvoicePdfRenderer {
        override suspend fun ensurePdf(document: InvoiceDocumentModel): java.io.File {
            error("PDF generation is not available in @Preview")
        }
    }
}

private fun previewDraftStore(): InvoiceDraftStore {
    return InvoiceDraftStore(initialDraft = previewInvoiceDraft())
}

private fun previewReferenceListDataSource(): ReferenceListDataSource {
    return object : ReferenceListDataSource {
        override suspend fun getAll(kind: ReferenceListKind): List<ReferenceListItem> = emptyList()
        override suspend fun add(kind: ReferenceListKind, value: String) {}
        override suspend fun delete(kind: ReferenceListKind, id: String) {}
    }
}

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

private fun previewInvoiceDraft(): InvoiceDraftUiState =
    InvoiceDraftUiState(
        customer = DraftCustomer(
            displayName = "Bhargava Industries",
            gstin = "27AAACB1234Z1Z"
        ),
        billedTo = DraftAddress(
            name = "Bhargava Industries",
            addressLine1 = "Industrial Area",
            city = "Mumbai",
            state = "Maharashtra",
            stateCode = "27",
            gstin = "27AAACB1234Z1Z",
            pincode = "400001"
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
            tax = DraftTaxBreakdown(
                mode = DraftTaxMode.INTRA_STATE,
                cgst = DraftTaxComponent(
                    ratePercent = 9,
                    amountPaise = 53550
                ),
                sgst = DraftTaxComponent(
                    ratePercent = 9,
                    amountPaise = 53550
                )
            ),
            taxTotalPaise = 107100,
            grandTotalPaise = 702100
        )
    )