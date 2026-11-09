package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.model.network.ServerCertificate
import com.patrickzedler.grocy.core.model.network.TrustedCertificate
import com.patrickzedler.grocy.core.testing.FakeSettingsStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class CertificateRepositoryTest {

    private val first = ServerCertificate("aa", "CN=a", "CN=a", 1)
    private val renewed = ServerCertificate("bb", "CN=a", "CN=a", 2)

    private fun TestScope.repository() = CertificateRepository(
        FakeSettingsStore(),
        backgroundScope + UnconfinedTestDispatcher(testScheduler),
    )

    @Test
    fun aRenewedCertificateReplacesTheFormerOfItsServer() = runTest {
        val repository = repository()
        repository.trust("grocy.local", first)

        repository.trust("grocy.local", renewed)

        assertEquals(listOf(renewed), repository.settings.value!!.trusted.map { it.certificate })
    }

    @Test
    fun aTrustedCertificateCountsOnlyForItsHost() = runTest {
        val repository = repository()

        repository.trust("grocy.local", first)

        assertTrue(repository.settings.value!!.isTrusted("GROCY.local", "aa"))
        assertFalse(repository.settings.value!!.isTrusted("other.local", "aa"))
    }

    @Test
    fun aClientCertificateCanBeChosenAndRemoved() = runTest {
        val repository = repository()

        repository.setClientAlias("phone")
        assertEquals("phone", repository.settings.value!!.clientAlias)

        repository.setClientAlias(null)
        assertNull(repository.settings.value!!.clientAlias)
    }

    @Test
    fun theGateStoresWhatTheUserTrusts() = runTest(UnconfinedTestDispatcher()) {
        val repository = repository()
        val gate = CertificateTrustGate(repository)
        val answer = async { gate.ask("grocy.local", first) }
        assertEquals(CertificateQuestion("grocy.local", first), gate.question.value)

        gate.onAnswered(true)

        assertTrue(answer.await())
        assertNull(gate.question.value)
        assertTrue(repository.settings.value!!.isTrusted("grocy.local", "aa"))
    }

    @Test
    fun theGateStoresNothingOnANo() = runTest(UnconfinedTestDispatcher()) {
        val repository = repository()
        val gate = CertificateTrustGate(repository)
        val answer = async { gate.ask("grocy.local", first) }

        gate.onAnswered(false)

        assertFalse(answer.await())
        assertEquals(emptyList(), repository.settings.value!!.trusted)
    }

    @Test
    fun whatThePhoneTrustsReplacesTheListButNotTheClientCertificate() = runTest {
        val repository = repository()
        repository.setClientAlias("key")
        repository.trust("old.local", first)

        repository.setTrusted(listOf(TrustedCertificate("grocy.local", renewed)))

        assertEquals(
            listOf(TrustedCertificate("grocy.local", renewed)),
            repository.settings.value!!.trusted,
        )
        assertEquals("key", repository.settings.value!!.clientAlias)
    }
}
