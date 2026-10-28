package com.patrickzedler.grocy.feature.settings.impl.user

import app.cash.turbine.test
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.server.ServerState
import com.patrickzedler.grocy.core.database.createInMemoryGrocyDatabase
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.testing.FakeGrocyServer
import com.patrickzedler.grocy.feature.settings.impl.UserSettingEvent
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class UserSettingsViewModelTest {

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

    private suspend fun TestScope.viewModel(locale: String): UserSettingsViewModel {
        server.respond("/api/user/settings", body = """{"locale":"$locale"}""")
        repository.refresh()
        val server = MutableStateFlow<ServerState?>(null)
        return UserSettingsViewModel(repository, server).also { advanceUntilIdle() }
    }

    private fun savedLocales() = server.requestsTo("/api/user/settings/locale", HttpMethod.Put)
        .map { (it.body as TextContent).text }

    @Test
    fun showsTheStoredLanguage() = runTest(dispatcher) {
        assertEquals("de", viewModel("de").locale)
    }

    @Test
    fun chosenLanguageIsSavedByGrocysCode() = runTest(dispatcher) {
        val viewModel = viewModel("")

        viewModel.chooseLocale("pt_BR")
        advanceUntilIdle()

        assertEquals("pt_BR", viewModel.locale)
        assertEquals(listOf("""{"value":"pt_BR"}"""), savedLocales())
    }

    @Test
    fun defaultIsSavedAsEmptyLanguage() = runTest(dispatcher) {
        val viewModel = viewModel("de")

        viewModel.chooseLocale(null)
        advanceUntilIdle()

        assertEquals(listOf("""{"value":""}"""), savedLocales())
    }

    @Test
    fun failedSaveReturnsToTheStoredLanguage() = runTest(dispatcher) {
        val viewModel = viewModel("de")
        server.unreachable = true

        viewModel.events.test {
            viewModel.chooseLocale("fr")
            advanceUntilIdle()

            assertEquals(UserSettingEvent.Offline, awaitItem())
        }
        assertEquals("de", viewModel.locale)
    }
}
