package com.patrickzedler.grocy.core.model.masterdata

/** A custom field the user defined for an entity, e.g. for all products. */
data class Userfield(
    val id: Int,
    val entity: String,
    val name: String,
    val caption: String,
    val type: UserfieldType,
    val showAsColumnInTables: Boolean = false,
    val inputRequired: Boolean = false,
    /** Options of the preset types, one per line in Grocy. */
    val options: List<String> = emptyList(),
    val defaultValue: String? = null,
    val sortNumber: Int? = null,
)

enum class UserfieldType(val value: String) {
    TEXT_SINGLE_LINE("text-single-line"),
    TEXT_MULTI_LINE("text-multi-line"),
    NUMBER_INTEGRAL("number-integral"),
    NUMBER_DECIMAL("number-decimal"),
    NUMBER_CURRENCY("number-currency"),
    DATE("date"),
    DATETIME("datetime"),
    CHECKBOX("checkbox"),
    PRESET_LIST("preset-list"),
    PRESET_CHECKLIST("preset-checklist"),
    LINK("link"),
    LINK_WITH_TITLE("link-with-title"),
    FILE("file"),
    IMAGE("image"),
    ;

    companion object {
        /** Unknown types of newer Grocy versions are edited as text. */
        fun of(value: String?): UserfieldType =
            entries.firstOrNull { it.value == value } ?: TEXT_SINGLE_LINE
    }
}
