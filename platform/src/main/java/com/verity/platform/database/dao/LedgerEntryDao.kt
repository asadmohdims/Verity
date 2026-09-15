package com.verity.platform.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.verity.platform.database.entities.LedgerEntryEntity

/**
 * LedgerEntryDao
 *
 * Append-only for the normal write path — no update/delete methods for that. upsertAllFromCloud
 * is the one exception, used only by FirebaseRestoreClient's new-device bulk restore.
 */
@Dao
interface LedgerEntryDao {

    @Insert
    suspend fun insert(entry: LedgerEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllFromCloud(entries: List<LedgerEntryEntity>)

    @Query("UPDATE ledger_entries SET syncedToCloud = 1 WHERE entryId = :entryId")
    suspend fun markSyncedToCloud(entryId: String)

    @Query("SELECT COUNT(*) FROM ledger_entries WHERE syncedToCloud = 0")
    suspend fun getPendingSyncCount(): Int

    @Query(
        """
        SELECT SUM(amountPaise)
        FROM ledger_entries
        WHERE orgId = :orgId AND customerId = :customerId
        """
    )
    suspend fun getBalanceForCustomer(orgId: String, customerId: String): Long?
}
