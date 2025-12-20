package com.verity.platform.database.projection

import com.verity.core.replay.document.DocumentIndexEventMapper
import com.verity.core.replay.document.DocumentIndexReplayEngine
import com.verity.core.replay.document.DocumentIndexState
import com.verity.core.replay.document.DocumentIndexMutation
import com.verity.platform.database.dao.DocumentIndexDao
import com.verity.platform.database.dao.EventDao
import com.verity.platform.database.entities.DocumentIndexEntity
import com.verity.platform.database.entities.EventEntity
import com.verity.platform.replay.document.DocumentIndexProjectionWriter
import com.verity.platform.replay.PlatformReplayableEvent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DocumentIndexProjectionWriterTest
 *
 * PURPOSE
 * -------
 * Verifies deterministic, cursor-safe, replay-driven behavior
 * of the DocumentIndexProjectionWriter.
 *
 * These tests lock:
 * • No-op behavior when no events exist
 * • Correct index write for valid events
 * • Loud failure on invalid events
 * • Cursor correctness
 */
class DocumentIndexProjectionWriterTest {

    private val orgId = "org-1"

    @Test
    fun no_events_results_in_no_projection_write() = runBlocking {
        val dao = FakeDocumentIndexDao()

        val writer = DocumentIndexProjectionWriter(
            eventDao = FakeEventDao(emptyList()),
            documentIndexDao = dao,
            mapper = DocumentIndexEventMapper(),
            replayEngine = FakeReplayEngine()
        )

        writer.run(orgId)

        assertTrue(dao.writtenRows.isEmpty())
    }

    @Test
    fun valid_event_produces_document_index_row_and_advances_cursor() = runBlocking {
        val event = EventEntity(
            eventId = "e1",
            orgId = orgId,
            eventType = "InvoiceFinalized",
            eventVersion = 1,
            occurredAt = 10L,
            recordedAt = 11L,
            payload = """
            {
              "invoiceId": "doc-1",
              "invoiceNumber": "INV-001",
              "customerId": "c1",
              "customerName": "Customer One",
              "documentDate": 1,
              "totals": { "grandTotal": 1000 }
            }
            """.trimIndent(),
            source = "local",
            correlationId = null
        )

        val dao = FakeDocumentIndexDao()

        val writer = DocumentIndexProjectionWriter(
            eventDao = FakeEventDao(listOf(event)),
            documentIndexDao = dao,
            mapper = DocumentIndexEventMapper(),
            replayEngine = FakeReplayEngine()
        )

        writer.run(orgId)

        assertEquals(1, dao.writtenRows.size)

        val row = dao.writtenRows.first()
        assertEquals("doc-1", row.documentId)
        assertEquals("INVOICE", row.documentType)
        assertEquals("INV-001", row.documentNumber)
        assertEquals("c1", row.customerId)
        assertEquals("Customer One", row.customerName)
        assertEquals(1000L, row.totalAmount)
        assertEquals("FINALIZED", row.status)
        assertEquals(orgId, row.orgId)

        assertEquals(10L, row.lastAppliedOccurredAt)
        assertEquals("e1", row.lastAppliedEventId)
    }

    @Test(expected = IllegalStateException::class)
    fun invalid_event_fails_loudly_and_writes_nothing() = runBlocking {
        val event = EventEntity(
            eventId = "e2",
            orgId = orgId,
            eventType = "InvoiceFinalized",
            eventVersion = 1,
            occurredAt = 20L,
            recordedAt = 21L,
            payload = "{}",
            source = "local",
            correlationId = null
        )

        val dao = FakeDocumentIndexDao()

        val writer = DocumentIndexProjectionWriter(
            eventDao = FakeEventDao(listOf(event)),
            documentIndexDao = dao,
            mapper = DocumentIndexEventMapper(),
            replayEngine = FakeReplayEngine()
        )

        try {
            writer.run(orgId)
        } finally {
            assertTrue(dao.writtenRows.isEmpty())
        }
    }

    // ------------------------------------------------------------------
    // FAKES
    // ------------------------------------------------------------------

    private class FakeEventDao(
        private val events: List<EventEntity>
    ) : EventDao {
        override suspend fun insertEvent(event: EventEntity) {}
        override suspend fun insertEvents(events: List<EventEntity>) {}
        override suspend fun getAllEventsForOrg(orgId: String): List<EventEntity> = events
        override suspend fun getEventsAfter(orgId: String, occurredAfter: Long): List<EventEntity> =
            events.filter { it.occurredAt > occurredAfter }
    }

    private class FakeDocumentIndexDao : DocumentIndexDao {
        val writtenRows = mutableListOf<DocumentIndexEntity>()

        override suspend fun upsertAll(documents: List<DocumentIndexEntity>) {
            writtenRows.addAll(documents)
        }

        override suspend fun getLatestCursor(): DocumentIndexEntity? =
            writtenRows.maxWithOrNull(
                compareBy<DocumentIndexEntity> { it.lastAppliedOccurredAt }
                    .thenBy { it.lastAppliedEventId }
            )

        override suspend fun getAll(): List<DocumentIndexEntity> = writtenRows

        override suspend fun clearAll() {
            writtenRows.clear()
        }
    }

    private class FakeReplayEngine : DocumentIndexReplayEngine {
        override fun replay(
            mutations: List<DocumentIndexMutation>
        ): DocumentIndexState =
            DocumentIndexState(
                documentsById = mutations
                    .filterIsInstance<DocumentIndexMutation.UpsertDocument>()
                    .associateBy(
                        keySelector = { it.documentId },
                        valueTransform = {
                            com.verity.core.replay.document.DocumentIndexRow(
                                documentId = it.documentId,
                                documentType = it.documentType,
                                documentNumber = it.documentNumber,
                                customerId = it.customerId,
                                customerName = it.customerName,
                                documentDate = it.documentDate,
                                totalAmount = it.totalAmount,
                                status = it.status,
                                orgId = it.orgId
                            )
                        }
                    )
            )
    }
}