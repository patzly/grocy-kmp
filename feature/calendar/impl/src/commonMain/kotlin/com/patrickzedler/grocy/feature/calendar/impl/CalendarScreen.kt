@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.patrickzedler.grocy.feature.calendar.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.FloatingToolbarIconButton
import com.patrickzedler.grocy.core.design.component.FloatingToolbarWithFab
import com.patrickzedler.grocy.core.design.component.NavigateBackButton
import com.patrickzedler.grocy.core.design.component.OverflowMenuPopup
import com.patrickzedler.grocy.core.design.component.SegmentedListScreen
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.insetItem
import com.patrickzedler.grocy.core.design.component.showNewestSnackbar
import com.patrickzedler.grocy.core.forms.actionMessage
import com.patrickzedler.grocy.core.model.calendar.CalendarEvent
import com.patrickzedler.grocy.core.model.calendar.CalendarView
import com.patrickzedler.grocy.core.model.recipes.DAYS_PER_WEEK
import com.patrickzedler.grocy.core.model.text.firstDayOfMonth
import com.patrickzedler.grocy.core.model.text.formatMonthRange
import com.patrickzedler.grocy.core.model.text.formatMonthYear
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.calendar_configure_colors
import com.patrickzedler.grocy.core.resources.calendar_next
import com.patrickzedler.grocy.core.resources.calendar_previous
import com.patrickzedler.grocy.core.resources.calendar_share
import com.patrickzedler.grocy.core.resources.calendar_title
import com.patrickzedler.grocy.core.resources.calendar_view_day
import com.patrickzedler.grocy.core.resources.calendar_view_month
import com.patrickzedler.grocy.core.resources.calendar_view_schedule
import com.patrickzedler.grocy.core.resources.calendar_view_week
import com.patrickzedler.grocy.core.resources.calendar_week_number
import com.patrickzedler.grocy.core.resources.data_load_failed
import com.patrickzedler.grocy.core.resources.data_save_failed
import com.patrickzedler.grocy.core.resources.ic_rounded_arrow_drop_down
import com.patrickzedler.grocy.core.resources.ic_rounded_chevron_left
import com.patrickzedler.grocy.core.resources.ic_rounded_chevron_right
import com.patrickzedler.grocy.core.resources.ic_rounded_today
import com.patrickzedler.grocy.core.resources.meal_plan_today
import com.patrickzedler.grocy.core.ui.RefreshErrorEffect
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Grocy's "Calendar" in the established form of Google Calendar: the period as title, its views
 * (day, week, month, schedule) in the toolbar's menu. Grocy's events have no duration, so all of them are one line chips, at their
 * time in the time grid and above it without one.
 */
@Composable
internal fun CalendarScreen(
    viewModel: CalendarViewModel,
    onEventClick: (CalendarEvent) -> Unit,
    menuItems: List<AppBarMenuItem>,
    onBackClick: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refresh by viewModel.refresh.collectAsStateWithLifecycle()
    val sharingLink by viewModel.sharingLink.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val current = state
    var showsColors by rememberSaveable { mutableStateOf(false) }

    val density = LocalDensity.current
    val windowSize = LocalWindowInfo.current.containerSize
    val windowWidth = with(density) { windowSize.width.toDp() }
    val windowHeight = with(density) { windowSize.height.toDp() }
    val view = current?.chosenView ?: defaultCalendarView(windowWidth)

    RefreshErrorEffect(
        error = refresh.error,
        hasData = current != null && current.eventsByDay.isNotEmpty(),
        snackbarHostState = snackbarHostState,
        onErrorShown = viewModel::onErrorShown,
    )
    CalendarEventEffect(viewModel, snackbarHostState)
    ViewScrollEffect(current, view, listState)

    sharingLink?.let { link -> CalendarSharingDialog(link, onDismiss = viewModel::closeSharing) }
    if (showsColors && current != null) {
        CalendarColorsDialog(
            state = current,
            onColorChange = viewModel::saveColor,
            onDismiss = { showsColors = false },
        )
    }

    SegmentedListScreen(
        title = current?.let { periodTitle(it, view) } ?: stringResource(Res.string.calendar_title),
        subtitle = current?.takeIf { view.isTimeGrid && it.config.calendarShowWeekOfYear }?.let {
            stringResource(Res.string.calendar_week_number, it.weekNumber)
        },
        navigationIcon = { NavigateBackButton(onClick = onBackClick) },
        actionMenuItems = listOf(
            AppBarMenuItem(stringResource(Res.string.calendar_share), onClick = viewModel::openSharing),
            AppBarMenuItem(stringResource(Res.string.calendar_configure_colors)) { showsColors = true },
        ),
        menuItems = menuItems,
        listState = listState,
        snackbarHostState = snackbarHostState,
        isRefreshing = refresh.isRefreshing,
        onRefresh = viewModel::refresh,
        itemSpacing = ListItemDefaults.SegmentedGap,
        floatingToolbar = { scrollBehavior, toolbarModifier ->
            FloatingToolbarWithFab(
                scrollBehavior = scrollBehavior,
                modifier = toolbarModifier,
                fabPainter = painterResource(Res.drawable.ic_rounded_today),
                fabDescription = stringResource(Res.string.meal_plan_today),
                onFabClick = viewModel::showToday,
            ) {
                FloatingToolbarIconButton(
                    painter = painterResource(Res.drawable.ic_rounded_chevron_left),
                    contentDescription = stringResource(Res.string.calendar_previous),
                    onClick = { viewModel.step(view, -1) },
                )
                ViewMenu(view, onViewChange = viewModel::chooseView)
                FloatingToolbarIconButton(
                    painter = painterResource(Res.drawable.ic_rounded_chevron_right),
                    contentDescription = stringResource(Res.string.calendar_next),
                    onClick = { viewModel.step(view, 1) },
                )
            }
        },
    ) {
        if (current == null) return@SegmentedListScreen
        val colors = EventColors(current)
        when (view) {
            CalendarView.DAY, CalendarView.WEEK -> timeGrid(
                days = current.gridDays(view),
                listState = listState,
                colors = colors,
                onDayClick = viewModel::openDay,
                onEventClick = onEventClick,
            )
            CalendarView.MONTH -> monthGrid(
                state = current,
                windowHeight = windowHeight,
                showsIcons = windowWidth >= ChipIconMinWindowWidth,
                colors = colors,
                onDayClick = viewModel::openDay,
                onEventClick = onEventClick,
            )
            CalendarView.SCHEDULE -> schedule(
                days = current.scheduleDays,
                isWide = windowWidth >= ScheduleWideWidth,
                colors = colors,
                onEventClick = onEventClick,
            )
        }
    }
}

/** Google's view button, with the toolbar's color: the current view, a tap offers the others. */
@Composable
private fun ViewMenu(view: CalendarView, onViewChange: (CalendarView) -> Unit) {
    val haptic = LocalHapticFeedback.current
    var isOpen by remember { mutableStateOf(false) }
    val items = CalendarView.entries.map { option ->
        AppBarMenuItem(stringResource(option.label)) { onViewChange(option) }
    }
    Box {
        TextButton(
            onClick = {
                haptic.confirm()
                isOpen = true
            },
            modifier = Modifier.clickCursor(),
            shapes = ButtonDefaults.shapes(),
            colors = ButtonDefaults.textButtonColors(contentColor = LocalContentColor.current),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(view.label), style = MaterialTheme.typography.labelLarge)
                Icon(
                    painter = painterResource(Res.drawable.ic_rounded_arrow_drop_down),
                    contentDescription = null,
                )
            }
        }
        OverflowMenuPopup(
            expanded = isOpen,
            groups = listOf(items),
            onDismissRequest = { isOpen = false },
        )
    }
}

private val CalendarView.label: StringResource
    get() = when (this) {
        CalendarView.DAY -> Res.string.calendar_view_day
        CalendarView.WEEK -> Res.string.calendar_view_week
        CalendarView.MONTH -> Res.string.calendar_view_month
        CalendarView.SCHEDULE -> Res.string.calendar_view_schedule
    }

/** Google's title: the month or the months a period spans. */
private fun periodTitle(state: CalendarUiState, view: CalendarView): String = when (view) {
    CalendarView.WEEK -> monthsTitle(state.weekStart, state.weekStart + DAYS_PER_WEEK - 1)
    CalendarView.DAY, CalendarView.MONTH, CalendarView.SCHEDULE -> formatMonthYear(state.anchor)
}

private fun monthsTitle(start: Long, end: Long): String =
    if (firstDayOfMonth(start) == firstDayOfMonth(end)) {
        formatMonthYear(start)
    } else {
        formatMonthRange(start, end)
    }

/**
 * Where the list stands: at the time grid's working hours, around now on a day that has it, when
 * its view opens, and it stays there when going on like in Google Calendar; other views at the top
 * of each period.
 */
@Composable
private fun ViewScrollEffect(
    state: CalendarUiState?,
    view: CalendarView,
    listState: LazyListState,
) {
    val density = LocalDensity.current
    val period = state?.takeUnless { view.isTimeGrid }?.anchor?.let(::firstDayOfMonth)
    val isLoaded = state != null
    val daysOfGrid = state?.takeIf { view.isTimeGrid }?.gridDays(view)
    LaunchedEffect(view, period, isLoaded) {
        if (!isLoaded) return@LaunchedEffect
        if (daysOfGrid == null) {
            listState.scrollToItem(0)
            return@LaunchedEffect
        }
        val minute = if (daysOfGrid.any { it.isToday }) currentMinuteOfDay() else DefaultGridMinute
        val top = (minute - GridLeadMinutes).coerceAtLeast(0) * HourHeight.value / MINUTES_PER_HOUR
        val offset = with(density) { top.dp.roundToPx() }
        // The sticky header of the days, then the hours
        listState.scrollToItem(1, offset)
    }
}

@Composable
private fun CalendarEventEffect(viewModel: CalendarViewModel, snackbarHostState: SnackbarHostState) {
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            snackbarHostState.showNewestSnackbar(
                when (event) {
                    is CalendarEvents.SharingFailed ->
                        event.error.actionMessage(Res.string.data_load_failed)
                    is CalendarEvents.ColorFailed ->
                        event.error.actionMessage(Res.string.data_save_failed)
                },
            )
        }
    }
}

private const val DefaultGridMinute = 7 * MINUTES_PER_HOUR
private const val GridLeadMinutes = MINUTES_PER_HOUR

private val ChipIconMinWindowWidth = 840.dp
private val ScheduleWideWidth = 600.dp
