package com.patrickzedler.grocy.feature.masterdata.impl.shoppinglist

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.stock.StockRepository
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingList
import com.patrickzedler.grocy.feature.masterdata.impl.product.FieldError
import com.patrickzedler.grocy.feature.masterdata.impl.product.UserfieldError
import com.patrickzedler.grocy.feature.masterdata.impl.product.UserfieldInput
import com.patrickzedler.grocy.feature.masterdata.impl.related.AmountUnit
import com.patrickzedler.grocy.feature.masterdata.impl.related.ChangeError
import com.patrickzedler.grocy.feature.masterdata.impl.related.changeErrorOf
import com.patrickzedler.grocy.feature.masterdata.impl.related.changedValues
import com.patrickzedler.grocy.feature.masterdata.impl.related.productAmountUnits
import com.patrickzedler.grocy.feature.masterdata.impl.related.userfieldInputs
import com.patrickzedler.grocy.feature.masterdata.impl.related.validationErrors
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** The open "Add to shopping list" form of one product. */
@Stable
internal class AddToShoppingListState(
    val productId: Int,
    val productName: String,
    /** Empty without Grocy's multiple shopping lists: everything goes to the first one. */
    val lists: List<ShoppingList>,
    initialListId: Int?,
    /** The units of the amount, converted from the product's [stockUnit]. */
    val units: List<AmountUnit>,
    val stockUnit: QuantityUnit?,
    initialQuId: Int?,
    val decimals: Int,
    val userfields: List<UserfieldInput>,
) {
    var listId by mutableStateOf(initialListId)
    val amount = TextFieldState("1")
    var quId by mutableStateOf(initialQuId)
    val note = TextFieldState()

    var isListUnmatched by mutableStateOf(false)
    var isUnitUnmatched by mutableStateOf(false)

    var errors by mutableStateOf<Map<ShoppingListItemField, FieldError>>(emptyMap())
    var userfieldErrors by mutableStateOf<Map<String, UserfieldError>>(emptyMap())
    var isSaving by mutableStateOf(false)
    var error by mutableStateOf<ChangeError?>(null)

    fun values() = ShoppingListItemValues(
        listId = listId,
        amount = amount.text.toString(),
        quId = quId,
        note = note.text.toString(),
    )
}

/** What was put on which list, for Grocy's message "Added … to the shopping list …". */
internal data class AddedToShoppingList(
    val amount: String,
    val unit: AmountUnit?,
    val productName: String,
    val listName: String,
)

/**
 * Grocy's "Add to shopping list" of a product as dialog ([dialog]): the amount in one of its
 * units, added to an item of the product on that list. Online only.
 */
internal class AddToShoppingListViewModel(
    private val masterData: MasterDataRepository,
    private val stock: StockRepository,
) : ViewModel() {

    var dialog by mutableStateOf<AddToShoppingListState?>(null)
        private set

    private val addedChannel = Channel<AddedToShoppingList>(Channel.BUFFERED)
    val added: Flow<AddedToShoppingList> = addedChannel.receiveAsFlow()

    fun open(productId: Int) {
        viewModelScope.launch {
            val product = masterData.product(productId).first() ?: return@launch
            val config = masterData.serverConfig.first()
            val lists = if (config.shoppingListMultipleLists) {
                masterData.shoppingLists.first()
            } else {
                emptyList()
            }
            val units = productAmountUnits(
                productId = productId,
                destinationQuId = product.quIdStock,
                resolved = masterData.resolvedQuantityUnitConversions.first(),
                units = masterData.quantityUnits.first(),
            )
            val fields = masterData.userfields(MasterDataRepository.SHOPPING_LIST).first()
            dialog = AddToShoppingListState(
                productId = productId,
                productName = product.name,
                lists = lists,
                // Grocy opens it for its first list
                initialListId = lists.firstOrNull { it.id == ShoppingList.DEFAULT_ID }?.id
                    ?: lists.firstOrNull()?.id
                    ?: ShoppingList.DEFAULT_ID,
                units = units,
                stockUnit = units.firstOrNull { it.unit.id == product.quIdStock }?.unit,
                // The purchase unit if the product converts into it, like Grocy
                initialQuId = units.firstOrNull { it.unit.id == product.quIdPurchase }?.unit?.id
                    ?: product.quIdStock,
                decimals = masterData.userSettings.first().stockDecimalPlacesAmounts,
                userfields = userfieldInputs(fields, values = null),
            )
        }
    }

    fun dismiss() {
        if (dialog?.isSaving != true) dialog = null
    }

    fun save() {
        val form = dialog ?: return
        if (form.isSaving) return
        val values = form.values()
        form.errors = values.validate(form.decimals)
        form.userfieldErrors = form.userfields.validationErrors()
        val hasUnmatched = form.isListUnmatched || form.isUnitUnmatched ||
            form.userfields.any { it.isUnmatched }
        if (form.errors.isNotEmpty() || form.userfieldErrors.isNotEmpty() || hasUnmatched) return

        val unit = form.units.firstOrNull { it.unit.id == values.quId }
        val listId = values.listId ?: return
        form.isSaving = true
        form.error = null
        viewModelScope.launch {
            val error = changeErrorOf {
                stock.addToShoppingList(
                    productId = form.productId,
                    listId = listId,
                    amount = values.stockAmount(unit),
                    quId = values.quId,
                    note = values.note,
                    userfields = form.userfields.changedValues(),
                )
            }
            form.isSaving = false
            if (error == null) {
                if (dialog === form) dialog = null
                val listName = form.lists.firstOrNull { it.id == listId }?.name
                    ?: masterData.shoppingLists.first().firstOrNull { it.id == listId }?.name
                    .orEmpty()
                addedChannel.send(
                    AddedToShoppingList(values.amount.trim(), unit, form.productName, listName),
                )
            } else {
                form.error = error
            }
        }
    }
}
