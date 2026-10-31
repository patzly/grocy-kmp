package com.patrickzedler.grocy.feature.masterdata.impl

import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.shoppinglist.ShoppingListRepository
import com.patrickzedler.grocy.core.data.stock.StockRepository
import com.patrickzedler.grocy.core.model.server.ServerConfig
import kotlinx.coroutines.flow.StateFlow

/** What the master data screens need from the app. */
interface MasterDataDependencies {
    val masterDataRepository: MasterDataRepository

    /** The labels the product overview and the product form print. */
    val stockRepository: StockRepository

    /** The stored server configuration, read at app start (null for the moment until then). */
    val serverConfig: StateFlow<ServerConfig?>

    /** The product overview's "Add to shopping list". */
    val shoppingListRepository: ShoppingListRepository
}
