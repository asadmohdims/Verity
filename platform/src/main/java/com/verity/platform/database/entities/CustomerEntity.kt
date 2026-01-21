package com.verity.platform.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

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
     * Indicates whether the customer is active.
     */
    val isActive: Boolean,

    /**
     * System time when this customer record was last updated.
     */
    val updatedAt: Long
)