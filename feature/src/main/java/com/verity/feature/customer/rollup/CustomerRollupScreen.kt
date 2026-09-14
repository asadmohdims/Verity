package com.verity.feature.customer.rollup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.verity.core.ui.primitives.VerityDivider
import com.verity.core.ui.primitives.VerityDividerStrength
import com.verity.core.ui.primitives.VeritySpace
import com.verity.core.ui.primitives.VeritySpacer
import com.verity.core.ui.primitives.VeritySurface
import com.verity.core.ui.primitives.VeritySurfaceType
import com.verity.core.ui.primitives.VerityText
import com.verity.core.ui.primitives.VerityTextStyle
import com.verity.core.ui.primitives.dp
import com.verity.feature.document.DocumentSummaryRow

/**
 * CustomerRollupRoute
 *
 * Obtains CustomerRollupViewModel's state — no refresh-on-return like Documents/Home, since a
 * customer's document history can't change from another tab the way "did I just finalize
 * something" can.
 */
@Composable
fun CustomerRollupRoute(
    viewModel: CustomerRollupViewModel,
    onDocumentClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    CustomerRollupScreen(
        state = state,
        onDocumentClick = onDocumentClick
    )
}

/**
 * CustomerRollupScreen
 *
 * Minimal, read-only: name, document count, running total, and the document list — reusing
 * DocumentSummaryRow exactly as the Documents tab does. No edit/deactivate/profile fields; that's
 * the full Customer Detail screen's job (Phase 2 of the UX roadmap, not built yet).
 */
@Composable
fun CustomerRollupScreen(
    state: CustomerRollupUiState,
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
            val rollup = state.rollup

            when {
                !state.isLoading && rollup == null -> CustomerNotFound()

                rollup != null -> {
                    RollupHeader(rollup)
                    VeritySpacer(size = VeritySpace.Medium)

                    if (rollup.documents.isEmpty()) {
                        NoDocumentsYet()
                    } else {
                        VeritySurface(type = VeritySurfaceType.Card) {
                            Column {
                                rollup.documents.forEachIndexed { index, document ->
                                    DocumentSummaryRow(
                                        document = document,
                                        onClick = { onDocumentClick(document.documentId) }
                                    )
                                    if (index != rollup.documents.lastIndex) {
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
}

@Composable
private fun RollupHeader(rollup: CustomerRollup) {
    VeritySurface(type = VeritySurfaceType.Card) {
        Column(modifier = Modifier.padding(VeritySpace.Medium.dp)) {
            VerityText(text = rollup.customerName, style = VerityTextStyle.Title)
            VeritySpacer(size = VeritySpace.ExtraSmall)
            val documentWord = if (rollup.documentCount == 1) "document" else "documents"
            VerityText(
                text = "${rollup.documentCount} $documentWord · ${rollup.runningTotal.format()}",
                style = VerityTextStyle.Caption
            )
        }
    }
}

@Composable
private fun NoDocumentsYet() {
    VeritySurface(type = VeritySurfaceType.Card) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(VeritySpace.Large.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            VerityText(text = "No documents yet for this customer.", style = VerityTextStyle.Caption)
        }
    }
}

@Composable
private fun CustomerNotFound() {
    VeritySurface(type = VeritySurfaceType.Card) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(VeritySpace.Large.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            VerityText(text = "Customer not found", style = VerityTextStyle.Body)
        }
    }
}
