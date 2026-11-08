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
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
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
import com.patrickzedler.grocy.core.wear.wearSyncIntervals
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** A setting with several values, a screen of its own. */
internal enum class WearOption(val route: String) {
    GROUPING("options/grouping"),
    STATUS("options/status"),
    SYNC_INTERVAL("options/interval"),
}

/**
 * The options of the phone's shopping mode, as Wear OS has no menus: what is on or off right here,
 * each setting with several values one tap away on a screen of its own, which shows the value
 * here.
 */
@Composable
internal fun WearOptionsScreen(
    state: WearShoppingState,
    isRefreshing: Boolean,
    onReloadClick: () -> Unit,
    onAutoSyncChange: (Boolean) -> Unit,
    onDoneItemsLastChange: (Boolean) -> Unit,
    onOptionClick: (WearOption) -> Unit,
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
            item(key = "autoSync") {
                OptionSwitch(settings.autoSync, onAutoSyncChange, spec) {
                    Text(stringResource(Res.string.shopping_mode_auto_sync))
                }
            }
            if (settings.autoSync) {
                item(key = "interval") {
                    OptionButton(
                        title = stringResource(Res.string.shopping_mode_sync_interval),
                        value = settings.syncIntervalSeconds.toString(),
                        onClick = { onOptionClick(WearOption.SYNC_INTERVAL) },
                        spec = spec,
                    )
                }
            }
            item(key = "doneLast") {
                OptionSwitch(settings.doneItemsLast, onDoneItemsLastChange, spec) {
                    Text(stringResource(Res.string.shopping_mode_done_last))
                }
            }
            item(key = "groupBy") {
                OptionButton(
                    title = stringResource(Res.string.stock_overview_group_by),
                    value = stringResource(state.grouping.label),
                    onClick = { onOptionClick(WearOption.GROUPING) },
                    spec = spec,
                )
            }
            item(key = "status") {
                OptionButton(
                    title = stringResource(Res.string.products_filter_status),
                    value = statusLabel(state.status, state),
                    onClick = { onOptionClick(WearOption.STATUS) },
                    spec = spec,
                )
            }
        }
    }
}

@Composable
internal fun WearGroupingScreen(
    state: WearShoppingState,
    onChange: (ShoppingListGrouping) -> Unit,
) {
    ChoiceScreen(
        title = stringResource(Res.string.stock_overview_group_by),
        choices = state.availableGroupings,
        selected = state.grouping,
        label = { stringResource(it.label) },
        onSelect = onChange,
    )
}

@Composable
internal fun WearStatusScreen(
    state: WearShoppingState,
    onChange: (ShoppingListStatus?) -> Unit,
) {
    ChoiceScreen(
        title = stringResource(Res.string.products_filter_status),
        choices = STATUSES,
        selected = state.status,
        label = { statusLabel(it, state) },
        onSelect = onChange,
    )
}

@Composable
internal fun WearSyncIntervalScreen(state: WearShoppingState, onChange: (Int) -> Unit) {
    ChoiceScreen(
        title = stringResource(Res.string.shopping_mode_sync_interval),
        choices = wearSyncIntervals(state.settings.syncIntervalSeconds),
        selected = state.settings.syncIntervalSeconds,
        label = { it.toString() },
        onSelect = onChange,
    )
}

/**
 * A setting's values below its title. A choice takes effect at once and the screen stays, the
 * user swipes back like from every screen of the watch.
 */
@Composable
private fun <T> ChoiceScreen(
    title: String,
    choices: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    val haptic = LocalHapticFeedback.current

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item(key = "title") {
                ListHeader(Modifier.transformedHeight(this, spec)) { Text(title) }
            }
            items(choices, key = { "choice-$it" }) { choice ->
                RadioButton(
                    selected = choice == selected,
                    onSelect = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onSelect(choice)
                    },
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                    label = { Text(label(choice)) },
                )
            }
        }
    }
}

@Composable
private fun statusLabel(status: ShoppingListStatus?, state: WearShoppingState): String =
    when (status) {
        null -> stringResource(Res.string.data_all)
        ShoppingListStatus.DONE ->
            stringResource(Res.string.shopping_list_status_done, state.doneCount)
        else -> stringResource(Res.string.shopping_list_status_undone, state.undoneCount)
    }

private val STATUSES = listOf(null, ShoppingListStatus.UNDONE, ShoppingListStatus.DONE)

private val ShoppingListGrouping.label
    get() = when (this) {
        ShoppingListGrouping.NONE -> Res.string.data_none
        ShoppingListGrouping.PRODUCT_GROUP -> Res.string.data_product_group
        ShoppingListGrouping.DEFAULT_STORE -> Res.string.products_sort_store
    }

/** Opens the screen of a setting, which shows its current [value] here. */
@Composable
private fun TransformingLazyColumnItemScope.OptionButton(
    title: String,
    value: String,
    onClick: () -> Unit,
    spec: TransformationSpec,
) {
    val haptic = LocalHapticFeedback.current
    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            onClick()
        },
        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
        transformation = SurfaceTransformation(spec),
        colors = ButtonDefaults.filledTonalButtonColors(),
        label = { Text(title) },
        secondaryLabel = { Text(value) },
    )
}

@Composable
private fun TransformingLazyColumnItemScope.OptionSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    spec: TransformationSpec,
    label: @Composable () -> Unit,
) {
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
