package com.patrickzedler.grocy.feature.settings.impl.general

import com.patrickzedler.grocy.core.data.server.ServerState
import com.patrickzedler.grocy.core.data.settings.AppearanceRepository
import com.patrickzedler.grocy.core.model.AppColor
import com.patrickzedler.grocy.core.model.AppContrast
import com.patrickzedler.grocy.core.model.AppTheme
import com.patrickzedler.grocy.core.model.AppearanceSettings
import com.patrickzedler.grocy.core.model.DemoServer
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.demoServerFor
import com.patrickzedler.grocy.core.network.GrocyApiException
import com.patrickzedler.grocy.core.testing.FakeAppLanguageStore
import com.patrickzedler.grocy.core.testing.FakeSettingsStore
import com.patrickzedler.grocy.feature.settings.impl.DemoServerSwitcher
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class GeneralSettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val settingsStore = FakeSettingsStore(
        appearance = AppearanceSettings(hue = 100f),
    )
    private val languageStore = FakeAppLanguageStore()
    private val server = MutableStateFlow<ServerState?>(
        ServerState(ServerConnection.Demo(), grocyVersion = "4.7.1"),
    )
    private val switchedTo = mutableListOf<DemoServer>()
    private var demoServerAnswers = true
    private var systemLanguage: String? = "de-CH"

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Like at app start, the stored appearance is read before the screen exists. */
    private fun TestScope.viewModel() = GeneralSettingsViewModel(
        AppearanceRepository(
            settingsStore,
            backgroundScope + UnconfinedTestDispatcher(testScheduler),
        ),
        languageStore,
        server = server,
        demoServerSwitcher = DemoServerSwitcher {
            if (!demoServerAnswers) {
                throw GrocyApiException.Unreachable(IllegalStateException("Demo is down"))
            }
            switchedTo += it
        },
        systemLanguage = { systemLanguage },
    )

    @Test
    fun appearanceIsThereWithoutWaiting() = runTest {
        // No advance: a value that arrives later would let the controls jump
        assertEquals(AppearanceSettings(hue = 100f), viewModel().appearance.value)
    }

    @Test
    fun changesKeepTheOtherValues() = runTest {
        val viewModel = viewModel()

        viewModel.updateAppearance { it.copy(theme = AppTheme.DARK) }
        advanceUntilIdle()
        viewModel.updateAppearance { it.copy(contrast = AppContrast.HIGH) }
        advanceUntilIdle()

        assertEquals(
            AppearanceSettings(theme = AppTheme.DARK, hue = 100f, contrast = AppContrast.HIGH),
            settingsStore.appearance.value,
        )
    }

    @Test
    fun colorSourceIsStored() = runTest {
        val viewModel = viewModel()

        viewModel.updateAppearance { it.copy(color = AppColor.DYNAMIC) }
        advanceUntilIdle()

        assertEquals(AppColor.DYNAMIC, settingsStore.appearance.value.color)
    }

    @Test
    fun chosenLanguageGoesToTheLanguageStore() = runTest {
        server.value = ServerState(ownServer, grocyVersion = "4.7.1")
        val viewModel = viewModel()

        viewModel.selectLanguage("en")

        assertEquals("en", languageStore.language.value)
        assertEquals("en", viewModel.language.value)
    }

    @Test
    fun systemLanguageIsNull() = runTest {
        server.value = ServerState(ownServer, grocyVersion = "4.7.1")
        languageStore.setLanguage("en")
        val viewModel = viewModel()

        viewModel.selectLanguage(null)

        assertNull(viewModel.language.value)
    }

    @Test
    fun languageOnTheDemoOffersTheDemoInThatLanguageFirst() = runTest {
        val viewModel = viewModel()

        viewModel.selectLanguage("de")

        assertEquals(DemoServerOffer("de", demoServerFor("de")!!), viewModel.demoServerOffer)
        // The language would restart the screen and end the question
        assertEquals(0, languageStore.changes)
    }

    @Test
    fun switchingLogsInToTheDemoAndThenAppliesTheLanguage() = runTest {
        val viewModel = viewModel()
        viewModel.selectLanguage("de")

        viewModel.answerDemoServerOffer(switch = true)
        assertTrue(viewModel.isSwitchingDemoServer)
        advanceUntilIdle()

        assertEquals(listOf(demoServerFor("de")), switchedTo)
        assertEquals("de", languageStore.language.value)
        assertNull(viewModel.demoServerOffer)
        assertFalse(viewModel.isSwitchingDemoServer)
    }

    @Test
    fun keepingTheDemoOnlyAppliesTheLanguage() = runTest {
        val viewModel = viewModel()
        viewModel.selectLanguage("fr")

        viewModel.answerDemoServerOffer(switch = false)

        assertEquals("fr", languageStore.language.value)
        assertEquals(emptyList(), switchedTo)
        assertNull(viewModel.demoServerOffer)
    }

    @Test
    fun unreachableDemoChangesNothingAndShowsAnError() = runTest {
        demoServerAnswers = false
        val viewModel = viewModel()
        viewModel.selectLanguage("de")

        viewModel.answerDemoServerOffer(switch = true)
        advanceUntilIdle()

        assertNull(languageStore.language.value)
        assertTrue(viewModel.demoServerSwitchFailed)
        assertNull(viewModel.demoServerOffer)
        viewModel.onDemoServerSwitchFailureShown()
        assertFalse(viewModel.demoServerSwitchFailed)
    }

    @Test
    fun ownServerGetsNoOffer() = runTest {
        server.value = ServerState(ownServer, grocyVersion = "4.7.1")
        val viewModel = viewModel()

        viewModel.selectLanguage("de")

        assertNull(viewModel.demoServerOffer)
        assertEquals("de", languageStore.language.value)
    }

    @Test
    fun demoInTheChosenLanguageAlreadyGetsNoOffer() = runTest {
        server.value = ServerState(ServerConnection.Demo("https://de.demo.grocy.info"), null)
        val viewModel = viewModel()

        viewModel.selectLanguage("de")

        assertNull(viewModel.demoServerOffer)
        assertEquals("de", languageStore.language.value)
    }

    @Test
    fun systemLanguageOffersTheDemoOfTheDevicesLanguage() = runTest {
        languageStore.setLanguage("fr")
        val viewModel = viewModel()

        viewModel.selectLanguage(null)

        assertEquals(DemoServerOffer(null, demoServerFor("de")!!), viewModel.demoServerOffer)
    }

    @Test
    fun systemLanguageWithoutTranslationOffersTheEnglishDemoTheAppShows() = runTest {
        // Danish has a demo, but the app shows English without a Danish translation
        systemLanguage = "da-DK"
        server.value = ServerState(ServerConnection.Demo("https://de.demo.grocy.info"), null)
        languageStore.setLanguage("de")
        val viewModel = viewModel()

        viewModel.selectLanguage(null)

        assertEquals(ServerConnection.Demo.DEFAULT_URL, viewModel.demoServerOffer?.server?.url)
    }

    private companion object {
        val ownServer = ServerConnection.ApiKey("https://grocy.test", "k")
    }
}
