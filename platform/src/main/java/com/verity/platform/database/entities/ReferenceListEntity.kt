package com.verity.platform.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * ReferenceListEntity
 *
 * One row of a manually-curated reference list (Transporter Name, HSN Code — see
 * ReferenceListKind in feature). A single generic table with a [kind] discriminator rather than
 * one table per kind: both are the same shape (a short string value the user maintains), so a
 * second near-identical table would just be duplication, not a real schema difference.
 *
 * The unique index on (kind, value) is case-sensitive at the SQLite level; case-insensitive
 * dedup is enforced one layer up, in DefaultReferenceListDataSource.add(), before the insert.
 */
@Entity(
    tableName = "reference_list_items",
    indices = [Index(value = ["kind", "value"], unique = true)]
)
data class ReferenceListEntity(
    @PrimaryKey
    val id: String,
    val kind: String,
    val value: String,
    val createdAt: Long
)
