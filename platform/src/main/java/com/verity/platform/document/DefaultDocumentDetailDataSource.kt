package com.verity.platform.document

import com.verity.core.document.model.InvoiceDocumentModel
import com.verity.feature.document.DocumentDetailDataSource
import com.verity.platform.database.PlatformDatabase
import com.verity.platform.sync.FirebaseSyncClient
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
    private val database: PlatformDatabase,
    private val syncClient: FirebaseSyncClient
) : DocumentDetailDataSource {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun loadDocument(documentId: String): InvoiceDocumentModel? {
        val entity = database.documentDao().getById(documentId) ?: return null
        return json.decodeFromString(InvoiceDocumentModel.serializer(), entity.payloadJson)
    }

    override suspend fun findLinkedDocumentId(documentId: String): String? =
        database.documentDao().findByLinkedDocumentId(documentId)?.documentId

    override suspend fun loadSelfNotes(documentId: String): String? =
        database.documentDao().getById(documentId)?.selfNotes

    override suspend fun updateSelfNotes(documentId: String, notes: String?) {
        database.documentDao().updateSelfNotes(documentId, notes)
        // Re-push the full row so the edit reaches other devices via the existing sync path
        // (FirebaseSyncClient.pushDocument does a full .set() overwrite — see its doc comment).
        database.documentDao().getById(documentId)?.let { syncClient.pushDocument(it) }
    }
}
