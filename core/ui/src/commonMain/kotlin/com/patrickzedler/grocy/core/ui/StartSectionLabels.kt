package com.patrickzedler.grocy.core.ui

import com.patrickzedler.grocy.core.model.start.StartSection
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.batteries_overview_title
import com.patrickzedler.grocy.core.resources.calendar_title
import com.patrickzedler.grocy.core.resources.chores_overview_title
import com.patrickzedler.grocy.core.resources.equipment_title
import com.patrickzedler.grocy.core.resources.ic_rounded_battery_0_bar
import com.patrickzedler.grocy.core.resources.ic_rounded_build
import com.patrickzedler.grocy.core.resources.ic_rounded_calendar_month
import com.patrickzedler.grocy.core.resources.ic_rounded_checklist
import com.patrickzedler.grocy.core.resources.ic_rounded_cleaning_services
import com.patrickzedler.grocy.core.resources.ic_rounded_database
import com.patrickzedler.grocy.core.resources.ic_rounded_date_range
import com.patrickzedler.grocy.core.resources.ic_rounded_kitchen
import com.patrickzedler.grocy.core.resources.ic_rounded_menu_book
import com.patrickzedler.grocy.core.resources.ic_rounded_shopping_cart
import com.patrickzedler.grocy.core.resources.masterdata_title
import com.patrickzedler.grocy.core.resources.meal_plan_title
import com.patrickzedler.grocy.core.resources.recipes_title
import com.patrickzedler.grocy.core.resources.shopping_list_list
import com.patrickzedler.grocy.core.resources.stock_overview_title
import com.patrickzedler.grocy.core.resources.tasks_title
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

/** The name of a section of the start screen, also in the dialog that arranges them. */
val StartSection.title: StringResource
    get() = when (this) {
        StartSection.STOCK_OVERVIEW -> Res.string.stock_overview_title
        StartSection.SHOPPING_LIST -> Res.string.shopping_list_list
        StartSection.RECIPES -> Res.string.recipes_title
        StartSection.MEAL_PLAN -> Res.string.meal_plan_title
        StartSection.CHORES -> Res.string.chores_overview_title
        StartSection.TASKS -> Res.string.tasks_title
        StartSection.BATTERIES -> Res.string.batteries_overview_title
        StartSection.EQUIPMENT -> Res.string.equipment_title
        StartSection.CALENDAR -> Res.string.calendar_title
        StartSection.MASTER_DATA -> Res.string.masterdata_title
    }

/** The icon of a section of the start screen. */
val StartSection.icon: DrawableResource
    get() = when (this) {
        StartSection.STOCK_OVERVIEW -> Res.drawable.ic_rounded_kitchen
        StartSection.SHOPPING_LIST -> Res.drawable.ic_rounded_shopping_cart
        StartSection.RECIPES -> Res.drawable.ic_rounded_menu_book
        StartSection.MEAL_PLAN -> Res.drawable.ic_rounded_calendar_month
        StartSection.CHORES -> Res.drawable.ic_rounded_cleaning_services
        StartSection.TASKS -> Res.drawable.ic_rounded_checklist
        StartSection.BATTERIES -> Res.drawable.ic_rounded_battery_0_bar
        StartSection.EQUIPMENT -> Res.drawable.ic_rounded_build
        StartSection.CALENDAR -> Res.drawable.ic_rounded_date_range
        StartSection.MASTER_DATA -> Res.drawable.ic_rounded_database
    }
