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
import androidx.compose.ui.res.painterResource
import com.verity.core.ui.icons.VerityIcon
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
    /**
     * Brand-tier chrome (no back arrow), used by all four bottom-nav root screens.
     *
     * [isEntrySurface] distinguishes Home's big splash title (Main.dc.html's 24px/800
     * `.topbar-brand__title`) from Documents/Customers/Settings' smaller, plain one
     * (DocumentsList.dc.html/CustomersList.dc.html/Settings.dc.html's 18px/600
     * `.topbar-brand2__title`) — the mockups only ever gave Home the large splash treatment,
     * the other three tabs you navigate *to* read as plain section headers.
     */
    data class Brand(val isEntrySurface: Boolean = false) : VerityChromeMode
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
    chromeMode: VerityChromeMode = VerityChromeMode.Brand(),
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
                is VerityChromeMode.Brand -> 50.dp
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
                            when (val icon = VerityIcons.Back) {
                                is VerityIcon.Material -> Icon(
                                    imageVector = icon.imageVector,
                                    contentDescription = navigationIcon.contentDescription,
                                    tint = VerityTheme.colors.primary
                                )
                                is VerityIcon.VectorRes -> Icon(
                                    painter = painterResource(icon.resId),
                                    contentDescription = navigationIcon.contentDescription,
                                    tint = VerityTheme.colors.primary
                                )
                            }
                        }
                    }
                }

                // Title anchor (fixed optical position)
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth()
                        .padding(
                            start = if (navigationIcon is VerityNavIcon.Back) {
                                workspaceTitleStart
                            } else {
                                brandTitleStart
                            }
                        )
                        .padding(end = 110.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Only Home (isEntrySurface) gets the large chromeTitle splash style, matching
                    // Main.dc.html's 24px/800 `.topbar-brand__title`. Documents/Customers/Settings
                    // (Brand, not an entry surface) and Workspace/Support ("Invoice", "Invoice
                    // Preview", ...) all use the smaller Title style — matches
                    // DocumentsList.dc.html/CustomersList.dc.html/Settings.dc.html's 18px/600
                    // `.topbar-brand2__title` and InvoiceWorkspace.dc.html/Preview.dc.html's
                    // matching `.topbar-ws__title`/`.topbar-sp__title`.
                    val isEntrySurface = chromeMode is VerityChromeMode.Brand && chromeMode.isEntrySurface

                    val titleStyle = if (isEntrySurface) {
                        VerityTheme.typography.chromeTitle
                    } else {
                        VerityTheme.typography.title
                    }

                    // Only the entry-surface splash title (Home) is brand-colored, matching
                    // Main.dc.html's `.topbar-brand__title{color:var(--primary)}`; the other three
                    // Brand-tier tabs use plain text-primary, matching `.topbar-brand2__title`.
                    // Workspace/Support keep the existing primary-tinted title unchanged here —
                    // out of scope for this pass (not part of what was flagged/approved).
                    val titleColor = if (chromeMode is VerityChromeMode.Brand && !isEntrySurface) {
                        VerityTheme.colors.text.primary
                    } else {
                        VerityTheme.colors.primary
                    }

                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = titleStyle,
                        color = titleColor
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
                                val tint = if (action.enabled) {
                                    VerityTheme.colors.primary
                                } else {
                                    VerityTheme.colors.text.disabled
                                }

                                IconButton(
                                    onClick = action.onClick,
                                    enabled = action.enabled
                                ) {
                                    when (val icon = action.icon) {
                                        is VerityIcon.Material -> Icon(
                                            imageVector = icon.imageVector,
                                            contentDescription = action.contentDescription,
                                            tint = tint
                                        )
                                        is VerityIcon.VectorRes -> Icon(
                                            painter = painterResource(icon.resId),
                                            contentDescription = action.contentDescription,
                                            tint = tint
                                        )
                                    }
                                }
                            }

                            is VerityTopBarAction.Overflow -> {
                                OverflowMenu(action)
                            }
                        }
                    }
                }
            }

            // Solid borders.divider, matching the mockup's `.topbar-brand{border-bottom:1px solid
            // var(--border-divider)}` — the alpha-derived Subtle divider renders visibly fainter.
            VerityDivider(
                strength = VerityDividerStrength.Divider
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
        val icon: VerityIcon,
        val contentDescription: String?,
        val onClick: () -> Unit,
        val enabled: Boolean = true
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
        when (val icon = VerityIcons.Overflow) {
            is VerityIcon.Material -> Icon(
                imageVector = icon.imageVector,
                contentDescription = "More options",
                tint = VerityTheme.colors.primary
            )
            is VerityIcon.VectorRes -> Icon(
                painter = painterResource(icon.resId),
                contentDescription = "More options",
                tint = VerityTheme.colors.primary
            )
        }
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