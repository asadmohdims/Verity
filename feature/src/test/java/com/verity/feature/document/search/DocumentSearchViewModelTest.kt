package com.verity.feature.document.search

import com.verity.core.document.model.DocumentType
import com.verity.core.formatting.money.Money
import com.verity.feature.home.DocumentSummary
import com.verity.feature.home.HomeDataSource
import com.verity.feature.home.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * DocumentSearchViewModelTest
 *
 * Same fake-based JVM style as DocumentDetailViewModelTest. Focuses on the two things a Compose
 * screen test can't easily assert: that a blank query clears instantly (no debounce wait) and
 * that the 300ms debounce actually suppresses a search until typing pauses — collectLatest also
 * means only the last of several rapid queries ever reaches the data source.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DocumentSearchViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeHomeDataSource(private val documents: List<DocumentSummary>) : HomeDataSource {
        override suspend fun loadAllDocuments(): List<DocumentSummary> = documents
        override suspend fun loadSyncStatus(): SyncStatus = SyncStatus(pendingCount = 0, lastSyncedAtEpochMillis = null)
    }

    private class FakeDocumentSearchDataSource : DocumentSearchDataSource {
        val queriesSearched = mutableListOf<String>()
        var resultsToReturn: DocumentSearchResults =
            DocumentSearchResults(customers = emptyList(), documents = emptyList())

        override suspend fun search(query: String): DocumentSearchResults {
            queriesSearched.add(query)
            return resultsToReturn
        }
    }

    private fun testDocument(documentId: String, finalizedAt: Long): DocumentSummary = DocumentSummary(
        documentId = documentId,
        documentNumber = "INV-$documentId",
        customerName = "Test Buyer",
        documentType = DocumentType.INVOICE,
        issueDate = LocalDate.of(2026, 9, 14),
        grandTotal = Money.ofPaise(1_000),
        finalizedAtEpochMillis = finalizedAt
    )

    @Test
    fun loads_recent_documents_newest_first_on_creation() = runTest(dispatcher) {
        val documents = listOf(
            testDocument("1", finalizedAt = 100),
            testDocument("2", finalizedAt = 300),
            testDocument("3", finalizedAt = 200)
        )
        val viewModel = DocumentSearchViewModel(
            homeDataSource = FakeHomeDataSource(documents),
            searchDataSource = FakeDocumentSearchDataSource()
        )

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            listOf("2", "3", "1"),
            viewModel.uiState.value.recentDocuments.map { it.documentId }
        )
    }

    @Test
    fun a_query_does_not_search_until_the_debounce_window_elapses() = runTest(dispatcher) {
        val searchDataSource = FakeDocumentSearchDataSource()
        val viewModel = DocumentSearchViewModel(
            homeDataSource = FakeHomeDataSource(emptyList()),
            searchDataSource = searchDataSource
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onQueryChanged("cement")
        dispatcher.scheduler.advanceTimeBy(100)
        assertTrue(searchDataSource.queriesSearched.isEmpty())

        dispatcher.scheduler.advanceTimeBy(250)
        assertEquals(listOf("cement"), searchDataSource.queriesSearched)
    }

    @Test
    fun only_the_last_of_several_rapid_queries_reaches_the_data_source() = runTest(dispatcher) {
        val searchDataSource = FakeDocumentSearchDataSource()
        val viewModel = DocumentSearchViewModel(
            homeDataSource = FakeHomeDataSource(emptyList()),
            searchDataSource = searchDataSource
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onQueryChanged("c")
        dispatcher.scheduler.advanceTimeBy(50)
        viewModel.onQueryChanged("ce")
        dispatcher.scheduler.advanceTimeBy(50)
        viewModel.onQueryChanged("cement")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("cement"), searchDataSource.queriesSearched)
    }

    @Test
    fun clearing_the_query_resets_results_immediately_without_waiting_for_debounce() = runTest(dispatcher) {
        val searchDataSource = FakeDocumentSearchDataSource().apply {
            resultsToReturn = DocumentSearchResults(
                customers = listOf(CustomerSearchResult("cust-1", "Acme", "27AAACB1234Z1Z")),
                documents = emptyList()
            )
        }
        val viewModel = DocumentSearchViewModel(
            homeDataSource = FakeHomeDataSource(emptyList()),
            searchDataSource = searchDataSource
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onQueryChanged("acme")
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.results.customers.size)

        viewModel.onQueryChanged("")

        assertTrue(viewModel.uiState.value.results.customers.isEmpty())
        assertTrue(viewModel.uiState.value.results.documents.isEmpty())
    }
}
