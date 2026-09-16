package com.verity.feature.customer.detail

import com.verity.core.formatting.money.Money
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CustomerDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeCustomerDetailDataSource(
        private val details: Map<String, CustomerDetail>
    ) : CustomerDetailDataSource {
        var loadCount = 0
        override suspend fun loadDetail(customerId: String): CustomerDetail? {
            loadCount++
            return details[customerId]
        }
    }

    private fun sampleDetail(id: String) = CustomerDetail(
        customerId = id,
        customerName = "Acme Traders",
        phone = null,
        gstin = "27AAACB1234Z1Z",
        addressLine1 = "Industrial Area",
        city = "Mumbai",
        state = "Maharashtra",
        stateCode = "27",
        pincode = "400001",
        notes = null,
        balanceDue = Money.ofPaise(1_000_00),
        documents = emptyList()
    )

    @Test
    fun `loads the detail matching the given id`() = runTest(dispatcher) {
        val dataSource = FakeCustomerDetailDataSource(mapOf("cust-1" to sampleDetail("cust-1")))

        val viewModel = CustomerDetailViewModel(customerId = "cust-1", dataSource = dataSource)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Acme Traders", viewModel.uiState.value.detail?.customerName)
    }

    @Test
    fun `exposes null when no customer matches the id`() = runTest(dispatcher) {
        val dataSource = FakeCustomerDetailDataSource(emptyMap())

        val viewModel = CustomerDetailViewModel(customerId = "missing", dataSource = dataSource)
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.detail)
    }

    @Test
    fun `refresh re-queries the data source`() = runTest(dispatcher) {
        val dataSource = FakeCustomerDetailDataSource(mapOf("cust-1" to sampleDetail("cust-1")))
        val viewModel = CustomerDetailViewModel(customerId = "cust-1", dataSource = dataSource)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, dataSource.loadCount)
    }
}
