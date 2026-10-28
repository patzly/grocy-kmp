package com.patrickzedler.grocy.feature.settings.impl.stock

import com.patrickzedler.grocy.core.model.server.UserSettingValue
import com.patrickzedler.grocy.core.model.server.UserSettings
import com.patrickzedler.grocy.core.model.text.formatDecimal
import com.patrickzedler.grocy.core.model.text.parseDecimal
import com.patrickzedler.grocy.feature.settings.impl.SettingError
import com.patrickzedler.grocy.feature.settings.impl.numberSettingError

/**
 * A number of Grocy's stock settings, [key] its user setting: at least [min], at most [max]
 * if given, with the decimal places [decimals] allows.
 */
internal enum class StockNumberSetting(
    val key: String,
    val min: Double,
    val max: Double? = null,
    val decimals: (UserSettings) -> Int = { 0 },
) {
    PRESET_DUE_DAYS("product_presets_default_due_days", min = -1.0),
    DUE_SOON_DAYS("stock_due_soon_days", min = 1.0),
    PURCHASE_AMOUNT(
        "stock_default_purchase_amount",
        min = 0.0,
        decimals = { it.stockDecimalPlacesAmounts },
    ),
    CONSUME_AMOUNT(
        "stock_default_consume_amount",
        min = 0.0,
        decimals = { it.stockDecimalPlacesAmounts },
    ),
    DECIMALS_AMOUNTS("stock_decimal_places_amounts", min = 0.0, max = MAX_DECIMAL_PLACES),
    DECIMALS_PRICES_INPUT("stock_decimal_places_prices_input", min = 0.0, max = MAX_DECIMAL_PLACES),
    DECIMALS_PRICES_DISPLAY(
        "stock_decimal_places_prices_display",
        min = 0.0,
        max = MAX_DECIMAL_PLACES,
    ),
    ;

    fun valueOf(settings: UserSettings): Double = when (this) {
        PRESET_DUE_DAYS -> settings.productPresets.defaultDueDays.toDouble()
        DUE_SOON_DAYS -> settings.stockDueSoonDays.toDouble()
        PURCHASE_AMOUNT -> settings.stockDefaultPurchaseAmount
        CONSUME_AMOUNT -> settings.stockDefaultConsumeAmount
        DECIMALS_AMOUNTS -> settings.stockDecimalPlacesAmounts.toDouble()
        DECIMALS_PRICES_INPUT -> settings.stockDecimalPlacesPricesInput.toDouble()
        DECIMALS_PRICES_DISPLAY -> settings.stockDecimalPlacesPricesDisplay.toDouble()
    }

    /** The stored value as the field shows it. */
    fun textOf(settings: UserSettings, decimalSeparator: Char): String =
        formatDecimal(valueOf(settings), decimals(settings), decimalSeparator)
}

/** One of Grocy's stock settings that is a checkbox. */
internal enum class StockSwitchSetting(val key: String) {
    TREAT_OPENED_AS_OUT_OF_STOCK("product_presets_treat_opened_as_out_of_stock"),
    SHOW_ICON_ON_SHOPPING_LIST("show_icon_on_stock_overview_page_when_product_is_on_shopping_list"),
    SHOW_ALL_OUT_OF_STOCK("stock_overview_show_all_out_of_stock_products"),
    SHOW_PURCHASED_DATE("show_purchased_date_on_purchase"),
    SHOW_DUE_DATE_WARNING("show_warning_on_purchase_when_due_date_is_earlier_than_next"),
    USE_QUICK_CONSUME_AMOUNT("stock_default_consume_amount_use_quick_consume_amount"),
    AUTO_DECIMAL_SEPARATOR("stock_auto_decimal_separator_prices"),
    ;

    fun valueOf(settings: UserSettings): Boolean = when (this) {
        TREAT_OPENED_AS_OUT_OF_STOCK -> settings.productPresets.treatOpenedAsOutOfStock
        SHOW_ICON_ON_SHOPPING_LIST -> settings.showIconOnStockOverviewWhenOnShoppingList
        SHOW_ALL_OUT_OF_STOCK -> settings.stockOverviewShowAllOutOfStockProducts
        SHOW_PURCHASED_DATE -> settings.showPurchasedDateOnPurchase
        SHOW_DUE_DATE_WARNING -> settings.showWarningOnPurchaseWhenDueDateIsEarlierThanNext
        USE_QUICK_CONSUME_AMOUNT -> settings.stockDefaultConsumeAmountUseQuickConsumeAmount
        AUTO_DECIMAL_SEPARATOR -> settings.stockAutoDecimalSeparatorPrices
    }
}

/** A preset for new products that names an object, -1 in Grocy for none. */
internal enum class StockPresetSetting(val key: String) {
    LOCATION("product_presets_location_id"),
    PRODUCT_GROUP("product_presets_product_group_id"),
    QUANTITY_UNIT("product_presets_qu_id"),
    ;

    fun valueOf(settings: UserSettings): Int? = when (this) {
        LOCATION -> settings.productPresets.locationId
        PRODUCT_GROUP -> settings.productPresets.productGroupId
        QUANTITY_UNIT -> settings.productPresets.quantityUnitId
    }

    /** What Grocy stores for [id]: the id, or -1 for no preset. */
    fun valueOf(id: Int?): UserSettingValue = UserSettingValue.Whole((id ?: NO_PRESET).toLong())
}

internal const val LABEL_TYPE_KEY = "product_presets_default_stock_label_type"

/**
 * Checks a typed number of [setting] like Grocy's number picker, which also names the range
 * for an empty field; null when it is fine.
 */
internal fun StockNumberSetting.errorOf(text: String, settings: UserSettings): SettingError? =
    numberSettingError(text, min, max, decimals(settings))

/** What Grocy stores for a valid typed number of [setting]: integers without fraction. */
internal fun StockNumberSetting.valueOf(text: String, settings: UserSettings): UserSettingValue {
    val value = requireNotNull(parseDecimal(text))
    return if (decimals(settings) == 0) {
        UserSettingValue.Whole(value.toLong())
    } else {
        UserSettingValue.Number(value)
    }
}

private const val NO_PRESET = -1
private const val MAX_DECIMAL_PLACES = 10.0
