package com.patrickzedler.grocy.core.data.calendar

import com.patrickzedler.grocy.core.testing.FakeGrocyServer
import com.patrickzedler.grocy.core.testing.RepositoryTestEnvironment
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest

class CalendarRepositoryTest {

    private val dispatcher = StandardTestDispatcher()
    private val environment = RepositoryTestEnvironment(dispatcher)
    private val server = environment.server
    private val repository = environment.calendarRepository

    @AfterTest
    fun tearDown() {
        environment.close()
    }

    private fun downloads() = server.requestedPaths.count { it == "/api/chores" }

    @Test
    fun aCheckWithoutChangesOnTheSameDayDownloadsNothingMore() = runTest(dispatcher) {
        assertTrue(repository.refreshIfChanged(today = 100))

        assertFalse(repository.refreshIfChanged(today = 100))

        assertEquals(1, downloads())
        assertEquals(2, server.requestedPaths.count { it == FakeGrocyServer.DB_CHANGED_TIME_PATH })
    }

    @Test
    fun aChangeOnTheServerDownloadsAgain() = runTest(dispatcher) {
        repository.refreshIfChanged(today = 100)
        server.respond(
            FakeGrocyServer.DB_CHANGED_TIME_PATH,
            body = FakeGrocyServer.dbChangedTimeJson("2026-11-04 11:00:00"),
        )

        repository.refreshIfChanged(today = 100)

        assertEquals(2, downloads())
    }

    @Test
    fun aNewDayDownloadsAgainBecauseTheServerComputesByDay() = runTest(dispatcher) {
        repository.refreshIfChanged(today = 100)

        repository.refreshIfChanged(today = 101)

        assertEquals(2, downloads())
    }
}
