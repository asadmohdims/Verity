package com.verity.core.ui.molecules

import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars


import androidx.compose.foundation.layout.Column
import com.verity.core.ui.primitives.VerityDivider
import com.verity.core.ui.primitives.VerityDividerStrength
import com.verity.core.ui.icons.VerityIcons

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.verity.core.theme.VerityTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import com.verity.core.ui.primitives.VeritySurface
import com.verity.core.ui.primitives.VeritySurfaceType
/**
 * Chrome mode for VerityTopAppBar.
 */
sealed interface VerityChromeMode {
    object Brand : VerityChromeMode
    object Workspace : VerityChromeMode
    object Support : VerityChromeMode
}

/**
 * VerityTopAppBar
 *
 * Structural application chrome primitive.
 *
 * Responsibilities:
 * - Render title + optional subtitle
 * - Render leading navigation affordance (none / back)
 * - Render trailing actions (icons / overflow)
 *
 * Non‑responsibilities:
 * - Navigation logic
 * - Screen state decisions
 * - Business rules
 * - Draft or document awareness
 */
@Composable
fun VerityTopAppBar(
    title: String,
    subtitle: String? = null,
    chromeMode: VerityChromeMode = VerityChromeMode.Brand,
    navigationIcon: VerityNavIcon = VerityNavIcon.None,
    actions: List<VerityTopBarAction> = emptyList()
) {
    VeritySurface(
        type = VeritySurfaceType.Base
    ) {
        Column(
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
        ) {
            val navZoneWidth = 56.dp   // icon + tap target + optical gap
            val brandTitleStart = 16.dp
            val workspaceTitleStart = navZoneWidth + 8.dp

            val barHeight = when (chromeMode) {
                VerityChromeMode.Brand -> 50.dp
                VerityChromeMode.Workspace -> 60.dp
                VerityChromeMode.Support -> 50.dp
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight)
            ) {
                // Navigation overlay (start-aligned, optional)
                if ((chromeMode is VerityChromeMode.Workspace || chromeMode is VerityChromeMode.Support)
                    && navigationIcon is VerityNavIcon.Back
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(navZoneWidth),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = navigationIcon.onClick) {
                            Icon(
                                imageVector = VerityIcons.Back,
                                contentDescription = navigationIcon.contentDescription,
                                tint = VerityTheme.colors.primary
                            )
                        }
                    }
                }

                // Title anchor (fixed optical position)
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth()
                        .padding(
                            start = when (chromeMode) {
                                VerityChromeMode.Brand -> brandTitleStart
                                VerityChromeMode.Workspace -> workspaceTitleStart
                                VerityChromeMode.Support -> workspaceTitleStart
                            }
                        )
                        .padding(end = 110.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = VerityTheme.typography.chromeTitle,
                        color = VerityTheme.colors.primary
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = VerityTheme.typography.caption,
                            color = VerityTheme.colors.text.muted
                        )
                    }
                }

                // Actions overlay (end-aligned)
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    actions.forEach { action ->
                        when (action) {
                            is VerityTopBarAction.Icon -> {
                                IconButton(onClick = action.onClick) {
                                    Icon(
                                        imageVector = action.icon,
                                        contentDescription = action.contentDescription,
                                        tint = VerityTheme.colors.primary
                                    )
                                }
                            }

                            is VerityTopBarAction.Overflow -> {
                                OverflowMenu(action)
                            }
                        }
                    }
                }
            }

            VerityDivider(
                strength = VerityDividerStrength.Subtle
            )
        }
    }
}

/**
 * Navigation affordances for the top app bar.
 */
sealed interface VerityNavIcon {
    object None : VerityNavIcon

    data class Back(
        val onClick: () -> Unit,
        val contentDescription: String? = null
    ) : VerityNavIcon
}

/**
 * Trailing action models for the top app bar.
 */
sealed interface VerityTopBarAction {

    data class Icon(
        val icon: ImageVector,
        val contentDescription: String?,
        val onClick: () -> Unit
    ) : VerityTopBarAction

    data class Overflow(
        val items: List<OverflowItem>
    ) : VerityTopBarAction
}

/**
 * Overflow menu item.
 */
data class OverflowItem(
    val label: String,
    val onClick: () -> Unit
)

@Composable
private fun OverflowMenu(action: VerityTopBarAction.Overflow) {
    val expandedState = remember { mutableStateOf(false) }

    IconButton(onClick = { expandedState.value = true }) {
        Icon(
            imageVector = VerityIcons.Overflow,
            contentDescription = "More options",
            tint = VerityTheme.colors.primary
        )
    }

    DropdownMenu(
        expanded = expandedState.value,
        onDismissRequest = { expandedState.value = false }
    ) {
        action.items.forEach { item ->
            DropdownMenuItem(
                text = { Text(text = item.label) },
                onClick = {
                    expandedState.value = false
                    item.onClick()
                }
            )
        }
    }
}