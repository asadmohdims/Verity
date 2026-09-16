package com.verity.feature.customer.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Clock
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddEditCustomerUiState(
    val isLoading: Boolean = false,
    val isEditMode: Boolean = false,
    val customerName: String = "",
    val gstin: String = "",
    val phone: String = "",
    val addressLine1: String = "",
    val city: String = "",
    val state: String = "",
    val stateCode: String = "",
    val pincode: String = "",
    val notes: String = "",
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val isDeactivated: Boolean = false
) {
    /** Matches the mockup's required fields: Business Name, GSTIN, Address, City, State, State Code. */
    val canSave: Boolean
        get() = customerName.isNotBlank() && gstin.isNotBlank() && addressLine1.isNotBlank() &&
            city.isNotBlank() && state.isNotBlank() && stateCode.isNotBlank()
}

/**
 * AddEditCustomerViewModel
 *
 * One screen, two modes — customerId == null is Add, non-null is Edit (per Single Ownership,
 * CLAUDE.md Principle 3: every field's value lives here in local UI state, never read
 * conditionally from the loaded CustomerFormData once editing starts). Constructed per
 * back-stack entry, same per-id pattern as CustomerDetailViewModel/DocumentDetailViewModel.
 */
class AddEditCustomerViewModel(
    private val customerId: String?,
    private val dataSource: CustomerEditDataSource,
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AddEditCustomerUiState(isLoading = customerId != null, isEditMode = customerId != null)
    )
    val uiState: StateFlow<AddEditCustomerUiState> = _uiState.asStateFlow()

    init {
        val id = customerId
        if (id != null) {
            viewModelScope.launch {
                val existing = dataSource.loadCustomer(id)
                _uiState.value = if (existing != null) {
                    _uiState.value.copy(
                        isLoading = false,
                        customerName = existing.customerName,
                        gstin = existing.gstin,
                        phone = existing.phone.orEmpty(),
                        addressLine1 = existing.addressLine1,
                        city = existing.city,
                        state = existing.state,
                        stateCode = existing.stateCode,
                        pincode = existing.pincode.orEmpty(),
                        notes = existing.notes.orEmpty()
                    )
                } else {
                    _uiState.value.copy(isLoading = false)
                }
            }
        }
    }

    fun onCustomerNameChange(value: String) {
        _uiState.value = _uiState.value.copy(customerName = value)
    }

    fun onGstinChange(value: String) {
        _uiState.value = _uiState.value.copy(gstin = value)
    }

    fun onPhoneChange(value: String) {
        _uiState.value = _uiState.value.copy(phone = value)
    }

    fun onAddressLine1Change(value: String) {
        _uiState.value = _uiState.value.copy(addressLine1 = value)
    }

    fun onCityChange(value: String) {
        _uiState.value = _uiState.value.copy(city = value)
    }

    fun onStateChange(value: String) {
        _uiState.value = _uiState.value.copy(state = value)
    }

    fun onStateCodeChange(value: String) {
        _uiState.value = _uiState.value.copy(stateCode = value)
    }

    fun onPincodeChange(value: String) {
        _uiState.value = _uiState.value.copy(pincode = value)
    }

    fun onNotesChange(value: String) {
        _uiState.value = _uiState.value.copy(notes = value)
    }

    fun onSave() {
        val state = _uiState.value
        if (!state.canSave || state.isSaving) return

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true)
            dataSource.save(
                CustomerFormData(
                    customerId = customerId ?: idGenerator(),
                    customerName = state.customerName.trim(),
                    gstin = state.gstin.trim(),
                    phone = state.phone.trim().ifBlank { null },
                    addressLine1 = state.addressLine1.trim(),
                    city = state.city.trim(),
                    state = state.state.trim(),
                    stateCode = state.stateCode.trim(),
                    pincode = state.pincode.trim().ifBlank { null },
                    updatedAtEpochMillis = clock.millis(),
                    notes = state.notes.trim().ifBlank { null }
                )
            )
            _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true)
        }
    }

    fun onDeactivate() {
        val id = customerId ?: return
        viewModelScope.launch {
            dataSource.deactivate(id)
            _uiState.value = _uiState.value.copy(isDeactivated = true)
        }
    }
}
