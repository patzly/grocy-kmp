package com.patrickzedler.grocy.feature.masterdata.impl

import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.settings.StockOverviewOptionsRepository
import com.patrickzedler.grocy.core.data.stock.StockRepository
import com.patrickzedler.grocy.core.model.server.ServerConfig
import kotlinx.coroutines.flow.StateFlow

/** What the master data screens need from the app. */
interface MasterDataDependencies {
    val masterDataRepository: MasterDataRepository

    /** Stock entries, the stock journal and bookings on them. */
    val stockRepository: StockRepository

    /** The stored server configuration, read at app start (null for the moment until then). */
    val serverConfig: StateFlow<ServerConfig?>

    /** Fields, grouping and sort of the stock overview, read at app start. */
    val stockOverviewOptions: StockOverviewOptionsRepository
}
