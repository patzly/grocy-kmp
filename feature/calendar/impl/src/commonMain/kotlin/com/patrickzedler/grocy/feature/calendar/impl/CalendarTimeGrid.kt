package com.patrickzedler.grocy.feature.calendar.impl

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.verticalScrollbar
import com.patrickzedler.grocy.core.design.withTabularNumbers
import com.patrickzedler.grocy.core.model.calendar.CalendarEvent
import com.patrickzedler.grocy.core.model.text.localNowMillis
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.calendar_more
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.pluralStringResource

/**
 * A page of Google Calendar's day or week: the days with what has no time stay on top, below them
 * only the hours scroll, as segmented rows with their time beside them and each event as a chip
 * at its time. Every page of both views shares [scroll], so paging and changing between them keep
 * the time in view. In the week a tap on a day's number shows that day ([onDayClick]).
 */
@Composable
internal fun TimeGridPage(
    days: List<CalendarDay>,
    scroll: ScrollState,
    bottomPadding: Dp,
    colors: EventColors,
    onDayClick: ((Long) -> Unit)?,
    onEventClick: (CalendarEvent) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        if (days.size == 1) {
            DayHeader(days.single(), colors, onEventClick)
        } else {
            WeekHeader(days, colors, onDayClick, onEventClick)
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScrollbar(scroll),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .calendarPadding()
                    .padding(top = 4.dp, bottom = bottomPadding),
            ) {
                TimeGridBody(days, colors, onEventClick)
            }
        }
    }
}

/** Google's day: its weekday and number at the start, what has no time beside them. */
@Composable
private fun DayHeader(
    day: CalendarDay,
    colors: EventColors,
    onEventClick: (CalendarEvent) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .calendarPadding()
            .padding(top = 4.dp, bottom = 8.dp),
    ) {
        DayBadge(day, onClick = null, modifier = Modifier.width(HourLabelWidth))
        AllDayChips(
            day = day,
            colors = colors,
            onEventClick = onEventClick,
            modifier = Modifier
                .weight(1f)
                .padding(top = 4.dp),
        )
    }
}

/** The week's days side by side, each with what has no time below it. */
@Composable
private fun WeekHeader(
    days: List<CalendarDay>,
    colors: EventColors,
    onDayClick: ((Long) -> Unit)?,
    onEventClick: (CalendarEvent) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .calendarPadding()
            .padding(bottom = 4.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(HourLabelWidth))
            days.forEach { day ->
                DayBadge(
                    day = day,
                    onClick = onDayClick?.let { open -> { open(day.epochDay) } },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(HourLabelWidth))
            days.forEach { day ->
                AllDayChips(
                    day = day,
                    colors = colors,
                    onEventClick = onEventClick,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = ColumnGap),
                )
            }
        }
    }
}

/**
 * What a day has without a time, three rows of it and "3 more" for the rest; expanded it scrolls
 * on its own beyond [AllDayMaxHeight], so the hours keep their room.
 */
@Composable
private fun AllDayChips(
    day: CalendarDay,
    colors: EventColors,
    onEventClick: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by rememberSaveable(day.epochDay) { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val wholeDay = day.events.filter { minuteOfTime(it.time) == null }
    val limit = if (isExpanded) wholeDay.size else AllDayRows
    val shown = shownChips(wholeDay.size, limit)
    BoxWithConstraints(modifier.heightIn(max = AllDayMaxHeight)) {
        val showsIcon = maxWidth >= ChipIconMinWidth
        Column(
            verticalArrangement = Arrangement.spacedBy(ChipGap),
            modifier = Modifier.verticalScroll(rememberScrollState()),
        ) {
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

/** The lines between days, in a row of the grid. */
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
            text = event.name,
            colors = colors,
            showsIcon = showsIcon,
            onClick = onEventClick,
            modifier = Modifier
                .offset(y = tops[index].dp)
                .padding(end = ColumnGap),
        )
    }
    if (day.isToday) {
        NowLine(Modifier.offset(y = HourHeight * rememberCurrentMinute() / MINUTES_PER_HOUR))
    }
}

/** The minute of the day it is now, moving on while it is shown. */
@Composable
internal fun rememberCurrentMinute(): Int {
    var minute by remember { mutableIntStateOf(currentMinuteOfDay()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(NOW_UPDATE_MILLIS)
            minute = currentMinuteOfDay()
        }
    }
    return minute
}

/** Google's line at the current time, with its round start; [modifier] places its middle. */
@Composable
internal fun NowLine(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.error
    Box(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .offset(y = -NowLineThickness / 2)
                .height(NowLineThickness)
                .background(color),
        )
        Box(
            Modifier
                .offset(x = -NowDotSize / 2, y = -NowDotSize / 2)
                .size(NowDotSize)
                .clip(CircleShape)
                .background(color),
        )
    }
}

/** The minute of the day it is now in the device's time zone. */
internal fun currentMinuteOfDay(): Int =
    (localNowMillis().mod(MILLIS_PER_DAY) / MILLIS_PER_MINUTE).toInt()

/**
 * Where the time grid scrolls to for [minute]: an hour above it, so what comes before stays in
 * view, [hourHeight] being an hour's height in pixels.
 */
internal fun gridOffsetOf(minute: Int, hourHeight: Float): Int =
    ((minute - MINUTES_PER_HOUR).coerceAtLeast(0) * hourHeight / MINUTES_PER_HOUR).toInt()

private const val AllDayRows = 3
private const val NOW_UPDATE_MILLIS = 30_000L
private const val MILLIS_PER_MINUTE = 60_000L
private const val MILLIS_PER_DAY = 86_400_000L

internal val HourHeight = 56.dp

/** The column of the hours, which the day's weekday and number share in the day. */
internal val HourLabelWidth = 64.dp
private val HourLabelLift: Dp = (-8).dp
private val ColumnGap = 2.dp
private val ChipIconMinWidth = 88.dp
private val AllDayMaxHeight = 220.dp
private val NowLineThickness = 2.dp
private val NowDotSize = 10.dp
