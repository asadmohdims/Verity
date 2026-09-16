package com.verity.feature.document

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.verity.core.document.model.DocumentType
import com.verity.core.theme.VerityTheme
import com.verity.core.ui.molecules.VerityCustomerAvatar
import com.verity.core.ui.molecules.VerityListItem
import com.verity.core.ui.primitives.VerityBadge
import com.verity.core.ui.primitives.VerityText
import com.verity.core.ui.primitives.VerityTextStyle
import com.verity.feature.home.DocumentSummary
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * DocumentSummaryRow
 *
 * One row rendering of a finalized document (number, customer, date, type, total), shared
 * between Home's Recent Documents list and the Documents tab's full list — both show the exact
 * same projection (DocumentSummary), just different-length lists.
 */
private val summaryDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

/**
 * [isLinked]: true only for a Challan whose reserved job-work Invoice was actually finalized (see
 * DocumentsListViewModel.linkedToDocumentIds) — always false for an Invoice, and false for a
 * standalone Challan with no continuation Invoice.
 *
 * [isSelectionMode]/[isSelected]/[onLongClick] back the Documents tab's multi-select-for-bulk-
 * share flow (see DocumentsListViewModel) — defaulted off/null so the other three call sites
 * (Home, Document Search, Customer Detail) render exactly as before.
 */
@Composable
fun DocumentSummaryRow(
    document: DocumentSummary,
    isLinked: Boolean = false,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val typeLabel = when (document.documentType) {
        DocumentType.INVOICE -> "INVOICE"
        DocumentType.CHALLAN -> "CHALLAN"
    }

    VerityListItem(
        leading = {
            if (isSelectionMode) {
                // Display-only — the row's own click (below) does the toggling, same as tapping
                // anywhere else on a Gmail/Photos row while selecting. onCheckedChange = null
                // means Compose's own toggleable semantics never get attached (there'd be no
                // click to run), so the checked state is announced explicitly here instead — both
                // for screen readers and so this state is assertable in tests.
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = null,
                    modifier = Modifier.semantics {
                        contentDescription = if (isSelected) "Selected" else "Not selected"
                    }
                )
            } else {
                VerityCustomerAvatar(customerName = document.customerName)
            }
        },
        title = "${document.documentNumber} · ${document.customerName}",
        titleMaxLines = 1,
        titleOverflow = TextOverflow.Ellipsis,
        subtitle = document.issueDate.format(summaryDateFormatter),
        trailing = {
            Column(horizontalAlignment = Alignment.End) {
                VerityText(text = document.grandTotal.format(), style = VerityTextStyle.Body)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    if (isLinked) {
                        VerityBadge(
                            label = "LINKED",
                            containerColor = VerityTheme.colors.surface.raised,
                            contentColor = VerityTheme.colors.text.muted
                        )
                    }
                    // Invoice = primary-tinted, Challan = neutral/muted — deliberately not accent:
                    // CLAUDE.md reserves accent for CTA emphasis only, never a type/state
                    // indicator. See DocumentSummaryRow's own doc comment for the fuller reasoning.
                    when (document.documentType) {
                        DocumentType.INVOICE -> VerityBadge(
                            label = typeLabel,
                            containerColor = VerityTheme.colors.surface.assistInteractive,
                            contentColor = VerityTheme.colors.primary
                        )
                        DocumentType.CHALLAN -> VerityBadge(
                            label = typeLabel,
                            containerColor = VerityTheme.colors.surface.assist,
                            contentColor = VerityTheme.colors.text.secondary
                        )
                    }
                }
            }
        },
        modifier = Modifier
            .background(if (isSelected) VerityTheme.colors.surface.assist else VerityTheme.colors.surface.base)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 8.dp, vertical = 12.dp)
    )
}
