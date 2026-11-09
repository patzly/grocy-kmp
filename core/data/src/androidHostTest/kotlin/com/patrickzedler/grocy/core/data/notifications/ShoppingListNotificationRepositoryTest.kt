package com.patrickzedler.grocy.core.data.notifications

import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.notifications.NotificationKind
import com.patrickzedler.grocy.core.model.notifications.NotificationSummary
import com.patrickzedler.grocy.core.model.notifications.ShoppingListChangeNames
import com.patrickzedler.grocy.core.model.notifications.ShoppingListNotificationSettings
import com.patrickzedler.grocy.core.model.notifications.ShoppingListNotificationType
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.testing.FakeGrocyServer
import com.patrickzedler.grocy.core.testing.FakeSettingsStore
import com.patrickzedler.grocy.core.testing.MasterDataJson
import com.patrickzedler.grocy.core.testing.RepositoryTestEnvironment
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

class ShoppingListNotificationRepositoryTest {

    private class FakeNotifier : Notifier {
        val shown = mutableListOf<ShoppingListChangeNames>()
        var isShowing = false

        override suspend fun show(summary: NotificationSummary, silently: Boolean) = true

        override suspend fun cancel(kind: NotificationKind) = Unit

        override suspend fun isShowing(kind: NotificationKind) = false

        override suspend fun showShoppingListChanges(names: ShoppingListChangeNames): Boolean {
            shown += names
            isShowing = true
            return true
        }

        override suspend fun cancelShoppingListChanges() {
            isShowing = false
        }

        override suspend fun isShowingShoppingListChanges() = isShowing
    }

    private val dispatcher = StandardTestDispatcher()
    private val environment = RepositoryTestEnvironment(dispatcher)
    private val server = environment.server
    private val notifier = FakeNotifier()
    private val store = FakeSettingsStore()
    private val active = mutableListOf<Boolean>()
    private val repository = ShoppingListNotificationRepository(
        settingsStore = store,
        connection = MutableStateFlow(ServerConnection.ApiKey("https://grocy.test", "key")),
        serverConfig = MutableStateFlow(ServerConfig()),
        api = server.api,
        shopping = environment.shoppingListRepository,
        masterData = environment.repository,
        notifier = notifier,
        poller = object : ShoppingListPoller {
            override fun setActive(active: Boolean) {
                this@ShoppingListNotificationRepositoryTest.active += active
            }
        },
        scope = CoroutineScope(dispatcher),
    )

    @AfterTest
    fun tearDown() {
        environment.close()
    }

    private fun itemJson(id: Int, note: String, done: Int = 0) =
        """{"id":$id,"product_id":null,"shopping_list_id":1,"note":"$note","amount":"1",""" +
            """"qu_id":null,"done":$done,"userfields":null}"""

    /** What others left on the server: Grocy's own two items and [items]. */
    private fun serverHas(vararg items: String) {
        val all = (MasterDataJson.SHOPPING_LIST_ITEMS.removeSurrounding("[", "]").let(::listOf) + items)
        server.respond(ITEMS, body = all.joinToString(",", "[", "]"))
    }

    private fun serverChanged(time: String = "2026-11-04 11:00:00") {
        server.respond(
            FakeGrocyServer.DB_CHANGED_TIME_PATH,
            body = FakeGrocyServer.dbChangedTimeJson(time),
        )
    }

    private fun turnOn(vararg types: ShoppingListNotificationType) {
        store.shoppingListNotifications.value = ShoppingListNotificationSettings(types.toSet())
    }

    /** The first download sets what later ones are compared with. */
    private suspend fun TestScope.downloaded() {
        environment.repository.refresh()
        environment.shoppingListRepository.refresh()
        advanceUntilIdle()
    }

    private fun downloads() = server.requestedPaths.count { it == ITEMS }

    @Test
    fun theFirstDownloadTellsNothing() = runTest(dispatcher) {
        turnOn(ShoppingListNotificationType.ADDED)

        repository.check()

        assertTrue(notifier.shown.isEmpty())
    }

    @Test
    fun whatOthersAddedComesAsOneNotification() = runTest(dispatcher) {
        turnOn(ShoppingListNotificationType.ADDED)
        downloaded()
        serverHas(itemJson(10, "Bread"), itemJson(11, "Eggs"))
        serverChanged()

        repository.check()

        assertEquals(listOf(ShoppingListChangeNames(added = listOf("Bread", "Eggs"))), notifier.shown)
    }

    @Test
    fun whatOthersCheckedOffIsToldWhenWanted() = runTest(dispatcher) {
        turnOn(ShoppingListNotificationType.CHECKED_OFF)
        serverHas(itemJson(10, "Bread"))
        downloaded()
        serverHas(itemJson(10, "Bread", done = 1))
        serverChanged()

        repository.check()

        assertEquals(listOf(ShoppingListChangeNames(checkedOff = listOf("Bread"))), notifier.shown)
    }

    @Test
    fun onlyTheWantedKindNotifies() = runTest(dispatcher) {
        turnOn(ShoppingListNotificationType.CHECKED_OFF)
        downloaded()
        serverHas(itemJson(10, "Bread"))
        serverChanged()

        repository.check()

        assertTrue(notifier.shown.isEmpty())
    }

    @Test
    fun aNotificationStillShowingGrows() = runTest(dispatcher) {
        turnOn(ShoppingListNotificationType.ADDED)
        downloaded()
        serverHas(itemJson(10, "Bread"))
        serverChanged()
        repository.check()
        serverHas(itemJson(10, "Bread"), itemJson(11, "Eggs"))
        serverChanged("2026-11-04 12:00:00")

        repository.check()

        assertEquals(ShoppingListChangeNames(added = listOf("Bread", "Eggs")), notifier.shown.last())
    }

    @Test
    fun aDismissedNotificationStartsOver() = runTest(dispatcher) {
        turnOn(ShoppingListNotificationType.ADDED)
        downloaded()
        serverHas(itemJson(10, "Bread"))
        serverChanged()
        repository.check()
        notifier.isShowing = false
        serverHas(itemJson(10, "Bread"), itemJson(11, "Eggs"))
        serverChanged("2026-11-04 12:00:00")

        repository.check()

        assertEquals(ShoppingListChangeNames(added = listOf("Eggs")), notifier.shown.last())
    }

    @Test
    fun aCheckWithoutChangesOnTheServerDownloadsNothing() = runTest(dispatcher) {
        turnOn(ShoppingListNotificationType.ADDED)
        repository.check()
        val before = downloads()

        repository.check()

        assertEquals(before, downloads())
    }

    @Test
    fun turnedOffItAsksTheServerNothing() = runTest(dispatcher) {
        repository.check()

        assertTrue(server.requestedPaths.isEmpty())
    }

    @Test
    fun whatTheUserDidOnThisDeviceNotifiesNobody() = runTest(dispatcher) {
        turnOn(ShoppingListNotificationType.ADDED)
        downloaded()
        serverHas(itemJson(10, "Bread"))

        environment.shoppingListRepository.refreshAfterOwnChange()

        assertTrue(notifier.shown.isEmpty())
    }

    @Test
    fun aListOnTheScreenNeedsNoNotification() = runTest(dispatcher) {
        turnOn(ShoppingListNotificationType.ADDED)
        downloaded()
        environment.shoppingListRepository.setListOnScreen(true)
        serverHas(itemJson(10, "Bread"))
        serverChanged()

        repository.check()

        assertTrue(notifier.shown.isEmpty())
    }

    @Test
    fun checksRunOnlyWhileTheyAreWanted() = runTest(dispatcher) {
        repository.start()
        advanceUntilIdle()
        assertEquals(listOf(false), active)

        turnOn(ShoppingListNotificationType.ADDED)
        advanceUntilIdle()

        assertEquals(listOf(false, true), active)
    }

    @Test
    fun turningEverythingOffTakesTheNotificationAway() = runTest(dispatcher) {
        turnOn(ShoppingListNotificationType.ADDED)
        notifier.isShowing = true

        repository.setEnabled(ShoppingListNotificationType.ADDED, false)

        assertEquals(false, notifier.isShowing)
    }

    private companion object {
        const val ITEMS = "/api/objects/shopping_list"
    }
}
