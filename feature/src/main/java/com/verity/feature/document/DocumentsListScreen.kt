package com.verity.feature.document

import androidx.compose.foundation.layout.Column
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
import com.verity.core.ui.molecules.VeritySegmentedControl
import com.verity.core.ui.primitives.VeritySpace
import com.verity.core.ui.primitives.VeritySpacer
import com.verity.core.ui.primitives.VeritySurface
import com.verity.core.ui.primitives.VeritySurfaceType
import com.verity.core.ui.primitives.VerityText
import com.verity.core.ui.primitives.VerityTextStyle
import com.verity.core.ui.primitives.VerityDivider
import com.verity.core.ui.primitives.VerityDividerStrength
import com.verity.core.ui.primitives.dp

/**
 * DocumentsListRoute
 *
 * Obtains the DocumentsListViewModel's state and re-triggers a refresh each time this destination
 * becomes current, same rationale as HomeRoute — a document finalized on another tab must show up
 * here without a fresh process/ViewModel.
 */
@Composable
fun DocumentsListRoute(
    viewModel: DocumentsListViewModel,
    onDocumentClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    DocumentsListScreen(
        state = state,
        onFilterChanged = viewModel::onFilterChanged,
        onDocumentClick = onDocumentClick,
        onDocumentLongPress = viewModel::onDocumentLongPress,
        onDocumentToggleSelect = viewModel::onToggleSelection
    )
}

/**
 * DocumentsListScreen
 *
 * Pure renderer — never obtains a ViewModel, never collects a Flow directly (see CLAUDE.md's
 * Route/Screen/ViewModel ownership rule). All/Invoices/Challans filtering reuses
 * VeritySegmentedControl (the same "small fixed option set" primitive Settings' theme picker
 * already uses) rather than a new chip-row component — no new interactive primitive needed for
 * an N-way picker that already exists.
 */
@Composable
fun DocumentsListScreen(
    state: DocumentsListUiState,
    onFilterChanged: (DocumentTypeFilter) -> Unit,
    onDocumentClick: (String) -> Unit,
    onDocumentLongPress: (String) -> Unit = {},
    onDocumentToggleSelect: (String) -> Unit = {},
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
            if (state.documents.isNotEmpty()) {
                VeritySegmentedControl(
                    options = DocumentTypeFilter.entries,
                    selected = state.filter,
                    labelFor = ::filterLabel,
                    onSelect = onFilterChanged
                )
                VeritySpacer(size = VeritySpace.Medium)
            }

            when {
                state.documents.isEmpty() && !state.isLoading -> EmptyDocuments()

                state.visibleDocuments.isEmpty() -> EmptyFilterResult(state.filter)

                else -> {
                    VeritySurface(type = VeritySurfaceType.Card) {
                        Column {
                            state.visibleDocuments.forEachIndexed { index, document ->
                                DocumentSummaryRow(
                                    document = document,
                                    isLinked = document.documentId in state.linkedToDocumentIds,
                                    isSelectionMode = state.isSelectionMode,
                                    isSelected = document.documentId in state.selectedDocumentIds,
                                    onLongClick = { onDocumentLongPress(document.documentId) },
                                    onClick = {
                                        if (state.isSelectionMode) {
                                            onDocumentToggleSelect(document.documentId)
                                        } else {
                                            onDocumentClick(document.documentId)
                                        }
                                    }
                                )

                                if (index != state.visibleDocuments.lastIndex) {
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

private fun filterLabel(filter: DocumentTypeFilter): String = when (filter) {
    DocumentTypeFilter.ALL -> "All"
    DocumentTypeFilter.INVOICES -> "Invoices"
    DocumentTypeFilter.CHALLANS -> "Challans"
}

@Composable
private fun EmptyDocuments() {
    VeritySurface(type = VeritySurfaceType.Card) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(VeritySpace.Large.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            VerityText(
                text = "No documents yet",
                style = VerityTextStyle.Title
            )
            VeritySpacer(size = VeritySpace.Small)
            VerityText(
                text = "Invoices and challans you finalize will show up here.",
                style = VerityTextStyle.Caption
            )
        }
    }
}

@Composable
private fun EmptyFilterResult(filter: DocumentTypeFilter) {
    val message = when (filter) {
        DocumentTypeFilter.ALL -> "No documents yet"
        DocumentTypeFilter.INVOICES -> "No invoices yet"
        DocumentTypeFilter.CHALLANS -> "No challans yet"
    }
    VeritySurface(type = VeritySurfaceType.Card) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(VeritySpace.Large.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            VerityText(text = message, style = VerityTextStyle.Title)
        }
    }
}
