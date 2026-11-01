package com.patrickzedler.grocy.feature.recipes.impl.mealplan

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.recipes.MealPlanRepository
import com.patrickzedler.grocy.core.data.recipes.RecipesRepository
import com.patrickzedler.grocy.core.data.stock.StockRepository
import com.patrickzedler.grocy.core.data.util.RefreshController
import com.patrickzedler.grocy.core.data.util.RefreshState
import com.patrickzedler.grocy.core.forms.ChangeError
import com.patrickzedler.grocy.core.forms.changeErrorOf
import com.patrickzedler.grocy.core.forms.ignoringServerErrors
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.recipes.DAYS_PER_WEEK
import com.patrickzedler.grocy.core.model.recipes.MealPlanEntry
import com.patrickzedler.grocy.core.model.recipes.MealPlanEntryType
import com.patrickzedler.grocy.core.model.recipes.MealPlanSection
import com.patrickzedler.grocy.core.model.recipes.mealPlanWeekStart
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.text.isoDateOf
import com.patrickzedler.grocy.core.model.text.localeFirstDayOfWeek
import com.patrickzedler.grocy.core.model.text.platformDecimalSeparator
import com.patrickzedler.grocy.core.model.text.todayEpochDay
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the meal plan shows of the chosen week; built from the database, never waits for it. */
internal data class MealPlanUiState(
    val week: MealPlanWeek,
    val today: Long,
    val config: ServerConfig,
    val amountDecimals: Int,
    val priceDecimals: Int,
) {
    val isCurrentWeek: Boolean
        get() = today in week.start..week.end
}

/** The actions of the meal plan, for their messages. */
internal enum class MealPlanAction { SAVE, DELETE, DONE, COPY_DAY, CONSUME, SHOPPING_LIST, UNDO }

/** What became of an action of [MealPlanViewModel], shown or acted on once. */
internal sealed interface MealPlanEvent {
    /** The entry was deleted like Grocy does without asking; [entry] brings it back. */
    data class Deleted(val entry: MealPlanEntry) : MealPlanEvent

    data object Restored : MealPlanEvent

    data object DayCopied : MealPlanEvent

    data class RecipeConsumed(val name: String) : MealPlanEvent

    /** Grocy's message with "Undo" of [transactionId]. */
    data class ProductConsumed(
        val amount: Double,
        val unit: QuantityUnit?,
        val name: String,
        val transactionId: String?,
    ) : MealPlanEvent

    data object WeekConsumed : MealPlanEvent

    data object PutOnShoppingList : MealPlanEvent

    data object Undone : MealPlanEvent

    /** Grocy shows the recipe at the planned servings, which it set as desired ones. */
    data class OpenRecipe(val recipeId: Int) : MealPlanEvent

    data class Failed(val action: MealPlanAction, val error: ChangeError) : MealPlanEvent
}

internal sealed interface MealPlanDialog {
    class Entry(val state: MealPlanEntryDialogState) : MealPlanDialog

    class CopyDay(val state: CopyDayDialogState) : MealPlanDialog

    /** Grocy's question before consuming what a planned recipe needs. */
    data class ConsumeRecipe(val item: MealPlanItem) : MealPlanDialog

    /** Grocy's question before consuming what the whole week needs. */
    data class ConsumeWeek(val weekRecipeId: Int) : MealPlanDialog
}

/**
 * Grocy's meal plan: the week of [initialDay] ("YYYY-MM-DD", today without) at once from the
 * database, downloaded with the recipes, master data and stock in the background. Weeks start
 * like Grocy's ([mealPlanWeekStart]). Every change is online only, like Grocy's buttons.
 */
internal class MealPlanViewModel(
    private val mealPlan: MealPlanRepository,
    private val recipes: RecipesRepository,
    private val masterData: MasterDataRepository,
    private val stock: StockRepository,
    initialDay: String? = null,
    private val today: () -> Long = ::todayEpochDay,
    private val localeFirstDay: () -> Int = ::localeFirstDayOfWeek,
    private val decimalSeparator: Char = platformDecimalSeparator(),
    computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    // A day of the shown week; weeks start where Grocy's settings say
    private val anchorDay = MutableStateFlow(anchorDayOf(initialDay, today()))

    var dialog by mutableStateOf<MealPlanDialog?>(null)
        private set

    /** An action runs on the server; the meal plan takes no other meanwhile. */
    var isRunning by mutableStateOf(false)
        private set

    private val eventChannel = Channel<MealPlanEvent>(Channel.BUFFERED)
    val events: Flow<MealPlanEvent> = eventChannel.receiveAsFlow()

    private val refreshController = RefreshController(viewModelScope) {
        val config = masterData.serverConfig.first()
        coroutineScope {
            launch { masterData.refresh() }
            launch { recipes.refresh() }
            launch { mealPlan.refresh() }
            // Stock amounts and last prices of planned products
            if (config.stock) launch { stock.refreshOverview(config.shoppingList) }
        }
        viewModelScope.launch { ignoringServerErrors { recipes.downloadMissingPictures() } }
    }
    val refresh: StateFlow<RefreshState> = refreshController.state

    private val data = combine(
        combine(mealPlan.entries, mealPlan.sections, mealPlan.mealPlanRecipes, ::Triple),
        combine(recipes.recipes, masterData.products, masterData.quantityUnits, ::Triple),
        combine(masterData.stock, stock.productStockInfo, ::Pair),
    ) { (entries, sections, mealPlanRecipes), (recipeList, products, units), (amounts, info) ->
        MealPlanData(
            entries = entries,
            sections = sections,
            mealPlanRecipes = mealPlanRecipes,
            recipes = recipeList,
            products = products,
            units = units,
            stock = amounts,
            stockInfo = info,
        )
    }

    val state: StateFlow<MealPlanUiState?> = combine(
        data,
        masterData.serverConfig,
        masterData.userSettings,
        anchorDay,
    ) { data, config, settings, anchor ->
        val today = today()
        MealPlanUiState(
            week = buildMealPlanWeek(weekStartOf(anchor, config, today), today, data),
            today = today,
            config = config,
            amountDecimals = settings.stockDecimalPlacesAmounts,
            priceDecimals = settings.stockDecimalPlacesPricesDisplay,
        )
    }
        .flowOn(computeDispatcher)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    init {
        refresh()
    }

    fun refresh() = refreshController.refresh()

    fun onErrorShown() = refreshController.onErrorShown()

    fun showPreviousWeek() {
        anchorDay.value -= DAYS_PER_WEEK
    }

    fun showNextWeek() {
        anchorDay.value += DAYS_PER_WEEK
    }

    /** Grocy's "Today": the week of today. */
    fun showToday() {
        anchorDay.value = today()
    }

    /** Grocy's "Add recipe", "Add product" and "Add note" of [day] ("YYYY-MM-DD"). */
    fun add(type: MealPlanEntryType, day: String) = openEntryDialog(type, original = null, day)

    /** Grocy's "Edit this item". */
    fun edit(entry: MealPlanEntry) = openEntryDialog(entry.type, entry, entry.day)

    /** Saves the open entry dialog; it closes once the server took the entry. */
    fun saveEntry() {
        val state = (dialog as? MealPlanDialog.Entry)?.state ?: return
        if (state.isSaving) return
        val entry = state.validated() ?: return
        state.isSaving = true
        state.error = null
        viewModelScope.launch {
            val error = changeErrorOf { mealPlan.saveEntry(entry) }
            state.isSaving = false
            if (error == null) {
                if ((dialog as? MealPlanDialog.Entry)?.state === state) dialog = null
            } else {
                state.error = error
            }
        }
    }

    /** Grocy's "Delete this item", at once like Grocy; the message can bring it back. */
    fun delete(entry: MealPlanEntry) = run(MealPlanAction.DELETE) {
        mealPlan.deleteEntry(entry.id)
        MealPlanEvent.Deleted(entry)
    }

    /** "Undo" of a deletion: the entry once more, done like it was. */
    fun restore(entry: MealPlanEntry) = run(MealPlanAction.UNDO) {
        mealPlan.saveEntry(entry.copy(id = MasterDataRepository.NEW_OBJECT_ID))
        MealPlanEvent.Restored
    }

    /** Grocy's "Mark this item as done" and "... as undone". */
    fun setDone(entry: MealPlanEntry, done: Boolean) = run(MealPlanAction.DONE) {
        mealPlan.setDone(entry.id, done)
        null
    }

    /** Grocy's "Copy this day" of [day]: asks for the day to copy it to. */
    fun requestCopyDay(day: String) {
        if (isRunning) return
        dialog = MealPlanDialog.CopyDay(CopyDayDialogState(day))
    }

    fun copyDay() {
        val state = (dialog as? MealPlanDialog.CopyDay)?.state ?: return
        if (state.isSaving) return
        val toDay = state.validated() ?: return
        state.isSaving = true
        state.saveError = null
        viewModelScope.launch {
            val error = changeErrorOf { mealPlan.copyDay(state.fromDay, toDay) }
            state.isSaving = false
            if (error == null) {
                if ((dialog as? MealPlanDialog.CopyDay)?.state === state) dialog = null
                eventChannel.send(MealPlanEvent.DayCopied)
            } else {
                state.saveError = error
            }
        }
    }

    /** Grocy's "Consume all ingredients needed by this recipe" of a planned recipe: asks first. */
    fun requestConsume(item: MealPlanItem) {
        if (isRunning) return
        dialog = MealPlanDialog.ConsumeRecipe(item)
    }

    fun consumeRecipe(item: MealPlanItem) {
        val content = item.content as? MealPlanContent.Recipe ?: return
        val shadowId = content.shadowRecipeId ?: return
        run(MealPlanAction.CONSUME) {
            mealPlan.consumeRecipe(item.entry.id, shadowId)
            MealPlanEvent.RecipeConsumed(content.name)
        }
    }

    /**
     * Grocy's "Consume" of a planned product: its amount from stock, then the entry is done;
     * the message can undo the booking.
     */
    fun consumeProduct(item: MealPlanItem) {
        val content = item.content as? MealPlanContent.Product ?: return
        run(MealPlanAction.CONSUME) {
            val config = masterData.serverConfig.first()
            val transactionId = stock.consumeProduct(
                productId = content.productId,
                amount = content.amount,
                spoiled = false,
                withShoppingLists = config.shoppingList,
            )
            mealPlan.setDone(item.entry.id, true)
            MealPlanEvent.ProductConsumed(
                content.amount,
                content.unit,
                content.name,
                transactionId,
            )
        }
    }

    /** "Undo" of a consumed product; the entry stays done like Grocy's undo leaves it. */
    fun undoTransaction(transactionId: String) = run(MealPlanAction.UNDO) {
        val config = masterData.serverConfig.first()
        stock.undoTransaction(transactionId, config.shoppingList)
        MealPlanEvent.Undone
    }

    /**
     * Grocy's "Put missing products on shopping list" of a planned recipe at its servings, or
     * with [recipeId] of the whole week; off while the shopping list covers what is missing.
     */
    fun putOnShoppingList(recipeId: Int) = run(MealPlanAction.SHOPPING_LIST) {
        mealPlan.putMissingOnShoppingList(recipeId)
        MealPlanEvent.PutOnShoppingList
    }

    /** Grocy's "Consume all ingredients needed by this weeks recipes or products": asks first. */
    fun requestConsumeWeek(weekRecipeId: Int) {
        if (isRunning) return
        dialog = MealPlanDialog.ConsumeWeek(weekRecipeId)
    }

    fun consumeWeek(weekRecipeId: Int) = run(MealPlanAction.CONSUME) {
        mealPlan.consumeWeek(weekRecipeId)
        MealPlanEvent.WeekConsumed
    }

    /**
     * Grocy's "Display recipe": the recipe at the entry's servings, which Grocy sets as its
     * desired servings first; offline it opens as it is.
     */
    fun openRecipe(item: MealPlanItem) {
        val content = item.content as? MealPlanContent.Recipe ?: return
        if (isRunning) return
        isRunning = true
        viewModelScope.launch {
            ignoringServerErrors { recipes.setDesiredServings(content.recipeId, content.servings) }
            isRunning = false
            eventChannel.send(MealPlanEvent.OpenRecipe(content.recipeId))
        }
    }

    fun dismissDialog() {
        val saving = when (val current = dialog) {
            is MealPlanDialog.Entry -> current.state.isSaving
            is MealPlanDialog.CopyDay -> current.state.isSaving
            else -> false
        }
        if (!saving) dialog = null
    }

    /** A recipe's picture, stored for offline use; null when it cannot be had. */
    suspend fun recipePicture(fileName: String): ByteArray? =
        ignoringServerErrors { recipes.picture(fileName) }

    /** A product's picture as in lists, stored for offline use; null when it cannot be had. */
    suspend fun productPicture(fileName: String): ByteArray? =
        ignoringServerErrors { masterData.productThumbnail(fileName) }

    private fun openEntryDialog(type: MealPlanEntryType, original: MealPlanEntry?, day: String) {
        if (isRunning) return
        viewModelScope.launch {
            val choices = MealPlanChoices(
                recipes = recipes.recipes.first(),
                products = masterData.products.first().filter { it.active },
                sections = mealPlan.sections.first().ifEmpty { listOf(DefaultSection) },
                units = masterData.quantityUnits.first(),
                conversions = masterData.resolvedQuantityUnitConversions.first(),
                decimals = masterData.userSettings.first().stockDecimalPlacesAmounts,
                decimalSeparator = decimalSeparator,
            )
            dialog = MealPlanDialog.Entry(MealPlanEntryDialogState(type, original, day, choices))
        }
    }

    private fun weekStartOf(anchor: Long, config: ServerConfig, today: Long): Long =
        mealPlanWeekStart(
            epochDay = anchor,
            mealPlanFirstDay = config.mealPlanFirstDayOfWeek,
            calendarFirstDay = config.calendarFirstDayOfWeek,
            localeFirstDay = localeFirstDay(),
            today = today,
        )

    private fun run(action: MealPlanAction, block: suspend () -> MealPlanEvent?) {
        if (isRunning) return
        dialog = null
        isRunning = true
        viewModelScope.launch {
            var event: MealPlanEvent? = null
            val error = changeErrorOf { event = block() }
            isRunning = false
            when {
                error != null -> eventChannel.send(MealPlanEvent.Failed(action, error))
                else -> event?.let { eventChannel.send(it) }
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L

        // Grocy always has its default section, also before the first download
        val DefaultSection = MealPlanSection(MealPlanSection.DEFAULT_ID, "", sortNumber = -1)
    }
}

/** The day new entries of the toolbar's "Add" go to: today in its week, else the week's first. */
internal fun addDayOf(state: MealPlanUiState): String =
    isoDateOf(if (state.isCurrentWeek) state.today else state.week.start)
