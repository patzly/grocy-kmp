package com.patrickzedler.grocy.feature.masterdata.impl.shoppinglist

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.shoppinglist.ShoppingListRepository
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingList
import com.patrickzedler.grocy.feature.masterdata.impl.objects.ObjectField
import com.patrickzedler.grocy.feature.masterdata.impl.product.FieldError
import com.patrickzedler.grocy.feature.masterdata.impl.product.UserfieldError
import com.patrickzedler.grocy.feature.masterdata.impl.product.UserfieldInput
import com.patrickzedler.grocy.feature.masterdata.impl.related.AmountUnit
import com.patrickzedler.grocy.feature.masterdata.impl.related.ChangeError
import com.patrickzedler.grocy.feature.masterdata.impl.related.changeErrorOf
import com.patrickzedler.grocy.feature.masterdata.impl.related.changedValues
import com.patrickzedler.grocy.feature.masterdata.impl.related.userfieldInputs
import com.patrickzedler.grocy.feature.masterdata.impl.related.validationErrors
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** The open "Add to shopping list" form of one product: Grocy's item form for it. */
@Stable
internal class AddToShoppingListState(
    val productId: Int,
    val productName: String,
    val inputs: ShoppingListItemInputs,
    val userfields: List<UserfieldInput>,
) {
    var errors by mutableStateOf<Map<ObjectField, FieldError>>(emptyMap())
    var userfieldErrors by mutableStateOf<Map<String, UserfieldError>>(emptyMap())
    var isSaving by mutableStateOf(false)
    var error by mutableStateOf<ChangeError?>(null)

    fun values() = ShoppingListItemValues(
        listId = inputs.listId,
        productId = productId,
        amount = inputs.amount.text.toString(),
        quId = inputs.quId,
        note = inputs.note.text.toString(),
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
    private val shoppingLists: ShoppingListRepository,
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
                shoppingLists.lists.first()
            } else {
                emptyList()
            }
            val units = itemUnits(
                product = product,
                resolved = masterData.resolvedQuantityUnitConversions.first(),
                units = masterData.quantityUnits.first(),
            )
            val fields = masterData.userfields(MasterDataRepository.SHOPPING_LIST).first()
            dialog = AddToShoppingListState(
                productId = productId,
                productName = product.name,
                inputs = ShoppingListItemInputs(
                    lists = lists,
                    // Grocy opens it for its first list
                    initialListId = lists.firstOrNull { it.id == ShoppingList.DEFAULT_ID }?.id
                        ?: lists.firstOrNull()?.id
                        ?: ShoppingList.DEFAULT_ID,
                    units = units,
                    stockUnit = units.firstOrNull { it.unit.id == product.quIdStock }?.unit,
                    initialQuId = defaultItemQuId(product, units),
                    decimals = masterData.userSettings.first().stockDecimalPlacesAmounts,
                ),
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
        val inputs = form.inputs
        val values = form.values()
        form.errors = values.validate(inputs.decimals)
        form.userfieldErrors = form.userfields.validationErrors()
        val hasUnmatched = inputs.hasUnmatchedChoices || form.userfields.any { it.isUnmatched }
        if (form.errors.isNotEmpty() || form.userfieldErrors.isNotEmpty() || hasUnmatched) return

        val unit = inputs.unit
        val listId = values.listId ?: return
        form.isSaving = true
        form.error = null
        viewModelScope.launch {
            val error = changeErrorOf {
                shoppingLists.addProduct(
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
                val listName = inputs.lists.firstOrNull { it.id == listId }?.name
                    ?: shoppingLists.lists.first().firstOrNull { it.id == listId }?.name
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
