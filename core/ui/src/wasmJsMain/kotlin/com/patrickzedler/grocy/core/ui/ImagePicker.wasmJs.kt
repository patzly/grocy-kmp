package com.patrickzedler.grocy.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlin.io.encoding.Base64

@Composable
actual fun rememberImagePicker(onPicked: (PickedImage) -> Unit): () -> Unit {
    val currentOnPicked by rememberUpdatedState(onPicked)
    return remember {
        {
            pickImageFile { name, base64 ->
                currentOnPicked(PickedImage(Base64.decode(base64), name))
            }
        }
    }
}

/** The file arrives as base64 text: one string crosses into Wasm faster than single bytes. */
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """(callback) => {
        const input = document.createElement('input');
        input.type = 'file';
        input.accept = 'image/*';
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
private external fun pickImageFile(callback: (name: String, base64: String) -> Unit)
