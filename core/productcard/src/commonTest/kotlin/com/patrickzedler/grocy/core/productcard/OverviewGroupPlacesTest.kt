package com.patrickzedler.grocy.core.productcard

import kotlin.test.Test
import kotlin.test.assertEquals

class OverviewGroupPlacesTest {

    @Test
    fun theDescriptionStandsRightBelowTheName() {
        val places = firstGroupPlaces(rows = 3, hasDescription = true)

        assertEquals(listOf(0, 2, 3), places.rows)
        assertEquals(1, DESCRIPTION_PLACE)
        assertEquals(4, places.count)
    }

    @Test
    fun withoutDescriptionTheRowsFollowEachOther() {
        val places = firstGroupPlaces(rows = 3, hasDescription = false)

        assertEquals(listOf(0, 1, 2), places.rows)
        assertEquals(3, places.count)
    }

    @Test
    fun theNameAloneAndTheDescriptionMakeTwoItems() {
        val places = firstGroupPlaces(rows = 1, hasDescription = true)

        assertEquals(listOf(0), places.rows)
        assertEquals(2, places.count)
    }
}
