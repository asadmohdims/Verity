package com.verity.core.ui.molecules

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
 * VerityTransportSummaryRow
 *
 * PURPOSE
 * -------
 * Renders a summary row for invoice transportation details.
 *
 * This molecule is:
 * - Transport-specific (NOT a line item)
 * - Purely presentational
 * - Stateless and side-effect free
 *
 * DESIGN CONTRACT
 * ---------------
 * - Mirrors the visual structure of invoice line items
 * - Two-row hierarchy:
 *      Row 1 → Transporter identity + freight amount
 *      Row 2 → Vehicle number and GR / LR number
 * - No quantity, rate, or tax semantics
 * - No actions, menus, icons, or callbacks
 *
 * DATA CONTRACT
 * -------------
 * All values are expected to be pre-validated and pre-formatted upstream.
 * This component does not perform calculations or formatting.
 */
@Composable
fun VerityTransportSummaryRow(
    transporterName: String,
    vehicleNumber: String?,
    grOrLrNumber: String?,
    freight: Money?,
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

                    // Left: Transporter name (unbounded wrapping)
                    VerityText(
                        text = transporterName,
                        style = VerityTextStyle.Body,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = VeritySpace.Small.dp)
                    )

                    // Right: Freight amount (if present)
                    if (freight != null) {
                        VerityText(
                            text = freight.format(),
                            style = VerityTextStyle.Title
                        )
                    }
                }

                if (!vehicleNumber.isNullOrBlank() || !grOrLrNumber.isNullOrBlank()) {
                    VeritySpacer(size = VeritySpace.Medium)

                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        if (!vehicleNumber.isNullOrBlank()) {
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.Start
                            ) {
                                VerityText(
                                    text = "VEHICLE",
                                    style = VerityTextStyle.Caption
                                )
                                VeritySpacer(size = VeritySpace.ExtraSmall)
                                VerityText(
                                    text = vehicleNumber,
                                    style = VerityTextStyle.Label
                                )
                            }
                        }

                        if (!grOrLrNumber.isNullOrBlank()) {
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                VerityText(
                                    text = "GR",
                                    style = VerityTextStyle.Caption
                                )
                                VeritySpacer(size = VeritySpace.ExtraSmall)
                                VerityText(
                                    text = grOrLrNumber,
                                    style = VerityTextStyle.Label
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
