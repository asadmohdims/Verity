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
            _uiState.value = _uiState.value.copy(syncStatusLabel = syncStatusText(status))
        }
    }

    fun onThemeModeSelected(mode: ThemeMode) {
        themeSettingsDataSource.setThemeMode(mode)
    }
}
