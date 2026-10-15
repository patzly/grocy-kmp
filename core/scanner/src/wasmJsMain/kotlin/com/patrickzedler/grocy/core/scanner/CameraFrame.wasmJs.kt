@file:OptIn(ExperimentalWasmJsInterop::class)

package com.patrickzedler.grocy.core.scanner

/** ImageData of one video frame, kept on the JavaScript side. */
actual class CameraFrame(val imageData: JsAny)
