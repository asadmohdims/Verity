package com.verity.core.theme

import androidx.compose.ui.graphics.Color

/**
 * Verity semantic color tokens.
 *
 * This file defines intent-only color roles.
 * No raw color values should be referenced outside theme.
 *
 * All UI, PDF, and rendering layers must consume colors
 * exclusively via these semantic tokens.
 */
data class VerityColors(
    val primary: Color,
    val accent: Color,
    val background: Background,
    val surface: Surface,
    val borders: Borders,
    val text: Text,
    val state: State,
    val finance: Finance
) {

    data class Background(
        val app: Color,
        val subtle: Color,
        val inverse: Color
    )

    data class Surface(
        /** Primary content surfaces */
        val base: Color,

        /** Elevated containers (dialogs, sheets) */
        val raised: Color,

        /**
         * Assist surfaces:
         * - search suggestions
         * - inline helpers
         * - guidance UI
         *
         * Calm, non-authoritative.
         */
        val assist: Color,

        /** Inverse surface for dark-on-light contexts */
        val inverse: Color
    )

    data class Borders(
        val subtle: Color,
        val strong: Color,
        val divider: Color
    )

    data class Text(
        val primary: Color,
        val secondary: Color,
        val muted: Color,
        val inverse: Color,
        val disabled: Color
    )

    data class State(
        val success: Color,
        val warning: Color,
        val error: Color,
        val info: Color
    )

    data class Finance(
        val neutral: Color,
        val emphasis: Color,
        val ledger: Color
    )
}