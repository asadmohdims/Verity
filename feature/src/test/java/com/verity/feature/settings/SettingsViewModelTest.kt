package com.verity.feature.settings

import com.verity.core.theme.ThemeMode
import com.verity.feature.home.DocumentSummary
import com.verity.feature.home.HomeDataSource
import com.verity.feature.home.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeThemeSettingsDataSource(initial: ThemeMode = ThemeMode.SYSTEM) : ThemeSettingsDataSource {
        private val _themeMode = MutableStateFlow(initial)
        override val themeMode: StateFlow<ThemeMode> = _themeMode
        var lastSetMode: ThemeMode? = null

        override fun setThemeMode(mode: ThemeMode) {
            lastSetMode = mode
            _themeMode.value = mode
        }
    }

    private class FakeHomeDataSource(private val syncStatus: SyncStatus) : HomeDataSource {
        override suspend fun loadAllDocuments(): List<DocumentSummary> = emptyList()
        override suspend fun loadSyncStatus(): SyncStatus = syncStatus
    }

    @Test
    fun `starts with the theme mode already stored`() = runTest(dispatcher) {
        val viewModel = SettingsViewModel(
            themeSettingsDataSource = FakeThemeSettingsDataSource(ThemeMode.DARK),
            homeDataSource = FakeHomeDataSource(SyncStatus(pendingCount = 0, lastSyncedAtEpochMillis = null)),
            appVersionLabel = "1.0"
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
    }

    @Test
    fun `selecting a theme mode writes through to the data source`() = runTest(dispatcher) {
        val themeStore = FakeThemeSettingsDataSource()
        val viewModel = SettingsViewModel(
            themeSettingsDataSource = themeStore,
            homeDataSource = FakeHomeDataSource(SyncStatus(pendingCount = 0, lastSyncedAtEpochMillis = null)),
            appVersionLabel = "1.0"
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onThemeModeSelected(ThemeMode.LIGHT)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(ThemeMode.LIGHT, themeStore.lastSetMode)
        assertEquals(ThemeMode.LIGHT, viewModel.uiState.value.themeMode)
    }

    @Test
    fun `sync status label reflects a pending count`() = runTest(dispatcher) {
        val viewModel = SettingsViewModel(
            themeSettingsDataSource = FakeThemeSettingsDataSource(),
            homeDataSource = FakeHomeDataSource(SyncStatus(pendingCount = 3, lastSyncedAtEpochMillis = null)),
            appVersionLabel = "1.0"
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("3 pending", viewModel.uiState.value.syncStatusLabel)
        assertEquals(false, viewModel.uiState.value.isSynced)
    }

    @Test
    fun `isSynced is true once every write has reached the cloud mirror`() = runTest(dispatcher) {
        val viewModel = SettingsViewModel(
            themeSettingsDataSource = FakeThemeSettingsDataSource(),
            homeDataSource = FakeHomeDataSource(
                SyncStatus(pendingCount = 0, lastSyncedAtEpochMillis = System.currentTimeMillis())
            ),
            appVersionLabel = "1.0"
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.isSynced)
    }

    @Test
    fun `isSynced is false when sync has never run`() = runTest(dispatcher) {
        val viewModel = SettingsViewModel(
            themeSettingsDataSource = FakeThemeSettingsDataSource(),
            homeDataSource = FakeHomeDataSource(SyncStatus(pendingCount = 0, lastSyncedAtEpochMillis = null)),
            appVersionLabel = "1.0"
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isSynced)
    }

    @Test
    fun `app version label is passed straight through`() = runTest(dispatcher) {
        val viewModel = SettingsViewModel(
            themeSettingsDataSource = FakeThemeSettingsDataSource(),
            homeDataSource = FakeHomeDataSource(SyncStatus(pendingCount = 0, lastSyncedAtEpochMillis = null)),
            appVersionLabel = "1.0 (Milestone 1)"
        )

        assertEquals("1.0 (Milestone 1)", viewModel.uiState.value.appVersionLabel)
    }
}
