package com.patrickzedler.grocy.core.ui

import androidx.compose.runtime.Composable

@Composable
internal expect fun rememberTextSharer(): (String) -> Unit
