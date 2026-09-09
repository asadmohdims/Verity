package com.verity.platform.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.verity.platform.database.entities.DocumentEntity

/**
 * DocumentDao
 *
 * Persistence for finalized documents. Insert-only: a duplicate `documentId` must fail loudly
 * (Room's default ABORT conflict strategy), never silently replace — finalized documents are
 * immutable.
 */
@Dao
interface DocumentDao {

    @Insert
    suspend fun insert(document: DocumentEntity)

    /**
     * Highest sequence number assigned so far for this org+type, or null if none exist yet.
     * The caller adds 1 to get the next number. Deliberately no distinct "counter" table —
     * see CLAUDE.md's invoice numbering decision (no collision handling, accepted rare risk).
     */
    @Query(
        """
        SELECT MAX(sequenceNumber)
        FROM documents
        WHERE orgId = :orgId AND documentType = :documentType
        """
    )
    suspend fun getMaxSequenceNumber(orgId: String, documentType: String): Long?

    @Query("SELECT * FROM documents WHERE documentId = :documentId")
    suspend fun getById(documentId: String): DocumentEntity?

    @Query("SELECT * FROM documents")
    suspend fun getAll(): List<DocumentEntity>
}
