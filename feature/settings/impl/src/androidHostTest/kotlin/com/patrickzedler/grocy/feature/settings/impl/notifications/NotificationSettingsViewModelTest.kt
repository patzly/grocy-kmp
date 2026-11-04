package com.patrickzedler.grocy.feature.settings.impl.notifications

import com.patrickzedler.grocy.core.data.notifications.NotificationAlarm
import com.patrickzedler.grocy.core.data.notifications.NotificationDataSource
import com.patrickzedler.grocy.core.data.notifications.NotificationRepository
import com.patrickzedler.grocy.core.data.notifications.Notifier
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.notifications.NotificationKind
import com.patrickzedler.grocy.core.model.notifications.NotificationKindSettings
import com.patrickzedler.grocy.core.model.notifications.NotificationSummary
import com.patrickzedler.grocy.core.model.notifications.NotificationTopic
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.testing.FakeSettingsStore
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class NotificationSettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val store = FakeSettingsStore()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val notifier = object : Notifier {
        val cancelled = mutableListOf<NotificationKind>()
        override suspend fun show(summary: NotificationSummary, silently: Boolean) = Unit
        override suspend fun cancel(kind: NotificationKind) {
            cancelled += kind
        }
        override suspend fun isShowing(kind: NotificationKind) = false
    }

    private val alarm = object : NotificationAlarm {
        override fun set(localMillis: Long) = Unit
        override fun cancel() = Unit
    }

    private fun viewModel(scope: TestScope) = NotificationSettingsViewModel(
        repository = NotificationRepository(
            settingsStore = store,
            connection = MutableStateFlow<ServerConnection?>(null),
            serverConfig = MutableStateFlow(ServerConfig()),
            dataSource = object : NotificationDataSource {
                override suspend fun refresh(kinds: Set<NotificationKind>) = kinds
                override suspend fun serverConfig() = ServerConfig()
                override suspend fun summary(
                    kind: NotificationKind,
                    settings: NotificationKindSettings,
                    now: Long,
                ) = NotificationSummary(kind, emptyList(), showNames = true)
            },
            notifier = notifier,
            alarm = alarm,
            scope = scope.backgroundScope + UnconfinedTestDispatcher(scope.testScheduler),
            now = { 20_000 * 86_400_000L },
        ),
        serverConfig = MutableStateFlow(ServerConfig()),
    )

    @Test
    fun settingsAreThereInTheFirstFrame() = runTest(dispatcher) {
        val viewModel = viewModel(this)

        assertFalse(viewModel.settings.value!![NotificationKind.STOCK].enabled)
    }

    @Test
    fun turningOnIsStored() = runTest(dispatcher) {
        val viewModel = viewModel(this)

        viewModel.setEnabled(NotificationKind.CHORES, true)
        advanceUntilIdle()

        assertTrue(store.notificationSettings.value[NotificationKind.CHORES].enabled)
    }

    @Test
    fun turningOffTakesTheNotificationAway() = runTest(dispatcher) {
        val viewModel = viewModel(this)
        viewModel.setEnabled(NotificationKind.CHORES, true)
        advanceUntilIdle()

        viewModel.setEnabled(NotificationKind.CHORES, false)
        advanceUntilIdle()

        assertEquals(listOf(NotificationKind.CHORES), notifier.cancelled)
    }

    @Test
    fun theTimeIsStoredAsMinuteOfTheDay() = runTest(dispatcher) {
        val viewModel = viewModel(this)

        viewModel.setTime(NotificationKind.TASKS, 7, 45)
        advanceUntilIdle()

        assertEquals(465, store.notificationSettings.value[NotificationKind.TASKS].minuteOfDay)
    }

    @Test
    fun topicsCanBeTurnedOffAndOn() = runTest(dispatcher) {
        val viewModel = viewModel(this)

        viewModel.setTopic(NotificationTopic.STOCK_EXPIRED, false)
        advanceUntilIdle()
        assertFalse(
            NotificationTopic.STOCK_EXPIRED in
                store.notificationSettings.value[NotificationKind.STOCK].topics,
        )

        viewModel.setTopic(NotificationTopic.STOCK_EXPIRED, true)
        advanceUntilIdle()
        assertTrue(
            NotificationTopic.STOCK_EXPIRED in
                store.notificationSettings.value[NotificationKind.STOCK].topics,
        )
    }

    @Test
    fun namesAndAssignmentsAreStored() = runTest(dispatcher) {
        val viewModel = viewModel(this)

        viewModel.setShowNames(NotificationKind.TASKS, false)
        viewModel.setOnlyMine(NotificationKind.TASKS, true)
        advanceUntilIdle()

        val tasks = store.notificationSettings.value[NotificationKind.TASKS]
        assertFalse(tasks.showNames)
        assertTrue(tasks.onlyMine)
    }

    @Test
    fun nothingToShowIsReported() = runTest(dispatcher) {
        val viewModel = viewModel(this)

        viewModel.showNow(NotificationKind.STOCK)

        assertEquals(Unit, viewModel.nothingToShow.first())
    }

    @Test
    fun onlyFeaturesOfTheServerAreShown() {
        val config = ServerConfig(chores = false, recipesMealPlan = false)

        assertEquals(
            listOf(NotificationKind.STOCK, NotificationKind.TASKS, NotificationKind.BATTERIES),
            shownNotificationKinds(config),
        )
    }

    @Test
    fun batteriesHideWithTheirFeatureAndAreAssignedToNobody() {
        val shown = shownNotificationKinds(ServerConfig(batteries = false))

        assertTrue(NotificationKind.BATTERIES !in shown)
        assertTrue(!canNotifyOnlyMine(NotificationKind.BATTERIES, ServerConfig()))
    }

    @Test
    fun topicsFollowTheServersFeatures() {
        val config = ServerConfig(stockBestBeforeDateTracking = false)

        // A single topic is no choice
        assertTrue(shownNotificationTopics(NotificationKind.STOCK, config).isEmpty())
        assertEquals(
            NotificationKind.STOCK.topics,
            shownNotificationTopics(NotificationKind.STOCK, ServerConfig()),
        )
        assertTrue(shownNotificationTopics(NotificationKind.MEAL_PLAN, ServerConfig()).isEmpty())
    }

    @Test
    fun onlyAssignedThingsCanBeLimitedToTheUser() {
        val unassigned = ServerConfig(choresAssignments = false)

        assertFalse(canNotifyOnlyMine(NotificationKind.CHORES, unassigned))
        assertTrue(canNotifyOnlyMine(NotificationKind.CHORES, ServerConfig()))
        assertTrue(canNotifyOnlyMine(NotificationKind.TASKS, unassigned))
        assertFalse(canNotifyOnlyMine(NotificationKind.STOCK, ServerConfig()))
    }
}
