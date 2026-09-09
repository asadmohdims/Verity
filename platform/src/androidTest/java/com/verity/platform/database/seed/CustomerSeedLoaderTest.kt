package com.verity.platform.database.seed

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerSeedLoaderTest {

    private val context: Context =
        InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun customers_json_loads_successfully() {
        val customers = CustomerSeedLoader.load(context)

        assertTrue(
            "Customer seed JSON must not be empty",
            customers.isNotEmpty()
        )
    }

    @Test
    fun all_customers_are_invoice_ready() {
        val customers = CustomerSeedLoader.load(context)

        customers.forEach { customer ->
            assertTrue(customer.customerName.isNotBlank())
            assertTrue(customer.state.isNotBlank())
            assertTrue(customer.stateCode.isNotBlank())

            // GSTIN is mandatory for invoice preview/finalisation
            assertNotNull(
                "GSTIN must be present for ${customer.customerName}",
                customer.gstin
            )

            // Address must exist (single-line or multi-line is fine)
            assertTrue(
                customer.addressLine1.isNotBlank()
            )
        }
    }

    @Test
    fun pincode_is_optional_and_safe() {
        val customers = CustomerSeedLoader.load(context)

        customers.forEach { customer ->
            // Should never crash or fail parsing
            customer.pincode?.let {
                assertTrue(it.isNotBlank())
            }
        }
    }
}