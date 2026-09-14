package com.verity.feature.invoice.pdf

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import com.verity.core.theme.VerityTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * PdfViewerScreen
 *
 * Renders a locally generated invoice PDF for the user to look at in-app, using the platform's
 * own android.graphics.pdf.PdfRenderer (stable since API 21) rather than a third-party viewer or
 * Google's still pre-1.0 androidx.pdf Compose library.
 *
 * [file] is null while the caller (AppNavShell's PDF_VIEWER route) is still generating/locating
 * the PDF - this screen shows a loading state until it resolves.
 *
 * Pinch-zoom/pan is one shared transform over all rendered pages rather than per-page state, and
 * there's no separate scroll container - content is scaled-to-fit by default and pan moves it
 * once zoomed in. Deliberately simple: combining a scrollable container with a custom
 * pinch-zoom gesture detector is a well-known source of gesture conflicts in Compose, and a GST
 * invoice is confirmed one page in the common case (design stress-tested up to 10 line items).
 */
@Composable
fun PdfViewerScreen(file: File?) {
    if (file == null) {
        LoadingIndicator()
        return
    }

    val pageBitmaps by produceState<List<Bitmap>?>(initialValue = null, file) {
        value = withContext(Dispatchers.IO) { renderPdfPages(file) }
    }

    val pages = pageBitmaps
    if (pages == null) {
        LoadingIndicator()
        return
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
        ) {
            pages.forEach { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Invoice PDF page",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = VerityTheme.colors.primary)
    }
}

/** Renders at 2x the PDF's own point size so pinch-zooming in stays reasonably crisp. */
private const val PAGE_RENDER_SCALE = 2

private fun renderPdfPages(file: File): List<Bitmap> =
    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
        PdfRenderer(descriptor).use { renderer ->
            (0 until renderer.pageCount).map { index ->
                renderer.openPage(index).use { page ->
                    val bitmap = Bitmap.createBitmap(
                        page.width * PAGE_RENDER_SCALE,
                        page.height * PAGE_RENDER_SCALE,
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                }
            }
        }
    }
