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
     * Checks local disk, then Firebase Storage, before falling back to on-device rendering - the
     * right order for a document that might already have a PDF from an earlier session or another
     * device (e.g. the PDF viewer opening it defensively).
     */
    suspend fun ensurePdf(document: InvoiceDocumentModel): File

    /**
     * Renders and returns a fresh PDF for [document], skipping ensurePdf()'s local-file and
     * Storage-download checks. For a document that was JUST finalized in this same call: it
     * cannot already exist on disk or in Storage (nothing's been generated or uploaded for it
     * yet), so those checks are two guaranteed-to-fail round trips, not real ones - confirmed live
     * on-device to add ~1-2s of real network latency (a Storage 404) to every single finalize for
     * no benefit. Default delegates to ensurePdf() so existing fakes/tests don't need updating;
     * the real renderer overrides it to actually skip the checks.
     */
    suspend fun generateFreshPdf(document: InvoiceDocumentModel): File = ensurePdf(document)
}
