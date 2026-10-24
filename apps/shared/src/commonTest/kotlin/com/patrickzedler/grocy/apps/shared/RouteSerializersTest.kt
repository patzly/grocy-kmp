package com.patrickzedler.grocy.apps.shared

import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.feature.about.api.AboutRoute
import com.patrickzedler.grocy.feature.login.api.LoginChoiceRoute
import com.patrickzedler.grocy.feature.login.api.LoginManualRoute
import com.patrickzedler.grocy.feature.login.api.LoginScanRoute
import com.patrickzedler.grocy.feature.masterdata.api.BatteriesRoute
import com.patrickzedler.grocy.feature.masterdata.api.BatteryRoute
import com.patrickzedler.grocy.feature.masterdata.api.ChoreRoute
import com.patrickzedler.grocy.feature.masterdata.api.ChoresRoute
import com.patrickzedler.grocy.feature.masterdata.api.LocationRoute
import com.patrickzedler.grocy.feature.masterdata.api.LocationsRoute
import com.patrickzedler.grocy.feature.masterdata.api.MasterDataRoute
import com.patrickzedler.grocy.feature.masterdata.api.ProductGroupRoute
import com.patrickzedler.grocy.feature.masterdata.api.ProductGroupsRoute
import com.patrickzedler.grocy.feature.masterdata.api.ProductRoute
import com.patrickzedler.grocy.feature.masterdata.api.ProductsRoute
import com.patrickzedler.grocy.feature.masterdata.api.QuantityUnitRoute
import com.patrickzedler.grocy.feature.masterdata.api.QuantityUnitsRoute
import com.patrickzedler.grocy.feature.masterdata.api.StoreRoute
import com.patrickzedler.grocy.feature.masterdata.api.StoresRoute
import com.patrickzedler.grocy.feature.masterdata.api.TaskCategoriesRoute
import com.patrickzedler.grocy.feature.masterdata.api.TaskCategoryRoute
import com.patrickzedler.grocy.feature.masterdata.api.UserentitiesRoute
import com.patrickzedler.grocy.feature.masterdata.api.UserentityRoute
import com.patrickzedler.grocy.feature.masterdata.api.UserfieldRoute
import com.patrickzedler.grocy.feature.masterdata.api.UserfieldsRoute
import com.patrickzedler.grocy.feature.settings.api.GeneralSettingsRoute
import com.patrickzedler.grocy.feature.settings.api.LogRoute
import com.patrickzedler.grocy.feature.settings.api.ServerSettingsRoute
import com.patrickzedler.grocy.feature.settings.api.SettingsRoute
import com.patrickzedler.grocy.feature.start.api.StartRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The back stack is saved through [routeSerializers]. Wasm has no reflection, so a route missing
 * there only fails at runtime; this test catches it. Add every new route to [allRoutes].
 */
class RouteSerializersTest {

    private val allRoutes: List<NavKey> = listOf(
        StartRoute,
        LoginChoiceRoute,
        LoginScanRoute,
        LoginManualRoute,
        AboutRoute,
        SettingsRoute,
        GeneralSettingsRoute,
        ServerSettingsRoute,
        LogRoute,
        MasterDataRoute,
        ProductsRoute,
        ProductRoute(),
        ProductRoute(productId = 7),
        LocationsRoute,
        LocationRoute(),
        LocationRoute(id = 3),
        StoresRoute,
        StoreRoute(id = 3),
        QuantityUnitsRoute,
        QuantityUnitRoute(id = 3),
        ProductGroupsRoute,
        ProductGroupRoute(id = 3),
        ChoresRoute,
        ChoreRoute(id = 3),
        BatteriesRoute,
        BatteryRoute(id = 3),
        TaskCategoriesRoute,
        TaskCategoryRoute(id = 3),
        UserfieldsRoute(),
        UserfieldsRoute(entity = "products"),
        UserfieldRoute(entity = "chores"),
        UserfieldRoute(id = 3),
        UserentitiesRoute,
        UserentityRoute(id = 3),
    )

    private val json = Json { serializersModule = routeSerializers }
    private val serializer = PolymorphicSerializer(NavKey::class)

    @Test
    fun everyRouteSurvivesStateSaving() {
        allRoutes.forEach { route ->
            val encoded = json.encodeToString(serializer, route)

            assertEquals(route, json.decodeFromString(serializer, encoded))
        }
    }

    @Test
    fun everyRouteHasItsOwnUrlPath() {
        // The @SerialName is the path in the web app's URL ("#/settings/general")
        val paths = allRoutes.map { route ->
            json.encodeToJsonElement(serializer, route).jsonObject.getValue("type").jsonPrimitive
                .content
        }

        paths.forEach { path ->
            assertTrue(urlPath.matches(path), "$path is no URL path, add a @SerialName")
        }
        // A route with arguments appears once per value, its path once
        assertEquals(
            paths.toSet().size,
            allRoutes.map { it::class }.toSet().size,
            "URL paths must be unique: $paths",
        )
    }

    private companion object {
        val urlPath = Regex("[a-z0-9-]+(/[a-z0-9-]+)*")
    }
}
