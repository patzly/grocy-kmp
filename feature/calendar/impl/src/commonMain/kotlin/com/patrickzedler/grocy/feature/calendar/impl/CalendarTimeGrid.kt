package com.patrickzedler.grocy.feature.calendar.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.withTabularNumbers
import com.patrickzedler.grocy.core.model.calendar.CalendarEvent
import com.patrickzedler.grocy.core.model.text.dayOfMonthOf
import com.patrickzedler.grocy.core.model.text.formatDate
import com.patrickzedler.grocy.core.model.text.formatWeekday
import com.patrickzedler.grocy.core.model.text.formatWeekdayShort
import com.patrickzedler.grocy.core.model.text.localNowMillis
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.calendar_more
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.pluralStringResource

/**
 * Google Calendar's time grid: the days stay on top with what has no time, below them the hours as
 * segmented rows with their time beside them and each event as a chip at its time.
 */
internal fun LazyListScope.timeGrid(
    days: List<CalendarDay>,
    listState: LazyListState,
    colors: EventColors,
    onDayClick: (Long) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
) {
    stickyHeader(key = DAYS_KEY, contentType = DAYS_KEY) {
        TimeGridHeader(
            days = days,
            colors = colors,
            onDayClick = onDayClick,
            onEventClick = onEventClick,
            modifier = Modifier.belowContentPadding(listState),
        )
    }
    item(key = "hours", contentType = "hours") {
        TimeGridBody(days, colors, onEventClick)
    }
}

/**
 * A sticky header sticks to the list's edge, which lies below the app bar: this moves it down by as
 * much of the list's padding as it has passed, without changing its height and so the list.
 */
private fun Modifier.belowContentPadding(listState: LazyListState): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            val info = listState.layoutInfo
            val offset = info.visibleItemsInfo.firstOrNull { it.key == DAYS_KEY }?.offset ?: 0
            // The list's own space above its first item is no part of what covers it
            val padding = info.beforeContentPadding - SegmentedListDefaults.GroupSpacing.roundToPx()
            placeable.placeRelative(0, stickyShift(offset, padding.coerceAtLeast(0)))
        }
    }

/** The days' names and numbers, today's on a filled circle, and what has no time. */
@Composable
private fun TimeGridHeader(
    days: List<CalendarDay>,
    colors: EventColors,
    onDayClick: (Long) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val isSingle = days.size == 1
    val scrollState = rememberScrollState()
    Column(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(bottom = 4.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(HourLabelWidth))
            days.forEach { day ->
                DayHeading(
                    day = day,
                    alignment = if (isSingle) Alignment.Start else Alignment.CenterHorizontally,
                    onClick = if (isSingle) {
                        null
                    } else {
                        {
                            haptic.confirm()
                            onDayClick(day.epochDay)
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        // Expanded it may hold more than fits, the hours keep their place below it
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(max = AllDayMaxHeight)
                .verticalScroll(scrollState),
        ) {
            Spacer(Modifier.width(HourLabelWidth))
            days.forEach { day ->
                val wholeDay = day.events.filter { minuteOfTime(it.time) == null }
                val limit = if (isExpanded) wholeDay.size else AllDayRows
                val shown = shownChips(wholeDay.size, limit)
                BoxWithConstraints(Modifier.weight(1f).padding(end = ColumnGap)) {
                    val showsIcon = maxWidth >= ChipIconMinWidth
                    Column(verticalArrangement = Arrangement.spacedBy(ChipGap)) {
                        wholeDay.take(shown).forEach { event ->
                            EventChip(event, event.name, colors, showsIcon, onEventClick)
                        }
                        if (shown < wholeDay.size) {
                            MoreChip(wholeDay.size - shown) {
                                haptic.confirm()
                                isExpanded = true
                            }
                        }
                    }
                }
            }
        }
    }
}

/** "3 more" of a place that does not show all its events. */
@Composable
internal fun MoreChip(count: Int, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .fillMaxWidth()
            .height(ChipHeight)
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .clickCursor(),
    ) {
        Text(
            text = pluralStringResource(Res.plurals.calendar_more, count, count),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

@Composable
private fun DayHeading(
    day: CalendarDay,
    alignment: Alignment.Horizontal,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val description = "${formatWeekday(day.epochDay)}, ${formatDate(day.epochDay)}"
    Column(
        horizontalAlignment = alignment,
        modifier = modifier
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick).clickCursor() else Modifier,
            )
            .padding(vertical = 4.dp)
            .semantics(mergeDescendants = true) { heading() },
    ) {
        Text(
            text = formatWeekdayShort(day.epochDay),
            style = MaterialTheme.typography.labelMedium,
            color = if (day.isToday) scheme.primary else scheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.clearAndSetSemantics { },
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(DayNumberSize)
                .clip(CircleShape)
                .then(if (day.isToday) Modifier.background(scheme.primary) else Modifier),
        ) {
            Text(
                text = dayOfMonthOf(day.epochDay).toString(),
                style = MaterialTheme.typography.titleLarge.withTabularNumbers(),
                color = if (day.isToday) scheme.onPrimary else scheme.onSurface,
                modifier = Modifier.semantics { contentDescription = description },
            )
        }
    }
}

/** The hours, the columns' lines and each day's chips at their times. */
@Composable
private fun TimeGridBody(
    days: List<CalendarDay>,
    colors: EventColors,
    onEventClick: (CalendarEvent) -> Unit,
) {
    val height = HourHeight * HOURS_PER_DAY
    Box(Modifier.fillMaxWidth().height(height)) {
        Column(Modifier.fillMaxWidth()) {
            repeat(HOURS_PER_DAY) { hour ->
                HourRow(hour, columns = days.size)
            }
        }
        Row(Modifier.fillMaxWidth().height(height).padding(start = HourLabelWidth)) {
            days.forEach { day ->
                BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
                    DayEvents(
                        day = day,
                        showsIcon = maxWidth >= ChipIconMinWidth,
                        colors = colors,
                        onEventClick = onEventClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun HourRow(hour: Int, columns: Int) {
    Row(Modifier.fillMaxWidth().height(HourHeight)) {
        Box(Modifier.width(HourLabelWidth)) {
            if (hour > 0) {
                Text(
                    text = "${hour.toString().padStart(2, '0')}:00",
                    style = MaterialTheme.typography.labelMedium.withTabularNumbers(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = HourLabelLift)
                        .padding(end = 8.dp),
                )
            }
        }
        Surface(
            shape = SegmentedListDefaults.shape(index = hour, count = HOURS_PER_DAY),
            color = SegmentedListDefaults.containerColor,
            modifier = Modifier
                .weight(1f)
                .height(HourHeight - ListItemDefaults.SegmentedGap),
        ) {
            ColumnLines(columns)
        }
    }
}

/** The lines between days, in a row of the grid or the month. */
@Composable
internal fun ColumnLines(columns: Int) {
    Row(Modifier.fillMaxWidth().fillMaxHeight()) {
        repeat(columns) { index ->
            Box(Modifier.weight(1f).fillMaxHeight()) {
                if (index > 0) {
                    VerticalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
                }
            }
        }
    }
}

@Composable
private fun DayEvents(
    day: CalendarDay,
    showsIcon: Boolean,
    colors: EventColors,
    onEventClick: (CalendarEvent) -> Unit,
) {
    val timed = day.events.mapNotNull { event -> minuteOfTime(event.time)?.let { it to event } }
    val tops = remember(timed) {
        chipTops(
            minutes = timed.map { it.first },
            minuteHeight = HourHeight.value / MINUTES_PER_HOUR,
            chipHeight = (ChipHeight + ChipGap).value,
            totalHeight = (HourHeight * HOURS_PER_DAY).value,
        )
    }
    timed.forEachIndexed { index, (_, event) ->
        EventChip(
            event = event,
            text = "${event.name}, ${event.time}",
            colors = colors,
            showsIcon = showsIcon,
            onClick = onEventClick,
            modifier = Modifier
                .offset(y = tops[index].dp)
                .padding(end = ColumnGap),
        )
    }
    if (day.isToday) NowLine()
}

/** Google's line at the current time, with its round start. */
@Composable
private fun NowLine() {
    var minute by remember { mutableIntStateOf(currentMinuteOfDay()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(NOW_UPDATE_MILLIS)
            minute = currentMinuteOfDay()
        }
    }
    val color = MaterialTheme.colorScheme.error
    val top = HourHeight * minute / MINUTES_PER_HOUR
    Box(Modifier.fillMaxWidth().offset(y = top - NowLineThickness / 2).height(NowLineThickness).background(color))
    Box(
        Modifier
            .offset(x = -NowDotSize / 2, y = top - NowDotSize / 2)
            .size(NowDotSize)
            .clip(CircleShape)
            .background(color),
    )
}

/** The minute of the day it is now in the device's time zone. */
internal fun currentMinuteOfDay(): Int =
    (localNowMillis().mod(MILLIS_PER_DAY) / MILLIS_PER_MINUTE).toInt()

private const val DAYS_KEY = "days"
private const val AllDayRows = 3
private const val NOW_UPDATE_MILLIS = 30_000L
private const val MILLIS_PER_MINUTE = 60_000L
private const val MILLIS_PER_DAY = 86_400_000L

internal val HourHeight = 56.dp
private val HourLabelWidth = 52.dp
private val HourLabelLift: Dp = (-8).dp
private val DayNumberSize = 40.dp
private val ColumnGap = 2.dp
private val ChipIconMinWidth = 88.dp
private val AllDayMaxHeight = 220.dp
private val NowLineThickness = 2.dp
private val NowDotSize = 10.dp
