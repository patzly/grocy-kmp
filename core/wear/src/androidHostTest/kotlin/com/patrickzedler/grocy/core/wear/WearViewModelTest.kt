package com.patrickzedler.grocy.core.wear

import com.patrickzedler.grocy.core.data.auth.AuthRepository
import com.patrickzedler.grocy.core.data.credentials.ServerConnectionCodec
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListItem
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListItemSync
import com.patrickzedler.grocy.core.testing.FakeCredentialStore
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class WearViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val environment = RepositoryTestEnvironment(dispatcher)
    private val store = FakeCredentialStore()
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

    private fun TestScope.viewModel(): WearViewModel = WearViewModel(
        auth = auth,
        shopping = environment.shoppingListRepository,
        masterData = environment.repository,
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
        assertTrue(shopping.items.isNotEmpty())
        assertTrue(shopping.items.all { it.title.isNotEmpty() })
        val paths = environment.server.requestedPaths
        assertTrue("/api/objects/products" in paths)
        assertTrue("/api/stock" !in paths)
        assertTrue("/api/objects/chores" !in paths)
    }

    @Test
    fun aCheckOffWorksOfflineAndShowsItIsNotSent() = runTest(dispatcher) {
        store.save(ServerConnection.ApiKey("https://grocy.test", "key"))
        val viewModel = viewModel()
        val item = assertIs<WearState.LoggedIn>(viewModel.state.value).shopping.items.first()
        environment.server.respond(
            "/api/objects/shopping_list/${item.id}",
            status = HttpStatusCode.ServiceUnavailable,
            method = HttpMethod.Put,
        )

        viewModel.setDone(item.id, !item.done)
        advanceUntilIdle()

        val updated = assertIs<WearState.LoggedIn>(viewModel.state.value).shopping.items
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
    fun rowsNameTheProductWithAmountUnitAndNoteAndKeepWhatIsLeftOnTop() {
        val items = listOf(
            ShoppingListItem(1, 1, 1, "ripe", amount = 2.0, quId = 1, done = true),
            ShoppingListItem(2, 1, null, "Stamps", amount = 0.0, quId = null, done = false),
            ShoppingListItem(3, 1, 2, null, amount = 1.0, quId = 1, done = false),
            ShoppingListItem(4, 2, 2, null, amount = 1.0, quId = 1, done = false),
        )
        val products = listOf(
            Product(id = 1, name = "Bananas"),
            Product(id = 2, name = "Apple"),
        )
        val units = listOf(QuantityUnit(id = 1, name = "Piece", namePlural = "Pieces"))

        val rows = wearShoppingItems(
            items = items,
            listId = 1,
            products = products,
            units = units,
            syncStates = mapOf(3 to ShoppingListItemSync.SENDING),
        )

        assertEquals(listOf("Apple", "Stamps", "Bananas"), rows.map { it.title })
        assertEquals("1 Piece", rows[0].details)
        assertEquals(ShoppingListItemSync.SENDING, rows[0].sync)
        assertNull(rows[1].details)
        assertEquals("2 Pieces · ripe", rows[2].details)
    }
}
