package com.verity.feature.invoice.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.verity.core.formatting.money.formatPaiseAsRupeesInput
import com.verity.core.formatting.money.parseRupeesInputToPaise
import com.verity.core.ui.primitives.VerityButton
import com.verity.core.ui.primitives.VerityButtonRole
import com.verity.core.ui.primitives.VerityButtonState
import com.verity.core.ui.primitives.VeritySpace
import com.verity.core.ui.primitives.VeritySpacer
import com.verity.core.ui.primitives.VeritySurface
import com.verity.core.ui.primitives.VeritySurfaceType
import com.verity.core.ui.primitives.VerityTextField
import com.verity.core.ui.primitives.VerityTextFieldRole
import com.verity.core.ui.primitives.dp
import com.verity.feature.invoice.draft.DraftLineItem

/**
 * LineItemEntryScreen
 *
 * A dedicated full-screen surface for adding/editing one invoice line item — replaces the old
 * inline VerityEditBlock that used to expand in place inside InvoiceWorkspaceScreen's single long
 * scrollable Column. That shape is what let the keyboard cover the field being typed into on a
 * long invoice: the edit block could open dozens of items down, sharing one scroll region with
 * every other section on the screen, with no `imePadding()` anywhere in the app to shrink the
 * visible viewport for the keyboard.
 *
 * This screen is a normal pushed (Support-mode) NavHost destination sharing the same
 * InvoiceWorkspaceViewModel/InvoiceDraftStore instance as the Workspace screen underneath it —
 * not a second owner of line-item state, just a different renderer over the same source of truth,
 * the same relationship Preview already has with Workspace. `imePadding()` on the root Column
 * plus Compose's own scroll-to-focused-field behavior (implicit in `Modifier.verticalScroll`)
 * was the original plan for field-to-field movement, but that didn't hold up on-device once
 * focus moved between fields with the keyboard already open (found 2026-09-15, on Transportation
 * Mode's identical field chain) — every field here explicitly wires its own
 * `BringIntoViewRequester` via `rememberFocusScrollModifier` instead of trusting the implicit
 * behavior, matching this app's "Focus/IME ownership: explicit... never inferred" rule.
 *
 * Add mode offers "Save & Add Another" as the primary action — re-opening a blank form with focus
 * back on Description without leaving the screen — because the actual pain point reported wasn't
 * just "the field is hidden," it was entering many line items in one sitting; closing and
 * re-opening the entry surface for every single row would still be slow even with the keyboard
 * bug fixed.
 */
@Composable
fun LineItemEntryRoute(
    viewModel: InvoiceWorkspaceViewModel,
    editingIndex: Int?,
    onDone: () -> Unit
) {
    val draft by viewModel.uiState.collectAsState()
    val hsnCodeSuggestions by viewModel.hsnCodeSuggestions.collectAsState()
    val unitSuggestions by viewModel.unitSuggestions.collectAsState()

    val existingItem = editingIndex?.let { draft.lineItems.getOrNull(it) }

    LineItemEntryScreen(
        isEditMode = editingIndex != null,
        existingItem = existingItem,
        hsnCodeSuggestions = hsnCodeSuggestions,
        unitSuggestions = unitSuggestions,
        onAdd = { item -> viewModel.onAddLineItem(item) },
        onSave = { item -> viewModel.onUpdateLineItem(index = editingIndex!!, item = item) },
        onDelete = {
            viewModel.onRemoveLineItem(editingIndex!!)
            onDone()
        },
        onDone = onDone
    )
}

@Composable
fun LineItemEntryScreen(
    isEditMode: Boolean,
    existingItem: DraftLineItem?,
    hsnCodeSuggestions: List<String>,
    unitSuggestions: List<String>,
    onAdd: (DraftLineItem) -> Unit,
    onSave: (DraftLineItem) -> Unit,
    onDelete: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    var description by remember { mutableStateOf(existingItem?.description ?: "") }
    var hsn by remember { mutableStateOf(existingItem?.hsnCode ?: "") }
    var quantity by remember { mutableStateOf(existingItem?.quantity?.toString() ?: "") }
    var unit by remember { mutableStateOf(existingItem?.unit ?: "") }
    var rate by remember {
        mutableStateOf(existingItem?.let { formatPaiseAsRupeesInput(it.ratePaise) } ?: "")
    }

    val validation = validateLineItemInput(
        description = description,
        quantityInput = quantity,
        rateInput = rate
    )

    val descriptionFocus = remember { FocusRequester() }
    val hsnFocus = remember { FocusRequester() }
    val quantityFocus = remember { FocusRequester() }
    val unitFocus = remember { FocusRequester() }
    val rateFocus = remember { FocusRequester() }

    val coroutineScope = rememberCoroutineScope()

    fun buildItem() = DraftLineItem(
        description = description,
        hsnCode = hsn,
        quantity = quantity.toLongOrNull(),
        unit = unit,
        ratePaise = parseRupeesInputToPaise(rate)
    )

    fun clearFieldsForNextItem() {
        description = ""
        hsn = ""
        quantity = ""
        unit = ""
        rate = ""
    }

    // Auto-focus the first field on entry, matching CLAUDE.md's stated (until now unimplemented)
    // EditBlock convention: edit surfaces auto-focus their first field on expand.
    LaunchedEffect(Unit) {
        descriptionFocus.requestFocus()
    }

    VeritySurface(type = VeritySurfaceType.Base, modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(VeritySpace.Medium.dp)
        ) {
            VerityTextField(
                role = VerityTextFieldRole.Basic,
                label = "Description",
                value = description,
                onValueChange = { description = it },
                editing = true,
                onEnterEdit = null,
                onExitEdit = null,
                suggestions = emptyList(),
                onSelectSuggestion = null,
                fieldModifier = rememberFocusScrollModifier(descriptionFocus, coroutineScope),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { hsnFocus.requestFocus() })
            )

            VeritySpacer(size = VeritySpace.Small)

            VerityTextField(
                role = VerityTextFieldRole.SelectionSearch,
                label = "HSN Code",
                value = hsn,
                onValueChange = { hsn = it },
                editing = true,
                onEnterEdit = null,
                onExitEdit = null,
                suggestions = hsnCodeSuggestions.toMatchingSuggestions(hsn),
                onSelectSuggestion = { hsn = it.primary },
                expandSuggestionsOnFocus = true,
                fieldModifier = rememberFocusScrollModifier(hsnFocus, coroutineScope),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { quantityFocus.requestFocus() })
            )

            VeritySpacer(size = VeritySpace.Small)

            VerityTextField(
                role = VerityTextFieldRole.Basic,
                label = "Quantity (optional)",
                value = quantity,
                onValueChange = { quantity = it },
                editing = true,
                onEnterEdit = null,
                onExitEdit = null,
                suggestions = emptyList(),
                onSelectSuggestion = null,
                errorText = if (validation.showQuantityError) "Enter a quantity greater than 0" else null,
                fieldModifier = rememberFocusScrollModifier(quantityFocus, coroutineScope),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { unitFocus.requestFocus() })
            )

            VeritySpacer(size = VeritySpace.Small)

            VerityTextField(
                role = VerityTextFieldRole.SelectionSearch,
                label = "Unit",
                value = unit,
                onValueChange = { unit = it },
                editing = true,
                onEnterEdit = null,
                onExitEdit = null,
                suggestions = unitSuggestions.toMatchingSuggestions(unit),
                onSelectSuggestion = { unit = it.primary },
                expandSuggestionsOnFocus = true,
                fieldModifier = rememberFocusScrollModifier(unitFocus, coroutineScope),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { rateFocus.requestFocus() })
            )

            VeritySpacer(size = VeritySpace.Small)

            VerityTextField(
                role = VerityTextFieldRole.Basic,
                label = "Rate",
                value = rate,
                onValueChange = { rate = it },
                editing = true,
                onEnterEdit = null,
                onExitEdit = null,
                suggestions = emptyList(),
                onSelectSuggestion = null,
                errorText = if (validation.showRateError) "Enter a rate greater than 0" else null,
                fieldModifier = rememberFocusScrollModifier(rateFocus, coroutineScope),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (validation.canSubmit) {
                            if (isEditMode) {
                                onSave(buildItem())
                                onDone()
                            } else {
                                onAdd(buildItem())
                                clearFieldsForNextItem()
                                descriptionFocus.requestFocus()
                            }
                        }
                    }
                )
            )

            VeritySpacer(size = VeritySpace.Large)

            if (isEditMode) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    VerityButton(
                        label = "Delete",
                        role = VerityButtonRole.Destructive,
                        onClick = onDelete
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    VerityButton(
                        label = "Save",
                        role = VerityButtonRole.Primary,
                        onClick = {
                            onSave(buildItem())
                            onDone()
                        },
                        state = if (validation.canSubmit) VerityButtonState.Enabled else VerityButtonState.Disabled
                    )
                }
            } else {
                // "Save & Add Another" on the left (Secondary) since it's the one reached for
                // most often mid-entry; "Save & Close" stays Primary/green on the right as the
                // eventual, more consequential action that ends the flow.
                Row(modifier = Modifier.fillMaxWidth()) {
                    VerityButton(
                        label = "Save & Add Another",
                        role = VerityButtonRole.Secondary,
                        onClick = {
                            onAdd(buildItem())
                            clearFieldsForNextItem()
                            descriptionFocus.requestFocus()
                        },
                        state = if (validation.canSubmit) VerityButtonState.Enabled else VerityButtonState.Disabled,
                        modifier = Modifier.weight(1f)
                    )
                    VeritySpacer(size = VeritySpace.Medium, horizontal = true)
                    VerityButton(
                        label = "Save & Close",
                        role = VerityButtonRole.Primary,
                        onClick = {
                            onAdd(buildItem())
                            onDone()
                        },
                        state = if (validation.canSubmit) VerityButtonState.Enabled else VerityButtonState.Disabled,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
