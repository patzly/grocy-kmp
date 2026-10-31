package com.patrickzedler.grocy.feature.masterdata.impl.shoppinglist

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.input.KeyboardType
import com.patrickzedler.grocy.core.design.component.DropdownOption
import com.patrickzedler.grocy.core.design.component.NumberStepper
import com.patrickzedler.grocy.core.forms.AmountUnit
import com.patrickzedler.grocy.core.forms.DropdownItem
import com.patrickzedler.grocy.core.forms.FieldError
import com.patrickzedler.grocy.core.forms.GroupItem
import com.patrickzedler.grocy.core.forms.GroupItems
import com.patrickzedler.grocy.core.forms.ObjectField
import com.patrickzedler.grocy.core.forms.TextItem
import com.patrickzedler.grocy.core.forms.destinationAmount
import com.patrickzedler.grocy.core.forms.fieldErrorText
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.nameFor
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingList
import com.patrickzedler.grocy.core.model.text.formatDecimal
import com.patrickzedler.grocy.core.model.text.platformDecimalSeparator
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.data_amount
import com.patrickzedler.grocy.core.resources.data_note
import com.patrickzedler.grocy.core.resources.data_quantity_unit
import com.patrickzedler.grocy.core.resources.data_this_equals
import com.patrickzedler.grocy.core.resources.shopping_list_list
import com.patrickzedler.grocy.core.resources.shopping_list_product_or_note_required
import com.patrickzedler.grocy.core.resources.shopping_list_required
import org.jetbrains.compose.resources.stringResource

/**
 * The inputs both of Grocy's item forms share, the full form and its "Add to shopping list" of
 * a product: list, amount, its unit and note. The units follow the chosen product.
 *
 * @param lists the lists to choose from, by name; empty without Grocy's multiple shopping
 *   lists, everything goes to the first one then.
 */
@Stable
internal class ShoppingListItemInputs(
    val lists: List<ShoppingList>,
    initialListId: Int?,
    units: List<AmountUnit>,
    stockUnit: QuantityUnit?,
    initialQuId: Int?,
    val decimals: Int,
    initialAmount: String = "1",
    initialNote: String = "",
) {
    var listId by mutableStateOf(initialListId)
    val amount = TextFieldState(initialAmount)
    val note = TextFieldState(initialNote)

    /** The units of the chosen product, empty without product. */
    var units by mutableStateOf(units)
        private set
    var stockUnit by mutableStateOf(stockUnit)
        private set
    var quId by mutableStateOf(initialQuId)

    var isListUnmatched by mutableStateOf(false)
    var isUnitUnmatched by mutableStateOf(false)

    val hasUnmatchedChoices: Boolean
        get() = isListUnmatched || isUnitUnmatched

    /** The chosen unit with its factor, null without product. */
    val unit: AmountUnit?
        get() = units.firstOrNull { it.unit.id == quId }

    /** Another product was chosen: its units, starting with [quId]. */
    fun setUnits(units: List<AmountUnit>, stockUnit: QuantityUnit?, quId: Int?) {
        this.units = units
        this.stockUnit = stockUnit
        this.quId = quId
        isUnitUnmatched = false
    }
}

/**
 * The fields of [inputs] as one segmented group in Grocy's order: list, then [product] (Grocy's
 * product picker of the full form), amount with "This equals" in the stock unit, the unit while
 * a product is chosen, and the note.
 */
@Composable
internal fun ShoppingListItemFields(
    inputs: ShoppingListItemInputs,
    errors: Map<ObjectField, FieldError>,
    product: GroupItem? = null,
) {
    val language = LocalLocale.current.toLanguageTag()
    val stockUnit = inputs.stockUnit
    val converted = destinationAmount(inputs.amount.text.toString(), inputs.unit)
    GroupItems(
        if (inputs.lists.isNotEmpty()) {
            { index, count ->
                DropdownItem(
                    label = stringResource(Res.string.shopping_list_list),
                    options = inputs.lists.map { DropdownOption(it.id, it.name) },
                    selectedId = inputs.listId,
                    onSelectedIdChange = { inputs.listId = it },
                    onUnmatchedChange = { inputs.isListUnmatched = it },
                    error = fieldErrorText(
                        errors[ObjectField.SHOPPING_LIST],
                        Res.string.shopping_list_required,
                    ),
                    index = index,
                    count = count,
                )
            }
        } else {
            null
        },
        product,
        { index, count ->
            TextItem(
                state = inputs.amount,
                label = stringResource(Res.string.data_amount),
                error = fieldErrorText(errors[ObjectField.AMOUNT]),
                hint = if (converted != null && stockUnit != null) {
                    stringResource(
                        Res.string.data_this_equals,
                        formatDecimal(converted, inputs.decimals, platformDecimalSeparator()),
                        stockUnit.nameFor(converted, language),
                    )
                } else {
                    null
                },
                keyboardType = KeyboardType.Decimal,
                // Grocy allows zero
                stepper = NumberStepper(min = 0.0),
                index = index,
                count = count,
            )
        },
        if (inputs.units.isNotEmpty()) {
            { index, count ->
                DropdownItem(
                    label = stringResource(Res.string.data_quantity_unit),
                    options = inputs.units.map { DropdownOption(it.unit.id, it.unit.name) },
                    selectedId = inputs.quId,
                    onSelectedIdChange = { inputs.quId = it },
                    onUnmatchedChange = { inputs.isUnitUnmatched = it },
                    optional = true,
                    // Grocy disables a choice of one
                    enabled = inputs.units.size > 1,
                    index = index,
                    count = count,
                )
            }
        } else {
            null
        },
        { index, count ->
            TextItem(
                state = inputs.note,
                label = stringResource(Res.string.data_note),
                error = fieldErrorText(
                    errors[ObjectField.NOTE],
                    Res.string.shopping_list_product_or_note_required,
                ),
                multiLine = true,
                index = index,
                count = count,
            )
        },
    )
}
