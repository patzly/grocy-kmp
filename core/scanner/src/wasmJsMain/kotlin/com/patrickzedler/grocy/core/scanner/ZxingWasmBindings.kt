@file:JsModule("zxing-wasm/reader")
@file:OptIn(ExperimentalWasmJsInterop::class)

package com.patrickzedler.grocy.core.scanner

import kotlin.js.Promise

internal external fun readBarcodes(
    input: JsAny,
    readerOptions: JsAny,
): Promise<JsArray<ZxingReadResult>>

internal external fun prepareZXingModule(options: JsAny): Promise<JsAny?>
