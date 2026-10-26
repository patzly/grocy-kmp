package com.patrickzedler.grocy.feature.start.impl

import com.patrickzedler.grocy.feature.masterdata.api.MasterDataRoute
import com.patrickzedler.grocy.feature.masterdata.api.ProductsRoute
import com.patrickzedler.grocy.feature.start.api.StartRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StartSectionTest {

    @Test
    fun sectionsOpenTheirPage() {
        assertEquals(MasterDataRoute, StartSection.MASTER_DATA.route)
    }

    @Test
    fun shownPageMarksItsSection() {
        assertEquals(StartSection.MASTER_DATA, startSectionOf(MasterDataRoute))
    }

    @Test
    fun otherRoutesMarkNoSection() {
        // The start screen alone, or a list opened from a section's page
        assertNull(startSectionOf(StartRoute))
        assertNull(startSectionOf(ProductsRoute))
    }
}
