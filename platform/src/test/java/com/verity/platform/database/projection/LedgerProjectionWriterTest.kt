package com.verity.platform.database.projection

import com.verity.platform.database.dao.EventDao
import com.verity.platform.database.dao.LedgerBalanceDao
import com.verity.platform.database.entities.EventEntity
import com.verity.platform.database.entities.LedgerBalanceEntity
import com.verity.platform.database.mapping.FinancialEventMapper
import com.verity.platform.database.mapping.FinancialMappingResult
import com.verity.core.replay.ledger.LedgerReplayEngine
import com.verity.core.replay.ledger.LedgerState
import com.verity.core.replay.ledger.ReplayInput
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LedgerProjectionWriterTest {

    private val orgId = "org1"

    @Test
    fun no_events_results_in_no_projection_write() = runBlocking {
        val ledgerDao = FakeLedgerBalanceDao()
        val writer = LedgerProjectionWriter(
            orgId = orgId,
            eventDao = FakeEventDao(emptyList()),
            ledgerBalanceDao = ledgerDao,
            financialEventMapper = FakeMapper(),
            replayEngine = FakeReplayEngine(),
            now = { 1000L }
        )

        writer.runIncrementalReplay()

        assertTrue(ledgerDao.writtenRows.isEmpty())
    }

    @Test
    fun valid_financial_event_updates_balance_and_cursor() = runBlocking {
        val event = EventEntity(
            eventId = "e1",
            orgId = orgId,
            eventType = "InvoiceFinalized",
            eventVersion = 1,
            occurredAt = 10L,
            recordedAt = 11L,
            payload = "{}",
            source = "local",
            correlationId = null
        )

        val ledgerDao = FakeLedgerBalanceDao()
        val writer = LedgerProjectionWriter(
            orgId = orgId,
            eventDao = FakeEventDao(listOf(event)),
            ledgerBalanceDao = ledgerDao,
            financialEventMapper = FakeMapper(),
            replayEngine = FakeReplayEngine(),
            now = { 1000L }
        )

        writer.runIncrementalReplay()

        assertEquals(1, ledgerDao.writtenRows.size)
        val row = ledgerDao.writtenRows.first()
        assertEquals("c1", row.customerId)
        assertEquals(500L, row.balance)
        assertEquals(10L, row.lastAppliedOccurredAt)
        assertEquals("e1", row.lastAppliedEventId)
    }

    @Test(expected = IllegalStateException::class)
    fun invalid_financial_event_fails_loudly_and_writes_nothing() = runBlocking {
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

        val ledgerDao = FakeLedgerBalanceDao()
        val writer = LedgerProjectionWriter(
            orgId = orgId,
            eventDao = FakeEventDao(listOf(event)),
            ledgerBalanceDao = ledgerDao,
            financialEventMapper = FakeInvalidMapper(),
            replayEngine = FakeReplayEngine(),
            now = { 1000L }
        )

        try {
            writer.runIncrementalReplay()
        } finally {
            assertTrue(ledgerDao.writtenRows.isEmpty())
        }
    }

    // ---------- Fakes ----------

    private class FakeEventDao(
        private val events: List<EventEntity>
    ) : EventDao {
        override suspend fun insertEvent(event: EventEntity) {}
        override suspend fun insertEvents(events: List<EventEntity>) {}
        override suspend fun getAllEventsForOrg(orgId: String): List<EventEntity> = events
        override suspend fun getEventsAfter(orgId: String, occurredAfter: Long): List<EventEntity> =
            events.filter { it.occurredAt > occurredAfter }
    }

    private class FakeLedgerBalanceDao : LedgerBalanceDao {
        val writtenRows = mutableListOf<LedgerBalanceEntity>()

        override suspend fun upsertAll(balances: List<LedgerBalanceEntity>) {
            writtenRows.addAll(balances)
        }

        override suspend fun getBalanceForCustomer(customerId: String): LedgerBalanceEntity? = null
        override suspend fun getAllBalances(): List<LedgerBalanceEntity> = writtenRows
        override suspend fun clearAll() {
            writtenRows.clear()
        }
    }

    private class FakeMapper : FinancialEventMapper {
        override fun map(event: EventEntity): FinancialMappingResult =
            FinancialMappingResult.Financial(
                ReplayInput(
                    eventId = event.eventId,
                    eventType = event.eventType,
                    customerId = "c1",
                    amount = 500L,
                    occurredAt = event.occurredAt
                )
            )
    }

    private class FakeInvalidMapper : FinancialEventMapper {
        override fun map(event: EventEntity): FinancialMappingResult =
            FinancialMappingResult.Invalid("invalid")
    }

    private class FakeReplayEngine : LedgerReplayEngine {
        override fun replay(events: List<ReplayInput>): LedgerState =
            LedgerState(
                balancesByCustomer = mapOf("c1" to 500L),
                entries = emptyList()
            )
    }
}
