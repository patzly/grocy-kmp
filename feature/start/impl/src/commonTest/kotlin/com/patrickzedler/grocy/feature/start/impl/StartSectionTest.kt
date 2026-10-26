package com.patrickzedler.grocy.feature.start.impl

import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.feature.masterdata.api.MasterDataRoute
import com.patrickzedler.grocy.feature.masterdata.api.ProductsRoute
import com.patrickzedler.grocy.feature.masterdata.api.StockOverviewRoute
import com.patrickzedler.grocy.feature.start.api.StartRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StartSectionTest {

    @Test
    fun sectionsOpenTheirPage() {
        assertEquals(StockOverviewRoute, StartSection.STOCK_OVERVIEW.route)
        assertEquals(MasterDataRoute, StartSection.MASTER_DATA.route)
    }

    @Test
    fun shownPageMarksItsSection() {
        assertEquals(StartSection.STOCK_OVERVIEW, startSectionOf(StockOverviewRoute))
        assertEquals(StartSection.MASTER_DATA, startSectionOf(MasterDataRoute))
    }

    @Test
    fun stockOverviewComesFirstAndOnlyWithTheStockFeature() {
        assertEquals(
            listOf(StartSection.STOCK_OVERVIEW, StartSection.MASTER_DATA),
            availableSections(ServerConfig()),
        )
        assertEquals(
            listOf(StartSection.MASTER_DATA),
            availableSections(ServerConfig(stock = false)),
        )
    }

    @Test
    fun everySectionShowsUntilTheConfigurationIsRead() {
        assertEquals(StartSection.entries, availableSections(null))
    }

    @Test
    fun otherRoutesMarkNoSection() {
        // The start screen alone, or a list opened from a section's page
        assertNull(startSectionOf(StartRoute))
        assertNull(startSectionOf(ProductsRoute))
    }
}
