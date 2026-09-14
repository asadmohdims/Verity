package com.verity.feature.document

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verity.feature.home.DocumentSummary
import com.verity.feature.home.HomeDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DocumentsListUiState(
    val isLoading: Boolean = true,
    val documents: List<DocumentSummary> = emptyList()
)

/**
 * DocumentsListViewModel
 *
 * Screen-level controller for the Documents tab. Reuses HomeDataSource — it already "deliberately
 * returns every document rather than a pre-filtered/pre-limited set" (see HomeDataSource's own
 * doc comment), so the full-list Documents tab needs no new query, just a different in-memory
 * shaping (newest-first, unlimited) than Home's "recent N" via HomeDashboardCalculator.
 *
 * refresh() is re-invoked by the Route each time this destination becomes current, same rationale
 * as HomeViewModel: a document finalized on another tab must show up here without a fresh
 * process/ViewModel.
 */
class DocumentsListViewModel(
    private val homeDataSource: HomeDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentsListUiState())
    val uiState: StateFlow<DocumentsListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val documents = homeDataSource.loadAllDocuments()
                .sortedByDescending { it.finalizedAtEpochMillis }

            _uiState.value = DocumentsListUiState(
                isLoading = false,
                documents = documents
            )
        }
    }
}
