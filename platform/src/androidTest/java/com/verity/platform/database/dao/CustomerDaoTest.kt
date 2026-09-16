package com.verity.platform.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.verity.platform.database.PlatformDatabase
import com.verity.platform.database.entities.CustomerEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * CustomerDaoTest
 *
 * Real in-memory Room, not mocks — verifying deactivate() genuinely soft-deletes: the row stays
 * queryable by getById (a historical document must still show a real customer name even after
 * deactivation) but drops out of getActiveCustomers().
 */
@RunWith(AndroidJUnit4::class)
class CustomerDaoTest {

    private lateinit var database: PlatformDatabase
    private lateinit var customerDao: CustomerDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PlatformDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        customerDao = database.customerDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun testCustomer(id: String) = CustomerEntity(
        customerId = id,
        customerName = "Test Customer",
        phone = null,
        gstin = "27AAACB1234Z1Z",
        addressLine1 = "Test Address",
        city = "Mumbai",
        state = "Maharashtra",
        stateCode = "27",
        pincode = null,
        isActive = true,
        updatedAt = 0,
        notes = null
    )

    @Test
    fun deactivate_removes_the_customer_from_getActiveCustomers() = runBlocking {
        customerDao.insert(testCustomer("cust-1"))

        assertEquals(1, customerDao.getActiveCustomers().size)

        customerDao.deactivate("cust-1")

        assertTrue(customerDao.getActiveCustomers().isEmpty())
    }

    @Test
    fun deactivate_does_not_delete_the_row_getById_still_finds_it() = runBlocking {
        customerDao.insert(testCustomer("cust-1"))

        customerDao.deactivate("cust-1")

        val found = customerDao.getById("cust-1")
        assertEquals("cust-1", found?.customerId)
        assertEquals(false, found?.isActive)
    }

    @Test
    fun notes_round_trips_through_insert_and_a_later_replace_edit() = runBlocking {
        customerDao.insert(testCustomer("cust-1"))
        assertEquals(null, customerDao.getById("cust-1")?.notes)

        // insert() is REPLACE-on-conflict — the same call an edit-by-id save uses (see
        // DefaultCustomerEditDataSource), so a later edit just re-inserts the full row.
        customerDao.insert(testCustomer("cust-1").copy(notes = "Sold this part at a discounted rate"))

        assertEquals("Sold this part at a discounted rate", customerDao.getById("cust-1")?.notes)
    }

    @Test
    fun new_customer_defaults_to_not_yet_synced() = runBlocking {
        customerDao.insert(testCustomer("cust-1"))

        assertEquals(false, customerDao.getById("cust-1")?.syncedToCloud)
        assertEquals(listOf("cust-1"), customerDao.getUnsyncedCustomers().map { it.customerId })
    }

    @Test
    fun markSyncedToCloud_removes_the_customer_from_getUnsyncedCustomers() = runBlocking {
        customerDao.insert(testCustomer("cust-1"))

        customerDao.markSyncedToCloud("cust-1")

        assertEquals(true, customerDao.getById("cust-1")?.syncedToCloud)
        assertTrue(customerDao.getUnsyncedCustomers().isEmpty())
    }

    @Test
    fun upsertAllFromCloud_replaces_an_existing_row_by_id() = runBlocking {
        customerDao.insert(testCustomer("cust-1"))

        customerDao.upsertAllFromCloud(
            listOf(testCustomer("cust-1").copy(customerName = "Renamed via cloud", syncedToCloud = true))
        )

        val found = customerDao.getById("cust-1")
        assertEquals("Renamed via cloud", found?.customerName)
        assertEquals(true, found?.syncedToCloud)
        assertEquals(1, customerDao.count())
    }
}
