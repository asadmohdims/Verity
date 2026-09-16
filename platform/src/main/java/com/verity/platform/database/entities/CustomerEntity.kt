package com.verity.platform.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.verity.platform.finalize.DEFAULT_ORG_ID

/**
 * CustomerEntity
 *
 * PURPOSE
 * -------
 * Represents the canonical, CRUD-managed customer identity record.
 *
 * Customers are supporting identity data in Verity. They are created
 * primarily via bulk import (e.g. Excel) and updated infrequently.
 * This table serves as the authoritative source for customer identity
 * during draft creation and UI workflows.
 *
 * INTENT
 * ------
 * • Provide a simple, authoritative customer identity store
 * • Support autocomplete and selection during invoice creation
 * • Allow controlled creation, update, and deactivation of customers
 *
 * CONSTRAINTS
 * -----------
 * • This table is NOT a source of financial or historical truth
 * • Customer edits must not retroactively affect finalized documents
 * • Invoices and Challans snapshot customer identity at creation time
 * • Rows are mutated only via explicit CRUD operations
 */
@Entity(tableName = "customers")
data class CustomerEntity(

    /**
     * Globally unique identifier of the customer.
     */
    @PrimaryKey
    val customerId: String,

    /**
     * Canonical customer name.
     *
     * Mandatory for all invoices.
     */
    val customerName: String,

    /**
     * Optional contact phone number.
     */
    val phone: String?,

    // ─────────────────────────────────────────
    // Invoice‑grade identity (v1)
    // ─────────────────────────────────────────

    /**
     * GSTIN associated with the customer.
     *
     * Mandatory for invoice generation in v1.
     */
    val gstin: String,

    /**
     * Primary billing address line.
     */
    val addressLine1: String,

    /**
     * City associated with the customer's address.
     */
    val city: String,

    /**
     * State associated with the customer's address.
     */
    val state: String,

    /**
     * GST state code (2‑digit numeric, e.g. "27").
     */
    val stateCode: String,

    /**
     * Postal pincode associated with the customer's address.
     *
     * OPTIONAL:
     * - Not required for invoice validity in v1
     * - Included for future logistics, analytics, and address enrichment
     */
    val pincode: String?,

    /**
     * Indicates whether the customer is active.
     */
    val isActive: Boolean,

    /**
     * System time when this customer record was last updated.
     */
    val updatedAt: Long,

    /**
     * Free-form private notes about this customer (pricing agreed, contact history, etc).
     * Read and updated freely via CRUD — not structured, not event-sourced. Added schema v5,
     * see Migration4To5.
     */
    val notes: String? = null,

    /**
     * The one gap among synced entities before schema v6: DocumentEntity/LedgerEntryEntity have
     * always self-described their org, CustomerEntity never did (single-org app, never needed
     * for a purely local table). Added alongside cloud sync (Migration5To6) so a customer's
     * Firestore path (orgs/{orgId}/customers/{customerId}) doesn't depend on a caller-supplied
     * constant.
     */
    val orgId: String = DEFAULT_ORG_ID,

    /**
     * Flips to true once FirebaseSyncClient.pushCustomer's push succeeds — same role as
     * DocumentEntity.syncedToCloud. Also doubles as the one-time-backfill marker: every row that
     * existed before schema v6 defaults to false, so CustomerDao.getUnsyncedCustomers() naturally
     * picks up this device's pre-existing customers the first time it runs post-upgrade (see
     * MainActivity's seed/restore/backfill sequencing).
     */
    val syncedToCloud: Boolean = false
)