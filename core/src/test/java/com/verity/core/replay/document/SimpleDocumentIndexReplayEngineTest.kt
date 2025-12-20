package com.verity.core.replay.document

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * SimpleDocumentIndexReplayEngineTest
 *
 * PURPOSE
 * -------
 * Verifies deterministic and fail-loud behavior of
 * SimpleDocumentIndexReplayEngine.
 *
 * These tests lock the correctness contract for document
 * index replay and must never be weakened.
 */
class SimpleDocumentIndexReplayEngineTest {

    private val replayEngine = SimpleDocumentIndexReplayEngine()

    @Test
    fun `upsert document creates index row`() {
        val mutation = DocumentIndexMutation.UpsertDocument(
            documentId = "doc-1",
            documentType = "INVOICE",
            documentNumber = "INV-001",
            customerId = "cust-1",
            customerName = "Acme Corp",
            documentDate = 1_700_000_000L,
            totalAmount = 10_000L,
            status = "FINALIZED",
            orgId = "org-1",
            occurredAt = 1_700_000_000L,
            eventId = "evt-1"
        )

        val state = replayEngine.replay(listOf(mutation))

        assertEquals(1, state.documentsById.size)

        val row = state.documentsById["doc-1"]!!
        assertEquals("INV-001", row.documentNumber)
        assertEquals("FINALIZED", row.status)
        assertEquals(10_000L, row.totalAmount)
    }

    @Test
    fun `update status changes existing document`() {
        val upsert = DocumentIndexMutation.UpsertDocument(
            documentId = "doc-1",
            documentType = "INVOICE",
            documentNumber = "INV-001",
            customerId = "cust-1",
            customerName = "Acme Corp",
            documentDate = 1_700_000_000L,
            totalAmount = 10_000L,
            status = "FINALIZED",
            orgId = "org-1",
            occurredAt = 1_700_000_000L,
            eventId = "evt-1"
        )

        val cancel = DocumentIndexMutation.UpdateStatus(
            documentId = "doc-1",
            documentType = "INVOICE",
            status = "CANCELLED",
            occurredAt = 1_700_000_001L,
            eventId = "evt-2"
        )

        val state = replayEngine.replay(listOf(upsert, cancel))

        val row = state.documentsById["doc-1"]!!
        assertEquals("CANCELLED", row.status)
    }

    @Test
    fun `update status without prior upsert fails loudly`() {
        val cancel = DocumentIndexMutation.UpdateStatus(
            documentId = "doc-1",
            documentType = "INVOICE",
            status = "CANCELLED",
            occurredAt = 1_700_000_001L,
            eventId = "evt-2"
        )

        assertThrows(
            IllegalStateException::class.java
        ) {
            replayEngine.replay(listOf(cancel))
        }
    }
}