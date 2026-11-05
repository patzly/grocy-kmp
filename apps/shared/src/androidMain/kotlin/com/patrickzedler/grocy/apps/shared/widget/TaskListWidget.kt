package com.patrickzedler.grocy.apps.shared.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.patrickzedler.grocy.apps.shared.R
import com.patrickzedler.grocy.apps.shared.WidgetLinks

/**
 * Google's task list widget with Grocy's tasks to do: a tap on the circle is Grocy's "Mark task as
 * completed", on the task its form, on the title the tasks page, "+" a new task.
 */
internal class TaskListWidget : GrocyWidget() {

    @Composable
    override fun Content(data: WidgetData, colors: WidgetColors, texts: WidgetTexts) {
        WidgetSurface(colors) {
            val message = unavailableText(data, texts, isAvailable = data.config.tasks)
            if (message != null) {
                WidgetMessage(texts.tasks, message, colors)
            } else {
                TaskList(data, colors, texts)
            }
        }
    }
}

class TaskListWidgetReceiver : GrocyWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TaskListWidget()
}

@Composable
private fun TaskList(data: WidgetData, colors: WidgetColors, texts: WidgetTexts) {
    val tasks = remember(data.tasks, data.today, data.completing) {
        widgetTasksOf(data.tasks, data.today, data.completing)
    }
    val openTasks = openApp(WidgetLinks.tasks())
    val createTask = openApp(WidgetLinks.newTask())
    // The title scrolls with the tasks like in Google's widget
    LazyColumn(GlanceModifier.fillMaxSize()) {
        item {
            WidgetHeader(texts.tasks, colors, openTasks, texts.createTask, createTask)
        }
        if (tasks.isEmpty()) item { MessageTile(texts.noTasks, colors, openTasks) }
        items(tasks, itemId = { it.id.toLong() }) { task -> TaskRow(task, colors, texts) }
        item { Spacer(GlanceModifier.height(12.dp)) }
    }
}

@Composable
private fun TaskRow(task: WidgetTask, colors: WidgetColors, texts: WidgetTexts) {
    Row(GlanceModifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = GlanceModifier.size(48.dp)
                .clickable(
                    actionRunCallback<CompleteTaskAction>(actionParametersOf(TaskIdKey to task.id)),
                )
                .semantics { contentDescription = "${texts.complete}: ${task.name}" },
        ) {
            // Checked while the server completes it, then it leaves the list
            if (task.isCompleting) {
                WidgetIcon(R.drawable.widget_ic_check_circle, colors.primary, 24.dp)
            } else {
                WidgetIcon(R.drawable.widget_ic_radio_button_unchecked, colors.onBackground, 24.dp)
            }
        }
        Column(
            GlanceModifier.defaultWeight()
                .padding(start = 8.dp, top = 12.dp, bottom = 12.dp)
                .clickable(openApp(WidgetLinks.task(task.id))),
        ) {
            Text(
                text = task.name,
                maxLines = 3,
                style = TextStyle(
                    color = colors.onBackground,
                    fontSize = 16.sp,
                    fontFamily = BodyFont,
                ),
            )
            task.dueDay?.let { day ->
                val color = if (task.isOverdue) colors.error else colors.onBackgroundVariant
                Spacer(GlanceModifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WidgetIcon(R.drawable.widget_ic_schedule, color, 16.dp)
                    Spacer(GlanceModifier.width(6.dp))
                    Text(
                        text = formatWeekdayDate(day),
                        maxLines = 1,
                        style = TextStyle(color = color, fontSize = 14.sp, fontFamily = BodyFont),
                    )
                }
            }
        }
    }
}
