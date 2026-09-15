package com.verity.feature.customer.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.verity.core.theme.VerityTheme
import com.verity.core.ui.primitives.VerityButton
import com.verity.core.ui.primitives.VerityButtonState
import com.verity.core.ui.primitives.VerityDivider
import com.verity.core.ui.primitives.VerityDividerStrength
import com.verity.core.ui.primitives.VeritySpace
import com.verity.core.ui.primitives.VeritySpacer
import com.verity.core.ui.primitives.VeritySurface
import com.verity.core.ui.primitives.VeritySurfaceType
import com.verity.core.ui.primitives.VerityText
import com.verity.core.ui.primitives.VerityTextField
import com.verity.core.ui.primitives.VerityTextFieldRole
import com.verity.core.ui.primitives.VerityTextStyle
import com.verity.core.ui.primitives.dp

/**
 * AddEditCustomerRoute
 *
 * Pops back once a save or deactivate actually completes (isSaved/isDeactivated), rather than
 * navigating eagerly on button tap — same "wait for the real state change" shape as the invoice
 * finalize flow's LaunchedEffect(finalizedDocument) in AppNavShell.
 */
@Composable
fun AddEditCustomerRoute(
    viewModel: AddEditCustomerViewModel,
    onDone: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isSaved, state.isDeactivated) {
        if (state.isSaved || state.isDeactivated) {
            onDone()
        }
    }

    AddEditCustomerScreen(
        state = state,
        onCustomerNameChange = viewModel::onCustomerNameChange,
        onGstinChange = viewModel::onGstinChange,
        onPhoneChange = viewModel::onPhoneChange,
        onAddressLine1Change = viewModel::onAddressLine1Change,
        onCityChange = viewModel::onCityChange,
        onStateChange = viewModel::onStateChange,
        onStateCodeChange = viewModel::onStateCodeChange,
        onPincodeChange = viewModel::onPincodeChange,
        onSave = viewModel::onSave,
        onDeactivate = viewModel::onDeactivate
    )
}

/**
 * AddEditCustomerScreen
 *
 * Pure renderer. A dedicated full-screen form, not the inline VerityEditBlock pattern — the
 * approved mockup (AddEditCustomer.dc.html) shows a standalone screen, not an expandable block
 * inside another surface. The mockup's top-bar "Save" text action isn't rendered here: the shared
 * VerityTopBarAction model only supports icon/overflow actions today, so Save is a full-width
 * VerityButton at the bottom of the form instead — avoids extending shared chrome for one screen.
 * "Deactivate customer" applies immediately on check (no separate confirm step, matching the
 * mockup's plain checkbox) and is shown only in Edit mode.
 */
@Composable
fun AddEditCustomerScreen(
    state: AddEditCustomerUiState,
    onCustomerNameChange: (String) -> Unit,
    onGstinChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onAddressLine1Change: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onStateChange: (String) -> Unit,
    onStateCodeChange: (String) -> Unit,
    onPincodeChange: (String) -> Unit,
    onSave: () -> Unit,
    onDeactivate: () -> Unit,
    modifier: Modifier = Modifier
) {
    VeritySurface(
        type = VeritySurfaceType.Base,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(VeritySpace.Medium.dp)
        ) {
            FormField(label = "Business Name", value = state.customerName, onValueChange = onCustomerNameChange)
            VeritySpacer(size = VeritySpace.Small)

            FormField(label = "GSTIN", value = state.gstin, onValueChange = onGstinChange)
            VeritySpacer(size = VeritySpace.Small)

            FormField(
                label = "Phone (optional)",
                value = state.phone,
                onValueChange = onPhoneChange
            )
            VeritySpacer(size = VeritySpace.Small)

            FormField(label = "Address Line 1", value = state.addressLine1, onValueChange = onAddressLine1Change)
            VeritySpacer(size = VeritySpace.Small)

            Row(modifier = Modifier.fillMaxWidth()) {
                FormField(
                    label = "City",
                    value = state.city,
                    onValueChange = onCityChange,
                    modifier = Modifier.weight(1f)
                )
                VeritySpacer(size = VeritySpace.Small, horizontal = true)
                FormField(
                    label = "State",
                    value = state.state,
                    onValueChange = onStateChange,
                    modifier = Modifier.weight(1f)
                )
            }
            VeritySpacer(size = VeritySpace.Small)

            Row(modifier = Modifier.fillMaxWidth()) {
                FormField(
                    label = "State Code",
                    value = state.stateCode,
                    onValueChange = onStateCodeChange,
                    modifier = Modifier.weight(1f)
                )
                VeritySpacer(size = VeritySpace.Small, horizontal = true)
                FormField(
                    label = "Pincode",
                    value = state.pincode,
                    onValueChange = onPincodeChange,
                    modifier = Modifier.weight(1f)
                )
            }

            if (state.isEditMode) {
                VeritySpacer(size = VeritySpace.Large)
                VerityDivider(strength = VerityDividerStrength.Subtle)
                VeritySpacer(size = VeritySpace.Medium)
                DeactivateRow(onDeactivate = onDeactivate)
            }

            VeritySpacer(size = VeritySpace.Large)

            VerityButton(
                label = "Save",
                onClick = onSave,
                state = if (state.canSave && !state.isSaving) VerityButtonState.Enabled else VerityButtonState.Disabled,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    VerityTextField(
        role = VerityTextFieldRole.Basic,
        label = label,
        value = value,
        onValueChange = onValueChange,
        editing = true,
        onEnterEdit = null,
        onExitEdit = null,
        suggestions = emptyList(),
        onSelectSuggestion = null,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun DeactivateRow(onDeactivate: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        VerityText(text = "Deactivate customer", style = VerityTextStyle.Body)
        Checkbox(
            checked = false,
            onCheckedChange = { checked -> if (checked) onDeactivate() },
            colors = CheckboxDefaults.colors(checkedColor = VerityTheme.colors.state.error),
            modifier = Modifier.testTag("deactivate-customer-checkbox")
        )
    }
}
