package com.patrickzedler.grocy.core.ui

import androidx.compose.runtime.Composable

/** A file to save, e.g. a grocycode image. */
class SavedFile(
    val name: String,
    val bytes: ByteArray,
)

/**
 * Remembers a function that saves files of [mimeType] (e.g. "image/png") where the user
 * chooses: the system's "Save as" on Android (no permission needed), a download in browsers.
 * [onSaved] gets true once it is written, false when writing failed; nothing when the user
 * cancels.
 */
@Composable
expect fun rememberFileSaver(
    mimeType: String,
    onSaved: (success: Boolean) -> Unit = {},
): (SavedFile) -> Unit
