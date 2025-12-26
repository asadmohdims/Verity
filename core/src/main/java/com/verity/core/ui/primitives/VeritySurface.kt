package com.verity.core.ui.primitives

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.verity.core.theme.VerityTheme

/**
 * Semantic surface types used across the Verity UI.
 *
 * These map to luminance and elevation intent,
 * not raw dp values.
 */
enum class VeritySurfaceType {
    Base,
    Raised,
    Assist
}

/**
 * VeritySurface is the only allowed container surface in the UI layer.
 *
 * It enforces:
 * - semantic surface usage
 * - theme-driven background colors
 * - consistency across light/dark mode
 *
 * This component intentionally does NOT:
 * - add padding
 * - add shape
 * - add borders
 */
@Composable
fun VeritySurface(
    type: VeritySurfaceType,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = VerityTheme.colors

    val backgroundColor = when (type) {
        VeritySurfaceType.Base -> colors.surface.base
        VeritySurfaceType.Raised -> colors.surface.raised
        VeritySurfaceType.Assist -> colors.surface.assist
    }

    Surface(
        modifier = modifier,
        color = backgroundColor,
        content = content
    )
}