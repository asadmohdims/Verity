package com.verity.feature.invoice.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.verity.core.document.model.DocumentType
import com.verity.core.document.model.InvoiceDocumentModel
import com.verity.core.formatting.money.Money
import com.verity.core.theme.VerityTheme
import com.verity.core.ui.icons.VerityIconGlyph
import com.verity.core.ui.icons.VerityIcons
import com.verity.core.ui.primitives.VerityButton
import com.verity.core.ui.primitives.VerityButtonRole
import com.verity.core.ui.primitives.VeritySpace
import com.verity.core.ui.primitives.VeritySpacer
import com.verity.core.ui.primitives.VerityText
import com.verity.core.ui.primitives.VerityTextStyle
import com.verity.core.ui.primitives.dp

/**
 * Confirmation screen shown right after a document is finalized — matches Finalized.dc.html: a
 * checkmark, "Invoice Finalized"/"Challan Finalized" (per document type), the document number and
 * grand total, and a "View Document" CTA.
 * "View PDF" is an addition beyond the mockup: it opens the actual generated PDF file (via
 * InvoicePdfRenderer/PdfViewerScreen), distinct from "View Document"'s in-app Compose preview of
 * the same document.
 *
 * Deliberately has no sync/background-sync status indicator: the mockup shows one, but this app
 * has no real background sync (no WorkManager/Supabase outbox) built yet — adding it here would be
 * misleading placeholder UI implying functionality that doesn't exist (decided with the user
 * 2026-09-14; add it for real once background sync ships).
 */
@Composable
fun InvoiceFinalizedScreen(
    document: InvoiceDocumentModel,
    onViewDocument: () -> Unit,
    onViewPdf: () -> Unit,
    onContinueToJobWorkInvoice: (() -> Unit)? = null
) {
    // A job-work Challan (jobWorkLink present, on the Challan side of the pair) gets a
    // prominent way to continue straight into its pre-filled Invoice — but "View PDF" stays
    // available too, since the Challan's own PDF must physically travel with the goods and
    // Share/Print/Export isn't built yet (see CLAUDE.md's "Next up").
    val isJobWorkChallan = document.jobWorkLink != null && document.identity.documentType == DocumentType.CHALLAN
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(VeritySpace.ExtraLarge.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(VerityTheme.colors.surface.assistInteractive),
                contentAlignment = Alignment.Center
            ) {
                CompositionLocalProvider(LocalContentColor provides VerityTheme.colors.primary) {
                    VerityIconGlyph(
                        icon = VerityIcons.FinalizedCheck,
                        contentDescription = null,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            VeritySpacer(size = VeritySpace.Large)

            val documentNoun = when (document.identity.documentType) {
                DocumentType.INVOICE -> "Invoice"
                DocumentType.CHALLAN -> "Challan"
            }
            VerityText(text = "$documentNoun Finalized", style = VerityTextStyle.Title)

            VeritySpacer(size = VeritySpace.Small)

            VerityText(
                text = "${document.identity.documentNumber} · ${Money.ofPaise(document.totals.grandTotalPaise).format()}",
                style = VerityTextStyle.Label
            )

            VeritySpacer(size = VeritySpace.ExtraLarge)

            if (isJobWorkChallan && onContinueToJobWorkInvoice != null) {
                VerityButton(
                    label = "Continue to Invoice",
                    onClick = onContinueToJobWorkInvoice,
                    role = VerityButtonRole.Primary
                )
                VeritySpacer(size = VeritySpace.Small)
            }

            VerityButton(
                label = "View Document",
                onClick = onViewDocument,
                role = if (isJobWorkChallan) VerityButtonRole.Secondary else VerityButtonRole.Primary
            )

            VeritySpacer(size = VeritySpace.Small)

            VerityButton(
                label = "View PDF",
                onClick = onViewPdf,
                role = VerityButtonRole.Secondary
            )
        }
    }
}
