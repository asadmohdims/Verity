package com.verity.feature.customer.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CustomersListUiState(
    val isLoading: Boolean = true,
    val customers: List<CustomerListItem> = emptyList()
)

/**
 * CustomersListViewModel
 *
 * Tab-root ViewModel (no id), same shape as DocumentsListViewModel — constructed once at the
 * composition root, not per back-stack entry. refresh() is re-invoked by the Route each time this
 * destination becomes current, so a customer added/edited from this tab shows up immediately on
 * return.
 */
class CustomersListViewModel(
    private val dataSource: CustomerListDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomersListUiState())
    val uiState: StateFlow<CustomersListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val customers = dataSource.loadCustomers()
            _uiState.value = CustomersListUiState(isLoading = false, customers = customers)
        }
    }
}
