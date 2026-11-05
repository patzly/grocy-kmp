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
import com.patrickzedler.grocy.core.resources.notifications_names_and
import com.patrickzedler.grocy.core.resources.notifications_names_more
import com.patrickzedler.grocy.core.resources.notifications_sentence_names
import com.patrickzedler.grocy.core.resources.notifications_stale
import com.patrickzedler.grocy.core.resources.product_group_stock
import com.patrickzedler.grocy.core.resources.start_batteries_due_soon
import com.patrickzedler.grocy.core.resources.start_batteries_due_today
import com.patrickzedler.grocy.core.resources.start_batteries_overdue
import com.patrickzedler.grocy.core.resources.start_chores_due_soon
import com.patrickzedler.grocy.core.resources.start_chores_due_today
import com.patrickzedler.grocy.core.resources.start_chores_overdue
import com.patrickzedler.grocy.core.resources.start_list_join
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
 * A notification in words: [text] for one line, [details] each topic on a line of its own when
 * expanded and [note] that it shows the last known state.
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
    val sentences = summary.sections.map { section ->
        sentenceWithNames(sentenceOf(section, summary.dueSoonDays), namesOf(section.names))
    }
    return NotificationText(
        title = notificationTitleOf(summary.kind),
        text = sentences.joinToString(" "),
        details = sentences.joinToString("\n"),
        note = if (summary.isStale) getString(Res.string.notifications_stale) else null,
    )
}

/** A sentence of the start screen continued with [names], ended by the template's punctuation. */
private suspend fun sentenceWithNames(sentence: String, names: String): String = getString(
    Res.string.notifications_sentence_names,
    sentence.trimEnd(*SENTENCE_ENDS),
    names.trimEnd(*SENTENCE_ENDS),
)

private suspend fun namesOf(names: List<String>): String = notificationNames(
    names = names,
    join = { first, second -> getString(Res.string.start_list_join, first, second) },
    and = { first, second -> getString(Res.string.notifications_names_and, first, second) },
    more = { shown, rest ->
        getPluralString(Res.plurals.notifications_names_more, rest, shown, rest)
    },
)

/**
 * [names] as a list in a sentence: up to [MAX_NOTIFICATION_NAMES], the last ones told apart by
 * [and], or by [more] with how many are left. Notifications only have room for so many lines.
 */
internal suspend fun notificationNames(
    names: List<String>,
    join: suspend (String, String) -> String,
    and: suspend (String, String) -> String,
    more: suspend (String, Int) -> String,
): String {
    suspend fun joined(list: List<String>) = list.reduce { first, second -> join(first, second) }
    val rest = names.size - MAX_NOTIFICATION_NAMES
    return when {
        rest > 0 -> more(joined(names.take(MAX_NOTIFICATION_NAMES)), rest)
        names.size < 2 -> names.singleOrNull().orEmpty()
        else -> and(joined(names.dropLast(1)), names.last())
    }
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

internal const val MAX_NOTIFICATION_NAMES = 3

// What the start screen's sentences end with, in every language
private val SENTENCE_ENDS = charArrayOf('.', '。')
