package com.patrickzedler.grocy.feature.chores.impl

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.patrickzedler.grocy.core.forms.DateKeyboardType
import com.patrickzedler.grocy.core.forms.DatePickerButton
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
import com.patrickzedler.grocy.core.resources.chore_code_unknown
import com.patrickzedler.grocy.core.resources.chore_date_required
import com.patrickzedler.grocy.core.resources.chore_done_by
import com.patrickzedler.grocy.core.resources.chore_field
import com.patrickzedler.grocy.core.resources.chore_overview
import com.patrickzedler.grocy.core.resources.chore_required
import com.patrickzedler.grocy.core.resources.chore_skip
import com.patrickzedler.grocy.core.resources.chore_tracked_time
import com.patrickzedler.grocy.core.resources.chore_tracking_title
import com.patrickzedler.grocy.core.resources.chores_track_failed
import com.patrickzedler.grocy.core.resources.chores_undone
import com.patrickzedler.grocy.core.resources.ic_rounded_check
import com.patrickzedler.grocy.core.resources.ic_rounded_fast_forward
import com.patrickzedler.grocy.core.resources.ic_rounded_info
import com.patrickzedler.grocy.core.resources.ic_rounded_vertical_align_top
import com.patrickzedler.grocy.core.resources.product_userfields
import com.patrickzedler.grocy.core.scanner.BarcodeScanner
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Grocy's "Chore tracking": the chore in its form order, Grocy's "OK" as FAB and "Skip" in the
 * toolbar, its message with "Undo" as snackbar, the chore's overview as dialog from the toolbar
 * once one is chosen, like the product overview of the stock pages.
 */
@Composable
internal fun ChoreTrackingScreen(
    viewModel: ChoreTrackingViewModel,
    menuItems: List<AppBarMenuItem>,
    isBesideList: Boolean,
    onBackClick: () -> Unit,
    overview: ChoreOverviewViewModel,
    overviewLinks: ChoreOverviewLinks,
    modifier: Modifier = Modifier,
) {
    val options by viewModel.options.collectAsStateWithLifecycle()
    val shownChore by viewModel.shownChore.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val scanner = viewModel.scanner
    var skipping by remember { mutableStateOf(false) }
    val formFocus = rememberFormFocus(listState) {
        val skipped = skipping
        skipping = false
        viewModel.track(skipped)
    }
    // The next chore starts at the top, also after tracking from the end of a long form
    val startsOver = scanner.isOpen || scanner.focusTarget == FormFocusTarget.CODE_FIELD
    LaunchedEffect(startsOver) {
        if (startsOver && listState.canScrollBackward) listState.animateScrollToItem(0)
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ChoreTrackingEvent.UnknownCode -> snackbarHostState.showSnackbar(
                    getString(Res.string.chore_code_unknown),
                )
                is ChoreTrackingEvent.Result -> when (val result = event.event) {
                    is ChoreEvent.Tracked ->
                        showTracked(result, snackbarHostState, viewModel::undo)
                    ChoreEvent.Undone ->
                        snackbarHostState.showSnackbar(getString(Res.string.chores_undone))
                    is ChoreEvent.Failed -> snackbarHostState.showSnackbar(
                        result.error.actionMessage(Res.string.chores_track_failed),
                    )
                    else -> Unit
                }
            }
        }
    }

    ChoreOverviewHost(overview, overviewLinks, snackbarHostState)

    CompositionLocalProvider(LocalFormFocus provides formFocus) {
        SegmentedListScreen(
            title = stringResource(Res.string.chore_tracking_title),
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
                    FloatingToolbarIconButton(
                        painter = painterResource(Res.drawable.ic_rounded_fast_forward),
                        contentDescription = stringResource(Res.string.chore_skip),
                        onClick = {
                            skipping = true
                            formFocus.submit()
                        },
                        enabled = viewModel.canSkip,
                    )
                    // Grocy's card, once a chore is chosen
                    val chore = shownChore
                    FloatingToolbarIconButton(
                        painter = painterResource(Res.drawable.ic_rounded_info),
                        contentDescription = stringResource(Res.string.chore_overview),
                        onClick = { chore?.let { overview.show(it.id, it.name) } },
                        enabled = chore != null,
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
                val choreFocus = remember { FocusRequester() }
                val timeFocus = remember { FocusRequester() }
                FocusRequestEffect(
                    scanner.focusTarget == FormFocusTarget.CODE_FIELD,
                    choreFocus,
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
                            label = stringResource(Res.string.chore_field),
                            options = current.chores.map { DropdownOption(it.id, it.name) },
                            selectedId = viewModel.choreId,
                            onSelectedIdChange = viewModel::selectChore,
                            onUnmatchedChange = { viewModel.isChoreUnmatched = it },
                            error = fieldErrorText(
                                viewModel.errors[ChoreTrackingField.CHORE],
                                Res.string.chore_required,
                            ),
                            index = index,
                            count = count,
                            focusRequester = choreFocus,
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
                        TextItem(
                            state = viewModel.trackedTime,
                            label = stringResource(Res.string.chore_tracked_time),
                            hint = if (viewModel.trackDateOnly) {
                                "YYYY-MM-DD"
                            } else {
                                "YYYY-MM-DD HH:MM:SS"
                            },
                            error = fieldErrorText(
                                viewModel.errors[ChoreTrackingField.TRACKED_TIME],
                                Res.string.chore_date_required,
                            ),
                            keyboardType = DateKeyboardType,
                            // Who did it is prefilled, what is left are the userfields
                            isLastField = !hasUserfields,
                            index = index,
                            count = count,
                            focusRequester = timeFocus,
                            trailingAction = {
                                DatePickerButton(
                                    viewModel.trackedTime,
                                    withTime = !viewModel.trackDateOnly,
                                )
                            },
                        )
                    },
                    if (current.users.isNotEmpty()) {
                        { index, count ->
                            DropdownItem(
                                label = stringResource(Res.string.chore_done_by),
                                options = current.users
                                    .map { DropdownOption(it.id, it.displayName) },
                                selectedId = viewModel.doneByUserId,
                                onSelectedIdChange = { viewModel.doneByUserId = it },
                                onUnmatchedChange = { viewModel.isDoneByUnmatched = it },
                                isLastField = !hasUserfields,
                                index = index,
                                count = count,
                            )
                        }
                    } else {
                        null
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
