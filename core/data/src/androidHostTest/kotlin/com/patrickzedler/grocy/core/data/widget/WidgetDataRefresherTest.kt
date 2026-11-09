package com.patrickzedler.grocy.core.data.widget

import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.testing.FakeGrocyServer
import com.patrickzedler.grocy.core.testing.RepositoryTestEnvironment
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest

class WidgetDataRefresherTest {

    private val dispatcher = StandardTestDispatcher()
    private val environment = RepositoryTestEnvironment(dispatcher)
    private val server = environment.server
    private val refresher = WidgetDataRefresher(
        api = server.api,
        connection = { ServerConnection.ApiKey("https://grocy.test", "key") },
        masterData = environment.repository,
        calendar = environment.calendarRepository,
        shoppingLists = environment.shoppingListRepository,
    )

    @AfterTest
    fun tearDown() {
        environment.close()
    }

    private fun calendarDownloads() = server.requestedPaths.count { it == "/api/chores" }

    private fun shoppingDownloads() =
        server.requestedPaths.count { it == "/api/objects/shopping_list" }

    private suspend fun refreshCalendar(today: Long = 100) =
        refresher.refreshIfChanged(today, calendarWidgets = true, shoppingListWidgets = false)

    private suspend fun refreshShoppingLists(today: Long = 100) =
        refresher.refreshIfChanged(today, calendarWidgets = false, shoppingListWidgets = true)

    private fun serverChanges() {
        server.respond(
            FakeGrocyServer.DB_CHANGED_TIME_PATH,
            body = FakeGrocyServer.dbChangedTimeJson("2026-11-04 11:00:00"),
        )
    }

    @Test
    fun aCheckWithoutChangesOnTheSameDayDownloadsNothingMore() = runTest(dispatcher) {
        assertTrue(refreshCalendar())

        assertFalse(refreshCalendar())

        assertEquals(1, calendarDownloads())
        assertEquals(2, server.requestedPaths.count { it == FakeGrocyServer.DB_CHANGED_TIME_PATH })
    }

    @Test
    fun aChangeOnTheServerDownloadsAgain() = runTest(dispatcher) {
        refreshCalendar()
        serverChanges()

        refreshCalendar()

        assertEquals(2, calendarDownloads())
    }

    @Test
    fun aNewDayDownloadsTheCalendarAgainBecauseTheServerComputesByDay() = runTest(dispatcher) {
        refreshCalendar(today = 100)

        refreshCalendar(today = 101)

        assertEquals(2, calendarDownloads())
    }

    @Test
    fun aNewDayAloneDownloadsNoShoppingList() = runTest(dispatcher) {
        refreshShoppingLists(today = 100)

        assertFalse(refreshShoppingLists(today = 101))

        assertEquals(1, shoppingDownloads())
    }

    @Test
    fun onlyTheDataOfPlacedWidgetsIsDownloaded() = runTest(dispatcher) {
        refreshShoppingLists()

        assertEquals(1, shoppingDownloads())
        assertEquals(0, calendarDownloads())
    }

    @Test
    fun theShoppingListFollowsChangesOnTheServerWithItsNames() = runTest(dispatcher) {
        refreshShoppingLists()
        serverChanges()

        assertTrue(refreshShoppingLists())

        assertEquals(2, shoppingDownloads())
        assertEquals(2, server.requestedPaths.count { it == "/api/objects/products" })
    }

    @Test
    fun bothKindsOfWidgetsCostOneCheck() = runTest(dispatcher) {
        refresher.refreshIfChanged(100, calendarWidgets = true, shoppingListWidgets = true)

        assertEquals(1, server.requestedPaths.count { it == FakeGrocyServer.DB_CHANGED_TIME_PATH })
    }
}
