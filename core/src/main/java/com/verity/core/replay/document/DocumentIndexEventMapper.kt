package com.verity.core.replay.document

import com.verity.core.replay.common.ReplayableEvent
import org.json.JSONObject

/**
 * DocumentIndexEventMapper
 *
 * PURPOSE
 * -------
 * Translates raw EventEntity records into validated
 * DocumentIndexMutation intents for document index replay.
 *
 * This mapper is the sole authority for interpreting document-related
 * event payloads. It must fail loudly on malformed or unsupported
 * events to preserve replay correctness.
 *
 * CONSTRAINTS
 * -----------
 * • No side effects
 * • No persistence access
 * • No inference or defaults for required fields
 * • Strict eventVersion handling
 */
class DocumentIndexEventMapper {

    sealed class MappingResult {
        object NonIndexable : MappingResult()
        data class Mutation(val mutation: DocumentIndexMutation) : MappingResult()
        data class Invalid(val reason: String) : MappingResult()
    }

    fun map(event: ReplayableEvent): MappingResult {
        return when (event.eventType) {
            "InvoiceFinalized" -> mapInvoiceFinalized(event)
            "InvoiceCancelled" -> mapInvoiceCancelled(event)
            "ChallanIssued" -> mapChallanIssued(event)
            "ChallanCancelled" -> mapChallanCancelled(event)
            else -> MappingResult.NonIndexable
        }
    }

    private fun mapInvoiceFinalized(event: ReplayableEvent): MappingResult {
        if (event.eventVersion != 1) {
            return MappingResult.Invalid("Unsupported InvoiceFinalized version=${event.eventVersion}")
        }

        return try {
            val json = JSONObject(event.payload)
            val totals = json.getJSONObject("totals")

            MappingResult.Mutation(
                DocumentIndexMutation.UpsertDocument(
                    documentId = json.getString("invoiceId"),
                    documentType = "INVOICE",
                    documentNumber = json.getString("invoiceNumber"),
                    customerId = json.getString("customerId"),
                    customerName = json.getString("customerName"),
                    documentDate = json.getLong("documentDate"),
                    totalAmount = totals.getLong("grandTotal"),
                    status = "FINALIZED",
                    orgId = event.orgId,
                    occurredAt = event.occurredAt,
                    eventId = event.eventId
                )
            )
        } catch (ex: Exception) {
            MappingResult.Invalid("Invalid InvoiceFinalized payload: ${ex.message}")
        }
    }

    private fun mapInvoiceCancelled(event: ReplayableEvent): MappingResult {
        if (event.eventVersion != 1) {
            return MappingResult.Invalid("Unsupported InvoiceCancelled version=${event.eventVersion}")
        }

        return try {
            val json = JSONObject(event.payload)

            MappingResult.Mutation(
                DocumentIndexMutation.UpdateStatus(
                    documentId = json.getString("invoiceId"),
                    documentType = "INVOICE",
                    status = "CANCELLED",
                    occurredAt = event.occurredAt,
                    eventId = event.eventId
                )
            )
        } catch (ex: Exception) {
            MappingResult.Invalid("Invalid InvoiceCancelled payload: ${ex.message}")
        }
    }

    private fun mapChallanIssued(event: ReplayableEvent): MappingResult {
        if (event.eventVersion != 1) {
            return MappingResult.Invalid("Unsupported ChallanIssued version=${event.eventVersion}")
        }

        return try {
            val json = JSONObject(event.payload)
            val declaredValue = json.optJSONObject("declaredValue")
            val amount = declaredValue?.optLong("amount", 0L) ?: 0L

            MappingResult.Mutation(
                DocumentIndexMutation.UpsertDocument(
                    documentId = json.getString("challanId"),
                    documentType = "CHALLAN",
                    documentNumber = json.getString("challanNumber"),
                    customerId = json.getString("customerId"),
                    customerName = json.getString("customerName"),
                    documentDate = json.getLong("documentDate"),
                    totalAmount = amount,
                    status = "ISSUED",
                    orgId = event.orgId,
                    occurredAt = event.occurredAt,
                    eventId = event.eventId
                )
            )
        } catch (ex: Exception) {
            MappingResult.Invalid("Invalid ChallanIssued payload: ${ex.message}")
        }
    }

    private fun mapChallanCancelled(event: ReplayableEvent): MappingResult {
        if (event.eventVersion != 1) {
            return MappingResult.Invalid("Unsupported ChallanCancelled version=${event.eventVersion}")
        }

        return try {
            val json = JSONObject(event.payload)

            MappingResult.Mutation(
                DocumentIndexMutation.UpdateStatus(
                    documentId = json.getString("challanId"),
                    documentType = "CHALLAN",
                    status = "CANCELLED",
                    occurredAt = event.occurredAt,
                    eventId = event.eventId
                )
            )
        } catch (ex: Exception) {
            MappingResult.Invalid("Invalid ChallanCancelled payload: ${ex.message}")
        }
    }
}
