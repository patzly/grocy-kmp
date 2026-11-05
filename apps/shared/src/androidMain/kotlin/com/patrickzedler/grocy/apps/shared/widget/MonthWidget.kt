package com.patrickzedler.grocy.apps.shared.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.patrickzedler.grocy.apps.shared.R
import com.patrickzedler.grocy.apps.shared.WidgetLinks
import com.patrickzedler.grocy.core.model.calendar.CalendarEvent
import com.patrickzedler.grocy.core.model.calendar.CalendarView
import com.patrickzedler.grocy.core.model.recipes.DAYS_PER_WEEK
import com.patrickzedler.grocy.core.model.text.dayOfMonthOf
import com.patrickzedler.grocy.core.model.text.localeFirstDayOfWeek

/**
 * Google's month widget with Grocy's calendar: every day of the month with as many events as fit,
 * dots for the rest. Previous and next go through the months, a tap on a day shows it in the app,
 * on the title the month, "+" a new task.
 */
internal class MonthWidget : GrocyWidget() {

    @Composable
    override fun Content(data: WidgetData, colors: WidgetColors, texts: WidgetTexts) {
        WidgetSurface(colors) {
            val message = unavailableText(data, texts, isAvailable = data.config.calendar)
            if (message != null) {
                WidgetMessage(texts.calendar, message, colors)
            } else {
                val state = currentState<Preferences>()
                val month = shownMonthOf(
                    offset = state[MonthOffsetKey] ?: 0,
                    steppedOn = state[SteppedOnKey],
                    today = data.today,
                )
                Month(month, data, colors, texts)
            }
        }
    }
}

class MonthWidgetReceiver : GrocyWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MonthWidget()
}

@Composable
private fun Month(month: Long, data: WidgetData, colors: WidgetColors, texts: WidgetTexts) {
    val firstDayOfWeek = data.config.calendarFirstDayOfWeek ?: localeFirstDayOfWeek()
    val weeks = remember(month, data.today, firstDayOfWeek, data.events) {
        monthCellsOf(month, data.today, firstDayOfWeek, data.events)
    }
    val size = LocalSize.current
    val cellHeight = (size.height - HeaderHeight - WeekdaysHeight).value / weeks.size - LINE_WIDTH
    val places = monthChipPlaces(cellHeight)
    val showsIcons = size.width.value / DAYS_PER_WEEK >= ICON_CELL_WIDTH
    Column(GlanceModifier.fillMaxSize()) {
        WidgetHeader(
            title = formatMonthTitle(month, data.today),
            colors = colors,
            onTitleClick = openApp(WidgetLinks.calendar(CalendarView.MONTH, month)),
            addLabel = texts.createTask.takeIf { data.config.tasks },
            onAddClick = if (data.config.tasks) openApp(WidgetLinks.newTask()) else null,
        ) {
            HeaderIconButton(R.drawable.widget_ic_chevron_left, texts.previous, colors, step(-1))
            HeaderIconButton(R.drawable.widget_ic_chevron_right, texts.next, colors, step(1))
        }
        Row(GlanceModifier.fillMaxWidth().height(WeekdaysHeight)) {
            weeks.first().forEach { cell ->
                Text(
                    text = formatWeekdayNarrow(cell.epochDay),
                    style = TextStyle(
                        color = colors.onBackgroundVariant,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        fontFamily = BodyFont,
                    ),
                    modifier = GlanceModifier.defaultWeight(),
                )
            }
        }
        weeks.forEach { week ->
            Column(GlanceModifier.fillMaxWidth().defaultWeight()) {
                Spacer(GlanceModifier.fillMaxWidth().height(LINE_WIDTH.dp).background(colors.line))
                Row(GlanceModifier.fillMaxSize()) {
                    week.forEachIndexed { index, cell ->
                        val openDay = openApp(WidgetLinks.calendar(CalendarView.DAY, cell.epochDay))
                        Row(GlanceModifier.defaultWeight().fillMaxHeight().clickable(openDay)) {
                            if (index > 0) {
                                Spacer(
                                    GlanceModifier.width(LINE_WIDTH.dp).fillMaxHeight()
                                        .background(colors.line),
                                )
                            }
                            DayCell(
                                cell = cell,
                                places = places,
                                showsIcons = showsIcons,
                                data = data,
                                colors = colors,
                                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun step(steps: Int) =
    actionRunCallback<MonthStepAction>(actionParametersOf(StepsKey to steps))

@Composable
private fun DayCell(
    cell: MonthCell,
    places: Int,
    showsIcons: Boolean,
    data: WidgetData,
    colors: WidgetColors,
    modifier: GlanceModifier,
) {
    val shown = shownMonthChips(cell.events.size, places)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 2.dp),
    ) {
        DayNumber(cell, colors)
        cell.events.take(shown).forEach { event ->
            Spacer(GlanceModifier.height(MONTH_CHIP_GAP.dp))
            MonthChip(event, showsIcons, data, colors)
        }
        if (shown < cell.events.size) {
            Text(
                text = "•••",
                maxLines = 1,
                style = TextStyle(
                    color = colors.onBackgroundVariant,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                ),
                modifier = GlanceModifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DayNumber(cell: MonthCell, colors: WidgetColors) {
    val text = dayOfMonthOf(cell.epochDay).toString()
    Box(
        contentAlignment = Alignment.Center,
        modifier = GlanceModifier.fillMaxWidth().height(MONTH_DAY_HEIGHT.dp),
    ) {
        if (cell.isToday) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = GlanceModifier.size(20.dp).rounded(colors.primary, WidgetShape.FULL),
            ) {
                Text(
                    text = text,
                    style = TextStyle(
                        color = colors.onPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = BodyFont,
                    ),
                )
            }
        } else {
            Text(
                text = text,
                style = TextStyle(
                    color = if (cell.isOutside) colors.onBackgroundVariant else colors.onBackground,
                    fontSize = 13.sp,
                    fontFamily = BodyFont,
                ),
            )
        }
    }
}

/** An event in Google's month widget: one line in its color, cut where the day ends. */
@Composable
private fun MonthChip(
    event: CalendarEvent,
    showsIcon: Boolean,
    data: WidgetData,
    colors: WidgetColors,
) {
    val roles = colors.event(event.kind, data.settings)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = GlanceModifier.fillMaxWidth().height(MONTH_CHIP_HEIGHT.dp)
            .rounded(roles.container, WidgetShape.SMALL)
            .padding(horizontal = 3.dp),
    ) {
        if (showsIcon) {
            WidgetIcon(event.kind.icon, roles.content, 10.dp)
            Spacer(GlanceModifier.width(2.dp))
        }
        Text(
            text = unbroken(event.name),
            maxLines = 1,
            style = TextStyle(color = roles.content, fontSize = 11.sp, fontFamily = BodyFont),
        )
    }
}

private val WeekdaysHeight = 24.dp
private const val LINE_WIDTH = 1f

// From about this wide on a day has room for an icon in front of the name
private const val ICON_CELL_WIDTH = 48f
