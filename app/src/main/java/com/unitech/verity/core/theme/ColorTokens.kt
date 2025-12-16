package com.unitech.verity.core.theme

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
    val background: Background,
    val surface: Surface,
    val text: Text,
    val action: Action,
    val state: State,
    val finance: Finance
) {

    data class Background(
        val app: Color,
        val subtle: Color,
        val inverse: Color
    )

    data class Surface(
        val base: Color,
        val raised: Color,
        val sunken: Color,
        val inverse: Color
    )

    data class Text(
        val primary: Color,
        val secondary: Color,
        val muted: Color,
        val inverse: Color,
        val disabled: Color
    )

    data class Action(
        val primary: Color,
        val secondary: Color,
        val destructive: Color,
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
        val emphasis: Color
    )
}