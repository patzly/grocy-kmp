package com.patrickzedler.grocy.feature.settings.impl.grocy

import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.server.UserSettings
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.batteries_settings_overview
import com.patrickzedler.grocy.core.resources.chores_settings_overview
import com.patrickzedler.grocy.core.resources.chores_settings_swap_buttons
import com.patrickzedler.grocy.core.resources.ic_rounded_battery_charging_full
import com.patrickzedler.grocy.core.resources.ic_rounded_checklist
import com.patrickzedler.grocy.core.resources.ic_rounded_cleaning_services
import com.patrickzedler.grocy.core.resources.ic_rounded_restaurant
import com.patrickzedler.grocy.core.resources.ic_rounded_shopping_cart
import com.patrickzedler.grocy.core.resources.recipes_settings_card
import com.patrickzedler.grocy.core.resources.recipes_settings_group_ingredients
import com.patrickzedler.grocy.core.resources.recipes_settings_ingredient_checkbox
import com.patrickzedler.grocy.core.resources.recipes_settings_ingredient_checkbox_hint
import com.patrickzedler.grocy.core.resources.recipes_settings_side_by_side
import com.patrickzedler.grocy.core.resources.settings_batteries
import com.patrickzedler.grocy.core.resources.settings_chores
import com.patrickzedler.grocy.core.resources.settings_recipes
import com.patrickzedler.grocy.core.resources.settings_shopping_list
import com.patrickzedler.grocy.core.resources.settings_tasks
import com.patrickzedler.grocy.core.resources.shopping_list_list
import com.patrickzedler.grocy.core.resources.shopping_list_settings_auto_add
import com.patrickzedler.grocy.core.resources.shopping_list_settings_auto_submit
import com.patrickzedler.grocy.core.resources.shopping_list_settings_calendar
import com.patrickzedler.grocy.core.resources.shopping_list_settings_round_up
import com.patrickzedler.grocy.core.resources.shopping_list_settings_workflow
import com.patrickzedler.grocy.feature.settings.api.GrocySettingsPage
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

/** A checkbox of one of Grocy's settings pages, [key] its user setting. */
internal enum class GrocySwitchSetting(
    val key: String,
    val label: StringResource,
    val hint: StringResource? = null,
    val valueOf: (UserSettings) -> Boolean,
) {
    SHOW_CALENDAR(
        "shopping_list_show_calendar",
        Res.string.shopping_list_settings_calendar,
        valueOf = { it.shoppingListShowCalendar },
    ),
    ROUND_UP(
        "shopping_list_round_up",
        Res.string.shopping_list_settings_round_up,
        valueOf = { it.shoppingListRoundUp },
    ),
    AUTO_ADD_BELOW_MIN_STOCK(
        "shopping_list_auto_add_below_min_stock_amount",
        Res.string.shopping_list_settings_auto_add,
        valueOf = { it.shoppingListAutoAddBelowMinStock },
    ),
    AUTO_SUBMIT(
        "shopping_list_to_stock_workflow_auto_submit_when_prefilled",
        Res.string.shopping_list_settings_auto_submit,
        valueOf = { it.shoppingListToStockAutoSubmit },
    ),
    LIST_SIDE_BY_SIDE(
        "recipes_show_list_side_by_side",
        Res.string.recipes_settings_side_by_side,
        valueOf = { it.recipesShowListSideBySide },
    ),
    INGREDIENT_CHECKBOX(
        "recipes_show_ingredient_checkbox",
        Res.string.recipes_settings_ingredient_checkbox,
        hint = Res.string.recipes_settings_ingredient_checkbox_hint,
        valueOf = { it.recipesShowIngredientCheckbox },
    ),
    GROUP_INGREDIENTS(
        "recipe_ingredients_group_by_product_group",
        Res.string.recipes_settings_group_ingredients,
        valueOf = { it.recipeIngredientsGroupByProductGroup },
    ),
    SWAP_TRACKING_BUTTONS(
        "chores_overview_swap_tracking_buttons",
        Res.string.chores_settings_swap_buttons,
        valueOf = { it.choresOverviewSwapTrackingButtons },
    ),
}

/** Grocy's "Due soon days" of a feature: whole days from 0, which hides "due soon". */
internal enum class DueSoonDaysSetting(val key: String, val valueOf: (UserSettings) -> Int) {
    CHORES("chores_due_soon_days", { it.choresDueSoonDays }),
    TASKS("tasks_due_soon_days", { it.tasksDueSoonDays }),
    BATTERIES("batteries_due_soon_days", { it.batteriesDueSoonDays }),
}

/** A setting of a [GrocySettingsGroup]. */
internal sealed interface GrocySetting {
    data class Switch(val setting: GrocySwitchSetting) : GrocySetting

    data class DueSoonDays(val setting: DueSoonDaysSetting) : GrocySetting

    /**
     * The shopping list that products below their min. stock amount go to, part of
     * [GrocySwitchSetting.AUTO_ADD_BELOW_MIN_STOCK] like the choice inside its label in Grocy.
     */
    data object AutoAddList : GrocySetting
}

/** A group of a settings page, [title] like Grocy's headings, null where it has none. */
internal data class GrocySettingsGroup(
    val title: StringResource?,
    val settings: List<GrocySetting>,
)

/** The title of the page like Grocy's settings menu. */
internal val GrocySettingsPage.title: StringResource
    get() = when (this) {
        GrocySettingsPage.SHOPPING_LIST -> Res.string.settings_shopping_list
        GrocySettingsPage.RECIPES -> Res.string.settings_recipes
        GrocySettingsPage.CHORES -> Res.string.settings_chores
        GrocySettingsPage.TASKS -> Res.string.settings_tasks
        GrocySettingsPage.BATTERIES -> Res.string.settings_batteries
    }

internal val GrocySettingsPage.icon: DrawableResource
    get() = when (this) {
        GrocySettingsPage.SHOPPING_LIST -> Res.drawable.ic_rounded_shopping_cart
        GrocySettingsPage.RECIPES -> Res.drawable.ic_rounded_restaurant
        GrocySettingsPage.CHORES -> Res.drawable.ic_rounded_cleaning_services
        GrocySettingsPage.TASKS -> Res.drawable.ic_rounded_checklist
        GrocySettingsPage.BATTERIES -> Res.drawable.ic_rounded_battery_charging_full
    }

/** Hidden like Grocy's menu entries while the server turned the feature off. */
internal fun GrocySettingsPage.isAvailable(config: ServerConfig): Boolean = when (this) {
    GrocySettingsPage.SHOPPING_LIST -> config.shoppingList
    GrocySettingsPage.RECIPES -> config.recipes
    GrocySettingsPage.CHORES -> config.chores
    GrocySettingsPage.TASKS -> config.tasks
    GrocySettingsPage.BATTERIES -> config.batteries
}

/**
 * The settings of the page in the groups and order of Grocy's page; with only one shopping
 * list there is nothing to choose for the products below their min. stock amount.
 */
internal fun GrocySettingsPage.groups(config: ServerConfig): List<GrocySettingsGroup> =
    when (this) {
        GrocySettingsPage.SHOPPING_LIST -> listOf(
            GrocySettingsGroup(
                title = Res.string.shopping_list_list,
                settings = listOfNotNull(
                    GrocySetting.Switch(GrocySwitchSetting.SHOW_CALENDAR),
                    GrocySetting.Switch(GrocySwitchSetting.ROUND_UP),
                    GrocySetting.Switch(GrocySwitchSetting.AUTO_ADD_BELOW_MIN_STOCK),
                    GrocySetting.AutoAddList.takeIf { config.shoppingListMultipleLists },
                ),
            ),
            GrocySettingsGroup(
                title = Res.string.shopping_list_settings_workflow,
                settings = listOf(GrocySetting.Switch(GrocySwitchSetting.AUTO_SUBMIT)),
            ),
        )

        GrocySettingsPage.RECIPES -> listOf(
            GrocySettingsGroup(
                title = null,
                settings = listOf(
                    GrocySetting.Switch(GrocySwitchSetting.LIST_SIDE_BY_SIDE),
                    GrocySetting.Switch(GrocySwitchSetting.INGREDIENT_CHECKBOX),
                ),
            ),
            GrocySettingsGroup(
                title = Res.string.recipes_settings_card,
                settings = listOf(GrocySetting.Switch(GrocySwitchSetting.GROUP_INGREDIENTS)),
            ),
        )

        GrocySettingsPage.CHORES -> listOf(
            GrocySettingsGroup(
                title = Res.string.chores_settings_overview,
                settings = listOf(
                    GrocySetting.DueSoonDays(DueSoonDaysSetting.CHORES),
                    GrocySetting.Switch(GrocySwitchSetting.SWAP_TRACKING_BUTTONS),
                ),
            ),
        )

        GrocySettingsPage.TASKS -> listOf(
            GrocySettingsGroup(
                title = null,
                settings = listOf(GrocySetting.DueSoonDays(DueSoonDaysSetting.TASKS)),
            ),
        )

        GrocySettingsPage.BATTERIES -> listOf(
            GrocySettingsGroup(
                title = Res.string.batteries_settings_overview,
                settings = listOf(GrocySetting.DueSoonDays(DueSoonDaysSetting.BATTERIES)),
            ),
        )
    }

/** The user setting of the shopping list [GrocySetting.AutoAddList] chooses. */
internal const val AUTO_ADD_LIST_KEY = "shopping_list_auto_add_below_min_stock_amount_list_id"
