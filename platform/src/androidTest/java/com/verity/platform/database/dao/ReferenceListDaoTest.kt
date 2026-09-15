package com.verity.platform.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.verity.platform.database.PlatformDatabase
import com.verity.platform.database.entities.ReferenceListEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ReferenceListDaoTest
 *
 * Real in-memory Room, not mocks — verifies getAll() is scoped per kind (a Transporter Name
 * never leaks into the HSN Code list, and vice versa) and that deleteById() actually removes the
 * row rather than soft-deleting it (unlike CustomerDao.deactivate — nothing else references these
 * rows, so a hard delete is safe here).
 */
@RunWith(AndroidJUnit4::class)
class ReferenceListDaoTest {

    private lateinit var database: PlatformDatabase
    private lateinit var referenceListDao: ReferenceListDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PlatformDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        referenceListDao = database.referenceListDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun item(id: String, kind: String, value: String) = ReferenceListEntity(
        id = id,
        kind = kind,
        value = value,
        createdAt = 0
    )

    @Test
    fun getAll_is_scoped_to_the_requested_kind() = runBlocking {
        referenceListDao.insert(item("t-1", "TRANSPORTER_NAME", "MZN Transport"))
        referenceListDao.insert(item("h-1", "HSN_CODE", "7208"))

        val transporters = referenceListDao.getAll("TRANSPORTER_NAME")
        val hsnCodes = referenceListDao.getAll("HSN_CODE")

        assertEquals(listOf("MZN Transport"), transporters.map { it.value })
        assertEquals(listOf("7208"), hsnCodes.map { it.value })
    }

    @Test
    fun deleteById_removes_the_row_entirely() = runBlocking {
        referenceListDao.insert(item("t-1", "TRANSPORTER_NAME", "MZN Transport"))

        referenceListDao.deleteById("t-1")

        assertTrue(referenceListDao.getAll("TRANSPORTER_NAME").isEmpty())
    }

    @Test
    fun a_duplicate_kind_and_value_is_silently_ignored_by_the_unique_index() = runBlocking {
        referenceListDao.insert(item("t-1", "TRANSPORTER_NAME", "MZN Transport"))
        referenceListDao.insert(item("t-2", "TRANSPORTER_NAME", "MZN Transport"))

        assertEquals(1, referenceListDao.getAll("TRANSPORTER_NAME").size)
    }
}
