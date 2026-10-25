package com.patrickzedler.grocy.feature.masterdata.impl

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.util.RefreshController
import com.patrickzedler.grocy.core.data.util.RefreshState
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.LeadingTrailingContentWrapper
import com.patrickzedler.grocy.core.design.component.NavigateBackButton
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.SegmentedListScreen
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.segmentedGroup
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.ic_rounded_battery_charging_full
import com.patrickzedler.grocy.core.resources.ic_rounded_category
import com.patrickzedler.grocy.core.resources.ic_rounded_checklist
import com.patrickzedler.grocy.core.resources.ic_rounded_cleaning_services
import com.patrickzedler.grocy.core.resources.ic_rounded_dataset
import com.patrickzedler.grocy.core.resources.ic_rounded_edit_attributes
import com.patrickzedler.grocy.core.resources.ic_rounded_location_on
import com.patrickzedler.grocy.core.resources.ic_rounded_stacks
import com.patrickzedler.grocy.core.resources.ic_rounded_storefront
import com.patrickzedler.grocy.core.resources.ic_rounded_straighten
import com.patrickzedler.grocy.core.resources.masterdata_batteries
import com.patrickzedler.grocy.core.resources.masterdata_batteries_count
import com.patrickzedler.grocy.core.resources.masterdata_chores
import com.patrickzedler.grocy.core.resources.masterdata_chores_count
import com.patrickzedler.grocy.core.resources.masterdata_locations
import com.patrickzedler.grocy.core.resources.masterdata_locations_count
import com.patrickzedler.grocy.core.resources.masterdata_product_groups
import com.patrickzedler.grocy.core.resources.masterdata_product_groups_count
import com.patrickzedler.grocy.core.resources.masterdata_products
import com.patrickzedler.grocy.core.resources.masterdata_products_count
import com.patrickzedler.grocy.core.resources.masterdata_quantity_units
import com.patrickzedler.grocy.core.resources.masterdata_quantity_units_count
import com.patrickzedler.grocy.core.resources.masterdata_stores
import com.patrickzedler.grocy.core.resources.masterdata_stores_count
import com.patrickzedler.grocy.core.resources.masterdata_task_categories
import com.patrickzedler.grocy.core.resources.masterdata_task_categories_count
import com.patrickzedler.grocy.core.resources.masterdata_title
import com.patrickzedler.grocy.core.resources.masterdata_userentities
import com.patrickzedler.grocy.core.resources.masterdata_userentities_count
import com.patrickzedler.grocy.core.resources.masterdata_userfields
import com.patrickzedler.grocy.core.resources.masterdata_userfields_count
import com.patrickzedler.grocy.core.ui.RefreshErrorEffect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** The entries of Grocy's "Manage master data" menu, in its order. */
internal enum class MasterDataCategory(
    val title: StringResource,
    val icon: DrawableResource,
    /** "N objects" below the title. */
    val count: PluralStringResource,
    /** Grocy hides the entry when the server turned the feature off. */
    val isAvailable: (ServerConfig) -> Boolean = { true },
) {
    PRODUCTS(
        Res.string.masterdata_products,
        Res.drawable.ic_rounded_category,
        Res.plurals.masterdata_products_count,
    ),
    LOCATIONS(
        Res.string.masterdata_locations,
        Res.drawable.ic_rounded_location_on,
        Res.plurals.masterdata_locations_count,
        { it.stock && it.stockLocationTracking },
    ),
    STORES(
        Res.string.masterdata_stores,
        Res.drawable.ic_rounded_storefront,
        Res.plurals.masterdata_stores_count,
        { it.stock && it.stockPriceTracking },
    ),
    QUANTITY_UNITS(
        Res.string.masterdata_quantity_units,
        Res.drawable.ic_rounded_straighten,
        Res.plurals.masterdata_quantity_units_count,
    ),
    PRODUCT_GROUPS(
        Res.string.masterdata_product_groups,
        Res.drawable.ic_rounded_stacks,
        Res.plurals.masterdata_product_groups_count,
    ),
    CHORES(
        Res.string.masterdata_chores,
        Res.drawable.ic_rounded_cleaning_services,
        Res.plurals.masterdata_chores_count,
        { it.chores },
    ),
    BATTERIES(
        Res.string.masterdata_batteries,
        Res.drawable.ic_rounded_battery_charging_full,
        Res.plurals.masterdata_batteries_count,
        { it.batteries },
    ),
    TASK_CATEGORIES(
        Res.string.masterdata_task_categories,
        Res.drawable.ic_rounded_checklist,
        Res.plurals.masterdata_task_categories_count,
        { it.tasks },
    ),
    USERFIELDS(
        Res.string.masterdata_userfields,
        Res.drawable.ic_rounded_edit_attributes,
        Res.plurals.masterdata_userfields_count,
    ),
    USERENTITIES(
        Res.string.masterdata_userentities,
        Res.drawable.ic_rounded_dataset,
        Res.plurals.masterdata_userentities_count,
    ),
}

private fun availableCategories(config: ServerConfig) =
    MasterDataCategory.entries.filter { it.isAvailable(config) }

internal data class MasterDataUiState(
    val categories: List<MasterDataCategory>,
    /** Number of objects per category, missing until known (stored objects or a download). */
    val counts: Map<MasterDataCategory, Int>,
)

/** All objects of [category] in the database, disabled ones included like Grocy's tables. */
private fun MasterDataRepository.objectsOf(category: MasterDataCategory): Flow<List<Any>> =
    when (category) {
        MasterDataCategory.PRODUCTS -> products
        MasterDataCategory.LOCATIONS -> locations
        MasterDataCategory.STORES -> stores
        MasterDataCategory.QUANTITY_UNITS -> quantityUnits
        MasterDataCategory.PRODUCT_GROUPS -> productGroups
        MasterDataCategory.CHORES -> chores
        MasterDataCategory.BATTERIES -> batteries
        MasterDataCategory.TASK_CATEGORIES -> taskCategories
        MasterDataCategory.USERFIELDS -> allUserfields
        MasterDataCategory.USERENTITIES -> userentities
    }

/**
 * The categories the server's features allow and their numbers of objects, from the database;
 * pull to refresh downloads them again. The categories come from [serverConfig], read at app
 * start, so they show in the first frame even while a download keeps the database busy.
 */
internal class MasterDataViewModel(
    repository: MasterDataRepository,
    serverConfig: StateFlow<ServerConfig?>,
) : ViewModel() {

    private val refreshController = RefreshController(viewModelScope) { repository.refresh() }
    val refresh: StateFlow<RefreshState> = refreshController.state

    // Unknown until the database answers, which must not hold back the categories
    private val counts: Flow<Map<MasterDataCategory, Int>> = combine(
        MasterDataCategory.entries.map { category ->
            repository.objectsOf(category).map { category to it.size }
        },
    ) { it.toMap() }.onStart { emit(emptyMap()) }

    val state: StateFlow<MasterDataUiState?> = combine(
        serverConfig.filterNotNull(),
        counts,
        refreshController.state,
    ) { config, counts, refresh ->
        MasterDataUiState(
            categories = availableCategories(config),
            // An empty database is no proof that the server has no objects
            counts = counts.filterValues { it > 0 || refresh.loadedOnce },
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        serverConfig.value?.let { MasterDataUiState(availableCategories(it), counts = emptyMap()) },
    )

    init {
        refresh()
    }

    fun refresh() = refreshController.refresh()

    fun onErrorShown() = refreshController.onErrorShown()
}

@Composable
internal fun MasterDataScreen(
    menuItems: List<AppBarMenuItem>,
    viewModel: MasterDataViewModel,
    onBackClick: () -> Unit,
    onCategoryClick: (MasterDataCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refresh by viewModel.refresh.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    RefreshErrorEffect(
        error = refresh.error,
        hasData = true,
        snackbarHostState = snackbarHostState,
        onErrorShown = viewModel::onErrorShown,
    )

    SegmentedListScreen(
        title = stringResource(Res.string.masterdata_title),
        modifier = modifier,
        navigationIcon = { NavigateBackButton(onClick = onBackClick) },
        menuItems = menuItems,
        snackbarHostState = snackbarHostState,
        isRefreshing = refresh.isRefreshing,
        onRefresh = viewModel::refresh,
    ) {
        val shown = state ?: return@SegmentedListScreen
        segmentedGroup(key = "categories") {
            shown.categories.forEachIndexed { index, category ->
                CategoryItem(
                    category = category,
                    supporting = shown.counts[category]?.let {
                        pluralStringResource(category.count, it, it)
                    },
                    index = index,
                    count = shown.categories.size,
                    onClick = { onCategoryClick(category) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CategoryItem(
    category: MasterDataCategory,
    supporting: String?,
    index: Int,
    count: Int,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    SegmentedListItem(
        onClick = {
            haptic.confirm()
            onClick()
        },
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = SegmentedListDefaults.colors(),
        modifier = Modifier.fillMaxWidth().clickCursor(),
        verticalAlignment = Alignment.CenterVertically,
        supportingContent = supporting?.let { { Text(it) } },
        leadingContent = {
            LeadingTrailingContentWrapper {
                Icon(painter = painterResource(category.icon), contentDescription = null)
            }
        },
    ) {
        Text(stringResource(category.title))
    }
}
