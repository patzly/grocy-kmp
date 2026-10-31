package com.patrickzedler.grocy.feature.shoppinglist.impl

import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.settings.ShoppingListOptionsRepository
import com.patrickzedler.grocy.core.data.settings.ShoppingModeSettingsRepository
import com.patrickzedler.grocy.core.data.shoppinglist.ShoppingListRepository
import com.patrickzedler.grocy.core.data.stock.StockRepository

/** What Grocy's shopping list needs from the app. */
interface ShoppingListDependencies {
    val masterDataRepository: MasterDataRepository

    /** Stock amounts of the items' products and the product overview's labels. */
    val stockRepository: StockRepository

    /** The shopping lists and their items, checked off offline too. */
    val shoppingListRepository: ShoppingListRepository

    /** Fields and grouping of the shopping list, read at app start. */
    val shoppingListOptions: ShoppingListOptionsRepository

    /** How the shopping mode syncs and shows the list, read at app start. */
    val shoppingModeSettings: ShoppingModeSettingsRepository
}
