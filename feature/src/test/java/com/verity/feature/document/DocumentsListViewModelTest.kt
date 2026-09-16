package com.verity.feature.document

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
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DocumentsListViewModelTest {

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

    private fun document(
        id: String,
        type: DocumentType,
        linkedDocumentId: String? = null,
        finalizedAt: Long = 0L
    ) = DocumentSummary(
        documentId = id,
        documentNumber = if (type == DocumentType.INVOICE) "INV-$id" else "CH-$id",
        customerName = "Test Buyer",
        documentType = type,
        issueDate = LocalDate.of(2026, 9, 14),
        grandTotal = Money.ofPaise(10_000),
        finalizedAtEpochMillis = finalizedAt,
        linkedDocumentId = linkedDocumentId
    )

    @Test
    fun `refresh sorts newest first and defaults the filter to ALL`() = runTest(dispatcher) {
        val older = document("1", DocumentType.INVOICE, finalizedAt = 100)
        val newer = document("2", DocumentType.CHALLAN, finalizedAt = 200)
        val viewModel = DocumentsListViewModel(FakeHomeDataSource(listOf(older, newer)))

        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(newer, older), state.documents)
        assertEquals(DocumentTypeFilter.ALL, state.filter)
    }

    @Test
    fun `refresh derives linkedToDocumentIds from linkedDocumentId across all documents`() = runTest(dispatcher) {
        val challan = document("challan-1", DocumentType.CHALLAN)
        val invoice = document("invoice-1", DocumentType.INVOICE, linkedDocumentId = "challan-1")
        val viewModel = DocumentsListViewModel(FakeHomeDataSource(listOf(challan, invoice)))

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(setOf("challan-1"), viewModel.uiState.value.linkedToDocumentIds)
    }

    @Test
    fun `onFilterChanged updates the filter without re-fetching`() = runTest(dispatcher) {
        val invoice = document("1", DocumentType.INVOICE)
        val viewModel = DocumentsListViewModel(FakeHomeDataSource(listOf(invoice)))
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onFilterChanged(DocumentTypeFilter.CHALLANS)

        assertEquals(DocumentTypeFilter.CHALLANS, viewModel.uiState.value.filter)
        assertEquals(emptyList<DocumentSummary>(), viewModel.uiState.value.visibleDocuments)
    }

    @Test
    fun `a subsequent refresh preserves the currently selected filter`() = runTest(dispatcher) {
        val invoice = document("1", DocumentType.INVOICE)
        val viewModel = DocumentsListViewModel(FakeHomeDataSource(listOf(invoice)))
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onFilterChanged(DocumentTypeFilter.INVOICES)

        viewModel.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(DocumentTypeFilter.INVOICES, viewModel.uiState.value.filter)
    }

    @Test
    fun `a long press selects a document and enters selection mode`() = runTest(dispatcher) {
        val invoice = document("1", DocumentType.INVOICE)
        val viewModel = DocumentsListViewModel(FakeHomeDataSource(listOf(invoice)))
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onDocumentLongPress("1")

        assertEquals(setOf("1"), viewModel.uiState.value.selectedDocumentIds)
        assertEquals(true, viewModel.uiState.value.isSelectionMode)
    }

    @Test
    fun `toggling an already-selected document deselects it and exits selection mode`() = runTest(dispatcher) {
        val invoice = document("1", DocumentType.INVOICE)
        val viewModel = DocumentsListViewModel(FakeHomeDataSource(listOf(invoice)))
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onDocumentLongPress("1")

        viewModel.onToggleSelection("1")

        assertEquals(emptySet<String>(), viewModel.uiState.value.selectedDocumentIds)
        assertEquals(false, viewModel.uiState.value.isSelectionMode)
    }

    @Test
    fun `toggling a second document extends the selection`() = runTest(dispatcher) {
        val invoice = document("1", DocumentType.INVOICE)
        val challan = document("2", DocumentType.CHALLAN)
        val viewModel = DocumentsListViewModel(FakeHomeDataSource(listOf(invoice, challan)))
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onDocumentLongPress("1")

        viewModel.onToggleSelection("2")

        assertEquals(setOf("1", "2"), viewModel.uiState.value.selectedDocumentIds)
    }

    @Test
    fun `onClearSelection empties the selection`() = runTest(dispatcher) {
        val invoice = document("1", DocumentType.INVOICE)
        val viewModel = DocumentsListViewModel(FakeHomeDataSource(listOf(invoice)))
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onDocumentLongPress("1")

        viewModel.onClearSelection()

        assertEquals(emptySet<String>(), viewModel.uiState.value.selectedDocumentIds)
    }

    @Test
    fun `changing the filter clears an existing selection`() = runTest(dispatcher) {
        val invoice = document("1", DocumentType.INVOICE)
        val viewModel = DocumentsListViewModel(FakeHomeDataSource(listOf(invoice)))
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onDocumentLongPress("1")

        viewModel.onFilterChanged(DocumentTypeFilter.INVOICES)

        assertEquals(emptySet<String>(), viewModel.uiState.value.selectedDocumentIds)
    }

    @Test
    fun `a subsequent refresh clears an existing selection`() = runTest(dispatcher) {
        val invoice = document("1", DocumentType.INVOICE)
        val viewModel = DocumentsListViewModel(FakeHomeDataSource(listOf(invoice)))
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onDocumentLongPress("1")

        viewModel.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(emptySet<String>(), viewModel.uiState.value.selectedDocumentIds)
    }
}
