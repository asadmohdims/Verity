package com.verity.feature.invoice.ui

import androidx.compose.ui.tooling.preview.Preview
import com.verity.core.theme.VerityTheme
import com.verity.core.theme.VerityBaseTypography
import com.verity.invoice.draft.DraftCustomer
import com.verity.invoice.draft.DraftLineItem
import com.verity.invoice.draft.DraftAdditionalDetails
import com.verity.invoice.draft.DraftSummary

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.verity.core.ui.molecules.VerityHeader
import com.verity.core.ui.molecules.VeritySection
import com.verity.core.ui.primitives.VeritySpacer
import com.verity.core.ui.primitives.VeritySpace
import com.verity.core.ui.primitives.VerityText
import com.verity.core.ui.primitives.VerityTextStyle
import com.verity.invoice.draft.InvoiceDraftUiState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.verity.core.ui.primitives.VeritySurface
import com.verity.core.ui.primitives.VeritySurfaceType
import com.verity.core.ui.primitives.dp
import com.verity.core.ui.molecules.VerityListItem

/**
 * InvoiceWorkspaceScreen
 *
 * PURPOSE
 * -------
 * Primary working surface for invoice / challan creation.
 *
 * This screen represents the user's daily workspace and is the
 * default landing screen of the application.
 *
 * DELIVERY PHASE
 * --------------
 * D1A — UI-only, read-only skeleton.
 *
 * This implementation intentionally:
 * • Renders draft state only
 * • Does NOT allow editing
 * • Does NOT emit commands
 * • Does NOT invoke replay or persistence
 *
 * NON-GOALS (EXPLICIT)
 * -------------------
 * • No validation
 * • No inputs
 * • No buttons with behavior
 * • No preview / finalize flows
 *
 * This screen exists purely to validate:
 * • Structural hierarchy
 * • Visual rhythm
 * • Section composition
 */
@Composable
fun InvoiceWorkspaceScreen(
    draft: InvoiceDraftUiState
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        // ─────────────────────────────────────────────
        // Screen Header
        // ─────────────────────────────────────────────

        VerityHeader(
            title = "Invoice Workspace",
            subtitle = "Invoice",
            trailing = {
                VerityText(
                    text = "Search",
                    style = VerityTextStyle.Label
                )
            }
        )

        VeritySpacer(size = VeritySpace.Large)

        VerityText(
            text = "Document Type: Invoice",
            style = VerityTextStyle.Caption,
            modifier = Modifier.padding(horizontal = VeritySpace.Small.dp)
        )

        VeritySpacer(size = VeritySpace.Small)

        // ─────────────────────────────────────────────
        // Parties Section (formerly Customer Details)
        // ─────────────────────────────────────────────

        VeritySurface(
            type = VeritySurfaceType.Base,
            modifier = Modifier.padding(horizontal = VeritySpace.Small.dp)
        ) {
            VeritySection {

                androidx.compose.foundation.layout.Row {

                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        VerityText(
                            text = "Billed To",
                            style = VerityTextStyle.Label
                        )

                        VeritySpacer(size = VeritySpace.ExtraSmall)

                        VerityText(
                            text = draft.customer?.displayName ?: "Select customer",
                            style = VerityTextStyle.Body
                        )
                    }

                    VeritySpacer(size = VeritySpace.Large, horizontal = true)

                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        VerityText(
                            text = "Shipped To",
                            style = VerityTextStyle.Label
                        )

                        VeritySpacer(size = VeritySpace.ExtraSmall)

                        VerityText(
                            text = draft.customer?.displayName ?: "Same as billed",
                            style = VerityTextStyle.Body
                        )

                        VerityText(
                            text = "(Same as billed)",
                            style = VerityTextStyle.Caption
                        )
                    }
                }
            }
        }

        VeritySpacer(size = VeritySpace.Medium)

        // ─────────────────────────────────────────────
        // Line Items Section
        // ─────────────────────────────────────────────

        VeritySection(title = "Line Items") {

            if (draft.lineItems.isEmpty()) {
                VerityText(
                    text = "No line items added",
                    style = VerityTextStyle.Caption
                )
            } else {
                draft.lineItems.forEach { item ->
                    VerityListItem(
                        title = item.description,
                        subtitle =
                            "HSN ${item.hsnCode} · ${item.quantity} ${item.unit} × ${item.rate}  |  Amount: ${item.amount}"
                    )

                    VeritySpacer(size = VeritySpace.Small)
                }
            }
        }

        VeritySpacer(size = VeritySpace.Large)

        // ─────────────────────────────────────────────
        // Logistics Section (formerly Additional Details)
        // ─────────────────────────────────────────────

        VeritySection(title = "Logistics") {

            VerityText(
                text = "Transporter: —",
                style = VerityTextStyle.Caption
            )

            VerityText(
                text = "Vehicle No: —",
                style = VerityTextStyle.Caption
            )

            VerityText(
                text = "Supply Date: —",
                style = VerityTextStyle.Caption
            )

            VeritySpacer(size = VeritySpace.Small)

            VerityText(
                text = "Linked Challan: —",
                style = VerityTextStyle.Caption
            )
        }

        VeritySpacer(size = VeritySpace.Large)

        // ─────────────────────────────────────────────
        // Summary Section
        // ─────────────────────────────────────────────

        VeritySurface(
            type = VeritySurfaceType.Sunken,
            modifier = Modifier.padding(horizontal = VeritySpace.Small.dp)
        ) {
            VeritySection(title = "Summary") {
                VerityText(
                    text = "Subtotal: ${draft.summary.subtotal}",
                    style = VerityTextStyle.Body
                )

                VeritySpacer(size = VeritySpace.ExtraSmall)

                VerityText(
                    text = "Freight: ${draft.additionalDetails.freightAmount ?: 0}",
                    style = VerityTextStyle.Body
                )

                VeritySpacer(size = VeritySpace.ExtraSmall)

                VerityText(
                    text = "Tax: ${draft.summary.taxTotal}",
                    style = VerityTextStyle.Body
                )

                VeritySpacer(size = VeritySpace.Small)

                VerityText(
                    text = "Grand Total: ${draft.summary.grandTotal}",
                    style = VerityTextStyle.Title
                )
            }
        }
    }
}

@Preview(
    name = "Invoice Workspace — Light",
    showBackground = true
)
@Composable
private fun InvoiceWorkspacePreviewLight() {
    VerityTheme(
        darkTheme = false,
        typography = VerityBaseTypography
    ) {
        VeritySurface(
            type = VeritySurfaceType.Base,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(VeritySpace.Medium.dp)
            ) {
                InvoiceWorkspaceScreen(
                    draft = previewInvoiceDraft()
                )
            }
        }
    }
}

@Preview(
    name = "Invoice Workspace — Dark",
    showBackground = true
)
@Composable
private fun InvoiceWorkspacePreviewDark() {
    VerityTheme(
        darkTheme = true,
        typography = VerityBaseTypography
    ) {
        VeritySurface(
            type = VeritySurfaceType.Base,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(VeritySpace.Medium.dp)
            ) {
                InvoiceWorkspaceScreen(
                    draft = previewInvoiceDraft()
                )
            }
        }
    }
}

@Composable
private fun previewInvoiceDraft(): InvoiceDraftUiState =
    InvoiceDraftUiState(
        customer = DraftCustomer(
            displayName = "Bhargava Industries",
            gstin = "27AAACB1234Z1Z"
        ),
        lineItems = listOf(
            DraftLineItem(
                description = "Metal Sheet",
                hsnCode = "7208",
                quantity = 10.0,
                unit = "PCS",
                rate = 320,
                amount = 3200
            ),
            DraftLineItem(
                description = "Cold Rolled Coil",
                hsnCode = "7209",
                quantity = 5.0,
                unit = "KG",
                rate = 450,
                amount = 2250
            )
        ),
        additionalDetails = DraftAdditionalDetails(
            freightAmount = 500
        ),
        summary = DraftSummary(
            subtotal = 5950,
            taxTotal = 1071,
            grandTotal = 7021
        )
    )