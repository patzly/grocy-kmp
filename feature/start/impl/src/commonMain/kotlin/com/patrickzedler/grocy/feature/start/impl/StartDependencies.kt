package com.patrickzedler.grocy.feature.start.impl

import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.model.server.ServerConfig
import kotlinx.coroutines.flow.StateFlow

/** What the start screen needs from the app. */
interface StartDependencies {
    val masterDataRepository: MasterDataRepository

    /** The stored server configuration, read at app start (null for the moment until then). */
    val serverConfig: StateFlow<ServerConfig?>
}
