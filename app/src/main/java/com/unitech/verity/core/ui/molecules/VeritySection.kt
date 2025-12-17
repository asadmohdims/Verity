package com.unitech.verity.core.ui.molecules

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.unitech.verity.core.ui.primitives.VerityDivider
import com.unitech.verity.core.ui.primitives.VerityDividerStrength
import com.unitech.verity.core.ui.primitives.VeritySpacer
import com.unitech.verity.core.ui.primitives.VeritySpace
import com.unitech.verity.core.ui.primitives.VeritySurface
import com.unitech.verity.core.ui.primitives.VeritySurfaceType
import com.unitech.verity.core.ui.primitives.VerityText
import com.unitech.verity.core.ui.primitives.VerityTextStyle

/**
 * VeritySection is a structural grouping molecule.
 *
 * It composes surfaces, spacing, and optional headers
 * to create consistent UI sections across the app.
 */
@Composable
fun VeritySection(
    modifier: Modifier = Modifier,
    title: String? = null,
    surfaceType: VeritySurfaceType = VeritySurfaceType.Base,
    showDivider: Boolean = false,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier) {

        VeritySurface(type = surfaceType) {
            Column {
                if (title != null) {
                    VerityText(
                        text = title,
                        style = VerityTextStyle.Title
                    )
                    VeritySpacer(size = VeritySpace.Small)
                }

                content()
            }
        }

        if (showDivider) {
            VeritySpacer(size = VeritySpace.Small)
            VerityDivider(strength = VerityDividerStrength.Subtle)
        }
    }
}