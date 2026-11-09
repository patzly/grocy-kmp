package com.patrickzedler.grocy.core.model.network

import kotlinx.serialization.Serializable

/** What a server identifies itself with; [sha256] tells this certificate from every other. */
@Serializable
data class ServerCertificate(
    /** Lower case hexadecimal digits of the certificate's SHA-256. */
    val sha256: String,
    val subject: String,
    val issuer: String,
    /** The last day it is valid. */
    val notAfterEpochDay: Long,
) {
    /** [sha256] the way certificates are shown everywhere: AB:CD:... */
    val fingerprint: String
        get() = sha256.uppercase().chunked(2).joinToString(":")
}

/** A certificate no authority vouches for, trusted by the user for the server at [host]. */
@Serializable
data class TrustedCertificate(val host: String, val certificate: ServerCertificate)

/** How the app's TLS connections differ from the platform's, by default not at all. */
@Serializable
data class CertificateSettings(
    /** The certificate of Android's key store to identify with if a server asks for one. */
    val clientAlias: String? = null,
    val trusted: List<TrustedCertificate> = emptyList(),
) {
    fun isTrusted(sha256: String): Boolean = trusted.any { it.certificate.sha256 == sha256 }

    fun isTrusted(host: String, sha256: String): Boolean =
        trusted.any { it.host.equals(host, ignoreCase = true) && it.certificate.sha256 == sha256 }
}
