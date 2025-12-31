package com.verity.core.ui.molecules

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.SnackbarData
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.verity.core.ui.primitives.VeritySurface
import com.verity.core.ui.primitives.VeritySurfaceType
import com.verity.core.ui.primitives.VerityText
import com.verity.core.ui.primitives.VerityTextStyle
import androidx.compose.ui.draw.alpha

/**
 * VeritySnackbar
 *
 * Neutral, informational snackbar content styled as a calm pill.
 *
 * Design:
 * - surface.floatingPill
 * - Body typography
 * - No actions
 * - No semantic coloring
 *
 * Ownership:
 * - Rendering only
 * - State & timing handled by screen-level SnackbarHost
 */
@Composable
fun VeritySnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier
) {
    VeritySurface(
        type = VeritySurfaceType.FloatingPill,
        modifier = modifier.wrapContentWidth()
    ) {
        VerityText(
            text = snackbarData.visuals.message,
            style = VerityTextStyle.Body,
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .alpha(0.9f)
        )
    }
}
