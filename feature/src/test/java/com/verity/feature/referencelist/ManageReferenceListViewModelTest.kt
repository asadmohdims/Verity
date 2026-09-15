package com.verity.feature.referencelist

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ManageReferenceListViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeReferenceListDataSource(
        initial: List<ReferenceListItem> = emptyList()
    ) : ReferenceListDataSource {
        private val items = initial.toMutableList()
        var lastAddedKind: ReferenceListKind? = null

        override suspend fun getAll(kind: ReferenceListKind): List<ReferenceListItem> = items.toList()

        override suspend fun add(kind: ReferenceListKind, value: String) {
            lastAddedKind = kind
            items.add(ReferenceListItem(id = "id-${items.size}", value = value))
        }

        override suspend fun delete(kind: ReferenceListKind, id: String) {
            items.removeAll { it.id == id }
        }
    }

    @Test
    fun `starts by loading every existing item for the given kind`() = runTest(dispatcher) {
        val dataSource = FakeReferenceListDataSource(
            initial = listOf(ReferenceListItem(id = "t-1", value = "MZN Transport"))
        )
        val viewModel = ManageReferenceListViewModel(
            kind = ReferenceListKind.TRANSPORTER_NAME,
            dataSource = dataSource
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("MZN Transport"), viewModel.uiState.value.items.map { it.value })
        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    @Test
    fun `onAdd adds the trimmed input value and clears the input field`() = runTest(dispatcher) {
        val dataSource = FakeReferenceListDataSource()
        val viewModel = ManageReferenceListViewModel(
            kind = ReferenceListKind.HSN_CODE,
            dataSource = dataSource
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onNewValueInputChanged("  7208  ")
        viewModel.onAdd()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("7208"), viewModel.uiState.value.items.map { it.value })
        assertEquals("", viewModel.uiState.value.newValueInput)
        assertEquals(ReferenceListKind.HSN_CODE, dataSource.lastAddedKind)
    }

    @Test
    fun `onAdd with a blank input does nothing`() = runTest(dispatcher) {
        val dataSource = FakeReferenceListDataSource()
        val viewModel = ManageReferenceListViewModel(
            kind = ReferenceListKind.HSN_CODE,
            dataSource = dataSource
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onNewValueInputChanged("   ")
        viewModel.onAdd()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(emptyList<String>(), viewModel.uiState.value.items.map { it.value })
    }

    @Test
    fun `onDelete removes the item and refreshes the list`() = runTest(dispatcher) {
        val dataSource = FakeReferenceListDataSource(
            initial = listOf(
                ReferenceListItem(id = "t-1", value = "MZN Transport"),
                ReferenceListItem(id = "t-2", value = "Sharma Roadways")
            )
        )
        val viewModel = ManageReferenceListViewModel(
            kind = ReferenceListKind.TRANSPORTER_NAME,
            dataSource = dataSource
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onDelete("t-1")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("Sharma Roadways"), viewModel.uiState.value.items.map { it.value })
    }
}
