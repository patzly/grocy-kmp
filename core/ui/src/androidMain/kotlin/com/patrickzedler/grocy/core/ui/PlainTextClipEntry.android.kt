package com.patrickzedler.grocy.core.ui

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.toClipEntry

actual fun plainTextClipEntry(text: String, label: String): ClipEntry =
    ClipData.newPlainText(label, text).toClipEntry()
