package com.patrickzedler.grocy.feature.settings.impl.general

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.language.AppLanguageStore
import com.patrickzedler.grocy.core.data.server.ServerState
import com.patrickzedler.grocy.core.data.settings.AppearanceRepository
import com.patrickzedler.grocy.core.model.AppLanguages
import com.patrickzedler.grocy.core.model.AppearanceSettings
import com.patrickzedler.grocy.core.model.DemoServer
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.demoServerFor
import com.patrickzedler.grocy.core.model.demoServerOf
import com.patrickzedler.grocy.core.model.resolveAppLanguage
import com.patrickzedler.grocy.core.network.GrocyApiException
import com.patrickzedler.grocy.feature.settings.impl.DemoServerSwitcher
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** The demo in the language just chosen, offered before the language is applied. */
internal data class DemoServerOffer(
    /** The chosen language, null for the system's. */
    val languageTag: String?,
    val server: DemoServer,
)

/**
 * @param server the logged in server, read at app start.
 * @param systemLanguage the device's language, which "System default" shows.
 */
internal class GeneralSettingsViewModel(
    private val appearanceRepository: AppearanceRepository,
    private val languageStore: AppLanguageStore,
    private val server: StateFlow<ServerState?>,
    private val demoServerSwitcher: DemoServerSwitcher,
    private val systemLanguage: () -> String? = ::systemLanguageTag,
) : ViewModel() {

    /** Read at app start, so the controls show the stored values in the first frame. */
    val appearance: StateFlow<AppearanceSettings?> = appearanceRepository.appearance

    val language: StateFlow<String?> = languageStore.language

    /** Asked after a language was chosen on a demo, until answered. */
    var demoServerOffer by mutableStateOf<DemoServerOffer?>(null)
        private set

    /** True while the offered demo is being switched to. */
    var isSwitchingDemoServer by mutableStateOf(false)
        private set

    /** True once switching failed, until the error was shown. */
    var demoServerSwitchFailed by mutableStateOf(false)
        private set

    fun updateAppearance(update: (AppearanceSettings) -> AppearanceSettings) {
        viewModelScope.launch { appearanceRepository.update(update) }
    }

    /**
     * [tag] null is the system language; the screen restarts in the new language. On one of
     * Grocy's demos the demo in that language is offered first, since the restart would end
     * the question.
     */
    fun selectLanguage(tag: String?) {
        val offeredServer = demoServerOfLanguage(tag)
        if (offeredServer == null) {
            languageStore.setLanguage(tag)
        } else {
            demoServerOffer = DemoServerOffer(tag, offeredServer)
        }
    }

    /** Switching applies the language only once the demo answered; dismissing keeps the demo. */
    fun answerDemoServerOffer(switch: Boolean) {
        val offer = demoServerOffer ?: return
        if (isSwitchingDemoServer) return
        if (!switch) {
            demoServerOffer = null
            languageStore.setLanguage(offer.languageTag)
            return
        }
        isSwitchingDemoServer = true
        viewModelScope.launch {
            try {
                demoServerSwitcher.switchTo(offer.server)
                languageStore.setLanguage(offer.languageTag)
            } catch (_: GrocyApiException) {
                // Nothing changes, the language can be chosen again
                demoServerSwitchFailed = true
            } finally {
                demoServerOffer = null
                isSwitchingDemoServer = false
            }
        }
    }

    fun onDemoServerSwitchFailureShown() {
        demoServerSwitchFailed = false
    }

    /** The demo whose language the app will show, null unless it differs from the current. */
    private fun demoServerOfLanguage(tag: String?): DemoServer? {
        val connection = server.value?.connection as? ServerConnection.Demo ?: return null
        // Without a translation of the system language, the app shows the English base
        val shownLanguage = tag ?: resolveAppLanguage(systemLanguage()) ?: AppLanguages.first()
        val demo = demoServerFor(shownLanguage) ?: return null
        return demo.takeIf { it != demoServerOf(connection.serverUrl) }
    }
}
