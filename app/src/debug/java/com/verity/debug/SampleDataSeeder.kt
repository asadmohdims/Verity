package com.verity.debug

import android.content.Context
import com.verity.feature.invoice.draft.DraftAddress
import com.verity.feature.invoice.draft.DraftDocumentType
import com.verity.feature.invoice.draft.DraftLineItem
import com.verity.feature.invoice.draft.DraftTransportDetails
import com.verity.feature.invoice.draft.InvoiceDraftReducer
import com.verity.feature.invoice.draft.InvoiceDraftUiState
import com.verity.platform.database.PlatformDatabaseFactory
import com.verity.platform.database.entities.CustomerEntity
import com.verity.platform.database.entities.DocumentEntity
import com.verity.platform.database.entities.LedgerEntryEntity
import com.verity.platform.finalize.DefaultInvoiceFinalizer
import com.verity.platform.sync.DefaultInvoiceNumberAllocator
import com.verity.platform.sync.FirebaseSyncClient
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.random.Random

/**
 * SampleDataSeeder
 *
 * DEBUG-ONLY (this whole file lives under app/src/debug, excluded from release builds by the
 * Gradle source set, not just a runtime BuildConfig.DEBUG check). Finalizes sample invoices
 * through the real InvoiceDraftReducer + DefaultInvoiceFinalizer path — not hand-built
 * DocumentEntity rows — so tax computation, sequence numbering, and searchIndexText all come out
 * the same way a real invoice would.
 *
 * Deliberately runs in-process via a BroadcastReceiver (see SeedSampleDataReceiver), not via
 * `connectedAndroidTest`: that command uninstalls the app-under-test (and wipes its data) once
 * the instrumented test finishes — exactly what happened the first time this seeding was
 * attempted that way, wiping every document on the device. This version only ever calls
 * `installDebug`, which never uninstalls anything.
 */
object SampleDataSeeder {

    private data class LineItemTemplate(
        val description: String,
        val hsnCode: String,
        val unit: String,
        val rateRangePaise: LongRange
    )

    private val lineItemPool = listOf(
        LineItemTemplate("Portland Cement", "2523", "BAG", 35_000L..45_000L),
        LineItemTemplate("TMT Steel Bars 12mm", "7214", "KG", 5_500L..7_000L),
        LineItemTemplate("Hydraulic Pump Assembly", "8413", "PCS", 1_500_000L..2_500_000L),
        LineItemTemplate("Industrial Ball Bearing", "8482", "PCS", 80_000L..150_000L),
        LineItemTemplate("Conveyor Belt Rubber", "4010", "MTR", 120_000L..180_000L),
        LineItemTemplate("Motor Coupling Flexible", "8483", "PCS", 200_000L..350_000L),
        LineItemTemplate("Welding Electrode Rods", "8311", "KG", 18_000L..25_000L),
        LineItemTemplate("Lubricant Oil Industrial", "2710", "LTR", 35_000L..50_000L),
        LineItemTemplate("Grey PVC Pipe 4 inch", "3917", "MTR", 22_000L..30_000L),
        LineItemTemplate("Aluminium Sheet 2mm", "7606", "KG", 28_000L..35_000L),
        LineItemTemplate("Copper Wire 4mm", "7408", "KG", 70_000L..90_000L),
        LineItemTemplate("Diesel Generator Set 15KVA", "8502", "PCS", 15_000_000L..22_000_000L)
    )

    private val vehicleNumbers = listOf(
        "MH12AB3456", "MH04CD7890", "GJ05EF1122", "DL8CAG3344",
        "RJ14GH5566", "MH31JK7788", "KA03LM9900", "UP16NO2233"
    )

    private val transporters = listOf(
        "Speed Carriers", "Bharat Roadlines", "Om Sai Transport",
        "National Logistics", "Shree Cargo Movers"
    )

    /** Returns the number of documents actually created. */
    suspend fun seed(context: Context, count: Int = 50): Int {
        val database = PlatformDatabaseFactory.create(context.applicationContext)
        val customers = database.customerDao().getActiveCustomers()
        if (customers.isEmpty()) return 0

        val random = Random(seed = 42)
        // Debug fixture data never touches the network - no-op sync (never pollutes a real
        // Firebase project with fake demo invoices) and a numbering allocator whose online path
        // always fails immediately, falling straight to the local sequence.
        val numberAllocator = DefaultInvoiceNumberAllocator(
            onlineCounterSource = { _, _ -> error("SampleDataSeeder never allocates online") },
            documentDao = database.documentDao()
        )

        repeat(count) { index ->
            val customer = customers[index % customers.size]
            val draft = buildDraft(customer, random)
            val clock = Clock.fixed(
                Instant.now().minus(random.nextLong(0, 60), ChronoUnit.DAYS),
                ZoneOffset.UTC
            )
            DefaultInvoiceFinalizer(database, clock, numberAllocator, NoOpFirebaseSyncClient)
                .finalize(draft, customer.customerId)
        }

        return count
    }

    /** See seed()'s comment above on why debug fixture data never actually reaches Firebase. */
    private object NoOpFirebaseSyncClient : FirebaseSyncClient {
        override fun pushDocument(document: DocumentEntity) = Unit
        override fun pushLedgerEntry(entry: LedgerEntryEntity) = Unit
        override fun pushCustomer(customer: CustomerEntity) = Unit
        override fun pushPdf(orgId: String, documentNumber: String, file: File) = Unit
        override suspend fun downloadPdf(orgId: String, documentNumber: String, destination: File) = false
    }

    private fun buildDraft(customer: CustomerEntity, random: Random): InvoiceDraftUiState {
        val billedTo = DraftAddress(
            name = customer.customerName,
            gstin = customer.gstin,
            addressLine1 = customer.addressLine1,
            city = customer.city,
            state = customer.state,
            stateCode = customer.stateCode,
            pincode = customer.pincode,
            customerId = customer.customerId
        )

        var draft = InvoiceDraftReducer.setBilledTo(InvoiceDraftUiState(), billedTo)

        val itemCount = random.nextInt(1, 4)
        val items = lineItemPool.shuffled(random).take(itemCount)
        items.forEach { template ->
            draft = InvoiceDraftReducer.addLineItem(
                draft,
                DraftLineItem(
                    description = template.description,
                    hsnCode = template.hsnCode,
                    quantity = random.nextLong(1, 25),
                    unit = template.unit,
                    ratePaise = random.nextLong(template.rateRangePaise.first, template.rateRangePaise.last)
                )
            )
        }

        // Roughly two-thirds of documents carry transport details, exercising both the
        // "matches a transport field" and "no logistics at all" search paths.
        if (random.nextInt(0, 3) != 0) {
            draft = InvoiceDraftReducer.setTransportDetails(
                draft,
                DraftTransportDetails(
                    transporterName = transporters.random(random),
                    vehicleNumber = vehicleNumbers.random(random),
                    supplyDate = null,
                    grOrLrNumber = "LR-" + random.nextInt(1000, 9999),
                    freightPaise = if (random.nextBoolean()) random.nextLong(30_000, 80_000) else null,
                    notes = null,
                    ewayBillNumber = "EWAY-" + random.nextInt(100_000, 999_999)
                )
            )
        }

        return InvoiceDraftReducer.setDocumentType(draft, DraftDocumentType.INVOICE)
    }
}
