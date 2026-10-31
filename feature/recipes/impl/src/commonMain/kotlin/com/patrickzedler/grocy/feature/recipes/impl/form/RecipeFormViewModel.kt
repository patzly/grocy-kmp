package com.patrickzedler.grocy.feature.recipes.impl.form

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.masterdata.PictureChange
import com.patrickzedler.grocy.core.data.recipes.RecipesRepository
import com.patrickzedler.grocy.core.forms.AmountUnit
import com.patrickzedler.grocy.core.forms.ChangeError
import com.patrickzedler.grocy.core.forms.FieldError
import com.patrickzedler.grocy.core.forms.ObjectField
import com.patrickzedler.grocy.core.forms.ObjectFormViewModel
import com.patrickzedler.grocy.core.forms.changeErrorOf
import com.patrickzedler.grocy.core.forms.ignoringServerErrors
import com.patrickzedler.grocy.core.forms.productAmountUnits
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.ResolvedQuantityUnitConversion
import com.patrickzedler.grocy.core.model.masterdata.nameFor
import com.patrickzedler.grocy.core.model.recipes.Recipe
import com.patrickzedler.grocy.core.model.recipes.RecipeIngredient
import com.patrickzedler.grocy.core.model.recipes.RecipeNesting
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.text.formatDecimal
import com.patrickzedler.grocy.core.model.text.parseDecimal
import com.patrickzedler.grocy.core.model.text.platformDecimalSeparator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** An ingredient of the form's "Ingredients list", like a row of Grocy's table. */
internal data class IngredientItem(
    val id: Int,
    /** "2 Pieces Milk", or the variable amount instead of amount and unit. */
    val text: String,
    val group: String?,
    val note: String?,
    val isVariableAmount: Boolean,
)

/** An included recipe of the form, like a row of Grocy's table. */
internal data class NestingItem(val id: Int, val name: String, val servings: String)

/** The ingredients and included recipes of the edited recipe. */
internal data class RecipeRelated(
    val ingredients: List<IngredientItem> = emptyList(),
    val nestings: List<NestingItem> = emptyList(),
)

/** Grocy's ingredient form of a dialog; [id] is 0 for a new ingredient. */
@Stable
internal class IngredientDialogState(
    val id: Int,
    val original: RecipeIngredient?,
    initial: IngredientFormValues,
    val recipeName: String,
    val products: List<Product>,
    private val allUnits: List<QuantityUnit>,
    private val conversions: List<ResolvedQuantityUnitConversion>,
    val decimals: Int,
    val showsStockOptions: Boolean,
    val showsPriceFactor: Boolean,
) {
    var productId by mutableStateOf(initial.productId)
    var onlyCheckSingleUnitInStock by mutableStateOf(initial.onlyCheckSingleUnitInStock)
    val amount = TextFieldState(initial.amount)
    var quId by mutableStateOf(initial.quId)
    val variableAmount = TextFieldState(initial.variableAmount)
    var roundUp by mutableStateOf(initial.roundUp)
    var notCheckStockFulfillment by mutableStateOf(initial.notCheckStockFulfillment)
    val group = TextFieldState(initial.group)
    val note = TextFieldState(initial.note)
    val priceFactor = TextFieldState(initial.priceFactor)

    var isProductUnmatched by mutableStateOf(false)
    var isUnitUnmatched by mutableStateOf(false)
    var errors by mutableStateOf<Map<IngredientField, FieldError>>(emptyMap())
    var isSaving by mutableStateOf(false)
    var error by mutableStateOf<ChangeError?>(null)

    val isNew: Boolean
        get() = id == MasterDataRepository.NEW_OBJECT_ID

    /**
     * The units of the amount: those the product's stock unit converts into, every unit with
     * "Only check if any amount is in stock" like Grocy's picker then.
     */
    val units: List<AmountUnit>
        get() {
            val product = products.firstOrNull { it.id == productId } ?: return emptyList()
            return if (onlyCheckSingleUnitInStock) {
                allUnits.filter { it.active }.map { AmountUnit(it, 1.0) }
                    .sortedBy { it.unit.name.lowercase() }
            } else {
                productAmountUnits(product.id, product.quIdStock, conversions, allUnits)
            }
        }

    /** Grocy's choices for a newly chosen product: its stock unit, its fulfillment check. */
    fun selectProduct(id: Int?) {
        productId = id
        val product = products.firstOrNull { it.id == id } ?: return
        if (!onlyCheckSingleUnitInStock) quId = product.quIdStock
        if (isNew) notCheckStockFulfillment = product.notCheckStockFulfillmentForRecipes
    }

    /** Back to the product's stock unit when any unit is no longer allowed. */
    fun setOnlyCheckSingleUnit(checked: Boolean) {
        onlyCheckSingleUnitInStock = checked
        if (!checked && units.none { it.unit.id == quId }) {
            quId = products.firstOrNull { it.id == productId }?.quIdStock
        }
    }

    fun values() = IngredientFormValues(
        productId = productId,
        onlyCheckSingleUnitInStock = onlyCheckSingleUnitInStock,
        amount = amount.text.toString(),
        quId = quId,
        variableAmount = variableAmount.text.toString(),
        roundUp = roundUp,
        notCheckStockFulfillment = notCheckStockFulfillment,
        group = group.text.toString(),
        note = note.text.toString(),
        priceFactor = priceFactor.text.toString(),
    )
}

/** Grocy's included recipe form of a dialog; [id] is 0 for a new inclusion. */
@Stable
internal class NestingDialogState(
    val id: Int,
    initial: NestingFormValues,
    /** The recipes it can include, the edited one left out. */
    val recipes: List<Recipe>,
    val decimals: Int,
) {
    var recipeId by mutableStateOf(initial.recipeId)
    val servings = TextFieldState(initial.servings)
    var isRecipeUnmatched by mutableStateOf(false)
    var errors by mutableStateOf<Map<ObjectField, FieldError>>(emptyMap())
    var isSaving by mutableStateOf(false)
    var error by mutableStateOf<ChangeError?>(null)

    val isNew: Boolean
        get() = id == MasterDataRepository.NEW_OBJECT_ID

    fun values() = NestingFormValues(recipeId, servings.text.toString())
}

/** Grocy's questions before an ingredient or an included recipe is removed. */
internal sealed interface RelatedDelete {
    data class Ingredient(val id: Int, val name: String) : RelatedDelete

    data class Nesting(val id: Int, val name: String) : RelatedDelete
}

/**
 * Grocy's recipe form: name, servings, shopping list check, produced product, preparation,
 * userfields and picture are saved together; ingredients and included recipes, Grocy's tables
 * of the saved recipe, through their own dialogs right away. A created recipe becomes its edit
 * form, so they can be added, like Grocy's "Save & continue".
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class RecipeFormViewModel(
    id: Int?,
    repository: MasterDataRepository,
    private val recipes: RecipesRepository,
    private val language: String,
    private val decimalSeparator: Char = platformDecimalSeparator(),
) : ObjectFormViewModel<RecipeFormValues, RecipeFormState>(id, repository) {

    override val userfieldEntity: String = RecipesRepository.RECIPES

    var config by mutableStateOf(ServerConfig())
        private set

    var amountDecimals by mutableStateOf(DEFAULT_DECIMALS)
        private set

    var ingredientDialog by mutableStateOf<IngredientDialogState?>(null)
        private set

    var nestingDialog by mutableStateOf<NestingDialogState?>(null)
        private set

    var relatedDelete by mutableStateOf<RelatedDelete?>(null)
        private set

    /** Why removing an ingredient or included recipe failed, shown once. */
    var relatedDeleteError by mutableStateOf<ChangeError?>(null)
        private set

    val related: StateFlow<RecipeRelated> = snapshotFlow { objectId }
        .flatMapLatest { recipeId ->
            combine(
                recipes.ingredients,
                recipes.nestings,
                recipes.recipes,
                combine(repository.products, repository.quantityUnits, ::Pair),
                repository.resolvedQuantityUnitConversions,
            ) { ingredients, nestings, all, (products, units), conversions ->
                relatedOf(recipeId, ingredients, nestings, all, products, units, conversions)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, RecipeRelated())

    init {
        start()
    }

    override suspend fun createForm(id: Int?): RecipeFormState? {
        config = repository.serverConfig.first()
        amountDecimals = repository.userSettings.first().stockDecimalPlacesAmounts
        val products = repository.products.first().filter { it.active }
        if (id == null) return RecipeFormState(RecipeFormValues(), null, products)
        val recipe = recipes.recipe(id).first() ?: return null
        // A disabled product produced so far stays choosable
        val choices = products + repository.products.first()
            .filter { !it.active && it.id == recipe.productId }
        val values = recipe.toFormValues(amountDecimals, decimalSeparator)
        return RecipeFormState(values, recipe, choices)
    }

    override suspend fun userfieldValues(id: Int): Map<String, String?> =
        recipes.recipe(id).first()?.userfields.orEmpty()

    override suspend fun refreshForUnknownObject() {
        recipes.refresh()
    }

    override fun validate(form: RecipeFormState): Map<ObjectField, FieldError> =
        form.values().validate(amountDecimals)

    override suspend fun save(
        id: Int,
        form: RecipeFormState,
        userfields: Map<String, String?>,
    ): Int {
        val values = form.values()
        val savedId = recipes.saveRecipe(
            recipe = values.toRecipe(id, form.original),
            userfields = userfields,
            picture = values.picture,
        )
        // The form shows the saved recipe from now on, its new picture included
        form.original = recipes.recipe(savedId).first()
        form.picture = PictureChange.Keep
        return savedId
    }

    /** The picture the form shows: the stored one of the saved recipe. */
    suspend fun picture(fileName: String): ByteArray? =
        ignoringServerErrors { recipes.picture(fileName) }

    /** Opens Grocy's ingredient form of ingredient [id], or of a new one without. */
    fun openIngredient(id: Int? = null) {
        val recipeId = objectId.takeUnless { isNew } ?: return
        viewModelScope.launch {
            val original = id?.let { ingredientId ->
                recipes.ingredients.first().firstOrNull { it.id == ingredientId }
            }
            if (id != null && original == null) return@launch
            val products = repository.products.first()
            val units = repository.quantityUnits.first()
            val conversions = repository.resolvedQuantityUnitConversions.first()
            val choices = products.filter { it.active || it.id == original?.productId }
            val initial = original?.toFormValues(
                units = productAmountUnits(
                    productId = original.productId,
                    destinationQuId = products.firstOrNull { it.id == original.productId }
                        ?.quIdStock,
                    resolved = conversions,
                    units = units,
                ),
                decimals = amountDecimals,
                decimalSeparator = decimalSeparator,
            ) ?: IngredientFormValues()
            ingredientDialog = IngredientDialogState(
                id = original?.id ?: MasterDataRepository.NEW_OBJECT_ID,
                original = original,
                initial = initial,
                recipeName = recipes.recipe(recipeId).first()?.name.orEmpty(),
                products = choices,
                allUnits = units,
                conversions = conversions,
                decimals = amountDecimals,
                showsStockOptions = config.stock,
                showsPriceFactor = config.stockPriceTracking,
            )
        }
    }

    /** Saves the open ingredient form; it closes once the server saved the ingredient. */
    fun saveIngredient() {
        val form = ingredientDialog ?: return
        if (form.isSaving) return
        val values = form.values()
        form.errors = values.validate(form.decimals)
        if (form.errors.isNotEmpty() || form.isProductUnmatched || form.isUnitUnmatched) return
        val recipeId = objectId
        form.isSaving = true
        form.error = null
        viewModelScope.launch {
            val supportsRoundUp = recipes.ingredients.first().any { it.roundUp != null }
            val ingredient = values.toIngredient(
                id = form.id,
                recipeId = recipeId,
                units = form.units,
                original = form.original,
                supportsRoundUp = supportsRoundUp,
                decimals = form.decimals,
            )
            val error = changeErrorOf { recipes.saveIngredient(ingredient) }
            form.isSaving = false
            if (error == null) {
                if (ingredientDialog === form) ingredientDialog = null
            } else {
                form.error = error
            }
        }
    }

    fun dismissIngredient() {
        if (ingredientDialog?.isSaving != true) ingredientDialog = null
    }

    /** Opens Grocy's included recipe form of inclusion [id], or of a new one without. */
    fun openNesting(id: Int? = null) {
        val recipeId = objectId.takeUnless { isNew } ?: return
        viewModelScope.launch {
            val original = id?.let { nestingId ->
                recipes.nestings.first().firstOrNull { it.id == nestingId }
            }
            if (id != null && original == null) return@launch
            nestingDialog = NestingDialogState(
                id = original?.id ?: MasterDataRepository.NEW_OBJECT_ID,
                initial = original?.let {
                    NestingFormValues(
                        recipeId = it.includesRecipeId,
                        servings = formatDecimal(it.servings, amountDecimals, decimalSeparator),
                    )
                } ?: NestingFormValues(),
                recipes = recipes.recipes.first().filter { it.id != recipeId },
                decimals = amountDecimals,
            )
        }
    }

    /** Saves the open included recipe form; it closes once the server saved it. */
    fun saveNesting() {
        val form = nestingDialog ?: return
        if (form.isSaving) return
        val recipeId = objectId
        val values = form.values()
        form.errors = values.validate(form.decimals, recipeId)
        if (form.errors.isNotEmpty() || form.isRecipeUnmatched) return
        form.isSaving = true
        form.error = null
        viewModelScope.launch {
            val error = changeErrorOf { recipes.saveNesting(values.toNesting(form.id, recipeId)) }
            form.isSaving = false
            if (error == null) {
                if (nestingDialog === form) nestingDialog = null
            } else {
                form.error = error
            }
        }
    }

    fun dismissNesting() {
        if (nestingDialog?.isSaving != true) nestingDialog = null
    }

    /** Grocy asks before an ingredient or included recipe is removed. */
    fun requestDelete(delete: RelatedDelete) {
        relatedDelete = delete
    }

    fun dismissDelete() {
        relatedDelete = null
    }

    fun confirmDelete() {
        val delete = relatedDelete ?: return
        relatedDelete = null
        viewModelScope.launch {
            relatedDeleteError = changeErrorOf {
                when (delete) {
                    is RelatedDelete.Ingredient -> recipes.deleteIngredient(delete.id)
                    is RelatedDelete.Nesting -> recipes.deleteNesting(delete.id)
                }
            }
        }
    }

    fun onDeleteErrorShown() {
        relatedDeleteError = null
    }

    private fun relatedOf(
        recipeId: Int,
        ingredients: List<RecipeIngredient>,
        nestings: List<RecipeNesting>,
        all: List<Recipe>,
        products: List<Product>,
        units: List<QuantityUnit>,
        conversions: List<ResolvedQuantityUnitConversion>,
    ): RecipeRelated {
        if (recipeId == MasterDataRepository.NEW_OBJECT_ID) return RecipeRelated()
        val productsById = products.associateBy { it.id }
        val unitsById = units.associateBy { it.id }
        val names = all.associate { it.id to it.name }
        return RecipeRelated(
            ingredients = ingredients.filter { it.recipeId == recipeId }
                // Grocy's table groups by ingredient group, then its own order
                .sortedWith(compareBy(nullsFirst()) { it.ingredientGroup })
                .map { ingredient ->
                    val product = productsById[ingredient.productId]
                    val shown = ingredient.toFormValues(
                        units = productAmountUnits(
                            productId = ingredient.productId,
                            destinationQuId = product?.quIdStock,
                            resolved = conversions,
                            units = units,
                        ),
                        decimals = amountDecimals,
                        decimalSeparator = decimalSeparator,
                    )
                    val unit = ingredient.quId?.let { unitsById[it] }
                    val amount = ingredient.variableAmount ?: listOfNotNull(
                        shown.amount,
                        unit?.nameFor(parseDecimal(shown.amount) ?: 1.0, language),
                    ).joinToString(" ")
                    IngredientItem(
                        id = ingredient.id,
                        text = "$amount ${product?.name.orEmpty()}".trim(),
                        group = ingredient.ingredientGroup,
                        note = ingredient.note,
                        isVariableAmount = ingredient.variableAmount != null,
                    )
                },
            nestings = nestings.filter { it.recipeId == recipeId }.map { nesting ->
                NestingItem(
                    id = nesting.id,
                    name = names[nesting.includesRecipeId].orEmpty(),
                    servings = formatDecimal(nesting.servings, amountDecimals, decimalSeparator),
                )
            },
        )
    }

    private companion object {
        // Grocy's default of the user setting, until it is read
        const val DEFAULT_DECIMALS = 4
    }
}
