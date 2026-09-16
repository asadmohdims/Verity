package com.verity.platform.customer

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.verity.feature.customer.edit.CustomerFormData
import com.verity.platform.database.PlatformDatabase
import com.verity.platform.database.entities.CustomerEntity
import com.verity.platform.database.entities.DocumentEntity
import com.verity.platform.database.entities.LedgerEntryEntity
import com.verity.platform.sync.FirebaseSyncClient
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * DefaultCustomerEditDataSourceTest
 *
 * Verifies save()/deactivate() both push the resulting row to Firestore, against a real
 * in-memory Room instance and a recording FirebaseSyncClient fake — same pattern as
 * DefaultInvoiceFinalizerTest's RecordingFirebaseSyncClient. This is the fix for the
 * customerId-divergence bug: every device used to seed its own random customer ids with nothing
 * ever reaching the cloud to make them canonical.
 */
@RunWith(AndroidJUnit4::class)
class DefaultCustomerEditDataSourceTest {

    private lateinit var database: PlatformDatabase
    private lateinit var syncClient: RecordingFirebaseSyncClient
    private lateinit var dataSource: DefaultCustomerEditDataSource

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PlatformDatabase::class.java
        )
            .allowMainThreadQueries() // instrumentation tests only
            .build()

        syncClient = RecordingFirebaseSyncClient()
        dataSource = DefaultCustomerEditDataSource(database, syncClient)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun testForm(id: String = "cust-1") = CustomerFormData(
        customerId = id,
        customerName = "Test Buyer",
        gstin = "27AAACB1234Z1Z",
        phone = null,
        addressLine1 = "Test Address",
        city = "Mumbai",
        state = "Maharashtra",
        stateCode = "27",
        pincode = null,
        updatedAtEpochMillis = 1_000L
    )

    @Test
    fun save_pushes_the_saved_customer_to_the_cloud() = runBlocking {
        dataSource.save(testForm())

        assertEquals(1, syncClient.pushedCustomers.size)
        assertEquals("cust-1", syncClient.pushedCustomers.single().customerId)
        assertEquals("Test Buyer", syncClient.pushedCustomers.single().customerName)
    }

    @Test
    fun deactivate_pushes_the_now_inactive_customer_to_the_cloud() = runBlocking {
        dataSource.save(testForm())
        syncClient.pushedCustomers.clear()

        dataSource.deactivate("cust-1")

        assertEquals(1, syncClient.pushedCustomers.size)
        assertTrue(!syncClient.pushedCustomers.single().isActive)
    }

    private class RecordingFirebaseSyncClient : FirebaseSyncClient {
        val pushedCustomers = mutableListOf<CustomerEntity>()

        override fun pushDocument(document: DocumentEntity) = Unit
        override fun pushLedgerEntry(entry: LedgerEntryEntity) = Unit
        override fun pushCustomer(customer: CustomerEntity) {
            pushedCustomers += customer
        }
        override fun pushPdf(orgId: String, documentNumber: String, file: File) = Unit
        override suspend fun downloadPdf(orgId: String, documentNumber: String, destination: File) = false
    }
}
