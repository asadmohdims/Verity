package com.verity.feature.invoice.pdf

import com.verity.core.document.model.InvoiceDocumentModel
import java.io.File

/**
 * InvoicePdfRenderer
 *
 * Port for turning a finalized document into a real PDF file on local storage. Feature defines
 * this interface; platform implements it (same direction as InvoiceFinalizer) because drawing to
 * android.graphics.pdf.PdfDocument and writing to disk is platform infrastructure.
 *
 * Idempotent by design: the file path is deterministic (derived from documentNumber, which is
 * unique and immutable once assigned), so calling this twice for the same document is safe and
 * cheap on the second call - it need not regenerate a file that's already there. That makes this
 * function safe to call both eagerly right after finalize, and again defensively whenever the
 * document is opened for viewing, without any separate "is the PDF ready" state to track.
 */
interface InvoicePdfRenderer {

    /**
     * Returns the local PDF file for [document], generating it first if it doesn't already exist.
     */
    suspend fun ensurePdf(document: InvoiceDocumentModel): File
}
