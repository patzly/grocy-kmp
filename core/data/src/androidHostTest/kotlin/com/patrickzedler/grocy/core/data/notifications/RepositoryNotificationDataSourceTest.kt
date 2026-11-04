package com.patrickzedler.grocy.core.data.notifications

import com.patrickzedler.grocy.core.model.notifications.NotificationKind
import com.patrickzedler.grocy.core.model.notifications.NotificationSection
import com.patrickzedler.grocy.core.model.notifications.NotificationSettings
import com.patrickzedler.grocy.core.model.notifications.NotificationTopic
import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.core.testing.RepositoryTestEnvironment
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest

class RepositoryNotificationDataSourceTest {

    private val dispatcher = StandardTestDispatcher()
    private val environment = RepositoryTestEnvironment(dispatcher)
    private val source = RepositoryNotificationDataSource(
        masterData = environment.repository,
        stock = environment.stockRepository,
        chores = environment.choresRepository,
        tasks = environment.tasksRepository,
        recipes = environment.recipesRepository,
        mealPlan = environment.mealPlanRepository,
        timeout = 20.seconds,
    )

    // 2026-11-04 09:00, after the fake server's tasks and chores were due
    private val now = epochDayOf("2026-11-04")!! * 86_400_000L + 9 * 3_600_000L

    @AfterTest
    fun tearDown() = environment.close()

    @Test
    fun refreshTellsTheKindsThatGotTheirData() = runTest(dispatcher) {
        val kinds = NotificationKind.entries.toSet()

        assertEquals(kinds, source.refresh(kinds))
    }

    @Test
    fun offlineNoKindGetsItsData() = runTest(dispatcher) {
        environment.server.unreachable = true

        assertTrue(source.refresh(setOf(NotificationKind.TASKS)).isEmpty())
    }

    @Test
    fun aFailureOfOneKindLeavesTheOthers() = runTest(dispatcher) {
        environment.server.respond("/api/objects/tasks", HttpStatusCode.InternalServerError, "{}")

        val fresh = source.refresh(setOf(NotificationKind.TASKS, NotificationKind.CHORES))

        assertEquals(setOf(NotificationKind.CHORES), fresh)
    }

    @Test
    fun summariesComeFromWhatWasDownloaded() = runTest(dispatcher) {
        source.refresh(setOf(NotificationKind.TASKS, NotificationKind.CHORES))
        val settings = NotificationSettings()

        val tasks = source.summary(NotificationKind.TASKS, settings[NotificationKind.TASKS], now)
        val chores = source.summary(NotificationKind.CHORES, settings[NotificationKind.CHORES], now)

        assertEquals(
            listOf(NotificationSection(NotificationTopic.TASKS_OVERDUE, listOf("Mow the lawn"))),
            tasks.sections,
        )
        assertEquals(
            listOf(NotificationSection(NotificationTopic.CHORES_OVERDUE, listOf("Vacuum"))),
            chores.sections,
        )
    }

    @Test
    fun theStoredStateRemainsOfflineAfterADownload() = runTest(dispatcher) {
        source.refresh(setOf(NotificationKind.TASKS))
        environment.server.unreachable = true
        source.refresh(setOf(NotificationKind.TASKS))

        val summary = source.summary(
            NotificationKind.TASKS,
            NotificationSettings()[NotificationKind.TASKS],
            now,
        )

        assertEquals(listOf("Mow the lawn"), summary.sections.single().names)
    }
}
