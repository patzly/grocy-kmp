package com.patrickzedler.grocy.core.network

import com.patrickzedler.grocy.core.model.network.ServerCertificate

internal actual val sendApiKeyAsQueryParameter: Boolean = true

actual val isHomeAssistantIngressSupported: Boolean = false

actual val isProxySupported: Boolean = false

actual val areCertificatesSupported: Boolean = false

internal actual fun Throwable.untrustedCertificate(): ServerCertificate? = null
