package com.verity.feature.customer.detail

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
import com.verity.core.ui.primitives.VerityButton
import com.verity.core.ui.primitives.VerityButtonRole
import com.verity.core.ui.primitives.VerityButtonState
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
 * CustomerDetailRoute
 *
 * Re-refreshes on re-entry (unlike the old read-only rollup) since this screen now has an edit
 * path (the chrome's pencil action, wired in AppNavShell) that can change the very data shown
 * here — same rationale as Documents/Home re-querying when the user backs out of a mutation.
 */
@Composable
fun CustomerDetailRoute(
    viewModel: CustomerDetailViewModel,
    onDocumentClick: (String) -> Unit,
    onNewInvoice: (CustomerDetail) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    CustomerDetailScreen(
        state = state,
        onDocumentClick = onDocumentClick,
        onNewInvoice = onNewInvoice
    )
}

/**
 * CustomerDetailScreen
 *
 * Pure renderer — never obtains a ViewModel, never collects a Flow directly (see CLAUDE.md's
 * Route/Screen/ViewModel ownership rule). Profile header, Balance Due hero, New Invoice/Record
 * Payment quick actions (Record Payment stays disabled — no Payment entity exists yet, see
 * CLAUDE.md's Future Concepts), and the document list, reusing DocumentSummaryRow exactly as the
 * Documents tab does.
 */
@Composable
fun CustomerDetailScreen(
    state: CustomerDetailUiState,
    onDocumentClick: (String) -> Unit,
    onNewInvoice: (CustomerDetail) -> Unit,
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
            val detail = state.detail

            when {
                !state.isLoading && detail == null -> CustomerNotFound()

                detail != null -> {
                    ProfileHeader(detail)

                    if (!detail.notes.isNullOrBlank()) {
                        VeritySpacer(size = VeritySpace.Small)
                        NotesSection(detail.notes)
                    }

                    VeritySpacer(size = VeritySpace.Medium)

                    BalanceDueHero(detail)
                    VeritySpacer(size = VeritySpace.Medium)

                    QuickActionsRow(onNewInvoice = { onNewInvoice(detail) })
                    VeritySpacer(size = VeritySpace.Large)

                    VerityText(text = "Documents", style = VerityTextStyle.Label)
                    VeritySpacer(size = VeritySpace.Small)

                    if (detail.documents.isEmpty()) {
                        NoDocumentsYet()
                    } else {
                        VeritySurface(type = VeritySurfaceType.Card) {
                            Column {
                                detail.documents.forEachIndexed { index, document ->
                                    DocumentSummaryRow(
                                        document = document,
                                        onClick = { onDocumentClick(document.documentId) }
                                    )
                                    if (index != detail.documents.lastIndex) {
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
private fun ProfileHeader(detail: CustomerDetail) {
    Column {
        VerityText(text = detail.customerName, style = VerityTextStyle.Title)
        VeritySpacer(size = VeritySpace.ExtraSmall)
        VerityText(
            text = "${detail.gstin} · ${detail.city}, ${detail.state}",
            style = VerityTextStyle.Caption
        )
    }
}

@Composable
private fun NotesSection(notes: String) {
    VeritySurface(type = VeritySurfaceType.Card, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(VeritySpace.Medium.dp)) {
            VerityText(text = "Notes", style = VerityTextStyle.Label)
            VeritySpacer(size = VeritySpace.ExtraSmall)
            VerityText(text = notes, style = VerityTextStyle.Body)
        }
    }
}

@Composable
private fun BalanceDueHero(detail: CustomerDetail) {
    VeritySurface(type = VeritySurfaceType.AssistInteractive, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(VeritySpace.Medium.dp)) {
            VerityText(text = "Balance Due", style = VerityTextStyle.Label)
            VeritySpacer(size = VeritySpace.ExtraSmall)
            VerityText(text = detail.balanceDue.format(), style = VerityTextStyle.Display)
            VeritySpacer(size = VeritySpace.ExtraSmall)
            val documentWord = if (detail.documentCount == 1) "document" else "documents"
            VerityText(
                text = "Across ${detail.documentCount} $documentWord",
                style = VerityTextStyle.Caption
            )
        }
    }
}

@Composable
private fun QuickActionsRow(onNewInvoice: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()) {
        VerityButton(
            label = "New Invoice",
            onClick = onNewInvoice,
            role = VerityButtonRole.Secondary,
            modifier = Modifier.weight(1f)
        )
        VeritySpacer(size = VeritySpace.Small, horizontal = true)
        VerityButton(
            label = "Record Payment",
            onClick = {},
            role = VerityButtonRole.Secondary,
            state = VerityButtonState.Disabled,
            modifier = Modifier.weight(1f)
        )
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
