package com.patrickzedler.grocy.core.ui

import androidx.compose.runtime.Composable

private val ShareText: (String) -> Unit = { shareText(it) }

@Composable
internal actual fun rememberTextSharer(): (String) -> Unit = ShareText

internal actual fun isTextSharingAvailable(): Boolean = hasWebShare()

/** Web Share API; the user closing the share sheet is no error. */
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(text) => { navigator.share({ text: text }).catch(() => {}); }")
private external fun shareText(text: String)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => typeof navigator.share === 'function'")
private external fun hasWebShare(): Boolean
