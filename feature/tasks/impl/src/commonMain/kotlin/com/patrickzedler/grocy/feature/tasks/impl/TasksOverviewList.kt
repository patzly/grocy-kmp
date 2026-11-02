package com.patrickzedler.grocy.feature.tasks.impl

import com.patrickzedler.grocy.core.forms.sortableUserfieldColumns
import com.patrickzedler.grocy.core.forms.userfieldText
import com.patrickzedler.grocy.core.forms.userfieldTextOrder
import com.patrickzedler.grocy.core.model.User
import com.patrickzedler.grocy.core.model.masterdata.TableColumn
import com.patrickzedler.grocy.core.model.masterdata.TaskCategory
import com.patrickzedler.grocy.core.model.masterdata.Userfield
import com.patrickzedler.grocy.core.model.tasks.Task
import com.patrickzedler.grocy.core.model.tasks.TaskDueStatus
import com.patrickzedler.grocy.core.model.tasks.TasksOverviewField
import com.patrickzedler.grocy.core.model.tasks.TasksOverviewGrouping
import com.patrickzedler.grocy.core.model.tasks.TasksOverviewOptions
import com.patrickzedler.grocy.core.model.tasks.TasksOverviewSortField
import com.patrickzedler.grocy.core.model.tasks.taskDueStatusOf
import com.patrickzedler.grocy.core.model.text.FuzzySearch

/** What Grocy's category filter offers besides its categories. */
internal const val UNCATEGORIZED = -1

/** Grocy's status, category and assignment filters and its "Show done tasks". */
internal data class TasksFilter(
    val status: TaskDueStatus? = null,
    /** A category's id or [UNCATEGORIZED]. */
    val categoryId: Int? = null,
    val userId: Int? = null,
    val showDone: Boolean = false,
) {
    val isActive: Boolean
        get() = status != null || categoryId != null || userId != null || showDone
}

/** One row of Grocy's tasks page. */
internal data class TaskRow(
    val task: Task,
    val dueStatus: TaskDueStatus?,
    /** Null while uncategorized. */
    val categoryName: String?,
    val assignedUserName: String?,
    /** The userfields Grocy shows as table columns with a value, as caption and text. */
    val userfields: List<Pair<String, String>>,
    /** Their texts by the field's name, what sorting and grouping by them use. */
    val userfieldTexts: Map<String, String> = emptyMap(),
)

/**
 * Rows below a group title; [title] is null without grouping and for rows without the value, which
 * [isUnset] tells apart. Grouped by due date it is the date as Grocy stores it.
 */
internal data class TaskGroup(
    val title: String?,
    val rows: List<TaskRow>,
    val isUnset: Boolean = false,
)

/** What the tasks page's list is built from. */
internal class TasksData(
    val tasks: List<Task>,
    val categories: List<TaskCategory>,
    val users: List<User>,
    val userfields: List<Userfield>,
)

/** The columns the server's userfields [hasUserfieldColumns] offer. */
internal fun availableTaskFields(hasUserfieldColumns: Boolean): List<TasksOverviewField> =
    TasksOverviewField.entries.filter { it != TasksOverviewField.USERFIELDS || hasUserfieldColumns }

/** Grocy's groupings and the server's [userfieldColumns]. */
internal fun availableTaskGroupings(
    userfieldColumns: List<Userfield>,
): List<TableColumn<TasksOverviewGrouping>> =
    TasksOverviewGrouping.entries.map { TableColumn.Own(it) } +
        userfieldColumns.map { TableColumn.OfUserfield(it.name) }

/** The grouping the list has: the chosen one, none while its column is gone. */
internal fun effectiveTaskGrouping(
    options: TasksOverviewOptions,
    available: List<TableColumn<TasksOverviewGrouping>>,
): TableColumn<TasksOverviewGrouping> =
    options.groupingColumn.takeIf { it in available }
        ?: TableColumn.Own(TasksOverviewGrouping.NONE)

/** The name and the shown columns, what the list can be sorted by. */
internal fun availableTaskSortColumns(
    fields: Collection<TasksOverviewField>,
    userfieldColumns: List<Userfield>,
): List<TableColumn<TasksOverviewSortField>> =
    TasksOverviewSortField.entries.filter {
        it == TasksOverviewSortField.NAME || it.field in fields
    }.map { TableColumn.Own(it) } + if (TasksOverviewField.USERFIELDS in fields) {
        userfieldColumns.map { TableColumn.OfUserfield(it.name) }
    } else {
        emptyList()
    }

/** The order the list has: the chosen one, Grocy's default while its column is hidden. */
internal fun effectiveTaskSort(
    options: TasksOverviewOptions,
    fields: Collection<TasksOverviewField>,
    userfieldColumns: List<Userfield> = emptyList(),
): Pair<TableColumn<TasksOverviewSortField>, Boolean> {
    val column = options.sortColumn
    if (column in availableTaskSortColumns(fields, userfieldColumns)) {
        return column to options.ascending
    }
    return TableColumn.Own(defaultTaskSort(fields)) to true
}

/** Grocy's default: by due date, the name while that is hidden. */
internal fun defaultTaskSort(fields: Collection<TasksOverviewField>): TasksOverviewSortField =
    if (TasksOverviewField.DUE in fields) {
        TasksOverviewSortField.DUE
    } else {
        TasksOverviewSortField.NAME
    }

private val TasksOverviewSortField.field: TasksOverviewField?
    get() = when (this) {
        TasksOverviewSortField.NAME -> null
        TasksOverviewSortField.DUE -> TasksOverviewField.DUE
        TasksOverviewSortField.CATEGORY -> TasksOverviewField.CATEGORY
        TasksOverviewSortField.ASSIGNED_TO -> TasksOverviewField.ASSIGNED_TO
    }

/**
 * Grocy's tasks page: the rows [filter]ed and searched by [query], sorted and grouped. Done tasks
 * only show with [TasksFilter.showDone]; its status "due soon" includes what is due today.
 */
internal fun buildTasksOverview(
    data: TasksData,
    filter: TasksFilter,
    sort: Pair<TableColumn<TasksOverviewSortField>, Boolean>,
    grouping: TableColumn<TasksOverviewGrouping>,
    query: String,
    today: Long,
    dueSoonDays: Int,
): List<TaskGroup> {
    val categories = data.categories.associateBy { it.id }
    val users = data.users.associateBy { it.id }
    val columns = data.userfields.filter { it.showAsColumnInTables }
    val sortable = sortableUserfieldColumns(data.userfields)
    val rows = data.tasks.filter { filter.showDone || !it.done }.map { task ->
        TaskRow(
            task = task,
            dueStatus = taskDueStatusOf(task.dueDate, today, dueSoonDays),
            categoryName = task.categoryId?.let { categories[it]?.name },
            assignedUserName = task.assignedToUserId?.let { users[it]?.displayName },
            userfields = columns.mapNotNull { field ->
                userfieldText(field, task.userfields[field.name])?.let { field.caption to it }
            },
            userfieldTexts = columns.mapNotNull { field ->
                userfieldText(field, task.userfields[field.name])?.let { field.name to it }
            }.toMap(),
        )
    }
    val filtered = rows.filter { row ->
        val status = filter.status == null || row.dueStatus == filter.status ||
            (filter.status == TaskDueStatus.DUE_SOON && row.dueStatus == TaskDueStatus.DUE_TODAY)
        val category = when (filter.categoryId) {
            null -> true
            UNCATEGORIZED -> row.categoryName == null
            else -> row.task.categoryId == filter.categoryId
        }
        status && category && (filter.userId == null || row.task.assignedToUserId == filter.userId)
    }
    val sorted = sorted(search(filtered, query), sort, sortable)
    val value: (TaskRow) -> String?
    var order = userfieldTextOrder(null)
    when (grouping) {
        is TableColumn.OfUserfield -> {
            value = { row -> row.userfieldTexts[grouping.name] }
            order = userfieldTextOrder(sortable.firstOrNull { it.name == grouping.name })
        }
        is TableColumn.Own -> value = when (grouping.column) {
            TasksOverviewGrouping.NONE ->
                return if (sorted.isEmpty()) emptyList() else listOf(TaskGroup(null, sorted))
            TasksOverviewGrouping.DUE -> { row -> row.task.dueDate?.take(DATE_LENGTH) }
            TasksOverviewGrouping.CATEGORY -> { row -> row.categoryName }
            TasksOverviewGrouping.ASSIGNED_TO -> { row -> row.assignedUserName }
        }
    }
    val (withValue, withoutValue) = sorted.partition { value(it) != null }
    return withValue.groupBy { value(it).orEmpty() }.entries
        .sortedWith(compareBy(order) { it.key })
        .map { (title, group) -> TaskGroup(title, group) } +
        listOfNotNull(
            withoutValue.takeIf { it.isNotEmpty() }?.let { TaskGroup(null, it, isUnset = true) },
        )
}

private fun search(rows: List<TaskRow>, query: String): List<TaskRow> {
    if (query.isBlank()) return rows
    val matches = FuzzySearch.filter(rows, query) { row ->
        listOfNotNull(row.task.name, row.categoryName, row.assignedUserName) +
            row.userfields.map { it.second }
    }.map { it.task.id }.toSet()
    return rows.filter { it.task.id in matches }
}

/** Sorted by the chosen column, rows without a value last in both directions, then by name. */
private fun sorted(
    rows: List<TaskRow>,
    sort: Pair<TableColumn<TasksOverviewSortField>, Boolean>,
    userfieldColumns: List<Userfield>,
): List<TaskRow> {
    val (column, ascending) = sort
    val byName = compareBy<TaskRow> { it.task.name.lowercase() }
    val textOrder = if (column is TableColumn.OfUserfield) {
        userfieldTextOrder(userfieldColumns.firstOrNull { it.name == column.name })
    } else {
        naturalOrder()
    }
    val value: (TaskRow) -> String? = when (column) {
        is TableColumn.OfUserfield -> { row -> row.userfieldTexts[column.name] }
        is TableColumn.Own -> when (column.column) {
            TasksOverviewSortField.NAME ->
                return rows.sortedWith(if (ascending) byName else byName.reversed())
            TasksOverviewSortField.DUE -> { row -> row.task.dueDate }
            TasksOverviewSortField.CATEGORY -> { row -> row.categoryName?.lowercase() }
            TasksOverviewSortField.ASSIGNED_TO -> { row -> row.assignedUserName?.lowercase() }
        }
    }
    val (withValue, withoutValue) = rows.partition { value(it) != null }
    val byValue = compareBy<TaskRow, String>(textOrder) { value(it).orEmpty() }
    val order = if (ascending) byValue else byValue.reversed()
    return withValue.sortedWith(order.then(byName)) + withoutValue.sortedWith(byName)
}

private const val DATE_LENGTH = 10
