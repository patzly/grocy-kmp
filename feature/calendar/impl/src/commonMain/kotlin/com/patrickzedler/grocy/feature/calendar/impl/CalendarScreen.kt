@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class)

package com.patrickzedler.grocy.feature.calendar.impl

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
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
import com.patrickzedler.grocy.core.design.component.showNewestSnackbar
import com.patrickzedler.grocy.core.forms.actionMessage
import com.patrickzedler.grocy.core.model.calendar.CalendarEvent
import com.patrickzedler.grocy.core.model.calendar.CalendarView
import com.patrickzedler.grocy.core.model.recipes.DAYS_PER_WEEK
import com.patrickzedler.grocy.core.model.text.firstDayOfMonth
import com.patrickzedler.grocy.core.model.text.formatMonthRange
import com.patrickzedler.grocy.core.model.text.formatMonthYearShort
import com.patrickzedler.grocy.core.model.text.todayEpochDay
import com.patrickzedler.grocy.core.productcard.LabelPrintEffect
import com.patrickzedler.grocy.core.productcard.ProductOverviewHost
import com.patrickzedler.grocy.core.productcard.StockPageActions
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.calendar_configure_colors
import com.patrickzedler.grocy.core.resources.calendar_next
import com.patrickzedler.grocy.core.resources.calendar_now
import com.patrickzedler.grocy.core.resources.calendar_previous
import com.patrickzedler.grocy.core.resources.calendar_share
import com.patrickzedler.grocy.core.resources.calendar_title
import com.patrickzedler.grocy.core.resources.calendar_view_day
import com.patrickzedler.grocy.core.resources.calendar_view_list
import com.patrickzedler.grocy.core.resources.calendar_view_month
import com.patrickzedler.grocy.core.resources.calendar_view_week
import com.patrickzedler.grocy.core.resources.calendar_week_number
import com.patrickzedler.grocy.core.resources.data_load_failed
import com.patrickzedler.grocy.core.resources.data_save_failed
import com.patrickzedler.grocy.core.resources.ic_rounded_arrow_drop_down
import com.patrickzedler.grocy.core.resources.ic_rounded_chevron_left
import com.patrickzedler.grocy.core.resources.ic_rounded_chevron_right
import com.patrickzedler.grocy.core.resources.ic_rounded_schedule
import com.patrickzedler.grocy.core.resources.ic_rounded_today
import com.patrickzedler.grocy.core.resources.meal_plan_today
import com.patrickzedler.grocy.core.ui.RefreshErrorEffect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Grocy's "Calendar" in the established form of Google Calendar: its views (day, week, month,
 * list) in the toolbar's menu, the period in the subtitle. Day, week and month page sideways
 * through their periods, by swiping or the toolbar's arrows; what heads them stays while below it
 * scrolls. Changing the view moves what both show (events, days) into its new place. Grocy's
 * events have no duration, so all of them are chips like Google's tasks. A tap on an event opens
 * what it is about, a product's overview over the calendar.
 */
@Composable
internal fun CalendarScreen(
    viewModel: CalendarViewModel,
    stockPageActions: StockPageActions,
    initialProductId: Int?,
    onEventClick: (CalendarEvent) -> Unit,
    menuItems: List<AppBarMenuItem>,
    onBackClick: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refresh by viewModel.refresh.collectAsStateWithLifecycle()
    val sharingLink by viewModel.sharingLink.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val current = state
    val view = current?.view ?: CalendarView.Default
    var showsColors by rememberSaveable { mutableStateOf(false) }

    val density = LocalDensity.current
    val windowWidth = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }
    val hourHeight = with(density) { HourHeight.toPx() }
    val gridScroll = rememberScrollState()
    val listState = rememberLazyListState()
    // The pages of every view count from this day, which only has to stay the same
    val base = rememberSaveable { todayEpochDay() }
    val pagers = remember { CalendarPagers() }
    val nowMinute = rememberCurrentMinute()
    val listRows = remember(current, nowMinute) {
        current?.let { listRowsOf(it, nowMinute) }.orEmpty()
    }

    val actions = stockPageActions
    val openEvent: (CalendarEvent) -> Unit = { event ->
        val productId = event.productId
        if (productId != null) actions.overview.show(productId) else onEventClick(event)
    }
    InitialProductEffect(initialProductId, actions)
    LabelPrintEffect(actions.labels, snackbarHostState)
    RefreshErrorEffect(
        error = refresh.error,
        hasData = current != null && current.eventsByDay.isNotEmpty(),
        snackbarHostState = snackbarHostState,
        onErrorShown = viewModel::onErrorShown,
    )
    CalendarEventEffect(viewModel, snackbarHostState)
    GridStartEffect(view, current, gridScroll, hourHeight)
    ListStartEffect(view, current, listRows, listState)

    sharingLink?.let { link -> CalendarSharingDialog(link, onDismiss = viewModel::closeSharing) }
    if (showsColors && current != null) {
        CalendarColorsDialog(
            state = current,
            onColorChange = viewModel::saveColor,
            onDismiss = { showsColors = false },
        )
    }

    // Grocy's "Today", the current time in the day: scrolled to with the page turned to it
    val showNow: () -> Unit = {
        viewModel.showToday()
        scope.launch {
            when {
                view.isTimeGrid ->
                    gridScroll.animateScrollTo(gridOffsetOf(currentMinuteOfDay(), hourHeight))
                view == CalendarView.LIST -> current?.let {
                    listState.animateScrollToItem(listIndexOf(listRows, it.today))
                }
                else -> Unit
            }
        }
    }
    val step: (Int) -> Unit = { steps ->
        pagers.current?.let { pager ->
            scope.launch { pager.animateScrollToPage(pager.currentPage + steps) }
        }
    }

    SegmentedListScreen(
        title = stringResource(Res.string.calendar_title),
        subtitle = current?.let { periodSubtitle(it, view, listRows, listState) },
        navigationIcon = { NavigateBackButton(onClick = onBackClick) },
        actionMenuItems = listOf(
            AppBarMenuItem(
                text = stringResource(Res.string.calendar_share),
                onClick = viewModel::openSharing,
            ),
            AppBarMenuItem(stringResource(Res.string.calendar_configure_colors)) {
                showsColors = true
            },
        ),
        menuItems = menuItems,
        listState = listState,
        snackbarHostState = snackbarHostState,
        isRefreshing = refresh.isRefreshing,
        onRefresh = viewModel::refresh,
        floatingToolbar = { scrollBehavior, toolbarModifier ->
            val isDay = view == CalendarView.DAY
            FloatingToolbarWithFab(
                scrollBehavior = scrollBehavior,
                modifier = toolbarModifier,
                fabPainter = painterResource(
                    if (isDay) Res.drawable.ic_rounded_schedule else Res.drawable.ic_rounded_today,
                ),
                fabDescription = stringResource(
                    if (isDay) Res.string.calendar_now else Res.string.meal_plan_today,
                ),
                onFabClick = showNow,
            ) {
                // The list goes on by scrolling, it has no periods to step through
                AnimatedVisibility(
                    visible = view.isPaged,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally(),
                ) {
                    FloatingToolbarIconButton(
                        painter = painterResource(Res.drawable.ic_rounded_chevron_left),
                        contentDescription = stringResource(Res.string.calendar_previous),
                        onClick = { step(-1) },
                    )
                }
                ViewMenu(view, onViewChange = viewModel::chooseView)
                AnimatedVisibility(
                    visible = view.isPaged,
                    enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
                    exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start),
                ) {
                    FloatingToolbarIconButton(
                        painter = painterResource(Res.drawable.ic_rounded_chevron_right),
                        contentDescription = stringResource(Res.string.calendar_next),
                        onClick = { step(1) },
                    )
                }
            }
        },
        body = { padding ->
            if (current == null) return@SegmentedListScreen
            val colors = EventColors(current)
            val bottom = padding.content.calculateBottomPadding()
            val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
            SharedTransitionLayout(Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = view,
                    transitionSpec = { fadeIn(effects) togetherWith fadeOut(effects) },
                    label = "calendar view",
                ) { shown ->
                    val transition = CalendarTransition(this@SharedTransitionLayout, this)
                    CompositionLocalProvider(LocalCalendarTransition provides transition) {
                        Box(Modifier.fillMaxSize().padding(top = padding.top)) {
                            if (shown == CalendarView.LIST) {
                                CalendarList(
                                    rows = listRows,
                                    listState = listState,
                                    contentPadding = PaddingValues(top = 8.dp, bottom = bottom),
                                    colors = colors,
                                    onDayClick = viewModel::openDay,
                                    onEventClick = openEvent,
                                )
                            } else {
                                PagedView(
                                    view = shown,
                                    state = current,
                                    base = base,
                                    pagers = pagers,
                                    gridScroll = gridScroll,
                                    bottomPadding = bottom,
                                    showsIcons = windowWidth >= ChipIconMinWindowWidth,
                                    colors = colors,
                                    onShowDay = viewModel::showDay,
                                    onDayClick = viewModel::openDay,
                                    onEventClick = openEvent,
                                )
                            }
                        }
                    }
                }
            }
        },
    )

    ProductOverviewHost(
        overview = actions.overview,
        shoppingList = actions.shoppingList,
        labels = actions.labels,
        snackbarHostState = snackbarHostState,
        links = actions.links,
        nameOf = { id ->
            current?.eventsByDay?.values?.flatten()
                ?.firstOrNull { it.productId == id }?.name.orEmpty()
        },
    )
}

/** The pager of the shown view, which the toolbar's arrows turn. */
@Stable
private class CalendarPagers {
    var current: PagerState? by mutableStateOf(null)
}

/**
 * A view that pages through its periods, each page standing on a day counted from [base]. A
 * settled page moves the calendar to its day ([onShowDay]); a day chosen elsewhere (today, a tap)
 * turns the pages there with their animation.
 */
@Composable
private fun PagedView(
    view: CalendarView,
    state: CalendarUiState,
    base: Long,
    pagers: CalendarPagers,
    gridScroll: ScrollState,
    bottomPadding: Dp,
    showsIcons: Boolean,
    colors: EventColors,
    onShowDay: (Long) -> Unit,
    onDayClick: (Long) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
) {
    val firstDayOfWeek = state.firstDayOfWeek
    val target = pageOfDay(view, base, state.anchor, firstDayOfWeek)
    val pagerState = rememberPagerState(initialPage = target) { PageCount }
    val isShown by rememberUpdatedState(view == state.view)
    LaunchedEffect(pagerState, isShown) {
        if (isShown) pagers.current = pagerState
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.drop(1).collect { page ->
            if (isShown) onShowDay(dayOfPage(view, base, page))
        }
    }
    LaunchedEffect(target) {
        if (isShown && pagerState.settledPage != target && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(target)
        }
    }
    HorizontalPager(
        state = pagerState,
        key = { it },
        modifier = Modifier.fillMaxSize(),
    ) { page ->
        val day = dayOfPage(view, base, page)
        when (view) {
            CalendarView.DAY, CalendarView.WEEK -> TimeGridPage(
                days = state.gridDays(view, day),
                scroll = gridScroll,
                bottomPadding = bottomPadding,
                colors = colors,
                onDayClick = onDayClick.takeIf { view == CalendarView.WEEK },
                onEventClick = onEventClick,
            )
            CalendarView.MONTH -> MonthPage(
                weeks = state.monthWeeks(day),
                showsNumbers = state.config.calendarShowWeekOfYear,
                showsIcons = showsIcons,
                bottomPadding = bottomPadding,
                colors = colors,
                onDayClick = onDayClick,
                onEventClick = onEventClick,
            )
            CalendarView.LIST -> Unit
        }
    }
}

/** Google's view button, with the toolbar's color: the current view, a tap offers all of them. */
@Composable
private fun ViewMenu(view: CalendarView, onViewChange: (CalendarView) -> Unit) {
    val haptic = LocalHapticFeedback.current
    var isOpen by remember { mutableStateOf(false) }
    val items = CalendarView.entries.map { option ->
        AppBarMenuItem(stringResource(option.label), selected = option == view) {
            onViewChange(option)
        }
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
        // Right above the toolbar, whose bar reaches 8.dp above this button's touch target
        OverflowMenuPopup(
            expanded = isOpen,
            groups = listOf(items),
            onDismissRequest = { isOpen = false },
            above = true,
            offset = DpOffset(x = 0.dp, y = -MenuToolbarGap),
        )
    }
}

private val CalendarView.label: StringResource
    get() = when (this) {
        CalendarView.DAY -> Res.string.calendar_view_day
        CalendarView.WEEK -> Res.string.calendar_view_week
        CalendarView.MONTH -> Res.string.calendar_view_month
        CalendarView.LIST -> Res.string.calendar_view_list
    }

/**
 * The period below the title "Calendar", short: the month ("Nov 2026") or the months of a week,
 * with Grocy's `CALENDAR_SHOW_WEEK_OF_YEAR` the week's number; the list's at its top.
 */
@Composable
private fun periodSubtitle(
    state: CalendarUiState,
    view: CalendarView,
    listRows: List<ListRow>,
    listState: LazyListState,
): String {
    val rows by rememberUpdatedState(listRows)
    val listTop by remember(listState) {
        derivedStateOf { rows.getOrNull(listState.firstVisibleItemIndex)?.epochDay }
    }
    val week = stringResource(Res.string.calendar_week_number, state.weekNumberOf(state.anchor))
        .takeIf { state.config.calendarShowWeekOfYear }
    return when (view) {
        CalendarView.DAY -> listOfNotNull(formatMonthYearShort(state.anchor), week)
        CalendarView.WEEK -> {
            val start = state.weekStartOf(state.anchor)
            listOfNotNull(monthsTitle(start, start + DAYS_PER_WEEK - 1), week)
        }
        CalendarView.MONTH -> listOf(formatMonthYearShort(state.anchor))
        CalendarView.LIST -> listOf(formatMonthYearShort(listTop ?: state.today))
    }.joinToString(" · ")
}

private fun monthsTitle(start: Long, end: Long): String =
    if (firstDayOfMonth(start) == firstDayOfMonth(end)) {
        formatMonthYearShort(start)
    } else {
        formatMonthRange(start, end)
    }

/**
 * The time grid opens around now on today, else at the start of a working day, once: afterwards
 * every page and both grid views keep the time the user scrolled to, like Google Calendar.
 */
@Composable
private fun GridStartEffect(
    view: CalendarView,
    state: CalendarUiState?,
    scroll: ScrollState,
    hourHeight: Float,
) {
    var isPlaced by rememberSaveable { mutableStateOf(false) }
    val isGrid = state != null && view.isTimeGrid
    LaunchedEffect(isGrid) {
        if (!isGrid || isPlaced || state == null) return@LaunchedEffect
        val minute = if (state.anchor == state.today) currentMinuteOfDay() else DefaultGridMinute
        // Its height is known once laid out
        snapshotFlow { scroll.maxValue }.first { it > 0 && it != Int.MAX_VALUE }
        scroll.scrollTo(gridOffsetOf(minute, hourHeight))
        isPlaced = true
    }
}

/** The list opens at the day the calendar stands on, today unless another one was shown. */
@Composable
private fun ListStartEffect(
    view: CalendarView,
    state: CalendarUiState?,
    rows: List<ListRow>,
    listState: LazyListState,
) {
    val isReady = view == CalendarView.LIST && state != null && rows.isNotEmpty()
    LaunchedEffect(isReady) {
        if (isReady && state != null) listState.scrollToItem(listIndexOf(rows, state.anchor))
    }
}

/** The overview of the product a widget's event is about, opened once over the calendar. */
@Composable
private fun InitialProductEffect(productId: Int?, actions: StockPageActions) {
    var isShown by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(productId) {
        if (productId != null && !isShown) {
            actions.overview.show(productId)
            isShown = true
        }
    }
}

@Composable
private fun CalendarEventEffect(
    viewModel: CalendarViewModel,
    snackbarHostState: SnackbarHostState,
) {
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

private const val DefaultGridMinute = 8 * MINUTES_PER_HOUR

private val ChipIconMinWindowWidth = 840.dp

// The toolbar's 64.dp bar around this 48.dp touch target, then Material's 8.dp to the menu
private val MenuToolbarGap = 16.dp
