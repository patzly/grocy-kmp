package com.patrickzedler.grocy.feature.start.impl

import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.server.ServerState
import com.patrickzedler.grocy.core.data.stock.StockRepository
import com.patrickzedler.grocy.core.model.server.ServerConfig
import kotlinx.coroutines.flow.StateFlow

/** What the start screen needs from the app. */
interface StartDependencies {
    val masterDataRepository: MasterDataRepository
    val stockRepository: StockRepository

    /** The stored server configuration, read at app start (null for the moment until then). */
    val serverConfig: StateFlow<ServerConfig?>

    /** The logged in server, read at app start, for the demo's warning. */
    val server: StateFlow<ServerState?>
}
