package com.patrickzedler.grocy.feature.settings.impl.certificates

import androidx.compose.runtime.Composable

// Browsers ask for client certificates themselves, the page does not exist there
@Composable
internal actual fun rememberClientCertificatePicker(
    currentAlias: String?,
    onPicked: (String) -> Unit,
): () -> Unit = {}
