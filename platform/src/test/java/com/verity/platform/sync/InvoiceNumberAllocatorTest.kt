package com.verity.platform.sync

import com.verity.platform.database.dao.DocumentDao
import com.verity.platform.database.entities.DocumentEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * InvoiceNumberAllocatorTest
 *
 * Pure JVM — DocumentDao is faked directly (a plain Kotlin interface; no Room runtime needed for
 * this test), so it runs without an emulator. Covers the three paths DefaultInvoiceNumberAllocator
 * is built around (see its doc comment): online success, online failure, online timeout — every
 * non-success path must fall back to the local sequence, never propagate an exception or hang.
 */
class InvoiceNumberAllocatorTest {

    private val fakeDocumentDao = FakeDocumentDao()

    @Test
    fun allocate_returns_the_online_value_when_the_counter_succeeds() = runTest {
        val allocator = DefaultInvoiceNumberAllocator(
            onlineCounterSource = { _, _ -> 42L },
            documentDao = fakeDocumentDao
        )

        assertEquals(42L, allocator.allocate("org-1", "INVOICE"))
    }

    @Test
    fun allocate_falls_back_to_local_when_the_online_counter_throws() = runTest {
        fakeDocumentDao.maxSequenceNumber = 5L
        val allocator = DefaultInvoiceNumberAllocator(
            onlineCounterSource = { _, _ -> error("simulated network failure") },
            documentDao = fakeDocumentDao
        )

        assertEquals(6L, allocator.allocate("org-1", "INVOICE"))
    }

    @Test
    fun allocate_falls_back_to_local_when_the_online_counter_times_out() = runTest {
        fakeDocumentDao.maxSequenceNumber = 9L
        val allocator = DefaultInvoiceNumberAllocator(
            onlineCounterSource = { _, _ ->
                delay(10_000) // longer than the allocator's timeout below
                99L
            },
            documentDao = fakeDocumentDao,
            onlineTimeoutMillis = 50
        )

        assertEquals(10L, allocator.allocate("org-1", "INVOICE"))
    }

    @Test
    fun allocate_falls_back_to_one_when_there_is_no_local_history_either() = runTest {
        fakeDocumentDao.maxSequenceNumber = null
        val allocator = DefaultInvoiceNumberAllocator(
            onlineCounterSource = { _, _ -> error("simulated network failure") },
            documentDao = fakeDocumentDao
        )

        assertEquals(1L, allocator.allocate("org-1", "INVOICE"))
    }

    private class FakeDocumentDao : DocumentDao {
        var maxSequenceNumber: Long? = null

        override suspend fun insert(document: DocumentEntity) = error("not used by this test")
        override suspend fun upsertAllFromCloud(documents: List<DocumentEntity>) = error("not used by this test")
        override suspend fun markSyncedToCloud(documentId: String) = error("not used by this test")
        override suspend fun getPendingSyncCount(): Int = error("not used by this test")
        override suspend fun count(): Int = error("not used by this test")
        override suspend fun getMaxSequenceNumber(orgId: String, documentType: String): Long? = maxSequenceNumber
        override suspend fun getById(documentId: String): DocumentEntity? = error("not used by this test")
        override suspend fun getAll(): List<DocumentEntity> = error("not used by this test")
        override suspend fun search(normalizedQuery: String): List<DocumentEntity> = error("not used by this test")
        override suspend fun getByCustomerId(customerId: String): List<DocumentEntity> = error("not used by this test")
        override suspend fun findByLinkedDocumentId(challanDocumentId: String): DocumentEntity? = error("not used by this test")
        override suspend fun findByDocumentNumber(orgId: String, documentNumber: String): DocumentEntity? = error("not used by this test")
    }
}
