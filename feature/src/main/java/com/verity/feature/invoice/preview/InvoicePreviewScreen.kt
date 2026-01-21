package com.verity.feature.invoice.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.verity.core.ui.primitives.dp
import androidx.compose.ui.unit.dp
import com.verity.core.document.model.InvoiceDocumentModel
import com.verity.core.ui.molecules.VeritySection
import com.verity.core.ui.primitives.VeritySpacer
import com.verity.core.ui.primitives.VeritySurface
import com.verity.core.ui.primitives.VeritySurfaceType
import com.verity.core.ui.primitives.VerityText
import com.verity.core.ui.primitives.VerityTextStyle
import com.verity.core.ui.primitives.VeritySpace
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Divider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicePreviewScreen(
    document: InvoiceDocumentModel,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = "Invoice Preview")
                },
                navigationIcon = {
                    // Navigation handled by parent (e.g. NavHost back)
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            VeritySpacer(size = VeritySpace.Large)

            // ─────────────────────────────────────────────
            // Section 1: Document Header (Number + Date)
            // ─────────────────────────────────────────────
            VeritySurface(
                type = VeritySurfaceType.Base,
                modifier = Modifier.padding(horizontal = VeritySpace.Small.dp)
            ) {
                VeritySection {
                    VerityText(
                        text = document.identity.documentNumber,
                        style = VerityTextStyle.Title
                    )

                    VeritySpacer(size = VeritySpace.ExtraSmall)

                    VerityText(
                        text = formatDate(document.identity.issueDate),
                        style = VerityTextStyle.Body
                    )
                }
            }

            VeritySpacer(size = VeritySpace.Medium)

            // ─────────────────────────────────────────────
            // Section 2: Parties (Billed To / Shipped To)
            // ─────────────────────────────────────────────
            VeritySurface(
                type = VeritySurfaceType.Base,
                modifier = Modifier.padding(horizontal = VeritySpace.Small.dp)
            ) {
                VeritySection {
                    VerityText(
                        text = "Billed To",
                        style = VerityTextStyle.Label
                    )
                    VerityText(
                        text = document.parties.billedTo.name,
                        style = VerityTextStyle.Body
                    )
                    VerityText(
                        text = document.parties.billedTo.addressLines.joinToString(", "),
                        style = VerityTextStyle.Body
                    )

                    VeritySpacer(size = VeritySpace.Small)

                    VerityText(
                        text = "Shipped To",
                        style = VerityTextStyle.Label
                    )
                    VerityText(
                        text = document.parties.shippedTo.name,
                        style = VerityTextStyle.Body
                    )
                    VerityText(
                        text = document.parties.shippedTo.addressLines.joinToString(", "),
                        style = VerityTextStyle.Body
                    )
                }
            }

            VeritySpacer(size = VeritySpace.Medium)

            // ─────────────────────────────────────────────
            // Section 3: Line Items
            // ─────────────────────────────────────────────
            VeritySurface(
                type = VeritySurfaceType.Base,
                modifier = Modifier.padding(horizontal = VeritySpace.Small.dp)
            ) {
                VeritySection {
                    VerityText(
                        text = "Line Items",
                        style = VerityTextStyle.Label
                    )

                    document.lineItems.forEach { item ->
                        VeritySpacer(size = VeritySpace.ExtraSmall)

                        Row {
                            VerityText(
                                text = item.description,
                                style = VerityTextStyle.Body
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            VerityText(
                                text = "₹${item.amountPaise / 100}",
                                style = VerityTextStyle.Body
                            )
                        }
                    }
                }
            }

            VeritySpacer(size = VeritySpace.Medium)

            // ─────────────────────────────────────────────
            // Section 4: Totals
            // ─────────────────────────────────────────────
            VeritySurface(
                type = VeritySurfaceType.Base,
                modifier = Modifier.padding(horizontal = VeritySpace.Small.dp)
            ) {
                VeritySection {
                    VerityText(
                        text = "Grand Total",
                        style = VerityTextStyle.Label
                    )
                    VerityText(
                        text = "₹${document.totals.grandTotalPaise / 100}",
                        style = VerityTextStyle.Title
                    )
                }
            }
        }
    }
}

private fun formatDate(date: java.time.LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    return date.format(formatter)
}