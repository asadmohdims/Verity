package com.verity.platform.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.verity.platform.database.entities.CustomerEntity

/**
 * CustomerDao
 *
 * PURPOSE
 * -------
 * Persistence interface for CRUD-managed customer identity data.
 *
 * This DAO provides authoritative access to customer records,
 * which are supporting identity data in Verity. Customers are
 * created primarily via bulk import and updated infrequently.
 *
 * INTENT
 * ------
 * • Persist and update customer identity records
 * • Support bulk upsert for Excel-based imports
 * • Provide read-only queries for autocomplete and selection
 *
 * CONSTRAINTS
 * -----------
 * • This table is NOT a source of financial or historical truth
 * • Customer edits must not retroactively affect finalized documents
 * • UI layers must not bypass ViewModels for mutation
 * • Customers are soft-deactivated, not deleted
 */
@Dao
interface CustomerDao {

    /**
     * Inserts or replaces a batch of customer rows.
     *
     * Replacement is safe because customers can be bulk imported and updated.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(customers: List<CustomerEntity>)

    /**
     * Inserts a single customer row.
     *
     * Used for debug seeding and admin tooling.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: CustomerEntity)

    /**
     * Returns all active customers.
     *
     * Intended strictly for read-only use by UI components such as
     * autocomplete, selection dialogs, or diagnostics.
     */
    @Query(
        """
        SELECT *
        FROM customers
        WHERE isActive = 1
        ORDER BY customerName ASC
        """
    )
    suspend fun getActiveCustomers(): List<CustomerEntity>

    /**
     * Deletes all customer rows.
     *
     * Used only during administrative or bootstrap scenarios such as Excel re-import.
     */
    @Query("DELETE FROM customers")
    suspend fun deleteAll()

    /**
     * Returns total number of customers.
     *
     * Used only for bootstrap / debug seeding checks.
     */
    @Query("SELECT COUNT(*) FROM customers")
    suspend fun count(): Int
}