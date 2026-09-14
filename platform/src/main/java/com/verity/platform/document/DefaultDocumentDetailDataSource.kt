package com.verity.platform.document

import com.verity.core.document.model.InvoiceDocumentModel
import com.verity.feature.document.DocumentDetailDataSource
import com.verity.platform.database.PlatformDatabase
import kotlinx.serialization.json.Json

/**
 * DefaultDocumentDetailDataSource
 *
 * platform's implementation of DocumentDetailDataSource: reads the row via DocumentDao.getById
 * and deserializes its payloadJson back into the full InvoiceDocumentModel it was written from
 * (see DefaultInvoiceFinalizer, which writes it via Json.encodeToString).
 *
 * ignoreUnknownKeys = true here (unlike DefaultInvoiceFinalizer's encode-side Json, where the
 * flag has no effect) is the standard defensive choice for a read path: it must keep decoding
 * older persisted rows once new optional fields are added to InvoiceDocumentModel later.
 */
class DefaultDocumentDetailDataSource(
    private val database: PlatformDatabase
) : DocumentDetailDataSource {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun loadDocument(documentId: String): InvoiceDocumentModel? {
        val entity = database.documentDao().getById(documentId) ?: return null
        return json.decodeFromString(InvoiceDocumentModel.serializer(), entity.payloadJson)
    }
}
