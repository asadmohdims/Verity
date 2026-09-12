package com.verity.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.verity.core.ui.primitives.VeritySpace
import com.verity.core.ui.primitives.VeritySpacer
import com.verity.core.ui.primitives.VeritySurface
import com.verity.core.ui.primitives.VeritySurfaceType
import com.verity.core.ui.primitives.VerityText
import com.verity.core.ui.primitives.VerityTextStyle
import com.verity.core.ui.primitives.dp

/**
 * PlaceholderScreen
 *
 * Temporary content for a nav-shell destination whose real screen hasn't been built yet
 * (Documents/Customers/Settings — Phase 2/3 of the approved Design Blueprint roadmap; see
 * CLAUDE.md's "UX direction"). Exists purely so the bottom nav has somewhere honest to land
 * tonight — deliberately not a feature-module screen, since it gets deleted wholesale the moment
 * the real screen lands.
 */
@Composable
fun PlaceholderScreen(title: String, message: String) {
    VeritySurface(
        type = VeritySurfaceType.Base,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(VeritySpace.Large.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            VeritySpacer(size = VeritySpace.ExtraLarge)
            VerityText(text = title, style = VerityTextStyle.Title)
            VeritySpacer(size = VeritySpace.Small)
            VerityText(text = message, style = VerityTextStyle.Caption)
        }
    }
}
