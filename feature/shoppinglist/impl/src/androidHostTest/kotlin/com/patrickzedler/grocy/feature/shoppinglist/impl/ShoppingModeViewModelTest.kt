package com.patrickzedler.grocy.feature.shoppinglist.impl

import com.patrickzedler.grocy.core.data.settings.ShoppingListOptionsRepository
import com.patrickzedler.grocy.core.data.settings.ShoppingModeSettingsRepository
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListGrouping
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListOptions
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListStatus
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingModeSettings
import com.patrickzedler.grocy.core.testing.FakeSettingsStore
import com.patrickzedler.grocy.core.testing.RepositoryTestEnvironment
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
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

class ShoppingModeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val environment = RepositoryTestEnvironment(dispatcher)
    private val settingsStore = FakeSettingsStore(
        shoppingListOptions = ShoppingListOptions(grouping = ShoppingListGrouping.NONE),
        shoppingModeSettings = ShoppingModeSettings(syncIntervalSeconds = 10),
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        environment.close()
        Dispatchers.resetMain()
    }

    private lateinit var options: ShoppingListOptionsRepository

    private fun TestScope.viewModel(): ShoppingModeViewModel {
        // Unconfined, so options and settings are read right away like at app start
        val appScope = backgroundScope + UnconfinedTestDispatcher(testScheduler)
        options = ShoppingListOptionsRepository(settingsStore, appScope)
        return ShoppingModeViewModel(
            masterData = environment.repository,
            stock = environment.stockRepository,
            shoppingLists = environment.shoppingListRepository,
            optionsRepository = options,
            settingsRepository = ShoppingModeSettingsRepository(settingsStore, appScope),
            computeDispatcher = dispatcher,
            decimalSeparator = '.',
            language = "en",
        ).also {
            backgroundScope.launch { it.state.collect {} }
            advanceUntilIdle()
        }
    }

    private val ShoppingModeViewModel.rows
        get() = state.value?.groups?.flatMap { it.rows }.orEmpty()

    private fun itemDownloads() =
        environment.server.requestsTo("/api/objects/shopping_list", HttpMethod.Get).size

    @Test
    fun doneItemsComeLastInTheirOwnGroupWithTheSetting() = runTest(dispatcher) {
        val viewModel = viewModel()

        // Batteries are done, Milk is not
        val groups = viewModel.state.value!!.groups
        assertEquals(listOf(null, ShoppingListGroupTitle.Done), groups.map { it.title })
        assertEquals(listOf("Batteries"), groups.last().rows.map { it.title })
    }

    @Test
    fun doneItemsKeepTheirPlaceWithoutTheSetting() = runTest(dispatcher) {
        settingsStore.shoppingModeSettings.value = ShoppingModeSettings(doneItemsLast = false)
        val viewModel = viewModel()

        assertEquals(listOf("Batteries", "Milk"), viewModel.rows.map { it.title })
        assertEquals(1, viewModel.state.value!!.groups.size)
    }

    @Test
    fun aTapChecksTheItemOffAndSendsIt() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.toggleDone(viewModel.rows.first { it.id == 5 })
        advanceUntilIdle()

        val sent = environment.server.requestsTo("/api/objects/shopping_list/5", HttpMethod.Put)
        assertEquals(listOf("""{"done":1}"""), sent.map { (it.body as TextContent).text })
    }

    @Test
    fun autoSyncDownloadsTheItemsEveryIntervalWhileShown() = runTest(dispatcher) {
        val viewModel = viewModel()
        val before = itemDownloads()

        val sync = backgroundScope.launch { viewModel.syncWhileShown() }
        runCurrent()
        advanceTimeBy(10.seconds + 1.milliseconds)
        assertEquals(before + 1, itemDownloads())
        advanceTimeBy(10.seconds)
        assertEquals(before + 2, itemDownloads())
        sync.cancel()
        advanceTimeBy(30.seconds)

        assertEquals(before + 2, itemDownloads())
    }

    @Test
    fun withoutAutoSyncNothingIsDownloaded() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.setAutoSync(false)
        advanceUntilIdle()
        val before = itemDownloads()

        backgroundScope.launch { viewModel.syncWhileShown() }
        advanceTimeBy(60.seconds)

        assertEquals(before, itemDownloads())
        assertFalse(settingsStore.shoppingModeSettings.value.autoSync)
    }

    @Test
    fun autoSyncStaysQuietWhileOffline() = runTest(dispatcher) {
        val viewModel = viewModel()
        environment.server.unreachable = true

        backgroundScope.launch { viewModel.syncWhileShown() }
        advanceTimeBy(25.seconds)

        assertEquals(null, viewModel.refresh.value.error)
        assertEquals(2, viewModel.rows.size)
    }

    @Test
    fun statusFilterIsTheShoppingListOne() = runTest(dispatcher) {
        val viewModel = viewModel()
        options.setStatus(ShoppingListStatus.UNDONE)
        advanceUntilIdle()

        assertEquals(listOf(5), viewModel.rows.map { it.id })
        viewModel.clearFilter()
        advanceUntilIdle()
        assertEquals(2, viewModel.rows.size)
    }

    @Test
    fun anotherListIsShownWhenChosen() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.selectList(2)
        advanceUntilIdle()

        assertEquals("Hardware store", viewModel.state.value?.list?.name)
    }
}
