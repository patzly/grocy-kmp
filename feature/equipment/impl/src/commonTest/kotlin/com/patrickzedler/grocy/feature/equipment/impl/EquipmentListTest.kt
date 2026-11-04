package com.patrickzedler.grocy.feature.equipment.impl

import com.patrickzedler.grocy.core.data.masterdata.PictureChange
import com.patrickzedler.grocy.core.forms.FieldError
import com.patrickzedler.grocy.core.forms.ObjectField
import com.patrickzedler.grocy.core.model.equipment.Equipment
import com.patrickzedler.grocy.core.model.equipment.EquipmentField
import com.patrickzedler.grocy.core.model.equipment.EquipmentGrouping
import com.patrickzedler.grocy.core.model.equipment.EquipmentOptions
import com.patrickzedler.grocy.core.model.equipment.EquipmentSortField
import com.patrickzedler.grocy.core.model.masterdata.TableColumn
import com.patrickzedler.grocy.core.model.masterdata.Userfield
import com.patrickzedler.grocy.core.model.masterdata.UserfieldType
import com.patrickzedler.grocy.feature.equipment.api.EquipmentDetailRoute
import com.patrickzedler.grocy.feature.equipment.api.EquipmentFormRoute
import com.patrickzedler.grocy.feature.equipment.api.EquipmentRoute
import com.patrickzedler.grocy.feature.masterdata.api.UserfieldsRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EquipmentListTest {
    private val room = Userfield(
        id = 1,
        entity = "equipment",
        name = "room",
        caption = "Room",
        type = UserfieldType.TEXT_SINGLE_LINE,
        showAsColumnInTables = true,
    )
    private val warranty = Userfield(
        id = 2,
        entity = "equipment",
        name = "warranty",
        caption = "Warranty",
        type = UserfieldType.FILE,
        showAsColumnInTables = true,
    )
    private val equipment = listOf(
        Equipment(1, "Drill", userfields = mapOf("room" to "Garage", "warranty" to "x_y")),
        Equipment(2, "Ladder"),
        Equipment(3, "Saw", userfields = mapOf("room" to "Attic")),
    )
    private val byName = TableColumn.Own(EquipmentSortField.NAME) to true
    private val ungrouped = TableColumn.Own(EquipmentGrouping.NONE)

    private fun names(groups: List<EquipmentGroup>) =
        groups.flatMap { group -> group.rows.map { it.equipment.name } }

    @Test
    fun fileUserfieldsAreTabsNoColumns() {
        val row = buildEquipmentList(equipment, listOf(room, warranty), byName, ungrouped, "")
            .single().rows.first()

        assertEquals(listOf("Room" to "Garage"), row.userfields)
    }

    @Test
    fun sortsByNameAndUserfieldsWithEmptyOnesLast() {
        val down = TableColumn.Own(EquipmentSortField.NAME) to false
        val byRoom = TableColumn.OfUserfield("room") to false

        assertEquals(
            listOf("Saw", "Ladder", "Drill"),
            names(buildEquipmentList(equipment, listOf(room), down, ungrouped, "")),
        )
        assertEquals(
            listOf("Drill", "Saw", "Ladder"),
            names(buildEquipmentList(equipment, listOf(room), byRoom, ungrouped, "")),
        )
    }

    @Test
    fun groupsByAUserfieldWithEmptyOnesLast() {
        val groups = buildEquipmentList(
            equipment,
            listOf(room),
            byName,
            TableColumn.OfUserfield("room"),
            "",
        )

        assertEquals(listOf("Attic", "Garage", null), groups.map { it.title })
        assertTrue(groups.last().isUnassigned)
    }

    @Test
    fun searchesNamesAndUserfields() {
        assertEquals(
            listOf("Drill"),
            names(buildEquipmentList(equipment, listOf(room), byName, ungrouped, "Garage")),
        )
        assertTrue(buildEquipmentList(equipment, listOf(room), byName, ungrouped, "zzzz").isEmpty())
    }

    @Test
    fun aGoneColumnFallsBackToTheName() {
        val options = EquipmentOptions(
            ascending = false,
            sortUserfield = "gone",
            groupingUserfield = "gone",
        )

        assertEquals(byName, effectiveEquipmentSort(options, EquipmentField.entries, listOf(room)))
        assertEquals(
            ungrouped,
            effectiveEquipmentGrouping(options, availableEquipmentGroupings(listOf(room))),
        )
    }

    @Test
    fun hidingTheUserfieldsEndsSortingByThem() {
        val options = EquipmentOptions(sortUserfield = "room")

        assertEquals(byName, effectiveEquipmentSort(options, emptyList(), listOf(room)))
    }

    @Test
    fun theFormNeedsAName() {
        assertEquals(
            mapOf(ObjectField.NAME to FieldError.Required),
            EquipmentValues(name = " ").validate(),
        )
        assertTrue(EquipmentValues(name = "Drill").validate().isEmpty())
    }

    @Test
    fun unchangedNotesKeepTheirHtmlAndChangedOnesBecomeParagraphs() {
        val original = Equipment(1, "Drill", description = "<p>Keep <b>dry</b></p>")

        val unchanged = original.toValues().toEquipment(1, original)
        val changed = original.toValues().copy(notes = "New").toEquipment(1, original)

        assertEquals("<p>Keep <b>dry</b></p>", unchanged.description)
        assertEquals("<p>New</p>", changed.description)
    }

    @Test
    fun theManualStaysWithThePieceUntilItIsChanged() {
        val original = Equipment(1, "Drill", instructionManualFileName = "a.pdf")

        val saved = original.toValues().toEquipment(1, original)

        assertEquals("a.pdf", saved.instructionManualFileName)
        assertEquals(PictureChange.Keep, original.toValues().manual)
    }

    @Test
    fun theTabsAreGrocysAndOpenWithTheManualIfThereIsOne() {
        assertEquals(
            listOf(
                EquipmentTab.Manual,
                EquipmentTab.Notes,
                EquipmentTab.File("warranty", "Warranty"),
            ),
            equipmentTabsOf(listOf(room, warranty)),
        )
        assertEquals(
            EquipmentTab.Manual,
            firstTabOf(Equipment(1, "Drill", instructionManualFileName = "a.pdf")),
        )
        assertEquals(EquipmentTab.Notes, firstTabOf(Equipment(2, "Ladder")))
    }

    @Test
    fun pagesBesideTheListMarkTheirPiece() {
        assertEquals(3, selectedEquipmentOf(EquipmentDetailRoute(3)))
        assertEquals(3, selectedEquipmentOf(EquipmentFormRoute(3)))
        assertNull(selectedEquipmentOf(EquipmentFormRoute()))
        assertNull(selectedEquipmentOf(EquipmentRoute))
    }

    @Test
    fun aScreenOfItsOwnIsNoPartOfTheScene() {
        assertTrue(isInEquipmentScene(EquipmentRoute))
        assertTrue(isInEquipmentScene(EquipmentDetailRoute(1)))
        assertTrue(isInEquipmentScene(EquipmentFormRoute()))
        assertFalse(isInEquipmentScene(EquipmentDetailRoute(1, inList = false)))
        assertFalse(isInEquipmentScene(UserfieldsRoute("equipment")))
    }
}
