package com.verity.platform.home

import com.verity.core.document.model.DocumentType
import com.verity.core.formatting.money.Money
import com.verity.feature.home.DocumentSummary
import com.verity.feature.home.HomeDataSource
import com.verity.platform.database.PlatformDatabase
import com.verity.platform.database.entities.DocumentEntity
import java.time.LocalDate

/**
 * DefaultHomeDataSource
 *
 * platform's implementation of HomeDataSource: reads finalized documents straight off
 * DocumentDao.getAll() and maps the already-denormalized DocumentEntity columns to
 * DocumentSummary. No new query, no JSON payload deserialization — a Home dashboard row never
 * needs the full InvoiceDocumentModel.
 */
class DefaultHomeDataSource(
    private val database: PlatformDatabase
) : HomeDataSource {

    override suspend fun loadAllDocuments(): List<DocumentSummary> =
        database.documentDao().getAll().map { it.toDocumentSummary() }
}

private fun DocumentEntity.toDocumentSummary(): DocumentSummary =
    DocumentSummary(
        documentId = documentId,
        documentNumber = documentNumber,
        customerName = customerName,
        documentType = DocumentType.valueOf(documentType),
        issueDate = LocalDate.ofEpochDay(issueDateEpochDay),
        grandTotal = Money.ofPaise(grandTotalPaise),
        finalizedAtEpochMillis = finalizedAt
    )
