package com.unitech.verity.core.ui.primitives

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.unitech.verity.core.theme.VerityTheme

/**
 * Semantic text styles allowed in Verity.
 *
 * This mirrors VerityTypography exactly.
 * No additional styles are permitted.
 */
enum class VerityTextStyle {
    Display,
    Title,
    Body,
    Label,
    Caption
}

/**
 * VerityText is the only allowed text-rendering primitive.
 *
 * It enforces:
 * - semantic typography usage
 * - theme-driven text colors
 * - consistent hierarchy across the app
 *
 * This component intentionally does NOT:
 * - expose font sizes
 * - expose font weights
 * - allow arbitrary colors
 */
@Composable
fun VerityText(
    text: String,
    style: VerityTextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val typography = VerityTheme.typography
    val colors = VerityTheme.colors

    val textStyle = when (style) {
        VerityTextStyle.Display -> typography.display
        VerityTextStyle.Title -> typography.title
        VerityTextStyle.Body -> typography.body
        VerityTextStyle.Label -> typography.label
        VerityTextStyle.Caption -> typography.caption
    }

    val textColor = when (style) {
        VerityTextStyle.Caption -> colors.text.muted
        VerityTextStyle.Label -> colors.text.secondary
        else -> colors.text.primary
    }

    Text(
        text = text,
        modifier = modifier,
        style = textStyle,
        color = textColor,
        maxLines = maxLines,
        overflow = overflow
    )
}