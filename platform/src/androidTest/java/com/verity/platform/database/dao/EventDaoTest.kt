package com.verity.platform.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.verity.platform.database.PlatformDatabase
import com.verity.platform.database.entities.EventEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * EventDaoTest
 *
 * PURPOSE
 * -------
 * Verifies correctness guarantees of the EventDao against a real Room database.
 *
 * These tests lock append-only semantics, deterministic ordering,
 * and organization isolation for the event store.
 */
@RunWith(AndroidJUnit4::class)
class EventDaoTest {

    private lateinit var database: PlatformDatabase
    private lateinit var eventDao: EventDao

    @Before
    fun setup() {
        database =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                PlatformDatabase::class.java
            )
                .allowMainThreadQueries() // instrumentation tests only
                .build()

        eventDao = database.eventDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun duplicate_event_insert_is_ignored() = runBlocking {
        val eventId = UUID.randomUUID().toString()

        val event = baseEvent(
            eventId = eventId,
            occurredAt = 1000L
        )

        eventDao.insertEvent(event)
        eventDao.insertEvent(event)

        val events = eventDao.getAllEventsForOrg(event.orgId)

        assertEquals(1, events.size)
    }

    @Test
    fun events_are_returned_in_deterministic_order() = runBlocking {
        val orgId = "org-1"

        val e1 = baseEvent(
            eventId = "b-event",
            orgId = orgId,
            occurredAt = 1000L
        )

        val e2 = baseEvent(
            eventId = "a-event",
            orgId = orgId,
            occurredAt = 1000L
        )

        val e3 = baseEvent(
            eventId = "c-event",
            orgId = orgId,
            occurredAt = 2000L
        )

        eventDao.insertEvents(listOf(e1, e2, e3))

        val events = eventDao.getAllEventsForOrg(orgId)

        assertEquals(
            listOf("a-event", "b-event", "c-event"),
            events.map { it.eventId }
        )
    }

    @Test
    fun events_are_isolated_by_organization() = runBlocking {
        val eventA = baseEvent(orgId = "org-a")
        val eventB = baseEvent(orgId = "org-b")

        eventDao.insertEvent(eventA)
        eventDao.insertEvent(eventB)

        val orgAEvents = eventDao.getAllEventsForOrg("org-a")
        val orgBEvents = eventDao.getAllEventsForOrg("org-b")

        assertEquals(1, orgAEvents.size)
        assertEquals(1, orgBEvents.size)
    }

    @Test
    fun incremental_query_returns_only_events_after_timestamp() = runBlocking {
        val orgId = "org-1"

        val e1 = baseEvent(eventId = "e1", orgId = orgId, occurredAt = 1000L)
        val e2 = baseEvent(eventId = "e2", orgId = orgId, occurredAt = 2000L)
        val e3 = baseEvent(eventId = "e3", orgId = orgId, occurredAt = 3000L)

        eventDao.insertEvents(listOf(e1, e2, e3))

        val eventsAfter =
            eventDao.getEventsAfter(
                orgId = orgId,
                occurredAfter = 1500L
            )

        assertEquals(
            listOf("e2", "e3"),
            eventsAfter.map { it.eventId }
        )
    }

    private fun baseEvent(
        eventId: String = UUID.randomUUID().toString(),
        orgId: String = "org-test",
        eventType: String = "TestEvent",
        eventVersion: Int = 1,
        occurredAt: Long = System.currentTimeMillis(),
        recordedAt: Long = occurredAt,
        payload: String = "{}",
        source: String = "test",
        correlationId: String? = null
    ): EventEntity =
        EventEntity(
            eventId = eventId,
            orgId = orgId,
            eventType = eventType,
            eventVersion = eventVersion,
            occurredAt = occurredAt,
            recordedAt = recordedAt,
            payload = payload,
            source = source,
            correlationId = correlationId
        )
}