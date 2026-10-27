package com.patrickzedler.grocy.core.model.stock

import com.patrickzedler.grocy.core.model.masterdata.DueType
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.StockAmount
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.text.epochDayOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StockSummaryTest {

    private val today = epochDayOf("2026-10-25")!!

    private val products = listOf(
        Product(1, "Milk"),
        Product(2, "Cheese", dueType = DueType.EXPIRATION),
        Product(3, "Bread"),
        Product(4, "Yoghurt"),
        Product(5, "Hidden", hideOnStockOverview = true),
    )

    private val stock = listOf(
        StockAmount(1, 2.0, 2.0, value = 1.5, bestBeforeDate = "2026-10-27"),
        StockAmount(2, 1.0, 1.0, value = 3.0, bestBeforeDate = "2026-10-20"),
        StockAmount(3, 1.0, 1.0, value = 2.0, bestBeforeDate = "2026-10-24"),
        StockAmount(5, 1.0, 1.0, value = 1.0, bestBeforeDate = "2026-10-20"),
    )

    private val info = listOf(
        ProductStockInfo(4, amountMissing = 1.0, onShoppingList = true),
        ProductStockInfo(3, amountMissing = 2.0),
        ProductStockInfo(5, amountMissing = 1.0, onShoppingList = true),
    )

    private fun summary(config: ServerConfig = ServerConfig()) =
        stockSummaryOf(products, stock, info, config, dueSoonDays = 5, today = today)

    @Test
    fun countsTheRowsOfGrocysStockOverview() {
        val summary = summary()

        // Hidden products are not counted, their value is
        assertEquals(3, summary.productCount)
        assertEquals(1, summary.dueSoonCount)
        assertEquals(1, summary.overdueCount)
        assertEquals(1, summary.expiredCount)
        assertEquals(2, summary.belowMinStockCount)
        assertEquals(1, summary.belowMinStockOnShoppingListCount)
        assertEquals(7.5, summary.totalValue)
    }

    @Test
    fun featuresTheServerTurnedOffCountNothing() {
        val summary = summary(
            ServerConfig(
                stockPriceTracking = false,
                stockBestBeforeDateTracking = false,
                shoppingList = false,
            ),
        )

        assertNull(summary.totalValue)
        assertEquals(0, summary.dueSoonCount + summary.overdueCount + summary.expiredCount)
        assertEquals(0, summary.belowMinStockOnShoppingListCount)
        assertEquals(2, summary.belowMinStockCount)
    }

    @Test
    fun stockThatIsNeverOverdueOrUsedUpIsNotDue() {
        val summary = stockSummaryOf(
            products = products,
            stock = listOf(
                StockAmount(1, 1.0, 1.0, bestBeforeDate = StockEntry.NEVER_OVERDUE),
                StockAmount(3, 0.0, 1.0, bestBeforeDate = "2026-10-20"),
            ),
            info = emptyList(),
            config = ServerConfig(),
            dueSoonDays = 5,
            today = today,
        )

        assertEquals(0, summary.dueSoonCount + summary.overdueCount + summary.expiredCount)
    }
}
