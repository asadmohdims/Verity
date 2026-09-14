package com.verity.feature.document

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verity.core.document.model.DocumentType
import com.verity.core.theme.VerityTheme
import com.verity.core.ui.molecules.VerityListItem
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

@Composable
fun DocumentSummaryRow(document: DocumentSummary, onClick: () -> Unit) {
    val typeLabel = when (document.documentType) {
        DocumentType.INVOICE -> "Invoice"
        DocumentType.CHALLAN -> "Challan"
    }

    VerityListItem(
        leading = { CustomerAvatar(customerName = document.customerName) },
        title = "${document.documentNumber} · ${document.customerName}",
        titleMaxLines = 1,
        titleOverflow = TextOverflow.Ellipsis,
        subtitle = "${document.issueDate.format(summaryDateFormatter)} · $typeLabel",
        trailing = {
            VerityText(text = document.grandTotal.format(), style = VerityTextStyle.Body)
        },
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp)
    )
}

/**
 * Small circular initials avatar, matching the approved Design Blueprint's Home mockup.
 *
 * Uses raw Material3 Text rather than VerityText: this is the one place a two-letter initial
 * needs to sit on a brand-tinted circle in VerityTheme.colors.primary specifically, at the
 * mockup's 13sp/Bold weight, which none of VerityTextStyle's fixed style→color mappings produce.
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
            style = VerityTheme.typography.label.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
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
