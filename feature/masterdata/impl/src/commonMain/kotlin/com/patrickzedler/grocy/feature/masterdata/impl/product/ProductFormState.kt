package com.patrickzedler.grocy.feature.masterdata.impl.product

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.patrickzedler.grocy.core.model.masterdata.DueType
import com.patrickzedler.grocy.core.model.masterdata.PurchasePriceType
import com.patrickzedler.grocy.core.model.masterdata.StockLabelType

/** The pickers of the product form. */
internal enum class ProductChoice {
    PARENT,
    LOCATION,
    CONSUME_LOCATION,
    STORE,
    PRODUCT_GROUP,
    QU_STOCK,
    QU_PURCHASE,
    QU_CONSUME,
    QU_PRICE,
}

/** Editable state of the product form, starting from [initial]. */
@Stable
internal class ProductFormState(initial: ProductFormValues) {
    val name = TextFieldState(initial.name)
    var active by mutableStateOf(initial.active)
    var parentProductId by mutableStateOf(initial.parentProductId)
    val description = TextFieldState(initial.description)
    var locationId by mutableStateOf(initial.locationId)
    var defaultConsumeLocationId by mutableStateOf(initial.defaultConsumeLocationId)
    var moveOnOpen by mutableStateOf(initial.moveOnOpen)
    var storeId by mutableStateOf(initial.storeId)
    val minStockAmount = TextFieldState(initial.minStockAmount)
    var cumulateMinStockAmountOfSubProducts by mutableStateOf(
        initial.cumulateMinStockAmountOfSubProducts,
    )
    var treatOpenedAsOutOfStock by mutableStateOf(initial.treatOpenedAsOutOfStock)
    var dueType by mutableStateOf(initial.dueType)
    val defaultDueDays = TextFieldState(initial.defaultDueDays)
    val defaultDueDaysAfterOpen = TextFieldState(initial.defaultDueDaysAfterOpen)
    val defaultDueDaysAfterFreezing = TextFieldState(initial.defaultDueDaysAfterFreezing)
    val defaultDueDaysAfterThawing = TextFieldState(initial.defaultDueDaysAfterThawing)
    var shouldNotBeFrozen by mutableStateOf(initial.shouldNotBeFrozen)
    var productGroupId by mutableStateOf(initial.productGroupId)
    var quIdStock by mutableStateOf(initial.quIdStock)
        private set
    var quIdPurchase by mutableStateOf(initial.quIdPurchase)
    var quIdConsume by mutableStateOf(initial.quIdConsume)
    var quIdPrice by mutableStateOf(initial.quIdPrice)
    var enableTareWeightHandling by mutableStateOf(initial.enableTareWeightHandling)
    val tareWeight = TextFieldState(initial.tareWeight)
    var notCheckStockFulfillmentForRecipes by mutableStateOf(
        initial.notCheckStockFulfillmentForRecipes,
    )
    val calories = TextFieldState(initial.calories)
    val quickConsumeAmount = TextFieldState(initial.quickConsumeAmount)
    val quickOpenAmount = TextFieldState(initial.quickOpenAmount)
    var defaultStockLabelType: StockLabelType by mutableStateOf(initial.defaultStockLabelType)
    var autoReprintStockLabel by mutableStateOf(initial.autoReprintStockLabel)
    var defaultPurchasePriceType: PurchasePriceType by mutableStateOf(
        initial.defaultPurchasePriceType,
    )
    var disableOpen by mutableStateOf(initial.disableOpen)
    var hideOnStockOverview by mutableStateOf(initial.hideOnStockOverview)
    var noOwnStock by mutableStateOf(initial.noOwnStock)

    /** Pickers whose text names none of their options; the form is not saved while any do. */
    var unmatchedChoices by mutableStateOf(emptySet<ProductChoice>())
        private set

    fun setUnmatched(choice: ProductChoice, unmatched: Boolean) {
        unmatchedChoices = if (unmatched) unmatchedChoices + choice else unmatchedChoices - choice
    }

    /** The other units follow the stock unit unless the user chose them, like in Grocy. */
    fun changeStockUnit(id: Int?) {
        val previous = quIdStock
        quIdStock = id
        quIdPurchase = linkedQuantityUnit(quIdPurchase, previous, id)
        quIdConsume = linkedQuantityUnit(quIdConsume, previous, id)
        quIdPrice = linkedQuantityUnit(quIdPrice, previous, id)
    }

    fun values() = ProductFormValues(
        name = name.text.toString(),
        active = active,
        parentProductId = parentProductId,
        description = description.text.toString(),
        locationId = locationId,
        defaultConsumeLocationId = defaultConsumeLocationId,
        moveOnOpen = moveOnOpen,
        storeId = storeId,
        minStockAmount = minStockAmount.text.toString(),
        cumulateMinStockAmountOfSubProducts = cumulateMinStockAmountOfSubProducts,
        treatOpenedAsOutOfStock = treatOpenedAsOutOfStock,
        dueType = dueType,
        defaultDueDays = defaultDueDays.text.toString(),
        defaultDueDaysAfterOpen = defaultDueDaysAfterOpen.text.toString(),
        defaultDueDaysAfterFreezing = defaultDueDaysAfterFreezing.text.toString(),
        defaultDueDaysAfterThawing = defaultDueDaysAfterThawing.text.toString(),
        shouldNotBeFrozen = shouldNotBeFrozen,
        productGroupId = productGroupId,
        quIdStock = quIdStock,
        quIdPurchase = quIdPurchase,
        quIdConsume = quIdConsume,
        quIdPrice = quIdPrice,
        enableTareWeightHandling = enableTareWeightHandling,
        tareWeight = tareWeight.text.toString(),
        notCheckStockFulfillmentForRecipes = notCheckStockFulfillmentForRecipes,
        calories = calories.text.toString(),
        quickConsumeAmount = quickConsumeAmount.text.toString(),
        quickOpenAmount = quickOpenAmount.text.toString(),
        defaultStockLabelType = defaultStockLabelType,
        autoReprintStockLabel = autoReprintStockLabel,
        defaultPurchasePriceType = defaultPurchasePriceType,
        disableOpen = disableOpen,
        hideOnStockOverview = hideOnStockOverview,
        noOwnStock = noOwnStock,
    )
}
