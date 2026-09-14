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
        onDocumentClick = onDocumentClick
    )
}

/**
 * DocumentsListScreen
 *
 * Pure renderer — never obtains a ViewModel, never collects a Flow directly (see CLAUDE.md's
 * Route/Screen/ViewModel ownership rule). Every finalized document, newest first — no filtering
 * or search yet (out of scope for this pass).
 */
@Composable
fun DocumentsListScreen(
    state: DocumentsListUiState,
    onDocumentClick: (String) -> Unit,
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
            when {
                state.documents.isEmpty() && !state.isLoading -> EmptyDocuments()

                state.documents.isNotEmpty() -> {
                    VeritySurface(type = VeritySurfaceType.Card) {
                        Column {
                            state.documents.forEachIndexed { index, document ->
                                DocumentSummaryRow(
                                    document = document,
                                    onClick = { onDocumentClick(document.documentId) }
                                )

                                if (index != state.documents.lastIndex) {
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
