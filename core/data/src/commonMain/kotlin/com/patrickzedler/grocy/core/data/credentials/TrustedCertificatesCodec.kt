package com.patrickzedler.grocy.core.data.credentials

import com.patrickzedler.grocy.core.model.network.TrustedCertificate
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** The servers' certificates a phone hands to its watch, as text. */
object TrustedCertificatesCodec {

    private val serializer = ListSerializer(TrustedCertificate.serializer())

    // A newer app on the phone may add fields
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(certificates: List<TrustedCertificate>): String =
        json.encodeToString(serializer, certificates)

    /** Nothing for what cannot be read: a certificate is only trusted if it is known exactly. */
    fun decode(encoded: String?): List<TrustedCertificate> = try {
        encoded?.let { json.decodeFromString(serializer, it) }.orEmpty()
    } catch (_: SerializationException) {
        emptyList()
    } catch (_: IllegalArgumentException) {
        emptyList()
    }
}
