package com.patrickzedler.grocy.feature.calendar.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.withTabularNumbers
import com.patrickzedler.grocy.core.model.text.dayOfMonthOf
import com.patrickzedler.grocy.core.model.text.formatDate
import com.patrickzedler.grocy.core.model.text.formatWeekday
import com.patrickzedler.grocy.core.model.text.formatWeekdayShort
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.calendar_view_day
import org.jetbrains.compose.resources.stringResource

/** The screen's edges like its lists keep them: beside the cutouts, 16.dp from the window. */
@Composable
internal fun Modifier.calendarPadding(): Modifier = this
    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
    .padding(horizontal = 16.dp)

/**
 * Google Calendar's heading of a day: its short weekday above its number, today's in the primary
 * color on a filled circle. Both move between the views ([dayKey], [weekdayKey]); [onClick] shows
 * the day on its own.
 */
@Composable
internal fun DayBadge(
    day: CalendarDay,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    size: Dp = DayBadgeSize,
) {
    val scheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val description = "${formatWeekday(day.epochDay)}, ${formatDate(day.epochDay)}"
    val openLabel = stringResource(Res.string.calendar_view_day)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.semantics(mergeDescendants = true) {
            heading()
            contentDescription = description
        },
    ) {
        Text(
            text = formatWeekdayShort(day.epochDay),
            style = MaterialTheme.typography.labelMedium,
            color = if (day.isToday) scheme.primary else scheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier
                .sharedCalendarBounds(weekdayKey(day.epochDay))
                .clearAndSetSemantics { },
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .sharedCalendarBounds(dayKey(day.epochDay))
                .size(size)
                .clip(CircleShape)
                .then(if (day.isToday) Modifier.background(scheme.primary) else Modifier)
                .then(
                    if (onClick != null) {
                        Modifier
                            .clickable(onClickLabel = openLabel) {
                                haptic.confirm()
                                onClick()
                            }
                            .clickCursor()
                    } else {
                        Modifier
                    },
                ),
        ) {
            Text(
                text = dayOfMonthOf(day.epochDay).toString(),
                style = MaterialTheme.typography.titleLarge.withTabularNumbers(),
                color = if (day.isToday) scheme.onPrimary else scheme.onSurface,
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
    }
}

private val DayBadgeSize = 40.dp

// Between the app bar and the weekdays of the day, week and month
internal val HeaderTopPadding = 12.dp
