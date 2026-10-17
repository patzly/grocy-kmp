package com.patrickzedler.grocy.core.ui

import androidx.compose.runtime.Composable

/** A picture the user chose, e.g. for a product. */
class PickedImage(
    val bytes: ByteArray,
    val fileName: String,
)

/**
 * Remembers a function that lets the user choose one picture: the system photo picker on
 * Android (no permission needed), a file chooser in browsers. [onPicked] is not called when the
 * user cancels.
 */
@Composable
expect fun rememberImagePicker(onPicked: (PickedImage) -> Unit): () -> Unit
