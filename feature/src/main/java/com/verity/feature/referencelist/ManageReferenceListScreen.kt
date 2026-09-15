package com.verity.feature.referencelist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.verity.core.theme.VerityTheme
import com.verity.core.ui.molecules.VerityListItem
import com.verity.core.ui.primitives.VerityButton
import com.verity.core.ui.primitives.VerityButtonRole
import com.verity.core.ui.primitives.VerityButtonState
import com.verity.core.ui.primitives.VerityDivider
import com.verity.core.ui.primitives.VerityDividerStrength
import com.verity.core.ui.primitives.VerityTextField
import com.verity.core.ui.primitives.VerityTextFieldRole
import com.verity.core.ui.primitives.VeritySpace
import com.verity.core.ui.primitives.VeritySpacer
import com.verity.core.ui.primitives.VeritySurface
import com.verity.core.ui.primitives.VeritySurfaceType
import com.verity.core.ui.primitives.VerityText
import com.verity.core.ui.primitives.VerityTextStyle
import com.verity.core.ui.primitives.dp

/**
 * ManageReferenceListRoute
 *
 * Re-checks the list each time this destination becomes current — same rationale as
 * Documents/Customers re-querying on re-entry.
 */
@Composable
fun ManageReferenceListRoute(viewModel: ManageReferenceListViewModel) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    ManageReferenceListScreen(
        state = state,
        onNewValueInputChanged = viewModel::onNewValueInputChanged,
        onAdd = viewModel::onAdd,
        onDelete = viewModel::onDelete
    )
}

/**
 * ManageReferenceListScreen
 *
 * Pure renderer — never obtains a ViewModel, never collects a Flow directly (see CLAUDE.md's
 * Route/Screen/ViewModel ownership rule). A plain add-row-then-list utility screen built entirely
 * from existing design-system components (VerityTextField, VerityListItem, VeritySurface Card) —
 * deliberately not put through its own mockup pass, unlike Settings/Customers, since it introduces
 * no new visual language; worth a dedicated design pass later if it needs one.
 */
@Composable
fun ManageReferenceListScreen(
    state: ManageReferenceListUiState,
    onNewValueInputChanged: (String) -> Unit,
    onAdd: () -> Unit,
    onDelete: (String) -> Unit,
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
            Row(verticalAlignment = Alignment.Top) {
                VerityTextField(
                    role = VerityTextFieldRole.Basic,
                    label = "Add new value",
                    value = state.newValueInput,
                    onValueChange = onNewValueInputChanged,
                    editing = true,
                    onEnterEdit = null,
                    onExitEdit = null,
                    suggestions = emptyList(),
                    onSelectSuggestion = null,
                    modifier = Modifier.weight(1f)
                )
                VeritySpacer(size = VeritySpace.Small, horizontal = true)
                VerityButton(
                    label = "Add",
                    role = VerityButtonRole.Primary,
                    state = if (state.newValueInput.isBlank()) VerityButtonState.Disabled else VerityButtonState.Enabled,
                    onClick = onAdd
                )
            }

            VeritySpacer(size = VeritySpace.Large)

            when {
                state.items.isEmpty() && !state.isLoading -> EmptyReferenceList()

                state.items.isNotEmpty() -> {
                    VeritySurface(type = VeritySurfaceType.Card) {
                        Column {
                            state.items.forEachIndexed { index, item ->
                                ReferenceListRow(item = item, onDelete = { onDelete(item.id) })
                                if (index != state.items.lastIndex) {
                                    VerityDivider(strength = VerityDividerStrength.Divider)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReferenceListRow(item: ReferenceListItem, onDelete: () -> Unit) {
    VerityListItem(
        title = item.value,
        trailing = {
            VerityText(
                text = "Remove",
                style = VerityTextStyle.Caption,
                modifier = Modifier.clickable(onClick = onDelete)
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 14.dp)
    )
}

@Composable
private fun EmptyReferenceList() {
    VeritySurface(type = VeritySurfaceType.Card) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(VeritySpace.Large.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            VerityText(text = "Nothing added yet", style = VerityTextStyle.Title)
            VeritySpacer(size = VeritySpace.Small)
            VerityText(
                text = "Values you add here show up as suggestions on invoices and challans.",
                style = VerityTextStyle.Caption
            )
        }
    }
}
