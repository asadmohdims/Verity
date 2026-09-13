package com.verity.core.ui.molecules

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.verity.core.theme.VerityTheme
import com.verity.core.ui.icons.VerityIcon
import com.verity.core.ui.icons.VerityIconGlyph
import com.verity.core.ui.primitives.VerityDivider
import com.verity.core.ui.primitives.VerityDividerStrength

/**
 * One bottom-navigation destination.
 */
data class VerityBottomNavItem(
    val route: String,
    val label: String,
    val icon: VerityIcon
)

/**
 * VerityBottomNav
 *
 * Structural application chrome primitive for the four Brand-mode root destinations (Home,
 * Documents, Customers, Settings — see CLAUDE.md's "UX direction"). Parallel in intent to
 * VerityTopAppBar: wraps a raw Material3 component (NavigationBar) entirely in Verity's own
 * semantic tokens rather than trusting Material3's default color scheme.
 *
 * The label uses raw Material3 Text, not VerityText, deliberately: NavigationBarItem drives
 * selected/unselected coloring via ambient LocalContentColor, and VerityText always forces its
 * own style-based color, which would silently defeat that state-driven tinting.
 */
@Composable
fun VerityBottomNav(
    items: List<VerityBottomNavItem>,
    selectedRoute: String?,
    onSelect: (String) -> Unit
) {
    Column {
        // Matches the mockup's `.bottomnav{border-top:1px solid var(--border-divider)}` — plain
        // M3 NavigationBar draws no top border of its own.
        VerityDivider(strength = VerityDividerStrength.Divider)

        NavigationBar(
            containerColor = VerityTheme.colors.surface.base,
            contentColor = VerityTheme.colors.text.muted
        ) {
            items.forEach { item ->
                NavigationBarItem(
                    selected = item.route == selectedRoute,
                    onClick = { onSelect(item.route) },
                    icon = {
                        VerityIconGlyph(icon = item.icon, contentDescription = item.label)
                    },
                    label = {
                        Text(text = item.label, style = VerityTheme.typography.caption)
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = VerityTheme.colors.primary,
                        selectedTextColor = VerityTheme.colors.primary,
                        // Transparent, not a tinted pill: the mockup's `.navitem.active` only
                        // recolors the icon/label (see Main.dc.html) — there's no background
                        // behind it.
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = VerityTheme.colors.text.muted,
                        unselectedTextColor = VerityTheme.colors.text.muted
                    )
                )
            }
        }
    }
}
