package com.patrickzedler.grocy.core.model.notifications

import com.patrickzedler.grocy.core.model.chores.ChoreDueStatus
import com.patrickzedler.grocy.core.model.chores.CurrentChore
import com.patrickzedler.grocy.core.model.chores.choreDueStatusOf
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.StockAmount
import com.patrickzedler.grocy.core.model.recipes.MealPlanEntry
import com.patrickzedler.grocy.core.model.recipes.MealPlanSection
import com.patrickzedler.grocy.core.model.recipes.Recipe
import com.patrickzedler.grocy.core.model.recipes.mealPlanNamesOf
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.stock.DueStatus
import com.patrickzedler.grocy.core.model.stock.ProductStockInfo
import com.patrickzedler.grocy.core.model.stock.dueProductsOf
import com.patrickzedler.grocy.core.model.tasks.Task
import com.patrickzedler.grocy.core.model.tasks.TaskDueStatus
import com.patrickzedler.grocy.core.model.tasks.taskDueStatusOf

/** What a notification tells about one [topic]; the count is the number of [names]. */
data class NotificationSection(
    val topic: NotificationTopic,
    val names: List<String>,
)

/**
 * The content of a notification. [isStale] when the server could not be reached and it shows the
 * last known state; [dueSoonDays] are the user's days of "due soon" for this kind.
 */
data class NotificationSummary(
    val kind: NotificationKind,
    val sections: List<NotificationSection>,
    val showNames: Boolean,
    val dueSoonDays: Int = 0,
    val isStale: Boolean = false,
)

private fun sectionsOf(
    settings: NotificationKindSettings,
    names: (NotificationTopic) -> List<String>,
    kind: NotificationKind,
): List<NotificationSection> = kind.topics
    .filter { it in settings.topics }
    .map { NotificationSection(it, names(it)) }
    .filter { it.names.isNotEmpty() }

/** Stock counted like Grocy's messages and the start screen (`stockSummaryOf`). */
fun stockNotificationSections(
    products: List<Product>,
    stock: List<StockAmount>,
    info: List<ProductStockInfo>,
    config: ServerConfig,
    settings: NotificationKindSettings,
    dueSoonDays: Int,
    today: Long,
): List<NotificationSection> {
    val shown = products.filter { !it.hideOnStockOverview }.associateBy { it.id }
    val due = dueProductsOf(shown, stock, config, dueSoonDays, today)
    fun dueNames(status: DueStatus) =
        due.filter { it.second == status }.map { it.first.name }.sortedBy { it.lowercase() }
    return sectionsOf(
        settings = settings,
        kind = NotificationKind.STOCK,
        names = { topic ->
            when (topic) {
                NotificationTopic.STOCK_DUE_SOON -> dueNames(DueStatus.DUE_SOON)
                NotificationTopic.STOCK_OVERDUE -> dueNames(DueStatus.OVERDUE)
                NotificationTopic.STOCK_EXPIRED -> dueNames(DueStatus.EXPIRED)
                else -> info.filter { it.amountMissing != null }
                    .mapNotNull { shown[it.productId]?.name }
                    .sortedBy { it.lowercase() }
            }
        },
    ).filter { it.topic.isAvailable(config) }
}

/** The chores like Grocy's status filter at [now] ("YYYY-MM-DD HH:MM:SS"). */
fun choresNotificationSections(
    chores: List<CurrentChore>,
    settings: NotificationKindSettings,
    dueSoonDays: Int,
    now: String,
    currentUserId: Int?,
): List<NotificationSection> {
    val mine = chores.filter { !settings.onlyMine || it.assignedUserId == currentUserId }
    fun names(vararg statuses: ChoreDueStatus) = mine
        .filter { choreDueStatusOf(it.nextEstimatedExecutionTime, now, dueSoonDays) in statuses }
        .map { it.name }
    return sectionsOf(settings, kind = NotificationKind.CHORES, names = { topic ->
        when (topic) {
            NotificationTopic.CHORES_OVERDUE -> names(ChoreDueStatus.OVERDUE)
            NotificationTopic.CHORES_DUE_TODAY -> names(ChoreDueStatus.DUE_TODAY)
            // Like Grocy's message and filter, what is due today is due soon as well
            else -> names(ChoreDueStatus.DUE_TODAY, ChoreDueStatus.DUE_SOON)
        }
    })
}

/** The tasks still to do like Grocy's status filter on the day [today]. */
fun tasksNotificationSections(
    tasks: List<Task>,
    settings: NotificationKindSettings,
    dueSoonDays: Int,
    today: Long,
    currentUserId: Int?,
): List<NotificationSection> {
    val mine = tasks.filter {
        !it.done && (!settings.onlyMine || it.assignedToUserId == currentUserId)
    }
    fun names(vararg statuses: TaskDueStatus) = mine
        .filter { taskDueStatusOf(it.dueDate, today, dueSoonDays) in statuses }
        .map { it.name }
    return sectionsOf(settings, kind = NotificationKind.TASKS, names = { topic ->
        when (topic) {
            NotificationTopic.TASKS_OVERDUE -> names(TaskDueStatus.OVERDUE)
            NotificationTopic.TASKS_DUE_TODAY -> names(TaskDueStatus.DUE_TODAY)
            // Like Grocy's message and filter, what is due today is due soon as well
            else -> names(TaskDueStatus.DUE_TODAY, TaskDueStatus.DUE_SOON)
        }
    })
}

/** Today's meal plan not crossed off yet, like the start screen and Grocy's day show it. */
fun mealPlanNotificationSections(
    entries: List<MealPlanEntry>,
    sections: List<MealPlanSection>,
    recipes: List<Recipe>,
    products: List<Product>,
    settings: NotificationKindSettings,
    today: Long,
): List<NotificationSection> {
    val names = mealPlanNamesOf(entries.filter { !it.done }, sections, recipes, products, today)
    return sectionsOf(settings, kind = NotificationKind.MEAL_PLAN, names = { names })
}
