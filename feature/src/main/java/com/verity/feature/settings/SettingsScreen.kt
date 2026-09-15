package com.verity.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verity.core.document.model.HARDCODED_SELLER
import com.verity.core.theme.ThemeMode
import com.verity.core.theme.VerityTheme
import com.verity.core.ui.icons.VerityIconGlyph
import com.verity.core.ui.icons.VerityIcons
import com.verity.core.ui.molecules.VerityListItem
import com.verity.core.ui.molecules.VeritySegmentedControl
import com.verity.core.ui.primitives.VerityDivider
import com.verity.core.ui.primitives.VerityDividerStrength
import com.verity.core.ui.primitives.VeritySpace
import com.verity.core.ui.primitives.VeritySpacer
import com.verity.core.ui.primitives.VeritySurface
import com.verity.core.ui.primitives.VeritySurfaceType
import com.verity.core.ui.primitives.VerityText
import com.verity.core.ui.primitives.VerityTextStyle
import com.verity.core.ui.primitives.dp

/**
 * SettingsRoute
 *
 * Re-checks sync status each time this destination becomes current, same rationale as
 * Documents/Home. Theme mode itself is collected live from ThemeSettingsDataSource inside the
 * ViewModel (not refreshed here), since it can change instantly the moment the user taps a
 * segment.
 */
@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onManageTransporterNames: () -> Unit,
    onManageHsnCodes: () -> Unit,
    onManageUnits: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshSyncStatus()
    }

    SettingsScreen(
        state = state,
        onThemeModeSelected = viewModel::onThemeModeSelected,
        onManageTransporterNames = onManageTransporterNames,
        onManageHsnCodes = onManageHsnCodes,
        onManageUnits = onManageUnits
    )
}

/**
 * SettingsScreen
 *
 * Pure renderer — never obtains a ViewModel, never collects a Flow directly (see CLAUDE.md's
 * Route/Screen/ViewModel ownership rule). The Business Profile row shows the real hardcoded
 * seller name but has no onClick — no Business Profile screen exists yet (undesigned, deferred to
 * its own design pass), so it ships visible-but-inert this round rather than a dead navigation.
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onManageTransporterNames: () -> Unit,
    onManageHsnCodes: () -> Unit,
    onManageUnits: () -> Unit,
    modifier: Modifier = Modifier
) {
    VeritySurface(
        type = VeritySurfaceType.Base,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(VeritySpace.Medium.dp)
        ) {
            SectionLabel(text = "Business")
            VeritySurface(type = VeritySurfaceType.Card) {
                VerityListItem(
                    title = "Business Profile",
                    subtitle = null,
                    trailing = {
                        VerityText(text = HARDCODED_SELLER.name, style = VerityTextStyle.Caption)
                        VeritySpacer(size = VeritySpace.ExtraSmall, horizontal = true)
                        VerityIconGlyph(icon = VerityIcons.ChevronRight, contentDescription = null)
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)
                )
            }

            VeritySpacer(size = VeritySpace.Large)
            SectionLabel(text = "Reference Lists")
            VeritySurface(type = VeritySurfaceType.Card) {
                Column {
                    VerityListItem(
                        title = "Transporter Names",
                        trailing = {
                            VerityIconGlyph(icon = VerityIcons.ChevronRight, contentDescription = null)
                        },
                        modifier = Modifier
                            .clickable(onClick = onManageTransporterNames)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 14.dp)
                    )
                    VerityDivider(strength = VerityDividerStrength.Divider)
                    VerityListItem(
                        title = "HSN Codes",
                        trailing = {
                            VerityIconGlyph(icon = VerityIcons.ChevronRight, contentDescription = null)
                        },
                        modifier = Modifier
                            .clickable(onClick = onManageHsnCodes)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 14.dp)
                    )
                    VerityDivider(strength = VerityDividerStrength.Divider)
                    VerityListItem(
                        title = "Units",
                        trailing = {
                            VerityIconGlyph(icon = VerityIcons.ChevronRight, contentDescription = null)
                        },
                        modifier = Modifier
                            .clickable(onClick = onManageUnits)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 14.dp)
                    )
                }
            }

            VeritySpacer(size = VeritySpace.Large)
            SectionLabel(text = "Appearance")
            VeritySurface(type = VeritySurfaceType.Card) {
                Column(modifier = Modifier.padding(VeritySpace.Medium.dp)) {
                    VerityText(text = "Theme", style = VerityTextStyle.Body)
                    VeritySpacer(size = VeritySpace.Small)
                    VeritySegmentedControl(
                        options = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK),
                        selected = state.themeMode,
                        labelFor = { mode ->
                            when (mode) {
                                ThemeMode.SYSTEM -> "System"
                                ThemeMode.LIGHT -> "Light"
                                ThemeMode.DARK -> "Dark"
                            }
                        },
                        onSelect = onThemeModeSelected
                    )
                }
            }

            if (state.syncStatusLabel != null) {
                VeritySpacer(size = VeritySpace.Large)
                SectionLabel(text = "Data")
                VeritySurface(type = VeritySurfaceType.Card) {
                    VerityListItem(
                        title = "Sync status",
                        trailing = { SyncStatusValue(label = state.syncStatusLabel, isSynced = state.isSynced) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 14.dp)
                    )
                }
            }

            VeritySpacer(size = VeritySpace.Large)
            SectionLabel(text = "About")
            VeritySurface(type = VeritySurfaceType.Card) {
                VerityListItem(
                    title = "Version",
                    trailing = { VerityText(text = state.appVersionLabel, style = VerityTextStyle.Caption) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 14.dp)
                )
            }
        }
    }
}

/**
 * Matches Settings.dc.html's `.sectiontitle` (12px/700/uppercase/0.03em letter-spacing/muted).
 * Goes through raw Material3 Text rather than VerityText/VerityTextStyle.Label: Label is also
 * used as a plain (non-uppercase) field label elsewhere (e.g. InvoicePreviewScreen's "Billed
 * To"), so this small-caps treatment can't be folded into it without changing those other call
 * sites too — same rationale as VeritySegmentedControl's own raw-Text usage.
 */
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = TextStyle(
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.36.sp
        ),
        color = VerityTheme.colors.text.muted
    )
    VeritySpacer(size = VeritySpace.Small)
}

/**
 * Matches Settings.dc.html's `.settingsrow__val.ok` (checkmark + success color) for the synced
 * case; plain caption text otherwise (the design doesn't specify a "pending" treatment).
 */
@Composable
private fun SyncStatusValue(label: String, isSynced: Boolean) {
    if (isSynced) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CompositionLocalProvider(LocalContentColor provides VerityTheme.colors.state.success) {
                VerityIconGlyph(
                    icon = VerityIcons.FinalizedCheck,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
            VeritySpacer(size = VeritySpace.ExtraSmall, horizontal = true)
            Text(
                text = label,
                style = VerityTheme.typography.caption,
                color = VerityTheme.colors.state.success
            )
        }
    } else {
        VerityText(text = label, style = VerityTextStyle.Caption)
    }
}
