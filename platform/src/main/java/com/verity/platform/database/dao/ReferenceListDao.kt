package com.verity.platform.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.verity.platform.database.entities.ReferenceListEntity

/**
 * ReferenceListDao
 *
 * Persistence for the manually-curated reference lists (Transporter Name, HSN Code — see
 * ReferenceListEntity's doc comment for why one table serves both).
 */
@Dao
interface ReferenceListDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: ReferenceListEntity)

    @Query("SELECT * FROM reference_list_items WHERE kind = :kind ORDER BY value ASC")
    suspend fun getAll(kind: String): List<ReferenceListEntity>

    @Query("DELETE FROM reference_list_items WHERE id = :id")
    suspend fun deleteById(id: String)
}
