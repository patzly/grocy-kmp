package com.patrickzedler.grocy.feature.batteries.impl

import com.patrickzedler.grocy.feature.batteries.api.BatteriesJournalRoute
import com.patrickzedler.grocy.feature.batteries.api.BatteriesOverviewRoute
import com.patrickzedler.grocy.feature.batteries.api.BatteryTrackingRoute
import com.patrickzedler.grocy.feature.masterdata.api.BatteryRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BatteriesEntriesTest {

    @Test
    fun pagesBesideTheListMarkTheirBattery() {
        assertEquals(3, selectedBatteryOf(BatteriesJournalRoute(3)))
        assertEquals(3, selectedBatteryOf(BatteryTrackingRoute(3)))
        assertNull(selectedBatteryOf(BatteriesOverviewRoute))
    }

    @Test
    fun aFormOrAScreenOfItsOwnIsNoPartOfTheScene() {
        assertTrue(isInBatteriesScene(BatteriesOverviewRoute))
        assertTrue(isInBatteriesScene(BatteriesJournalRoute()))
        assertTrue(isInBatteriesScene(BatteryTrackingRoute()))
        assertFalse(isInBatteriesScene(BatteryTrackingRoute(inList = false)))
        assertFalse(isInBatteriesScene(BatteryRoute(1)))
    }
}
