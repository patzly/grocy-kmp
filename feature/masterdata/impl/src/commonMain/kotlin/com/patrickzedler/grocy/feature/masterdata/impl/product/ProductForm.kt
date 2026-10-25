package com.patrickzedler.grocy.feature.masterdata.impl.product

import com.patrickzedler.grocy.core.model.masterdata.DueType
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.PurchasePriceType
import com.patrickzedler.grocy.core.model.masterdata.StockLabelType
import com.patrickzedler.grocy.core.model.server.ProductPresets
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.text.decimalPlaces
import com.patrickzedler.grocy.core.model.text.formatDecimal
import com.patrickzedler.grocy.core.model.text.parseDecimal

/** The product form's inputs as typed, before validation. */
internal data class ProductFormValues(
    val name: String = "",
    val active: Boolean = true,
    val parentProductId: Int? = null,
    val description: String = "",
    val locationId: Int? = null,
    val defaultConsumeLocationId: Int? = null,
    val moveOnOpen: Boolean = false,
    val storeId: Int? = null,
    val minStockAmount: String = "0",
    val cumulateMinStockAmountOfSubProducts: Boolean = false,
    val treatOpenedAsOutOfStock: Boolean = true,
    val dueType: DueType = DueType.BEST_BEFORE,
    val defaultDueDays: String = "0",
    val defaultDueDaysAfterOpen: String = "0",
    val defaultDueDaysAfterFreezing: String = "0",
    val defaultDueDaysAfterThawing: String = "0",
    val shouldNotBeFrozen: Boolean = false,
    val productGroupId: Int? = null,
    val quIdStock: Int? = null,
    val quIdPurchase: Int? = null,
    val quIdConsume: Int? = null,
    val quIdPrice: Int? = null,
    val enableTareWeightHandling: Boolean = false,
    val tareWeight: String = "0",
    val notCheckStockFulfillmentForRecipes: Boolean = false,
    val calories: String = "",
    val quickConsumeAmount: String = "1",
    val quickOpenAmount: String = "1",
    val defaultStockLabelType: StockLabelType = StockLabelType.NONE,
    val autoReprintStockLabel: Boolean = false,
    val defaultPurchasePriceType: PurchasePriceType = PurchasePriceType.UNSPECIFIED,
    val disableOpen: Boolean = false,
    val hideOnStockOverview: Boolean = false,
    val noOwnStock: Boolean = false,
)

/** Inputs that can be invalid. */
internal enum class ProductField {
    NAME,
    LOCATION,
    MIN_STOCK_AMOUNT,
    DUE_DAYS,
    DUE_DAYS_AFTER_OPEN,
    DUE_DAYS_AFTER_FREEZING,
    DUE_DAYS_AFTER_THAWING,
    QU_STOCK,
    QU_PURCHASE,
    QU_CONSUME,
    QU_PRICE,
    TARE_WEIGHT,
    CALORIES,
    QUICK_CONSUME_AMOUNT,
    QUICK_OPEN_AMOUNT,
}

internal sealed interface FieldError {
    data object Required : FieldError

    /** Grocy's number message: at least [min], at most [decimals] decimal places. */
    data class InvalidNumber(val min: String, val decimals: Int) : FieldError

    /** Not a date and time as "YYYY-MM-DD HH:MM:SS". */
    data object InvalidDateTime : FieldError

    /** Not a date as "YYYY-MM-DD". */
    data object InvalidDate : FieldError
}

/** Which parts of the form the server's features show, like Grocy's feature flags. */
internal data class ProductFormFeatures(
    val locations: Boolean,
    val stores: Boolean,
    val dueDates: Boolean,
    val opening: Boolean,
    val freezing: Boolean,
    val recipes: Boolean,
    val labelPrinter: Boolean,
) {
    companion object {
        fun of(config: ServerConfig) = ProductFormFeatures(
            locations = config.stockLocationTracking,
            stores = config.stockPriceTracking,
            dueDates = config.stockBestBeforeDateTracking,
            opening = config.stockProductOpenedTracking,
            freezing = config.stockProductFreezing,
            recipes = config.recipes,
            labelPrinter = config.labelPrinter,
        )
    }
}

/** Smallest amount Grocy accepts for quick amounts: 1 without decimals, else 0.0…01. */
internal fun minimumAmount(decimals: Int): Double {
    var value = 1.0
    repeat(decimals.coerceAtLeast(0)) { value /= 10 }
    return value
}

/**
 * Checks the values like Grocy's form: required fields and number ranges of the visible
 * fields. [minStockEditable] is false when the parent product accumulates the min. stock amount.
 */
internal fun ProductFormValues.validate(
    features: ProductFormFeatures,
    decimals: Int,
    minStockEditable: Boolean = true,
): Map<ProductField, FieldError> = buildMap {
    fun require(field: ProductField, valid: Boolean) {
        if (!valid) put(field, FieldError.Required)
    }

    fun number(field: ProductField, text: String, min: Double, places: Int, required: Boolean) {
        if (text.isBlank() && !required) return
        val value = parseDecimal(text)
        if (value == null || value < min || decimalPlaces(text) > places) {
            put(field, FieldError.InvalidNumber(formatDecimal(min, places), places))
        }
    }

    require(ProductField.NAME, name.isNotBlank())
    if (features.locations) require(ProductField.LOCATION, locationId != null)
    if (minStockEditable) {
        number(ProductField.MIN_STOCK_AMOUNT, minStockAmount, 0.0, decimals, required = true)
    }
    if (features.dueDates) {
        number(ProductField.DUE_DAYS, defaultDueDays, -1.0, 0, required = true)
        if (features.opening) {
            number(ProductField.DUE_DAYS_AFTER_OPEN, defaultDueDaysAfterOpen, 0.0, 0, true)
        }
    }
    if (features.freezing) {
        number(ProductField.DUE_DAYS_AFTER_FREEZING, defaultDueDaysAfterFreezing, -1.0, 0, true)
        number(ProductField.DUE_DAYS_AFTER_THAWING, defaultDueDaysAfterThawing, 0.0, 0, true)
    }
    require(ProductField.QU_STOCK, quIdStock != null)
    require(ProductField.QU_PURCHASE, quIdPurchase != null)
    require(ProductField.QU_CONSUME, quIdConsume != null)
    require(ProductField.QU_PRICE, quIdPrice != null)
    if (enableTareWeightHandling) {
        number(ProductField.TARE_WEIGHT, tareWeight, 0.0, decimals, required = true)
    }
    number(ProductField.CALORIES, calories, 0.0, decimals, required = false)
    val minimum = minimumAmount(decimals)
    number(ProductField.QUICK_CONSUME_AMOUNT, quickConsumeAmount, minimum, decimals, true)
    number(ProductField.QUICK_OPEN_AMOUNT, quickOpenAmount, minimum, decimals, true)
}

/**
 * The product to save from valid values. Fields the server's features hide keep the values of
 * [original] (or Grocy's defaults for a new product), as Grocy's form does not send them.
 * [originalDescriptionText] is the edited text of the original description: unchanged, the
 * description keeps its HTML.
 */
internal fun ProductFormValues.toProduct(
    id: Int,
    original: Product?,
    originalDescriptionText: String,
    features: ProductFormFeatures,
): Product {
    val base = original ?: Product(id = id, name = "")
    fun int(text: String, fallback: Int) = parseDecimal(text)?.toInt() ?: fallback
    fun decimal(text: String, fallback: Double) = parseDecimal(text) ?: fallback
    return base.copy(
        id = id,
        name = name.trim(),
        active = active,
        parentProductId = parentProductId,
        description = if (description == originalDescriptionText) {
            base.description
        } else {
            plainTextToHtml(description)
        },
        locationId = if (features.locations) locationId else base.locationId,
        defaultConsumeLocationId =
            if (features.locations) defaultConsumeLocationId else base.defaultConsumeLocationId,
        moveOnOpen = if (features.locations && features.opening) moveOnOpen else base.moveOnOpen,
        storeId = if (features.stores) storeId else base.storeId,
        minStockAmount = decimal(minStockAmount, base.minStockAmount),
        cumulateMinStockAmountOfSubProducts = cumulateMinStockAmountOfSubProducts,
        treatOpenedAsOutOfStock =
            if (features.opening) treatOpenedAsOutOfStock else base.treatOpenedAsOutOfStock,
        dueType = if (features.dueDates) dueType else base.dueType,
        defaultDueDays = if (features.dueDates) int(defaultDueDays, 0) else base.defaultDueDays,
        defaultDueDaysAfterOpen = if (features.dueDates && features.opening) {
            int(defaultDueDaysAfterOpen, 0)
        } else {
            base.defaultDueDaysAfterOpen
        },
        defaultDueDaysAfterFreezing = if (features.freezing) {
            int(defaultDueDaysAfterFreezing, 0)
        } else {
            base.defaultDueDaysAfterFreezing
        },
        defaultDueDaysAfterThawing = if (features.freezing) {
            int(defaultDueDaysAfterThawing, 0)
        } else {
            base.defaultDueDaysAfterThawing
        },
        shouldNotBeFrozen = if (features.freezing) shouldNotBeFrozen else base.shouldNotBeFrozen,
        productGroupId = productGroupId,
        quIdStock = quIdStock,
        quIdPurchase = quIdPurchase,
        quIdConsume = quIdConsume,
        quIdPrice = quIdPrice,
        enableTareWeightHandling = enableTareWeightHandling,
        tareWeight = if (enableTareWeightHandling) decimal(tareWeight, 0.0) else base.tareWeight,
        notCheckStockFulfillmentForRecipes = if (features.recipes) {
            notCheckStockFulfillmentForRecipes
        } else {
            base.notCheckStockFulfillmentForRecipes
        },
        calories = parseDecimal(calories),
        quickConsumeAmount = decimal(quickConsumeAmount, 1.0),
        quickOpenAmount = decimal(quickOpenAmount, 1.0),
        defaultStockLabelType =
            if (features.labelPrinter) defaultStockLabelType else base.defaultStockLabelType,
        autoReprintStockLabel =
            if (features.labelPrinter) autoReprintStockLabel else base.autoReprintStockLabel,
        defaultPurchasePriceType = if (features.stores) {
            defaultPurchasePriceType
        } else {
            base.defaultPurchasePriceType
        },
        disableOpen = if (features.opening) disableOpen else base.disableOpen,
        hideOnStockOverview = hideOnStockOverview,
        noOwnStock = noOwnStock,
    )
}

/** The values of an existing product, numbers in the user's notation. */
internal fun Product.toFormValues(decimals: Int, decimalSeparator: Char) = ProductFormValues(
    name = name,
    active = active,
    parentProductId = parentProductId,
    description = htmlToPlainText(description.orEmpty()),
    locationId = locationId,
    defaultConsumeLocationId = defaultConsumeLocationId,
    moveOnOpen = moveOnOpen,
    storeId = storeId,
    minStockAmount = formatDecimal(minStockAmount, decimals, decimalSeparator),
    cumulateMinStockAmountOfSubProducts = cumulateMinStockAmountOfSubProducts,
    treatOpenedAsOutOfStock = treatOpenedAsOutOfStock,
    dueType = dueType,
    defaultDueDays = defaultDueDays.toString(),
    defaultDueDaysAfterOpen = defaultDueDaysAfterOpen.toString(),
    defaultDueDaysAfterFreezing = defaultDueDaysAfterFreezing.toString(),
    defaultDueDaysAfterThawing = defaultDueDaysAfterThawing.toString(),
    shouldNotBeFrozen = shouldNotBeFrozen,
    productGroupId = productGroupId,
    quIdStock = quIdStock,
    quIdPurchase = quIdPurchase,
    quIdConsume = quIdConsume,
    quIdPrice = quIdPrice,
    enableTareWeightHandling = enableTareWeightHandling,
    tareWeight = formatDecimal(tareWeight, decimals, decimalSeparator),
    notCheckStockFulfillmentForRecipes = notCheckStockFulfillmentForRecipes,
    calories = calories?.let { formatDecimal(it, decimals, decimalSeparator) }.orEmpty(),
    quickConsumeAmount = formatDecimal(quickConsumeAmount, decimals, decimalSeparator),
    quickOpenAmount = formatDecimal(quickOpenAmount, decimals, decimalSeparator),
    defaultStockLabelType = defaultStockLabelType,
    autoReprintStockLabel = autoReprintStockLabel,
    defaultPurchasePriceType = defaultPurchasePriceType,
    disableOpen = disableOpen,
    hideOnStockOverview = hideOnStockOverview,
    noOwnStock = noOwnStock,
)

/** A new product as Grocy prefills it, with the user's "Presets for new products". */
internal fun newProductValues(presets: ProductPresets, features: ProductFormFeatures) =
    ProductFormValues(
        locationId = presets.locationId.takeIf { features.locations },
        productGroupId = presets.productGroupId,
        // The preset unit fills all four unit fields
        quIdStock = presets.quantityUnitId,
        quIdPurchase = presets.quantityUnitId,
        quIdConsume = presets.quantityUnitId,
        quIdPrice = presets.quantityUnitId,
        defaultDueDays = presets.defaultDueDays.toString(),
        treatOpenedAsOutOfStock = presets.treatOpenedAsOutOfStock,
        defaultStockLabelType = StockLabelType.of(presets.defaultStockLabelType),
    )

/**
 * Grocy's rule for the purchase, consume and price unit when the stock unit changes: they follow
 * it while empty or equal to the previous stock unit, a unit the user chose stays.
 */
internal fun linkedQuantityUnit(current: Int?, previousStock: Int?, newStock: Int?): Int? =
    if (current == null || current == previousStock) newStock else current

/** Grocy keeps descriptions as HTML; the form edits them as plain text. */
internal fun htmlToPlainText(html: String): String {
    if (html.isBlank()) return ""
    val text = html
        .replace(Regex("(?i)<br\\s*/?>"), "\n")
        .replace(Regex("(?i)</(p|div|li|h[1-6])>"), "\n\n")
        .replace(Regex("(?i)<li[^>]*>"), "• ")
        .replace(Regex("<[^>]*>"), "")
        .replace("&nbsp;", " ")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&amp;", "&")
    return text.replace(Regex("\n{3,}"), "\n\n").trim()
}

/** Plain text as HTML paragraphs for Grocy's description, line breaks kept. */
internal fun plainTextToHtml(text: String): String {
    if (text.isBlank()) return ""
    return text.trim()
        .split(Regex("\n\\s*\n"))
        .joinToString("") { paragraph ->
            val escaped = paragraph.trim()
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>")
            "<p>$escaped</p>"
        }
}
