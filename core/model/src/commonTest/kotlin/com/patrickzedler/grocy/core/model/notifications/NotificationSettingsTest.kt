package com.patrickzedler.grocy.core.model.notifications

import com.patrickzedler.grocy.core.model.server.ServerConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationSettingsTest {

    @Test
    fun everythingIsOffUntilTurnedOn() {
        val settings = NotificationSettings.of(null)

        assertTrue(settings.enabledKinds.isEmpty())
        assertEquals(9 * 60, settings[NotificationKind.STOCK].minuteOfDay)
        assertEquals(16 * 60, settings[NotificationKind.MEAL_PLAN].minuteOfDay)
    }

    @Test
    fun defaultTopicsLeaveOutWhatIsNotDueYet() {
        val settings = NotificationSettings()

        assertEquals(
            NotificationKind.STOCK.topics.toSet(),
            settings[NotificationKind.STOCK].topics,
        )
        assertEquals(
            setOf(NotificationTopic.CHORES_OVERDUE, NotificationTopic.CHORES_DUE_TODAY),
            settings[NotificationKind.CHORES].topics,
        )
    }

    @Test
    fun storedFormStaysReadable() {
        val stored = "STOCK=1;450;STOCK_OVERDUE,STOCK_EXPIRED;0;0|" +
            "CHORES=0;540;CHORES_OVERDUE;1;1"

        val settings = NotificationSettings.of(stored)

        assertEquals(
            NotificationKindSettings(
                enabled = true,
                minuteOfDay = 450,
                topics = setOf(NotificationTopic.STOCK_OVERDUE, NotificationTopic.STOCK_EXPIRED),
                showNames = false,
                onlyMine = false,
            ),
            settings[NotificationKind.STOCK],
        )
        assertEquals(
            NotificationKindSettings(
                enabled = false,
                minuteOfDay = 540,
                topics = setOf(NotificationTopic.CHORES_OVERDUE),
                showNames = true,
                onlyMine = true,
            ),
            settings[NotificationKind.CHORES],
        )
    }

    @Test
    fun storingAndReadingGivesTheSame() {
        val kind = NotificationSettings()[NotificationKind.TASKS].copy(
            enabled = true,
            minuteOfDay = 1,
            topics = setOf(NotificationTopic.TASKS_DUE_SOON),
            onlyMine = true,
        )
        val settings = NotificationSettings().with(NotificationKind.TASKS, kind)

        val read = NotificationSettings.of(settings.stored())

        assertEquals(kind, read[NotificationKind.TASKS])
        assertEquals(setOf(NotificationKind.TASKS), read.enabledKinds)
    }

    @Test
    fun whatANewerVersionWroteFallsBack() {
        val settings = NotificationSettings.of("STOCK=1;99999;NEW_TOPIC|NEW_KIND=1;5;A;1;1")

        val stock = settings[NotificationKind.STOCK]
        assertTrue(stock.enabled)
        assertEquals(9 * 60, stock.minuteOfDay)
        assertTrue(stock.topics.isEmpty())
        assertTrue(stock.showNames)
    }

    @Test
    fun kindsFollowTheServersFeatures() {
        assertFalse(NotificationKind.STOCK.isAvailable(ServerConfig(stock = false)))
        assertFalse(NotificationKind.CHORES.isAvailable(ServerConfig(chores = false)))
        assertFalse(NotificationKind.TASKS.isAvailable(ServerConfig(tasks = false)))
        assertFalse(NotificationKind.BATTERIES.isAvailable(ServerConfig(batteries = false)))
        assertFalse(NotificationKind.MEAL_PLAN.isAvailable(ServerConfig(recipes = false)))
        assertFalse(NotificationKind.MEAL_PLAN.isAvailable(ServerConfig(recipesMealPlan = false)))
        NotificationKind.entries.forEach { assertTrue(it.isAvailable(ServerConfig())) }
    }

    @Test
    fun dueDatesNeedTheirFeature() {
        val config = ServerConfig(stockBestBeforeDateTracking = false)

        assertFalse(NotificationTopic.STOCK_DUE_SOON.isAvailable(config))
        assertFalse(NotificationTopic.STOCK_OVERDUE.isAvailable(config))
        assertFalse(NotificationTopic.STOCK_EXPIRED.isAvailable(config))
        assertTrue(NotificationTopic.STOCK_BELOW_MIN.isAvailable(config))
    }
}
