package com.verity.feature.customer.rollup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CustomerRollupUiState(
    val isLoading: Boolean = true,
    val rollup: CustomerRollup? = null
)

/**
 * CustomerRollupViewModel
 *
 * Scoped to one customerId, same shape as DocumentDetailViewModel — constructed per back-stack
 * entry (see AppNavShell) since it's specific to whichever customer a search result was tapped
 * for, not a shared tab-root ViewModel.
 */
class CustomerRollupViewModel(
    private val customerId: String,
    private val dataSource: CustomerRollupDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerRollupUiState())
    val uiState: StateFlow<CustomerRollupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val rollup = dataSource.loadRollup(customerId)
            _uiState.value = CustomerRollupUiState(isLoading = false, rollup = rollup)
        }
    }
}
