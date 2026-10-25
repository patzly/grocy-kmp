package com.patrickzedler.grocy.feature.settings.impl.user

import com.patrickzedler.grocy.core.model.server.ServerConfig
import kotlin.test.Test
import kotlin.test.assertSame
import kotlinx.coroutines.flow.MutableStateFlow

class UserSettingsViewModelTest {

    @Test
    fun showsTheEagerConfigurationInItsFirstFrame() {
        // The app scope's flow itself, so switched off features are hidden right away
        val config = MutableStateFlow<ServerConfig?>(ServerConfig(stock = false))

        val viewModel = UserSettingsViewModel(config)

        assertSame(config, viewModel.serverConfig)
    }
}
