package com.verity.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.verity.core.document.model.DocumentType
import com.verity.core.theme.VerityTheme
import com.verity.core.ui.molecules.VerityListItem
import com.verity.core.ui.primitives.VerityButton
import com.verity.core.ui.primitives.VerityDivider
import com.verity.core.ui.primitives.VerityDividerStrength
import com.verity.core.ui.primitives.VeritySpace
import com.verity.core.ui.primitives.VeritySpacer
import com.verity.core.ui.primitives.VeritySurface
import com.verity.core.ui.primitives.VeritySurfaceType
import com.verity.core.ui.primitives.VerityText
import com.verity.core.ui.primitives.VerityTextStyle
import com.verity.core.ui.primitives.dp
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * HomeRoute
 *
 * Obtains the HomeViewModel's state and re-triggers a refresh each time this destination becomes
 * current — the NavHost does not save/restore this route's state (see AppNavShell), so a plain
 * LaunchedEffect(Unit) re-runs on every re-entry, keeping "Recent Documents" honest after the
 * user finalizes something on another tab and comes back.
 */
@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    onCreateNew: () -> Unit,
    onSeeAllDocuments: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    HomeScreen(
        state = state,
        onCreateNew = onCreateNew,
        onSeeAllDocuments = onSeeAllDocuments
    )
}

/**
 * HomeScreen
 *
 * Pure renderer — never obtains a ViewModel, never collects a Flow directly (see CLAUDE.md's
 * Route/Screen/ViewModel ownership rule).
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onCreateNew: () -> Unit,
    onSeeAllDocuments: () -> Unit,
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
            ThisMonthCard(
                total = state.thisMonthTotal.format(),
                documentCount = state.thisMonthDocumentCount
            )

            VeritySpacer(size = VeritySpace.Large)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                VerityText(text = "Recent Documents", style = VerityTextStyle.Title)

                if (state.recentDocuments.isNotEmpty()) {
                    VerityText(
                        text = "See all",
                        style = VerityTextStyle.Label,
                        modifier = Modifier.clickable(onClick = onSeeAllDocuments)
                    )
                }
            }

            VeritySpacer(size = VeritySpace.Small)

            when {
                state.recentDocuments.isEmpty() && !state.isLoading -> {
                    EmptyRecentDocuments(onCreateNew = onCreateNew)
                }

                state.recentDocuments.isNotEmpty() -> {
                    VeritySurface(type = VeritySurfaceType.Raised) {
                        Column(modifier = Modifier.padding(VeritySpace.Medium.dp)) {
                            state.recentDocuments.forEachIndexed { index, document ->
                                RecentDocumentRow(document)

                                if (index != state.recentDocuments.lastIndex) {
                                    VeritySpacer(size = VeritySpace.Small)
                                    VerityDivider(strength = VerityDividerStrength.Subtle)
                                    VeritySpacer(size = VeritySpace.Small)
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
private fun ThisMonthCard(total: String, documentCount: Int) {
    VeritySurface(
        type = VeritySurfaceType.AssistInteractive,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(VeritySpace.Medium.dp)) {
            VerityText(text = "This Month", style = VerityTextStyle.Label)
            VeritySpacer(size = VeritySpace.ExtraSmall)
            VerityText(text = total, style = VerityTextStyle.Display)
            VeritySpacer(size = VeritySpace.ExtraSmall)
            VerityText(
                text = if (documentCount == 1) {
                    "1 document invoiced"
                } else {
                    "$documentCount documents invoiced"
                },
                style = VerityTextStyle.Caption
            )
        }
    }
}

private val recentDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

@Composable
private fun RecentDocumentRow(document: DocumentSummary) {
    val typeLabel = when (document.documentType) {
        DocumentType.INVOICE -> "Invoice"
        DocumentType.CHALLAN -> "Challan"
    }

    VerityListItem(
        leading = { CustomerAvatar(customerName = document.customerName) },
        title = "${document.documentNumber} · ${document.customerName}",
        titleMaxLines = 1,
        titleOverflow = TextOverflow.Ellipsis,
        subtitle = "${document.issueDate.format(recentDateFormatter)} · $typeLabel",
        trailing = {
            VerityText(text = document.grandTotal.format(), style = VerityTextStyle.Body)
        }
    )
}

/**
 * Small circular initials avatar, matching the approved Design Blueprint's Home mockup.
 *
 * Uses raw Material3 Text rather than VerityText: this is the one place a two-letter initial
 * needs to sit on a brand-tinted circle in VerityTheme.colors.primary specifically, which none of
 * VerityTextStyle's fixed style→color mappings produce.
 */
@Composable
private fun CustomerAvatar(customerName: String) {
    val initials = remember(customerName) { initialsFor(customerName) }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(VerityTheme.colors.surface.assistInteractive),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = VerityTheme.typography.label,
            color = VerityTheme.colors.primary
        )
    }
}

private fun initialsFor(customerName: String): String {
    val words = customerName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        words.isEmpty() -> "?"
        words.size == 1 -> words.first().take(2).uppercase(Locale.ENGLISH)
        else -> (words[0].take(1) + words[1].take(1)).uppercase(Locale.ENGLISH)
    }
}

@Composable
private fun EmptyRecentDocuments(onCreateNew: () -> Unit) {
    VeritySurface(type = VeritySurfaceType.Raised) {
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
            VeritySpacer(size = VeritySpace.Medium)
            VerityButton(
                label = "Create your first invoice",
                onClick = onCreateNew
            )
        }
    }
}
