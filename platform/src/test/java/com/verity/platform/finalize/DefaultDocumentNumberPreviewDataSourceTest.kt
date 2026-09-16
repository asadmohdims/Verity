package com.verity.platform.finalize

import com.verity.feature.invoice.draft.DraftDocumentType
import com.verity.platform.database.dao.DocumentDao
import com.verity.platform.database.entities.DocumentEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultDocumentNumberPreviewDataSourceTest {

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

    @Test
    fun `predicts one past the current max for Invoice`() = runTest {
        val dao = FakeDocumentDao().apply { maxSequenceNumber = 42L }
        val dataSource = DefaultDocumentNumberPreviewDataSource(dao)

        assertEquals("INV-000043", dataSource.peekNextNumber(DraftDocumentType.INVOICE))
    }

    @Test
    fun `predicts one past the current max for Challan`() = runTest {
        val dao = FakeDocumentDao().apply { maxSequenceNumber = 9L }
        val dataSource = DefaultDocumentNumberPreviewDataSource(dao)

        assertEquals("CH-000010", dataSource.peekNextNumber(DraftDocumentType.CHALLAN))
    }

    @Test
    fun `predicts number one when there is no history yet`() = runTest {
        val dao = FakeDocumentDao().apply { maxSequenceNumber = null }
        val dataSource = DefaultDocumentNumberPreviewDataSource(dao)

        assertEquals("INV-000001", dataSource.peekNextNumber(DraftDocumentType.INVOICE))
    }
}
