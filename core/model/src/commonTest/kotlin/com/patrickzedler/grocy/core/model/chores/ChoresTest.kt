package com.patrickzedler.grocy.core.model.chores

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChoresTest {

    private val now = "2026-11-02 12:00:00"

    @Test
    fun dueStatusFollowsGrocysOverview() {
        fun status(next: String?, days: Int = 5) = choreDueStatusOf(next, now, days)

        assertEquals(ChoreDueStatus.OVERDUE, status("2026-11-02 11:59:59"))
        assertEquals(ChoreDueStatus.DUE_TODAY, status("2026-11-02 12:00:00"))
        assertEquals(ChoreDueStatus.DUE_TODAY, status("2026-11-02 23:59:59"))
        assertEquals(ChoreDueStatus.DUE_SOON, status("2026-11-03 00:00:00"))
        assertEquals(ChoreDueStatus.DUE_SOON, status("2026-11-07 12:00:00"))
        assertNull(status("2026-11-07 12:00:01"))
        assertNull(status(null))
    }

    @Test
    fun noDueSoonDaysHideDueSoonOnly() {
        assertNull(choreDueStatusOf("2026-11-03 00:00:00", now, dueSoonDays = 0))
        assertEquals(
            ChoreDueStatus.DUE_TODAY,
            choreDueStatusOf("2026-11-02 18:00:00", now, dueSoonDays = 0),
        )
    }

    @Test
    fun dueSoonCrossesMonthEnds() {
        assertEquals(
            ChoreDueStatus.DUE_SOON,
            choreDueStatusOf("2027-01-02 10:00:00", "2026-12-30 12:00:00", dueSoonDays = 5),
        )
    }

    @Test
    fun summaryCountsLikeGrocysMessages() {
        val chores = listOf(
            CurrentChore(1, "Overdue", nextEstimatedExecutionTime = "2026-11-01 08:00:00"),
            CurrentChore(
                choreId = 2,
                name = "Today",
                nextEstimatedExecutionTime = "2026-11-02 18:00:00",
                assignedUserId = 7,
            ),
            CurrentChore(3, "Soon", nextEstimatedExecutionTime = "2026-11-04 18:00:00"),
            CurrentChore(4, "Later", nextEstimatedExecutionTime = "2027-01-01 18:00:00"),
            CurrentChore(5, "Manually", assignedUserId = 8),
        )

        assertEquals(
            ChoresSummary(count = 5, overdue = 1, dueToday = 1, dueSoon = 2, assignedToMe = 1),
            choresSummaryOf(chores, now, dueSoonDays = 5, userId = 7),
        )
    }

    @Test
    fun summaryWithoutKnownUserAssignsNothingToMe() {
        val chores = listOf(CurrentChore(1, "Vacuum", assignedUserId = 7))

        assertEquals(0, choresSummaryOf(chores, now, dueSoonDays = 5, userId = null).assignedToMe)
    }

    @Test
    fun buttonsTrackTheNextScheduleOrNow() {
        val next = "2026-11-05 08:00:00"

        assertEquals(next, choreTrackedTime(next, false, now, atNextSchedule = true))
        assertEquals(now, choreTrackedTime(next, false, now, atNextSchedule = false))
        assertEquals(now, choreTrackedTime(null, false, now, atNextSchedule = true))
    }

    @Test
    fun choresThatTrackNoTimeSendOnlyTheDate() {
        val next = "2026-11-05 23:59:59"

        assertEquals("2026-11-05", choreTrackedTime(next, true, now, atNextSchedule = true))
        assertEquals("2026-11-02", choreTrackedTime(next, true, now, atNextSchedule = false))
    }

    @Test
    fun executionsTrackedAfterTheirScheduleAreLate() {
        fun execution(tracked: String, skipped: Boolean = false, scheduled: String? = now) =
            ChoreExecution(1, 1, tracked, scheduled, skipped = skipped)

        assertTrue(execution("2026-11-02 12:00:01").isLate)
        assertFalse(execution("2026-11-02 12:00:00").isLate)
        assertFalse(execution("2026-11-03 12:00:00", skipped = true).isLate)
        assertFalse(execution("2026-11-03 12:00:00", scheduled = null).isLate)
    }

    @Test
    fun optionsReadTheStoredFormat() {
        val options = ChoresOverviewOptions.of("LAST_TRACKED,USERFIELDS;ASSIGNED_TO;NAME;false")

        assertEquals(
            ChoresOverviewOptions(
                fields = setOf(ChoresOverviewField.LAST_TRACKED, ChoresOverviewField.USERFIELDS),
                grouping = ChoresOverviewGrouping.ASSIGNED_TO,
                sortField = ChoresOverviewSortField.NAME,
                ascending = false,
            ),
            options,
        )
        assertEquals("LAST_TRACKED,USERFIELDS;ASSIGNED_TO;NAME;false", options.stored())
    }

    @Test
    fun optionsWithoutStoredValuesAreGrocysDefaults() {
        val options = ChoresOverviewOptions.of(null)

        assertEquals(ChoresOverviewField.entries.toSet(), options.fields)
        assertEquals(ChoresOverviewGrouping.NONE, options.grouping)
        assertEquals(ChoresOverviewSortField.NEXT_ESTIMATED_TRACKING, options.sortField)
        assertTrue(options.ascending)
    }

    @Test
    fun unknownStoredOptionsFallBackToDefaults() {
        val options = ChoresOverviewOptions.of("OTHER,LAST_TRACKED;SOMETHING;ELSE;maybe")

        assertEquals(setOf(ChoresOverviewField.LAST_TRACKED), options.fields)
        assertEquals(ChoresOverviewGrouping.NONE, options.grouping)
        assertEquals(ChoresOverviewSortField.NEXT_ESTIMATED_TRACKING, options.sortField)
        assertTrue(options.ascending)
    }

    @Test
    fun noShownFieldsStayNoShownFields() {
        val options = ChoresOverviewOptions(fields = emptySet())

        assertEquals(emptySet(), ChoresOverviewOptions.of(options.stored()).fields)
    }
}
