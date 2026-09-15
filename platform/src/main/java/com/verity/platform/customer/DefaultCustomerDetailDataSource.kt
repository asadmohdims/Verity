package com.verity.platform.customer

import com.verity.core.document.model.DocumentType
import com.verity.core.formatting.money.Money
import com.verity.feature.customer.detail.CustomerDetail
import com.verity.feature.customer.detail.CustomerDetailDataSource
import com.verity.feature.home.DocumentSummary
import com.verity.platform.database.PlatformDatabase
import com.verity.platform.database.entities.DocumentEntity
import com.verity.platform.finalize.DEFAULT_ORG_ID
import java.time.LocalDate

/**
 * DefaultCustomerDetailDataSource
 *
 * platform's implementation of CustomerDetailDataSource. `DocumentDao.getByCustomerId` already
 * returns denormalized rows (customerName, grandTotalPaise, etc.) — no JSON payload decode
 * needed, same reasoning as DefaultHomeDataSource. balanceDue comes from
 * `LedgerEntryDao.getBalanceForCustomer` — the real per-customer ledger sum, not a re-derivation
 * from document totals (a Challan's goods value never hits the ledger).
 */
class DefaultCustomerDetailDataSource(
    private val database: PlatformDatabase
) : CustomerDetailDataSource {

    override suspend fun loadDetail(customerId: String): CustomerDetail? {
        val customer = database.customerDao().getById(customerId) ?: return null

        val documents = database.documentDao()
            .getByCustomerId(customerId)
            .map { it.toDocumentSummary() }

        val balancePaise = database.ledgerEntryDao()
            .getBalanceForCustomer(orgId = DEFAULT_ORG_ID, customerId = customerId) ?: 0L

        return CustomerDetail(
            customerId = customer.customerId,
            customerName = customer.customerName,
            phone = customer.phone,
            gstin = customer.gstin,
            addressLine1 = customer.addressLine1,
            city = customer.city,
            state = customer.state,
            stateCode = customer.stateCode,
            pincode = customer.pincode,
            balanceDue = Money.ofPaise(balancePaise),
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
