package com.patrickzedler.grocy.core.ui

import androidx.compose.runtime.Composable

/** A file the user chose, e.g. an instruction manual. */
class PickedFile(
    val bytes: ByteArray,
    val fileName: String,
)

/**
 * Remembers a function that lets the user choose one file of [mimeType]: the system's document
 * picker on Android (no permission needed), a file chooser in browsers. [onPicked] is not called
 * when the user cancels.
 */
@Composable
expect fun rememberFilePicker(mimeType: String, onPicked: (PickedFile) -> Unit): () -> Unit
