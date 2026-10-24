package com.patrickzedler.grocy.core.model.masterdata

data class Chore(
    val id: Int,
    val name: String,
    val description: String? = null,
    val active: Boolean = true,
    val periodType: ChorePeriodType = ChorePeriodType.MANUALLY,
    /** Day of the month for monthly chores. */
    val periodDays: Int = 0,
    val periodInterval: Int = 1,
    /** Weekdays of weekly chores. */
    val weekdays: Set<Weekday> = emptySet(),
    /** Grocy's "YYYY-MM-DD HH:MM:SS"; the first execution is scheduled from it. */
    val startDate: String? = null,
    val assignmentType: ChoreAssignmentType = ChoreAssignmentType.NO_ASSIGNMENT,
    /** Ids of the users the executions are assigned to. */
    val assignedUserIds: List<Int> = emptyList(),
    val trackDateOnly: Boolean = false,
    val rollover: Boolean = false,
    val consumeProductOnExecution: Boolean = false,
    val productId: Int? = null,
    val productAmount: Double? = null,
    val userfields: Map<String, String?> = emptyMap(),
)

/** Grocy's period types, in the order of its form. */
enum class ChorePeriodType(val value: String) {
    HOURLY("hourly"),
    DAILY("daily"),
    MANUALLY("manually"),
    MONTHLY("monthly"),
    WEEKLY("weekly"),
    YEARLY("yearly"),
    ADAPTIVE("adaptive"),
    ;

    companion object {
        fun of(value: String?): ChorePeriodType =
            entries.firstOrNull { it.value == value } ?: MANUALLY
    }
}

/** Grocy's assignment types, in the order of its form. */
enum class ChoreAssignmentType(val value: String) {
    IN_ALPHABETICAL_ORDER("in-alphabetical-order"),
    NO_ASSIGNMENT("no-assignment"),
    RANDOM("random"),
    WHO_LEAST_DID_FIRST("who-least-did-first"),
    ;

    companion object {
        fun of(value: String?): ChoreAssignmentType =
            entries.firstOrNull { it.value == value } ?: NO_ASSIGNMENT
    }
}

/** Weekdays as Grocy names them in `period_config`, Monday first like its form. */
enum class Weekday(val value: String) {
    MONDAY("monday"),
    TUESDAY("tuesday"),
    WEDNESDAY("wednesday"),
    THURSDAY("thursday"),
    FRIDAY("friday"),
    SATURDAY("saturday"),
    SUNDAY("sunday"),
    ;

    companion object {
        /** The weekdays of a comma separated `period_config`, unknown parts left out. */
        fun parse(config: String?): Set<Weekday> {
            val parts = config.orEmpty().split(",").map { it.trim() }.toSet()
            return entries.filter { it.value in parts }.toSet()
        }

        fun format(weekdays: Set<Weekday>): String =
            entries.filter { it in weekdays }.joinToString(",") { it.value }
    }
}
