package com.verity.platform.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.verity.platform.database.entities.DocumentIndexEntity

/**
 * DocumentIndexDao
 *
 * PURPOSE
 * -------
 * Persistence interface for the document index projection.
 *
 * This DAO exists solely to support replay-driven writes and
 * fast, read-only access to indexed business documents
 * (Invoices and Challans).
 *
 * INTENT
 * ------
 * • Persist deterministic projection rows derived from events
 * • Support incremental replay via cursor inspection
 * • Provide efficient, read-only queries for navigation and search
 *
 * CONSTRAINTS
 * -----------
 * • This table is NOT a source of truth
 * • All writes must originate from projection writers
 * • UI layers must never mutate this table
 * • Rows may be cleared and rebuilt during full replay
 */
@Dao
interface DocumentIndexDao {

    /**
     * Inserts or replaces a batch of document index rows.
     *
     * Replacement is safe because document_index is a deterministic
     * projection that can always be rebuilt from immutable events.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(documents: List<DocumentIndexEntity>)

    /**
     * Returns the most recently applied projection row based on
     * the global replay cursor.
     *
     * Used by projection writers to determine how far event replay
     * has progressed.
     */
    @Query(
        """
        SELECT *
        FROM document_index
        ORDER BY lastAppliedOccurredAt DESC, lastAppliedEventId DESC
        LIMIT 1
        """
    )
    suspend fun getLatestCursor(): DocumentIndexEntity?

    /**
     * Returns all indexed documents.
     *
     * Intended strictly for read-only use by UI, search,
     * or diagnostics. Callers must not mutate results.
     */
    @Query(
        """
        SELECT *
        FROM document_index
        """
    )
    suspend fun getAll(): List<DocumentIndexEntity>

    /**
     * Deletes all document index rows.
     *
     * Used only during full replay or recovery scenarios where
     * projections are intentionally rebuilt from scratch.
     */
    @Query("DELETE FROM document_index")
    suspend fun clearAll()

    /**
     * Atomically replaces the entire document index projection.
     *
     * Ensures callers do not observe partially-updated state
     * during full rebuild operations.
     */
    @Transaction
    suspend fun replaceAll(documents: List<DocumentIndexEntity>) {
        clearAll()
        upsertAll(documents)
    }
}
