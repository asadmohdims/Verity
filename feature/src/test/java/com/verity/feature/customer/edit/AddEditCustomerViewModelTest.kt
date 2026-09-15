package com.verity.feature.customer.edit

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddEditCustomerViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val fixedClock: Clock = Clock.fixed(Instant.parse("2026-09-15T00:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeCustomerEditDataSource(
        private val existing: Map<String, CustomerFormData> = emptyMap()
    ) : CustomerEditDataSource {
        val saved = mutableListOf<CustomerFormData>()
        val deactivated = mutableListOf<String>()

        override suspend fun loadCustomer(customerId: String): CustomerFormData? = existing[customerId]

        override suspend fun save(form: CustomerFormData) {
            saved += form
        }

        override suspend fun deactivate(customerId: String) {
            deactivated += customerId
        }
    }

    private fun requiredFieldsFilled(viewModel: AddEditCustomerViewModel) {
        viewModel.onCustomerNameChange("Ramesh Textiles")
        viewModel.onGstinChange("24AAACR5678Q1Z2")
        viewModel.onAddressLine1Change("Ring Road Industrial Estate")
        viewModel.onCityChange("Surat")
        viewModel.onStateChange("Gujarat")
        viewModel.onStateCodeChange("24")
    }

    @Test
    fun `Add mode starts blank and cannot save until required fields are filled`() = runTest(dispatcher) {
        val viewModel = AddEditCustomerViewModel(
            customerId = null,
            dataSource = FakeCustomerEditDataSource(),
            clock = fixedClock
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isEditMode)
        assertFalse(viewModel.uiState.value.canSave)

        requiredFieldsFilled(viewModel)

        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `Save in Add mode generates a fresh id and calls save exactly once`() = runTest(dispatcher) {
        val dataSource = FakeCustomerEditDataSource()
        val viewModel = AddEditCustomerViewModel(
            customerId = null,
            dataSource = dataSource,
            idGenerator = { "generated-id" },
            clock = fixedClock
        )
        dispatcher.scheduler.advanceUntilIdle()
        requiredFieldsFilled(viewModel)

        viewModel.onSave()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, dataSource.saved.size)
        assertEquals("generated-id", dataSource.saved.single().customerId)
        assertEquals("Ramesh Textiles", dataSource.saved.single().customerName)
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun `Edit mode loads the existing customer's fields`() = runTest(dispatcher) {
        val existing = CustomerFormData(
            customerId = "cust-1",
            customerName = "Sharma Enterprises",
            gstin = "27AAACS1234Z1Z",
            phone = "9999999999",
            addressLine1 = "MIDC Estate",
            city = "Pune",
            state = "Maharashtra",
            stateCode = "27",
            pincode = "411001",
            updatedAtEpochMillis = 0
        )
        val viewModel = AddEditCustomerViewModel(
            customerId = "cust-1",
            dataSource = FakeCustomerEditDataSource(mapOf("cust-1" to existing)),
            clock = fixedClock
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEditMode)
        assertEquals("Sharma Enterprises", viewModel.uiState.value.customerName)
        assertEquals("411001", viewModel.uiState.value.pincode)
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `Deactivate calls the data source and flips isDeactivated`() = runTest(dispatcher) {
        val dataSource = FakeCustomerEditDataSource(
            mapOf(
                "cust-1" to CustomerFormData(
                    customerId = "cust-1",
                    customerName = "Sharma Enterprises",
                    gstin = "27AAACS1234Z1Z",
                    phone = null,
                    addressLine1 = "MIDC Estate",
                    city = "Pune",
                    state = "Maharashtra",
                    stateCode = "27",
                    pincode = null,
                    updatedAtEpochMillis = 0
                )
            )
        )
        val viewModel = AddEditCustomerViewModel(
            customerId = "cust-1",
            dataSource = dataSource,
            clock = fixedClock
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onDeactivate()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("cust-1"), dataSource.deactivated)
        assertTrue(viewModel.uiState.value.isDeactivated)
    }
}
