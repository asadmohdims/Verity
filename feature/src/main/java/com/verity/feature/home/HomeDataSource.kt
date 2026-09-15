package com.verity.feature.home

import com.verity.core.document.model.DocumentType
import com.verity.core.formatting.money.Money
import java.time.LocalDate

/**
 * HomeDataSource
 *
 * Port for reading finalized documents for the Home dashboard. Feature defines this interface;
 * platform implements it (same direction as CustomerAutocompleteDataSource / InvoiceFinalizer)
 * because reading persisted documents requires database access, which only platform may touch.
 *
 * Deliberately returns every document rather than a pre-filtered/pre-limited set: "this month"
 * and "recent N" are both derived, in-memory, by HomeDashboardCalculator — no new persistence or
 * query logic, per the approved Design Blueprint's Decision 1.
 */
interface HomeDataSource {
    suspend fun loadAllDocuments(): List<DocumentSummary>

    /** Cloud sync status for the small Home indicator — see SyncStatus's doc comment. */
    suspend fun loadSyncStatus(): SyncStatus
}

/**
 * pendingCount is a plain count of local rows not yet mirrored to the cloud (documents +
 * ledger entries combined) — read from a boolean column, not a separate outbox table, since
 * Phase 1 cloud sync has no outbox (see FirebaseSyncClient). lastSyncedAtEpochMillis is null
 * until the first successful push ever completes.
 */
data class SyncStatus(
    val pendingCount: Int,
    val lastSyncedAtEpochMillis: Long?
)

/**
 * Denormalized projection of a finalized document, just enough to render a Home dashboard row.
 * Deliberately not InvoiceDocumentModel — that requires deserializing the full JSON payload,
 * which a list row never needs.
 */
data class DocumentSummary(
    val documentId: String,
    val documentNumber: String,
    val customerName: String,
    val documentType: DocumentType,
    val issueDate: LocalDate,
    val grandTotal: Money,
    /** When this document was actually finalized — the truth for "most recent", since issueDate is a user-editable business date. */
    val finalizedAtEpochMillis: Long
)
