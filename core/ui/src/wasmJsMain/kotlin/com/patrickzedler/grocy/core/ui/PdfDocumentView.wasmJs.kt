@file:OptIn(ExperimentalWasmJsInterop::class)

package com.patrickzedler.grocy.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.HtmlElementView
import kotlin.io.encoding.Base64
import org.w3c.dom.HTMLElement

@Composable
actual fun PdfDocumentView(
    bytes: ByteArray,
    isCovered: Boolean,
    onFailed: () -> Unit,
    modifier: Modifier,
) {
    val currentOnFailed by rememberUpdatedState(onFailed)
    LaunchedEffect(Unit) {
        if (!hasPdfViewer()) currentOnFailed()
    }
    if (!hasPdfViewer()) return
    // Always of the type PDF: the blob shares the app's origin, so it must never run as a page
    val url = remember(bytes) { pdfUrlOf(Base64.encode(bytes)) }
    DisposableEffect(url) {
        onDispose { revokeUrl(url) }
    }
    HtmlElementView(
        factory = { createPdfFrame() },
        modifier = modifier,
        update = {
            setFrameUrl(it, url)
            setFrameHidden(it, isCovered)
        },
    )
}

@JsFun("() => navigator.pdfViewerEnabled !== false")
private external fun hasPdfViewer(): Boolean

@JsFun(
    """(base64) => {
        const binary = atob(base64);
        const bytes = new Uint8Array(binary.length);
        for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
        return URL.createObjectURL(new Blob([bytes], { type: 'application/pdf' }));
    }""",
)
private external fun pdfUrlOf(base64: String): String

@JsFun("(url) => URL.revokeObjectURL(url)")
private external fun revokeUrl(url: String)

@JsFun(
    """() => {
        const frame = document.createElement('iframe');
        frame.style.border = '0';
        frame.style.width = '100%';
        frame.style.height = '100%';
        // Compose's clips end at the canvas; the corners of its medium shape
        frame.style.borderRadius = '16px';
        return frame;
    }""",
)
private external fun createPdfFrame(): HTMLElement

@JsFun("(frame, url) => { if (frame.src !== url) frame.src = url; }")
private external fun setFrameUrl(frame: HTMLElement, url: String)

// Hidden, not removed: the viewer keeps its page and zoom
@JsFun("(frame, hidden) => { frame.style.visibility = hidden ? 'hidden' : 'visible'; }")
private external fun setFrameHidden(frame: HTMLElement, hidden: Boolean)
