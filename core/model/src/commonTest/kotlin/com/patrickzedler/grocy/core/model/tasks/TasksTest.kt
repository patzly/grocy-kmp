package com.patrickzedler.grocy.core.model.tasks

import com.patrickzedler.grocy.core.model.text.epochDayOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TasksTest {

    private val today = epochDayOf("2026-11-02")!!

    @Test
    fun aTaskIsDueUntilItsDayEnds() {
        assertEquals(TaskDueStatus.OVERDUE, taskDueStatusOf("2026-11-01", today, 5))
        assertEquals(TaskDueStatus.DUE_TODAY, taskDueStatusOf("2026-11-02", today, 5))
        assertEquals(TaskDueStatus.DUE_SOON, taskDueStatusOf("2026-11-07", today, 5))
        assertNull(taskDueStatusOf("2026-11-08", today, 5))
    }

    @Test
    fun zeroDueSoonDaysNeverMeanDueSoon() {
        assertNull(taskDueStatusOf("2026-11-03", today, 0))
        assertEquals(TaskDueStatus.DUE_TODAY, taskDueStatusOf("2026-11-02", today, 0))
    }

    @Test
    fun aTaskWithoutADateHasNoStatus() {
        assertNull(taskDueStatusOf(null, today, 5))
    }

    @Test
    fun aDueDateWithATimeCountsByItsDay() {
        assertEquals(TaskDueStatus.DUE_TODAY, taskDueStatusOf("2026-11-02 00:00:00", today, 5))
    }

    @Test
    fun summaryCountsLikeGrocysMessagesWithoutDoneTasks() {
        val tasks = listOf(
            Task(1, "Late", dueDate = "2026-10-30"),
            Task(2, "Today", dueDate = "2026-11-02"),
            Task(3, "Soon", dueDate = "2026-11-04"),
            Task(4, "Later", dueDate = "2026-12-24"),
            Task(5, "Whenever"),
            Task(6, "Done and late", dueDate = "2026-10-30", done = true),
        )

        val summary = tasksSummaryOf(tasks, today, dueSoonDays = 5)

        assertEquals(TasksSummary(count = 5, overdue = 1, dueToday = 1, dueSoon = 2), summary)
    }
}
