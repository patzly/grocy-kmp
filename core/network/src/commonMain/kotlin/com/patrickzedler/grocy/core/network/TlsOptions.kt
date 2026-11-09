package com.patrickzedler.grocy.core.network

import com.patrickzedler.grocy.core.model.network.CertificateSettings

/** Where the private keys of client certificates come from, which the platform keeps. */
expect interface ClientKeySource

/**
 * What the app's TLS connections follow on platforms where it can decide, read at every
 * handshake. Without a client certificate and trusted certificates they behave like the
 * platform's, nothing asks the user or tells of missing certificates.
 *
 * @param settings the current settings, null until they are read.
 * @param awaitSettings suspends until they are, so no request at app start goes without.
 */
class TlsOptions(
    internal val clientKeys: ClientKeySource?,
    internal val settings: () -> CertificateSettings?,
    internal val awaitSettings: suspend () -> Unit,
) {
    internal var onChanged: () -> Unit = {}

    /** The settings changed: connections that exist were made with the former ones. */
    fun notifyChanged() = onChanged()
}
