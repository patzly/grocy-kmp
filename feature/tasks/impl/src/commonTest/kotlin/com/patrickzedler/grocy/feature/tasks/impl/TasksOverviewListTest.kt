package com.patrickzedler.grocy.feature.tasks.impl

import com.patrickzedler.grocy.core.forms.sortableUserfieldColumns
import com.patrickzedler.grocy.core.model.User
import com.patrickzedler.grocy.core.model.masterdata.TableColumn
import com.patrickzedler.grocy.core.model.masterdata.TaskCategory
import com.patrickzedler.grocy.core.model.masterdata.Userfield
import com.patrickzedler.grocy.core.model.masterdata.UserfieldType
import com.patrickzedler.grocy.core.model.tasks.Task
import com.patrickzedler.grocy.core.model.tasks.TaskDueStatus
import com.patrickzedler.grocy.core.model.tasks.TasksOverviewField
import com.patrickzedler.grocy.core.model.tasks.TasksOverviewGrouping
import com.patrickzedler.grocy.core.model.tasks.TasksOverviewOptions
import com.patrickzedler.grocy.core.model.tasks.TasksOverviewSortField
import com.patrickzedler.grocy.core.model.text.epochDayOf
import kotlin.test.Test
import kotlin.test.assertEquals

class TasksOverviewListTest {

    private val today = epochDayOf("2026-11-02")!!
    private val data = TasksData(
        tasks = listOf(
            Task(1, "Mow", dueDate = "2026-11-01", categoryId = 1, assignedToUserId = 1),
            Task(2, "Water", dueDate = "2026-11-02", categoryId = 2, assignedToUserId = 2),
            Task(3, "Plant", dueDate = "2026-11-04"),
            Task(4, "Dream"),
            Task(5, "Rake", dueDate = "2026-10-01", categoryId = 1, done = true),
        ),
        categories = listOf(
            TaskCategory(id = 1, name = "Garden"),
            TaskCategory(id = 2, name = "Balcony"),
        ),
        users = listOf(
            User(id = 1, username = "alice", displayName = "Alice"),
            User(id = 2, username = "bob", displayName = "Bob"),
        ),
        userfields = emptyList(),
    )

    private fun build(
        filter: TasksFilter = TasksFilter(),
        sort: Pair<TasksOverviewSortField, Boolean> = TasksOverviewSortField.DUE to true,
        grouping: TasksOverviewGrouping = TasksOverviewGrouping.NONE,
        query: String = "",
    ) = buildTasksOverview(
        data = data,
        filter = filter,
        sort = TableColumn.Own(sort.first) to sort.second,
        grouping = TableColumn.Own(grouping),
        query = query,
        today = today,
        dueSoonDays = 5,
    )

    private fun names(groups: List<TaskGroup>) = groups.flatMap { it.rows }.map { it.task.name }

    @Test
    fun doneTasksOnlyShowWhenAskedFor() {
        assertEquals(listOf("Mow", "Water", "Plant", "Dream"), names(build()))
        assertEquals(
            listOf("Rake", "Mow", "Water", "Plant", "Dream"),
            names(build(TasksFilter(showDone = true))),
        )
    }

    @Test
    fun rowsTellTheirStatusCategoryAndUser() {
        val row = build().single().rows.first()

        assertEquals(TaskDueStatus.OVERDUE, row.dueStatus)
        assertEquals("Garden", row.categoryName)
        assertEquals("Alice", row.assignedUserName)
    }

    @Test
    fun dueSoonIncludesWhatIsDueTodayLikeGrocysHiddenColumn() {
        assertEquals(
            listOf("Water", "Plant"),
            names(build(TasksFilter(status = TaskDueStatus.DUE_SOON))),
        )
        assertEquals(
            listOf("Water"),
            names(build(TasksFilter(status = TaskDueStatus.DUE_TODAY))),
        )
    }

    @Test
    fun categoryFilterKnowsUncategorized() {
        assertEquals(listOf("Mow"), names(build(TasksFilter(categoryId = 1))))
        assertEquals(
            listOf("Plant", "Dream"),
            names(build(TasksFilter(categoryId = UNCATEGORIZED))),
        )
    }

    @Test
    fun assignmentFilterTakesAUser() {
        assertEquals(listOf("Water"), names(build(TasksFilter(userId = 2))))
    }

    @Test
    fun searchFindsCategoriesAndUsers() {
        assertEquals(listOf("Mow"), names(build(query = "Garden")))
        assertEquals(listOf("Water"), names(build(query = "Bob")))
    }

    @Test
    fun tasksWithoutADateSortLastInBothDirections() {
        assertEquals(
            listOf("Plant", "Water", "Mow", "Dream"),
            names(build(sort = TasksOverviewSortField.DUE to false)),
        )
    }

    @Test
    fun sortsByNameDownwardsToo() {
        assertEquals(
            listOf("Water", "Plant", "Mow", "Dream"),
            names(build(sort = TasksOverviewSortField.NAME to false)),
        )
    }

    @Test
    fun groupsByCategoryWithUncategorizedLast() {
        val groups = build(grouping = TasksOverviewGrouping.CATEGORY)

        assertEquals(listOf("Balcony", "Garden", null), groups.map { it.title })
        assertEquals(listOf(false, false, true), groups.map { it.isUnset })
    }

    @Test
    fun groupsByDueDateInOrder() {
        val groups = build(grouping = TasksOverviewGrouping.DUE)

        assertEquals(
            listOf("2026-11-01", "2026-11-02", "2026-11-04", null),
            groups.map { it.title },
        )
    }

    @Test
    fun groupsByAssignedUser() {
        val groups = build(grouping = TasksOverviewGrouping.ASSIGNED_TO)

        assertEquals(listOf("Alice", "Bob", null), groups.map { it.title })
    }

    @Test
    fun nothingToShowIsNoGroup() {
        assertEquals(emptyList(), build(query = "nothing like this"))
    }

    @Test
    fun aHiddenColumnFallsBackToGrocysDefaultOrder() {
        val options = TasksOverviewOptions(sortField = TasksOverviewSortField.CATEGORY)
        val shown = listOf(TasksOverviewField.DUE)

        assertEquals(
            TableColumn.Own(TasksOverviewSortField.DUE) to true,
            effectiveTaskSort(options, shown),
        )
        assertEquals(TasksOverviewSortField.NAME, defaultTaskSort(emptyList()))
    }

    @Test
    fun userfieldColumnsOnlyWhenTheServerHasThem() {
        assertEquals(false, TasksOverviewField.USERFIELDS in availableTaskFields(false))
        assertEquals(true, TasksOverviewField.USERFIELDS in availableTaskFields(true))
    }

    private val userfieldData = TasksData(
        tasks = listOf(
            Task(1, "Mow", userfields = mapOf("hours" to "10", "area" to "Back")),
            Task(2, "Water", userfields = mapOf("hours" to "2.5", "area" to "Front")),
            Task(3, "Plant"),
        ),
        categories = emptyList(),
        users = emptyList(),
        userfields = listOf(
            Userfield(1, "tasks", "hours", "Hours", UserfieldType.NUMBER_DECIMAL, true),
            Userfield(2, "tasks", "area", "Area", UserfieldType.TEXT_SINGLE_LINE, true),
            Userfield(3, "tasks", "file", "File", UserfieldType.FILE, true),
        ),
    )

    private fun byUserfield(
        sort: Pair<TableColumn<TasksOverviewSortField>, Boolean> =
            TableColumn.Own(TasksOverviewSortField.NAME) to true,
        grouping: TableColumn<TasksOverviewGrouping> = TableColumn.Own(TasksOverviewGrouping.NONE),
    ) = buildTasksOverview(
        data = userfieldData,
        filter = TasksFilter(),
        sort = sort,
        grouping = grouping,
        query = "",
        today = today,
        dueSoonDays = 5,
    )

    @Test
    fun numberUserfieldsSortByTheirValueWithoutOnesLast() {
        val column = TableColumn.OfUserfield("hours")

        assertEquals(listOf("Water", "Mow", "Plant"), names(byUserfield(sort = column to true)))
        assertEquals(listOf("Mow", "Water", "Plant"), names(byUserfield(sort = column to false)))
    }

    @Test
    fun groupsByAUserfieldWithoutOnesLast() {
        val groups = byUserfield(grouping = TableColumn.OfUserfield("area"))

        assertEquals(listOf("Back", "Front", null), groups.map { it.title })
        assertEquals(listOf(false, false, true), groups.map { it.isUnset })
    }

    @Test
    fun userfieldColumnsAreOfferedLikeTheOthersExceptFiles() {
        val columns = sortableUserfieldColumns(userfieldData.userfields)

        assertEquals(listOf("hours", "area"), columns.map { it.name })
        assertEquals(true, TableColumn.OfUserfield("area") in availableTaskGroupings(columns))
        assertEquals(
            true,
            TableColumn.OfUserfield("area") in
                availableTaskSortColumns(listOf(TasksOverviewField.USERFIELDS), columns),
        )
    }

    @Test
    fun hiddenUserfieldColumnsAreNoSortChoiceAndFallBack() {
        val columns = sortableUserfieldColumns(userfieldData.userfields)
        val options = TasksOverviewOptions(sortUserfield = "area", ascending = false)
        val hidden = listOf(TasksOverviewField.DUE)

        assertEquals(
            false,
            TableColumn.OfUserfield("area") in availableTaskSortColumns(hidden, columns),
        )
        assertEquals(
            TableColumn.Own(TasksOverviewSortField.DUE) to true,
            effectiveTaskSort(options, hidden, columns),
        )
    }

    @Test
    fun aGroupingByADeletedUserfieldEndsTheGrouping() {
        val options = TasksOverviewOptions(groupingUserfield = "gone")

        assertEquals(
            TableColumn.Own(TasksOverviewGrouping.NONE),
            effectiveTaskGrouping(options, availableTaskGroupings(emptyList())),
        )
    }
}
