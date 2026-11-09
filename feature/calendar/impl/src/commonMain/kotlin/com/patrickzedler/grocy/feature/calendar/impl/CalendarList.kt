package com.patrickzedler.grocy.feature.calendar.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.component.InsetLazyColumn
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.insetItem
import com.patrickzedler.grocy.core.design.component.verticalScrollbar
import com.patrickzedler.grocy.core.design.withTabularNumbers
import com.patrickzedler.grocy.core.model.calendar.CalendarEvent
import com.patrickzedler.grocy.core.model.text.formatDayRange
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.calendar_assigned_to
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Grocy's list going on like Google Calendar's schedule: the days with events and today below the
 * headings of their weeks, each day's weekday and number at the start, its events as chips with
 * time and section, today's with the line of the current time. As wide as the screen's lists.
 * A tap on a day's number shows the day, where the number moves to as its heading. The badge of
 * the day at the top stays in view while its events scroll past.
 */
@Composable
internal fun CalendarList(
    rows: List<ListRow>,
    listState: LazyListState,
    contentPadding: PaddingValues,
    colors: EventColors,
    onDayClick: (Long) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        InsetLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = contentPadding,
        ) {
            rows.forEach { row ->
                insetItem(key = row.key, contentType = row::class.simpleName) {
                    when (row) {
                        is ListRow.Week -> WeekHeading(row)
                        is ListRow.Day -> ListDay(row, listState, colors, onDayClick, onEventClick)
                    }
                }
            }
        }
        Box(
            Modifier
                .matchParentSize()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .verticalScrollbar(listState),
        )
    }
}

@Composable
private fun WeekHeading(row: ListRow.Week) {
    Text(
        text = formatDayRange(row.start, row.end),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(start = HourLabelWidth, top = 16.dp, bottom = 4.dp)
            .semantics { heading() },
    )
}

@Composable
private fun ListDay(
    row: ListRow.Day,
    listState: LazyListState,
    colors: EventColors,
    onDayClick: (Long) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        DayBadge(
            day = row.day,
            onClick = { onDayClick(row.day.epochDay) },
            modifier = Modifier
                .width(HourLabelWidth)
                .stuckToListTop(listState, row.key),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(ListChipGap),
            modifier = Modifier
                .weight(1f)
                .padding(top = 4.dp),
        ) {
            row.day.events.forEachIndexed { index, event ->
                if (index == row.nowIndex) ListNowLine()
                ListEventChip(event, colors, onEventClick)
            }
            if (row.nowIndex != null && row.nowIndex >= row.day.events.size) ListNowLine()
        }
    }
}

/**
 * Keeps the badge of the day [key] at the top of the list while the day's events scroll past,
 * until the next day's badge pushes it away, like Google's schedule. Only moves the placement, so
 * scrolling recomposes nothing.
 */
private fun Modifier.stuckToListTop(listState: LazyListState, key: Any): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }
            val shift = item?.let { stuckBadgeShift(it.offset, it.size, placeable.height) } ?: 0
            placeable.placeRelative(0, shift)
        }
    }

/**
 * How far a day's badge moves down in its row: as far as the row left the top of the list
 * ([itemOffset] below 0), at most until the badge's end meets the next row's badge.
 */
internal fun stuckBadgeShift(itemOffset: Int, itemSize: Int, badgeHeight: Int): Int =
    (-itemOffset).coerceAtMost(itemSize - badgeHeight).coerceAtLeast(0)

/** The line of the current time between today's events, as in Google's schedule. */
@Composable
private fun ListNowLine() {
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        NowLine()
    }
}

/**
 * An event of the list like one of Google's schedule: its icon and Grocy's title, below them its time,
 * section and who a chore is assigned to, all in its color; it moves to its chip of other views.
 */
@Composable
private fun ListEventChip(
    event: CalendarEvent,
    colors: EventColors,
    onClick: (CalendarEvent) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val roles = colors.of(event.kind)
    val description = eventDescription(event)
    val details = listOfNotNull(
        event.time,
        event.section,
        event.assignedTo?.let { stringResource(Res.string.calendar_assigned_to, it) },
    ).joinToString(" · ").ifEmpty { null }
    val click = {
        haptic.confirm()
        onClick(event)
    }
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .sharedCalendarBounds(event.key)
            .fillMaxWidth()
            .heightIn(min = ListChipMinHeight)
            .clip(MaterialTheme.shapes.large)
            .background(roles.container)
            .clickable(onClick = click)
            .clickCursor()
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                onClick {
                    click()
                    true
                }
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                painter = painterResource(event.kind.icon),
                contentDescription = null,
                tint = roles.content,
                // On the first line when a long title wraps
                modifier = Modifier.padding(top = 3.dp).size(ListChipIconSize),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = eventTitle(event),
                style = MaterialTheme.typography.titleMedium,
                color = roles.content,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        details?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium.withTabularNumbers(),
                color = roles.content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private val ListChipMinHeight = 44.dp
private val ListChipGap = 6.dp
private val ListChipIconSize = 18.dp
