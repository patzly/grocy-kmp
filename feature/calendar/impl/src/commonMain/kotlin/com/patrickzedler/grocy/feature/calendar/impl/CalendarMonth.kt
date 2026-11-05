package com.patrickzedler.grocy.feature.calendar.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.insetItem
import com.patrickzedler.grocy.core.design.withTabularNumbers
import com.patrickzedler.grocy.core.model.calendar.CalendarEvent
import com.patrickzedler.grocy.core.model.text.dayOfMonthOf
import com.patrickzedler.grocy.core.model.text.formatDate
import com.patrickzedler.grocy.core.model.text.formatWeekday
import com.patrickzedler.grocy.core.model.text.formatWeekdayShort
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.calendar_week_number
import org.jetbrains.compose.resources.stringResource

/**
 * Google Calendar's month: a segmented row per week, as much of the window's height as the weeks
 * can share, its days with their number on top and their events as chips; a tap on a day shows it.
 */
internal fun LazyListScope.monthGrid(
    state: CalendarUiState,
    windowHeight: Dp,
    showsIcons: Boolean,
    colors: EventColors,
    onDayClick: (Long) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
) {
    val showsNumbers = state.config.calendarShowWeekOfYear
    val weeks = state.monthWeeks()
    val rowHeight = monthRowHeight(windowHeight, weeks.size)
    insetItem(key = "weekdays", contentType = "weekdays", maxWidth = Dp.Infinity) {
        WeekdaysRow(weeks.first().days, showsNumbers)
    }
    weeks.forEachIndexed { index, week ->
        insetItem(
            key = "week-${week.days.first().epochDay}",
            contentType = "week",
            maxWidth = Dp.Infinity,
        ) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).heightIn(min = rowHeight)) {
                if (showsNumbers) WeekNumber(week.number)
                Surface(
                    shape = SegmentedListDefaults.shape(index = index, count = weeks.size),
                    color = SegmentedListDefaults.containerColor,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                ) {
                    Row(Modifier.fillMaxWidth().fillMaxHeight()) {
                        week.days.forEachIndexed { dayIndex, day ->
                            if (dayIndex > 0) {
                                VerticalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
                            }
                            MonthDay(
                                day = day,
                                showsIcons = showsIcons,
                                colors = colors,
                                onDayClick = onDayClick,
                                onEventClick = onEventClick,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekdaysRow(days: List<CalendarDay>, showsNumbers: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        if (showsNumbers) Spacer(Modifier.width(WeekNumberWidth))
        days.forEach { day ->
            Text(
                text = formatWeekdayShort(day.epochDay),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun WeekNumber(number: Int) {
    val description = stringResource(Res.string.calendar_week_number, number)
    Text(
        text = number.toString(),
        style = MaterialTheme.typography.labelMedium.withTabularNumbers(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .width(WeekNumberWidth)
            .padding(top = 8.dp)
            .semantics { contentDescription = description },
    )
}

@Composable
private fun MonthDay(
    day: CalendarDay,
    showsIcons: Boolean,
    colors: EventColors,
    onDayClick: (Long) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val open = {
        haptic.confirm()
        onDayClick(day.epochDay)
    }
    val shown = shownChips(day.events.size, MonthDayChips)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ChipGap),
        modifier = modifier
            .clickable(onClick = open)
            .clickCursor()
            .padding(start = 2.dp, end = 2.dp, top = 4.dp, bottom = 8.dp),
    ) {
        DayNumber(day)
        day.events.take(shown).forEach { event ->
            EventChip(
                event = event,
                text = event.time?.let { "$it ${event.name}" } ?: event.name,
                colors = colors,
                showsIcon = showsIcons,
                onClick = onEventClick,
            )
        }
        if (shown < day.events.size) MoreChip(day.events.size - shown, onClick = open)
    }
}

/** The day's number, today's on a filled circle, those of other months muted. */
@Composable
private fun DayNumber(day: CalendarDay) {
    val scheme = MaterialTheme.colorScheme
    val description = "${formatWeekday(day.epochDay)}, ${formatDate(day.epochDay)}"
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(DayNumberSize)
            .clip(CircleShape)
            .then(if (day.isToday) Modifier.background(scheme.primary) else Modifier)
            .alpha(if (day.isOutside) OUTSIDE_ALPHA else 1f)
            .semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Text(
            text = dayOfMonthOf(day.epochDay).toString(),
            style = MaterialTheme.typography.labelLarge.withTabularNumbers(),
            color = if (day.isToday) scheme.onPrimary else scheme.onSurface,
            maxLines = 1,
        )
    }
}

/**
 * How high a month's rows are at least: they share the window below its bar and above its toolbar,
 * so a month shows at once, but stay high enough for a chip below the number.
 */
internal fun monthRowHeight(windowHeight: Dp, weeks: Int): Dp =
    ((windowHeight - MonthReservedHeight) / weeks).coerceAtLeast(MinMonthRowHeight)

private const val MonthDayChips = 4
private const val OUTSIDE_ALPHA = 0.6f

private val WeekNumberWidth = 24.dp
private val DayNumberSize = 28.dp
private val MonthReservedHeight = 190.dp
private val MinMonthRowHeight = 72.dp
