package com.patrickzedler.grocy.apps.wear

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListSubHeader
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListGrouping
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListStatus
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_reload
import com.patrickzedler.grocy.core.resources.data_all
import com.patrickzedler.grocy.core.resources.data_none
import com.patrickzedler.grocy.core.resources.data_product_group
import com.patrickzedler.grocy.core.resources.ic_rounded_refresh
import com.patrickzedler.grocy.core.resources.products_filter_status
import com.patrickzedler.grocy.core.resources.products_sort_store
import com.patrickzedler.grocy.core.resources.shopping_list_status_done
import com.patrickzedler.grocy.core.resources.shopping_list_status_undone
import com.patrickzedler.grocy.core.resources.shopping_mode_auto_sync
import com.patrickzedler.grocy.core.resources.shopping_mode_done_last
import com.patrickzedler.grocy.core.resources.shopping_mode_settings
import com.patrickzedler.grocy.core.resources.shopping_mode_sync_interval
import com.patrickzedler.grocy.core.resources.stock_overview_group_by
import com.patrickzedler.grocy.core.wear.WearShoppingState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val SYNC_INTERVALS = listOf(5, 10, 15, 30, 60)

/**
 * The options of the phone's shopping mode in one scrolling list, as Wear OS has no menus: first
 * what its overflow menu holds, then the choices of its filter chips.
 */
@Composable
internal fun WearOptionsScreen(
    state: WearShoppingState,
    isRefreshing: Boolean,
    onReloadClick: () -> Unit,
    onAutoSyncChange: (Boolean) -> Unit,
    onDoneItemsLastChange: (Boolean) -> Unit,
    onGroupingChange: (ShoppingListGrouping) -> Unit,
    onStatusChange: (ShoppingListStatus?) -> Unit,
    onSyncIntervalChange: (Int) -> Unit,
) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    val haptic = LocalHapticFeedback.current
    val settings = state.settings

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item(key = "title") {
                ListHeader(Modifier.transformedHeight(this, spec)) {
                    Text(stringResource(Res.string.shopping_mode_settings))
                }
            }
            item(key = "reload") {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onReloadClick()
                    },
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                    enabled = !isRefreshing,
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    icon = {
                        if (isRefreshing) {
                            CircularProgressIndicator(Modifier.size(24.dp))
                        } else {
                            Icon(
                                painter = painterResource(Res.drawable.ic_rounded_refresh),
                                contentDescription = null,
                            )
                        }
                    },
                    label = { Text(stringResource(Res.string.action_reload)) },
                )
            }
            switchItem(
                key = "autoSync",
                checked = settings.autoSync,
                onCheckedChange = onAutoSyncChange,
                spec = spec,
            ) { Text(stringResource(Res.string.shopping_mode_auto_sync)) }
            switchItem(
                key = "doneLast",
                checked = settings.doneItemsLast,
                onCheckedChange = onDoneItemsLastChange,
                spec = spec,
            ) { Text(stringResource(Res.string.shopping_mode_done_last)) }

            subHeader("groupBy", spec) { Text(stringResource(Res.string.stock_overview_group_by)) }
            items(state.availableGroupings, key = { "grouping-$it" }) { grouping ->
                OptionRow(
                    selected = grouping == state.grouping,
                    onSelect = { onGroupingChange(grouping) },
                    spec = spec,
                ) { Text(stringResource(grouping.label)) }
            }

            subHeader("status", spec) { Text(stringResource(Res.string.products_filter_status)) }
            items(STATUSES, key = { "status-$it" }) { status ->
                OptionRow(
                    selected = status == state.status,
                    onSelect = { onStatusChange(status) },
                    spec = spec,
                ) {
                    Text(
                        when (status) {
                            null -> stringResource(Res.string.data_all)
                            ShoppingListStatus.DONE -> stringResource(
                                Res.string.shopping_list_status_done,
                                state.doneCount,
                            )
                            else -> stringResource(
                                Res.string.shopping_list_status_undone,
                                state.undoneCount,
                            )
                        },
                    )
                }
            }

            if (settings.autoSync) {
                subHeader("interval", spec) {
                    Text(stringResource(Res.string.shopping_mode_sync_interval))
                }
                // An interval set on an older version stays selectable
                val intervals = (SYNC_INTERVALS + settings.syncIntervalSeconds).distinct().sorted()
                items(intervals, key = { "interval-$it" }) { seconds ->
                    OptionRow(
                        selected = seconds == settings.syncIntervalSeconds,
                        onSelect = { onSyncIntervalChange(seconds) },
                        spec = spec,
                    ) { Text(seconds.toString()) }
                }
            }
        }
    }
}

private val STATUSES = listOf(null, ShoppingListStatus.UNDONE, ShoppingListStatus.DONE)

private val ShoppingListGrouping.label
    get() = when (this) {
        ShoppingListGrouping.NONE -> Res.string.data_none
        ShoppingListGrouping.PRODUCT_GROUP -> Res.string.data_product_group
        ShoppingListGrouping.DEFAULT_STORE -> Res.string.products_sort_store
    }

private fun TransformingLazyColumnScope.subHeader(
    key: String,
    spec: TransformationSpec,
    label: @Composable () -> Unit,
) {
    item(key = key) {
        ListSubHeader(Modifier.transformedHeight(this, spec)) { label() }
    }
}

private fun TransformingLazyColumnScope.switchItem(
    key: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    spec: TransformationSpec,
    label: @Composable () -> Unit,
) {
    item(key = key) {
        val haptic = LocalHapticFeedback.current
        SwitchButton(
            checked = checked,
            onCheckedChange = {
                haptic.performHapticFeedback(
                    if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff,
                )
                onCheckedChange(it)
            },
            modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
            transformation = SurfaceTransformation(spec),
            label = { label() },
        )
    }
}

@Composable
private fun TransformingLazyColumnItemScope.OptionRow(
    selected: Boolean,
    onSelect: () -> Unit,
    spec: TransformationSpec,
    label: @Composable () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    RadioButton(
        selected = selected,
        onSelect = {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            onSelect()
        },
        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
        transformation = SurfaceTransformation(spec),
        label = { label() },
    )
}
