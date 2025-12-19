package com.verity.core.ui.primitives

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.verity.core.theme.VerityTheme

/**
 * Semantic divider strengths used across the Verity UI.
 *
 * Dividers are for structure, not decoration.
 */
enum class VerityDividerStrength {
    Subtle,
    Strong
}

/**
 * VerityDivider is the only allowed divider primitive.
 *
 * It enforces:
 * - theme-driven separation
 * - consistent visual rhythm
 * - dark/light parity
 *
 * This component intentionally does NOT:
 * - expose raw colors
 * - expose arbitrary thickness
 * - act as a border replacement
 */
@Composable
fun VerityDivider(
    strength: VerityDividerStrength,
    modifier: Modifier = Modifier
) {
    val colors = VerityTheme.colors

    val dividerColor = when (strength) {
        VerityDividerStrength.Subtle -> colors.text.muted.copy(alpha = 0.25f)
        VerityDividerStrength.Strong -> colors.text.muted.copy(alpha = 0.45f)
    }

    val thickness = when (strength) {
        VerityDividerStrength.Subtle -> 1.dp
        VerityDividerStrength.Strong -> 1.5.dp
    }

    Divider(
        modifier = modifier.fillMaxWidth().height(thickness),
        color = dividerColor
    )
}