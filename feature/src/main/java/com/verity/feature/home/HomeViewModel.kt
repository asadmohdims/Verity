package com.verity.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verity.core.formatting.money.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

data class HomeUiState(
    val isLoading: Boolean = true,
    val thisMonthTotal: Money = Money.ofPaise(0),
    val thisMonthDocumentCount: Int = 0,
    val thisMonthLabel: String = "",
    val recentDocuments: List<DocumentSummary> = emptyList(),
    val syncStatus: SyncStatus = SyncStatus(pendingCount = 0, lastSyncedAtEpochMillis = null)
)

/**
 * HomeViewModel
 *
 * Screen-level controller for the Home dashboard.
 *
 * Responsibilities:
 * - Load documents via HomeDataSource and aggregate them via HomeDashboardCalculator
 * - Expose immutable UI state for observation
 *
 * Non-responsibilities:
 * - No persistence, no navigation, no UI logic
 *
 * refresh() is called from init AND re-invoked by the Route each time Home becomes the visible
 * destination (see HomeRoute) — a finalized invoice created while on another tab must show up
 * here without requiring a fresh process/ViewModel.
 */
class HomeViewModel(
    private val homeDataSource: HomeDataSource,
    private val clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val documents = homeDataSource.loadAllDocuments()
            val dashboard = HomeDashboardCalculator.buildDashboard(
                documents = documents,
                referenceDate = LocalDate.now(clock)
            )
            val syncStatus = homeDataSource.loadSyncStatus()

            _uiState.value = HomeUiState(
                isLoading = false,
                thisMonthTotal = dashboard.thisMonthTotal,
                thisMonthDocumentCount = dashboard.thisMonthDocumentCount,
                thisMonthLabel = dashboard.thisMonthLabel,
                recentDocuments = dashboard.recentDocuments,
                syncStatus = syncStatus
            )
        }
    }
}
