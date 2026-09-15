package com.verity.feature.customer.list

import com.verity.core.formatting.money.Money
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CustomersListViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeCustomerListDataSource(private val customers: List<CustomerListItem>) : CustomerListDataSource {
        var loadCount = 0
        override suspend fun loadCustomers(): List<CustomerListItem> {
            loadCount++
            return customers
        }
    }

    private fun sampleCustomer(id: String, balancePaise: Long) = CustomerListItem(
        customerId = id,
        customerName = "Customer $id",
        city = "Mumbai",
        state = "Maharashtra",
        balanceDue = Money.ofPaise(balancePaise)
    )

    @Test
    fun `loads customers on init`() = runTest(dispatcher) {
        val dataSource = FakeCustomerListDataSource(listOf(sampleCustomer("1", 100_00)))

        val viewModel = CustomersListViewModel(dataSource = dataSource)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.customers.size)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `refresh re-queries the data source`() = runTest(dispatcher) {
        val dataSource = FakeCustomerListDataSource(emptyList())
        val viewModel = CustomersListViewModel(dataSource = dataSource)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, dataSource.loadCount)
    }

    @Test
    fun `a zero balance customer is settled`() {
        val customer = sampleCustomer("1", 0)
        assertEquals(true, customer.isSettled)
    }

    @Test
    fun `a positive balance customer is not settled`() {
        val customer = sampleCustomer("1", 1)
        assertEquals(false, customer.isSettled)
    }
}
