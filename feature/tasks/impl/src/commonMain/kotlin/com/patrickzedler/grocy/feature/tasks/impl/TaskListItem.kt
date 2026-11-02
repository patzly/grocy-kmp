@file:OptIn(ExperimentalLayoutApi::class)

package com.patrickzedler.grocy.feature.tasks.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.StatusColors
import com.patrickzedler.grocy.core.design.component.SwipeAction
import com.patrickzedler.grocy.core.design.component.SwipeActionBox
import com.patrickzedler.grocy.core.design.component.rememberStatusColors
import com.patrickzedler.grocy.core.forms.DisabledAlpha
import com.patrickzedler.grocy.core.forms.FieldChip
import com.patrickzedler.grocy.core.forms.FieldChipView
import com.patrickzedler.grocy.core.forms.ItemButton
import com.patrickzedler.grocy.core.forms.ItemButtons
import com.patrickzedler.grocy.core.forms.ListDetailItem
import com.patrickzedler.grocy.core.forms.labelled
import com.patrickzedler.grocy.core.model.tasks.Task
import com.patrickzedler.grocy.core.model.tasks.TaskDueStatus
import com.patrickzedler.grocy.core.model.tasks.TasksOverviewField
import com.patrickzedler.grocy.core.model.text.todayEpochDay
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_delete
import com.patrickzedler.grocy.core.resources.chores_assigned_to
import com.patrickzedler.grocy.core.resources.ic_rounded_category
import com.patrickzedler.grocy.core.resources.ic_rounded_check
import com.patrickzedler.grocy.core.resources.ic_rounded_dataset
import com.patrickzedler.grocy.core.resources.ic_rounded_delete
import com.patrickzedler.grocy.core.resources.ic_rounded_event
import com.patrickzedler.grocy.core.resources.ic_rounded_person
import com.patrickzedler.grocy.core.resources.ic_rounded_undo
import com.patrickzedler.grocy.core.resources.task_category
import com.patrickzedler.grocy.core.resources.task_complete
import com.patrickzedler.grocy.core.resources.task_done
import com.patrickzedler.grocy.core.resources.task_due
import com.patrickzedler.grocy.core.resources.task_edit
import com.patrickzedler.grocy.core.resources.task_uncategorized
import com.patrickzedler.grocy.core.resources.task_undo
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** What the rows of the tasks page do, Grocy's buttons of a row. */
internal class TaskRowActions(
    /** Opens the task's form. */
    val onClick: (Task) -> Unit,
    val onComplete: (Task) -> Unit,
    val onUndo: (Task) -> Unit,
    val onDelete: (Task) -> Unit,
)

/**
 * A row of the tasks page: the name as title, struck through once done, its shown fields as chips
 * below, the due date colored like Grocy's rows (red overdue, blue due today, yellow due soon).
 * Grocy's main button stays on the row, its others in the menu; swiping towards the end does the
 * main one, towards the start deletes, both also in the accessibility actions. [selected] marks
 * the task shown beside the list, null without details.
 */
@Composable
internal fun TaskListItem(
    row: TaskRow,
    fields: List<TasksOverviewField>,
    index: Int,
    count: Int,
    actions: TaskRowActions,
    selected: Boolean?,
    modifier: Modifier = Modifier,
) {
    val task = row.task
    val mainIcon = painterResource(
        if (task.done) Res.drawable.ic_rounded_undo else Res.drawable.ic_rounded_check,
    )
    val mainText = stringResource(
        if (task.done) Res.string.task_undo else Res.string.task_complete,
    )
    val onMain = { if (task.done) actions.onUndo(task) else actions.onComplete(task) }
    val deleteText = stringResource(Res.string.action_delete)
    val doneText = stringResource(Res.string.task_done)
    val contentAlpha = if (task.done) DisabledAlpha else 1f

    SwipeActionBox(
        startAction = SwipeAction(icon = mainIcon, onSwipe = onMain),
        endAction = SwipeAction(
            icon = painterResource(Res.drawable.ic_rounded_delete),
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        ) { actions.onDelete(task) },
        modifier = modifier,
    ) {
        ListDetailItem(
            selected = selected,
            onClick = { actions.onClick(task) },
            index = index,
            count = count,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    if (task.done) stateDescription = doneText
                    customActions = listOf(
                        CustomAccessibilityAction(mainText) {
                            onMain()
                            true
                        },
                        CustomAccessibilityAction(deleteText) {
                            actions.onDelete(task)
                            true
                        },
                    )
                },
            supportingContent = { TaskChips(row, fields) },
            trailingContent = {
                ItemButtons(
                    buttons = listOf(
                        ItemButton(painter = mainIcon, description = mainText, onClick = onMain),
                    ),
                    menuGroups = listOf(
                        listOf(
                            AppBarMenuItem(stringResource(Res.string.task_edit)) {
                                actions.onClick(task)
                            },
                            AppBarMenuItem(deleteText) { actions.onDelete(task) },
                        ),
                    ),
                )
            },
        ) {
            Text(
                text = task.name,
                modifier = Modifier.alpha(contentAlpha),
                textDecoration = if (task.done) TextDecoration.LineThrough else null,
            )
        }
    }
}

@Composable
private fun TaskChips(row: TaskRow, fields: List<TasksOverviewField>) {
    val chips = taskChips(row, fields)
    key(chips.map { it.text }) {
        if (chips.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                chips.forEach { FieldChipView(it) }
            }
        }
    }
}

@Composable
private fun taskChips(row: TaskRow, fields: List<TasksOverviewField>): List<FieldChip> {
    val colors = rememberStatusColors()
    val today = remember { todayEpochDay() }
    val chips = mutableListOf<FieldChip>()
    for (field in fields) {
        when (field) {
            TasksOverviewField.DUE -> row.task.dueDate?.let { due ->
                chips += FieldChip(
                    text = taskRelativeLabel(due, today),
                    description = labelled(
                        stringResource(Res.string.task_due),
                        taskDateWithRelative(due, today),
                    ),
                    icon = Res.drawable.ic_rounded_event,
                    status = row.dueStatus?.let { statusColor(it, colors) },
                )
            }
            TasksOverviewField.CATEGORY -> {
                val name = row.categoryName ?: stringResource(Res.string.task_uncategorized)
                chips += FieldChip(
                    text = name,
                    description = labelled(stringResource(Res.string.task_category), name),
                    icon = Res.drawable.ic_rounded_category,
                )
            }
            TasksOverviewField.ASSIGNED_TO -> row.assignedUserName?.let { name ->
                chips += FieldChip(
                    text = name,
                    description = labelled(stringResource(Res.string.chores_assigned_to), name),
                    icon = Res.drawable.ic_rounded_person,
                )
            }
            TasksOverviewField.USERFIELDS -> row.userfields.forEach { (caption, value) ->
                chips += FieldChip(
                    text = value,
                    description = labelled(caption, value),
                    icon = Res.drawable.ic_rounded_dataset,
                )
            }
        }
    }
    return chips
}

private fun statusColor(status: TaskDueStatus, colors: StatusColors) = when (status) {
    TaskDueStatus.OVERDUE -> colors.critical
    TaskDueStatus.DUE_TODAY -> colors.info
    TaskDueStatus.DUE_SOON -> colors.warning
}
