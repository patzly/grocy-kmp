package com.patrickzedler.grocy.feature.batteries.impl

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.DropdownOption
import com.patrickzedler.grocy.core.design.component.FloatingToolbarIconButton
import com.patrickzedler.grocy.core.design.component.FloatingToolbarWithFab
import com.patrickzedler.grocy.core.design.component.FocusRequestEffect
import com.patrickzedler.grocy.core.design.component.LocalFormFocus
import com.patrickzedler.grocy.core.design.component.NavigateBackButton
import com.patrickzedler.grocy.core.design.component.ScreenPane
import com.patrickzedler.grocy.core.design.component.SegmentedListScreen
import com.patrickzedler.grocy.core.design.component.rememberFormFocus
import com.patrickzedler.grocy.core.design.component.segmentedGroup
import com.patrickzedler.grocy.core.design.component.showNewestSnackbar
import com.patrickzedler.grocy.core.forms.DateTextItem
import com.patrickzedler.grocy.core.forms.DropdownItem
import com.patrickzedler.grocy.core.forms.FormFocusTarget
import com.patrickzedler.grocy.core.forms.GroupItems
import com.patrickzedler.grocy.core.forms.ScannerToggleButton
import com.patrickzedler.grocy.core.forms.TextItem
import com.patrickzedler.grocy.core.forms.UserfieldItem
import com.patrickzedler.grocy.core.forms.actionMessage
import com.patrickzedler.grocy.core.forms.fieldErrorText
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_ok
import com.patrickzedler.grocy.core.resources.action_scroll_to_top
import com.patrickzedler.grocy.core.resources.batteries_track_failed
import com.patrickzedler.grocy.core.resources.batteries_undone
import com.patrickzedler.grocy.core.resources.battery_code_unknown
import com.patrickzedler.grocy.core.resources.battery_field
import com.patrickzedler.grocy.core.resources.battery_overview
import com.patrickzedler.grocy.core.resources.battery_required
import com.patrickzedler.grocy.core.resources.battery_tracking_title
import com.patrickzedler.grocy.core.resources.chore_date_required
import com.patrickzedler.grocy.core.resources.chore_tracked_time
import com.patrickzedler.grocy.core.resources.ic_rounded_check
import com.patrickzedler.grocy.core.resources.ic_rounded_info
import com.patrickzedler.grocy.core.resources.ic_rounded_vertical_align_top
import com.patrickzedler.grocy.core.resources.product_userfields
import com.patrickzedler.grocy.core.scanner.BarcodeScanner
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Grocy's "Battery tracking": the battery in its form order, Grocy's "OK" as FAB, its message with
 * "Undo" as snackbar, the battery's overview as dialog from the toolbar once one is chosen, like
 * the product overview of the stock pages.
 */
@Composable
internal fun BatteryTrackingScreen(
    viewModel: BatteryTrackingViewModel,
    menuItems: List<AppBarMenuItem>,
    isBesideList: Boolean,
    onBackClick: () -> Unit,
    overview: BatteryOverviewViewModel,
    overviewLinks: BatteryOverviewLinks,
    modifier: Modifier = Modifier,
) {
    val options by viewModel.options.collectAsStateWithLifecycle()
    val shownBattery by viewModel.shownBattery.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val scanner = viewModel.scanner
    val formFocus = rememberFormFocus(listState) { viewModel.track() }
    // The next battery starts at the top, also after tracking from the end of a long form
    val startsOver = scanner.isOpen || scanner.focusTarget == FormFocusTarget.CODE_FIELD
    LaunchedEffect(startsOver) {
        if (startsOver && listState.canScrollBackward) listState.animateScrollToItem(0)
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                BatteryTrackingEvent.UnknownCode -> snackbarHostState.showNewestSnackbar(
                    getString(Res.string.battery_code_unknown),
                )
                is BatteryTrackingEvent.Result -> when (val result = event.event) {
                    is BatteryEvent.Tracked ->
                        showTracked(result, snackbarHostState, viewModel::undo)
                    BatteryEvent.Undone ->
                        snackbarHostState.showNewestSnackbar(getString(Res.string.batteries_undone))
                    is BatteryEvent.Failed -> snackbarHostState.showNewestSnackbar(
                        result.error.actionMessage(Res.string.batteries_track_failed),
                    )
                    else -> Unit
                }
            }
        }
    }

    BatteryOverviewHost(overview, overviewLinks, snackbarHostState)

    CompositionLocalProvider(LocalFormFocus provides formFocus) {
        SegmentedListScreen(
            title = stringResource(Res.string.battery_tracking_title),
            modifier = modifier,
            navigationIcon = { if (!isBesideList) NavigateBackButton(onClick = onBackClick) },
            menuItems = menuItems,
            pane = ScreenPane.Detail.takeIf { isBesideList },
            listState = listState,
            snackbarHostState = snackbarHostState,
            floatingToolbar = { scrollBehavior, toolbarModifier ->
                FloatingToolbarWithFab(
                    scrollBehavior = scrollBehavior,
                    modifier = toolbarModifier,
                    fabPainter = painterResource(Res.drawable.ic_rounded_check),
                    fabDescription = stringResource(Res.string.action_ok),
                    onFabClick = formFocus::submit,
                ) {
                    // Grocy's card, once a battery is chosen
                    val battery = shownBattery
                    FloatingToolbarIconButton(
                        painter = painterResource(Res.drawable.ic_rounded_info),
                        contentDescription = stringResource(Res.string.battery_overview),
                        onClick = { battery?.let { overview.show(it.id, it.name) } },
                        enabled = battery != null,
                    )
                    FloatingToolbarIconButton(
                        painter = painterResource(Res.drawable.ic_rounded_vertical_align_top),
                        contentDescription = stringResource(Res.string.action_scroll_to_top),
                        onClick = { scope.launch { listState.animateScrollToItem(0) } },
                        enabled = listState.canScrollBackward,
                    )
                }
            },
        ) {
            val current = options ?: return@SegmentedListScreen
            segmentedGroup(key = "form") {
                val batteryFocus = remember { FocusRequester() }
                val timeFocus = remember { FocusRequester() }
                FocusRequestEffect(
                    scanner.focusTarget == FormFocusTarget.CODE_FIELD,
                    batteryFocus,
                    scanner::onFocusHandled,
                )
                FocusRequestEffect(
                    scanner.focusTarget == FormFocusTarget.NEXT_FIELD,
                    timeFocus,
                    scanner::onFocusHandled,
                )
                val hasUserfields = viewModel.userfields.isNotEmpty()
                GroupItems(
                    { index, count ->
                        DropdownItem(
                            label = stringResource(Res.string.battery_field),
                            options = current.map { DropdownOption(it.id, it.name) },
                            selectedId = viewModel.batteryId,
                            onSelectedIdChange = viewModel::selectBattery,
                            onUnmatchedChange = { viewModel.isBatteryUnmatched = it },
                            error = fieldErrorText(
                                viewModel.errors[BatteryTrackingField.BATTERY],
                                Res.string.battery_required,
                            ),
                            index = index,
                            count = count,
                            focusRequester = batteryFocus,
                            onSubmitText = { text, _ -> viewModel.onTextSubmitted(text) },
                            onKeyboardNext = viewModel::goToTrackedTime,
                            onFocusChange = scanner::onFieldFocusChange,
                            below = if (scanner.isOpen) {
                                {
                                    BarcodeScanner(
                                        onBarcodeScanned = { viewModel.onScanned(it.value) },
                                        shape = MaterialTheme.shapes.small,
                                        containerColor =
                                            MaterialTheme.colorScheme.surfaceContainerHighest,
                                    )
                                }
                            } else {
                                null
                            },
                            trailingAction = {
                                ScannerToggleButton(
                                    isScannerOpen = scanner.isOpen,
                                    onClick = scanner::toggle,
                                )
                            },
                        )
                    },
                    { index, count ->
                        DateTextItem(
                            withTime = true,
                            state = viewModel.trackedTime,
                            label = stringResource(Res.string.chore_tracked_time),
                            error = fieldErrorText(
                                viewModel.errors[BatteryTrackingField.TRACKED_TIME],
                                Res.string.chore_date_required,
                            ),
                            isLastField = !hasUserfields,
                            index = index,
                            count = count,
                            focusRequester = timeFocus,
                        )
                    },
                )
            }
            if (viewModel.userfields.isNotEmpty()) {
                segmentedGroup(key = "userfields", title = Res.string.product_userfields) {
                    viewModel.userfields.forEachIndexed { index, input ->
                        UserfieldItem(
                            input = input,
                            error = viewModel.userfieldErrors[input.userfield.name],
                            index = index,
                            count = viewModel.userfields.size,
                            isLastField = index == viewModel.userfields.lastIndex,
                        )
                    }
                }
            }
        }
    }
}
