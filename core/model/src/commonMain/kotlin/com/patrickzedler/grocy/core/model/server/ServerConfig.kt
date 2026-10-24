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
    val recipes: Boolean = true,
    val chores: Boolean = true,
    val tasks: Boolean = true,
    val batteries: Boolean = true,
    val labelPrinter: Boolean = false,
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

/** User settings of the logged in Grocy user the app uses. Defaults are Grocy's own. */
@Serializable
data class UserSettings(
    /** Decimal places of amounts in inputs and lists. */
    val stockDecimalPlacesAmounts: Int = 4,
    /** Decimal places of prices where they are only shown. */
    val stockDecimalPlacesPricesDisplay: Int = 2,
    val productPresets: ProductPresets = ProductPresets(),
)

/** "Presets for new products" of the stock settings; null ids mean no preset. */
@Serializable
data class ProductPresets(
    val locationId: Int? = null,
    val productGroupId: Int? = null,
    val quantityUnitId: Int? = null,
    val defaultDueDays: Int = 0,
    val treatOpenedAsOutOfStock: Boolean = true,
    val defaultStockLabelType: Int = 0,
)
