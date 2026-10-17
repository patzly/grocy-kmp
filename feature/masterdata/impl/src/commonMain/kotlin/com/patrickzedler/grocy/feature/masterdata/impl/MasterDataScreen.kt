package com.patrickzedler.grocy.feature.masterdata.impl

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.design.component.LeadingContentWrapper
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
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

/** The categories the server's features allow, read from the database at once. */
internal class MasterDataViewModel(repository: MasterDataRepository) : ViewModel() {
    val categories: StateFlow<List<MasterDataCategory>?> = repository.serverConfig
        .map { config -> MasterDataCategory.entries.filter { it.isAvailable(config) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
internal fun MasterDataScreen(
    viewModel: MasterDataViewModel,
    onBackClick: () -> Unit,
    onCategoryClick: (MasterDataCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    SegmentedListScreen(
        title = stringResource(Res.string.masterdata_title),
        modifier = modifier,
        navigationIcon = { NavigateBackButton(onClick = onBackClick) },
    ) {
        val shown = categories ?: return@SegmentedListScreen
        segmentedGroup(key = "categories") {
            shown.forEachIndexed { index, category ->
                CategoryItem(
                    category = category,
                    index = index,
                    count = shown.size,
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
        leadingContent = {
            LeadingContentWrapper {
                Icon(painter = painterResource(category.icon), contentDescription = null)
            }
        },
    ) {
        Text(stringResource(category.title))
    }
}
