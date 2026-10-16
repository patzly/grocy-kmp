package com.patrickzedler.grocy.feature.settings.impl.log

import androidx.compose.ui.platform.ClipEntry

/** Clipboard content of the platform for plain [text]. */
internal expect fun plainTextClipEntry(text: String): ClipEntry
