package com.verity.platform.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.text.TextUtils
import com.verity.core.document.model.DocumentParty
import com.verity.core.document.model.DocumentType
import com.verity.core.document.model.InvoiceDocumentModel
import com.verity.core.formatting.money.AmountInWords
import com.verity.core.formatting.money.Money
import com.verity.feature.invoice.pdf.InvoicePdfRenderer
import com.verity.platform.sync.FirebaseSyncClient
import java.io.File
import java.io.FileOutputStream
import java.time.format.DateTimeFormatter

/**
 * DefaultInvoicePdfRenderer
 *
 * Draws InvoiceDocumentModel to a real PDF file using android.graphics.pdf.PdfDocument +
 * Canvas/Paint - vector text output (crisp, selectable, small file size), zero new dependencies,
 * fully on-device (no network, no downloadable fonts - see CLAUDE.md's offline requirement).
 *
 * Layout follows the finalized "Familiar Grid, Modernized" design
 * (Main.dc.html in the approved PDF design artifact) field-for-field and color-for-color, at a
 * faithful-but-not-pixel-exact translation from its CSS mockup to PDF point geometry (the mockup
 * is 794x1123 CSS px at 96dpi; this draws true A4 at 595x842pt/72dpi, i.e. mockup-px * 0.75).
 * Typography uses Android's built-in sans-serif/monospace families rather than bundling the
 * mockup's exact web fonts (Public Sans / IBM Plex Mono) - a deliberate simplification to avoid
 * new font assets for a first pass; easy to upgrade later without touching layout logic.
 *
 * ensurePdf()'s fallback order is local file -> Storage download -> regenerate (last resort).
 * Regeneration is no longer the normal "file missing" path: since the exact PDF is backed up to
 * Firebase Storage after generation (see the cloud-sync plan's PDF backup section - chosen for
 * GST audit fidelity over relying on regeneration, which could drift from the original if the
 * renderer's output ever changes in a future app version), a missing local file on a restored or
 * reinstalled device should almost always be satisfied by the Storage download instead.
 * Regeneration remains only for the rare case where even the upload hasn't landed yet.
 */
class DefaultInvoicePdfRenderer(
    private val context: Context,
    private val syncClient: FirebaseSyncClient,
    private val orgId: String
) : InvoicePdfRenderer {

    override suspend fun ensurePdf(document: InvoiceDocumentModel): File {
        val file = pdfFile(document.identity.documentNumber)
        if (file.exists()) return file

        val downloaded = syncClient.downloadPdf(orgId, document.identity.documentNumber, file)
        if (downloaded) return file

        val pdfDocument = InvoicePdfPageDrawer(document).draw()
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { pdfDocument.writeTo(it) }
        pdfDocument.close()
        // Fire-and-forget, mirroring FirebaseSyncClient's push pattern elsewhere - never blocks
        // the PDF viewer on the upload.
        syncClient.pushPdf(orgId, document.identity.documentNumber, file)
        return file
    }

    private fun pdfFile(documentNumber: String): File {
        // getExternalFilesDir returns null only if external storage isn't currently mounted
        // (rare) - fall back to internal storage so this never hard-fails.
        val documentsDir = context.getExternalFilesDir("documents")
            ?: File(context.filesDir, "documents").apply { mkdirs() }
        return File(documentsDir, "$documentNumber.pdf")
    }
}

/* ---------- Palette & type (this document's own look, distinct from the app's VerityColors -
   the printed invoice deliberately uses a bespoke navy+brass business-document palette, not the
   app's mobile UI chrome palette) ---------- */

private object InvoicePalette {
    val ink = Color.parseColor("#1a1f26")
    val muted = Color.parseColor("#6b7280")
    val mutedTwo = Color.parseColor("#8a919c")
    val line = Color.parseColor("#dde1e6")
    val navy = Color.parseColor("#1f3a5c")
    val washStrong = Color.parseColor("#e7edf3")
    val washSoft = Color.parseColor("#f5f8fa")
    val brass = Color.parseColor("#cda45b")
    val brassDeep = Color.parseColor("#8a5a1f")
    val white = Color.WHITE
}

/* ---------- Page geometry (points, 72/inch; true A4 = 595 x 842) ---------- */

private const val PAGE_WIDTH = 595f
private const val PAGE_HEIGHT = 842f
private const val MARGIN = 32f
private const val CONTENT_LEFT = MARGIN
private const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN
private const val CONTENT_RIGHT = PAGE_WIDTH - MARGIN
private const val BOTTOM_LIMIT = PAGE_HEIGHT - MARGIN

private const val BAR_HEIGHT = 15f
private const val DETAIL_ROW_HEIGHT = 15f
private const val TABLE_HEADER_HEIGHT = 14f
private const val TABLE_ROW_HEIGHT = 13f
private const val HEADER_BAND_HEIGHT = 96f
private const val GRAND_TOTAL_BAR_HEIGHT = 22f
/** Totals box: 5 rows (Sub Total, Freight, CGST, SGST, IGST) + the grand-total bar, plus a
 *  trailing gap before the next section - used to decide whether to page-break before drawing it. */
private const val TOTALS_BLOCK_HEIGHT = 5 * DETAIL_ROW_HEIGHT + GRAND_TOTAL_BAR_HEIGHT + 12f

private val printDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

/**
 * Draws one InvoiceDocumentModel across one or more PdfDocument pages. Line items are the only
 * section that can genuinely overflow a page (design validated up to 10 items on one page, but
 * a real invoice could exceed that) - silently truncating a financial document's line items would
 * be a real correctness bug, so the item table paginates rather than assuming a single page.
 */
private class InvoicePdfPageDrawer(private val document: InvoiceDocumentModel) {

    private val pdf = PdfDocument()
    private lateinit var page: PdfDocument.Page
    private lateinit var canvas: Canvas
    private var pageNumber = 0
    private var y = 0f

    fun draw(): PdfDocument {
        startNewPage()
        drawTopStrip()
        drawHeaderBand()
        drawDetailsGrid()
        drawParties()
        drawLineItemsTable()
        ensureSpace(TOTALS_BLOCK_HEIGHT)
        drawTotalsAndWords()
        drawBankDetails()
        drawSignatureBlock()
        drawFooterTerms()
        pdf.finishPage(page)
        return pdf
    }

    private fun startNewPage() {
        if (::page.isInitialized) pdf.finishPage(page)
        pageNumber++
        val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), pageNumber).create()
        page = pdf.startPage(info)
        canvas = page.canvas
        y = MARGIN
    }

    private fun ensureSpace(height: Float) {
        if (y + height > BOTTOM_LIMIT) startNewPage()
    }

    // ---------- Top strip: GSTIN | TAX INVOICE | Original/Duplicate/Triplicate ----------

    private fun drawTopStrip() {
        val baseline = y + 8f
        canvas.drawText(
            "GSTIN  ${document.identity.seller.gstin.orEmpty()}",
            CONTENT_LEFT,
            baseline,
            paint(InvoicePalette.muted, 7.5f, mono = true)
        )

        val title = when (document.identity.documentType) {
            DocumentType.INVOICE -> "TAX INVOICE"
            DocumentType.CHALLAN -> "DELIVERY CHALLAN"
        }
        canvas.drawText(
            title,
            PAGE_WIDTH / 2f,
            baseline,
            paint(InvoicePalette.navy, 9.5f, bold = true, align = Paint.Align.CENTER)
        )

        // Copy marking: this build only ever produces one PDF per finalize, always marked
        // "Original" - no copy-type selector (Original/Duplicate/Triplicate print run) exists or
        // is planned; see the PDF-generation plan for why that's out of scope for now.
        val otherCopiesText = "Duplicate    Triplicate"
        val otherCopiesPaint = paint(InvoicePalette.mutedTwo, 7.5f, align = Paint.Align.RIGHT)
        canvas.drawText(otherCopiesText, CONTENT_RIGHT, baseline, otherCopiesPaint)
        val otherCopiesWidth = otherCopiesPaint.measureText(otherCopiesText)

        val badgeText = "Original"
        val badgePaint = paint(InvoicePalette.brass, 7.5f, bold = true)
        val badgeWidth = badgePaint.measureText(badgeText) + 14f
        val badgeRight = CONTENT_RIGHT - otherCopiesWidth - 10f
        val badgeLeft = badgeRight - badgeWidth
        canvas.drawRoundRect(badgeLeft, y, badgeLeft + badgeWidth, y + 12f, 6f, 6f, fill(InvoicePalette.navy))
        canvas.drawText(badgeText, badgeLeft + 7f, y + 9f, badgePaint)

        y += 12f
        canvas.drawLine(CONTENT_LEFT, y, CONTENT_RIGHT, y, stroke(InvoicePalette.line))
        y += 10f
    }

    // ---------- Header band: business identity ----------

    private fun drawHeaderBand() {
        val seller = document.identity.seller
        canvas.drawRect(CONTENT_LEFT, y, CONTENT_RIGHT, y + HEADER_BAND_HEIGHT, fill(InvoicePalette.washStrong))
        canvas.drawRect(CONTENT_LEFT, y + HEADER_BAND_HEIGHT - 2f, CONTENT_RIGHT, y + HEADER_BAND_HEIGHT, fill(InvoicePalette.navy))

        var lineY = y + 24f
        canvas.drawText(seller.name, CONTENT_LEFT + 14f, lineY, paint(InvoicePalette.ink, 20f, bold = true))

        lineY += 8f
        canvas.drawRect(CONTENT_LEFT + 14f, lineY, CONTENT_LEFT + 14f + 40f, lineY + 2f, fill(InvoicePalette.brassDeep))

        seller.tagline?.let {
            lineY += 12f
            canvas.drawText(it, CONTENT_LEFT + 14f, lineY, paint(InvoicePalette.muted, 8.5f))
        }

        val addressLine = listOfNotNull(seller.addressLine1, seller.addressLine2, seller.city)
            .joinToString(", ")
        lineY += 12f
        canvas.drawText(addressLine, CONTENT_LEFT + 14f, lineY, paint(InvoicePalette.muted, 8f))

        val contactLine = listOfNotNull(
            seller.msmeOrUamNumber?.let { "MSME/UAM: $it" },
            seller.email,
            seller.phone
        ).joinToString("   ·   ")
        if (contactLine.isNotEmpty()) {
            lineY += 10f
            canvas.drawText(contactLine, CONTENT_LEFT + 14f, lineY, paint(InvoicePalette.mutedTwo, 7f, mono = true))
        }

        y += HEADER_BAND_HEIGHT + 10f
    }

    // ---------- Invoice Details / Transportation Mode two-column grid ----------

    private fun drawDetailsGrid() {
        val colWidth = CONTENT_WIDTH / 2f
        val leftX = CONTENT_LEFT
        val rightX = CONTENT_LEFT + colWidth
        val top = y

        drawBar("Invoice Details", leftX, top, colWidth)
        drawBar("Transportation Mode", rightX, top, colWidth)

        val invoiceRows = listOf(
            "Invoice Number" to document.identity.documentNumber,
            "Invoice Date" to document.identity.issueDate.format(printDateFormatter),
            "Reverse Charge" to if (document.identity.reverseChargeApplicable) "Yes" else "No",
            "Place of Supply" to "${document.identity.placeOfSupplyState} (${document.identity.placeOfSupplyStateCode})"
        )
        val logistics = document.logistics
        val transportRows = listOf(
            "Transporter" to (logistics?.transporterName?.takeIf { it.isNotBlank() } ?: "—"),
            "Vehicle Number" to (logistics?.vehicleNumber?.takeIf { it.isNotBlank() } ?: "—"),
            "Supply Date" to (logistics?.supplyDate?.format(printDateFormatter) ?: "—"),
            "E-Way Bill No." to (logistics?.ewayBillNumber?.takeIf { it.isNotBlank() } ?: "—")
        )

        var rowY = top + BAR_HEIGHT
        for (i in invoiceRows.indices) {
            drawKeyValueRow(invoiceRows[i].first, invoiceRows[i].second, leftX, rowY, colWidth)
            drawKeyValueRow(transportRows[i].first, transportRows[i].second, rightX, rowY, colWidth)
            rowY += DETAIL_ROW_HEIGHT
        }

        val bottom = rowY
        canvas.drawLine(leftX, top, leftX, bottom, stroke(InvoicePalette.line))
        canvas.drawLine(rightX, top, rightX, bottom, stroke(InvoicePalette.line))
        canvas.drawLine(CONTENT_RIGHT, top, CONTENT_RIGHT, bottom, stroke(InvoicePalette.line))
        canvas.drawLine(leftX, bottom, CONTENT_RIGHT, bottom, stroke(InvoicePalette.line))

        y = bottom + 10f
    }

    // ---------- Billed To / Shipped To ----------

    private fun drawParties() {
        val billedTo = document.parties.billedTo
        val shippedTo = document.parties.shippedTo
        val sameAddress = billedTo == shippedTo

        if (sameAddress) {
            val top = y
            drawBar("Billed To", CONTENT_LEFT, top, CONTENT_WIDTH)
            var partyY = top + BAR_HEIGHT + 3f
            partyY = drawPartyBlock(billedTo, CONTENT_LEFT + 14f, partyY, threeColumn = true)
            partyY += 9f
            canvas.drawText(
                "Shipped to same address",
                CONTENT_LEFT + 14f,
                partyY,
                paint(InvoicePalette.mutedTwo, 7f, italic = true)
            )
            val bottom = partyY + 6f
            canvas.drawRect(CONTENT_LEFT, top, CONTENT_RIGHT, bottom, outline())
            y = bottom + 10f
        } else {
            val colWidth = CONTENT_WIDTH / 2f
            val leftX = CONTENT_LEFT
            val rightX = CONTENT_LEFT + colWidth
            val top = y
            drawBar("Billed To", leftX, top, colWidth)
            drawBar("Shipped To", rightX, top, colWidth)

            val billedBottom = drawPartyBlock(billedTo, leftX + 14f, top + BAR_HEIGHT + 3f, threeColumn = false)
            val shippedBottom = drawPartyBlock(shippedTo, rightX + 14f, top + BAR_HEIGHT + 3f, threeColumn = false)
            val bottom = maxOf(billedBottom, shippedBottom) + 6f

            canvas.drawLine(leftX, top, leftX, bottom, stroke(InvoicePalette.line))
            canvas.drawLine(rightX, top, rightX, bottom, stroke(InvoicePalette.line))
            canvas.drawLine(CONTENT_RIGHT, top, CONTENT_RIGHT, bottom, stroke(InvoicePalette.line))
            canvas.drawLine(leftX, bottom, CONTENT_RIGHT, bottom, stroke(InvoicePalette.line))

            y = bottom + 10f
        }
    }

    /** Draws a party's name/address/GSTIN/state; returns the y position after the last line drawn. */
    private fun drawPartyBlock(party: DocumentParty, x: Float, startY: Float, threeColumn: Boolean): Float {
        var lineY = startY + 6f
        canvas.drawText(party.name, x, lineY, paint(InvoicePalette.ink, 10.5f, bold = true))
        lineY += 11f
        canvas.drawText(party.addressLines.joinToString(", "), x, lineY, paint(InvoicePalette.muted, 8.5f))
        lineY += 11f

        if (threeColumn) {
            canvas.drawText("GSTIN  ", x, lineY, paint(InvoicePalette.muted, 8.5f))
            val gstinLabelWidth = paint(InvoicePalette.muted, 8.5f).measureText("GSTIN  ")
            canvas.drawText(party.gstin, x + gstinLabelWidth, lineY, paint(InvoicePalette.ink, 8.5f, bold = true, mono = true))
            val stateX = x + 170f
            canvas.drawText("State  ", stateX, lineY, paint(InvoicePalette.muted, 8.5f))
            val stateLabelWidth = paint(InvoicePalette.muted, 8.5f).measureText("State  ")
            canvas.drawText(
                "${party.state} · ${party.stateCode}",
                stateX + stateLabelWidth,
                lineY,
                paint(InvoicePalette.ink, 8.5f, bold = true)
            )
        } else {
            canvas.drawText("GSTIN  ${party.gstin}", x, lineY, paint(InvoicePalette.ink, 8f, mono = true))
            lineY += 10f
            canvas.drawText("State  ${party.state} · ${party.stateCode}", x, lineY, paint(InvoicePalette.ink, 8f))
        }

        return lineY
    }

    // ---------- Line items table (paginates) ----------

    private val colStarts: FloatArray = floatArrayOf(0f, 24f, 248f, 300f, 336f, 376f, 446f)
    private val colWidths: FloatArray = floatArrayOf(24f, 224f, 52f, 36f, 40f, 70f, 85f)

    private fun drawLineItemsTable() {
        ensureSpace(TABLE_HEADER_HEIGHT + TABLE_ROW_HEIGHT)
        // tableTop tracks where THIS page's table (header + rows so far) began, so the closing
        // side borders can be drawn from the right place regardless of how many page breaks the
        // table has gone through - top alone isn't enough once a page break has moved it.
        var tableTop = y
        drawTableHeader(tableTop)
        var top = tableTop + TABLE_HEADER_HEIGHT

        document.lineItems.forEachIndexed { index, item ->
            if (top + TABLE_ROW_HEIGHT > BOTTOM_LIMIT) {
                closeTableBorders(tableTop, top)
                startNewPage()
                tableTop = y
                drawTableHeader(tableTop)
                top = tableTop + TABLE_HEADER_HEIGHT
            }

            if (index % 2 == 1) {
                canvas.drawRect(CONTENT_LEFT, top, CONTENT_RIGHT, top + TABLE_ROW_HEIGHT, fill(InvoicePalette.washSoft))
            }

            val baseline = top + TABLE_ROW_HEIGHT - 4f
            fun cellX(col: Int) = CONTENT_LEFT + colStarts[col]
            fun cellRight(col: Int) = cellX(col) + colWidths[col]

            canvas.drawText((index + 1).toString(), cellX(0) + 4f, baseline, paint(InvoicePalette.ink, 8.5f))

            val descPaint = textPaint(InvoicePalette.ink, 8.5f)
            val ellipsized = TextUtils.ellipsize(item.description, descPaint, colWidths[1] - 6f, TextUtils.TruncateAt.END)
            canvas.drawText(ellipsized.toString(), cellX(1), baseline, descPaint)

            canvas.drawText(item.hsnCode, cellX(2), baseline, paint(InvoicePalette.muted, 8f, mono = true))
            canvas.drawText(item.quantity?.toString() ?: "—", cellRight(3) - 4f, baseline, paint(InvoicePalette.ink, 8.5f, mono = true, align = Paint.Align.RIGHT))
            canvas.drawText(item.unit, cellX(4), baseline, paint(InvoicePalette.muted, 8.5f))
            canvas.drawText(
                Money.ofPaise(item.ratePaise).formatPlain(alwaysTwoDecimals = true),
                cellRight(5) - 4f,
                baseline,
                paint(InvoicePalette.ink, 8.5f, mono = true, align = Paint.Align.RIGHT)
            )
            canvas.drawText(
                Money.ofPaise(item.amountPaise).formatPlain(alwaysTwoDecimals = true),
                cellRight(6) - 4f,
                baseline,
                paint(InvoicePalette.ink, 8.5f, bold = true, mono = true, align = Paint.Align.RIGHT)
            )

            top += TABLE_ROW_HEIGHT
        }

        closeTableBorders(tableTop, top)
        y = top + 10f
    }

    /** Draws the bottom border and left/right side borders closing off one page's table section. */
    private fun closeTableBorders(tableTop: Float, tableBottom: Float) {
        canvas.drawLine(CONTENT_LEFT, tableBottom, CONTENT_RIGHT, tableBottom, stroke(InvoicePalette.line))
        canvas.drawLine(CONTENT_LEFT, tableTop, CONTENT_LEFT, tableBottom, stroke(InvoicePalette.line))
        canvas.drawLine(CONTENT_RIGHT, tableTop, CONTENT_RIGHT, tableBottom, stroke(InvoicePalette.line))
    }

    private fun drawTableHeader(top: Float) {
        canvas.drawRect(CONTENT_LEFT, top, CONTENT_RIGHT, top + TABLE_HEADER_HEIGHT, fill(InvoicePalette.navy))
        val baseline = top + TABLE_HEADER_HEIGHT - 4f
        val headerPaint = paint(InvoicePalette.white, 7.5f, bold = true)
        val labels = listOf("#", "Description of Goods", "HSN", "Qty", "Unit", "Rate", "Amount")
        val rightAligned = setOf(3, 5, 6)
        for (col in labels.indices) {
            val x = CONTENT_LEFT + colStarts[col]
            if (col in rightAligned) {
                canvas.drawText(labels[col].uppercase(), x + colWidths[col] - 4f, baseline, headerPaint.alignedRight())
            } else {
                canvas.drawText(labels[col].uppercase(), x + 4f, baseline, headerPaint)
            }
        }
    }

    // ---------- Totals + amount in words ----------

    private fun drawTotalsAndWords() {
        val totalsWidth = 190f
        val gap = 14f
        val wordsWidth = CONTENT_WIDTH - totalsWidth - gap
        val top = y

        // Amount in words + reverse-charge note (left column)
        canvas.drawRect(CONTENT_LEFT, top, CONTENT_LEFT + wordsWidth, top + 46f, fill(InvoicePalette.washSoft))
        canvas.drawRect(CONTENT_LEFT, top, CONTENT_LEFT + wordsWidth, top + 46f, outline())
        canvas.drawText(
            "AMOUNT IN WORDS",
            CONTENT_LEFT + 10f,
            top + 13f,
            paint(InvoicePalette.navy, 7f, bold = true)
        )
        val wordsPaint = textPaint(InvoicePalette.ink, 9f, bold = true)
        val wordsText = TextUtils.ellipsize(
            AmountInWords.forPaise(document.totals.grandTotalPaise),
            wordsPaint,
            wordsWidth - 20f,
            TextUtils.TruncateAt.END
        )
        canvas.drawText(wordsText.toString(), CONTENT_LEFT + 10f, top + 26f, wordsPaint)
        canvas.drawText(
            "GST Payable on Reverse Charge: ${if (document.identity.reverseChargeApplicable) "Yes" else "No"}",
            CONTENT_LEFT,
            top + 58f,
            paint(InvoicePalette.muted, 7.5f)
        )

        // Totals box (right column)
        val boxLeft = CONTENT_LEFT + wordsWidth + gap
        val boxRight = boxLeft + totalsWidth
        val taxation = document.taxation
        val rows = mutableListOf(
            "Sub Total" to Money.ofPaise(document.totals.itemsSubtotalPaise).formatPlain(alwaysTwoDecimals = true),
            "Add Freight" to if (document.totals.freightPaise > 0) {
                Money.ofPaise(document.totals.freightPaise).formatPlain(alwaysTwoDecimals = true)
            } else "—"
        )
        val cgst = taxation?.cgst
        val sgst = taxation?.sgst
        val igst = taxation?.igst
        rows += taxRow("CGST", cgst?.ratePercent, cgst?.amountPaise)
        rows += taxRow("SGST", sgst?.ratePercent, sgst?.amountPaise)
        rows += taxRow("IGST", igst?.ratePercent, igst?.amountPaise)

        var rowY = top
        for ((label, value) in rows) {
            drawKeyValueRow(label, value, boxLeft, rowY, totalsWidth, mono = true)
            rowY += DETAIL_ROW_HEIGHT
        }
        canvas.drawLine(boxLeft, top, boxLeft, rowY, stroke(InvoicePalette.line))
        canvas.drawLine(boxRight, top, boxRight, rowY, stroke(InvoicePalette.line))

        canvas.drawRect(boxLeft, rowY, boxRight, rowY + GRAND_TOTAL_BAR_HEIGHT, fill(InvoicePalette.navy))
        canvas.drawText(
            "TOTAL AMOUNT AFTER TAX",
            boxLeft + 10f,
            rowY + GRAND_TOTAL_BAR_HEIGHT / 2f + 3f,
            paint(InvoicePalette.white, 7f, bold = true)
        )
        canvas.drawText(
            Money.ofPaise(document.totals.grandTotalPaise).format(),
            boxRight - 10f,
            rowY + GRAND_TOTAL_BAR_HEIGHT / 2f + 5f,
            paint(InvoicePalette.brass, 15f, bold = true, mono = true, align = Paint.Align.RIGHT)
        )

        // Left column (amount in words + reverse-charge note) is a fixed 46f + 12f-offset note;
        // right column (totals box) is always taller in practice - take whichever is bigger.
        y = maxOf(rowY + GRAND_TOTAL_BAR_HEIGHT, top + 67f) + 12f
    }

    // ---------- Bank details (omitted entirely if the seller has none on file) ----------

    private fun drawBankDetails() {
        val seller = document.identity.seller
        val bankName = seller.bankName ?: return

        ensureSpace(BAR_HEIGHT + 26f)
        val top = y
        drawBar("Bank Details", CONTENT_LEFT, top, CONTENT_WIDTH)
        canvas.drawRect(CONTENT_LEFT, top + BAR_HEIGHT, CONTENT_RIGHT, top + BAR_HEIGHT + 26f, fill(InvoicePalette.washSoft))

        canvas.drawText("Bank  ", CONTENT_LEFT + 14f, top + BAR_HEIGHT + 12f, paint(InvoicePalette.muted, 8f))
        val bankLabelWidth = paint(InvoicePalette.muted, 8f).measureText("Bank  ")
        canvas.drawText(bankName, CONTENT_LEFT + 14f + bankLabelWidth, top + BAR_HEIGHT + 12f, paint(InvoicePalette.ink, 8f, bold = true))

        val accountText = listOfNotNull(seller.name, seller.bankAccountNumber).joinToString(" · ")
        canvas.drawText("A/C Name & No.  ", CONTENT_LEFT + 14f, top + BAR_HEIGHT + 22f, paint(InvoicePalette.muted, 8f))
        val acLabelWidth = paint(InvoicePalette.muted, 8f).measureText("A/C Name & No.  ")
        canvas.drawText(accountText, CONTENT_LEFT + 14f + acLabelWidth, top + BAR_HEIGHT + 22f, paint(InvoicePalette.ink, 8f, bold = true, mono = true))

        seller.bankIfsc?.let {
            canvas.drawText("IFSC  $it", CONTENT_LEFT + 320f, top + BAR_HEIGHT + 22f, paint(InvoicePalette.ink, 8f, bold = true, mono = true))
        }

        y = top + BAR_HEIGHT + 26f + 10f
    }

    // ---------- Certification + signature ----------

    private fun drawSignatureBlock() {
        ensureSpace(48f)
        val top = y
        val certification = document.footer.declarationText
        val certPaint = paint(InvoicePalette.muted, 8f)
        val certLines = wrapText(certification, certPaint, 260f)
        var certY = top + 8f
        for (line in certLines) {
            canvas.drawText(line, CONTENT_LEFT, certY, certPaint)
            certY += 10f
        }

        val sigRight = CONTENT_RIGHT
        canvas.drawText(
            "For ${document.identity.seller.name}",
            sigRight,
            top + 8f,
            paint(InvoicePalette.ink, 8.5f, bold = true, align = Paint.Align.RIGHT)
        )
        val sigLineY = top + 40f
        canvas.drawLine(sigRight - 150f, sigLineY, sigRight, sigLineY, stroke(InvoicePalette.line))
        canvas.drawText(
            "Authorised Signatory / Proprietor",
            sigRight,
            sigLineY + 9f,
            paint(InvoicePalette.muted, 7f, align = Paint.Align.RIGHT)
        )

        y = maxOf(certY, sigLineY + 9f) + 14f
    }

    // ---------- Footer / terms (omitted entirely if the seller has none on file) ----------

    private fun drawFooterTerms() {
        val terms = document.identity.seller.termsAndConditions
        if (terms.isNullOrEmpty()) return

        val termsPaint = paint(InvoicePalette.muted, 7.5f)
        val estimatedHeight = 16f + terms.sumOf { wrapText(it, termsPaint, CONTENT_WIDTH).size } * 9f
        ensureSpace(estimatedHeight)

        canvas.drawLine(CONTENT_LEFT, y, CONTENT_RIGHT, y, stroke(InvoicePalette.line))
        y += 12f
        canvas.drawText("TERMS & CONDITIONS", CONTENT_LEFT, y, paint(InvoicePalette.navy, 7f, bold = true))
        y += 10f
        for (term in terms) {
            for (line in wrapText(term, termsPaint, CONTENT_WIDTH)) {
                canvas.drawText(line, CONTENT_LEFT, y, termsPaint)
                y += 9f
            }
        }
    }

    /** A tax row label only carries a "%" suffix when that tax component actually applies. */
    private fun taxRow(label: String, ratePercent: Long?, amountPaise: Long?): Pair<String, String> =
        if (ratePercent != null && amountPaise != null) {
            "$label  $ratePercent%" to Money.ofPaise(amountPaise).formatPlain(alwaysTwoDecimals = true)
        } else {
            label to "—"
        }

    // ---------- Shared drawing helpers ----------

    private fun drawBar(label: String, left: Float, top: Float, width: Float) {
        canvas.drawRect(left, top, left + width, top + BAR_HEIGHT, fill(InvoicePalette.navy))
        canvas.drawText(label.uppercase(), left + 8f, top + BAR_HEIGHT - 4.5f, paint(InvoicePalette.white, 7.5f, bold = true))
    }

    private fun drawKeyValueRow(label: String, value: String, left: Float, top: Float, width: Float, mono: Boolean = false) {
        val baseline = top + DETAIL_ROW_HEIGHT - 4.5f
        canvas.drawText(label, left + 8f, baseline, paint(InvoicePalette.muted, 8f))
        canvas.drawText(value, left + width - 8f, baseline, paint(InvoicePalette.ink, 8f, bold = true, mono = mono, align = Paint.Align.RIGHT))
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "${current} $word"
            if (paint.measureText(candidate) > maxWidth && current.isNotEmpty()) {
                lines += current.toString()
                current = StringBuilder(word)
            } else {
                current = StringBuilder(candidate)
            }
        }
        if (current.isNotEmpty()) lines += current.toString()
        return lines
    }

    private fun paint(
        color: Int,
        size: Float,
        bold: Boolean = false,
        mono: Boolean = false,
        italic: Boolean = false,
        align: Paint.Align = Paint.Align.LEFT
    ): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        textAlign = align
        val family = if (mono) Typeface.MONOSPACE else Typeface.SANS_SERIF
        val style = when {
            bold && italic -> Typeface.BOLD_ITALIC
            bold -> Typeface.BOLD
            italic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
        typeface = Typeface.create(family, style)
    }

    private fun Paint.alignedRight(): Paint = Paint(this).apply { textAlign = Paint.Align.RIGHT }

    /** TextUtils.ellipsize requires a TextPaint specifically, not just any Paint. */
    private fun textPaint(color: Int, size: Float, bold: Boolean = false): TextPaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }

    private fun fill(color: Int): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }

    private fun stroke(color: Int, width: Float = 0.75f): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = width
    }

    private fun outline(): Paint = stroke(InvoicePalette.line)
}
