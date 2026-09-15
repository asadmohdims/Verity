package com.verity.platform.settings

import android.content.Context
import com.verity.core.theme.ThemeMode
import com.verity.feature.settings.ThemeSettingsDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ThemePreferenceStore
 *
 * Mirrors SyncStatusStore's shape (plain SharedPreferences, not DataStore — one small setting
 * doesn't earn a new dependency, see CLAUDE.md's "Next up" decision). The one thing
 * SharedPreferences doesn't give for free that DataStore's Flow-based API would: live
 * reactivity. Solved here with a single in-memory StateFlow, seeded from prefs at construction
 * and updated on every write — sufficient because this is a single-process app with exactly one
 * instance (constructed once at the composition root, shared between MainActivity's theme
 * resolution and SettingsViewModel), not a cross-process listener problem. Implements
 * ThemeSettingsDataSource directly (see that interface's doc comment) so MainActivity can hand
 * this same instance to SettingsViewModel across the feature/platform boundary.
 */
class ThemePreferenceStore(context: Context) : ThemeSettingsDataSource {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(readStoredMode())
    override val themeMode: StateFlow<ThemeMode> = _themeMode

    override fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    private fun readStoredMode(): ThemeMode {
        val stored = prefs.getString(KEY_THEME_MODE, null) ?: return ThemeMode.SYSTEM
        return runCatching { ThemeMode.valueOf(stored) }.getOrDefault(ThemeMode.SYSTEM)
    }

    private companion object {
        const val PREFS_NAME = "verity_settings"
        const val KEY_THEME_MODE = "theme_mode"
    }
}
