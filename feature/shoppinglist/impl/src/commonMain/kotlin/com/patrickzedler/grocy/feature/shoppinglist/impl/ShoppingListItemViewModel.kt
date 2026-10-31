package com.patrickzedler.grocy.feature.shoppinglist.impl

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.shoppinglist.ShoppingListRepository
import com.patrickzedler.grocy.core.forms.FieldError
import com.patrickzedler.grocy.core.forms.ObjectField
import com.patrickzedler.grocy.core.forms.ObjectFormState
import com.patrickzedler.grocy.core.forms.ObjectFormViewModel
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.ProductBarcode
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.ResolvedQuantityUnitConversion
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingList
import com.patrickzedler.grocy.core.model.text.platformDecimalSeparator
import com.patrickzedler.grocy.core.productcard.ShoppingListItemInputs
import com.patrickzedler.grocy.core.productcard.ShoppingListItemValues
import com.patrickzedler.grocy.core.productcard.defaultItemQuId
import com.patrickzedler.grocy.core.productcard.itemAmountText
import com.patrickzedler.grocy.core.productcard.itemUnits
import com.patrickzedler.grocy.core.productcard.productIdForCode
import com.patrickzedler.grocy.core.productcard.toItem
import com.patrickzedler.grocy.core.productcard.validate
import kotlinx.coroutines.flow.first

/**
 * Grocy's shopping list item form: list, product (also scanned), amount in one of the
 * product's units, note. Choosing another product starts with its purchase unit and keeps the
 * number, like Grocy.
 *
 * @param products the active products by name, the product picker's choices.
 */
@Stable
internal class ShoppingListItemFormState(
    val inputs: ShoppingListItemInputs,
    initialProductId: Int?,
    val products: List<Product>,
    private val barcodes: List<ProductBarcode>,
    private val resolved: List<ResolvedQuantityUnitConversion>,
    private val quantityUnits: List<QuantityUnit>,
    /** Whether the item is done, which the form never changes. */
    val done: Boolean,
    focusesProduct: Boolean = false,
) : ObjectFormState<ShoppingListItemValues> {

    /**
     * Whether the product field takes the focus once shown, so typing starts right away like in
     * Grocy's form of a new item; once only, not again when it scrolls back into view.
     */
    var focusesProduct by mutableStateOf(focusesProduct)
        private set

    fun onProductFocused() {
        focusesProduct = false
    }

    var productId by mutableStateOf(initialProductId)
        private set

    var isProductUnmatched by mutableStateOf(false)

    override val hasUnmatchedChoices: Boolean
        get() = inputs.hasUnmatchedChoices || isProductUnmatched

    override fun values() = ShoppingListItemValues(
        listId = inputs.listId,
        productId = productId,
        amount = inputs.amount.text.toString(),
        quId = inputs.quId,
        note = inputs.note.text.toString(),
    )

    /** Chooses product [id] (none for null) with its units, starting with its purchase unit. */
    fun selectProduct(id: Int?) {
        if (id == productId) return
        productId = id
        val product = products.firstOrNull { it.id == id }
        val units = itemUnits(product, resolved, quantityUnits)
        inputs.setUnits(
            units = units,
            stockUnit = units.firstOrNull { it.unit.id == product?.quIdStock }?.unit,
            quId = defaultItemQuId(product, units),
        )
        if (product != null && inputs.amount.text.isBlank()) {
            inputs.amount.setTextAndPlaceCursorAtEnd("1")
        }
    }

    /** The product of a scanned grocycode or barcode, null if none of the choices. */
    fun productForCode(code: String): Product? {
        val id = productIdForCode(code, barcodes)
        return products.firstOrNull { it.id == id }
    }
}

/**
 * Grocy's item form: edits item [id] or creates one on list [presetListId] (Grocy's `list`),
 * with product [presetProductId] preselected if given. Saving is online only; like Grocy's
 * dialog the form closes once saved, also for a created item.
 */
internal class ShoppingListItemViewModel(
    id: Int?,
    private val presetListId: Int?,
    private val presetProductId: Int?,
    masterData: MasterDataRepository,
    private val shoppingLists: ShoppingListRepository,
    private val decimalSeparator: Char = platformDecimalSeparator(),
) : ObjectFormViewModel<ShoppingListItemValues, ShoppingListItemFormState>(id, masterData) {

    override val userfieldEntity = MasterDataRepository.SHOPPING_LIST

    override val closesWhenCreated = true

    init {
        start()
    }

    override suspend fun createForm(id: Int?): ShoppingListItemFormState? {
        val item = id?.let { shoppingLists.item(it).first() ?: return null }
        val config = repository.serverConfig.first()
        val allLists = shoppingLists.lists.first()
        val products = repository.products.first().filter { it.active }
            .sortedBy { it.name.lowercase() }
        val resolved = repository.resolvedQuantityUnitConversions.first()
        val quantityUnits = repository.quantityUnits.first()
        val decimals = repository.userSettings.first().stockDecimalPlacesAmounts
        val productId = item?.productId ?: presetProductId
        val product = products.firstOrNull { it.id == productId }
        val units = itemUnits(product, resolved, quantityUnits)
        val listId = when {
            item != null -> item.listId
            !config.shoppingListMultipleLists -> ShoppingList.DEFAULT_ID
            // Grocy's first list by name, unless the page names one
            else -> allLists.firstOrNull { it.id == presetListId }?.id
                ?: allLists.firstOrNull()?.id
                ?: ShoppingList.DEFAULT_ID
        }
        val inputs = ShoppingListItemInputs(
            lists = if (config.shoppingListMultipleLists) allLists else emptyList(),
            initialListId = listId,
            units = units,
            stockUnit = units.firstOrNull { it.unit.id == product?.quIdStock }?.unit,
            initialQuId = if (item != null) {
                item.quId?.takeIf { quId -> units.any { it.unit.id == quId } }
                    ?: defaultItemQuId(product, units)
            } else {
                defaultItemQuId(product, units)
            },
            decimals = decimals,
            initialAmount = item?.let { itemAmountText(it, units, decimals, decimalSeparator) }
                ?: "1",
            initialNote = item?.note.orEmpty(),
        )
        return ShoppingListItemFormState(
            inputs = inputs,
            initialProductId = product?.id,
            products = products,
            barcodes = repository.productBarcodes.first(),
            resolved = resolved,
            quantityUnits = quantityUnits,
            done = item?.done ?: false,
            // A preset product is chosen already
            focusesProduct = item == null && product == null,
        )
    }

    override suspend fun userfieldValues(id: Int): Map<String, String?> =
        shoppingLists.item(id).first()?.userfields.orEmpty()

    override suspend fun refreshForUnknownObject() {
        shoppingLists.refresh()
    }

    override fun validate(form: ShoppingListItemFormState): Map<ObjectField, FieldError> =
        form.values().validate(form.inputs.decimals)

    override suspend fun save(
        id: Int,
        form: ShoppingListItemFormState,
        userfields: Map<String, String?>,
    ): Int = shoppingLists.saveItem(
        item = form.values().toItem(id, form.done, form.inputs.unit),
        userfields = userfields,
    )
}
