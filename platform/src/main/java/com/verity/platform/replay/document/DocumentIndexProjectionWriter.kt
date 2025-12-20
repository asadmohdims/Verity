package com.verity.platform.replay.document

import com.verity.core.replay.document.DocumentIndexEventMapper
import com.verity.core.replay.document.DocumentIndexReplayEngine
import com.verity.core.replay.document.DocumentIndexState
import com.verity.platform.database.dao.DocumentIndexDao
import com.verity.platform.database.dao.EventDao
import com.verity.platform.database.entities.DocumentIndexEntity
import com.verity.platform.replay.PlatformReplayableEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * DocumentIndexProjectionWriter
 *
 * PURPOSE
 * -------
 * Incrementally materializes the document index projection by
 * replaying immutable domain events in a deterministic and
 * crash-safe manner.
 *
 * This writer orchestrates platform persistence, core replay,
 * and strict cursor advancement. It contains no business logic
 * and must be safe to rerun at any time.
 *
 * CONSTRAINTS
 * -----------
 * • Projection-only writes (no domain mutation)
 * • Fail-loud on invalid events or payloads
 * • Cursor advances only after successful persistence
 * • Idempotent and deterministic
 */
class DocumentIndexProjectionWriter(
    private val eventDao: EventDao,
    private val documentIndexDao: DocumentIndexDao,
    private val mapper: DocumentIndexEventMapper,
    private val replayEngine: DocumentIndexReplayEngine
) {

    /**
     * Incrementally applies new events to the document index
     * projection for the given organization.
     */
    suspend fun run(orgId: String) = withContext(Dispatchers.IO) {
        val latest = documentIndexDao.getLatestCursor()

        val events = if (latest == null) {
            eventDao.getAllEventsForOrg(orgId)
        } else {
            eventDao.getEventsAfter(
                orgId = orgId,
                occurredAfter = latest.lastAppliedOccurredAt
            )
        }

        if (events.isEmpty()) return@withContext

        val mutations = mutableListOf<com.verity.core.replay.document.DocumentIndexMutation>()

        var lastOccurredAt = latest?.lastAppliedOccurredAt ?: 0L
        var lastEventId = latest?.lastAppliedEventId ?: ""

        for (entity in events) {
            val replayable = PlatformReplayableEvent(entity)
            when (val result = mapper.map(replayable)) {
                is DocumentIndexEventMapper.MappingResult.NonIndexable -> {
                    lastOccurredAt = entity.occurredAt
                    lastEventId = entity.eventId
                }

                is DocumentIndexEventMapper.MappingResult.Mutation -> {
                    mutations.add(result.mutation)
                    lastOccurredAt = entity.occurredAt
                    lastEventId = entity.eventId
                }

                is DocumentIndexEventMapper.MappingResult.Invalid -> {
                    error("DocumentIndex projection failed: ${result.reason}")
                }
            }
        }

        if (mutations.isEmpty()) return@withContext

        val state: DocumentIndexState = replayEngine.replay(mutations)

        val now = System.currentTimeMillis()

        val entities = state.documentsById.values.map { row ->
            DocumentIndexEntity(
                documentId = row.documentId,
                documentType = row.documentType,
                documentNumber = row.documentNumber,
                customerId = row.customerId,
                customerName = row.customerName,
                documentDate = row.documentDate,
                totalAmount = row.totalAmount,
                status = row.status,
                orgId = row.orgId,
                lastAppliedOccurredAt = lastOccurredAt,
                lastAppliedEventId = lastEventId,
                updatedAt = now
            )
        }

        documentIndexDao.upsertAll(entities)
    }

    /**
     * Performs a full rebuild of the document index projection
     * from the complete event history.
     */
    suspend fun rebuild(orgId: String) = withContext(Dispatchers.IO) {
        val events = eventDao.getAllEventsForOrg(orgId)
        if (events.isEmpty()) {
            documentIndexDao.clearAll()
            return@withContext
        }

        val mutations = mutableListOf<com.verity.core.replay.document.DocumentIndexMutation>()

        var lastOccurredAt = 0L
        var lastEventId = ""

        for (entity in events) {
            val replayable = PlatformReplayableEvent(entity)
            when (val result = mapper.map(replayable)) {
                is DocumentIndexEventMapper.MappingResult.NonIndexable -> {
                    lastOccurredAt = entity.occurredAt
                    lastEventId = entity.eventId
                }

                is DocumentIndexEventMapper.MappingResult.Mutation -> {
                    mutations.add(result.mutation)
                    lastOccurredAt = entity.occurredAt
                    lastEventId = entity.eventId
                }

                is DocumentIndexEventMapper.MappingResult.Invalid -> {
                    error("DocumentIndex rebuild failed: ${result.reason}")
                }
            }
        }

        val state = replayEngine.replay(mutations)
        val now = System.currentTimeMillis()

        val entities = state.documentsById.values.map { row ->
            DocumentIndexEntity(
                documentId = row.documentId,
                documentType = row.documentType,
                documentNumber = row.documentNumber,
                customerId = row.customerId,
                customerName = row.customerName,
                documentDate = row.documentDate,
                totalAmount = row.totalAmount,
                status = row.status,
                orgId = row.orgId,
                lastAppliedOccurredAt = lastOccurredAt,
                lastAppliedEventId = lastEventId,
                updatedAt = now
            )
        }

        documentIndexDao.replaceAll(entities)
    }
}
