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
     *
     * This identifier is stable across the entire lifecycle
     * of the customer and is used as the primary lookup key
     * throughout the system.
     */
    @PrimaryKey
    val customerId: String,

    /**
     * Canonical customer name.
     *
     * This represents the current authoritative name of the
     * customer.
     */
    val customerName: String,

    /**
     * Primary phone number associated with the customer.
     *
     * Nullable because a customer may be created without
     * contact information.
     */
    val phone: String?,

    /**
     * GSTIN associated with the customer, if available.
     *
     * Nullable for non-GST or unregistered customers.
     */
    val gstin: String?,

    /**
     * City associated with the customer's primary address.
     *
     * Used for search refinement and display only.
     */
    val city: String?,

    /**
     * State associated with the customer's primary address.
     *
     * Used for tax context and UI display.
     */
    val state: String?,

    /**
     * GST state code associated with the customer's primary address.
     *
     * This is the official 2-digit numeric GST state code
     * (e.g. "27" for Maharashtra, "29" for Karnataka).
     *
     * This field is used for:
     * • Draft tax derivation (CGST/SGST vs IGST)
     * • GSTIN validation (future)
     *
     * Nullable to support:
     * • Non-GST customers
     * • Legacy / incomplete imports
     */
    val stateCode: String?,

    /**
     * Indicates whether the customer is currently active.
     *
     * Inactive customers remain in the table for historical
     * reference and search, but may be restricted from
     * new document creation.
     */
    val isActive: Boolean,

    /**
     * System time when this customer record was last updated.
     *
     * Used strictly for diagnostics and debugging.
     */
    val updatedAt: Long
)