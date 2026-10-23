package com.patrickzedler.grocy.core.ui

import androidx.compose.runtime.Composable

@Composable
internal expect fun rememberTextSharer(): (String) -> Unit

/** False where the platform has no share sheet, e.g. desktop browsers on Linux. */
internal expect fun isTextSharingAvailable(): Boolean
