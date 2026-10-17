package com.patrickzedler.grocy.core.network.dto

import com.patrickzedler.grocy.core.model.server.ProductPresets
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.server.UserSettings
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/**
 * The feature flags of `/system/config`. Only known keys are read, the rest of the server
 * configuration (e.g. webhooks) never reaches the app's storage.
 */
internal fun JsonObject.toServerConfig(): ServerConfig {
    val defaults = ServerConfig()
    fun flag(key: String, default: Boolean) = primitive(key)?.toFlag() ?: default
    return ServerConfig(
        stock = flag("FEATURE_FLAG_STOCK", defaults.stock),
        shoppingList = flag("FEATURE_FLAG_SHOPPINGLIST", defaults.shoppingList),
        recipes = flag("FEATURE_FLAG_RECIPES", defaults.recipes),
        chores = flag("FEATURE_FLAG_CHORES", defaults.chores),
        tasks = flag("FEATURE_FLAG_TASKS", defaults.tasks),
        batteries = flag("FEATURE_FLAG_BATTERIES", defaults.batteries),
        labelPrinter = flag("FEATURE_FLAG_LABEL_PRINTER", defaults.labelPrinter),
        stockPriceTracking = flag("FEATURE_FLAG_STOCK_PRICE_TRACKING", defaults.stockPriceTracking),
        stockLocationTracking =
            flag("FEATURE_FLAG_STOCK_LOCATION_TRACKING", defaults.stockLocationTracking),
        stockBestBeforeDateTracking = flag(
            "FEATURE_FLAG_STOCK_BEST_BEFORE_DATE_TRACKING",
            defaults.stockBestBeforeDateTracking,
        ),
        stockProductOpenedTracking = flag(
            "FEATURE_FLAG_STOCK_PRODUCT_OPENED_TRACKING",
            defaults.stockProductOpenedTracking,
        ),
        stockProductFreezing =
            flag("FEATURE_FLAG_STOCK_PRODUCT_FREEZING", defaults.stockProductFreezing),
        browserBarcodeCameraScanning =
            !flag("FEATURE_FLAG_DISABLE_BROWSER_BARCODE_CAMERA_SCANNING", false),
        energyUnit = primitive("ENERGY_UNIT")?.content?.takeIf { it.isNotBlank() }
            ?: defaults.energyUnit,
    )
}

/** The settings of `/user/settings` the app uses; Grocy fills in its defaults itself. */
internal fun JsonObject.toUserSettings(): UserSettings {
    val defaults = UserSettings()
    val presets = defaults.productPresets
    fun int(key: String) = primitive(key)?.content?.toDoubleOrNull()?.toInt()
    // -1 means "no preset" in Grocy
    fun id(key: String) = int(key)?.takeIf { it > 0 }
    return UserSettings(
        stockDecimalPlacesAmounts = int("stock_decimal_places_amounts")
            ?.coerceIn(0, MAX_DECIMAL_PLACES)
            ?: defaults.stockDecimalPlacesAmounts,
        productPresets = ProductPresets(
            locationId = id("product_presets_location_id"),
            productGroupId = id("product_presets_product_group_id"),
            quantityUnitId = id("product_presets_qu_id"),
            defaultDueDays = int("product_presets_default_due_days") ?: presets.defaultDueDays,
            treatOpenedAsOutOfStock = primitive("product_presets_treat_opened_as_out_of_stock")
                ?.toFlag() ?: presets.treatOpenedAsOutOfStock,
            defaultStockLabelType = int("product_presets_default_stock_label_type")
                ?: presets.defaultStockLabelType,
        ),
    )
}

private const val MAX_DECIMAL_PLACES = 10

private fun JsonObject.primitive(key: String): JsonPrimitive? =
    (get(key) as? JsonPrimitive)?.takeUnless { it is JsonNull }

private fun JsonPrimitive.toFlag(): Boolean? = booleanOrNull ?: when (content.lowercase()) {
    "1", "true", "on" -> true
    "0", "false", "off", "" -> false
    else -> null
}
