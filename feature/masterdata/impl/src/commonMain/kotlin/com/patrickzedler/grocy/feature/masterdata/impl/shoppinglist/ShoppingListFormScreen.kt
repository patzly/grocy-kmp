package com.patrickzedler.grocy.feature.masterdata.impl.shoppinglist

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.shoppinglist.ShoppingListRepository
import com.patrickzedler.grocy.core.design.component.segmentedGroup
import com.patrickzedler.grocy.core.forms.FieldError
import com.patrickzedler.grocy.core.forms.FormFrame
import com.patrickzedler.grocy.core.forms.ObjectField
import com.patrickzedler.grocy.core.forms.ObjectFormScreen
import com.patrickzedler.grocy.core.forms.ObjectFormState
import com.patrickzedler.grocy.core.forms.ObjectFormViewModel
import com.patrickzedler.grocy.core.forms.TextItem
import com.patrickzedler.grocy.core.forms.fieldErrorText
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingList
import com.patrickzedler.grocy.core.productcard.validate
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.product_name
import com.patrickzedler.grocy.core.resources.product_name_required
import com.patrickzedler.grocy.core.resources.shopping_list_create
import com.patrickzedler.grocy.core.resources.shopping_list_edit
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.stringResource

/** Grocy's shopping list form: only the name, the notes belong to the list's page. */
@Stable
internal class ShoppingListFormState(name: String, val description: String?) :
    ObjectFormState<String> {
    val name = TextFieldState(name)

    override fun values(): String = name.text.toString()
}

/** Grocy's "A name is required". */
internal fun validateShoppingListName(name: String): Map<ObjectField, FieldError> =
    if (name.isBlank()) mapOf(ObjectField.NAME to FieldError.Required) else emptyMap()

/**
 * Creates or renames the shopping list [id] with its userfields. Online only; once saved the
 * form closes like Grocy's dialog, and a created list is the one the shopping list shows next.
 */
internal class ShoppingListFormViewModel(
    id: Int?,
    masterData: MasterDataRepository,
    private val shoppingLists: ShoppingListRepository,
) : ObjectFormViewModel<String, ShoppingListFormState>(id, masterData) {

    override val userfieldEntity = ShoppingListRepository.SHOPPING_LISTS

    override val closesWhenCreated = true

    init {
        start()
    }

    override suspend fun createForm(id: Int?): ShoppingListFormState? {
        if (id == null) return ShoppingListFormState(name = "", description = null)
        val list = shoppingLists.list(id).first() ?: return null
        return ShoppingListFormState(list.name, list.description)
    }

    override suspend fun userfieldValues(id: Int): Map<String, String?> =
        shoppingLists.list(id).first()?.userfields.orEmpty()

    override suspend fun refreshForUnknownObject() {
        shoppingLists.refresh()
    }

    override fun validate(form: ShoppingListFormState) = validateShoppingListName(form.values())

    override suspend fun save(
        id: Int,
        form: ShoppingListFormState,
        userfields: Map<String, String?>,
    ): Int {
        val wasNew = isNew
        val savedId = shoppingLists.saveList(
            ShoppingList(id, form.values().trim(), form.description),
            userfields,
        )
        // Grocy switches to a new list
        if (wasNew) shoppingLists.selectList(savedId)
        return savedId
    }
}

/** Grocy's "Create shopping list" / "Edit shopping list". */
@Composable
internal fun ShoppingListFormScreen(
    viewModel: ShoppingListFormViewModel,
    frame: FormFrame,
    modifier: Modifier = Modifier,
) {
    ObjectFormScreen(
        title = if (viewModel.isNew) {
            stringResource(Res.string.shopping_list_create)
        } else {
            stringResource(Res.string.shopping_list_edit)
        },
        viewModel = viewModel,
        frame = frame,
        modifier = modifier,
    ) { form ->
        segmentedGroup(key = "list") {
            TextItem(
                state = form.name,
                label = stringResource(Res.string.product_name),
                error = fieldErrorText(
                    viewModel.errors[ObjectField.NAME],
                    Res.string.product_name_required,
                ),
                index = 0,
                count = 1,
            )
        }
    }
}
