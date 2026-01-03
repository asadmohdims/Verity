package com.verity.core.ui.molecules


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
    object Task : VerityChromeMode
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
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // Navigation slot (Task chrome only)
                if (chromeMode is VerityChromeMode.Task && navigationIcon is VerityNavIcon.Back) {
                    Box(
                        modifier = Modifier.width(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = navigationIcon.onClick) {
                            Icon(
                                imageVector = VerityIcons.Back,
                                contentDescription = navigationIcon.contentDescription
                            )
                        }
                    }
                }

                // Title + subtitle slot
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(
                            top = 20.dp,
                            bottom = 8.dp
                        ),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = VerityTheme.typography.chromeTitle,
                        color = VerityTheme.colors.primary
                    )

                    if (subtitle != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = VerityTheme.typography.caption,
                            color = VerityTheme.colors.text.muted
                        )
                    }
                }

                // Actions slot
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    actions.forEach { action ->
                        when (action) {
                            is VerityTopBarAction.Icon -> {
                                IconButton(onClick = action.onClick) {
                                    Icon(
                                        imageVector = action.icon,
                                        contentDescription = action.contentDescription
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
            contentDescription = "More options"
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