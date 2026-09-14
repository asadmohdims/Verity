package com.verity.platform.customer

import com.verity.core.document.model.DocumentType
import com.verity.core.formatting.money.Money
import com.verity.feature.customer.rollup.CustomerRollup
import com.verity.feature.customer.rollup.CustomerRollupDataSource
import com.verity.feature.home.DocumentSummary
import com.verity.platform.database.PlatformDatabase
import com.verity.platform.database.entities.DocumentEntity
import java.time.LocalDate

/**
 * DefaultCustomerRollupDataSource
 *
 * platform's implementation of CustomerRollupDataSource. `DocumentDao.getByCustomerId` already
 * returns denormalized rows (customerName, grandTotalPaise, etc.) — no JSON payload decode
 * needed, same reasoning as DefaultHomeDataSource.
 */
class DefaultCustomerRollupDataSource(
    private val database: PlatformDatabase
) : CustomerRollupDataSource {

    override suspend fun loadRollup(customerId: String): CustomerRollup? {
        val customer = database.customerDao().getById(customerId) ?: return null

        val documents = database.documentDao()
            .getByCustomerId(customerId)
            .map { it.toDocumentSummary() }

        return CustomerRollup(
            customerId = customer.customerId,
            customerName = customer.customerName,
            documents = documents
        )
    }
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
