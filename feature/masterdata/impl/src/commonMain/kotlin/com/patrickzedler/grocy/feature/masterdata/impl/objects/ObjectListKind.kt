package com.patrickzedler.grocy.feature.masterdata.impl.objects

import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.model.masterdata.ChoreAssignmentType
import com.patrickzedler.grocy.core.model.masterdata.ChorePeriodType
import com.patrickzedler.grocy.core.model.masterdata.UserfieldType
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.chore_assignment_in_alphabetical_order
import com.patrickzedler.grocy.core.resources.chore_assignment_none
import com.patrickzedler.grocy.core.resources.chore_assignment_random
import com.patrickzedler.grocy.core.resources.chore_assignment_who_least_did_first
import com.patrickzedler.grocy.core.resources.chore_period_adaptive
import com.patrickzedler.grocy.core.resources.chore_period_daily
import com.patrickzedler.grocy.core.resources.chore_period_hourly
import com.patrickzedler.grocy.core.resources.chore_period_manually
import com.patrickzedler.grocy.core.resources.chore_period_monthly
import com.patrickzedler.grocy.core.resources.chore_period_weekly
import com.patrickzedler.grocy.core.resources.chore_period_yearly
import com.patrickzedler.grocy.core.resources.objects_column_caption
import com.patrickzedler.grocy.core.resources.objects_column_charge_interval
import com.patrickzedler.grocy.core.resources.objects_column_description
import com.patrickzedler.grocy.core.resources.objects_column_entity
import com.patrickzedler.grocy.core.resources.objects_column_period_type
import com.patrickzedler.grocy.core.resources.objects_column_product_count
import com.patrickzedler.grocy.core.resources.objects_column_sort_number
import com.patrickzedler.grocy.core.resources.objects_column_type
import com.patrickzedler.grocy.core.resources.objects_column_used_in
import com.patrickzedler.grocy.core.resources.products_sort_name
import com.patrickzedler.grocy.core.resources.userfield_type_checkbox
import com.patrickzedler.grocy.core.resources.userfield_type_date
import com.patrickzedler.grocy.core.resources.userfield_type_datetime
import com.patrickzedler.grocy.core.resources.userfield_type_file
import com.patrickzedler.grocy.core.resources.userfield_type_image
import com.patrickzedler.grocy.core.resources.userfield_type_link
import com.patrickzedler.grocy.core.resources.userfield_type_link_with_title
import com.patrickzedler.grocy.core.resources.userfield_type_number_currency
import com.patrickzedler.grocy.core.resources.userfield_type_number_decimal
import com.patrickzedler.grocy.core.resources.userfield_type_number_integral
import com.patrickzedler.grocy.core.resources.userfield_type_preset_checklist
import com.patrickzedler.grocy.core.resources.userfield_type_preset_list
import com.patrickzedler.grocy.core.resources.userfield_type_text_multi_line
import com.patrickzedler.grocy.core.resources.userfield_type_text_single_line
import com.patrickzedler.grocy.feature.masterdata.impl.MasterDataCategory
import com.patrickzedler.grocy.feature.masterdata.impl.objects.ObjectColumn.CAPTION
import com.patrickzedler.grocy.feature.masterdata.impl.objects.ObjectColumn.CHARGE_INTERVAL
import com.patrickzedler.grocy.feature.masterdata.impl.objects.ObjectColumn.DESCRIPTION
import com.patrickzedler.grocy.feature.masterdata.impl.objects.ObjectColumn.ENTITY
import com.patrickzedler.grocy.feature.masterdata.impl.objects.ObjectColumn.NAME
import com.patrickzedler.grocy.feature.masterdata.impl.objects.ObjectColumn.PERIOD_TYPE
import com.patrickzedler.grocy.feature.masterdata.impl.objects.ObjectColumn.PRODUCT_COUNT
import com.patrickzedler.grocy.feature.masterdata.impl.objects.ObjectColumn.SORT_NUMBER
import com.patrickzedler.grocy.feature.masterdata.impl.objects.ObjectColumn.TYPE
import com.patrickzedler.grocy.feature.masterdata.impl.objects.ObjectColumn.USED_IN
import org.jetbrains.compose.resources.StringResource

/** Grocy's master data lists besides products, each like its table in the web frontend. */
internal enum class ObjectListKind(
    val category: MasterDataCategory,
    /** The columns of Grocy's table in its order, the fields the list can be sorted by. */
    val columns: List<ObjectColumn>,
    /** Grocy's tables start sorted by their first column. */
    val defaultColumn: ObjectColumn = columns.first(),
    val headline: ObjectColumn = NAME,
    /** Supporting lines of an item, each joining the values of its columns. */
    val supporting: List<List<ObjectColumn>>,
    /** False for objects without "Show disabled" in Grocy, which cannot be disabled. */
    val hasActive: Boolean = true,
    /** Grocy's entity of the objects' userfields, null where they have none. */
    val userfieldEntity: String?,
) {
    LOCATIONS(
        category = MasterDataCategory.LOCATIONS,
        columns = listOf(NAME, DESCRIPTION),
        supporting = listOf(listOf(DESCRIPTION)),
        userfieldEntity = MasterDataRepository.LOCATIONS,
    ),
    STORES(
        category = MasterDataCategory.STORES,
        columns = listOf(NAME, DESCRIPTION),
        supporting = listOf(listOf(DESCRIPTION)),
        userfieldEntity = MasterDataRepository.STORES,
    ),
    QUANTITY_UNITS(
        category = MasterDataCategory.QUANTITY_UNITS,
        columns = listOf(NAME, DESCRIPTION),
        supporting = listOf(listOf(DESCRIPTION)),
        userfieldEntity = MasterDataRepository.QUANTITY_UNITS,
    ),
    PRODUCT_GROUPS(
        category = MasterDataCategory.PRODUCT_GROUPS,
        columns = listOf(NAME, DESCRIPTION, PRODUCT_COUNT),
        supporting = listOf(listOf(DESCRIPTION), listOf(PRODUCT_COUNT)),
        userfieldEntity = MasterDataRepository.PRODUCT_GROUPS,
    ),
    CHORES(
        category = MasterDataCategory.CHORES,
        columns = listOf(NAME, PERIOD_TYPE, DESCRIPTION),
        supporting = listOf(listOf(PERIOD_TYPE), listOf(DESCRIPTION)),
        userfieldEntity = MasterDataRepository.CHORES,
    ),
    BATTERIES(
        category = MasterDataCategory.BATTERIES,
        columns = listOf(NAME, DESCRIPTION, USED_IN, CHARGE_INTERVAL),
        supporting = listOf(listOf(DESCRIPTION), listOf(USED_IN, CHARGE_INTERVAL)),
        userfieldEntity = MasterDataRepository.BATTERIES,
    ),
    TASK_CATEGORIES(
        category = MasterDataCategory.TASK_CATEGORIES,
        columns = listOf(NAME, DESCRIPTION),
        supporting = listOf(listOf(DESCRIPTION)),
        userfieldEntity = MasterDataRepository.TASK_CATEGORIES,
    ),
    USERFIELDS(
        category = MasterDataCategory.USERFIELDS,
        columns = listOf(ENTITY, NAME, CAPTION, TYPE, SORT_NUMBER),
        headline = CAPTION,
        supporting = listOf(listOf(ENTITY, NAME), listOf(TYPE, SORT_NUMBER)),
        hasActive = false,
        userfieldEntity = null,
    ),
    USERENTITIES(
        category = MasterDataCategory.USERENTITIES,
        columns = listOf(NAME, CAPTION),
        headline = CAPTION,
        supporting = listOf(listOf(NAME)),
        hasActive = false,
        userfieldEntity = null,
    ),
}

internal val ObjectColumn.label: StringResource
    get() = when (this) {
        NAME -> Res.string.products_sort_name
        DESCRIPTION -> Res.string.objects_column_description
        PRODUCT_COUNT -> Res.string.objects_column_product_count
        PERIOD_TYPE -> Res.string.objects_column_period_type
        USED_IN -> Res.string.objects_column_used_in
        CHARGE_INTERVAL -> Res.string.objects_column_charge_interval
        ENTITY -> Res.string.objects_column_entity
        CAPTION -> Res.string.objects_column_caption
        TYPE -> Res.string.objects_column_type
        SORT_NUMBER -> Res.string.objects_column_sort_number
    }

internal val ChorePeriodType.label: StringResource
    get() = when (this) {
        ChorePeriodType.HOURLY -> Res.string.chore_period_hourly
        ChorePeriodType.DAILY -> Res.string.chore_period_daily
        ChorePeriodType.MANUALLY -> Res.string.chore_period_manually
        ChorePeriodType.MONTHLY -> Res.string.chore_period_monthly
        ChorePeriodType.WEEKLY -> Res.string.chore_period_weekly
        ChorePeriodType.YEARLY -> Res.string.chore_period_yearly
        ChorePeriodType.ADAPTIVE -> Res.string.chore_period_adaptive
    }

internal val ChoreAssignmentType.label: StringResource
    get() = when (this) {
        ChoreAssignmentType.IN_ALPHABETICAL_ORDER ->
            Res.string.chore_assignment_in_alphabetical_order
        ChoreAssignmentType.NO_ASSIGNMENT -> Res.string.chore_assignment_none
        ChoreAssignmentType.RANDOM -> Res.string.chore_assignment_random
        ChoreAssignmentType.WHO_LEAST_DID_FIRST -> Res.string.chore_assignment_who_least_did_first
    }

internal val UserfieldType.label: StringResource
    get() = when (this) {
        UserfieldType.CHECKBOX -> Res.string.userfield_type_checkbox
        UserfieldType.DATE -> Res.string.userfield_type_date
        UserfieldType.DATETIME -> Res.string.userfield_type_datetime
        UserfieldType.NUMBER_INTEGRAL -> Res.string.userfield_type_number_integral
        UserfieldType.NUMBER_DECIMAL -> Res.string.userfield_type_number_decimal
        UserfieldType.NUMBER_CURRENCY -> Res.string.userfield_type_number_currency
        UserfieldType.FILE -> Res.string.userfield_type_file
        UserfieldType.IMAGE -> Res.string.userfield_type_image
        UserfieldType.LINK -> Res.string.userfield_type_link
        UserfieldType.LINK_WITH_TITLE -> Res.string.userfield_type_link_with_title
        UserfieldType.PRESET_CHECKLIST -> Res.string.userfield_type_preset_checklist
        UserfieldType.PRESET_LIST -> Res.string.userfield_type_preset_list
        UserfieldType.TEXT_SINGLE_LINE -> Res.string.userfield_type_text_single_line
        UserfieldType.TEXT_MULTI_LINE -> Res.string.userfield_type_text_multi_line
    }

/** Grocy's userfield types in the order of its form. */
internal val GrocyUserfieldTypes = listOf(
    UserfieldType.CHECKBOX,
    UserfieldType.DATE,
    UserfieldType.DATETIME,
    UserfieldType.NUMBER_INTEGRAL,
    UserfieldType.NUMBER_DECIMAL,
    UserfieldType.NUMBER_CURRENCY,
    UserfieldType.FILE,
    UserfieldType.IMAGE,
    UserfieldType.LINK,
    UserfieldType.LINK_WITH_TITLE,
    UserfieldType.PRESET_CHECKLIST,
    UserfieldType.PRESET_LIST,
    UserfieldType.TEXT_SINGLE_LINE,
    UserfieldType.TEXT_MULTI_LINE,
)

/**
 * The entities Grocy offers for userfields, sorted like its form: the exposed entities of its
 * API, one per userentity and its users.
 */
internal fun userfieldEntities(userentityNames: List<String>): List<String> =
    (ExposedEntities + userentityNames.map { MasterDataRepository.USERENTITY_PREFIX + it } +
        "users").distinct().sorted()

// The ExposedEntity enum of Grocy's OpenAPI specification
private val ExposedEntities = listOf(
    "products", "chores", "product_barcodes", "batteries", "locations", "quantity_units",
    "quantity_unit_conversions", "shopping_list", "shopping_lists", "shopping_locations",
    "recipes", "recipes_pos", "recipes_nestings", "tasks", "task_categories", "product_groups",
    "equipment", "api_keys", "userfields", "userentities", "userobjects", "meal_plan",
    "stock_log", "stock", "stock_current_locations", "chores_log", "meal_plan_sections",
    "products_last_purchased", "products_average_price", "quantity_unit_conversions_resolved",
    "recipes_pos_resolved", "battery_charge_cycles", "product_barcodes_view",
    "permission_hierarchy", "uihelper_shopping_list", "sessions",
)
