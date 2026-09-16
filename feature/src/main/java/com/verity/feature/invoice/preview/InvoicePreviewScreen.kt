package com.verity.feature.invoice.preview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.verity.core.ui.primitives.dp
import androidx.compose.ui.unit.dp
import com.verity.core.document.model.DocumentTaxMode
import com.verity.core.document.model.DocumentType
import com.verity.core.document.model.displayLabel
import com.verity.core.document.model.InvoiceDocumentModel
import com.verity.core.formatting.date.DocumentDate
import com.verity.core.formatting.money.Money
import com.verity.core.ui.molecules.VerityInvoiceLineItemRow
import com.verity.core.ui.molecules.VeritySection
import com.verity.core.ui.primitives.VerityButton
import com.verity.core.ui.primitives.VerityButtonRole
import com.verity.core.ui.primitives.VerityButtonState
import com.verity.core.ui.primitives.VerityDivider
import com.verity.core.ui.primitives.VerityDividerStrength
import com.verity.core.ui.primitives.VerityTextField
import com.verity.core.ui.primitives.VerityTextFieldRole
import com.verity.core.ui.primitives.VeritySpacer
import com.verity.core.ui.primitives.VeritySurface
import com.verity.core.ui.primitives.VeritySurfaceType
import com.verity.core.ui.primitives.VerityText
import com.verity.core.ui.primitives.VerityTextStyle
import com.verity.core.ui.primitives.VeritySpace
import androidx.compose.foundation.layout.Row

/**
 * Renders an InvoiceDocumentModel for either a draft preview or an already-finalized document —
 * the content is identical either way (both are just "here's what this document says"). Only
 * [onFinalize] distinguishes them: pass it (draft-preview context) to show a Finalize CTA, or
 * leave it null (viewing an already-finalized document) to hide it.
 *
 * Owns no chrome of its own (no Scaffold/top bar) — AppNavShell's VerityTopAppBar is the only top
 * bar; this screen previously rendered its own CenterAlignedTopAppBar too, producing two stacked
 * bars.
 */
@Composable
fun InvoicePreviewScreen(
    document: InvoiceDocumentModel,
    onBack: () -> Unit,
    onFinalize: (() -> Unit)? = null,
    isFinalizing: Boolean = false,
    onViewPdf: (() -> Unit)? = null,
    /** Non-null only once the linked document actually resolves to a real row — see
     *  DocumentDetailViewModel.linkedDocumentId. Null shows the reference as plain text instead
     *  of a tappable row (e.g. a job-work Challan whose reserved Invoice was never finalized). */
    onViewLinkedDocument: (() -> Unit)? = null,
    /**
     * Both non-null only on the reopened-from-Documents/Home/Customer-detail Document Detail
     * route — the "come back and review/add a note" surface (see AppNavShell). Null on the
     * pre-finalize Preview and the just-finalized in-memory routes, which hide the section
     * entirely rather than showing a note with nowhere to persist it yet. [selfNotes] is
     * DocumentEntity.selfNotes — deliberately not part of [document], since it's never printed.
     */
    selfNotes: String? = null,
    onSelfNotesChange: ((String) -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        VeritySpacer(size = VeritySpace.Large)

        // ─────────────────────────────────────────────
        // Document Header + Parties — one merged section, matching Preview.dc.html's single
        // `.section` (no separate divider between the doc number and Billed To/Shipped To).
        // ─────────────────────────────────────────────
        VeritySurface(
            type = VeritySurfaceType.Base,
            modifier = Modifier.padding(horizontal = VeritySpace.Small.dp)
        ) {
            VeritySection {
                VerityText(
                    text = document.identity.documentNumber,
                    style = VerityTextStyle.Display
                )

                VeritySpacer(size = VeritySpace.ExtraSmall)

                VerityText(
                    text = "Issue date · ${DocumentDate.format(document.identity.issueDate)}",
                    style = VerityTextStyle.Caption
                )

                document.inboundChallanReference?.let { reference ->
                    VeritySpacer(size = VeritySpace.ExtraSmall)
                    VerityText(
                        text = "Received vide Challan No. ${reference.challanNumber}" +
                            (reference.challanDate?.let { " dated ${DocumentDate.format(it)}" } ?: ""),
                        style = VerityTextStyle.Caption
                    )
                }

                document.jobWorkLink?.let { link ->
                    val label = when (document.identity.documentType) {
                        DocumentType.CHALLAN -> "Ref: Invoice No."
                        DocumentType.INVOICE -> "Ref: Challan No."
                    }
                    val referenceText = "$label ${link.linkedDocumentNumber} dated ${DocumentDate.format(link.linkedDocumentDate)}"
                    VeritySpacer(size = VeritySpace.ExtraSmall)
                    if (onViewLinkedDocument != null) {
                        VerityText(
                            text = "$referenceText ›",
                            style = VerityTextStyle.Caption,
                            modifier = Modifier.clickable(onClick = onViewLinkedDocument)
                        )
                    } else {
                        VerityText(text = referenceText, style = VerityTextStyle.Caption)
                    }
                }

                VeritySpacer(size = VeritySpace.Medium)

                Row {
                    Column(modifier = Modifier.weight(1f)) {
                        VerityText(text = "Billed To", style = VerityTextStyle.Label)
                        VeritySpacer(size = VeritySpace.ExtraSmall)
                        VerityText(text = document.parties.billedTo.name, style = VerityTextStyle.Body)
                        // Not in the mockup (which only shows the name), but this is a GST
                        // document under review before finalizing — dropping the address the
                        // user is about to commit to would be a real functional regression, not
                        // just a style simplification.
                        VerityText(
                            text = document.parties.billedTo.addressLines.joinToString(", "),
                            style = VerityTextStyle.Caption
                        )
                    }
                    VeritySpacer(size = VeritySpace.Large, horizontal = true)
                    Column(modifier = Modifier.weight(1f)) {
                        VerityText(text = "Shipped To", style = VerityTextStyle.Label)
                        VeritySpacer(size = VeritySpace.ExtraSmall)
                        VerityText(text = document.parties.shippedTo.name, style = VerityTextStyle.Body)
                        VerityText(
                            text = document.parties.shippedTo.addressLines.joinToString(", "),
                            style = VerityTextStyle.Caption
                        )
                    }
                }
            }
        }

        VerityDivider(
            strength = VerityDividerStrength.Divider,
            modifier = Modifier.padding(vertical = 14.dp)
        )

        // ─────────────────────────────────────────────
        // Line Items — reuses VerityInvoiceLineItemRow (same component Workspace's draft list
        // uses) rather than a bespoke row, so a GST-required field like HSN doesn't silently drop
        // out of the pre-finalize review just because this screen built its own simplified card.
        // ─────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = VeritySpace.Small.dp)) {
            document.lineItems.forEachIndexed { index, item ->
                if (index != 0) {
                    VeritySpacer(size = VeritySpace.Small)
                }

                VerityInvoiceLineItemRow(
                    description = item.description,
                    quantity = item.quantity,
                    unit = item.unit,
                    rate = Money.ofPaise(item.ratePaise),
                    amount = Money.ofPaise(item.amountPaise),
                    hsnCode = item.hsnCode
                )
            }
        }

        VeritySpacer(size = VeritySpace.Medium)

        // ─────────────────────────────────────────────
        // Summary — Items Subtotal / combined tax / Grand Total, matching `.summary`. Built
        // directly here rather than via VerityInvoiceSummary: that component's tiered API (separate
        // CGST/SGST rows, optional freight row) doesn't fit this screen's simpler 3-row summary.
        // ─────────────────────────────────────────────
        VeritySurface(
            type = VeritySurfaceType.Assist,
            modifier = Modifier.padding(horizontal = VeritySpace.Small.dp)
        ) {
            Column(modifier = Modifier.padding(VeritySpace.Medium.dp)) {
                SummaryRow(
                    label = "Items Subtotal",
                    value = Money.ofPaise(document.totals.itemsSubtotalPaise).format()
                )

                if (document.totals.freightPaise > 0) {
                    VeritySpacer(size = VeritySpace.Small)

                    SummaryRow(
                        label = "Add Freight",
                        value = Money.ofPaise(document.totals.freightPaise).format()
                    )
                }

                val taxation = document.taxation
                if (taxation != null) {
                    VeritySpacer(size = VeritySpace.Small)

                    SummaryRow(
                        label = when (taxation.mode) {
                            DocumentTaxMode.INTRA_STATE -> "CGST + SGST"
                            DocumentTaxMode.INTER_STATE -> "IGST"
                        },
                        value = Money.ofPaise(document.totals.taxTotalPaise).format()
                    )
                }

                VeritySpacer(size = VeritySpace.Medium)
                VerityDivider(strength = VerityDividerStrength.Subtle)
                VeritySpacer(size = VeritySpace.Medium)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    VerityText(text = "Grand Total", style = VerityTextStyle.Body, modifier = Modifier.weight(1f))
                    VerityText(
                        text = Money.ofPaise(document.totals.grandTotalPaise).format(),
                        style = VerityTextStyle.Title
                    )
                }
            }
        }

        if (onSelfNotesChange != null) {
            VeritySpacer(size = VeritySpace.Medium)
            SelfNotesSection(
                selfNotes = selfNotes.orEmpty(),
                onSave = onSelfNotesChange
            )
        }

        if (onFinalize != null) {
            VeritySpacer(size = VeritySpace.Medium)

            VerityButton(
                label = if (isFinalizing) "Finalizing…" else "Finalize ${document.identity.documentType.displayLabel}",
                onClick = onFinalize,
                role = VerityButtonRole.Primary,
                state = if (isFinalizing) VerityButtonState.Disabled else VerityButtonState.Enabled,
                modifier = Modifier.padding(horizontal = VeritySpace.Small.dp)
            )
        }

        if (onViewPdf != null) {
            VeritySpacer(size = VeritySpace.Medium)

            VerityButton(
                label = "View PDF",
                onClick = onViewPdf,
                role = VerityButtonRole.Secondary,
                modifier = Modifier.padding(horizontal = VeritySpace.Small.dp)
            )
        }

        VeritySpacer(size = VeritySpace.Large)
    }
}

/**
 * Private, never-printed per-document notes (see DocumentEntity.selfNotes) — a Save button
 * appears only once the field diverges from the last-persisted value, so a note isn't re-pushed
 * to Firestore on every keystroke (see DefaultDocumentDetailDataSource.updateSelfNotes).
 */
@Composable
private fun SelfNotesSection(selfNotes: String, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(selfNotes) }
    LaunchedEffect(selfNotes) { text = selfNotes }

    VeritySection(
        title = "Notes to Self (private — not printed)",
        modifier = Modifier.padding(horizontal = VeritySpace.Small.dp)
    ) {
        VerityTextField(
            role = VerityTextFieldRole.Basic,
            label = "Notes",
            placeholder = "e.g. sold this part at this rate, follow up next week",
            value = text,
            onValueChange = { text = it },
            editing = true,
            onEnterEdit = null,
            onExitEdit = null,
            suggestions = emptyList(),
            onSelectSuggestion = null,
            singleLine = false
        )

        if (text != selfNotes) {
            VeritySpacer(size = VeritySpace.Small)
            VerityButton(
                label = "Save Note",
                onClick = { onSave(text) },
                role = VerityButtonRole.Secondary
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        VerityText(text = label, style = VerityTextStyle.Body, modifier = Modifier.weight(1f))
        VerityText(text = value, style = VerityTextStyle.Body)
    }
}
