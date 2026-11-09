package com.patrickzedler.grocy.feature.settings.impl

import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.equipment_title
import com.patrickzedler.grocy.core.resources.stock_overview_title
import com.patrickzedler.grocy.feature.settings.impl.server.grocycodeTypeOf
import com.patrickzedler.grocy.feature.settings.impl.server.serverFeaturesOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServerFeaturesTest {

    @Test
    fun aFeatureTurnedOffIsListedAsOff() {
        val features = serverFeaturesOf(ServerConfig(equipment = false))

        assertFalse(features.single { it.name == Res.string.equipment_title }.isEnabled)
        assertTrue(features.single { it.name == Res.string.stock_overview_title }.isEnabled)
    }

    @Test
    fun theOrderIsGrocys() {
        assertEquals(Res.string.stock_overview_title, serverFeaturesOf(ServerConfig()).first().name)
    }

    @Test
    fun grocycodesAreNamedByTheirType() {
        assertEquals("DataMatrix (2D)", grocycodeTypeOf(ServerConfig(grocycode2D = true)))
        assertEquals("Code 128 (1D)", grocycodeTypeOf(ServerConfig(grocycode2D = false)))
    }
}
