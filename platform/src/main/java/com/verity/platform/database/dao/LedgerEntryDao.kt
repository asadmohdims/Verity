package com.verity.platform.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.verity.platform.database.entities.LedgerEntryEntity

/**
 * LedgerEntryDao
 *
 * Append-only. No update/delete methods exist by design.
 */
@Dao
interface LedgerEntryDao {

    @Insert
    suspend fun insert(entry: LedgerEntryEntity)

    @Query(
        """
        SELECT SUM(amountPaise)
        FROM ledger_entries
        WHERE orgId = :orgId AND customerId = :customerId
        """
    )
    suspend fun getBalanceForCustomer(orgId: String, customerId: String): Long?
}
