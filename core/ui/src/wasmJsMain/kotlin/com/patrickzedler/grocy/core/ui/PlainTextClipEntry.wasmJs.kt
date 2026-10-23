package com.patrickzedler.grocy.core.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry

@OptIn(ExperimentalComposeUiApi::class)
actual fun plainTextClipEntry(text: String, label: String): ClipEntry =
    ClipEntry.withPlainText(text)
