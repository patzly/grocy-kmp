package com.patrickzedler.grocy.apps.shared.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.patrickzedler.grocy.apps.shared.WidgetLinks
import com.patrickzedler.grocy.core.model.calendar.CalendarEvent
import com.patrickzedler.grocy.core.model.calendar.CalendarView

/**
 * "Calendar list": Google's schedule widget with Grocy's calendar from today on, each day with its
 * events like tasks, which have no duration either. A tap on an event opens what it is about, on a
 * day the calendar's list, "+" a new task. Its classes keep their first name, the system knows
 * placed widgets by it.
 */
internal class ScheduleWidget : GrocyWidget() {

    override val previewSizeMode = SizeMode.Responsive(setOf(DpSize(200.dp, 280.dp)))

    @Composable
    override fun Content(data: WidgetData, colors: WidgetColors, texts: WidgetTexts) {
        WidgetSurface(colors) {
            val message = unavailableText(data, texts, isAvailable = data.config.calendar)
            if (message != null) {
                WidgetMessage(texts.calendar, message, colors)
            } else {
                Schedule(data, colors, texts)
            }
        }
    }
}

class ScheduleWidgetReceiver : GrocyWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScheduleWidget()
}

@Composable
private fun Schedule(data: WidgetData, colors: WidgetColors, texts: WidgetTexts) {
    val rows = remember(data.events, data.today) { scheduleRowsOf(data.events, data.today) }
    val isNarrow = LocalSize.current.width < NarrowWidth
    val openSchedule = openApp(WidgetLinks.calendar(CalendarView.LIST, data.today))
    val createTask = if (data.config.tasks) openApp(WidgetLinks.newTask()) else null
    LazyColumn(GlanceModifier.fillMaxSize()) {
        itemsIndexed(rows, itemId = { index, _ -> index.toLong() }) { _, row ->
            when (row) {
                is ScheduleRow.Day -> if (row.isToday) {
                    WidgetHeader(
                        title = if (isNarrow) {
                            formatDayWeekday(row.epochDay)
                        } else {
                            formatWeekdayDate(row.epochDay)
                        },
                        colors = colors,
                        onTitleClick = openSchedule,
                        addLabel = texts.createTask.takeIf { createTask != null },
                        onAddClick = createTask,
                    )
                } else {
                    DayHeading(row.epochDay, colors)
                }
                is ScheduleRow.Event -> EventChip(row.event, data, colors, texts)
                ScheduleRow.NothingToday -> MessageTile(texts.nothingToday, colors, openSchedule)
            }
        }
        item { Spacer(GlanceModifier.height(12.dp)) }
    }
}

@Composable
private fun DayHeading(epochDay: Long, colors: WidgetColors) {
    Box(
        modifier = GlanceModifier.fillMaxWidth()
            .padding(start = WidgetInset, end = 16.dp, top = 12.dp, bottom = 8.dp)
            .clickable(openApp(WidgetLinks.calendar(CalendarView.LIST, epochDay))),
    ) {
        Text(
            text = formatWeekdayDate(epochDay),
            maxLines = 1,
            style = TextStyle(
                color = colors.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = TitleFont,
            ),
        )
    }
}

/** An event like a task of Google's widget: its icon and name, below them time and section. */
@Composable
private fun EventChip(
    event: CalendarEvent,
    data: WidgetData,
    colors: WidgetColors,
    texts: WidgetTexts,
) {
    val roles = colors.event(event.kind, data.settings)
    val details = listOfNotNull(event.time, event.section).joinToString(" · ").ifEmpty { null }
    Box(GlanceModifier.fillMaxWidth().padding(horizontal = ChipMargin).padding(bottom = 4.dp)) {
        Column(
            GlanceModifier.fillMaxWidth()
                .rounded(roles.container, WidgetShape.MEDIUM)
                .padding(horizontal = ChipPadding, vertical = 6.dp)
                .clickable(openApp(WidgetLinks.event(event)))
                .semantics { contentDescription = texts.description(event) },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WidgetIcon(event.kind.icon, roles.content, 18.dp)
                Spacer(GlanceModifier.width(8.dp))
                Text(
                    text = unbroken(event.name),
                    maxLines = 1,
                    style = TextStyle(
                        color = roles.content,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = BodyFont,
                    ),
                )
            }
            details?.let {
                Text(
                    text = it,
                    maxLines = 1,
                    style = TextStyle(
                        color = roles.content,
                        fontSize = 14.sp,
                        fontFamily = BodyFont,
                    ),
                )
            }
        }
    }
}

// Below it the date is as short as in Google's narrow widget
private val NarrowWidth = 200.dp
