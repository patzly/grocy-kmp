package com.patrickzedler.grocy.core.model.stock

import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.StockAmount
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.text.epochDayOf

/**
 * The numbers of Grocy's stock overview: its products in stock and what they cost, and how many
 * products are due, below their min. stock amount and so on, counted like its messages above
 * the table. Counts of features the server turned off are 0.
 */
data class StockSummary(
    val productCount: Int,
    /** Null without price tracking. */
    val totalValue: Double?,
    val dueSoonCount: Int,
    val overdueCount: Int,
    val expiredCount: Int,
    val belowMinStockCount: Int,
    /** The products below their min. stock amount that are on a shopping list. */
    val belowMinStockOnShoppingListCount: Int,
)

/**
 * Counts [stock] like Grocy's stock overview counts its messages: products not hidden there; due
 * ones need stock of their own and a due date that can be overdue, due soon within [dueSoonDays]
 * days of [today] (days since 1970-01-01).
 */
fun stockSummaryOf(
    products: List<Product>,
    stock: List<StockAmount>,
    info: List<ProductStockInfo>,
    config: ServerConfig,
    dueSoonDays: Int,
    today: Long,
): StockSummary {
    val shown = products.filter { !it.hideOnStockOverview }.associateBy { it.id }
    val shownStock = stock.filter { it.productId in shown }
    val dueStatuses = if (config.stockBestBeforeDateTracking) {
        shownStock.mapNotNull { amount ->
            val date = amount.bestBeforeDate?.takeIf { it != StockEntry.NEVER_OVERDUE }
            val day = date?.let(::epochDayOf)
            if (day == null || amount.amount <= 0) {
                null
            } else {
                dueStatusOf(day, today, dueSoonDays, shown.getValue(amount.productId).dueType)
            }
        }
    } else {
        emptyList()
    }
    val belowMinStock = info.filter { it.amountMissing != null && it.productId in shown }
    return StockSummary(
        productCount = shownStock.size,
        totalValue = stock.sumOf { it.value }.takeIf { config.stockPriceTracking },
        dueSoonCount = dueStatuses.count { it == DueStatus.DUE_SOON },
        overdueCount = dueStatuses.count { it == DueStatus.OVERDUE },
        expiredCount = dueStatuses.count { it == DueStatus.EXPIRED },
        belowMinStockCount = belowMinStock.size,
        belowMinStockOnShoppingListCount = belowMinStock
            .count { it.onShoppingList && config.shoppingList },
    )
}
