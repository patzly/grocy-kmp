package com.patrickzedler.grocy.feature.stock.impl

import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.settings.StockOverviewOptionsRepository
import com.patrickzedler.grocy.core.data.shoppinglist.ShoppingListRepository
import com.patrickzedler.grocy.core.data.stock.StockRepository

/** What Grocy's stock pages need from the app. */
interface StockDependencies {
    val masterDataRepository: MasterDataRepository

    /** Stock entries, the stock journal and bookings on them. */
    val stockRepository: StockRepository

    /** Fields, grouping and sort of the stock overview, read at app start. */
    val stockOverviewOptions: StockOverviewOptionsRepository

    /** The product overview's "Add to shopping list" and purchases of shopping list items. */
    val shoppingListRepository: ShoppingListRepository
}
