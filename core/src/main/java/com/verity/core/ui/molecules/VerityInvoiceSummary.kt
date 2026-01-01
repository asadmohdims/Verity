package com.verity.core.ui.molecules

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import com.verity.core.ui.primitives.VerityDivider
import com.verity.core.ui.primitives.VerityDividerStrength
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.verity.core.ui.primitives.VeritySpacer
import com.verity.core.ui.primitives.VerityText
import com.verity.core.ui.primitives.VerityTextStyle
import com.verity.core.ui.primitives.VeritySpace
import com.verity.core.ui.primitives.VeritySurface
import com.verity.core.ui.primitives.VeritySurfaceType
import com.verity.core.ui.primitives.dp
import com.verity.core.formatting.money.Money

/**
 * VerityInvoiceSummary
 *
 * PURPOSE
 * -------
 * Renders the financial summary section of an invoice, following
 * real-world GST invoice calculation flow.
 *
 * This molecule is:
 * - Invoice-specific
 * - Purely presentational
 * - Stateless and side-effect free
 *
 * DESIGN CONTRACT
 * ---------------
 * - Structured calculation narrative:
 *      1. Base value (items subtotal + additions)
 *      2. Tax application (CGST+SGST or IGST)
 *      3. Final payable amount
 * - Uses Assist surface (authoritative, non-interactive)
 * - No icons, actions, or interactivity
 * - No calculations performed internally
 *
 * DATA CONTRACT
 * -------------
 * All monetary values and tax components are expected to be
 * pre-computed upstream. This component only renders them.
 */
@Composable
fun VerityInvoiceSummary(
    itemsSubtotal: Money,
    freight: Money?,
    taxableSubtotal: Money,
    cgst: Pair<Int, Money>?,
    sgst: Pair<Int, Money>?,
    igst: Pair<Int, Money>?,
    totalAfterTax: Money,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = VeritySpace.Small.dp)
    ) {
        VeritySurface(type = VeritySurfaceType.Assist) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VeritySpace.Medium.dp, vertical = VeritySpace.Medium.dp)
            ) {

                // ─────────────────────────────────────────────
                // Tier 1 — Base & additions
                // ─────────────────────────────────────────────
                SummaryRow(
                    label = "Items Subtotal",
                    value = itemsSubtotal.format()
                )

                if (freight != null) {
                    VeritySpacer(size = VeritySpace.Small)
                    SummaryRow(
                        label = "Add Freight",
                        value = freight.format()
                    )
                }

                VeritySpacer(size = VeritySpace.Small)
                DividerRow()

                VeritySpacer(size = VeritySpace.Small)
                SummaryRow(
                    label = "Sub Total",
                    value = taxableSubtotal.format()
                )

                // ─────────────────────────────────────────────
                // Tier 2 — Tax breakdown
                // ─────────────────────────────────────────────
                if (cgst != null && sgst != null) {
                    VeritySpacer(size = VeritySpace.Medium)

                    SummaryRow(
                        label = "Add CGST @ ${cgst.first}%",
                        value = cgst.second.format()
                    )

                    VeritySpacer(size = VeritySpace.Small)

                    SummaryRow(
                        label = "Add SGST @ ${sgst.first}%",
                        value = sgst.second.format()
                    )
                }

                if (igst != null) {
                    VeritySpacer(size = VeritySpace.Medium)

                    SummaryRow(
                        label = "Add IGST @ ${igst.first}%",
                        value = igst.second.format()
                    )
                }

                // ─────────────────────────────────────────────
                // Tier 3 — Final payable
                // ─────────────────────────────────────────────
                VeritySpacer(size = VeritySpace.Medium)
                DividerRow()
                VeritySpacer(size = VeritySpace.Medium)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VerityText(
                        text = "Total Amount After Tax",
                        style = VerityTextStyle.Body,
                        modifier = Modifier.weight(1f)
                    )
                    VerityText(
                        text = totalAfterTax.format(),
                        style = VerityTextStyle.Title
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VerityText(
            text = label,
            style = VerityTextStyle.Body,
            modifier = Modifier.weight(1f)
        )
        VerityText(
            text = value,
            style = VerityTextStyle.Body
        )
    }
}

@Composable
private fun DividerRow() {
    VerityDivider(
        strength = VerityDividerStrength.Subtle
    )
}
