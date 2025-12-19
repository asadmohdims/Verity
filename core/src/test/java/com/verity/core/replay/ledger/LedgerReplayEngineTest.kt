package com.verity.core.replay.ledger

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * LedgerReplayEngineTest
 *
 * PURPOSE
 * -------
 * Defines correctness expectations for financial replay.
 *
 * These tests lock the meaning of financial events and
 * the invariants of derived ledger truth.
 *
 * This is a pure JVM test: no Android, no Room, no platform dependencies.
 */
class LedgerReplayEngineTest {

    private val engine: LedgerReplayEngine =
        SimpleLedgerReplayEngine()

    @Test
    fun invoice_followed_by_payment_results_in_zero_balance() {
        val events = listOf(
            invoice(
                eventId = "e1",
                customerId = "c1",
                amount = 1000,
                occurredAt = 1000
            ),
            payment(
                eventId = "e2",
                customerId = "c1",
                amount = 1000,
                occurredAt = 2000
            )
        )

        val state = engine.replay(events)

        assertEquals(
            0L,
            state.balancesByCustomer["c1"]
        )
    }

    @Test
    fun multiple_customers_are_isolated() {
        val events = listOf(
            invoice("e1", "c1", 500, 1000),
            invoice("e2", "c2", 700, 1000),
            payment("e3", "c1", 200, 2000)
        )

        val state = engine.replay(events)

        assertEquals(300L, state.balancesByCustomer["c1"])
        assertEquals(700L, state.balancesByCustomer["c2"])
    }

    @Test
    fun payment_before_invoice_creates_customer_credit() {
        val events = listOf(
            payment("e1", "c1", 500, 1000),
            invoice("e2", "c1", 300, 2000)
        )

        val state = engine.replay(events)

        assertEquals(
            -200L,
            state.balancesByCustomer["c1"]
        )
    }

    @Test
    fun replay_is_deterministic() {
        val events = listOf(
            invoice("e1", "c1", 1000, 1000),
            payment("e2", "c1", 400, 2000),
            payment("e3", "c1", 600, 3000)
        )

        val first = engine.replay(events)
        val second = engine.replay(events)

        assertEquals(first, second)
    }

    @Test
    fun unknown_event_type_does_not_affect_ledger_state() {
        val events = listOf(
            invoice("e1", "c1", 1000, 1000),
            ReplayInput(
                eventId = "e-unknown",
                eventType = "SomeFutureEvent",
                customerId = "c1",
                amount = 9999,
                occurredAt = 1500
            ),
            payment("e2", "c1", 400, 2000)
        )

        val state = engine.replay(events)

        // Only InvoiceFinalized (+1000) and PaymentRecorded (-400) must apply
        assertEquals(
            600L,
            state.balancesByCustomer["c1"]
        )

        // Unknown event must not produce a ledger entry
        assertEquals(
            2,
            state.entries.size
        )
    }

    // ---- helpers ----

    private fun invoice(
        eventId: String,
        customerId: String,
        amount: Long,
        occurredAt: Long
    ): ReplayInput =
        ReplayInput(
            eventId = eventId,
            eventType = "InvoiceFinalized",
            customerId = customerId,
            amount = amount,
            occurredAt = occurredAt
        )

    private fun payment(
        eventId: String,
        customerId: String,
        amount: Long,
        occurredAt: Long
    ): ReplayInput =
        ReplayInput(
            eventId = eventId,
            eventType = "PaymentRecorded",
            customerId = customerId,
            amount = amount,
            occurredAt = occurredAt
        )
}