package com.verity.platform.finalize

import com.verity.feature.invoice.draft.DraftDocumentType
import com.verity.feature.invoice.finalize.DocumentNumberPreviewDataSource
import com.verity.platform.database.dao.DocumentDao

/**
 * Reads the same local "max sequence + 1" query InvoiceNumberAllocator's offline fallback uses
 * (see that class) — a non-mutating peek, not an allocation. Deliberately local-only, no Firestore
 * round trip: this is a cosmetic hint on a draft screen, not a value anything depends on being
 * exactly right, so it isn't worth a network call or the latency/failure handling one would need.
 * It can trail the real online counter by one if another device finalized more recently and this
 * device hasn't synced yet — the same accepted, narrow mismatch category the numbering design
 * already documents elsewhere.
 */
class DefaultDocumentNumberPreviewDataSource(
    private val documentDao: DocumentDao
) : DocumentNumberPreviewDataSource {

    override suspend fun peekNextNumber(documentType: DraftDocumentType): String {
        val (column, prefix) = when (documentType) {
            DraftDocumentType.INVOICE -> "INVOICE" to "INV-"
            DraftDocumentType.CHALLAN -> "CHALLAN" to "CH-"
        }
        val next = (documentDao.getMaxSequenceNumber(DEFAULT_ORG_ID, column) ?: 0L) + 1
        return prefix + next.toString().padStart(6, '0')
    }
}
