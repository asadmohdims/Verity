package com.verity.feature.document.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.verity.core.theme.VerityTheme
import com.verity.core.ui.molecules.VerityListItem
import com.verity.core.ui.molecules.VeritySection
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
import com.verity.feature.document.DocumentSummaryRow
import com.verity.feature.home.DocumentSummary

/**
 * DocumentSearchRoute
 *
 * Obtains DocumentSearchViewModel's state — see AppNavShell for how this route is reached (a
 * search action icon on the Documents tab's chrome) and how customer/document taps navigate on.
 */
@Composable
fun DocumentSearchRoute(
    viewModel: DocumentSearchViewModel,
    onDocumentClick: (String) -> Unit,
    onCustomerClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    DocumentSearchScreen(
        state = state,
        onQueryChanged = viewModel::onQueryChanged,
        onDocumentClick = onDocumentClick,
        onCustomerClick = onCustomerClick
    )
}

/**
 * DocumentSearchScreen
 *
 * Empty query -> "Recent" (the same finalizedAt-sorted list Home/Documents tab already show).
 * Non-empty query -> a "Customers" group (0-2 matches, tap opens the customer rollup) above a
 * "Documents" group, ranked by DocumentSearchRanking, each row showing a highlighted snippet when
 * the match came from inside the document body rather than its number/customer.
 */
@Composable
fun DocumentSearchScreen(
    state: DocumentSearchUiState,
    onQueryChanged: (String) -> Unit,
    onDocumentClick: (String) -> Unit,
    onCustomerClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    VeritySurface(
        type = VeritySurfaceType.Base,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(VeritySpace.Medium.dp)
        ) {
            VerityTextField(
                role = VerityTextFieldRole.Basic,
                label = "Search",
                placeholder = "Customer, item, vehicle number, amount…",
                value = state.query,
                onValueChange = onQueryChanged,
                editing = true,
                onEnterEdit = null,
                onExitEdit = null,
                suggestions = emptyList(),
                onSelectSuggestion = null
            )

            VeritySpacer(size = VeritySpace.Medium)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                if (state.query.isBlank()) {
                    RecentSection(documents = state.recentDocuments, onDocumentClick = onDocumentClick)
                } else {
                    SearchResultsSection(
                        state = state,
                        onDocumentClick = onDocumentClick,
                        onCustomerClick = onCustomerClick
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentSection(documents: List<DocumentSummary>, onDocumentClick: (String) -> Unit) {
    if (documents.isEmpty()) return

    VeritySection(title = "Recent", surfaceType = VeritySurfaceType.Card) {
        Column {
            documents.forEachIndexed { index, document ->
                DocumentSummaryRow(document = document, onClick = { onDocumentClick(document.documentId) })
                if (index != documents.lastIndex) {
                    VerityDivider(strength = VerityDividerStrength.Divider)
                }
            }
        }
    }
}

@Composable
private fun SearchResultsSection(
    state: DocumentSearchUiState,
    onDocumentClick: (String) -> Unit,
    onCustomerClick: (String) -> Unit
) {
    val hasCustomers = state.results.customers.isNotEmpty()
    val hasDocuments = state.results.documents.isNotEmpty()

    if (!hasCustomers && !hasDocuments) {
        if (!state.isSearching) {
            NoResults(query = state.query)
        }
        return
    }

    if (hasCustomers) {
        VeritySection(title = "Customers", surfaceType = VeritySurfaceType.Card) {
            Column {
                state.results.customers.forEachIndexed { index, customer ->
                    VerityListItem(
                        title = customer.customerName,
                        subtitle = customer.gstin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = { onCustomerClick(customer.customerId) })
                            .padding(horizontal = 8.dp, vertical = 12.dp)
                    )
                    if (index != state.results.customers.lastIndex) {
                        VerityDivider(strength = VerityDividerStrength.Divider)
                    }
                }
            }
        }
        VeritySpacer(size = VeritySpace.Medium)
    }

    if (hasDocuments) {
        VeritySection(title = "Documents", surfaceType = VeritySurfaceType.Card) {
            Column {
                state.results.documents.forEachIndexed { index, result ->
                    DocumentSearchResultRow(result = result, onClick = { onDocumentClick(result.documentId) })
                    if (index != state.results.documents.lastIndex) {
                        VerityDivider(strength = VerityDividerStrength.Divider)
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentSearchResultRow(result: DocumentSearchResult, onClick: () -> Unit) {
    Column {
        DocumentSummaryRow(document = result.toDisplaySummary(), onClick = onClick)

        result.matchedSnippet?.let { snippet ->
            HighlightedSnippet(
                snippet = snippet,
                modifier = Modifier.padding(start = 56.dp, end = 8.dp, bottom = 4.dp)
            )
        }
    }
}

/** finalizedAtEpochMillis is unused here (this row never sorts) — 0L is a display-only stand-in. */
private fun DocumentSearchResult.toDisplaySummary(): DocumentSummary = DocumentSummary(
    documentId = documentId,
    documentNumber = documentNumber,
    customerName = customerName,
    documentType = documentType,
    issueDate = issueDate,
    grandTotal = grandTotal,
    finalizedAtEpochMillis = 0L
)

/**
 * Raw Material3 Text, not VerityText: per-substring bold+accent highlighting is one of the things
 * VerityText's design deliberately doesn't expose (see its own doc comment) — the same narrow
 * exception DocumentSummaryRow's CustomerAvatar already takes, for a different reason.
 */
@Composable
private fun HighlightedSnippet(snippet: SnippetMatch, modifier: Modifier = Modifier) {
    val mutedColor = VerityTheme.colors.text.muted
    val accentColor = VerityTheme.colors.primary

    val annotated = remember(snippet, mutedColor, accentColor) {
        val start = snippet.matchRange.first.coerceIn(0, snippet.text.length)
        val end = (snippet.matchRange.last + 1).coerceIn(start, snippet.text.length)

        buildAnnotatedString {
            append(snippet.text)
            addStyle(SpanStyle(color = mutedColor), 0, snippet.text.length)
            addStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold), start, end)
        }
    }

    Text(
        text = annotated,
        style = VerityTheme.typography.caption,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
private fun NoResults(query: String) {
    VeritySurface(type = VeritySurfaceType.Card) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(VeritySpace.Large.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            VerityText(text = "No documents match \"$query\"", style = VerityTextStyle.Body)
        }
    }
}
