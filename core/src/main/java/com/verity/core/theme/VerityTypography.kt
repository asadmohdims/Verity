package com.verity.core.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit

/**
 * Verity semantic typography tokens.
 *
 * Typography encodes hierarchy and intent, not decoration.
 * No UI should reference Material typography directly.
 *
 * Rules enforced by this contract:
 * - Max 5 text styles
 * - Numbers use tabular numerals
 * - Weight > color for hierarchy
 */
data class VerityTypography(
    val display: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val caption: TextStyle
)

/**
 * OpenType feature settings for numeric text.
 *
 * "tnum" enables tabular (monospaced) numerals for stable digit widths.
 * This uses the native Compose `fontFeatureSettings` API, which is string-based.
 */
const val NUMERIC_FONT_FEATURE_SETTINGS = "tnum"

/**
 * Factory helper to create a numeric-safe TextStyle.
 *
 * This is intent-only; actual font family and weights
 * will be provided later when values are defined.
 */
fun numericTextStyle(
    fontSize: TextUnit,
    fontWeight: FontWeight
): TextStyle =
    TextStyle(
        fontSize = fontSize,
        fontWeight = fontWeight,
        fontFamily = FontFamily.Default,
        fontFeatureSettings = NUMERIC_FONT_FEATURE_SETTINGS
    )