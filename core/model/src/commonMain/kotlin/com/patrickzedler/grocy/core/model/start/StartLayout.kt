package com.patrickzedler.grocy.core.model.start

import com.patrickzedler.grocy.core.model.server.ServerConfig

/** The sections of the start screen, in their default order. */
enum class StartSection(
    /** Hidden while the server turned the feature off, like Grocy's menu. */
    val isAvailable: (ServerConfig) -> Boolean = { true },
) {
    STOCK_OVERVIEW({ it.stock }),
    SHOPPING_LIST({ it.stock && it.shoppingList }),
    RECIPES({ it.recipes }),
    MEAL_PLAN({ it.recipes && it.recipesMealPlan }),
    CHORES({ it.chores }),
    TASKS({ it.tasks }),
    BATTERIES({ it.batteries }),
    EQUIPMENT({ it.equipment }),
    CALENDAR({ it.calendar }),
    MASTER_DATA,
}

/** What the start screen is made of: its sections and dividers that start a new group. */
sealed interface StartLayoutEntry {
    data class Section(val section: StartSection) : StartLayoutEntry

    data object Divider : StartLayoutEntry
}

/** The start screen as the user arranged it; a section that is missing was removed. */
data class StartLayout(val entries: List<StartLayoutEntry>) {

    /** The groups of what [config] allows (everything until it is read), never an empty one. */
    fun groups(config: ServerConfig?): List<List<StartSection>> {
        val groups = mutableListOf(mutableListOf<StartSection>())
        entries.forEach { entry ->
            when (entry) {
                StartLayoutEntry.Divider -> groups += mutableListOf<StartSection>()
                is StartLayoutEntry.Section ->
                    if (config == null || entry.section.isAvailable(config)) {
                        groups.last() += entry.section
                    }
            }
        }
        return groups.filter { it.isNotEmpty() }
    }

    /**
     * This arrangement of the sections the user could see, with the others back in [previous]'s
     * places behind the section they followed, so a feature that comes back is where it was.
     */
    fun withHiddenFrom(previous: StartLayout, isHidden: (StartSection) -> Boolean): StartLayout {
        val result = entries.toMutableList()
        previous.entries.forEachIndexed { index, entry ->
            if (entry !is StartLayoutEntry.Section || !isHidden(entry.section)) return@forEachIndexed
            val before = previous.entries.take(index).lastOrNull { it in result }
            val place = if (before == null) 0 else result.lastIndexOf(before) + 1
            result.add(place, entry)
        }
        return StartLayout(result)
    }

    fun stored(): String = entries.joinToString(SEPARATOR) { entry ->
        when (entry) {
            StartLayoutEntry.Divider -> DIVIDER
            is StartLayoutEntry.Section -> entry.section.name
        }
    }

    companion object {
        private const val SEPARATOR = ","
        private const val DIVIDER = "-"

        private fun sections(vararg sections: StartSection) =
            sections.map { StartLayoutEntry.Section(it) }

        /** What the start screen shows until the user arranges it. */
        val Default = StartLayout(
            sections(StartSection.STOCK_OVERVIEW, StartSection.SHOPPING_LIST) +
                StartLayoutEntry.Divider +
                sections(StartSection.RECIPES, StartSection.MEAL_PLAN) +
                StartLayoutEntry.Divider +
                sections(
                    StartSection.CHORES,
                    StartSection.TASKS,
                    StartSection.BATTERIES,
                    StartSection.EQUIPMENT,
                    StartSection.CALENDAR,
                ) +
                StartLayoutEntry.Divider +
                sections(StartSection.MASTER_DATA),
        )

        /** Reads [stored]; what a newer version wrote and this one does not know is left out. */
        fun of(stored: String?): StartLayout {
            if (stored == null) return Default
            val seen = mutableSetOf<StartSection>()
            return StartLayout(
                stored.split(SEPARATOR).filter { it.isNotEmpty() }.mapNotNull { name ->
                    if (name == DIVIDER) return@mapNotNull StartLayoutEntry.Divider
                    StartSection.entries.firstOrNull { it.name == name }
                        ?.takeIf(seen::add)
                        ?.let { StartLayoutEntry.Section(it) }
                },
            )
        }
    }
}
