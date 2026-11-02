package com.patrickzedler.grocy.core.model.chores

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ChoresJournalOptionsTest {

    @Test
    fun optionsReadTheStoredFormat() {
        val options = ChoresJournalOptions.of("TIME_OF_TRACKING,DONE_BY;CHORE;DONE_BY;true")

        assertEquals(
            ChoresJournalOptions(
                fields = setOf(ChoresJournalField.TIME_OF_TRACKING, ChoresJournalField.DONE_BY),
                grouping = ChoresJournalGrouping.CHORE,
                sortField = ChoresJournalSortField.DONE_BY,
                ascending = true,
            ),
            options,
        )
    }

    @Test
    fun optionsWithoutStoredValuesAreGrocysDefaults() {
        val options = ChoresJournalOptions.of(null)

        assertEquals(
            ChoresJournalField.entries.toSet() - ChoresJournalField.TIME_OF_TRACKING,
            options.fields,
        )
        assertEquals(ChoresJournalGrouping.NONE, options.grouping)
        assertEquals(ChoresJournalSortField.TRACKED_TIME, options.sortField)
        assertFalse(options.ascending)
    }

    @Test
    fun unknownStoredOptionsFallBackToDefaults() {
        val options = ChoresJournalOptions.of("OTHER,DONE_BY;SOMETHING;ELSE;maybe")

        assertEquals(setOf(ChoresJournalField.DONE_BY), options.fields)
        assertEquals(ChoresJournalGrouping.NONE, options.grouping)
        assertEquals(ChoresJournalSortField.TRACKED_TIME, options.sortField)
        assertFalse(options.ascending)
    }

    @Test
    fun optionsSurviveBeingStored() {
        val options = ChoresJournalOptions(
            fields = emptySet(),
            grouping = ChoresJournalGrouping.DONE_BY,
            sortField = ChoresJournalSortField.TIME_OF_TRACKING,
            ascending = true,
        )

        assertEquals(options, ChoresJournalOptions.of(options.stored()))
    }
}
