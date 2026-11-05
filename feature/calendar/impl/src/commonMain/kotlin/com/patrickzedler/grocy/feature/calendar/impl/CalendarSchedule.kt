package com.patrickzedler.grocy.feature.calendar.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.component.SegmentedContainer
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
import com.patrickzedler.grocy.core.resources.calendar_all_day
import com.patrickzedler.grocy.core.resources.calendar_assigned_to
import com.patrickzedler.grocy.core.resources.calendar_no_events
import org.jetbrains.compose.resources.stringResource

/**
 * Google Calendar's schedule, Grocy's list going on: every day with events from the shown one on
 * as a segmented row, its date at the start and its events with a mark, the time and the name.
 */
internal fun LazyListScope.schedule(
    days: List<CalendarDay>,
    isWide: Boolean,
    colors: EventColors,
    onEventClick: (CalendarEvent) -> Unit,
) {
    if (days.isEmpty()) {
        insetItem(key = "empty", contentType = "empty") {
            SegmentedContainer(index = 0, count = 1) {
                Text(
                    text = stringResource(Res.string.calendar_no_events),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }
    days.forEachIndexed { index, day ->
        insetItem(key = "day-${day.epochDay}", contentType = "day", maxWidth = ScheduleMaxWidth) {
            Surface(
                shape = SegmentedListDefaults.shape(index = index, count = days.size),
                color = SegmentedListDefaults.containerColor,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                    ScheduleDate(day)
                    Column(Modifier.weight(1f)) {
                        day.events.forEach { event ->
                            ScheduleEvent(event, isWide, colors, onEventClick)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleDate(day: CalendarDay) {
    val scheme = MaterialTheme.colorScheme
    val description = "${formatWeekday(day.epochDay)}, ${formatDate(day.epochDay)}"
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(DateWidth)
            .semantics(mergeDescendants = true) {
                heading()
                contentDescription = description
            },
    ) {
        Text(
            text = formatWeekdayShort(day.epochDay),
            style = MaterialTheme.typography.labelMedium,
            color = if (day.isToday) scheme.primary else scheme.onSurfaceVariant,
            maxLines = 1,
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(DateNumberSize)
                .clip(CircleShape)
                .then(if (day.isToday) Modifier.background(scheme.primary) else Modifier),
        ) {
            Text(
                text = dayOfMonthOf(day.epochDay).toString(),
                style = MaterialTheme.typography.titleLarge.withTabularNumbers(),
                color = if (day.isToday) scheme.onPrimary else scheme.onSurface,
            )
        }
    }
}

@Composable
private fun ScheduleEvent(
    event: CalendarEvent,
    isWide: Boolean,
    colors: EventColors,
    onClick: (CalendarEvent) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val time = event.time ?: stringResource(Res.string.calendar_all_day)
    val details = listOfNotNull(
        time.takeUnless { isWide },
        stringResource(event.kind.label),
        event.section,
        event.assignedTo?.let { stringResource(Res.string.calendar_assigned_to, it) },
    ).joinToString(" · ")
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable {
                haptic.confirm()
                onClick(event)
            }
            .clickCursor()
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        EventDot(
            color = colors.of(event.kind).color,
            modifier = Modifier.padding(top = 5.dp).size(DotSize),
        )
        Spacer(Modifier.width(12.dp))
        if (isWide) {
            Text(
                text = time,
                style = MaterialTheme.typography.bodyMedium.withTabularNumbers(),
                modifier = Modifier.width(TimeWidth),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(text = event.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val ScheduleMaxWidth = 960.dp
private val DateWidth = 56.dp
private val DateNumberSize = 40.dp
private val DotSize = 10.dp
private val TimeWidth = 88.dp
