package com.verity.core.ui.molecules

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.verity.core.theme.VerityTheme
import com.verity.core.ui.primitives.VeritySpace
import com.verity.core.ui.primitives.VeritySpacer
import com.verity.core.ui.primitives.VeritySurface
import com.verity.core.ui.primitives.VeritySurfaceType
import com.verity.core.ui.primitives.VerityText
import com.verity.core.ui.primitives.VerityTextStyle
import androidx.compose.ui.draw.alpha

/**
 * VeritySnackbar
 *
 * Neutral, informational snackbar content styled as a calm pill, with an optional action label
 * (e.g. "Undo") when the triggering [SnackbarData] carries one.
 *
 * Design:
 * - surface.floatingPill
 * - Body typography for the message
 * - Action label (if present) renders via Material3's own TextButton, colored via its
 *   `colors` param (primary) rather than through VerityText, which intentionally disallows
 *   arbitrary colors for non-interactive content text. TextButton also gets the correct 48dp
 *   touch target for free, unlike a bare Text().clickable{}.
 *
 * NOTE: an earlier version of this action never received clicks at all, on-device or in
 * InvoiceWorkspaceScreenUndoTest. That was NOT a problem with this composable -- it was
 * InvoiceWorkspaceScreen's Box declaring SnackbarHost *before* its scrollable Column, so the
 * Column (drawn on top, fillMaxSize) intercepted every tap in that screen region, snackbar
 * included, before it ever reached this content. Fixed by reordering that Box; see the comment
 * there.
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            VerityText(
                text = snackbarData.visuals.message,
                style = VerityTextStyle.Body,
                modifier = Modifier.alpha(0.9f)
            )

            val actionLabel = snackbarData.visuals.actionLabel
            if (actionLabel != null) {
                VeritySpacer(size = VeritySpace.Medium, horizontal = true)
                TextButton(
                    onClick = { snackbarData.performAction() },
                    colors = ButtonDefaults.textButtonColors(contentColor = VerityTheme.colors.primary)
                ) {
                    Text(
                        text = actionLabel,
                        style = VerityTheme.typography.label
                    )
                }
            }
        }
    }
}
