package com.patrickzedler.grocy.core.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.pdf_page
import java.io.File
import java.io.IOException
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun PdfDocumentView(
    bytes: ByteArray,
    isCovered: Boolean,
    onFailed: () -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val currentOnFailed by rememberUpdatedState(onFailed)
    val document by produceState<PdfPages?>(null, bytes) {
        val opened = withContext(Dispatchers.IO) { PdfPages.open(context, bytes) }
        if (opened == null) currentOnFailed()
        value = opened
        awaitDispose { opened?.close() }
    }
    val pages = document ?: return
    var zoom by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(
        modifier = modifier
            // Two fingers zoom and move; one is left to the list, which scrolls the pages
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        if (event.changes.count { it.pressed } >= 2) {
                            zoom = (zoom * event.calculateZoom()).coerceIn(1f, MAX_ZOOM)
                            offset = if (zoom == 1f) {
                                Offset.Zero
                            } else {
                                offset + event.calculatePan()
                            }
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
    ) {
        val width = with(LocalDensity.current) { maxWidth.roundToPx() }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.graphicsLayer {
                scaleX = zoom
                scaleY = zoom
                translationX = offset.x
                translationY = offset.y
            },
        ) {
            items(pages.count) { index ->
                val page by produceState<ImageBitmap?>(null, pages, index, width) {
                    value = pages.render(index, width)
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(pages.aspectRatio(index))
                        .background(Color.White),
                ) {
                    page?.let {
                        Image(
                            bitmap = it,
                            contentDescription = stringResource(
                                Res.string.pdf_page,
                                index + 1,
                                pages.count,
                            ),
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

/** An open PDF; the renderer only takes one page at a time. */
private class PdfPages private constructor(
    private val descriptor: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
) {
    private val mutex = Mutex()
    private val ratios = List(renderer.pageCount) { index ->
        renderer.openPage(index).use { it.width.toFloat() / it.height.coerceAtLeast(1) }
    }

    val count: Int
        get() = ratios.size

    fun aspectRatio(index: Int): Float = ratios[index]

    /** Page [index] sharper than [width] pixels, so it stays readable when zoomed. */
    suspend fun render(index: Int, width: Int): ImageBitmap? = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                renderer.openPage(index).use { page ->
                    val pixels = min(width * RENDER_SCALE, MAX_RENDER_WIDTH).coerceAtLeast(1)
                    val height = (pixels / ratios[index]).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(pixels, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(AndroidColor.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap.asImageBitmap()
                }
            } catch (_: IllegalStateException) {
                // Closed while the page was on its way, the screen is gone
                null
            }
        }
    }

    fun close() {
        // Waits for a page on its way: the renderer crashes when it closes meanwhile
        mutex.tryLock()
        renderer.close()
        descriptor.close()
    }

    companion object {
        /** Null when it is no PDF this device can read. */
        fun open(context: Context, bytes: ByteArray): PdfPages? {
            // The renderer needs a seekable file; gone from the disk once opened
            val file = File.createTempFile("document", ".pdf", context.cacheDir)
            return try {
                file.writeBytes(bytes)
                val descriptor =
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                try {
                    PdfPages(descriptor, PdfRenderer(descriptor))
                } catch (e: IOException) {
                    descriptor.close()
                    throw e
                } catch (e: SecurityException) {
                    descriptor.close()
                    throw e
                }
            } catch (_: IOException) {
                null
            } catch (_: SecurityException) {
                null
            } finally {
                file.delete()
            }
        }
    }
}

private const val MAX_ZOOM = 4f
private const val RENDER_SCALE = 2
private const val MAX_RENDER_WIDTH = 2400
