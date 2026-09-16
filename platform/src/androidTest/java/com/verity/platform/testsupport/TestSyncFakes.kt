package com.verity.platform.testsupport

import com.verity.platform.database.dao.DocumentDao
import com.verity.platform.database.entities.CustomerEntity
import com.verity.platform.database.entities.DocumentEntity
import com.verity.platform.database.entities.LedgerEntryEntity
import com.verity.platform.sync.DefaultInvoiceNumberAllocator
import com.verity.platform.sync.FirebaseSyncClient
import com.verity.platform.sync.InvoiceNumberAllocator
import java.io.File

/**
 * Shared androidTest fakes for constructing DefaultInvoiceFinalizer without any real Firebase
 * dependency — most androidTest suites in this module only need a *working* finalizer to set up
 * fixture data; they aren't testing the sync path itself (DefaultInvoiceFinalizerTest is, and
 * uses its own recording fake for that reason).
 */
object NoOpFirebaseSyncClient : FirebaseSyncClient {
    override fun pushDocument(document: DocumentEntity) = Unit
    override fun pushLedgerEntry(entry: LedgerEntryEntity) = Unit
    override fun pushCustomer(customer: CustomerEntity) = Unit
    override fun pushPdf(orgId: String, documentNumber: String, file: File) = Unit
    override suspend fun downloadPdf(orgId: String, documentNumber: String, destination: File) = false
}

/** Always falls straight to the local sequence — no real Firestore counter in these tests. */
fun localOnlyInvoiceNumberAllocator(documentDao: DocumentDao): InvoiceNumberAllocator =
    DefaultInvoiceNumberAllocator(
        onlineCounterSource = { _, _ -> error("test: online counter is not used here") },
        documentDao = documentDao
    )
