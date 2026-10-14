package com.patrickzedler.grocy.core.ui

import androidx.compose.runtime.Composable

private val ShareText: (String) -> Unit = { shareText(it) }

@Composable
internal actual fun rememberTextSharer(): (String) -> Unit = ShareText

/** Web Share API where available, otherwise the clipboard. */
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """(text) => {
        if (navigator.share) {
            navigator.share({ text: text }).catch(() => {});
        } else if (navigator.clipboard) {
            navigator.clipboard.writeText(text).catch(() => {});
        }
    }""",
)
private external fun shareText(text: String)
