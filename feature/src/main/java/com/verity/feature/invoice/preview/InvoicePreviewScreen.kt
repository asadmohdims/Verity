package com.verity.feature.invoice.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.verity.core.ui.primitives.dp
import androidx.compose.ui.unit.dp
import com.verity.core.document.model.DocumentTaxMode
import com.verity.core.document.model.InvoiceDocumentModel
import com.verity.core.formatting.date.DocumentDate
import com.verity.core.formatting.money.Money
import com.verity.core.ui.molecules.VeritySection
import com.verity.core.ui.primitives.VerityButton
import com.verity.core.ui.primitives.VerityButtonRole
import com.verity.core.ui.primitives.VerityButtonState
import com.verity.core.ui.primitives.VerityDivider
import com.verity.core.ui.primitives.VerityDividerStrength
import com.verity.core.ui.primitives.VeritySpacer
import com.verity.core.ui.primitives.VeritySurface
import com.verity.core.ui.primitives.VeritySurfaceType
import com.verity.core.ui.primitives.VerityText
import com.verity.core.ui.primitives.VerityTextStyle
import com.verity.core.ui.primitives.VeritySpace
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width

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
    isFinalizing: Boolean = false
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
        // Line Items — each its own Raised card, matching `.lineitem`.
        // ─────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = VeritySpace.Small.dp)) {
            document.lineItems.forEachIndexed { index, item ->
                if (index != 0) {
                    VeritySpacer(size = VeritySpace.Small)
                }

                VeritySurface(type = VeritySurfaceType.Raised) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(VeritySpace.Small.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        VerityText(
                            text = item.description,
                            style = VerityTextStyle.Body,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        VerityText(
                            text = Money.ofPaise(item.amountPaise).format(),
                            style = VerityTextStyle.Title
                        )
                    }
                }
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

                VeritySpacer(size = VeritySpace.Small)

                SummaryRow(
                    label = when (document.taxation?.mode) {
                        DocumentTaxMode.INTRA_STATE -> "CGST + SGST"
                        DocumentTaxMode.INTER_STATE -> "IGST"
                        null -> "Tax"
                    },
                    value = Money.ofPaise(document.totals.taxTotalPaise).format()
                )

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

        if (onFinalize != null) {
            VeritySpacer(size = VeritySpace.Medium)

            VerityButton(
                label = if (isFinalizing) "Finalizing…" else "Finalize Invoice",
                onClick = onFinalize,
                role = VerityButtonRole.Primary,
                state = if (isFinalizing) VerityButtonState.Disabled else VerityButtonState.Enabled,
                modifier = Modifier.padding(horizontal = VeritySpace.Small.dp)
            )
        }

        VeritySpacer(size = VeritySpace.Large)
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
