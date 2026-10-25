package com.patrickzedler.grocy.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlin.io.encoding.Base64

@Composable
actual fun rememberFileSaver(
    mimeType: String,
    onSaved: (success: Boolean) -> Unit,
): (SavedFile) -> Unit {
    val currentOnSaved by rememberUpdatedState(onSaved)
    return remember(mimeType) {
        { file ->
            downloadFile(file.name, mimeType, Base64.encode(file.bytes))
            currentOnSaved(true)
        }
    }
}

/** The file crosses as base64 text, one string is faster than single bytes. */
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """(name, type, base64) => {
        const binary = atob(base64);
        const bytes = new Uint8Array(binary.length);
        for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
        const url = URL.createObjectURL(new Blob([bytes], { type: type }));
        const link = document.createElement('a');
        link.href = url;
        link.download = name;
        document.body.appendChild(link);
        link.click();
        link.remove();
        setTimeout(() => URL.revokeObjectURL(url), 0);
    }""",
)
private external fun downloadFile(name: String, type: String, base64: String)
