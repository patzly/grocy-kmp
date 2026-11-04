package com.patrickzedler.grocy.core.model.equipment

import com.patrickzedler.grocy.core.model.masterdata.TableColumn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EquipmentTest {
    @Test
    fun userfieldFileNameIsTheSecondPartOfGrocysValue() {
        // btoa("x1.pdf") + "_" + btoa("Warranty.pdf")
        assertEquals("Warranty.pdf", userfieldFileNameOf("eDEucGRm_V2FycmFudHkucGRm"))
    }

    @Test
    fun valuesWithoutAFileNameHaveNone() {
        assertNull(userfieldFileNameOf("eDEucGRm"))
        assertNull(userfieldFileNameOf("eDEucGRm_%%%"))
    }

    @Test
    fun onlyPdfsCountAsPdf() {
        assertTrue(isPdf("%PDF-1.7 ...".encodeToByteArray()))
        assertFalse(isPdf("<html><script>".encodeToByteArray()))
        assertFalse(isPdf(ByteArray(0)))
    }

    @Test
    fun optionsReadAFixedStoredForm() {
        val options = EquipmentOptions.of("USERFIELDS;false;room;kind")

        assertEquals(setOf(EquipmentField.USERFIELDS), options.fields)
        assertEquals(TableColumn.OfUserfield("room"), options.sortColumn)
        assertEquals(TableColumn.OfUserfield("kind"), options.groupingColumn)
        assertFalse(options.ascending)
    }

    @Test
    fun optionsFallBackToGrocysDefaults() {
        val options = EquipmentOptions.of(null)

        assertEquals(TableColumn.Own(EquipmentSortField.NAME), options.sortColumn)
        assertEquals(TableColumn.Own(EquipmentGrouping.NONE), options.groupingColumn)
        assertTrue(options.ascending)
    }

    @Test
    fun choosingTheNameEndsSortingByAUserfield() {
        val options = EquipmentOptions(sortUserfield = "room")
            .withSort(TableColumn.Own(EquipmentSortField.NAME), ascending = false)

        assertNull(options.sortUserfield)
        assertFalse(options.ascending)
    }
}
