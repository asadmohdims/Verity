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
     * Single customer lookup by id, active or not — backs the customer document rollup, which
     * must still show a real name even for a since-deactivated customer.
     */
    @Query("SELECT * FROM customers WHERE customerId = :customerId")
    suspend fun getById(customerId: String): CustomerEntity?

    /**
     * Soft-deactivates a customer — the row stays for historical document lookups
     * (getById is deliberately not filtered by isActive), it just drops out of
     * getActiveCustomers().
     */
    @Query("UPDATE customers SET isActive = 0 WHERE customerId = :customerId")
    suspend fun deactivate(customerId: String)

    /**
     * Bulk REPLACE from a Firestore restore/sync pull — same rationale as
     * DocumentDao.upsertAllFromCloud (safe to re-run in full if interrupted, no separate resume
     * bookkeeping needed). Unlike upsertAll (used for local seeding/imports), this is specifically
     * the cloud-restore entry point — see FirebaseRestoreClient.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllFromCloud(customers: List<CustomerEntity>)

    /** Flips once FirebaseSyncClient.pushCustomer's push succeeds — see CustomerEntity.syncedToCloud. */
    @Query("UPDATE customers SET syncedToCloud = 1 WHERE customerId = :customerId")
    suspend fun markSyncedToCloud(customerId: String)

    /**
     * Every customer row never yet pushed to Firestore — every pre-existing row defaults here
     * after Migration5To6, so this is what pushes a device's already-existing local customers to
     * the cloud for the first time (see MainActivity's seed/restore/backfill sequencing). Cheap,
     * idempotent no-op on every later launch once everything is synced.
     */
    @Query("SELECT * FROM customers WHERE syncedToCloud = 0")
    suspend fun getUnsyncedCustomers(): List<CustomerEntity>

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