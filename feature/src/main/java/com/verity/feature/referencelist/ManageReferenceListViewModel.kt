package com.verity.feature.referencelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ManageReferenceListUiState(
    val isLoading: Boolean = true,
    val items: List<ReferenceListItem> = emptyList(),
    val newValueInput: String = ""
)

/**
 * ManageReferenceListViewModel
 *
 * Backs the "Transporter Names" / "HSN Codes" management screens reached from Settings — same
 * ViewModel and screen serve both, parameterized by [kind], since the two lists are structurally
 * identical (see ReferenceListEntity's doc comment).
 */
class ManageReferenceListViewModel(
    private val kind: ReferenceListKind,
    private val dataSource: ReferenceListDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManageReferenceListUiState())
    val uiState: StateFlow<ManageReferenceListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val items = dataSource.getAll(kind)
            _uiState.value = _uiState.value.copy(isLoading = false, items = items)
        }
    }

    fun onNewValueInputChanged(value: String) {
        _uiState.value = _uiState.value.copy(newValueInput = value)
    }

    fun onAdd() {
        val value = _uiState.value.newValueInput.trim()
        if (value.isEmpty()) return

        viewModelScope.launch {
            dataSource.add(kind, value)
            _uiState.value = _uiState.value.copy(newValueInput = "")
            refresh()
        }
    }

    fun onDelete(id: String) {
        viewModelScope.launch {
            dataSource.delete(kind, id)
            refresh()
        }
    }
}
