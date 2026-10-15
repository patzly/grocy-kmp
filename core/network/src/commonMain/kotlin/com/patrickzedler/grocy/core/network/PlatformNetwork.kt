package com.patrickzedler.grocy.core.network

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
