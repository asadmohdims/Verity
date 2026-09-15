package com.verity.feature.settings

import com.verity.core.theme.ThemeMode
import kotlinx.coroutines.flow.StateFlow

/**
 * ThemeSettingsDataSource
 *
 * Port for reading/writing the user's theme preference. `platform`'s ThemePreferenceStore is the
 * one implementation — its own public surface (StateFlow + a plain setter, no Android types) is
 * already platform-agnostic, so it implements this interface directly rather than needing a
 * separate Default* adapter class, unlike most other ports in this codebase.
 */
interface ThemeSettingsDataSource {
    val themeMode: StateFlow<ThemeMode>
    fun setThemeMode(mode: ThemeMode)
}
