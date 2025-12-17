package com.unitech.verity.core.ui.primitives

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Semantic spacing sizes used across the Verity UI.
 *
 * Spacing is a first-class design primitive.
 * Raw dp values must not be used in UI code.
 */
enum class VeritySpace {
    ExtraSmall,
    Small,
    Medium,
    Large,
    ExtraLarge
}

/**
 * Exposes the canonical Dp value for each semantic space.
 * This is the single source of truth for spacing rhythm.
 *
 * Usage:
 * - VeritySpace.Large.dp → 24.dp
 */
val VeritySpace.dp: Dp
    get() = when (this) {
        VeritySpace.ExtraSmall -> 4.dp
        VeritySpace.Small -> 8.dp
        VeritySpace.Medium -> 16.dp
        VeritySpace.Large -> 24.dp
        VeritySpace.ExtraLarge -> 32.dp
    }

/**
 * VeritySpacer enforces consistent spacing rhythm.
 *
 * Use this instead of Spacer + dp in UI code.
 */
@Composable
fun VeritySpacer(
    size: VeritySpace,
    modifier: Modifier = Modifier,
    horizontal: Boolean = false
) {
    val dimension = size.dp

    Spacer(
        modifier = modifier.then(
            if (horizontal) {
                Modifier.width(dimension)
            } else {
                Modifier.height(dimension)
            }
        )
    )
}