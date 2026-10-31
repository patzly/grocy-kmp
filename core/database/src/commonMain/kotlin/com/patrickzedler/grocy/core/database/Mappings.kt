package com.patrickzedler.grocy.core.database

import com.patrickzedler.grocy.core.model.User
import com.patrickzedler.grocy.core.model.masterdata.Battery
import com.patrickzedler.grocy.core.model.masterdata.Chore
import com.patrickzedler.grocy.core.model.masterdata.ChoreAssignmentType
import com.patrickzedler.grocy.core.model.masterdata.ChorePeriodType
import com.patrickzedler.grocy.core.model.masterdata.DueType
import com.patrickzedler.grocy.core.model.masterdata.Location
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.ProductBarcode
import com.patrickzedler.grocy.core.model.masterdata.ProductGroup
import com.patrickzedler.grocy.core.model.masterdata.PurchasePriceType
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnitConversion
import com.patrickzedler.grocy.core.model.masterdata.ResolvedQuantityUnitConversion
import com.patrickzedler.grocy.core.model.masterdata.StockAmount
import com.patrickzedler.grocy.core.model.masterdata.StockLabelType
import com.patrickzedler.grocy.core.model.masterdata.Store
import com.patrickzedler.grocy.core.model.masterdata.TaskCategory
import com.patrickzedler.grocy.core.model.masterdata.Userentity
import com.patrickzedler.grocy.core.model.masterdata.Userfield
import com.patrickzedler.grocy.core.model.masterdata.UserfieldType
import com.patrickzedler.grocy.core.model.masterdata.Weekday
import com.patrickzedler.grocy.core.model.recipes.Recipe
import com.patrickzedler.grocy.core.model.recipes.RecipeFulfillment
import com.patrickzedler.grocy.core.model.recipes.RecipeIngredient
import com.patrickzedler.grocy.core.model.recipes.RecipeNesting
import com.patrickzedler.grocy.core.model.recipes.ResolvedRecipeIngredient
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingList
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListItem
import com.patrickzedler.grocy.core.model.stock.ProductStockInfo
import com.patrickzedler.grocy.core.model.stock.StockEntry
import com.patrickzedler.grocy.core.model.stock.StockLocationAmount
import com.patrickzedler.grocy.core.model.stock.StockLogEntry
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
    userfields = encodeUserfieldValues(userfields),
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

private fun encodeUserfieldValues(values: Map<String, String?>): String =
    Json.encodeToString(userfieldValuesSerializer, values)

private fun decodeUserfieldValues(json: String): Map<String, String?> = try {
    Json.decodeFromString(userfieldValuesSerializer, json)
} catch (_: SerializationException) {
    emptyMap()
} catch (_: IllegalArgumentException) {
    emptyMap()
}

fun ProductGroup.toEntity() =
    ProductGroupEntity(id, name, description, active, encodeUserfieldValues(userfields))

fun ProductGroupEntity.toModel() =
    ProductGroup(id, name, description, active, decodeUserfieldValues(userfields))

fun Location.toEntity() =
    LocationEntity(id, name, description, isFreezer, active, encodeUserfieldValues(userfields))

fun LocationEntity.toModel() =
    Location(id, name, description, isFreezer, active, decodeUserfieldValues(userfields))

fun QuantityUnit.toEntity() = QuantityUnitEntity(
    id = id,
    name = name,
    namePlural = namePlural,
    description = description,
    active = active,
    pluralForms = pluralForms,
    userfields = encodeUserfieldValues(userfields),
)

fun QuantityUnitEntity.toModel() = QuantityUnit(
    id = id,
    name = name,
    namePlural = namePlural,
    description = description,
    active = active,
    pluralForms = pluralForms,
    userfields = decodeUserfieldValues(userfields),
)

fun Store.toEntity() = StoreEntity(id, name, description, active, encodeUserfieldValues(userfields))

fun StoreEntity.toModel() = Store(id, name, description, active, decodeUserfieldValues(userfields))

fun Chore.toEntity() = ChoreEntity(
    id = id,
    name = name,
    description = description,
    active = active,
    periodType = periodType.value,
    periodDays = periodDays,
    periodInterval = periodInterval,
    weekdays = Weekday.format(weekdays),
    startDate = startDate,
    assignmentType = assignmentType.value,
    assignedUserIds = assignedUserIds.joinToString(","),
    trackDateOnly = trackDateOnly,
    rollover = rollover,
    consumeProductOnExecution = consumeProductOnExecution,
    productId = productId,
    productAmount = productAmount,
    userfields = encodeUserfieldValues(userfields),
)

fun ChoreEntity.toModel() = Chore(
    id = id,
    name = name,
    description = description,
    active = active,
    periodType = ChorePeriodType.of(periodType),
    periodDays = periodDays,
    periodInterval = periodInterval,
    weekdays = Weekday.parse(weekdays),
    startDate = startDate,
    assignmentType = ChoreAssignmentType.of(assignmentType),
    assignedUserIds = assignedUserIds.split(",").mapNotNull { it.toIntOrNull() },
    trackDateOnly = trackDateOnly,
    rollover = rollover,
    consumeProductOnExecution = consumeProductOnExecution,
    productId = productId,
    productAmount = productAmount,
    userfields = decodeUserfieldValues(userfields),
)

fun Battery.toEntity() = BatteryEntity(
    id = id,
    name = name,
    description = description,
    active = active,
    usedIn = usedIn,
    chargeIntervalDays = chargeIntervalDays,
    userfields = encodeUserfieldValues(userfields),
)

fun BatteryEntity.toModel() = Battery(
    id = id,
    name = name,
    description = description,
    active = active,
    usedIn = usedIn,
    chargeIntervalDays = chargeIntervalDays,
    userfields = decodeUserfieldValues(userfields),
)

fun TaskCategory.toEntity() =
    TaskCategoryEntity(id, name, description, active, encodeUserfieldValues(userfields))

fun TaskCategoryEntity.toModel() =
    TaskCategory(id, name, description, active, decodeUserfieldValues(userfields))

fun Userentity.toEntity() =
    UserentityEntity(id, name, caption, description, showInSidebarMenu, iconCssClass)

fun UserentityEntity.toModel() =
    Userentity(id, name, caption, description, showInSidebarMenu, iconCssClass)

fun User.toEntity() = UserEntity(id, username, displayName)

fun UserEntity.toModel() = User(id, username, displayName)

fun ProductBarcode.toEntity() = ProductBarcodeEntity(
    id = id,
    productId = productId,
    barcode = barcode,
    quId = quId,
    amount = amount,
    storeId = storeId,
    lastPrice = lastPrice,
    note = note,
    userfields = encodeUserfieldValues(userfields),
)

fun ProductBarcodeEntity.toModel() = ProductBarcode(
    id = id,
    productId = productId,
    barcode = barcode,
    quId = quId,
    amount = amount,
    storeId = storeId,
    lastPrice = lastPrice,
    note = note,
    userfields = decodeUserfieldValues(userfields),
)

fun QuantityUnitConversion.toEntity() = QuantityUnitConversionEntity(
    id = id,
    fromQuId = fromQuId,
    toQuId = toQuId,
    factor = factor,
    productId = productId,
    userfields = encodeUserfieldValues(userfields),
)

fun QuantityUnitConversionEntity.toModel() = QuantityUnitConversion(
    id = id,
    fromQuId = fromQuId,
    toQuId = toQuId,
    factor = factor,
    productId = productId,
    userfields = decodeUserfieldValues(userfields),
)

fun ResolvedQuantityUnitConversion.toEntity() = ResolvedQuantityUnitConversionEntity(
    productId = productId,
    fromQuId = fromQuId,
    toQuId = toQuId,
    factor = factor,
)

fun ResolvedQuantityUnitConversionEntity.toModel() =
    ResolvedQuantityUnitConversion(productId, fromQuId, toQuId, factor)

fun StockEntry.toEntity() = StockEntryEntity(
    id = id,
    productId = productId,
    amount = amount,
    bestBeforeDate = bestBeforeDate,
    purchasedDate = purchasedDate,
    stockId = stockId,
    price = price,
    open = open,
    openedDate = openedDate,
    locationId = locationId,
    storeId = storeId,
    note = note,
    createdTimestamp = createdTimestamp,
    userfields = encodeUserfieldValues(userfields),
)

fun StockEntryEntity.toModel() = StockEntry(
    id = id,
    productId = productId,
    amount = amount,
    bestBeforeDate = bestBeforeDate,
    purchasedDate = purchasedDate,
    stockId = stockId,
    price = price,
    open = open,
    openedDate = openedDate,
    locationId = locationId,
    storeId = storeId,
    note = note,
    createdTimestamp = createdTimestamp,
    userfields = decodeUserfieldValues(userfields),
)

fun StockLogEntry.toEntity() = StockLogEntity(
    id = id,
    productId = productId,
    amount = amount,
    transactionType = transactionType,
    createdTimestamp = createdTimestamp,
    spoiled = spoiled,
    undone = undone,
    undoneTimestamp = undoneTimestamp,
    locationId = locationId,
    userId = userId,
    correlationId = correlationId,
    stockId = stockId,
    note = note,
)

fun StockLogEntity.toModel() = StockLogEntry(
    id = id,
    productId = productId,
    amount = amount,
    transactionType = transactionType,
    createdTimestamp = createdTimestamp,
    spoiled = spoiled,
    undone = undone,
    undoneTimestamp = undoneTimestamp,
    locationId = locationId,
    userId = userId,
    correlationId = correlationId,
    stockId = stockId,
    note = note,
)

fun ShoppingList.toEntity() =
    ShoppingListEntity(id, name, description, encodeUserfieldValues(userfields))

fun ShoppingListEntity.toModel() =
    ShoppingList(id, name, description, decodeUserfieldValues(userfields))

fun ShoppingListItem.toEntity() = ShoppingListItemEntity(
    id = id,
    listId = listId,
    productId = productId,
    note = note,
    amount = amount,
    quId = quId,
    done = done,
    userfields = encodeUserfieldValues(userfields),
)

fun ShoppingListItemEntity.toModel() = ShoppingListItem(
    id = id,
    listId = listId,
    productId = productId,
    note = note,
    amount = amount,
    quId = quId,
    done = done,
    userfields = decodeUserfieldValues(userfields),
)

fun StockAmount.toEntity() = StockEntity(
    productId = productId,
    amount = amount,
    amountAggregated = amountAggregated,
    amountOpened = amountOpened,
    amountOpenedAggregated = amountOpenedAggregated,
    value = value,
    bestBeforeDate = bestBeforeDate,
    isAggregatedAmount = isAggregatedAmount,
)

fun StockEntity.toModel() = StockAmount(
    productId = productId,
    amount = amount,
    amountAggregated = amountAggregated,
    amountOpened = amountOpened,
    amountOpenedAggregated = amountOpenedAggregated,
    value = value,
    bestBeforeDate = bestBeforeDate,
    isAggregatedAmount = isAggregatedAmount,
)

fun ProductStockInfo.toEntity() = ProductStockInfoEntity(
    productId = productId,
    amountMissing = amountMissing,
    onShoppingList = onShoppingList,
    lastPurchased = lastPurchased,
    lastPrice = lastPrice,
    averagePrice = averagePrice,
)

fun ProductStockInfoEntity.toModel() = ProductStockInfo(
    productId = productId,
    amountMissing = amountMissing,
    onShoppingList = onShoppingList,
    lastPurchased = lastPurchased,
    lastPrice = lastPrice,
    averagePrice = averagePrice,
)

fun StockLocationAmount.toEntity() = StockLocationEntity(productId, locationId, amount)

fun StockLocationEntity.toModel() = StockLocationAmount(productId, locationId, amount)

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

fun Recipe.toEntity() = RecipeEntity(
    id = id,
    name = name,
    description = description,
    pictureFileName = pictureFileName,
    baseServings = baseServings,
    desiredServings = desiredServings,
    notCheckShoppingList = notCheckShoppingList,
    productId = productId,
    userfields = encodeUserfieldValues(userfields),
)

fun RecipeEntity.toModel() = Recipe(
    id = id,
    name = name,
    description = description,
    pictureFileName = pictureFileName,
    baseServings = baseServings,
    desiredServings = desiredServings,
    notCheckShoppingList = notCheckShoppingList,
    productId = productId,
    userfields = decodeUserfieldValues(userfields),
)

fun RecipeFulfillment.toEntity() = RecipeFulfillmentEntity(
    recipeId = recipeId,
    needFulfilled = needFulfilled,
    needFulfilledWithShoppingList = needFulfilledWithShoppingList,
    missingProductsCount = missingProductsCount,
    costs = costs,
    costsPerServing = costsPerServing,
    calories = calories,
    dueScore = dueScore,
    productNames = productNames,
    pricesIncomplete = pricesIncomplete,
)

fun RecipeFulfillmentEntity.toModel() = RecipeFulfillment(
    recipeId = recipeId,
    needFulfilled = needFulfilled,
    needFulfilledWithShoppingList = needFulfilledWithShoppingList,
    missingProductsCount = missingProductsCount,
    costs = costs,
    costsPerServing = costsPerServing,
    calories = calories,
    dueScore = dueScore,
    productNames = productNames,
    pricesIncomplete = pricesIncomplete,
)

fun RecipeIngredient.toEntity() = RecipeIngredientEntity(
    id = id,
    recipeId = recipeId,
    productId = productId,
    amount = amount,
    quId = quId,
    note = note,
    onlyCheckSingleUnitInStock = onlyCheckSingleUnitInStock,
    ingredientGroup = ingredientGroup,
    notCheckStockFulfillment = notCheckStockFulfillment,
    variableAmount = variableAmount,
    priceFactor = priceFactor,
    roundUp = roundUp,
)

fun RecipeIngredientEntity.toModel() = RecipeIngredient(
    id = id,
    recipeId = recipeId,
    productId = productId,
    amount = amount,
    quId = quId,
    note = note,
    onlyCheckSingleUnitInStock = onlyCheckSingleUnitInStock,
    ingredientGroup = ingredientGroup,
    notCheckStockFulfillment = notCheckStockFulfillment,
    variableAmount = variableAmount,
    priceFactor = priceFactor,
    roundUp = roundUp,
)

fun RecipeNesting.toEntity() = RecipeNestingEntity(id, recipeId, includesRecipeId, servings)

fun RecipeNestingEntity.toModel() = RecipeNesting(id, recipeId, includesRecipeId, servings)

fun ResolvedRecipeIngredient.toEntity() = ResolvedRecipeIngredientEntity(
    recipeId = recipeId,
    ingredientId = ingredientId,
    productId = productId,
    recipeAmount = recipeAmount,
    stockAmount = stockAmount,
    needFulfilled = needFulfilled,
    missingAmount = missingAmount,
    amountOnShoppingList = amountOnShoppingList,
    needFulfilledWithShoppingList = needFulfilledWithShoppingList,
    quId = quId,
    costs = costs,
    isNested = isNested,
    ingredientGroup = ingredientGroup,
    productGroup = productGroup,
    childRecipeId = childRecipeId,
    note = note,
    variableAmount = variableAmount,
    onlyCheckSingleUnitInStock = onlyCheckSingleUnitInStock,
    calories = calories,
    productActive = productActive,
    dueScore = dueScore,
    productIdEffective = productIdEffective,
)

fun ResolvedRecipeIngredientEntity.toModel() = ResolvedRecipeIngredient(
    recipeId = recipeId,
    ingredientId = ingredientId,
    productId = productId,
    recipeAmount = recipeAmount,
    stockAmount = stockAmount,
    needFulfilled = needFulfilled,
    missingAmount = missingAmount,
    amountOnShoppingList = amountOnShoppingList,
    needFulfilledWithShoppingList = needFulfilledWithShoppingList,
    quId = quId,
    costs = costs,
    isNested = isNested,
    ingredientGroup = ingredientGroup,
    productGroup = productGroup,
    childRecipeId = childRecipeId,
    note = note,
    variableAmount = variableAmount,
    onlyCheckSingleUnitInStock = onlyCheckSingleUnitInStock,
    calories = calories,
    productActive = productActive,
    dueScore = dueScore,
    productIdEffective = productIdEffective,
)
