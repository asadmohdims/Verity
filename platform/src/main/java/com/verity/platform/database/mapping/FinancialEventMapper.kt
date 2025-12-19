package com.verity.platform.database.mapping

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException
import com.verity.platform.database.entities.EventEntity
import com.verity.core.replay.ledger.ReplayInput

/**
 * FinancialEventMapper
 *
 * PURPOSE
 * -------
 * Defines the authoritative boundary for translating persisted
 * domain events into financial replay inputs.
 *
 * This interface exists to:
 * • Centralize eventType + eventVersion interpretation
 * • Enforce payload validation before replay
 * • Prevent silent corruption of ledger truth
 */
interface FinancialEventMapper {

    /**
     * Attempts to map a stored EventEntity into financial replay semantics.
     *
     * @param event persisted domain event
     * @return explicit mapping result
     */
    fun map(event: EventEntity): FinancialMappingResult
}

/**
 * FinancialMappingResult
 *
 * PURPOSE
 * -------
 * Explicit outcome of mapping an EventEntity into financial replay semantics.
 */
sealed class FinancialMappingResult {

    /**
     * Valid financial domain fact safe for ledger replay.
     */
    data class Financial(val replayInput: ReplayInput) : FinancialMappingResult()

    /**
     * Event is non-financial and must be ignored by the ledger.
     */
    object NonFinancial : FinancialMappingResult()

    /**
     * Event claims to be financial but violates version or payload contract.
     * Replay must fail loudly.
     */
    data class Invalid(val reason: String) : FinancialMappingResult()
}

/**
 * DefaultFinancialEventMapper
 *
 * PURPOSE
 * -------
 * Production-safe mapper implementation skeleton.
 *
 * This implementation intentionally DOES NOT parse payload JSON yet.
 * Parsing strategy (kotlinx.serialization / Moshi / manual) must be
 * injected later once explicitly chosen.
 *
 * This avoids assumptions while locking failure semantics and boundaries.
 */
class DefaultFinancialEventMapper : FinancialEventMapper {

    private val json = Json {
        ignoreUnknownKeys = false
        explicitNulls = false
    }

    @Serializable
    private data class FinancialPayloadV1(
        val customerId: String,
        val amount: Long
    )

    override fun map(event: EventEntity): FinancialMappingResult {
        return when (event.eventType) {

            // --- Financial events (Phase 5) ---
            "InvoiceFinalized",
            "PaymentRecorded" -> mapFinancialEvent(event)

            // --- All other events are non-financial for ledger purposes ---
            else -> FinancialMappingResult.NonFinancial
        }
    }

    private fun mapFinancialEvent(event: EventEntity): FinancialMappingResult {
        // Version gate — production safety
        if (event.eventVersion != 1) {
            return FinancialMappingResult.Invalid(
                reason = "Unsupported eventVersion=${event.eventVersion} for eventType=${event.eventType}"
            )
        }

        val payload = try {
            json.decodeFromString(FinancialPayloadV1.serializer(), event.payload)
        } catch (e: SerializationException) {
            return FinancialMappingResult.Invalid(
                reason = "Invalid payload JSON for eventType=${event.eventType}: ${e.message}"
            )
        } catch (e: IllegalArgumentException) {
            return FinancialMappingResult.Invalid(
                reason = "Invalid payload values for eventType=${event.eventType}: ${e.message}"
            )
        }

        if (payload.customerId.isBlank()) {
            return FinancialMappingResult.Invalid(
                reason = "customerId must be non-blank for eventType=${event.eventType}"
            )
        }

        if (payload.amount <= 0L) {
            return FinancialMappingResult.Invalid(
                reason = "amount must be > 0 for eventType=${event.eventType}"
            )
        }

        return FinancialMappingResult.Financial(
            replayInput = ReplayInput(
                eventId = event.eventId,
                eventType = event.eventType,
                customerId = payload.customerId,
                amount = payload.amount,
                occurredAt = event.occurredAt
            )
        )
    }
}
