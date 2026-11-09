package com.patrickzedler.grocy.core.wear

import com.patrickzedler.grocy.core.data.auth.AuthRepository
import com.patrickzedler.grocy.core.data.credentials.ServerConnectionCodec
import com.patrickzedler.grocy.core.data.credentials.TrustedCertificatesCodec
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.network.ServerCertificate
import com.patrickzedler.grocy.core.model.network.TrustedCertificate
import com.patrickzedler.grocy.core.testing.FakeCredentialStore
import com.patrickzedler.grocy.core.testing.FakeGrocyServer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class WearConnectionSyncTest {

    private val store = FakeCredentialStore()
    private var cleared = 0
    private val auth = AuthRepository(store, FakeGrocyServer().api) { cleared++ }
    private val marker = object : PhoneLoginMarker {
        var fromPhone = false

        override suspend fun isFromPhone() = fromPhone

        override suspend fun setFromPhone(fromPhone: Boolean) {
            this.fromPhone = fromPhone
        }
    }
    private var trusted = emptyList<TrustedCertificate>()
    private val sync = WearConnectionSync(store, auth, marker) { trusted = it }
    private val phoneLogin = ServerConnection.ApiKey("https://grocy.example", "secret")

    @Test
    fun theWatchTakesThePhonesLogin() = runTest {
        sync.onPhoneConnection(ServerConnectionCodec.encode(phoneLogin))

        assertEquals(phoneLogin, store.connection.first())
        assertTrue(marker.fromPhone)
    }

    @Test
    fun theSameLoginAgainKeepsTheDataOfTheWatch() = runTest {
        sync.onPhoneConnection(ServerConnectionCodec.encode(phoneLogin))
        sync.onPhoneConnection(ServerConnectionCodec.encode(phoneLogin))

        assertEquals(1, cleared)
    }

    @Test
    fun anotherServerOnThePhoneReplacesTheDataOfTheWatch() = runTest {
        sync.onPhoneConnection(ServerConnectionCodec.encode(phoneLogin))
        val other = ServerConnection.Demo()

        sync.onPhoneConnection(ServerConnectionCodec.encode(other))

        assertEquals(other, store.connection.first())
        assertEquals(2, cleared)
    }

    @Test
    fun loggingOutOnThePhoneLogsOutAWatchThatGotItsLoginFromThere() = runTest {
        sync.onPhoneConnection(ServerConnectionCodec.encode(phoneLogin))

        sync.onPhoneConnection(null)

        assertNull(store.connection.first())
        assertFalse(marker.fromPhone)
    }

    @Test
    fun aLoginEnteredOnTheWatchSurvivesThePhoneLoggingOut() = runTest {
        sync.onManualLogin(phoneLogin)

        sync.onPhoneConnection(null)

        assertEquals(phoneLogin, store.connection.first())
    }

    @Test
    fun unreadableDataChangesNothing() = runTest {
        sync.onManualLogin(phoneLogin)

        sync.onPhoneConnection("""{"type":"from_a_future_version"}""")

        assertEquals(phoneLogin, store.connection.first())
        assertFalse(marker.fromPhone)
    }

    @Test
    fun anAddressThatIsNoServerUrlIsRefused() = runTest {
        val bad = ServerConnection.ApiKey("file:///etc/passwd", "secret")

        sync.onPhoneConnection(ServerConnectionCodec.encode(bad))

        assertNull(store.connection.first())
    }

    @Test
    fun theFormatTheFirstVersionPublishedStaysReadable() = runTest {
        sync.onPhoneConnection(
            """{"type":"api_key","serverUrl":"https://grocy.example","apiKey":"secret"}""",
        )

        assertEquals(phoneLogin, store.connection.first())
    }

    private val certificate = ServerCertificate("aa", "CN=grocy", "CN=grocy", 20000)
    private val ofThisServer = TrustedCertificate("grocy.example", certificate)
    private val ofAnotherServer = TrustedCertificate("other.example", certificate)

    @Test
    fun theCertificatesThePhoneTrustsForItsServerComeAlong() = runTest {
        val encoded = TrustedCertificatesCodec.encode(listOf(ofThisServer))

        sync.onPhoneConnection(ServerConnectionCodec.encode(phoneLogin), encoded)

        assertEquals(listOf(ofThisServer), trusted)
    }

    @Test
    fun theWatchTrustsNothingForOtherServersWhateverItIsSent() = runTest {
        val encoded = TrustedCertificatesCodec.encode(listOf(ofThisServer, ofAnotherServer))

        sync.onPhoneConnection(ServerConnectionCodec.encode(phoneLogin), encoded)

        assertEquals(listOf(ofThisServer), trusted)
    }

    @Test
    fun aRenewedCertificateFollowsWithTheSameLogin() = runTest {
        val login = ServerConnectionCodec.encode(phoneLogin)
        sync.onPhoneConnection(login, TrustedCertificatesCodec.encode(listOf(ofThisServer)))
        val clearedBefore = cleared
        val renewed = TrustedCertificate(
            "grocy.example",
            ServerCertificate("bb", "CN=grocy", "CN=grocy", 30000),
        )

        sync.onPhoneConnection(login, TrustedCertificatesCodec.encode(listOf(renewed)))

        assertEquals(listOf(renewed), trusted)
        assertEquals(clearedBefore, cleared)
    }

    @Test
    fun loggingOutOnThePhoneForgetsItsCertificates() = runTest {
        sync.onPhoneConnection(
            ServerConnectionCodec.encode(phoneLogin),
            TrustedCertificatesCodec.encode(listOf(ofThisServer)),
        )

        sync.onPhoneConnection(null)

        assertTrue(trusted.isEmpty())
    }

    @Test
    fun aLoginEnteredOnTheWatchKeepsItsCertificatesWhenThePhoneLogsOut() = runTest {
        sync.onManualLogin(phoneLogin)
        trusted = listOf(ofThisServer)

        sync.onPhoneConnection(null)

        assertEquals(listOf(ofThisServer), trusted)
    }
}
