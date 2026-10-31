package com.patrickzedler.grocy.feature.masterdata.impl

import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.settings.ShoppingListOptionsRepository
import com.patrickzedler.grocy.core.data.shoppinglist.ShoppingListRepository
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

    /** The shopping lists and their items, checked off offline too. */
    val shoppingListRepository: ShoppingListRepository

    /** Fields and grouping of the shopping list, read at app start. */
    val shoppingListOptions: ShoppingListOptionsRepository
}
