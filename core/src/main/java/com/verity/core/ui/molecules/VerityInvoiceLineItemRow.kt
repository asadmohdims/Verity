package com.verity.core.ui.molecules

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
 * VerityInvoiceLineItemRow
 *
 * PURPOSE
 * -------
 * Renders a single invoice line item as a structured, scan-friendly ledger row.
 *
 * This molecule is:
 * - Invoice-specific (NOT a generic list item)
 * - Purely presentational
 * - Stateless and side-effect free
 *
 * DESIGN CONTRACT
 * ---------------
 * - Variant A (Comfort density) layout
 * - Two-zone structure:
 *      Left  → Identification (description, HSN)
 *      Right → Financials (amount, quantity × rate)
 * - No per-line-item tax semantics
 * - No actions, menus, icons, or callbacks
 * - No business logic or calculations
 *
 * DATA CONTRACT
 * -------------
 * All numeric values are expected to be pre-computed and pre-formatted upstream.
 * This component does not perform rounding, currency formatting, or validation.
 */
@Composable
fun VerityInvoiceLineItemRow(
    description: String,
    quantity: Double,
    rate: Double,
    amount: Money,
    hsnCode: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = VeritySpace.Small.dp)
    ) {
        VeritySurface(type = VeritySurfaceType.Raised) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VeritySpace.Small.dp, vertical = VeritySpace.Small.dp)
            ) {

                // ─────────────────────────────────────────────
                // Row 1 — Primary information
                // ─────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {

                    // Left: Item description (unbounded wrapping)
                    VerityText(
                        text = description,
                        style = VerityTextStyle.Body,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = VeritySpace.Small.dp)
                    )

                    // Right: Line total amount (visual anchor)
                    VerityText(
                        text = amount.format(),
                        style = VerityTextStyle.Body
                    )
                }

                VeritySpacer(size = VeritySpace.Large)

                // ─────────────────────────────────────────────
                // Row 2 — Secondary information (micro-metrics layout, compact)
                // ─────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (!hsnCode.isNullOrBlank()) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.Start
                        ) {
                            VerityText(
                                text = "HSN",
                                style = VerityTextStyle.Caption
                            )
                            VeritySpacer(size = VeritySpace.ExtraSmall)
                            VerityText(
                                text = hsnCode.toString(),
                                style = VerityTextStyle.Label
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            VerityText(
                                text = "QTY",
                                style = VerityTextStyle.Caption
                            )
                            VeritySpacer(size = VeritySpace.ExtraSmall)
                            VerityText(
                                text = quantity.toString(),
                                style = VerityTextStyle.Label
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            VerityText(
                                text = "RATE",
                                style = VerityTextStyle.Caption
                            )
                            VeritySpacer(size = VeritySpace.ExtraSmall)
                            VerityText(
                                text = rate.toString(),
                                style = VerityTextStyle.Label
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.Start
                        ) {
                            VerityText(
                                text = "QTY",
                                style = VerityTextStyle.Caption
                            )
                            VeritySpacer(size = VeritySpace.ExtraSmall)
                            VerityText(
                                text = quantity.toString(),
                                style = VerityTextStyle.Label
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            VerityText(
                                text = "RATE",
                                style = VerityTextStyle.Caption
                            )
                            VeritySpacer(size = VeritySpace.ExtraSmall)
                            VerityText(
                                text = rate.toString(),
                                style = VerityTextStyle.Label
                            )
                        }
                    }
                }
            }
        }
    }
}
