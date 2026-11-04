package com.patrickzedler.grocy.feature.batteries.impl

import com.patrickzedler.grocy.core.model.batteries.BatteriesJournalField
import com.patrickzedler.grocy.core.model.batteries.BatteriesJournalGrouping
import com.patrickzedler.grocy.core.model.batteries.BatteriesJournalOptions
import com.patrickzedler.grocy.core.model.batteries.BatteriesJournalSortField
import com.patrickzedler.grocy.core.model.batteries.BatteriesOverviewField
import com.patrickzedler.grocy.core.model.batteries.BatteriesOverviewGrouping
import com.patrickzedler.grocy.core.model.batteries.BatteriesOverviewOptions
import com.patrickzedler.grocy.core.model.batteries.BatteriesOverviewSortField
import com.patrickzedler.grocy.core.model.batteries.BatteryChargeCycle
import com.patrickzedler.grocy.core.model.batteries.BatteryDueStatus
import com.patrickzedler.grocy.core.model.batteries.CurrentBattery
import com.patrickzedler.grocy.core.model.masterdata.Battery
import com.patrickzedler.grocy.core.model.masterdata.TableColumn
import com.patrickzedler.grocy.core.model.masterdata.Userfield
import com.patrickzedler.grocy.core.model.masterdata.UserfieldType
import com.patrickzedler.grocy.core.model.text.epochDayOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BatteriesListsTest {
    private val now = "2026-11-04 12:00:00"
    private val room = Userfield(
        id = 1,
        entity = "batteries",
        name = "room",
        caption = "Room",
        type = UserfieldType.TEXT_SINGLE_LINE,
        showAsColumnInTables = true,
    )
    private val data = BatteriesOverviewData(
        current = listOf(
            CurrentBattery(1, "2026-10-01 08:00:00", "2026-11-01 08:00:00"),
            CurrentBattery(2, "2026-11-01 08:00:00", "2026-11-04 20:00:00"),
            CurrentBattery(3, "2026-10-20 08:00:00", "2026-11-06 08:00:00"),
            CurrentBattery(4, null, null),
            // Downloaded before the battery itself, waits for the next download
            CurrentBattery(5, null, "2026-11-01 08:00:00"),
        ),
        batteries = listOf(
            Battery(1, "Remote", usedIn = "TV", userfields = mapOf("room" to "Attic")),
            Battery(2, "Scale", usedIn = "Bath"),
            Battery(3, "Torch", usedIn = "TV", userfields = mapOf("room" to "Cellar")),
            Battery(4, "Spare", usedIn = ""),
        ),
        userfields = listOf(room),
    )

    private fun build(
        status: BatteryDueStatus? = null,
        sort: Pair<TableColumn<BatteriesOverviewSortField>, Boolean> =
            TableColumn.Own(BatteriesOverviewSortField.NEXT_CHARGE) to true,
        grouping: TableColumn<BatteriesOverviewGrouping> =
            TableColumn.Own(BatteriesOverviewGrouping.NONE),
        query: String = "",
    ) = buildBatteriesOverview(data, status, sort, grouping, query, now, dueSoonDays = 5)

    @Test
    fun byDefaultWhatIsDueComesFirstAndNoScheduleLast() {
        val rows = build().single().rows

        assertEquals(listOf("Remote", "Scale", "Torch", "Spare"), rows.map { it.battery.name })
        assertEquals(BatteryDueStatus.OVERDUE, rows[0].dueStatus)
        assertNull(rows[3].dueStatus)
    }

    @Test
    fun withoutAValueRowsStayLastInBothDirections() {
        val rows = build(
            sort = TableColumn.Own(BatteriesOverviewSortField.NEXT_CHARGE) to false,
        ).single().rows

        assertEquals(listOf("Torch", "Scale", "Remote", "Spare"), rows.map { it.battery.name })
    }

    @Test
    fun dueSoonIncludesWhatIsDueTodayLikeGrocysHiddenColumn() {
        val names = build(status = BatteryDueStatus.DUE_SOON).single().rows.map { it.battery.name }

        assertEquals(listOf("Scale", "Torch"), names)
    }

    @Test
    fun nothingMatchingLeavesNoGroup() {
        assertTrue(build(query = "zzzzz").isEmpty())
    }

    @Test
    fun theSearchLooksAtWhereItIsUsed() {
        val names = build(query = "Bath").single().rows.map { it.battery.name }

        assertEquals(listOf("Scale"), names)
    }

    @Test
    fun groupingByUsedInPutsRowsWithoutOneLast() {
        val groups = build(grouping = TableColumn.Own(BatteriesOverviewGrouping.USED_IN))

        assertEquals(listOf("Bath", "TV", null), groups.map { it.title })
        assertTrue(groups.last().isUnassigned)
        assertEquals(listOf("Spare"), groups.last().rows.map { it.battery.name })
    }

    @Test
    fun userfieldColumnsSortAndGroup() {
        val sorted = build(sort = TableColumn.OfUserfield("room") to false).single().rows
        val groups = build(grouping = TableColumn.OfUserfield("room"))

        assertEquals("Torch", sorted.first().battery.name)
        assertEquals(listOf("Attic", "Cellar", null), groups.map { it.title })
    }

    @Test
    fun aHiddenColumnFallsBackToGrocysDefault() {
        val options = BatteriesOverviewOptions(
            sortField = BatteriesOverviewSortField.LAST_CHARGED,
            ascending = false,
        )

        val sort = effectiveBatterySort(options, listOf(BatteriesOverviewField.NEXT_CHARGE))
        val byName = effectiveBatterySort(options, emptyList())

        assertEquals(TableColumn.Own(BatteriesOverviewSortField.NEXT_CHARGE) to true, sort)
        assertEquals(TableColumn.Own(BatteriesOverviewSortField.NAME) to true, byName)
    }

    @Test
    fun aDeletedUserfieldColumnEndsItsGrouping() {
        val options = BatteriesOverviewOptions(groupingUserfield = "gone")

        assertEquals(
            TableColumn.Own(BatteriesOverviewGrouping.NONE),
            effectiveBatteryGrouping(options, availableBatteryGroupings(listOf(room))),
        )
    }

    @Test
    fun userfieldsAreOnlyOfferedWithColumns() {
        assertTrue(BatteriesOverviewField.USERFIELDS !in availableBatteryFields(false))
        assertTrue(BatteriesOverviewField.USERFIELDS in availableBatteryFields(true))
    }

    private val cycles = listOf(
        BatteryChargeCycle(3, 2, "2026-10-03 08:00:00"),
        BatteryChargeCycle(2, 1, "2026-10-02 08:00:00", undone = true),
        BatteryChargeCycle(1, 9, "2026-10-01 08:00:00"),
    )
    private val batteries = listOf(Battery(1, "Remote"), Battery(2, "Scale"))

    @Test
    fun theJournalLeavesOutDeletedBatteriesAndFilters() {
        val all = buildBatteriesJournal(
            cycles,
            batteries,
            emptyList(),
            BatteriesJournalFilter(),
            "",
        ).single().rows
        val one = buildBatteriesJournal(
            cycles,
            batteries,
            emptyList(),
            BatteriesJournalFilter(batteryId = 1),
            "",
        ).single().rows

        assertEquals(listOf(3, 2), all.map { it.cycle.id })
        assertEquals(listOf(2), one.map { it.cycle.id })
    }

    @Test
    fun theJournalGroupsAndSortsByBattery() {
        val groups = buildBatteriesJournal(
            cycles,
            batteries,
            emptyList(),
            BatteriesJournalFilter(),
            "",
            sort = BatteriesJournalSortField.BATTERY to false,
            grouping = BatteriesJournalGrouping.BATTERY,
        )

        assertEquals(listOf("Remote", "Scale"), groups.map { it.title })
    }

    @Test
    fun theJournalsDefaultFollowsItsColumns() {
        val options = BatteriesJournalOptions()

        assertEquals(
            BatteriesJournalSortField.TRACKED_TIME to false,
            effectiveBatteriesJournalSort(options, listOf(BatteriesJournalField.TRACKED_TIME)),
        )
        assertEquals(
            BatteriesJournalSortField.BATTERY to true,
            effectiveBatteriesJournalSort(options, emptyList()),
        )
    }

    @Test
    fun theDateRangeIsGrocysAndEverythingHasNoStart() {
        val today = epochDayOf("2026-11-04")!!

        assertEquals("2024-11-04", batteriesJournalSince(today, BATTERIES_JOURNAL_DEFAULT_MONTHS))
        assertNull(batteriesJournalSince(today, BATTERIES_JOURNAL_ALL_MONTHS))
    }
}
