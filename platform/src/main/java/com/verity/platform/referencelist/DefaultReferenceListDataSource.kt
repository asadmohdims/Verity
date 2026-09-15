package com.verity.platform.referencelist

import com.verity.feature.referencelist.ReferenceListDataSource
import com.verity.feature.referencelist.ReferenceListItem
import com.verity.feature.referencelist.ReferenceListKind
import com.verity.platform.database.dao.ReferenceListDao
import com.verity.platform.database.entities.ReferenceListEntity
import java.time.Clock
import java.util.UUID

/**
 * DefaultReferenceListDataSource
 *
 * Platform-owned implementation of ReferenceListDataSource, backed by ReferenceListDao.
 */
class DefaultReferenceListDataSource(
    private val referenceListDao: ReferenceListDao,
    private val clock: Clock = Clock.systemDefaultZone()
) : ReferenceListDataSource {

    override suspend fun getAll(kind: ReferenceListKind): List<ReferenceListItem> =
        referenceListDao.getAll(kind.name).map { ReferenceListItem(id = it.id, value = it.value) }

    override suspend fun add(kind: ReferenceListKind, value: String) {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return

        // Case-insensitive dedup: the unique index on (kind, value) is case-sensitive at the
        // SQLite level, so "MZN Transport" and "mzn transport" wouldn't collide there.
        val alreadyPresent = referenceListDao.getAll(kind.name)
            .any { it.value.equals(trimmed, ignoreCase = true) }
        if (alreadyPresent) return

        referenceListDao.insert(
            ReferenceListEntity(
                id = UUID.randomUUID().toString(),
                kind = kind.name,
                value = trimmed,
                createdAt = clock.millis()
            )
        )
    }

    override suspend fun delete(kind: ReferenceListKind, id: String) {
        referenceListDao.deleteById(id)
    }
}
