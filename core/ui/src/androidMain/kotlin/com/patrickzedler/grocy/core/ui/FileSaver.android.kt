package com.patrickzedler.grocy.core.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
actual fun rememberFileSaver(
    mimeType: String,
    onSaved: (success: Boolean) -> Unit,
): (SavedFile) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnSaved by rememberUpdatedState(onSaved)
    // The file waits here while the system asks where to save it
    var pending by remember { mutableStateOf<SavedFile?>(null) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(mimeType),
    ) { uri ->
        val file = pending
        pending = null
        if (uri != null && file != null) {
            scope.launch {
                currentOnSaved(withContext(Dispatchers.IO) { write(context, uri, file.bytes) })
            }
        }
    }
    return remember(launcher) {
        { file ->
            pending = file
            launcher.launch(file.name)
        }
    }
}

private fun write(context: Context, uri: Uri, bytes: ByteArray): Boolean = try {
    context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } != null
} catch (_: IOException) {
    false
} catch (_: SecurityException) {
    false
}
