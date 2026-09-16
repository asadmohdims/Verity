package com.verity.feature.document

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verity.core.document.model.DocumentType
import com.verity.feature.home.DocumentSummary
import com.verity.feature.home.HomeDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** All/Invoices/Challans — the Documents tab's type filter, applied in memory (see visibleDocuments). */
enum class DocumentTypeFilter {
    ALL,
    INVOICES,
    CHALLANS
}

data class DocumentsListUiState(
    val isLoading: Boolean = true,
    val documents: List<DocumentSummary> = emptyList(),
    val filter: DocumentTypeFilter = DocumentTypeFilter.ALL,
    /**
     * The set of documentIds pointed at by some other document's linkedDocumentId — i.e. Challans
     * with a resolved linked Invoice. Derived once in refresh() from the same documents list
     * (every DocumentSummary already carries linkedDocumentId), not a second query — see
     * DocumentsListViewModel.refresh().
     */
    val linkedToDocumentIds: Set<String> = emptySet()
) {
    /** The list DocumentsListScreen actually renders — [documents] filtered by [filter]. */
    val visibleDocuments: List<DocumentSummary>
        get() = when (filter) {
            DocumentTypeFilter.ALL -> documents
            DocumentTypeFilter.INVOICES -> documents.filter { it.documentType == DocumentType.INVOICE }
            DocumentTypeFilter.CHALLANS -> documents.filter { it.documentType == DocumentType.CHALLAN }
        }
}

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

            // Preserves the current filter across a refresh (e.g. re-entering this tab with
            // "Invoices" still selected) rather than resetting to ALL every time.
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                documents = documents,
                linkedToDocumentIds = documents.mapNotNull { it.linkedDocumentId }.toSet()
            )
        }
    }

    fun onFilterChanged(filter: DocumentTypeFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
    }
}
