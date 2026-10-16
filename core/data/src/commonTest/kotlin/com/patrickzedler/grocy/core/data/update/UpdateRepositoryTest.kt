package com.patrickzedler.grocy.core.data.update

import com.patrickzedler.grocy.core.model.UpdateCheck
import com.patrickzedler.grocy.core.testing.FakeAppUpdater
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

class UpdateRepositoryTest {

    private val updater = FakeAppUpdater(checksAutomatically = true)
    private val repository = UpdateRepository(updater, checkInterval = 4.hours)

    private fun TestScope.startAutomaticChecks() {
        backgroundScope.launch { repository.runAutomaticChecks() }
        runCurrent()
    }

    @Test
    fun automaticCheckOffersAFoundUpdate() = runTest {
        updater.result = UPDATE

        startAutomaticChecks()

        assertEquals(UPDATE, repository.pendingUpdate.value)
    }

    @Test
    fun noAutomaticChecksWhereOnlyTheUserAsks() = runTest {
        updater.checksAutomatically = false
        updater.result = UPDATE

        startAutomaticChecks()

        assertEquals(0, updater.checks)
        assertNull(repository.pendingUpdate.value)
    }

    @Test
    fun checksAgainAfterTheInterval() = runTest {
        startAutomaticChecks()

        advanceTimeBy(4.hours)
        runCurrent()

        assertEquals(2, updater.checks)
    }

    @Test
    fun checksAgainWhenTheAppReturnsToTheForeground() = runTest {
        startAutomaticChecks()

        updater.foregroundEvents.emit(Unit)
        runCurrent()

        assertEquals(2, updater.checks)
    }

    @Test
    fun postponedUpdateIsNotOfferedAgain() = runTest {
        updater.result = UPDATE
        startAutomaticChecks()

        repository.postpone(UPDATE)
        updater.foregroundEvents.emit(Unit)
        runCurrent()

        assertNull(repository.pendingUpdate.value)
    }

    @Test
    fun newerUpdateIsOfferedAfterPostponing() = runTest {
        updater.result = UPDATE
        startAutomaticChecks()
        repository.postpone(UPDATE)

        updater.result = UpdateCheck.Available("1.3.0")
        updater.foregroundEvents.emit(Unit)
        runCurrent()

        assertEquals(UpdateCheck.Available("1.3.0"), repository.pendingUpdate.value)
    }

    @Test
    fun failedAutomaticCheckStaysSilent() = runTest {
        updater.result = UpdateCheck.Failed

        startAutomaticChecks()

        assertNull(repository.pendingUpdate.value)
    }

    @Test
    fun checkOnRequestReturnsTheResult() = runTest {
        updater.checksAutomatically = false
        updater.result = UPDATE

        assertEquals(UPDATE, repository.check())
    }

    @Test
    fun installClearsTheOfferAndInstalls() = runTest {
        updater.result = UPDATE
        startAutomaticChecks()

        repository.install()

        assertNull(repository.pendingUpdate.value)
        assertEquals(1, updater.installs)
    }

    private companion object {
        val UPDATE = UpdateCheck.Available("1.2.0")
    }
}
