package com.patrickzedler.grocy.core.data.notifications

import com.patrickzedler.grocy.core.data.chores.ChoresRepository
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.recipes.MealPlanRepository
import com.patrickzedler.grocy.core.data.recipes.RecipesRepository
import com.patrickzedler.grocy.core.data.stock.StockRepository
import com.patrickzedler.grocy.core.data.tasks.TasksRepository
import com.patrickzedler.grocy.core.model.notifications.NotificationKind
import com.patrickzedler.grocy.core.model.notifications.NotificationKindSettings
import com.patrickzedler.grocy.core.model.notifications.NotificationSummary
import com.patrickzedler.grocy.core.model.notifications.choresNotificationSections
import com.patrickzedler.grocy.core.model.notifications.mealPlanNotificationSections
import com.patrickzedler.grocy.core.model.notifications.notificationDayOf
import com.patrickzedler.grocy.core.model.notifications.stockNotificationSections
import com.patrickzedler.grocy.core.model.notifications.tasksNotificationSections
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.text.grocyDateTimeOf
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Where notifications get their content from. */
interface NotificationDataSource {
    /** Downloads what [kinds] show within the time allowed; returns the kinds that got it. */
    suspend fun refresh(kinds: Set<NotificationKind>): Set<NotificationKind>

    suspend fun serverConfig(): ServerConfig

    /** What a notification of [kind] says at [now] (local time) from the stored data. */
    suspend fun summary(
        kind: NotificationKind,
        settings: NotificationKindSettings,
        now: Long,
    ): NotificationSummary
}

/**
 * The repositories of the screens, so notifications say what the screens would.
 *
 * @param timeout how long downloads may take at most: Android only keeps the app awake and
 * connected for a short time when an alarm fires, a notification must not wait for a slow server.
 */
class RepositoryNotificationDataSource(
    private val masterData: MasterDataRepository,
    private val stock: StockRepository,
    private val chores: ChoresRepository,
    private val tasks: TasksRepository,
    private val recipes: RecipesRepository,
    private val mealPlan: MealPlanRepository,
    private val timeout: Duration,
) : NotificationDataSource {

    override suspend fun serverConfig(): ServerConfig = masterData.serverConfig.first()

    override suspend fun refresh(kinds: Set<NotificationKind>): Set<NotificationKind> =
        coroutineScope {
            val config = serverConfig()
            // New products and users, the due soon days and whether the features are still on
            launch { tryWithinTimeout { masterData.refresh() } }
            kinds.map { kind ->
                async { kind.takeIf { tryWithinTimeout { download(kind, config) } } }
            }.awaitAll().filterNotNull().toSet()
        }

    private suspend fun tryWithinTimeout(block: suspend () -> Unit): Boolean = try {
        withTimeoutOrNull(timeout) { block() } != null
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        // Offline or the server failed: the notification tells the last known state
        false
    }

    private suspend fun download(kind: NotificationKind, config: ServerConfig) {
        when (kind) {
            NotificationKind.STOCK -> stock.refreshOverview(config.shoppingList)
            NotificationKind.CHORES -> chores.refresh()
            NotificationKind.TASKS -> tasks.refresh()
            NotificationKind.MEAL_PLAN -> coroutineScope {
                launch { mealPlan.refresh() }
                launch { recipes.refresh() }
            }
        }
    }

    override suspend fun summary(
        kind: NotificationKind,
        settings: NotificationKindSettings,
        now: Long,
    ): NotificationSummary {
        val config = serverConfig()
        val userSettings = masterData.userSettings.first()
        val today = notificationDayOf(now)
        return when (kind) {
            NotificationKind.STOCK -> NotificationSummary(
                kind = kind,
                sections = stockNotificationSections(
                    products = masterData.products.first(),
                    stock = masterData.stock.first(),
                    info = stock.productStockInfo.first(),
                    config = config,
                    settings = settings,
                    dueSoonDays = userSettings.stockDueSoonDays,
                    today = today,
                ),
                showNames = settings.showNames,
                dueSoonDays = userSettings.stockDueSoonDays,
            )
            NotificationKind.CHORES -> NotificationSummary(
                kind = kind,
                sections = choresNotificationSections(
                    chores = chores.current.first(),
                    settings = settings,
                    dueSoonDays = userSettings.choresDueSoonDays,
                    now = grocyDateTimeOf(now),
                    currentUserId = chores.currentUserId.first(),
                ),
                showNames = settings.showNames,
                dueSoonDays = userSettings.choresDueSoonDays,
            )
            NotificationKind.TASKS -> NotificationSummary(
                kind = kind,
                sections = tasksNotificationSections(
                    tasks = tasks.tasks.first(),
                    settings = settings,
                    dueSoonDays = userSettings.tasksDueSoonDays,
                    today = today,
                    currentUserId = tasks.currentUserId.first(),
                ),
                showNames = settings.showNames,
                dueSoonDays = userSettings.tasksDueSoonDays,
            )
            NotificationKind.MEAL_PLAN -> NotificationSummary(
                kind = kind,
                sections = mealPlanNotificationSections(
                    entries = mealPlan.entries.first(),
                    sections = mealPlan.sections.first(),
                    recipes = recipes.recipes.first(),
                    products = masterData.products.first(),
                    settings = settings,
                    today = today,
                ),
                showNames = settings.showNames,
            )
        }
    }
}
