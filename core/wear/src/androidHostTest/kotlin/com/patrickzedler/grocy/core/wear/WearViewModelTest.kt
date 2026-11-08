package com.patrickzedler.grocy.core.wear

import com.patrickzedler.grocy.core.data.auth.AuthRepository
import com.patrickzedler.grocy.core.data.credentials.ServerConnectionCodec
import com.patrickzedler.grocy.core.data.settings.ShoppingListOptionsRepository
import com.patrickzedler.grocy.core.data.settings.ShoppingModeSettingsRepository
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListGrouping
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListItemSync
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListStatus
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingModeSettings
import com.patrickzedler.grocy.core.testing.FakeCredentialStore
import com.patrickzedler.grocy.core.testing.FakeSettingsStore
import com.patrickzedler.grocy.core.testing.RepositoryTestEnvironment
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class WearViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val environment = RepositoryTestEnvironment(dispatcher)
    private val store = FakeCredentialStore()
    private val settings = FakeSettingsStore()
    private val timeSource = TestTimeSource()
    private val auth = AuthRepository(store, environment.server.api)
    private val phone = object : PhoneLink {
        var published: String? = null
        var requests = 0

        override suspend fun publishedConnection() = published

        override suspend fun requestConnection() {
            requests++
        }
    }
    private val marker = object : PhoneLoginMarker {
        var fromPhone = false

        override suspend fun isFromPhone() = fromPhone

        override suspend fun setFromPhone(fromPhone: Boolean) {
            this.fromPhone = fromPhone
        }
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        environment.server.respond(
            "/api/objects/shopping_list",
            body = """[{"id":5,"shopping_list_id":1,"product_id":1,"amount":"2","done":0}]""",
        )
        environment.server.respond(
            "/api/objects/shopping_lists",
            body = """[{"id":1,"name":"Shop"}]""",
        )
    }

    @AfterTest
    fun tearDown() {
        environment.close()
        Dispatchers.resetMain()
    }

    private fun TestScope.shoppingList(): WearShoppingList {
        val scope = backgroundScope + UnconfinedTestDispatcher(testScheduler)
        return WearShoppingList(
            shopping = environment.shoppingListRepository,
            masterData = environment.repository,
            options = ShoppingListOptionsRepository(settings, scope),
            modeSettings = ShoppingModeSettingsRepository(settings, scope),
            language = "en",
            decimalSeparator = '.',
            timeSource = timeSource,
        )
    }

    private fun TestScope.viewModel(): WearViewModel = WearViewModel(
        auth = auth,
        shopping = shoppingList(),
        sync = WearConnectionSync(store, auth, marker),
        phone = phone,
    ).also { advanceUntilIdle() }

    @Test
    fun loggedOutItAsksThePhone() = runTest(dispatcher) {
        val viewModel = viewModel()

        assertEquals(WearState.LoggedOut, viewModel.state.value)
        assertEquals(1, phone.requests)
    }

    @Test
    fun whatThePhonePublishedLogsInWithoutAnyTap() = runTest(dispatcher) {
        val login = ServerConnection.ApiKey("https://grocy.test", "key")
        phone.published = ServerConnectionCodec.encode(login)

        val viewModel = viewModel()

        assertEquals(login, store.connection.first())
        assertIs<WearState.LoggedIn>(viewModel.state.value)
    }

    @Test
    fun aLoginDownloadsTheListAndOnlyWhatNamesItsItems() = runTest(dispatcher) {
        store.save(ServerConnection.ApiKey("https://grocy.test", "key"))

        val viewModel = viewModel()

        val shopping = assertIs<WearState.LoggedIn>(viewModel.state.value).shopping
        val items = shopping.groups.flatMap { it.items }
        assertTrue(items.isNotEmpty())
        assertTrue(items.all { it.title.isNotEmpty() })
        val paths = environment.server.requestedPaths
        assertTrue("/api/objects/products" in paths)
        assertTrue("/api/objects/product_groups" in paths)
        assertTrue("/api/stock" !in paths)
        assertTrue("/api/objects/chores" !in paths)
    }

    @Test
    fun aCheckOffWorksOfflineAndShowsItIsNotSent() = runTest(dispatcher) {
        store.save(ServerConnection.ApiKey("https://grocy.test", "key"))
        val viewModel = viewModel()
        val item = assertIs<WearState.LoggedIn>(viewModel.state.value).shopping.groups
            .flatMap { it.items }
            .first()
        environment.server.respond(
            "/api/objects/shopping_list/${item.id}",
            status = HttpStatusCode.ServiceUnavailable,
            method = HttpMethod.Put,
        )

        viewModel.setDone(item.id, !item.done)
        advanceUntilIdle()

        val updated = assertIs<WearState.LoggedIn>(viewModel.state.value).shopping.groups
            .flatMap { it.items }
            .first { it.id == item.id }
        assertEquals(!item.done, updated.done)
        assertEquals(ShoppingListItemSync.FAILED, updated.sync)
    }

    @Test
    fun aWrongAddressIsSaidWithoutAsking() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.setServerUrl("not a url")
        viewModel.setApiKey("key")

        viewModel.connect()
        advanceUntilIdle()

        assertEquals(WearLoginError.INVALID_ADDRESS, viewModel.login.value.error)
        assertNull(store.connection.first())
    }

    @Test
    fun aRejectedLoginStaysOnTheFormWithItsError() = runTest(dispatcher) {
        environment.server.respond("/api/user", status = HttpStatusCode.Unauthorized)
        val viewModel = viewModel()
        viewModel.setServerUrl("grocy.test")
        viewModel.setApiKey("wrong")

        viewModel.connect()
        advanceUntilIdle()

        assertEquals(WearLoginError.FAILED, viewModel.login.value.error)
        assertEquals("wrong", viewModel.login.value.apiKey)
        assertNull(store.connection.first())
    }

    @Test
    fun aVerifiedLoginIsKeptAndIsNotFromThePhone() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.setServerUrl("grocy.test")
        viewModel.setApiKey("key")

        viewModel.connect()
        advanceUntilIdle()

        assertEquals(ServerConnection.ApiKey("https://grocy.test", "key"), store.connection.first())
        assertEquals(false, marker.fromPhone)
        assertEquals("", viewModel.login.value.apiKey)
    }

    @Test
    fun autoSyncIsOnByDefaultAndDownloadsWhatOthersChanged() = runTest(dispatcher) {
        store.save(ServerConnection.ApiKey("https://grocy.test", "key"))
        val viewModel = viewModel()
        val sync = backgroundScope.launch { viewModel.syncWhileShown() }
        environment.server.respond(
            "/api/objects/shopping_list",
            body = """[{"id":5,"shopping_list_id":1,"product_id":1,"amount":"2","done":1}]""",
        )

        advanceTimeBy(DEFAULT_SYNC_INTERVAL + 1.seconds)
        advanceUntilIdleExceptSync()

        val items = assertIs<WearState.LoggedIn>(viewModel.state.value).shopping.groups
            .flatMap { it.items }
        assertTrue(items.single { it.id == 5 }.done)
        sync.cancel()
    }

    @Test
    fun withoutAutoSyncNothingIsDownloadedByItself() = runTest(dispatcher) {
        store.save(ServerConnection.ApiKey("https://grocy.test", "key"))
        val viewModel = viewModel()
        viewModel.setAutoSync(false)
        advanceUntilIdle()
        val requests = itemRequests()
        val sync = backgroundScope.launch { viewModel.syncWhileShown() }

        advanceTimeBy(5.minutes)
        advanceUntilIdleExceptSync()

        assertEquals(requests, itemRequests())
        sync.cancel()
    }

    @Test
    fun loggedOutTheSyncSendsNothing() = runTest(dispatcher) {
        val viewModel = viewModel()
        val requests = itemRequests()
        val sync = backgroundScope.launch { viewModel.syncWhileShown() }

        advanceTimeBy(DEFAULT_SYNC_INTERVAL + 1.seconds)
        advanceUntilIdleExceptSync()

        assertEquals(requests, itemRequests())
        sync.cancel()
    }

    @Test
    fun theSyncDownloadsNamesOnlyForItemsThatNeedThem() = runTest(dispatcher) {
        store.save(ServerConnection.ApiKey("https://grocy.test", "key"))
        val viewModel = viewModel()
        val productDownloads = productRequests()
        val sync = backgroundScope.launch { viewModel.syncWhileShown() }

        advanceTimeBy(DEFAULT_SYNC_INTERVAL + 1.seconds)
        advanceUntilIdleExceptSync()
        assertEquals(productDownloads, productRequests())

        environment.server.respond(
            "/api/objects/shopping_list",
            body = """[{"id":6,"shopping_list_id":1,"product_id":999,"amount":"1","done":0}]""",
        )
        advanceTimeBy(DEFAULT_SYNC_INTERVAL + 1.seconds)
        advanceUntilIdleExceptSync()

        assertEquals(productDownloads + 1, productRequests())
        sync.cancel()
    }

    @Test
    fun theTileDownloadsAtMostOnceAMinute() = runTest(dispatcher) {
        store.save(ServerConnection.ApiKey("https://grocy.test", "key"))
        val list = shoppingList()
        list.refreshForTile()
        advanceUntilIdle()
        val requests = itemRequests()

        timeSource += 30.seconds
        list.refreshForTile()
        advanceUntilIdle()
        assertEquals(requests, itemRequests())

        timeSource += 31.seconds
        list.refreshForTile()
        advanceUntilIdle()
        assertEquals(requests + 1, itemRequests())
    }

    @Test
    fun theTileGoesOnWithTheStoredListOffline() = runTest(dispatcher) {
        store.save(ServerConnection.ApiKey("https://grocy.test", "key"))
        environment.server.respond(
            "/api/objects/shopping_list",
            status = HttpStatusCode.ServiceUnavailable,
        )

        shoppingList().refreshForTile()
        advanceUntilIdle()
    }

    @Test
    fun optionsAreKeptInTheStore() = runTest(dispatcher) {
        store.save(ServerConnection.ApiKey("https://grocy.test", "key"))
        val viewModel = viewModel()

        viewModel.setGrouping(ShoppingListGrouping.NONE)
        viewModel.setDoneItemsLast(false)
        viewModel.setSyncInterval(30)
        advanceUntilIdle()

        assertEquals(ShoppingListGrouping.NONE, settings.shoppingListOptions.first().grouping)
        val mode = settings.shoppingModeSettings.first()
        assertEquals(false, mode.doneItemsLast)
        assertEquals(30, mode.syncIntervalSeconds)
        val state = assertIs<WearState.LoggedIn>(viewModel.state.value).shopping
        assertEquals(ShoppingListGrouping.NONE, state.grouping)
        assertEquals(30, state.settings.syncIntervalSeconds)
    }

    @Test
    fun theStatusFilterIsForTheAppNotTheTile() = runTest(dispatcher) {
        store.save(ServerConnection.ApiKey("https://grocy.test", "key"))
        val list = shoppingList()
        list.refresh()
        advanceUntilIdle()

        list.setStatus(ShoppingListStatus.DONE)
        advanceUntilIdle()

        assertTrue(list.state.first().groups.isEmpty())
        assertEquals(1, list.tileSummary.first().left)
    }

    private fun itemRequests() =
        environment.server.requestedPaths.count { it == "/api/objects/shopping_list" }

    private fun productRequests() =
        environment.server.requestedPaths.count { it == "/api/objects/products" }

    /** The sync never ends, so only what is due right now runs. */
    private fun TestScope.advanceUntilIdleExceptSync() = runCurrent()

    private companion object {
        val DEFAULT_SYNC_INTERVAL = ShoppingModeSettings.DEFAULT_SYNC_INTERVAL_SECONDS.seconds
    }
}
