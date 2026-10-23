package com.patrickzedler.grocy.core.design.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DropdownTextMatchTest {

    private val options = listOf(DropdownOption(1, "Milk"), DropdownOption(2, "Bread "))

    @Test
    fun textNamingAnOptionChoosesIt() {
        assertEquals(1, dropdownTextMatch(options, "Milk"))
    }

    @Test
    fun caseAndSurroundingSpacesAreIgnored() {
        assertEquals(2, dropdownTextMatch(options, "  bread"))
    }

    @Test
    fun emptyTextChoosesNothing() {
        assertNull(dropdownTextMatch(options, "   "))
    }

    @Test
    fun partOfALabelIsNoMatch() {
        assertEquals(NoMatch, dropdownTextMatch(options, "Mil"))
    }
}
