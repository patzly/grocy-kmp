package com.patrickzedler.grocy.core.model.server

import kotlinx.serialization.Serializable

/**
 * The parts of the Grocy server configuration the app follows: features the server turned off
 * are hidden like in its web frontend. Defaults are Grocy's own.
 */
@Serializable
data class ServerConfig(
    val stock: Boolean = true,
    val shoppingList: Boolean = true,
    /** Several shopping lists; without, everything goes to the first one. */
    val shoppingListMultipleLists: Boolean = true,
    val recipes: Boolean = true,
    val chores: Boolean = true,
    /** Chores can be assigned to users. */
    val choresAssignments: Boolean = true,
    val tasks: Boolean = true,
    val batteries: Boolean = true,
    val labelPrinter: Boolean = false,
    /**
     * The server runs the label printer's webhook itself. Otherwise Grocy's web frontend calls
     * it, whose address the app never stores, so it prints nothing then.
     */
    val labelPrinterRunsOnServer: Boolean = true,
    /** Grocycodes as 2D DataMatrix, otherwise as 1D Code 128 like Grocy's `GROCYCODE_TYPE`. */
    val grocycode2D: Boolean = true,
    val stockPriceTracking: Boolean = true,
    val stockLocationTracking: Boolean = true,
    val stockBestBeforeDateTracking: Boolean = true,
    val stockProductOpenedTracking: Boolean = true,
    val stockProductFreezing: Boolean = true,
    val browserBarcodeCameraScanning: Boolean = true,
    val energyUnit: String = "kcal",
    /** ISO 4217 code of the server's prices. */
    val currency: String = "USD",
)

/**
 * User settings of the logged in Grocy user the app uses, the stock settings among them.
 * Defaults are Grocy's own. Stored as JSON, so new fields need defaults.
 */
@Serializable
data class UserSettings(
    /** Decimal places of amounts in inputs and lists. */
    val stockDecimalPlacesAmounts: Int = 4,
    /** Decimal places of prices where they are only shown. */
    val stockDecimalPlacesPricesDisplay: Int = 2,
    /** Decimal places of prices where they are entered. */
    val stockDecimalPlacesPricesInput: Int = 2,
    /** Price inputs get their decimal separator from the allowed decimal places. */
    val stockAutoDecimalSeparatorPrices: Boolean = false,
    val productPresets: ProductPresets = ProductPresets(),
    /** Entries due within these days are "due soon". */
    val stockDueSoonDays: Int = 5,
    val showIconOnStockOverviewWhenOnShoppingList: Boolean = true,
    val stockOverviewShowAllOutOfStockProducts: Boolean = false,
    val stockDefaultPurchaseAmount: Double = 0.0,
    val showPurchasedDateOnPurchase: Boolean = false,
    val showWarningOnPurchaseWhenDueDateIsEarlierThanNext: Boolean = true,
    val stockDefaultConsumeAmount: Double = 1.0,
    val stockDefaultConsumeAmountUseQuickConsumeAmount: Boolean = false,
)

/**
 * "Presets for new products" of the stock settings; null ids mean no preset, -1 due days that
 * new products are never overdue.
 */
@Serializable
data class ProductPresets(
    val locationId: Int? = null,
    val productGroupId: Int? = null,
    val quantityUnitId: Int? = null,
    val defaultDueDays: Int = 0,
    val treatOpenedAsOutOfStock: Boolean = true,
    val defaultStockLabelType: Int = 0,
)
