package com.verity.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verity.core.theme.ThemeMode
import com.verity.feature.home.HomeDataSource
import com.verity.feature.home.syncStatusText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val syncStatusLabel: String? = null,
    /**
     * True once every local write has reached the cloud mirror (pendingCount == 0 and at least
     * one sync has actually run) — distinguishes the "Synced" success state (checkmark, matching
     * Settings.dc.html's `.settingsrow__val.ok`) from "N pending" or "never synced", neither of
     * which the design gives its own treatment.
     */
    val isSynced: Boolean = false,
    val appVersionLabel: String = ""
)

/**
 * SettingsViewModel
 *
 * Tab-root ViewModel, same shape as HomeViewModel/DocumentsListViewModel. appVersionLabel is
 * passed in from the composition root rather than read here: `feature` can't depend on `app`'s
 * generated BuildConfig (wrong dependency direction — see CLAUDE.md's module boundaries).
 */
class SettingsViewModel(
    private val themeSettingsDataSource: ThemeSettingsDataSource,
    private val homeDataSource: HomeDataSource,
    appVersionLabel: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(appVersionLabel = appVersionLabel))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            themeSettingsDataSource.themeMode.collect { mode ->
                _uiState.value = _uiState.value.copy(themeMode = mode)
            }
        }
        refreshSyncStatus()
    }

    fun refreshSyncStatus() {
        viewModelScope.launch {
            val status = homeDataSource.loadSyncStatus()
            _uiState.value = _uiState.value.copy(
                syncStatusLabel = syncStatusText(status),
                isSynced = status.pendingCount == 0 && status.lastSyncedAtEpochMillis != null
            )
        }
    }

    fun onThemeModeSelected(mode: ThemeMode) {
        themeSettingsDataSource.setThemeMode(mode)
    }
}
