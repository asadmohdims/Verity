package com.verity.feature.document

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 */
@Composable
fun DocumentSummaryRow(document: DocumentSummary, isLinked: Boolean = false, onClick: () -> Unit) {
    val typeLabel = when (document.documentType) {
        DocumentType.INVOICE -> "INVOICE"
        DocumentType.CHALLAN -> "CHALLAN"
    }

    VerityListItem(
        leading = { VerityCustomerAvatar(customerName = document.customerName) },
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
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp)
    )
}
