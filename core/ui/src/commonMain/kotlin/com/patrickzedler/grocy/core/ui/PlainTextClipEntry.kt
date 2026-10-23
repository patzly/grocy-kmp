package com.patrickzedler.grocy.core.ui

import androidx.compose.ui.platform.ClipEntry

/** Clipboard content of the platform for plain [text]; [label] describes it on Android. */
expect fun plainTextClipEntry(text: String, label: String): ClipEntry
