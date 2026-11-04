package com.patrickzedler.grocy.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlin.io.encoding.Base64

@Composable
actual fun rememberFilePicker(mimeType: String, onPicked: (PickedFile) -> Unit): () -> Unit {
    val currentOnPicked by rememberUpdatedState(onPicked)
    return remember(mimeType) {
        {
            pickFile(mimeType) { name, base64 ->
                currentOnPicked(PickedFile(Base64.decode(base64), name))
            }
        }
    }
}

/** The file arrives as base64 text: one string crosses into Wasm faster than single bytes. */
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """(accept, callback) => {
        const input = document.createElement('input');
        input.type = 'file';
        input.accept = accept;
        input.onchange = () => {
            const file = input.files && input.files[0];
            if (!file) return;
            const reader = new FileReader();
            reader.onload = () => {
                const url = reader.result;
                callback(file.name, url.substring(url.indexOf(',') + 1));
            };
            reader.readAsDataURL(file);
        };
        input.click();
    }""",
)
private external fun pickFile(accept: String, callback: (name: String, base64: String) -> Unit)
