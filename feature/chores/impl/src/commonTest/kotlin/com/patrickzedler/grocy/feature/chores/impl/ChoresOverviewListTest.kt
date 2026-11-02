package com.patrickzedler.grocy.feature.chores.impl

import com.patrickzedler.grocy.core.forms.sortableUserfieldColumns
import com.patrickzedler.grocy.core.model.User
import com.patrickzedler.grocy.core.model.chores.ChoreDueStatus
import com.patrickzedler.grocy.core.model.chores.ChoresOverviewField
import com.patrickzedler.grocy.core.model.chores.ChoresOverviewGrouping
import com.patrickzedler.grocy.core.model.chores.ChoresOverviewOptions
import com.patrickzedler.grocy.core.model.chores.ChoresOverviewSortField
import com.patrickzedler.grocy.core.model.chores.CurrentChore
import com.patrickzedler.grocy.core.model.masterdata.Chore
import com.patrickzedler.grocy.core.model.masterdata.ChorePeriodType
import com.patrickzedler.grocy.core.model.masterdata.TableColumn
import com.patrickzedler.grocy.core.model.masterdata.Userfield
import com.patrickzedler.grocy.core.model.masterdata.UserfieldType
import com.patrickzedler.grocy.core.model.server.ServerConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChoresOverviewListTest {

    private val data = ChoresOverviewData(
        current = listOf(
            CurrentChore(
                choreId = 1,
                name = "Vacuum",
                nextEstimatedExecutionTime = "2026-11-03 08:00:00",
                lastTrackedTime = "2026-10-01 08:00:00",
                assignedUserId = 2,
            ),
            CurrentChore(choreId = 2, name = "Water plants"),
            CurrentChore(
                choreId = 3,
                name = "Clean oven",
                nextEstimatedExecutionTime = "2026-11-01 08:00:00",
                lastTrackedTime = "2026-10-20 08:00:00",
                assignedUserId = 1,
            ),
            CurrentChore(
                choreId = 4,
                name = "Dust",
                nextEstimatedExecutionTime = "2026-11-02 18:00:00",
            ),
        ),
        chores = listOf(
            Chore(1, "Vacuum", periodType = ChorePeriodType.WEEKLY),
            Chore(2, "Water plants", periodType = ChorePeriodType.MANUALLY),
            Chore(3, "Clean oven", userfields = mapOf("room" to "Kitchen", "hidden" to "x")),
            Chore(4, "Dust"),
        ),
        users = listOf(User(1, "alice", "Alice"), User(2, "bob", "Bob")),
        userfields = listOf(
            Userfield(1, "chores", "room", "Room", UserfieldType.TEXT_SINGLE_LINE, true),
            Userfield(2, "chores", "hidden", "Hidden", UserfieldType.TEXT_SINGLE_LINE, false),
        ),
    )

    private val nextFirst = ChoresOverviewSortField.NEXT_ESTIMATED_TRACKING to true

    private fun overview(
        filter: ChoresFilter = ChoresFilter(),
        sort: Pair<ChoresOverviewSortField, Boolean> = nextFirst,
        grouping: ChoresOverviewGrouping = ChoresOverviewGrouping.NONE,
        query: String = "",
    ) = buildChoresOverview(
        data = data,
        filter = filter,
        sort = TableColumn.Own(sort.first) to sort.second,
        grouping = TableColumn.Own(grouping),
        query = query,
        now = "2026-11-02 12:00:00",
        dueSoonDays = 5,
    )

    private fun names(vararg args: List<ChoreGroup>) =
        args.single().flatMap { group -> group.rows.map { it.chore.name } }

    @Test
    fun choresWithoutScheduleComeLast() {
        assertEquals(
            listOf("Clean oven", "Dust", "Vacuum", "Water plants"),
            names(overview()),
        )
    }

    @Test
    fun descendingKeepsChoresWithoutValueLast() {
        assertEquals(
            listOf("Vacuum", "Dust", "Clean oven", "Water plants"),
            names(overview(sort = ChoresOverviewSortField.NEXT_ESTIMATED_TRACKING to false)),
        )
    }

    @Test
    fun sortsByNameAndLastTracked() {
        assertEquals(
            listOf("Clean oven", "Dust", "Vacuum", "Water plants"),
            names(overview(sort = ChoresOverviewSortField.NAME to true)),
        )
        assertEquals(
            listOf("Water plants", "Vacuum", "Dust", "Clean oven"),
            names(overview(sort = ChoresOverviewSortField.NAME to false)),
        )
        assertEquals(
            listOf("Clean oven", "Vacuum", "Dust", "Water plants"),
            names(overview(sort = ChoresOverviewSortField.LAST_TRACKED to false)),
        )
    }

    @Test
    fun rowsTellTheirDueStatus() {
        val statuses = overview().single().rows.associate { it.chore.name to it.dueStatus }

        assertEquals(ChoreDueStatus.OVERDUE, statuses["Clean oven"])
        assertEquals(ChoreDueStatus.DUE_TODAY, statuses["Dust"])
        assertEquals(ChoreDueStatus.DUE_SOON, statuses["Vacuum"])
        assertNull(statuses["Water plants"])
    }

    @Test
    fun dueSoonFilterIncludesDueTodayLikeGrocy() {
        assertEquals(
            listOf("Dust", "Vacuum"),
            names(overview(filter = ChoresFilter(status = ChoreDueStatus.DUE_SOON))),
        )
        assertEquals(
            listOf("Dust"),
            names(overview(filter = ChoresFilter(status = ChoreDueStatus.DUE_TODAY))),
        )
    }

    @Test
    fun filterByAssignedUser() {
        assertEquals(listOf("Vacuum"), names(overview(filter = ChoresFilter(userId = 2))))
    }

    @Test
    fun onlyChoresWithoutScheduleCannotBeSkipped() {
        val rows = overview().single().rows.associateBy { it.chore.name }

        assertFalse(rows.getValue("Water plants").canSkip)
        assertTrue(rows.getValue("Vacuum").canSkip)
    }

    @Test
    fun rowsShowTheUserfieldsGrocyShowsAsColumns() {
        val row = overview().single().rows.first { it.chore.name == "Clean oven" }

        assertEquals(listOf("Room" to "Kitchen"), row.userfields)
    }

    @Test
    fun searchFindsNamesUsersAndUserfields() {
        assertEquals(listOf("Vacuum"), names(overview(query = "vacu")))
        assertEquals(listOf("Vacuum"), names(overview(query = "bob")))
        assertEquals(listOf("Clean oven"), names(overview(query = "kitchen")))
    }

    @Test
    fun groupsByAssignedUserWithTheUnassignedLast() {
        val groups = overview(grouping = ChoresOverviewGrouping.ASSIGNED_TO)

        assertEquals(listOf("Alice", "Bob", null), groups.map { it.title })
        assertEquals(listOf(false, false, true), groups.map { it.isUnassigned })
        assertEquals(listOf("Dust", "Water plants"), groups.last().rows.map { it.chore.name })
    }

    @Test
    fun nothingFoundIsNoGroupAtAll() {
        assertTrue(overview(query = "zzzzzzzz").isEmpty())
    }

    @Test
    fun assignmentsNeedTheirFeature() {
        val without = ServerConfig(choresAssignments = false)

        assertFalse(
            ChoresOverviewField.ASSIGNED_TO in
                availableChoreFields(without, hasUserfieldColumns = true),
        )
        assertEquals(
            listOf(TableColumn.Own(ChoresOverviewGrouping.NONE)),
            availableChoreGroupings(without, emptyList()),
        )
    }

    @Test
    fun userfieldsAreOfferedOnlyWhenTheServerHasColumns() {
        assertFalse(
            ChoresOverviewField.USERFIELDS in
                availableChoreFields(ServerConfig(), hasUserfieldColumns = false),
        )
    }

    @Test
    fun aHiddenSortColumnSortsByGrocysDefaultAgain() {
        val options = ChoresOverviewOptions(
            sortField = ChoresOverviewSortField.LAST_TRACKED,
            ascending = false,
        )

        assertEquals(
            TableColumn.Own(ChoresOverviewSortField.LAST_TRACKED) to false,
            effectiveChoreSort(options, ChoresOverviewField.entries),
        )
        assertEquals(
            TableColumn.Own(ChoresOverviewSortField.NEXT_ESTIMATED_TRACKING) to true,
            effectiveChoreSort(options, listOf(ChoresOverviewField.NEXT_ESTIMATED_TRACKING)),
        )
        assertEquals(
            TableColumn.Own(ChoresOverviewSortField.NAME) to true,
            effectiveChoreSort(options, emptyList()),
        )
    }

    private val numberData = ChoresOverviewData(
        current = listOf(
            CurrentChore(choreId = 1, name = "Vacuum"),
            CurrentChore(choreId = 2, name = "Dust"),
            CurrentChore(choreId = 3, name = "Mop"),
        ),
        chores = listOf(
            Chore(1, "Vacuum", userfields = mapOf("minutes" to "30", "room" to "Hall")),
            Chore(2, "Dust", userfields = mapOf("minutes" to "5", "room" to "Attic")),
            Chore(3, "Mop"),
        ),
        users = emptyList(),
        userfields = listOf(
            Userfield(1, "chores", "minutes", "Minutes", UserfieldType.NUMBER_INTEGRAL, true),
            Userfield(2, "chores", "room", "Room", UserfieldType.TEXT_SINGLE_LINE, true),
            Userfield(3, "chores", "photo", "Photo", UserfieldType.IMAGE, true),
        ),
    )

    private fun byUserfield(
        sort: Pair<TableColumn<ChoresOverviewSortField>, Boolean> = TableColumn.Own(
            ChoresOverviewSortField.NAME,
        ) to true,
        grouping: TableColumn<ChoresOverviewGrouping> = TableColumn.Own(
            ChoresOverviewGrouping.NONE,
        ),
    ) = buildChoresOverview(
        data = numberData,
        filter = ChoresFilter(),
        sort = sort,
        grouping = grouping,
        query = "",
        now = "2026-11-02 12:00:00",
        dueSoonDays = 5,
    )

    @Test
    fun numberUserfieldsSortByTheirValueWithoutOnesLast() {
        val column = TableColumn.OfUserfield("minutes")

        assertEquals(
            listOf("Dust", "Vacuum", "Mop"),
            names(byUserfield(sort = column to true)),
        )
        assertEquals(
            listOf("Vacuum", "Dust", "Mop"),
            names(byUserfield(sort = column to false)),
        )
    }

    @Test
    fun groupsByAUserfieldWithoutOnesLast() {
        val groups = byUserfield(grouping = TableColumn.OfUserfield("room"))

        assertEquals(listOf("Attic", "Hall", null), groups.map { it.title })
        assertEquals(listOf(false, false, true), groups.map { it.isUnassigned })
    }

    @Test
    fun userfieldColumnsAreOfferedLikeTheOthersExceptPictures() {
        val columns = sortableUserfieldColumns(
            numberData.userfields,
        )

        assertEquals(listOf("minutes", "room"), columns.map { it.name })
        assertTrue(
            TableColumn.OfUserfield("room") in
                availableChoreGroupings(ServerConfig(), columns),
        )
        assertTrue(
            TableColumn.OfUserfield("room") in
                availableChoreSortColumns(listOf(ChoresOverviewField.USERFIELDS), columns),
        )
    }

    @Test
    fun hiddenUserfieldColumnsAreNoSortChoiceAndFallBack() {
        val columns = sortableUserfieldColumns(
            numberData.userfields,
        )
        val options = ChoresOverviewOptions(sortUserfield = "room", ascending = false)
        val hidden = listOf(ChoresOverviewField.NEXT_ESTIMATED_TRACKING)

        assertFalse(TableColumn.OfUserfield("room") in availableChoreSortColumns(hidden, columns))
        assertEquals(
            TableColumn.Own(ChoresOverviewSortField.NEXT_ESTIMATED_TRACKING) to true,
            effectiveChoreSort(options, hidden, columns),
        )
    }

    @Test
    fun aGroupingByADeletedUserfieldEndsTheGrouping() {
        val options = ChoresOverviewOptions(groupingUserfield = "gone")

        assertEquals(
            TableColumn.Own(ChoresOverviewGrouping.NONE),
            effectiveChoreGrouping(options, availableChoreGroupings(ServerConfig(), emptyList())),
        )
    }
}
