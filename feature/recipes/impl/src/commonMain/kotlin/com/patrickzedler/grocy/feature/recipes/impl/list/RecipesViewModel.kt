package com.patrickzedler.grocy.feature.recipes.impl.list

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.recipes.RecipesRepository
import com.patrickzedler.grocy.core.data.settings.RecipesOptionsRepository
import com.patrickzedler.grocy.core.data.util.RefreshController
import com.patrickzedler.grocy.core.data.util.RefreshState
import com.patrickzedler.grocy.core.forms.ignoringServerErrors
import com.patrickzedler.grocy.core.model.recipes.RecipeField
import com.patrickzedler.grocy.core.model.recipes.RecipeGrouping
import com.patrickzedler.grocy.core.model.recipes.RecipeStatus
import com.patrickzedler.grocy.core.model.recipes.RecipesOptions
import com.patrickzedler.grocy.core.model.recipes.RecipesView
import com.patrickzedler.grocy.core.model.server.ServerConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the recipes page shows; built from the database, never waits for the server. */
internal data class RecipesUiState(
    val groups: List<RecipeGroup>,
    val statusCounts: Map<RecipeStatus, Int>,
    val filter: RecipesFilter,
    /** The order the list has, the default one when the chosen field is hidden. */
    val sort: RecipeSort,
    val isDefaultSort: Boolean,
    val grouping: RecipeGrouping,
    /** The shown fields besides the name, in Grocy's order. */
    val fields: List<RecipeField>,
    val availableFields: List<RecipeField>,
    val availableGroupings: List<RecipeGrouping>,
    val view: RecipesView,
    val config: ServerConfig,
    val amountDecimals: Int,
    /** False until the first download stored any recipe. */
    val hasRecipes: Boolean,
) {
    val isEmpty: Boolean
        get() = groups.isEmpty()

    val recipeCount: Int
        get() = groups.sumOf { it.items.size }
}

/**
 * Grocy's recipes page: shows the stored recipes at once and downloads them with the master
 * data in the background. Fields, grouping, sort and view are kept like Grocy's table options
 * and last tab ([RecipesOptionsRepository]), the status filter and the search ([search] at
 * first, e.g. a product from the stock overview) only here.
 */
@OptIn(FlowPreview::class)
internal class RecipesViewModel(
    private val recipes: RecipesRepository,
    private val masterData: MasterDataRepository,
    private val optionsRepository: RecipesOptionsRepository,
    search: String? = null,
    computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    val searchState = TextFieldState(search.orEmpty())

    var isSearchVisible by mutableStateOf(!search.isNullOrBlank())
        private set

    private val filter = MutableStateFlow(RecipesFilter())

    private val refreshController = RefreshController(viewModelScope) {
        coroutineScope {
            launch { masterData.refresh() }
            launch { recipes.refresh() }
        }
        // In the background, so the pictures show offline later
        viewModelScope.launch { ignoringServerErrors { recipes.downloadMissingPictures() } }
    }
    val refresh: StateFlow<RefreshState> = refreshController.state

    // Typing stays smooth with many recipes: the list follows after a short pause
    private val query = snapshotFlow { searchState.text.toString() }
        .distinctUntilChanged()
        .debounce { if (it.isEmpty()) 0L else SEARCH_DELAY_MILLIS }

    private val options = combine(
        masterData.serverConfig,
        masterData.userSettings,
        optionsRepository.options.filterNotNull(),
        filter,
        query,
    ) { config, settings, options, filter, query ->
        ListOptions(config, settings.stockDecimalPlacesAmounts, options, filter, query)
    }

    val state: StateFlow<RecipesUiState?> = combine(
        recipes.recipes,
        recipes.fulfillment,
        options,
    ) { recipeList, fulfillment, options ->
        val config = options.config
        val available = availableFields(config)
        val shown = available.filter { it in options.options.fields }
        val sort = effectiveSort(options.options.sortField, options.options.ascending, shown)
        val grouping = options.options.grouping.takeIf { it in availableGroupings(config) }
            ?: RecipeGrouping.NONE
        // Grocy's status needs the stock
        val filter = options.filter.takeIf { config.stock } ?: RecipesFilter()
        val list = buildRecipeList(
            recipes = recipeList,
            fulfillment = fulfillment,
            filter = filter,
            sort = sort,
            grouping = grouping,
            query = options.query,
        )
        RecipesUiState(
            groups = list.groups,
            statusCounts = list.statusCounts,
            filter = filter,
            sort = sort,
            isDefaultSort = sort == DefaultSort,
            grouping = grouping,
            fields = shown,
            availableFields = available,
            availableGroupings = availableGroupings(config),
            view = options.options.view,
            config = config,
            amountDecimals = options.amountDecimals,
            hasRecipes = recipeList.isNotEmpty(),
        )
    }
        .flowOn(computeDispatcher)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    init {
        refresh()
    }

    fun refresh() = refreshController.refresh()

    fun onErrorShown() = refreshController.onErrorShown()

    fun setStatus(status: RecipeStatus?) {
        filter.value = RecipesFilter(status)
    }

    /** Like Grocy's "Clear filter": resets the status and the search, not the table options. */
    fun clearFilter() {
        filter.value = RecipesFilter()
        searchState.clearText()
        isSearchVisible = false
    }

    fun setSort(field: RecipeField, ascending: Boolean) {
        updateOptions { it.copy(sortField = field, ascending = ascending) }
    }

    fun setGrouping(grouping: RecipeGrouping) {
        updateOptions { it.copy(grouping = grouping) }
    }

    /** Grocy's tabs "List" and "Gallery". */
    fun setView(view: RecipesView) {
        updateOptions { it.copy(view = view) }
    }

    /**
     * Shows or hides [field]. Hiding the field the list is sorted by sorts it by name again,
     * Grocy's default, instead of by something not shown.
     */
    fun setFieldShown(field: RecipeField, shown: Boolean) {
        updateOptions { options ->
            val fields = if (shown) options.fields + field else options.fields - field
            val updated = options.copy(fields = fields)
            if (shown || options.sortField != field) {
                updated
            } else {
                updated.copy(sortField = DefaultSort.field, ascending = DefaultSort.ascending)
            }
        }
    }

    /**
     * A recipe's picture, stored for offline use; null when it is not stored and cannot be
     * downloaded, e.g. offline.
     */
    suspend fun picture(fileName: String): ByteArray? =
        ignoringServerErrors { recipes.picture(fileName) }

    fun showSearch() {
        isSearchVisible = true
    }

    fun hideSearch() {
        searchState.clearText()
        isSearchVisible = false
    }

    private fun updateOptions(transform: (RecipesOptions) -> RecipesOptions) {
        viewModelScope.launch { optionsRepository.update(transform) }
    }

    private data class ListOptions(
        val config: ServerConfig,
        val amountDecimals: Int,
        val options: RecipesOptions,
        val filter: RecipesFilter,
        val query: String,
    )

    private companion object {
        const val SEARCH_DELAY_MILLIS = 150L
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
