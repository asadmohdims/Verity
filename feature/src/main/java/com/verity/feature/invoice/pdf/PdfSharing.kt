package com.verity.feature.invoice.pdf

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * Hands [file] to any installed app via the system share sheet (WhatsApp, Gmail, ... — WhatsApp
 * is the real-world delivery channel this was built for). Goes through FileProvider rather than a
 * raw file:// Uri: Android has blocked exposing app-private file:// paths to other apps since API
 * 24 (FileUriExposedException) — see the `<provider>` declaration in AndroidManifest.xml and
 * app/src/main/res/xml/file_paths.xml, scoped to exactly the "documents" folder PDFs live in
 * (DefaultInvoicePdfRenderer), not app-private storage generally.
 *
 * The document number shown as the share subject/chooser title is read off [file]'s own name
 * rather than passed in separately — DefaultInvoicePdfRenderer always names the file
 * "{documentNumber}.pdf", so the file itself is the one source of truth for it.
 */
fun sharePdf(context: Context, file: File) {
    val documentNumber = file.nameWithoutExtension
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, documentNumber)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share $documentNumber"))
}

/**
 * Bulk version of [sharePdf], for the Documents list's multi-select share action — same
 * FileProvider authority/scope, just Android's multi-file share intent instead of the single-file
 * one. A no-op on an empty list rather than firing a chooser with nothing attached.
 */
fun sharePdfs(context: Context, files: List<File>) {
    if (files.isEmpty()) return

    val uris = ArrayList(
        files.map { file -> FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file) }
    )
    val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "application/pdf"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share ${files.size} documents"))
}

/**
 * Sends [file] to Android's native print framework (the system print dialog — any registered
 * printer, "Save as PDF", etc.), not a share-to-a-printing-app workaround. [PdfFileAdapter]
 * streams the already-rendered PDF's bytes directly rather than redrawing the document — this is
 * the exact file generated at finalize, and re-rendering it here would risk a subtly different
 * printed page than what's actually on disk.
 */
fun printPdf(context: Context, file: File) {
    val documentNumber = file.nameWithoutExtension
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    printManager.print(
        documentNumber,
        PdfFileAdapter(file, documentNumber),
        PrintAttributes.Builder().build()
    )
}

private class PdfFileAdapter(
    private val file: File,
    private val jobName: String
) : PrintDocumentAdapter() {

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder(jobName)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .build()
        callback.onLayoutFinished(info, oldAttributes != newAttributes)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback
    ) {
        try {
            FileInputStream(file).use { input ->
                FileOutputStream(destination.fileDescriptor).use { output ->
                    input.copyTo(output)
                }
            }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: IOException) {
            callback.onWriteFailed(e.message)
        }
    }
}
