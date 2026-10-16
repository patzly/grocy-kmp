package com.patrickzedler.grocy.apps.shared

import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.feature.about.api.AboutRoute
import com.patrickzedler.grocy.feature.login.api.LoginChoiceRoute
import com.patrickzedler.grocy.feature.login.api.LoginManualRoute
import com.patrickzedler.grocy.feature.login.api.LoginScanRoute
import com.patrickzedler.grocy.feature.settings.api.GeneralSettingsRoute
import com.patrickzedler.grocy.feature.settings.api.LogRoute
import com.patrickzedler.grocy.feature.settings.api.SettingsRoute
import com.patrickzedler.grocy.feature.start.api.StartRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json

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
        LogRoute,
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
}
