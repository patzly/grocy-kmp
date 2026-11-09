package com.patrickzedler.grocy.feature.settings.impl.certificates

import androidx.compose.runtime.Composable

/** Opens the system's choice of client certificates; [onPicked] gets the chosen one's alias. */
@Composable
internal expect fun rememberClientCertificatePicker(
    currentAlias: String?,
    onPicked: (String) -> Unit,
): () -> Unit
