package com.patrickzedler.grocy.core.network

import com.patrickzedler.grocy.core.model.network.ServerCertificate

/**
 * Whether the API key goes into the URL instead of the GROCY-API-KEY header.
 *
 * Grocy checks the key before its CORS middleware runs, so the preflight a browser sends for the
 * custom header fails with a 401 without CORS headers. With the key as query parameter (supported
 * by Grocy), GET requests need no preflight and preflights of other requests carry the key.
 */
internal expect val sendApiKeyAsQueryParameter: Boolean

/**
 * Whether Home Assistant ingress can be used. Browsers cannot: ingress needs a session cookie,
 * which a page from another origin can neither set nor send, and ingress allows no CORS.
 */
expect val isHomeAssistantIngressSupported: Boolean

/**
 * Whether the app can choose a proxy or Tor for its requests. Browsers cannot: fetch() always uses
 * the browser's proxy settings.
 */
expect val isProxySupported: Boolean

/**
 * Whether the app can present client certificates and trust certificates itself. Browsers cannot:
 * they ask for client certificates and decide about server certificates on their own.
 */
expect val areCertificatesSupported: Boolean

/** The certificate a TLS failure was about, if the user could decide to trust it. */
internal expect fun Throwable.untrustedCertificate(): ServerCertificate?
