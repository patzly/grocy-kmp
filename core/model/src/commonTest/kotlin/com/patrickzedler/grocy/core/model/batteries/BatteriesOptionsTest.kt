package com.patrickzedler.grocy.core.model.batteries

import com.patrickzedler.grocy.core.model.masterdata.TableColumn
import kotlin.test.Test
import kotlin.test.assertEquals

class BatteriesOptionsTest {
    @Test
    fun overviewOptionsSurviveBeingStored() {
        val options = BatteriesOverviewOptions(
            fields = setOf(BatteriesOverviewField.USED_IN),
            grouping = BatteriesOverviewGrouping.USED_IN,
            sortField = BatteriesOverviewSortField.LAST_CHARGED,
            ascending = false,
        )

        assertEquals(options, BatteriesOverviewOptions.of(options.stored()))
    }

    @Test
    fun overviewReadsAFixedStoredForm() {
        val options = BatteriesOverviewOptions.of("USED_IN,NEXT_CHARGE;USED_IN;NAME;false;room;")

        assertEquals(
            setOf(BatteriesOverviewField.USED_IN, BatteriesOverviewField.NEXT_CHARGE),
            options.fields,
        )
        assertEquals(TableColumn.OfUserfield("room"), options.sortColumn)
        assertEquals(
            TableColumn.Own(BatteriesOverviewGrouping.USED_IN),
            options.groupingColumn,
        )
        assertEquals(false, options.ascending)
    }

    @Test
    fun overviewFallsBackToGrocysDefaults() {
        assertEquals(BatteriesOverviewOptions(), BatteriesOverviewOptions.of(null))
        assertEquals(
            TableColumn.Own(BatteriesOverviewSortField.NEXT_CHARGE),
            BatteriesOverviewOptions.of(null).sortColumn,
        )
    }

    @Test
    fun choosingAnOwnColumnEndsSortingByAUserfield() {
        val byUserfield = BatteriesOverviewOptions()
            .withSort(TableColumn.OfUserfield("room"), ascending = true)

        val sorted = byUserfield.withSort(
            TableColumn.Own(BatteriesOverviewSortField.NAME),
            ascending = true,
        )

        assertEquals(TableColumn.Own(BatteriesOverviewSortField.NAME), sorted.sortColumn)
    }

    @Test
    fun journalReadsAFixedStoredForm() {
        val options = BatteriesJournalOptions.of("TRACKED_TIME;BATTERY;BATTERY;true")

        assertEquals(setOf(BatteriesJournalField.TRACKED_TIME), options.fields)
        assertEquals(BatteriesJournalGrouping.BATTERY, options.grouping)
        assertEquals(BatteriesJournalSortField.BATTERY, options.sortField)
        assertEquals(true, options.ascending)
    }

    @Test
    fun journalOptionsSurviveBeingStored() {
        val options = BatteriesJournalOptions(grouping = BatteriesJournalGrouping.BATTERY)

        assertEquals(options, BatteriesJournalOptions.of(options.stored()))
    }
}
