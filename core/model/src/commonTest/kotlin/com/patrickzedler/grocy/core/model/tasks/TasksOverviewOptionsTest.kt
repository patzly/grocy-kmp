package com.patrickzedler.grocy.core.model.tasks

import com.patrickzedler.grocy.core.model.masterdata.TableColumn
import kotlin.test.Test
import kotlin.test.assertEquals

class TasksOverviewOptionsTest {

    @Test
    fun storedFormStaysReadable() {
        val options = TasksOverviewOptions.of("DUE,ASSIGNED_TO;CATEGORY;NAME;false")

        assertEquals(
            TasksOverviewOptions(
                fields = setOf(TasksOverviewField.DUE, TasksOverviewField.ASSIGNED_TO),
                grouping = TasksOverviewGrouping.CATEGORY,
                sortField = TasksOverviewSortField.NAME,
                ascending = false,
            ),
            options,
        )
    }

    @Test
    fun optionsSurviveStoringThem() {
        val options = TasksOverviewOptions(
            fields = emptySet(),
            grouping = TasksOverviewGrouping.DUE,
            sortField = TasksOverviewSortField.ASSIGNED_TO,
            ascending = false,
        )

        assertEquals(options, TasksOverviewOptions.of(options.stored()))
    }

    @Test
    fun missingOrUnknownValuesFallBackToGrocysDefaults() {
        assertEquals(TasksOverviewOptions(), TasksOverviewOptions.of(null))
        assertEquals(
            TasksOverviewOptions(fields = setOf(TasksOverviewField.DUE)),
            TasksOverviewOptions.of("DUE,GONE;GONE;GONE;maybe"),
        )
    }

    @Test
    fun userfieldColumnsOfStoredOptionsStayReadable() {
        val options = TasksOverviewOptions.of("DUE;NONE;NAME;false;area;;")

        assertEquals(TableColumn.OfUserfield("area"), options.sortColumn)
        assertEquals(TableColumn.Own(TasksOverviewGrouping.NONE), options.groupingColumn)
    }

    @Test
    fun choosingAnOwnColumnEndsTheUserfieldOne() {
        val options = TasksOverviewOptions()
            .withSort(TableColumn.OfUserfield("area"), ascending = false)
            .withGrouping(TableColumn.OfUserfield("floor"))
        assertEquals(options, TasksOverviewOptions.of(options.stored()))

        val own = options
            .withSort(TableColumn.Own(TasksOverviewSortField.NAME), ascending = true)
            .withGrouping(TableColumn.Own(TasksOverviewGrouping.CATEGORY))

        assertEquals(TableColumn.Own(TasksOverviewSortField.NAME), own.sortColumn)
        assertEquals(TableColumn.Own(TasksOverviewGrouping.CATEGORY), own.groupingColumn)
    }

    @Test
    fun optionsWithoutUserfieldColumnsKeepTheirStoredForm() {
        assertEquals("DUE;NONE;NAME;false", TasksOverviewOptions.of("DUE;NONE;NAME;false").stored())
    }
}
