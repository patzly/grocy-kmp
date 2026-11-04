package com.patrickzedler.grocy.apps.shared

import com.patrickzedler.grocy.core.model.notifications.NotificationKind
import com.patrickzedler.grocy.core.model.notifications.NotificationSection
import com.patrickzedler.grocy.core.model.notifications.NotificationSummary
import com.patrickzedler.grocy.core.model.notifications.NotificationTopic
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.masterdata_batteries
import com.patrickzedler.grocy.core.resources.masterdata_chores
import com.patrickzedler.grocy.core.resources.meal_plan_title
import com.patrickzedler.grocy.core.resources.notifications_meal_plan_today
import com.patrickzedler.grocy.core.resources.notifications_stale
import com.patrickzedler.grocy.core.resources.product_group_stock
import com.patrickzedler.grocy.core.resources.start_batteries_due_soon
import com.patrickzedler.grocy.core.resources.start_batteries_due_today
import com.patrickzedler.grocy.core.resources.start_batteries_overdue
import com.patrickzedler.grocy.core.resources.start_chores_due_soon
import com.patrickzedler.grocy.core.resources.start_chores_due_today
import com.patrickzedler.grocy.core.resources.start_chores_overdue
import com.patrickzedler.grocy.core.resources.start_stock_below_min
import com.patrickzedler.grocy.core.resources.start_stock_due_soon
import com.patrickzedler.grocy.core.resources.start_stock_expired
import com.patrickzedler.grocy.core.resources.start_stock_overdue
import com.patrickzedler.grocy.core.resources.start_stock_within_days
import com.patrickzedler.grocy.core.resources.start_tasks_due_soon
import com.patrickzedler.grocy.core.resources.start_tasks_due_today
import com.patrickzedler.grocy.core.resources.start_tasks_overdue
import com.patrickzedler.grocy.core.resources.tasks_title
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString

/**
 * A notification in words: [text] the short form, [details] with the names when expanded and
 * [note] that it shows the last known state.
 */
internal data class NotificationText(
    val title: String,
    val text: String,
    val details: String,
    val note: String?,
)

/** The name of the feature of [kind], also of its channel on Android. */
internal suspend fun notificationTitleOf(kind: NotificationKind): String = getString(
    when (kind) {
        NotificationKind.STOCK -> Res.string.product_group_stock
        NotificationKind.CHORES -> Res.string.masterdata_chores
        NotificationKind.TASKS -> Res.string.tasks_title
        NotificationKind.BATTERIES -> Res.string.masterdata_batteries
        NotificationKind.MEAL_PLAN -> Res.string.meal_plan_title
    },
)

/** Says [summary] with the sentences of the start screen, Grocy's messages above its tables. */
internal suspend fun notificationTextOf(summary: NotificationSummary): NotificationText {
    val sentences = summary.sections.map { sentenceOf(it, summary.dueSoonDays) }
    return NotificationText(
        title = notificationTitleOf(summary.kind),
        text = sentences.joinToString(" "),
        details = notificationDetails(summary, sentences),
        note = if (summary.isStale) getString(Res.string.notifications_stale) else null,
    )
}

/** Every sentence on a line of its own, below it the names when the user wants them. */
internal fun notificationDetails(summary: NotificationSummary, sentences: List<String>): String =
    summary.sections.zip(sentences).joinToString("\n") { (section, sentence) ->
        if (summary.showNames) "$sentence\n${section.names.joinToString(", ")}" else sentence
    }

private suspend fun sentenceOf(section: NotificationSection, dueSoonDays: Int): String {
    val count = section.names.size
    suspend fun within() =
        getPluralString(Res.plurals.start_stock_within_days, dueSoonDays, dueSoonDays)
    return when (section.topic) {
        NotificationTopic.STOCK_DUE_SOON ->
            getPluralString(Res.plurals.start_stock_due_soon, count, count, within())
        NotificationTopic.STOCK_OVERDUE ->
            getPluralString(Res.plurals.start_stock_overdue, count, count)
        NotificationTopic.STOCK_EXPIRED ->
            getPluralString(Res.plurals.start_stock_expired, count, count)
        NotificationTopic.STOCK_BELOW_MIN ->
            getPluralString(Res.plurals.start_stock_below_min, count, count)
        NotificationTopic.CHORES_OVERDUE ->
            getPluralString(Res.plurals.start_chores_overdue, count, count)
        NotificationTopic.CHORES_DUE_TODAY ->
            getPluralString(Res.plurals.start_chores_due_today, count, count)
        NotificationTopic.CHORES_DUE_SOON ->
            getPluralString(Res.plurals.start_chores_due_soon, count, count, within())
        NotificationTopic.TASKS_OVERDUE ->
            getPluralString(Res.plurals.start_tasks_overdue, count, count)
        NotificationTopic.TASKS_DUE_TODAY ->
            getPluralString(Res.plurals.start_tasks_due_today, count, count)
        NotificationTopic.TASKS_DUE_SOON ->
            getPluralString(Res.plurals.start_tasks_due_soon, count, count, within())
        NotificationTopic.BATTERIES_OVERDUE ->
            getPluralString(Res.plurals.start_batteries_overdue, count, count)
        NotificationTopic.BATTERIES_DUE_TODAY ->
            getPluralString(Res.plurals.start_batteries_due_today, count, count)
        NotificationTopic.BATTERIES_DUE_SOON ->
            getPluralString(Res.plurals.start_batteries_due_soon, count, count, within())
        NotificationTopic.MEAL_PLAN_TODAY ->
            getPluralString(Res.plurals.notifications_meal_plan_today, count, count)
    }
}
