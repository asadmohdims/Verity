package com.verity.platform.database.mapping

import com.verity.platform.database.entities.EventEntity
import com.verity.core.replay.ledger.ReplayInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialEventMapperTest {

    private val mapper = DefaultFinancialEventMapper()

    @Test
    fun invoiceFinalized_v1_with_valid_payload_maps_to_financial_replay_input() {
        val event = EventEntity(
            eventId = "e1",
            orgId = "org1",
            eventType = "InvoiceFinalized",
            eventVersion = 1,
            occurredAt = 1000L,
            recordedAt = 1100L,
            payload = """{"customerId":"c1","amount":1000}""",
            source = "local",
            correlationId = null
        )

        val result = mapper.map(event)

        assertTrue(result is FinancialMappingResult.Financial)

        val replayInput = (result as FinancialMappingResult.Financial).replayInput
        assertEquals(
            ReplayInput(
                eventId = "e1",
                eventType = "InvoiceFinalized",
                customerId = "c1",
                amount = 1000L,
                occurredAt = 1000L
            ),
            replayInput
        )
    }

    @Test
    fun unknown_event_type_is_treated_as_non_financial() {
        val event = EventEntity(
            eventId = "e2",
            orgId = "org1",
            eventType = "CustomerCreated",
            eventVersion = 1,
            occurredAt = 2000L,
            recordedAt = 2100L,
            payload = """{"id":"c1","name":"Alice"}""",
            source = "local",
            correlationId = null
        )

        val result = mapper.map(event)

        assertTrue(result is FinancialMappingResult.NonFinancial)
    }

    @Test
    fun invalid_financial_payload_fails_loudly() {
        val event = EventEntity(
            eventId = "e3",
            orgId = "org1",
            eventType = "PaymentRecorded",
            eventVersion = 1,
            occurredAt = 3000L,
            recordedAt = 3100L,
            payload = """{"customerId":"","amount":-500}""",
            source = "local",
            correlationId = null
        )

        val result = mapper.map(event)

        assertTrue(result is FinancialMappingResult.Invalid)
    }
}
