package com.patrickzedler.grocy.feature.settings.impl.grocy

import com.patrickzedler.grocy.core.model.server.ServerConfig
import kotlin.test.Test
import kotlin.test.assertSame
import kotlinx.coroutines.flow.MutableStateFlow

class GrocySettingsViewModelTest {

    @Test
    fun showsTheEagerConfigurationInItsFirstFrame() {
        // The app scope's flow itself, so switched off features are hidden right away
        val config = MutableStateFlow<ServerConfig?>(ServerConfig(stock = false))

        val viewModel = GrocySettingsViewModel(config)

        assertSame(config, viewModel.serverConfig)
    }
}
