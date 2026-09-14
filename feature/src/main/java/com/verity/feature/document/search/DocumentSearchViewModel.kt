package com.verity.feature.document.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verity.feature.home.DocumentSummary
import com.verity.feature.home.HomeDataSource
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private const val SEARCH_DEBOUNCE_MILLIS = 300L
private const val MAX_RECENT_DOCUMENTS = 8

private val EMPTY_RESULTS = DocumentSearchResults(customers = emptyList(), documents = emptyList())

data class DocumentSearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val recentDocuments: List<DocumentSummary> = emptyList(),
    val results: DocumentSearchResults = EMPTY_RESULTS
)

/**
 * DocumentSearchViewModel
 *
 * Backs the Documents search screen. Reuses HomeDataSource for the empty-query "Recent" state —
 * the exact same finalizedAt-sorted list Home/Documents-tab already produce, no new query.
 * Non-empty queries go through DocumentSearchDataSource, debounced 300ms: `Flow.debounce()` waits
 * for typing to pause before firing a search, which is what actually keeps fast typing feeling
 * responsive rather than a network/DB round trip firing on every keystroke.
 */
@OptIn(FlowPreview::class)
class DocumentSearchViewModel(
    private val homeDataSource: HomeDataSource,
    private val searchDataSource: DocumentSearchDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentSearchUiState())
    val uiState: StateFlow<DocumentSearchUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            val recent = homeDataSource.loadAllDocuments()
                .sortedByDescending { it.finalizedAtEpochMillis }
                .take(MAX_RECENT_DOCUMENTS)
            _uiState.value = _uiState.value.copy(recentDocuments = recent)
        }

        viewModelScope.launch {
            queryFlow
                .debounce(SEARCH_DEBOUNCE_MILLIS)
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.isBlank()) return@collectLatest

                    _uiState.value = _uiState.value.copy(isSearching = true)
                    val results = searchDataSource.search(query)
                    _uiState.value = _uiState.value.copy(isSearching = false, results = results)
                }
        }
    }

    fun onQueryChanged(query: String) {
        val blank = query.isBlank()
        _uiState.value = _uiState.value.copy(
            query = query,
            // A cleared query should feel instant, not wait out the debounce below.
            isSearching = if (blank) false else _uiState.value.isSearching,
            results = if (blank) EMPTY_RESULTS else _uiState.value.results
        )
        queryFlow.value = query
    }
}
