package com.verity.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.verity.core.theme.VerityTheme
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
import com.verity.feature.document.DocumentSummaryRow
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
    onSeeAllDocuments: () -> Unit,
    onDocumentClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    HomeScreen(
        state = state,
        onCreateNew = onCreateNew,
        onSeeAllDocuments = onSeeAllDocuments,
        onDocumentClick = onDocumentClick
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
            ThisMonthCard(
                total = state.thisMonthTotal.format(),
                documentCount = state.thisMonthDocumentCount,
                monthLabel = state.thisMonthLabel
            )

            VeritySpacer(size = VeritySpace.Large)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle(text = "Recent Documents")

                if (state.recentDocuments.isNotEmpty()) {
                    SectionLink(
                        text = "See all",
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
                    // White card, matching the mockup's
                    // `.listcard{background:var(--surface-base)}` — Raised's mint tint doesn't match.
                    VeritySurface(type = VeritySurfaceType.Card) {
                        Column {
                            state.recentDocuments.forEachIndexed { index, document ->
                                DocumentSummaryRow(
                                    document = document,
                                    onClick = { onDocumentClick(document.documentId) }
                                )

                                if (index != state.recentDocuments.lastIndex) {
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
private fun ThisMonthCard(total: String, documentCount: Int, monthLabel: String) {
    VeritySurface(
        type = VeritySurfaceType.AssistInteractive,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(VeritySpace.Medium.dp)) {
            OverlineLabel(text = "This Month")
            Spacer(modifier = Modifier.height(6.dp))
            VerityText(text = total, style = VerityTextStyle.Display)
            Spacer(modifier = Modifier.height(6.dp))
            VerityText(
                text = buildString {
                    append(if (documentCount == 1) "1 document invoiced" else "$documentCount documents invoiced")
                    if (monthLabel.isNotEmpty()) {
                        append(" · ")
                        append(monthLabel)
                    }
                },
                style = VerityTextStyle.Caption
            )
        }
    }
}

/**
 * Small-caps overline label ("THIS MONTH") matching the approved mockup's `.hero__label`. Reuses
 * Caption's size/weight/letter-spacing (already an exact match), but Caption always renders in
 * `text.muted` via VerityText — this label needs `text.secondary` plus an uppercase transform, so
 * it goes through raw Material3 Text instead, same rationale as CustomerAvatar below.
 */
@Composable
private fun OverlineLabel(text: String) {
    Text(
        text = text.uppercase(Locale.ENGLISH),
        style = VerityTheme.typography.caption,
        color = VerityTheme.colors.text.secondary
    )
}

/**
 * "Recent Documents" section heading — 14sp SemiBold in `text.primary`, matching the mockup's
 * `.sectiontitle`. Smaller than the shared `Title` token (18sp), which is used more broadly across
 * the app; kept local to Home rather than resizing that token app-wide.
 */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = VerityTheme.typography.label,
        color = VerityTheme.colors.text.primary
    )
}

/**
 * "See all" section-header link — 12sp SemiBold in `colors.primary`, matching the mockup's
 * `.sectiontitle .link`. Neither Label (14sp/secondary) nor Caption (12sp/muted) matches this
 * combination.
 */
@Composable
private fun SectionLink(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = VerityTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
        color = VerityTheme.colors.primary
    )
}

@Composable
private fun EmptyRecentDocuments(onCreateNew: () -> Unit) {
    // Same white Card treatment as the populated list below it — no design reference shows this
    // state, but the empty/populated card should look like the same card either way.
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
            VeritySpacer(size = VeritySpace.Medium)
            VerityButton(
                label = "Create your first invoice",
                onClick = onCreateNew
            )
        }
    }
}
