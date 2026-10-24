package com.patrickzedler.grocy.core.model.masterdata

import kotlin.test.Test
import kotlin.test.assertEquals

class ChoreTest {

    @Test
    fun weekdaysAreReadFromGrocysPeriodConfig() {
        val weekdays = Weekday.parse("friday, monday,unknown")

        assertEquals(setOf(Weekday.MONDAY, Weekday.FRIDAY), weekdays)
    }

    @Test
    fun weekdaysAreWrittenMondayFirstLikeGrocysForm() {
        val config = Weekday.format(setOf(Weekday.SUNDAY, Weekday.MONDAY, Weekday.WEDNESDAY))

        assertEquals("monday,wednesday,sunday", config)
    }

    @Test
    fun noWeekdaysAreAnEmptyConfig() {
        assertEquals(emptySet(), Weekday.parse(null))
        assertEquals("", Weekday.format(emptySet()))
    }

    @Test
    fun unknownTypesOfNewerGrocyVersionsFallBackToTheNeutralOnes() {
        assertEquals(ChorePeriodType.WEEKLY, ChorePeriodType.of("weekly"))
        assertEquals(ChorePeriodType.MANUALLY, ChorePeriodType.of("someday"))
        assertEquals(ChoreAssignmentType.RANDOM, ChoreAssignmentType.of("random"))
        assertEquals(ChoreAssignmentType.NO_ASSIGNMENT, ChoreAssignmentType.of(null))
    }
}
