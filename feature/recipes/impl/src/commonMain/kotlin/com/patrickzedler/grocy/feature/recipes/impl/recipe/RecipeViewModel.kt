package com.patrickzedler.grocy.feature.recipes.impl.recipe

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.recipes.RecipesRepository
import com.patrickzedler.grocy.core.data.shoppinglist.ShoppingListRepository
import com.patrickzedler.grocy.core.data.util.RefreshController
import com.patrickzedler.grocy.core.data.util.RefreshState
import com.patrickzedler.grocy.core.forms.ChangeError
import com.patrickzedler.grocy.core.forms.changeErrorOf
import com.patrickzedler.grocy.core.forms.ignoringServerErrors
import com.patrickzedler.grocy.core.forms.minimumAmount
import com.patrickzedler.grocy.core.forms.numberError
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.ResolvedQuantityUnitConversion
import com.patrickzedler.grocy.core.model.recipes.Recipe
import com.patrickzedler.grocy.core.model.recipes.RecipeFulfillment
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.server.UserSettings
import com.patrickzedler.grocy.core.model.text.formatDecimal
import com.patrickzedler.grocy.core.model.text.parseDecimal
import com.patrickzedler.grocy.core.model.text.platformDecimalSeparator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Grocy's recipe card, built from the database. */
internal data class RecipeUiState(
    val recipe: Recipe,
    /** Null until the server computed it. */
    val fulfillment: RecipeFulfillment?,
    val sections: List<RecipeSection>,
    val missingProducts: List<MissingProduct>,
    val config: ServerConfig,
    val priceDecimals: Int,
    val amountDecimals: Int,
    /** Grocy's user setting: a checkbox per ingredient to mark it as done while cooking. */
    val showsIngredientCheckboxes: Boolean,
)

/** The questions of the card before its actions run. */
internal sealed interface RecipeDialog {
    data object Consume : RecipeDialog

    /** The missing products, all checked first; unchecked ones stay off the shopping list. */
    data class AddMissing(val products: List<MissingProduct>) : RecipeDialog
}

/** What became of the card's actions, shown once. */
internal sealed interface RecipeEvent {
    data class Consumed(val recipeName: String) : RecipeEvent

    data object MissingAdded : RecipeEvent

    data class ConsumeFailed(val error: ChangeError) : RecipeEvent

    data class AddMissingFailed(val error: ChangeError) : RecipeEvent

    data class ServingsFailed(val error: ChangeError) : RecipeEvent
}

/**
 * Grocy's recipe card of [recipeId]: shows the stored recipe at once and downloads it with the
 * master data in the background. The desired servings save themselves like Grocy's field does
 * once typing pauses; the server then scales the ingredients anew. Consuming and putting the
 * missing products on the shopping list ask first and are online only.
 */
@OptIn(FlowPreview::class)
internal class RecipeViewModel(
    val recipeId: Int,
    private val recipes: RecipesRepository,
    private val masterData: MasterDataRepository,
    private val shoppingLists: ShoppingListRepository,
    language: String,
    private val decimalSeparator: Char = platformDecimalSeparator(),
    computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    /** Grocy's "Desired servings" of the card, filled once the recipe is read. */
    val servings = TextFieldState()

    var dialog by mutableStateOf<RecipeDialog?>(null)
        private set

    var isRunning by mutableStateOf(false)
        private set

    /**
     * The ingredients marked as done while cooking, Grocy's checkboxes: kept while the card is
     * shown, never saved, like Grocy forgets them on a reload.
     */
    var doneIngredients by mutableStateOf(emptySet<Int>())
        private set

    private val eventChannel = Channel<RecipeEvent>(Channel.BUFFERED)
    val events: Flow<RecipeEvent> = eventChannel.receiveAsFlow()

    private val refreshController = RefreshController(viewModelScope) {
        coroutineScope {
            launch { masterData.refresh() }
            launch { recipes.refresh() }
        }
    }
    val refresh: StateFlow<RefreshState> = refreshController.state

    private val masterDataPart = combine(
        masterData.products,
        masterData.quantityUnits,
        masterData.resolvedQuantityUnitConversions,
        masterData.serverConfig,
        masterData.userSettings,
    ) { products, units, conversions, config, settings ->
        MasterDataPart(products, units, conversions, config, settings)
    }

    val state: StateFlow<RecipeUiState?> = combine(
        recipes.recipe(recipeId),
        recipes.recipes,
        recipes.fulfillment,
        recipes.resolvedIngredients(recipeId),
        masterDataPart,
    ) { recipe, all, fulfillment, resolved, part ->
        recipe ?: return@combine null
        val format = IngredientFormat(
            products = part.products,
            units = part.units,
            conversions = part.conversions,
            decimals = part.settings.stockDecimalPlacesAmounts,
            decimalSeparator = decimalSeparator,
            language = language,
        )
        RecipeUiState(
            recipe = recipe,
            fulfillment = fulfillment.firstOrNull { it.recipeId == recipeId },
            sections = buildRecipeSections(
                recipe = recipe,
                recipes = all,
                resolved = resolved,
                format = format,
                groupByProductGroup = part.settings.recipeIngredientsGroupByProductGroup,
                checksStock = part.config.stock,
            ),
            missingProducts = missingProducts(resolved, part.products),
            config = part.config,
            priceDecimals = part.settings.stockDecimalPlacesPricesDisplay,
            amountDecimals = part.settings.stockDecimalPlacesAmounts,
            showsIngredientCheckboxes = part.settings.recipesShowIngredientCheckbox,
        )
    }
        .flowOn(computeDispatcher)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        refresh()
        viewModelScope.launch {
            val recipe = recipes.recipe(recipeId).filterNotNull().first()
            val decimals = masterData.userSettings.first().stockDecimalPlacesAmounts
            servings.setTextAndPlaceCursorAtEnd(
                formatDecimal(recipe.desiredServings, decimals, decimalSeparator),
            )
            saveServingsWhileTyping(decimals)
        }
    }

    fun refresh() = refreshController.refresh()

    fun setIngredientDone(ingredientId: Int, done: Boolean) {
        doneIngredients = if (done) {
            doneIngredients + ingredientId
        } else {
            doneIngredients - ingredientId
        }
    }

    fun onErrorShown() = refreshController.onErrorShown()

    // Like Grocy's field: every change that is a valid number saves once typing pauses
    private suspend fun saveServingsWhileTyping(decimals: Int) {
        snapshotFlow { servings.text.toString() }
            .drop(1)
            .distinctUntilChanged()
            .debounce(SERVINGS_DELAY_MILLIS)
            .collect { text ->
                val value = parseDecimal(text) ?: return@collect
                if (numberError(text, minimumAmount(decimals), decimals, required = true) != null) {
                    return@collect
                }
                val current = recipes.recipe(recipeId).first()?.desiredServings
                if (value == current) return@collect
                changeErrorOf { recipes.setDesiredServings(recipeId, value) }?.let {
                    eventChannel.send(RecipeEvent.ServingsFailed(it))
                }
            }
    }

    /** Grocy's "Consume all ingredients needed by this recipe" asks first. */
    fun requestConsume() {
        if (!isRunning) dialog = RecipeDialog.Consume
    }

    fun consume() = run {
        val name = state.value?.recipe?.name.orEmpty()
        val error = changeErrorOf { recipes.consumeRecipe(recipeId) }
        error?.let(RecipeEvent::ConsumeFailed) ?: RecipeEvent.Consumed(name)
    }

    /** Grocy's "Put missing products on shopping list" asks with the products to put there. */
    fun requestAddMissing() {
        val products = state.value?.missingProducts ?: return
        if (!isRunning) dialog = RecipeDialog.AddMissing(products)
    }

    fun addMissing(excludedProductIds: List<Int>) = run {
        val error = changeErrorOf {
            recipes.addMissingProductsToShoppingList(recipeId, excludedProductIds)
        }
        if (error == null) {
            // The shopping list shows them next time, offline too
            ignoringServerErrors { shoppingLists.refresh() }
            RecipeEvent.MissingAdded
        } else {
            RecipeEvent.AddMissingFailed(error)
        }
    }

    fun dismissDialog() {
        dialog = null
    }

    /**
     * The recipe picture, stored for offline use; null when it is not stored and cannot be
     * downloaded, e.g. offline.
     */
    suspend fun picture(fileName: String): ByteArray? =
        ignoringServerErrors { recipes.picture(fileName) }

    private fun run(block: suspend () -> RecipeEvent) {
        if (isRunning) return
        dialog = null
        isRunning = true
        viewModelScope.launch {
            val event = block()
            isRunning = false
            eventChannel.send(event)
        }
    }

    private class MasterDataPart(
        val products: List<Product>,
        val units: List<QuantityUnit>,
        val conversions: List<ResolvedQuantityUnitConversion>,
        val config: ServerConfig,
        val settings: UserSettings,
    )

    private companion object {
        const val SERVINGS_DELAY_MILLIS = 800L
    }
}
