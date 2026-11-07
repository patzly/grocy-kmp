package com.patrickzedler.grocy.apps.shared

import com.patrickzedler.grocy.feature.chores.api.ChoreTrackingRoute
import com.patrickzedler.grocy.feature.equipment.api.EquipmentDetailRoute
import com.patrickzedler.grocy.feature.equipment.api.EquipmentRoute
import com.patrickzedler.grocy.feature.recipes.api.RecipeRoute
import com.patrickzedler.grocy.feature.shoppinglist.api.ShoppingListRoute
import com.patrickzedler.grocy.feature.stock.api.ConsumeRoute
import com.patrickzedler.grocy.feature.stock.api.PurchaseRoute
import com.patrickzedler.grocy.feature.stock.api.StockOverviewRoute
import com.patrickzedler.grocy.feature.stock.api.StockScanRoute
import com.patrickzedler.grocy.feature.tasks.api.TaskRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DeepLinksTest {

    @Test
    fun pageLinksOpenTheirPageWhateverTheCase() {
        assertEquals(StockOverviewRoute, DeepLinks.routeOf("grocy://stockOverview"))
        assertEquals(StockOverviewRoute, DeepLinks.routeOf("GROCY://STOCKOVERVIEW/"))
        assertEquals(StockScanRoute, DeepLinks.routeOf("grocy:scan"))
    }

    @Test
    fun everyDocumentedPageOpensSomething() {
        DeepLinks.pages.forEach { page ->
            val link = if (page == "recipe") "grocy://$page?id=1" else "grocy://$page"
            assertNotNull(DeepLinks.routeOf(link), page)
        }
    }

    @Test
    fun parametersChooseTheObject() {
        assertEquals(ConsumeRoute(productId = 12), DeepLinks.routeOf("grocy://consume?product=12"))
        assertEquals(
            PurchaseRoute(productId = 4),
            DeepLinks.routeOf("grocy://purchase?Product=4&other=x"),
        )
        assertEquals(
            ChoreTrackingRoute(3, inList = false),
            DeepLinks.routeOf("grocy://choreTracking?chore=3"),
        )
        assertEquals(TaskRoute(inList = false), DeepLinks.routeOf("grocy://task"))
        assertEquals(RecipeRoute(5), DeepLinks.routeOf("grocy://recipe?id=5"))
        assertEquals(ShoppingListRoute(2), DeepLinks.routeOf("grocy://shoppingList?list=2"))
        assertEquals(
            EquipmentDetailRoute(8, inList = false),
            DeepLinks.routeOf("grocy://equipment?id=8"),
        )
        assertEquals(EquipmentRoute, DeepLinks.routeOf("grocy://equipment"))
    }

    @Test
    fun anythingElseOpensNothing() {
        listOf(
            "",
            "grocy://",
            "grocy://unknown",
            "https://stockOverview",
            "grocyx://stockOverview",
            "grocy://recipe",
            "grocy://consume?product=abc",
            "grocy://consume?product=-1",
            "grocy://consume?product=",
            "grocy://login",
        ).forEach { assertNull(DeepLinks.routeOf(it), it) }
    }
}
