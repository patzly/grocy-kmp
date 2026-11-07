package com.patrickzedler.grocy.feature.calendar.impl

import com.patrickzedler.grocy.core.data.calendar.CalendarRepository
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.settings.CalendarOptionsRepository
import com.patrickzedler.grocy.core.data.shoppinglist.ShoppingListRepository
import com.patrickzedler.grocy.core.data.stock.StockRepository

/** What Grocy's calendar needs from the app, the stock ones for its products' overview. */
interface CalendarDependencies {
    val masterDataRepository: MasterDataRepository

    val calendarRepository: CalendarRepository

    val calendarOptions: CalendarOptionsRepository

    val stockRepository: StockRepository

    val shoppingListRepository: ShoppingListRepository
}
