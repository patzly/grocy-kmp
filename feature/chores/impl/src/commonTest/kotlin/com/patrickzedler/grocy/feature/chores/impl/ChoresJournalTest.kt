package com.patrickzedler.grocy.feature.chores.impl

import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.User
import com.patrickzedler.grocy.core.model.chores.ChoreExecution
import com.patrickzedler.grocy.core.model.chores.ChoresJournalField
import com.patrickzedler.grocy.core.model.chores.ChoresJournalGrouping
import com.patrickzedler.grocy.core.model.chores.ChoresJournalOptions
import com.patrickzedler.grocy.core.model.chores.ChoresJournalSortField
import com.patrickzedler.grocy.core.model.masterdata.Chore
import com.patrickzedler.grocy.core.model.masterdata.Userfield
import com.patrickzedler.grocy.core.model.masterdata.UserfieldType
import com.patrickzedler.grocy.core.model.text.epochDayOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChoresJournalTest {

    private val executions = listOf(
        ChoreExecution(
            id = 3,
            choreId = 1,
            trackedTime = "2026-10-20 08:00:00",
            doneByUserId = 1,
            userfields = mapOf("mood" to "Good"),
        ),
        ChoreExecution(id = 2, choreId = 2, trackedTime = "2026-10-10", doneByUserId = 2),
        ChoreExecution(id = 1, choreId = 9, trackedTime = "2026-10-01 08:00:00"),
    )
    private val chores = listOf(
        Chore(1, "Vacuum"),
        Chore(2, "Water plants", trackDateOnly = true),
    )
    private val users = listOf(User(1, "alice", "Alice"), User(2, "bob", "Bob"))
    private val userfields = listOf(
        Userfield(1, "chores_log", "mood", "Mood", UserfieldType.TEXT_SINGLE_LINE, true),
    )

    private fun journal(filter: ChoresJournalFilter = ChoresJournalFilter(), query: String = "") =
        buildChoresJournal(executions, chores, users, userfields, filter, query)
            .flatMap { it.rows }

    @Test
    fun executionsOfDeletedChoresAreLeftOut() {
        assertEquals(listOf(3, 2), journal().map { it.execution.id })
    }

    @Test
    fun rowsNameChoreUserAndUserfields() {
        val row = journal().first()

        assertEquals("Vacuum", row.choreName)
        assertEquals("Alice", row.userName)
        assertEquals(listOf("Mood" to "Good"), row.userfields)
        assertTrue(journal().last().trackDateOnly)
    }

    @Test
    fun filtersByChore() {
        assertEquals(
            listOf(2),
            journal(ChoresJournalFilter(choreId = 2)).map { it.execution.id },
        )
    }

    @Test
    fun searchFindsChoreUserAndUserfields() {
        assertEquals(listOf(2), journal(query = "plants").map { it.execution.id })
        assertEquals(listOf(2), journal(query = "bob").map { it.execution.id })
        assertEquals(listOf(3), journal(query = "good").map { it.execution.id })
    }

    @Test
    fun rangeIsGrocysMonthsBack() {
        val today = epochDayOf("2026-11-02")!!

        assertEquals("2025-11-02", choresJournalSince(today, CHORES_JOURNAL_DEFAULT_MONTHS))
        assertEquals("2026-10-02", choresJournalSince(today, 1))
        assertNull(choresJournalSince(today, CHORES_JOURNAL_ALL_MONTHS))
    }

    @Test
    fun theDefaultRangeIsNoFilter() {
        assertFalse(ChoresJournalFilter().isActive)
        assertTrue(ChoresJournalFilter(months = 1).isActive)
        assertTrue(ChoresJournalFilter(choreId = 1).isActive)
    }

    private val forSorting = listOf(
        ChoreExecution(
            id = 1,
            choreId = 1,
            trackedTime = "2026-10-01 08:00:00",
            scheduledExecutionTime = "2026-10-05 08:00:00",
            createdTimestamp = "2026-10-09 08:00:00",
            doneByUserId = 2,
        ),
        ChoreExecution(
            id = 2,
            choreId = 2,
            trackedTime = "2026-10-02 08:00:00",
            createdTimestamp = "2026-10-08 08:00:00",
            doneByUserId = 1,
        ),
        ChoreExecution(
            id = 3,
            choreId = 1,
            trackedTime = "2026-10-03 08:00:00",
            scheduledExecutionTime = "2026-10-04 08:00:00",
            createdTimestamp = "2026-10-07 08:00:00",
        ),
    )

    private fun groups(
        grouping: ChoresJournalGrouping = ChoresJournalGrouping.NONE,
        sort: Pair<ChoresJournalSortField, Boolean> = ChoresJournalSortField.TRACKED_TIME to false,
    ) = buildChoresJournal(
        executions = forSorting,
        chores = chores,
        users = users,
        userfields = userfields,
        filter = ChoresJournalFilter(),
        query = "",
        sort = sort,
        grouping = grouping,
    )

    private fun sortedIds(field: ChoresJournalSortField, ascending: Boolean) =
        groups(sort = field to ascending).single().rows.map { it.execution.id }

    @Test
    fun theDefaultIsNewestTrackedFirst() {
        assertEquals(listOf(3, 2, 1), groups().single().rows.map { it.execution.id })
    }

    @Test
    fun sortsByTimeOfTracking() {
        assertEquals(listOf(3, 2, 1), sortedIds(ChoresJournalSortField.TIME_OF_TRACKING, true))
        assertEquals(listOf(1, 2, 3), sortedIds(ChoresJournalSortField.TIME_OF_TRACKING, false))
    }

    @Test
    fun rowsWithoutAValueStayLastInBothDirections() {
        assertEquals(
            listOf(3, 1, 2),
            sortedIds(ChoresJournalSortField.SCHEDULED_TRACKING_TIME, true),
        )
        assertEquals(
            listOf(1, 3, 2),
            sortedIds(ChoresJournalSortField.SCHEDULED_TRACKING_TIME, false),
        )
        assertEquals(listOf(2, 1, 3), sortedIds(ChoresJournalSortField.DONE_BY, true))
        assertEquals(listOf(1, 2, 3), sortedIds(ChoresJournalSortField.DONE_BY, false))
    }

    @Test
    fun groupsByChoreInAlphabeticalOrderKeepingTheSortInside() {
        val groups = groups(ChoresJournalGrouping.CHORE)

        assertEquals(listOf("Vacuum", "Water plants"), groups.map { it.title })
        assertEquals(listOf(3, 1), groups.first().rows.map { it.execution.id })
    }

    @Test
    fun groupsByDoneByWithTheUnknownUserLast() {
        val groups = groups(ChoresJournalGrouping.DONE_BY)

        assertEquals(listOf("Alice", "Bob", null), groups.map { it.title })
        assertEquals(listOf(false, false, true), groups.map { it.isUnknownUser })
        assertEquals(listOf(3), groups.last().rows.map { it.execution.id })
    }

    @Test
    fun anEmptyJournalHasNoGroups() {
        val filter = ChoresJournalFilter(choreId = 99)

        assertTrue(
            buildChoresJournal(forSorting, chores, users, userfields, filter, "").isEmpty(),
        )
    }

    @Test
    fun withoutAssignmentsDoneByIsNotOffered() {
        val config = ServerConfig(choresAssignments = false)

        assertFalse(
            ChoresJournalField.DONE_BY in availableChoresJournalFields(config, true),
        )
        assertEquals(
            listOf(ChoresJournalGrouping.NONE, ChoresJournalGrouping.CHORE),
            availableChoresJournalGroupings(config),
        )
    }

    @Test
    fun userfieldsAreOnlyOfferedWithColumns() {
        val config = ServerConfig()

        assertFalse(
            ChoresJournalField.USERFIELDS in availableChoresJournalFields(config, false),
        )
        assertTrue(
            ChoresJournalField.USERFIELDS in availableChoresJournalFields(config, true),
        )
    }

    @Test
    fun onlyShownColumnsCanBeSortedBy() {
        assertEquals(
            listOf(ChoresJournalSortField.CHORE, ChoresJournalSortField.TIME_OF_TRACKING),
            availableChoresJournalSortFields(listOf(ChoresJournalField.TIME_OF_TRACKING)),
        )
    }

    @Test
    fun aHiddenSortColumnFallsBackToTheDefault() {
        val options = ChoresJournalOptions(
            sortField = ChoresJournalSortField.TIME_OF_TRACKING,
            ascending = true,
        )

        assertEquals(
            ChoresJournalSortField.TRACKED_TIME to false,
            effectiveChoresJournalSort(options, listOf(ChoresJournalField.TRACKED_TIME)),
        )
        assertEquals(
            ChoresJournalSortField.CHORE to true,
            effectiveChoresJournalSort(options, emptyList()),
        )
    }
}
