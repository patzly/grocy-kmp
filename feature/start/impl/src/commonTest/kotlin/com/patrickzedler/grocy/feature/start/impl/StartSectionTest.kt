package com.patrickzedler.grocy.feature.start.impl

import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.navigation.DetailRoute
import com.patrickzedler.grocy.feature.masterdata.api.MasterDataRoute
import com.patrickzedler.grocy.feature.masterdata.api.ProductsRoute
import com.patrickzedler.grocy.feature.shoppinglist.api.ShoppingListRoute
import com.patrickzedler.grocy.feature.start.api.StartRoute
import com.patrickzedler.grocy.feature.stock.api.StockOverviewRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StartSectionTest {

    @Test
    fun sectionsOpenTheirPage() {
        assertEquals(StockOverviewRoute, StartSection.STOCK_OVERVIEW.route)
        assertEquals(ShoppingListRoute(), StartSection.SHOPPING_LIST.route)
        assertEquals(MasterDataRoute, StartSection.MASTER_DATA.route)
        // A list-detail scene of its own, never beside the start screen
        assertFalse(StartSection.SHOPPING_LIST.route is DetailRoute)
    }

    @Test
    fun shownPageMarksItsSection() {
        assertEquals(StartSection.STOCK_OVERVIEW, startSectionOf(StockOverviewRoute))
        assertEquals(StartSection.MASTER_DATA, startSectionOf(MasterDataRoute))
    }

    @Test
    fun stockSectionsComeFirstAndOnlyWithTheirFeatures() {
        assertEquals(
            listOf(
                StartSection.STOCK_OVERVIEW,
                StartSection.SHOPPING_LIST,
                StartSection.MASTER_DATA,
            ),
            availableSections(ServerConfig()),
        )
        assertEquals(
            listOf(StartSection.STOCK_OVERVIEW, StartSection.MASTER_DATA),
            availableSections(ServerConfig(shoppingList = false)),
        )
        assertEquals(
            listOf(StartSection.MASTER_DATA),
            availableSections(ServerConfig(stock = false)),
        )
    }

    @Test
    fun shoppingListSharesTheStockOverviewsGroup() {
        assertEquals(StartSection.STOCK_OVERVIEW.group, StartSection.SHOPPING_LIST.group)
        assertTrue(StartSection.MASTER_DATA.group != StartSection.STOCK_OVERVIEW.group)
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
