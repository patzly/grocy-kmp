package com.patrickzedler.grocy.feature.settings.impl.grocy

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.Snapshot
import app.cash.turbine.test
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.database.createInMemoryGrocyDatabase
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.testing.FakeGrocyServer
import com.patrickzedler.grocy.feature.settings.api.GrocySettingsPage
import com.patrickzedler.grocy.feature.settings.impl.UserSettingEvent
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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class GrocySettingsPageViewModelTest {

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

    private suspend fun TestScope.viewModel(page: GrocySettingsPage): GrocySettingsPageViewModel {
        server.respond(
            "/api/user/settings",
            body = """{"chores_due_soon_days":"3","shopping_list_round_up":"1",""" +
                """"shopping_list_auto_add_below_min_stock_amount_list_id":"2"}""",
        )
        repository.refresh()
        return GrocySettingsPageViewModel(repository, page).also { advanceUntilIdle() }
    }

    private fun saved(key: String) = server.requestsTo("/api/user/settings/$key", HttpMethod.Put)
        .map { (it.body as TextContent).text }

    @Test
    fun formShowsTheStoredSettings() = runTest(dispatcher) {
        val form = assertNotNull(viewModel(GrocySettingsPage.CHORES).form)

        assertEquals("3", form.dueSoonDays.getValue(DueSoonDaysSetting.CHORES).text.toString())
        assertTrue(form.switches.getValue(GrocySwitchSetting.ROUND_UP))
        assertEquals(2, form.autoAddListId)
    }

    @Test
    fun switchIsSavedAtOnce() = runTest(dispatcher) {
        val viewModel = viewModel(GrocySettingsPage.RECIPES)

        viewModel.setSwitch(GrocySwitchSetting.GROUP_INGREDIENTS, true)
        advanceUntilIdle()

        assertEquals(listOf("""{"value":true}"""), saved(GrocySwitchSetting.GROUP_INGREDIENTS.key))
    }

    @Test
    fun chosenListIsSavedByItsId() = runTest(dispatcher) {
        val viewModel = viewModel(GrocySettingsPage.SHOPPING_LIST)

        viewModel.setAutoAddList(1)
        advanceUntilIdle()

        assertEquals(listOf("""{"value":1}"""), saved(AUTO_ADD_LIST_KEY))
    }

    @Test
    fun zeroDueSoonDaysAreSaved() = runTest(dispatcher) {
        val viewModel = viewModel(GrocySettingsPage.CHORES)
        val field = viewModel.form!!.dueSoonDays.getValue(DueSoonDaysSetting.CHORES)

        field.setTextAndPlaceCursorAtEnd("0")
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()

        assertEquals(listOf("""{"value":0}"""), saved(DueSoonDaysSetting.CHORES.key))
    }

    @Test
    fun negativeDueSoonDaysAreShownAndNotSaved() = runTest(dispatcher) {
        val viewModel = viewModel(GrocySettingsPage.TASKS)
        val field = viewModel.form!!.dueSoonDays.getValue(DueSoonDaysSetting.TASKS)

        field.setTextAndPlaceCursorAtEnd("-1")
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()

        assertTrue(DueSoonDaysSetting.TASKS in viewModel.form!!.errors)
        assertTrue(saved(DueSoonDaysSetting.TASKS.key).isEmpty())
    }

    @Test
    fun offlineChangeReturnsToTheStoredValue() = runTest(dispatcher) {
        val viewModel = viewModel(GrocySettingsPage.SHOPPING_LIST)
        server.unreachable = true

        viewModel.events.test {
            viewModel.setSwitch(GrocySwitchSetting.SHOW_CALENDAR, true)
            advanceUntilIdle()

            assertEquals(UserSettingEvent.Offline, awaitItem())
        }
        assertFalse(viewModel.form!!.switches.getValue(GrocySwitchSetting.SHOW_CALENDAR))
    }
}
