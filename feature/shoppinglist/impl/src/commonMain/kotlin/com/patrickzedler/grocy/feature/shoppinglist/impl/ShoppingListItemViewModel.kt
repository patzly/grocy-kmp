package com.patrickzedler.grocy.feature.shoppinglist.impl

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.shoppinglist.ShoppingListRepository
import com.patrickzedler.grocy.core.forms.ExternalLookupResult
import com.patrickzedler.grocy.core.forms.FieldError
import com.patrickzedler.grocy.core.forms.FormFocusTarget
import com.patrickzedler.grocy.core.forms.ObjectField
import com.patrickzedler.grocy.core.forms.ObjectFormState
import com.patrickzedler.grocy.core.forms.ObjectFormViewModel
import com.patrickzedler.grocy.core.forms.ScannerFieldState
import com.patrickzedler.grocy.core.forms.UnknownProductFlow
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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

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
    products: List<Product>,
    private var allProducts: List<Product>,
    barcodes: List<ProductBarcode>,
    resolved: List<ResolvedQuantityUnitConversion>,
    private val quantityUnits: List<QuantityUnit>,
    /** Whether the item is done, which the form never changes. */
    val done: Boolean,
    /** The product's scanner and where the focus goes around it. */
    val scanner: ScannerFieldState = ScannerFieldState(),
) : ObjectFormState<ShoppingListItemValues> {

    var productId by mutableStateOf(initialProductId)
        private set

    var products by mutableStateOf(products)
        private set

    private var barcodes = barcodes
    private var resolved = resolved

    /** Grocy's "Create or assign product" for what names no product. */
    val unknownProduct = UnknownProductFlow()

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
        if (id != null) unknownProduct.stopWaiting()
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
        if (product != null) scanner.onValueChosen()
    }

    /** A scanned code: its product is chosen and the amount is next, else Grocy's question. */
    fun onScanned(code: String) {
        val product = productForCode(code)
        if (product == null) {
            unknownProduct.onTextWithoutProduct(code, false, allProducts, barcodes)
            // A code the form does not offer keeps scanning, a question needs its answer
            if (unknownProduct.input != null) scanner.closeWithoutFocus()
            return
        }
        unknownProduct.stopWaiting()
        selectProduct(product.id)
        scanner.onScanned()
        scanner.requestFocus(FormFocusTarget.NEXT_FIELD)
    }

    /**
     * Text submitted in the product field, which a barcode reader types like a keyboard, or left
     * standing in it: a code chooses its product, other text asks Grocy's question unless a
     * suggestion would take it; so does the scanner opening, which only takes the focus away.
     */
    fun onTextSubmitted(text: String, hasSuggestion: Boolean): Boolean {
        if (scanner.isOpen) return false
        val product = productForCode(text)
        if (product == null) {
            val used = unknownProduct
                .onTextWithoutProduct(text, hasSuggestion, allProducts, barcodes)
            // Grocy's error leaves the focus in the field for the next try
            if (unknownProduct.isNotOffered) scanner.requestFocus(FormFocusTarget.CODE_FIELD)
            return used
        }
        unknownProduct.stopWaiting()
        selectProduct(product.id)
        scanner.requestFocus(FormFocusTarget.NEXT_FIELD)
        return true
    }

    fun dismissUnknownProduct() {
        unknownProduct.dismiss()
        scanner.onFormReset()
    }

    /** Grocy's "Add as barcode to existing product": the user chooses the product now. */
    fun onBarcodeAssigned() {
        selectProduct(null)
        scanner.requestFocus(FormFocusTarget.CODE_FIELD)
    }

    /** Grocy's camera button in the question: on with the next code. */
    fun scanAgain() {
        selectProduct(null)
        if (!scanner.isOpen) scanner.toggle()
    }

    /** A product created meanwhile becomes a choice; the one the question waited for is chosen. */
    fun updateChoices(
        allProducts: List<Product>,
        products: List<Product>,
        barcodes: List<ProductBarcode>,
        resolved: List<ResolvedQuantityUnitConversion>,
    ) {
        this.allProducts = allProducts
        this.products = products
        this.barcodes = barcodes
        this.resolved = resolved
        unknownProduct.takeCreatedProduct(products, barcodes)?.let { created ->
            selectProduct(created.id)
            scanner.requestFocus(FormFocusTarget.NEXT_FIELD)
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
    private val scanner: (hasValue: Boolean) -> ScannerFieldState = {
        ScannerFieldState(hasValue = it)
    },
) : ObjectFormViewModel<ShoppingListItemValues, ShoppingListItemFormState>(id, masterData) {

    /** The server's barcode plugin, which Grocy's question offers. */
    val barcodeLookupPlugin: Flow<String?> = repository.serverConfig.map { it.barcodeLookupPlugin }

    private val externalLookups = Channel<ExternalLookupResult>(Channel.BUFFERED)

    /** What Grocy's "External barcode lookup" found, for the screen to show. */
    val externalLookupResults: Flow<ExternalLookupResult> = externalLookups.receiveAsFlow()

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
        val form = ShoppingListItemFormState(
            inputs = inputs,
            initialProductId = product?.id,
            products = products,
            allProducts = repository.products.first(),
            barcodes = repository.productBarcodes.first(),
            resolved = resolved,
            quantityUnits = quantityUnits,
            done = item?.done ?: false,
            // An edited item or a preset product goes on with the amount like Grocy's form
            scanner = scanner(item != null || product != null).also {
                if (item != null || product != null) {
                    it.requestFocus(FormFocusTarget.NEXT_FIELD)
                }
            },
        )
        viewModelScope.launch {
            combine(
                repository.products,
                repository.productBarcodes,
                repository.resolvedQuantityUnitConversions,
            ) { allProducts, barcodes, conversions ->
                form.updateChoices(
                    allProducts,
                    allProducts.filter { it.active }.sortedBy { it.name.lowercase() },
                    barcodes,
                    conversions,
                )
            }.collect()
        }
        return form
    }

    /** Grocy's "External barcode lookup"; a found product is shown for checking, then chosen. */
    fun lookUpExternally(form: ShoppingListItemFormState) {
        viewModelScope.launch {
            externalLookups.send(form.unknownProduct.lookUpExternally(repository))
        }
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
    ): Int {
        form.productId?.let { form.unknownProduct.savePendingBarcode(it, repository) }
        return shoppingLists.saveItem(
            item = form.values().toItem(id, form.done, form.inputs.unit),
            userfields = userfields,
        )
    }
}
