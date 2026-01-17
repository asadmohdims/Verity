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

            // Other sections will be added incrementally
        }
    }
}

private fun formatDate(date: java.time.LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    return date.format(formatter)
}