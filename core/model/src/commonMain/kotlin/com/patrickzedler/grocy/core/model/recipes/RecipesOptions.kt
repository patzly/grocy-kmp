package com.patrickzedler.grocy.core.model.recipes

/** The columns of Grocy's recipes table, in its order. */
enum class RecipeField {
    NAME,
    DESIRED_SERVINGS,
    DUE_SCORE,
    REQUIREMENTS_FULFILLED,
}

/** The columns Grocy's "Table options" can group the recipes by. */
enum class RecipeGrouping {
    NONE,
    DESIRED_SERVINGS,
    DUE_SCORE,
    REQUIREMENTS_FULFILLED,
}

/** Grocy's tabs of its recipes page. */
enum class RecipesView { LIST, GALLERY }

/**
 * How the user set up the recipes, kept across app starts like Grocy keeps its table options
 * and its last tab: the shown fields besides the name, grouping, sort and list or gallery. The
 * defaults are Grocy's: due score and requirements fulfilled shown, sorted by name.
 */
data class RecipesOptions(
    val fields: Set<RecipeField> = DEFAULT_FIELDS,
    val grouping: RecipeGrouping = RecipeGrouping.NONE,
    val sortField: RecipeField = RecipeField.NAME,
    val ascending: Boolean = true,
    val view: RecipesView = RecipesView.LIST,
) {
    companion object {
        val DEFAULT_FIELDS = setOf(RecipeField.DUE_SCORE, RecipeField.REQUIREMENTS_FULFILLED)

        /** Options from their stored parts; missing or unknown values fall back to defaults. */
        fun of(
            fields: String?,
            grouping: String?,
            sortField: String?,
            ascending: String?,
            view: String?,
        ): RecipesOptions {
            val default = RecipesOptions()
            return RecipesOptions(
                fields = fields?.let { stored ->
                    stored.split(SEPARATOR).mapNotNull { name ->
                        RecipeField.entries.firstOrNull { it.name == name }
                    }.toSet()
                } ?: default.fields,
                grouping = RecipeGrouping.entries.firstOrNull { it.name == grouping }
                    ?: default.grouping,
                sortField = RecipeField.entries.firstOrNull { it.name == sortField }
                    ?: default.sortField,
                ascending = ascending?.toBooleanStrictOrNull() ?: default.ascending,
                view = RecipesView.entries.firstOrNull { it.name == view } ?: default.view,
            )
        }

        /** [RecipesOptions.fields] as stored, read back by [of]. */
        fun storedFields(fields: Set<RecipeField>): String =
            fields.sortedBy { it.ordinal }.joinToString(SEPARATOR) { it.name }

        private const val SEPARATOR = ","
    }
}
