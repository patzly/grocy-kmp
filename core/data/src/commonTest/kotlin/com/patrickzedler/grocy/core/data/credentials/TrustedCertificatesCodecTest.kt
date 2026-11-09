package com.patrickzedler.grocy.core.data.credentials

import com.patrickzedler.grocy.core.model.network.ServerCertificate
import com.patrickzedler.grocy.core.model.network.TrustedCertificate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrustedCertificatesCodecTest {

    private val trusted = TrustedCertificate(
        host = "grocy.local",
        certificate = ServerCertificate("aa", "CN=grocy", "CN=grocy", 20000),
    )

    @Test
    fun certificatesSurviveTheWayToTheWatch() {
        val encoded = TrustedCertificatesCodec.encode(listOf(trusted))

        assertEquals(listOf(trusted), TrustedCertificatesCodec.decode(encoded))
    }

    @Test
    fun theFormatTheFirstVersionPublishedStaysReadable() {
        val published = """[{"host":"grocy.local","certificate":{"sha256":"aa",""" +
            """"subject":"CN=grocy","issuer":"CN=grocy","notAfterEpochDay":20000}}]"""

        assertEquals(listOf(trusted), TrustedCertificatesCodec.decode(published))
    }

    @Test
    fun whatCannotBeReadIsTrustedByNobody() {
        assertTrue(TrustedCertificatesCodec.decode("not json").isEmpty())
        assertTrue(TrustedCertificatesCodec.decode(null).isEmpty())
    }
}
