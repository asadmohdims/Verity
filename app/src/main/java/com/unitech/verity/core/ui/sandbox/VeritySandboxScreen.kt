package com.unitech.verity.core.ui.sandbox

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.unitech.verity.core.theme.VerityBaseTypography
import com.unitech.verity.core.theme.VerityTheme
import com.unitech.verity.core.ui.molecules.VerityHeader
import com.unitech.verity.core.ui.molecules.VerityListItem
import com.unitech.verity.core.ui.molecules.VeritySection
import com.unitech.verity.core.ui.primitives.VeritySpacer
import com.unitech.verity.core.ui.primitives.VeritySpace
import com.unitech.verity.core.ui.primitives.VeritySurface
import com.unitech.verity.core.ui.primitives.VeritySurfaceType
import com.unitech.verity.core.ui.primitives.VerityText
import com.unitech.verity.core.ui.primitives.VerityTextStyle
import com.unitech.verity.core.ui.primitives.dp

/**
 * VeritySandboxScreen
 *
 * Visual validation surface for:
 * - typography hierarchy
 * - spacing rhythm
 * - surface contrast
 *
 * Sandbox only. No logic. No navigation.
 */
@Composable
fun VeritySandboxScreen() {
    VeritySurface(
        type = VeritySurfaceType.Base,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(VeritySpace.Medium.dp)
        ) {

            // ── Screen Header ───────────────────────────────
            VerityHeader(
                title = "Verity Sandbox",
                subtitle = "Visual rhythm & typography validation"
            )

            VeritySpacer(size = VeritySpace.Large)

            // ── Typography Ladder ──────────────────────────
            VeritySection(
                title = "Typography Hierarchy",
                showDivider = true
            ) {

                VerityText(
                    text = "Display — Screen Identity",
                    style = VerityTextStyle.Display
                )

                VeritySpacer(size = VeritySpace.ExtraLarge)

                VerityText(
                    text = "Title — Section Anchor",
                    style = VerityTextStyle.Title
                )

                VeritySpacer(size = VeritySpace.Medium)

                VerityText(
                    text = "Body — Primary factual content used for names, ledger rows, and descriptions.",
                    style = VerityTextStyle.Body
                )

                VeritySpacer(size = VeritySpace.Small)

                VerityText(
                    text = "Label — Metadata and structural hints",
                    style = VerityTextStyle.Label
                )

                VeritySpacer(size = VeritySpace.Small)

                VerityText(
                    text = "Caption — Secondary context like dates · 12 Mar 2025",
                    style = VerityTextStyle.Caption
                )
            }

            VeritySpacer(size = VeritySpace.Large)

            // ── Numeric Stability ──────────────────────────
            VeritySection(
                title = "Numeric Stability",
                showDivider = true
            ) {
                VerityText(
                    text = "₹ 1,23,456.78",
                    style = VerityTextStyle.Body
                )

                VeritySpacer(size = VeritySpace.Small)

                VerityText(
                    text = "₹ 9,876.00",
                    style = VerityTextStyle.Body
                )

                VeritySpacer(size = VeritySpace.Small)

                VerityText(
                    text = "₹ 12,00,000.00",
                    style = VerityTextStyle.Body
                )
            }

            VeritySpacer(size = VeritySpace.Large)

            // ── List Rhythm ────────────────────────────────
            VeritySection(
                title = "List Rhythm",
                surfaceType = VeritySurfaceType.Raised
            ) {

                VerityListItem(
                    title = "Unitech Machineries",
                    subtitle = "Customer"
                )

                VeritySpacer(size = VeritySpace.Medium)

                VerityListItem(
                    title = "Invoice #INV-1024",
                    subtitle = "₹ 54,320.00"
                )

                VeritySpacer(size = VeritySpace.Medium)

                VerityListItem(
                    title = "Payment Received",
                    subtitle = "₹ 20,000.00 · 12 Mar"
                )
            }

            VeritySpacer(size = VeritySpace.Large)

            // ── Surface Contrast ───────────────────────────
            VeritySection(
                title = "Surface Contrast",
                surfaceType = VeritySurfaceType.Sunken
            ) {
                VerityText(
                    text = "Sunken surface for dense information zones.",
                    style = VerityTextStyle.Body
                )
            }

            VeritySpacer(size = VeritySpace.ExtraLarge)
        }
    }
}

/* ──────────────────────────────── */
/* Preview wrappers (sandbox only) */
/* ──────────────────────────────── */

@Preview(
    name = "Light",
    showBackground = true
)
@Composable
private fun VeritySandboxPreviewLight() {
    VerityTheme(
        darkTheme = false,
        typography = VerityBaseTypography
    ) {
        VeritySandboxScreen()
    }
}

@Preview(
    name = "Dark",
    showBackground = true
)
@Composable
private fun VeritySandboxPreviewDark() {
    VerityTheme(
        darkTheme = true,
        typography = VerityBaseTypography
    ) {
        VeritySandboxScreen()
    }
}