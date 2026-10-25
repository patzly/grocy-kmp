package com.patrickzedler.grocy.feature.settings.impl.stock

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.Snapshot
import app.cash.turbine.test
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.database.createInMemoryGrocyDatabase
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.testing.FakeGrocyServer
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class StockSettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val server = FakeGrocyServer(requestDispatcher = dispatcher)
    private val database = createInMemoryGrocyDatabase(queryContext = dispatcher)
    private val repository = MasterDataRepository(
        api = server.api,
        database = database,
        connection = { ServerConnection.ApiKey("https://grocy.test", "k") },
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    private suspend fun TestScope.viewModel(): StockSettingsViewModel {
        repository.refresh()
        return StockSettingsViewModel(repository, decimalSeparator = '.').also {
            advanceUntilIdle()
        }
    }

    private fun saved(key: String) = server.requestsTo("/api/user/settings/$key", HttpMethod.Put)
        .map { (it.body as TextContent).text }

    @Test
    fun formShowsTheStoredSettings() = runTest(dispatcher) {
        val form = assertNotNull(viewModel().form)

        assertEquals(2, form.presets[StockPresetSetting.LOCATION])
        assertEquals(1, form.presets[StockPresetSetting.QUANTITY_UNIT])
        assertEquals(
            "5",
            form.numbers.getValue(StockNumberSetting.DUE_SOON_DAYS).text.toString(),
        )
    }

    @Test
    fun switchIsSavedAtOnce() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.setSwitch(StockSwitchSetting.SHOW_ALL_OUT_OF_STOCK, true)
        advanceUntilIdle()

        assertEquals(
            listOf("""{"value":true}"""),
            saved(StockSwitchSetting.SHOW_ALL_OUT_OF_STOCK.key),
        )
    }

    @Test
    fun removedPresetIsSavedAsMinusOne() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.setPreset(StockPresetSetting.LOCATION, null)
        advanceUntilIdle()

        assertEquals(listOf("""{"value":-1}"""), saved(StockPresetSetting.LOCATION.key))
    }

    @Test
    fun offlineChangeReturnsToTheStoredValue() = runTest(dispatcher) {
        val viewModel = viewModel()
        server.unreachable = true

        viewModel.events.test {
            viewModel.setSwitch(StockSwitchSetting.SHOW_PURCHASED_DATE, true)
            advanceUntilIdle()

            assertEquals(StockSettingsEvent.Offline, awaitItem())
        }
        assertFalse(viewModel.form!!.switches.getValue(StockSwitchSetting.SHOW_PURCHASED_DATE))
    }

    @Test
    fun typedNumberIsSavedOnceTypingPauses() = runTest(dispatcher) {
        val viewModel = viewModel()
        val field = viewModel.form!!.numbers.getValue(StockNumberSetting.DUE_SOON_DAYS)

        field.setTextAndPlaceCursorAtEnd("1")
        Snapshot.sendApplyNotifications()
        advanceTimeBy(100)
        field.setTextAndPlaceCursorAtEnd("10")
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()

        assertEquals(listOf("""{"value":10}"""), saved(StockNumberSetting.DUE_SOON_DAYS.key))
    }

    @Test
    fun invalidNumberIsShownAndNotSaved() = runTest(dispatcher) {
        val viewModel = viewModel()
        val field = viewModel.form!!.numbers.getValue(StockNumberSetting.DUE_SOON_DAYS)

        field.setTextAndPlaceCursorAtEnd("0")
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()

        assertTrue(StockNumberSetting.DUE_SOON_DAYS in viewModel.form!!.errors)
        assertTrue(saved(StockNumberSetting.DUE_SOON_DAYS.key).isEmpty())
    }
}
