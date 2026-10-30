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
        shoppingListMultipleLists = flag(
            "FEATURE_FLAG_SHOPPINGLIST_MULTIPLE_LISTS",
            defaults.shoppingListMultipleLists,
        ),
        recipes = flag("FEATURE_FLAG_RECIPES", defaults.recipes),
        chores = flag("FEATURE_FLAG_CHORES", defaults.chores),
        choresAssignments =
            flag("FEATURE_FLAG_CHORES_ASSIGNMENTS", defaults.choresAssignments),
        tasks = flag("FEATURE_FLAG_TASKS", defaults.tasks),
        batteries = flag("FEATURE_FLAG_BATTERIES", defaults.batteries),
        labelPrinter = flag("FEATURE_FLAG_LABEL_PRINTER", defaults.labelPrinter),
        labelPrinterRunsOnServer =
            flag("LABEL_PRINTER_RUN_SERVER", defaults.labelPrinterRunsOnServer),
        grocycode2D = primitive("GROCYCODE_TYPE")?.content?.trim()?.uppercase()
            ?.let { it != "1D" }
            ?: defaults.grocycode2D,
        thermalPrinter = flag("FEATURE_FLAG_THERMAL_PRINTER", defaults.thermalPrinter),
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
        currency = primitive("CURRENCY")?.content?.trim()?.uppercase()
            ?.takeIf { CURRENCY_CODE.matches(it) }
            ?: defaults.currency,
    )
}

/** The settings of `/user/settings` the app uses; Grocy fills in its defaults itself. */
internal fun JsonObject.toUserSettings(): UserSettings {
    val defaults = UserSettings()
    val presets = defaults.productPresets
    fun int(key: String) = primitive(key)?.content?.toDoubleOrNull()?.toInt()
    // -1 means "no preset" in Grocy
    fun id(key: String) = int(key)?.takeIf { it > 0 }
    fun double(key: String) = primitive(key)?.content?.toDoubleOrNull()
    fun flag(key: String, default: Boolean) = primitive(key)?.toFlag() ?: default
    return UserSettings(
        stockDecimalPlacesAmounts = int("stock_decimal_places_amounts")
            ?.coerceIn(0, MAX_DECIMAL_PLACES)
            ?: defaults.stockDecimalPlacesAmounts,
        stockDecimalPlacesPricesDisplay = int("stock_decimal_places_prices_display")
            ?.coerceIn(0, MAX_DECIMAL_PLACES)
            ?: defaults.stockDecimalPlacesPricesDisplay,
        stockDecimalPlacesPricesInput = int("stock_decimal_places_prices_input")
            ?.coerceIn(0, MAX_DECIMAL_PLACES)
            ?: defaults.stockDecimalPlacesPricesInput,
        stockAutoDecimalSeparatorPrices = flag(
            "stock_auto_decimal_separator_prices",
            defaults.stockAutoDecimalSeparatorPrices,
        ),
        stockDueSoonDays = int("stock_due_soon_days") ?: defaults.stockDueSoonDays,
        showIconOnStockOverviewWhenOnShoppingList = flag(
            "show_icon_on_stock_overview_page_when_product_is_on_shopping_list",
            defaults.showIconOnStockOverviewWhenOnShoppingList,
        ),
        stockOverviewShowAllOutOfStockProducts = flag(
            "stock_overview_show_all_out_of_stock_products",
            defaults.stockOverviewShowAllOutOfStockProducts,
        ),
        stockDefaultPurchaseAmount =
            double("stock_default_purchase_amount") ?: defaults.stockDefaultPurchaseAmount,
        showPurchasedDateOnPurchase =
            flag("show_purchased_date_on_purchase", defaults.showPurchasedDateOnPurchase),
        showWarningOnPurchaseWhenDueDateIsEarlierThanNext = flag(
            "show_warning_on_purchase_when_due_date_is_earlier_than_next",
            defaults.showWarningOnPurchaseWhenDueDateIsEarlierThanNext,
        ),
        stockDefaultConsumeAmount =
            double("stock_default_consume_amount") ?: defaults.stockDefaultConsumeAmount,
        stockDefaultConsumeAmountUseQuickConsumeAmount = flag(
            "stock_default_consume_amount_use_quick_consume_amount",
            defaults.stockDefaultConsumeAmountUseQuickConsumeAmount,
        ),
        shoppingListShowCalendar =
            flag("shopping_list_show_calendar", defaults.shoppingListShowCalendar),
        shoppingListRoundUp = flag("shopping_list_round_up", defaults.shoppingListRoundUp),
        shoppingListAutoAddBelowMinStock = flag(
            "shopping_list_auto_add_below_min_stock_amount",
            defaults.shoppingListAutoAddBelowMinStock,
        ),
        shoppingListAutoAddListId = int("shopping_list_auto_add_below_min_stock_amount_list_id")
            ?: defaults.shoppingListAutoAddListId,
        shoppingListToStockAutoSubmit = flag(
            "shopping_list_to_stock_workflow_auto_submit_when_prefilled",
            defaults.shoppingListToStockAutoSubmit,
        ),
        shoppingListPrintShowHeader =
            flag("shopping_list_print_show_header", defaults.shoppingListPrintShowHeader),
        recipesShowListSideBySide =
            flag("recipes_show_list_side_by_side", defaults.recipesShowListSideBySide),
        recipesShowIngredientCheckbox =
            flag("recipes_show_ingredient_checkbox", defaults.recipesShowIngredientCheckbox),
        recipeIngredientsGroupByProductGroup = flag(
            "recipe_ingredients_group_by_product_group",
            defaults.recipeIngredientsGroupByProductGroup,
        ),
        choresDueSoonDays = int("chores_due_soon_days") ?: defaults.choresDueSoonDays,
        choresOverviewSwapTrackingButtons = flag(
            "chores_overview_swap_tracking_buttons",
            defaults.choresOverviewSwapTrackingButtons,
        ),
        tasksDueSoonDays = int("tasks_due_soon_days") ?: defaults.tasksDueSoonDays,
        batteriesDueSoonDays = int("batteries_due_soon_days") ?: defaults.batteriesDueSoonDays,
        scanModePurchase = flag("scan_mode_purchase_enabled", defaults.scanModePurchase),
        scanModeConsume = flag("scan_mode_consume_enabled", defaults.scanModeConsume),
        locale = primitive("locale")?.content ?: defaults.locale,
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

// Grocy takes any text, formatting prices needs an ISO 4217 code
private val CURRENCY_CODE = Regex("[A-Z]{3}")

private fun JsonObject.primitive(key: String): JsonPrimitive? =
    (get(key) as? JsonPrimitive)?.takeUnless { it is JsonNull }

private fun JsonPrimitive.toFlag(): Boolean? = booleanOrNull ?: when (content.lowercase()) {
    "1", "true", "on" -> true
    "0", "false", "off", "" -> false
    else -> null
}
