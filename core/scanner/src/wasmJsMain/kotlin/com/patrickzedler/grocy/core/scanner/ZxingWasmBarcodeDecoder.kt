@file:OptIn(ExperimentalWasmJsInterop::class)

package com.patrickzedler.grocy.core.scanner

import kotlinx.coroutines.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal actual fun createZxingBarcodeDecoder(): BarcodeDecoder = ZxingWasmBarcodeDecoder()

internal external interface ZxingReadResult : JsAny {
    val isValid: Boolean
    val text: String
    val format: String
}

/** zxing-wasm, the ZXing C++ port, so browsers decode with the same library as Android. */
internal class ZxingWasmBarcodeDecoder : BarcodeDecoder {

    override suspend fun decode(frame: CameraFrame, formats: Set<BarcodeFormat>): List<Barcode> {
        ZxingWasmModule.prepare()
        val names = formats.map { ZxingWasmFormats.nameOf(it).toJsString() }.toJsArray()
        val results = readBarcodes(frame.imageData, readerOptions(names))
            .await<JsArray<ZxingReadResult>>()
        return buildList {
            for (index in 0 until results.length) {
                val result = results[index] ?: continue
                val format = ZxingWasmFormats.formatOf(result.format)
                if (result.isValid && format != null) add(Barcode(result.text, format))
            }
        }
    }
}

private object ZxingWasmModule {
    private val mutex = Mutex()
    private var isPrepared = false

    suspend fun prepare() = mutex.withLock {
        if (!isPrepared) {
            prepareZXingModule(moduleOptions()).await<JsAny?>()
            isPrepared = true
        }
    }
}

private fun readerOptions(formats: JsArray<JsString>): JsAny =
    js("({ formats: formats, maxNumberOfSymbols: 1 })")

// By default the decoder is downloaded from a CDN; the app ships it next to index.html instead
private fun moduleOptions(): JsAny = js(
    """({
        overrides: {
            locateFile: (path, prefix) =>
                path.endsWith('.wasm') ? new URL(path, document.baseURI).href : prefix + path
        },
        fireImmediately: true
    })""",
)
