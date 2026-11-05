package com.patrickzedler.grocy.core.model.notifications

import com.patrickzedler.grocy.core.model.server.ServerConfig

/** The features of the server that notify once a day, each with its own settings. */
enum class NotificationKind(internal val defaultMinuteOfDay: Int) {
    STOCK(9 * MINUTES_PER_HOUR),
    CHORES(9 * MINUTES_PER_HOUR),
    TASKS(9 * MINUTES_PER_HOUR),
    BATTERIES(9 * MINUTES_PER_HOUR),
    MEAL_PLAN(16 * MINUTES_PER_HOUR),
    ;

    /** Whether [config] has this feature, Grocy hides its pages otherwise. */
    fun isAvailable(config: ServerConfig): Boolean = when (this) {
        STOCK -> config.stock
        CHORES -> config.chores
        TASKS -> config.tasks
        BATTERIES -> config.batteries
        MEAL_PLAN -> config.recipes && config.recipesMealPlan
    }

    /** What a notification of this kind can tell about, in Grocy's order of its messages. */
    val topics: List<NotificationTopic>
        get() = NotificationTopic.entries.filter { it.kind == this }
}

/** One thing a notification can tell about; [byDefault] when it is on until changed. */
enum class NotificationTopic(val kind: NotificationKind, internal val byDefault: Boolean = true) {
    STOCK_DUE_SOON(NotificationKind.STOCK),
    STOCK_OVERDUE(NotificationKind.STOCK),
    STOCK_EXPIRED(NotificationKind.STOCK),
    STOCK_BELOW_MIN(NotificationKind.STOCK),
    CHORES_OVERDUE(NotificationKind.CHORES),
    CHORES_DUE_TODAY(NotificationKind.CHORES),
    CHORES_DUE_SOON(NotificationKind.CHORES, byDefault = false),
    TASKS_OVERDUE(NotificationKind.TASKS),
    TASKS_DUE_TODAY(NotificationKind.TASKS),
    TASKS_DUE_SOON(NotificationKind.TASKS, byDefault = false),
    BATTERIES_OVERDUE(NotificationKind.BATTERIES),
    BATTERIES_DUE_TODAY(NotificationKind.BATTERIES),
    BATTERIES_DUE_SOON(NotificationKind.BATTERIES, byDefault = false),
    MEAL_PLAN_TODAY(NotificationKind.MEAL_PLAN),
    ;

    /** Whether [config] has what this tells about. */
    fun isAvailable(config: ServerConfig): Boolean = when (this) {
        STOCK_DUE_SOON, STOCK_OVERDUE, STOCK_EXPIRED -> config.stockBestBeforeDateTracking
        else -> true
    }
}

/**
 * How one [NotificationKind] notifies: every day at [minuteOfDay] about its [topics] and, where
 * Grocy assigns things, what the user has to do ([onlyMine]).
 */
data class NotificationKindSettings(
    val enabled: Boolean = false,
    val minuteOfDay: Int,
    val topics: Set<NotificationTopic>,
    val onlyMine: Boolean = false,
)

/** The settings of every [NotificationKind], kept on the device. */
data class NotificationSettings(
    private val kinds: Map<NotificationKind, NotificationKindSettings> =
        NotificationKind.entries.associateWith(::defaultsOf),
) {
    operator fun get(kind: NotificationKind): NotificationKindSettings = kinds.getValue(kind)

    fun with(kind: NotificationKind, settings: NotificationKindSettings) =
        NotificationSettings(kinds + (kind to settings))

    /** The kinds that are turned on, whether their feature is available or not. */
    val enabledKinds: Set<NotificationKind>
        get() = NotificationKind.entries.filterTo(mutableSetOf()) { this[it].enabled }

    /** What [of] reads back: "KIND=enabled;minute;TOPIC,TOPIC;1;mine", joined with "|". */
    fun stored(): String = NotificationKind.entries.joinToString(KIND_SEPARATOR) { kind ->
        val settings = this[kind]
        listOf(
            settings.enabled.stored(),
            settings.minuteOfDay.toString(),
            settings.topics.joinToString(TOPIC_SEPARATOR) { it.name },
            // The place of a former setting, so what is stored stays readable both ways
            TRUE,
            settings.onlyMine.stored(),
        ).joinToString(PART_SEPARATOR, prefix = "${kind.name}=")
    }

    companion object {
        /** Reads [stored]; what a newer version wrote falls back to the defaults. */
        fun of(stored: String?): NotificationSettings {
            val kinds = stored.orEmpty().split(KIND_SEPARATOR).mapNotNull { part ->
                val kind = NotificationKind.entries
                    .firstOrNull { it.name == part.substringBefore('=') }
                    ?: return@mapNotNull null
                kind to kindSettingsOf(kind, part.substringAfter('='))
            }
            return NotificationSettings(NotificationSettings().kinds + kinds)
        }

        private fun kindSettingsOf(kind: NotificationKind, text: String): NotificationKindSettings {
            val defaults = defaultsOf(kind)
            val parts = text.split(PART_SEPARATOR)
            val names = parts.getOrNull(TOPICS_PART)?.split(TOPIC_SEPARATOR)
            return NotificationKindSettings(
                enabled = parts.getOrNull(ENABLED_PART) == TRUE,
                minuteOfDay = parts.getOrNull(MINUTE_PART)?.toIntOrNull()
                    ?.takeIf { it in 0 until MINUTES_PER_DAY }
                    ?: defaults.minuteOfDay,
                topics = names?.let { list ->
                    kind.topics.filterTo(mutableSetOf()) { it.name in list }
                } ?: defaults.topics,
                onlyMine = parts.getOrNull(MINE_PART) == TRUE,
            )
        }
    }
}

private fun defaultsOf(kind: NotificationKind) = NotificationKindSettings(
    minuteOfDay = kind.defaultMinuteOfDay,
    topics = kind.topics.filterTo(mutableSetOf()) { it.byDefault },
)

private fun Boolean.stored() = if (this) TRUE else FALSE

internal const val MINUTES_PER_HOUR = 60
internal const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR
private const val KIND_SEPARATOR = "|"
private const val PART_SEPARATOR = ";"
private const val TOPIC_SEPARATOR = ","
private const val TRUE = "1"
private const val FALSE = "0"
private const val ENABLED_PART = 0
private const val MINUTE_PART = 1
private const val TOPICS_PART = 2
private const val MINE_PART = 4
