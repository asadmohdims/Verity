package com.verity.feature.customer.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CustomerDetailUiState(
    val isLoading: Boolean = true,
    val detail: CustomerDetail? = null
)

/**
 * CustomerDetailViewModel
 *
 * Scoped to one customerId, same shape as DocumentDetailViewModel — constructed per back-stack
 * entry (see AppNavShell) since it's specific to whichever customer was tapped, not a shared
 * tab-root ViewModel. refresh() lets the Route re-pull after returning from Add/Edit Customer,
 * same rationale as Documents/Home re-querying on re-entry.
 */
class CustomerDetailViewModel(
    private val customerId: String,
    private val dataSource: CustomerDetailDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerDetailUiState())
    val uiState: StateFlow<CustomerDetailUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val detail = dataSource.loadDetail(customerId)
            _uiState.value = CustomerDetailUiState(isLoading = false, detail = detail)
        }
    }
}
