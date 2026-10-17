package com.patrickzedler.grocy.core.database

import com.patrickzedler.grocy.core.model.masterdata.DueType
import com.patrickzedler.grocy.core.model.masterdata.Location
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.ProductBarcode
import com.patrickzedler.grocy.core.model.masterdata.ProductGroup
import com.patrickzedler.grocy.core.model.masterdata.PurchasePriceType
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.StockAmount
import com.patrickzedler.grocy.core.model.masterdata.StockLabelType
import com.patrickzedler.grocy.core.model.masterdata.Store
import com.patrickzedler.grocy.core.model.masterdata.Userfield
import com.patrickzedler.grocy.core.model.masterdata.UserfieldType
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val userfieldValuesSerializer =
    MapSerializer(String.serializer(), String.serializer().nullable)

fun Product.toEntity() = ProductEntity(
    id = id,
    name = name,
    description = description,
    active = active,
    parentProductId = parentProductId,
    locationId = locationId,
    defaultConsumeLocationId = defaultConsumeLocationId,
    moveOnOpen = moveOnOpen,
    storeId = storeId,
    minStockAmount = minStockAmount,
    cumulateMinStockAmountOfSubProducts = cumulateMinStockAmountOfSubProducts,
    treatOpenedAsOutOfStock = treatOpenedAsOutOfStock,
    dueType = dueType.value,
    defaultDueDays = defaultDueDays,
    defaultDueDaysAfterOpen = defaultDueDaysAfterOpen,
    defaultDueDaysAfterFreezing = defaultDueDaysAfterFreezing,
    defaultDueDaysAfterThawing = defaultDueDaysAfterThawing,
    shouldNotBeFrozen = shouldNotBeFrozen,
    productGroupId = productGroupId,
    quIdStock = quIdStock,
    quIdPurchase = quIdPurchase,
    quIdConsume = quIdConsume,
    quIdPrice = quIdPrice,
    enableTareWeightHandling = enableTareWeightHandling,
    tareWeight = tareWeight,
    notCheckStockFulfillmentForRecipes = notCheckStockFulfillmentForRecipes,
    calories = calories,
    quickConsumeAmount = quickConsumeAmount,
    quickOpenAmount = quickOpenAmount,
    defaultStockLabelType = defaultStockLabelType.value,
    autoReprintStockLabel = autoReprintStockLabel,
    defaultPurchasePriceType = defaultPurchasePriceType.value,
    disableOpen = disableOpen,
    hideOnStockOverview = hideOnStockOverview,
    noOwnStock = noOwnStock,
    pictureFileName = pictureFileName,
    userfields = Json.encodeToString(userfieldValuesSerializer, userfields),
)

fun ProductEntity.toModel() = Product(
    id = id,
    name = name,
    description = description,
    active = active,
    parentProductId = parentProductId,
    locationId = locationId,
    defaultConsumeLocationId = defaultConsumeLocationId,
    moveOnOpen = moveOnOpen,
    storeId = storeId,
    minStockAmount = minStockAmount,
    cumulateMinStockAmountOfSubProducts = cumulateMinStockAmountOfSubProducts,
    treatOpenedAsOutOfStock = treatOpenedAsOutOfStock,
    dueType = DueType.of(dueType),
    defaultDueDays = defaultDueDays,
    defaultDueDaysAfterOpen = defaultDueDaysAfterOpen,
    defaultDueDaysAfterFreezing = defaultDueDaysAfterFreezing,
    defaultDueDaysAfterThawing = defaultDueDaysAfterThawing,
    shouldNotBeFrozen = shouldNotBeFrozen,
    productGroupId = productGroupId,
    quIdStock = quIdStock,
    quIdPurchase = quIdPurchase,
    quIdConsume = quIdConsume,
    quIdPrice = quIdPrice,
    enableTareWeightHandling = enableTareWeightHandling,
    tareWeight = tareWeight,
    notCheckStockFulfillmentForRecipes = notCheckStockFulfillmentForRecipes,
    calories = calories,
    quickConsumeAmount = quickConsumeAmount,
    quickOpenAmount = quickOpenAmount,
    defaultStockLabelType = StockLabelType.of(defaultStockLabelType),
    autoReprintStockLabel = autoReprintStockLabel,
    defaultPurchasePriceType = PurchasePriceType.of(defaultPurchasePriceType),
    disableOpen = disableOpen,
    hideOnStockOverview = hideOnStockOverview,
    noOwnStock = noOwnStock,
    pictureFileName = pictureFileName,
    userfields = decodeUserfieldValues(userfields),
)

private fun decodeUserfieldValues(json: String): Map<String, String?> = try {
    Json.decodeFromString(userfieldValuesSerializer, json)
} catch (_: SerializationException) {
    emptyMap()
} catch (_: IllegalArgumentException) {
    emptyMap()
}

fun ProductGroup.toEntity() = ProductGroupEntity(id, name, description, active)

fun ProductGroupEntity.toModel() = ProductGroup(id, name, description, active)

fun Location.toEntity() = LocationEntity(id, name, description, isFreezer, active)

fun LocationEntity.toModel() = Location(id, name, description, isFreezer, active)

fun QuantityUnit.toEntity() = QuantityUnitEntity(id, name, namePlural, description, active)

fun QuantityUnitEntity.toModel() = QuantityUnit(id, name, namePlural, description, active)

fun Store.toEntity() = StoreEntity(id, name, description, active)

fun StoreEntity.toModel() = Store(id, name, description, active)

fun ProductBarcode.toEntity() =
    ProductBarcodeEntity(id, productId, barcode, quId, amount, storeId, lastPrice, note)

fun ProductBarcodeEntity.toModel() =
    ProductBarcode(id, productId, barcode, quId, amount, storeId, lastPrice, note)

fun StockAmount.toEntity() = StockEntity(productId, amount, amountAggregated)

fun StockEntity.toModel() = StockAmount(productId, amount, amountAggregated)

fun Userfield.toEntity() = UserfieldEntity(
    id = id,
    entity = entity,
    name = name,
    caption = caption,
    type = type.value,
    showAsColumnInTables = showAsColumnInTables,
    inputRequired = inputRequired,
    options = options.joinToString("\n"),
    defaultValue = defaultValue,
    sortNumber = sortNumber,
)

fun UserfieldEntity.toModel() = Userfield(
    id = id,
    entity = entity,
    name = name,
    caption = caption,
    type = UserfieldType.of(type),
    showAsColumnInTables = showAsColumnInTables,
    inputRequired = inputRequired,
    options = options.lines().filter { it.isNotEmpty() },
    defaultValue = defaultValue,
    sortNumber = sortNumber,
)
