package com.patrickzedler.grocy.core.network

import com.patrickzedler.grocy.core.model.network.ServerCertificate

// Not shown in logs of reverse proxies, unlike query parameters
internal actual val sendApiKeyAsQueryParameter: Boolean = false

actual val isHomeAssistantIngressSupported: Boolean = true

actual val isProxySupported: Boolean = true

actual val areCertificatesSupported: Boolean = true

internal actual fun Throwable.untrustedCertificate(): ServerCertificate? =
    generateSequence(this) { it.cause }
        .filterIsInstance<UntrustedCertificateException>()
        .firstOrNull()
        ?.certificate
