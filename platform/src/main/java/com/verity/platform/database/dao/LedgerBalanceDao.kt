package com.verity.platform.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.verity.platform.database.entities.LedgerBalanceEntity

/**
 * LedgerBalanceDao
 *
 * PURPOSE
 * -------
 * Persistence interface for the ledger balance projection.
 *
 * This DAO exists solely to support replay-driven writes and
 * fast, read-only access to derived customer balances.
 *
 * INTENT
 * ------
 * • Allow replay engine output to be persisted deterministically
 * • Provide efficient balance lookups for UI and queries
 * • Enforce projection-only write semantics (no ad-hoc mutation)
 *
 * CONSTRAINTS
 * -----------
 * • This DAO must never be called directly by UI layers
 * • All writes must originate from replay / projection writers
 * • No method here may alter domain truth
 * • Projection rows may be deleted and rebuilt at any time
 */
@Dao
interface LedgerBalanceDao {

    /**
     * Inserts or replaces a full set of ledger balance rows.
     *
     * This method is intended to be called by replay-driven
     * projection writers after applying a batch of events.
     *
     * Replacement is safe because ledger_balance is a
     * deterministic projection that can always be rebuilt
     * from the immutable event history.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(balances: List<LedgerBalanceEntity>)

    /**
     * Returns the current balance for a specific customer.
     *
     * This is a read-only convenience query for UI and
     * reporting layers. If no row exists, the customer has
     * no financial history yet.
     */
    @Query("""
        SELECT * FROM ledger_balance
        WHERE customerId = :customerId
        LIMIT 1
    """)
    suspend fun getBalanceForCustomer(customerId: String): LedgerBalanceEntity?

    /**
     * Returns all ledger balance rows.
     *
     * Intended for list views, exports, or diagnostics.
     * Callers must treat results as read-only.
     */
    @Query("""
        SELECT * FROM ledger_balance
    """)
    suspend fun getAllBalances(): List<LedgerBalanceEntity>

    /**
     * Deletes all ledger balance rows.
     *
     * This operation is used only during full replay or
     * recovery scenarios where projections are intentionally
     * rebuilt from scratch.
     */
    @Query("""
        DELETE FROM ledger_balance
    """)
    suspend fun clearAll()

    /**
     * Convenience wrapper to atomically replace the entire
     * ledger balance projection.
     *
     * This ensures callers do not observe partially-updated
     * projection state during rebuilds.
     */
    @Transaction
    suspend fun replaceAll(balances: List<LedgerBalanceEntity>) {
        clearAll()
        upsertAll(balances)
    }
}
