package com.verity.feature.document

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.verity.core.document.model.DocumentType
import com.verity.core.ui.molecules.VerityCustomerAvatar
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
        leading = { VerityCustomerAvatar(customerName = document.customerName) },
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
