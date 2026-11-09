package com.patrickzedler.grocy.apps.shared

import com.patrickzedler.grocy.feature.settings.api.NotificationSettingsRoute
import com.patrickzedler.grocy.feature.settings.api.SettingsRoute
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SystemSettingsLinksTest {

    private val main = "android.intent.action.MAIN"
    private val notificationPreferences = "android.intent.category.NOTIFICATION_PREFERENCES"

    @Test
    fun theAppInfoItemOpensTheSettings() {
        assertEquals(
            SettingsRoute,
            systemSettingsRouteOf("android.intent.action.APPLICATION_PREFERENCES", null),
        )
    }

    @Test
    fun theItemOfTheNotificationPageOpensTheNotificationSettings() {
        assertEquals(
            NotificationSettingsRoute,
            systemSettingsRouteOf(main, setOf(notificationPreferences)),
        )
    }

    @Test
    fun aNormalStartOpensNothing() {
        assertNull(systemSettingsRouteOf(main, setOf("android.intent.category.LAUNCHER")))
        assertNull(systemSettingsRouteOf(null, null))
    }

    @Test
    fun manifestOffersBothToAndroid() {
        val manifest = File("src/androidMain/AndroidManifest.xml").readText()

        assertTrue("android.intent.action.APPLICATION_PREFERENCES" in manifest)
        assertTrue(notificationPreferences in manifest)
    }
}
