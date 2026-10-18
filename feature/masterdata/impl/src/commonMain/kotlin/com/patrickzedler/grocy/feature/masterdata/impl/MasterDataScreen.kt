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
import com.patrickzedler.grocy.core.design.component.LeadingTrailingContentWrapper
import com.patrickzedler.grocy.core.design.component.NavigateBackButton
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.SegmentedListScreen
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
import com.patrickzedler.grocy.core.resources.masterdata_products_count
import com.patrickzedler.grocy.core.resources.masterdata_chores
import com.patrickzedler.grocy.core.resources.masterdata_locations
import com.patrickzedler.grocy.core.resources.masterdata_product_groups
import com.patrickzedler.grocy.core.resources.masterdata_products
import com.patrickzedler.grocy.core.resources.masterdata_quantity_units
import com.patrickzedler.grocy.core.resources.masterdata_stores
import com.patrickzedler.grocy.core.resources.masterdata_task_categories
import com.patrickzedler.grocy.core.resources.masterdata_title
import com.patrickzedler.grocy.core.resources.masterdata_userentities
import com.patrickzedler.grocy.core.resources.masterdata_userfields
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import com.patrickzedler.grocy.core.ui.RefreshErrorEffect
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** The entries of Grocy's "Manage master data" menu, in its order. */
internal enum class MasterDataCategory(
    val title: StringResource,
    val icon: DrawableResource,
    /** Grocy hides the entry when the server turned the feature off. */
    val isAvailable: (ServerConfig) -> Boolean = { true },
) {
    PRODUCTS(Res.string.masterdata_products, Res.drawable.ic_rounded_category),
    LOCATIONS(
        Res.string.masterdata_locations,
        Res.drawable.ic_rounded_location_on,
        { it.stock && it.stockLocationTracking },
    ),
    STORES(
        Res.string.masterdata_stores,
        Res.drawable.ic_rounded_storefront,
        { it.stock && it.stockPriceTracking },
    ),
    QUANTITY_UNITS(Res.string.masterdata_quantity_units, Res.drawable.ic_rounded_straighten),
    PRODUCT_GROUPS(Res.string.masterdata_product_groups, Res.drawable.ic_rounded_stacks),
    CHORES(Res.string.masterdata_chores, Res.drawable.ic_rounded_cleaning_services, { it.chores }),
    BATTERIES(
        Res.string.masterdata_batteries,
        Res.drawable.ic_rounded_battery_charging_full,
        { it.batteries },
    ),
    TASK_CATEGORIES(
        Res.string.masterdata_task_categories,
        Res.drawable.ic_rounded_checklist,
        { it.tasks },
    ),
    USERFIELDS(Res.string.masterdata_userfields, Res.drawable.ic_rounded_edit_attributes),
    USERENTITIES(Res.string.masterdata_userentities, Res.drawable.ic_rounded_dataset),
}

internal data class MasterDataUiState(
    val categories: List<MasterDataCategory>,
    /** Number of products, null until it is known (stored products or a finished download). */
    val productCount: Int?,
)

/**
 * The categories the server's features allow and the number of products, from the database;
 * pull to refresh downloads them again.
 */
internal class MasterDataViewModel(repository: MasterDataRepository) : ViewModel() {

    private val refreshController = RefreshController(viewModelScope) { repository.refresh() }
    val refresh: StateFlow<RefreshState> = refreshController.state

    val state: StateFlow<MasterDataUiState?> = combine(
        repository.serverConfig,
        repository.products,
        refreshController.state,
    ) { config, products, refresh ->
        MasterDataUiState(
            categories = MasterDataCategory.entries.filter { it.isAvailable(config) },
            productCount = products.size.takeIf { it > 0 || refresh.loadedOnce },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        refresh()
    }

    fun refresh() = refreshController.refresh()

    fun onErrorShown() = refreshController.onErrorShown()
}

@Composable
internal fun MasterDataScreen(
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
        snackbarHostState = snackbarHostState,
        isRefreshing = refresh.isRefreshing,
        onRefresh = viewModel::refresh,
    ) {
        val shown = state ?: return@SegmentedListScreen
        segmentedGroup(key = "categories") {
            shown.categories.forEachIndexed { index, category ->
                CategoryItem(
                    category = category,
                    supporting = if (category == MasterDataCategory.PRODUCTS) {
                        shown.productCount?.let {
                            pluralStringResource(Res.plurals.masterdata_products_count, it, it)
                        }
                    } else {
                        null
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
        modifier = Modifier.fillMaxWidth(),
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
