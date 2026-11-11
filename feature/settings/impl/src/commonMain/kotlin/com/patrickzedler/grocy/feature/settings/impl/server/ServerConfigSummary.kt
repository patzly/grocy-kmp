package com.patrickzedler.grocy.feature.settings.impl.server

import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.batteries_overview_title
import com.patrickzedler.grocy.core.resources.calendar_title
import com.patrickzedler.grocy.core.resources.chores_overview_title
import com.patrickzedler.grocy.core.resources.equipment_title
import com.patrickzedler.grocy.core.resources.meal_plan_title
import com.patrickzedler.grocy.core.resources.recipes_title
import com.patrickzedler.grocy.core.resources.settings_server_feature_camera_scanning
import com.patrickzedler.grocy.core.resources.settings_server_feature_chore_assignments
import com.patrickzedler.grocy.core.resources.settings_server_feature_due_date_tracking
import com.patrickzedler.grocy.core.resources.settings_server_feature_freezing
import com.patrickzedler.grocy.core.resources.settings_server_feature_label_printer
import com.patrickzedler.grocy.core.resources.settings_server_feature_location_tracking
import com.patrickzedler.grocy.core.resources.settings_server_feature_multiple_lists
import com.patrickzedler.grocy.core.resources.settings_server_feature_opened_tracking
import com.patrickzedler.grocy.core.resources.settings_server_feature_price_tracking
import com.patrickzedler.grocy.core.resources.shopping_list_list
import com.patrickzedler.grocy.core.resources.shopping_list_thermal_printer
import com.patrickzedler.grocy.core.resources.stock_overview_title
import com.patrickzedler.grocy.core.resources.tasks_title
import org.jetbrains.compose.resources.StringResource

/** A feature flag of the server's configuration. */
internal data class ServerFeature(val name: StringResource, val isEnabled: Boolean)

/** The server's feature flags in the order of Grocy's configuration file. */
internal fun serverFeaturesOf(config: ServerConfig): List<ServerFeature> = listOf(
    ServerFeature(Res.string.stock_overview_title, config.stock),
    ServerFeature(Res.string.settings_server_feature_price_tracking, config.stockPriceTracking),
    ServerFeature(
        Res.string.settings_server_feature_location_tracking,
        config.stockLocationTracking,
    ),
    ServerFeature(
        Res.string.settings_server_feature_due_date_tracking,
        config.stockBestBeforeDateTracking,
    ),
    ServerFeature(
        Res.string.settings_server_feature_opened_tracking,
        config.stockProductOpenedTracking,
    ),
    ServerFeature(Res.string.settings_server_feature_freezing, config.stockProductFreezing),
    ServerFeature(
        Res.string.settings_server_feature_camera_scanning,
        config.browserBarcodeCameraScanning,
    ),
    ServerFeature(Res.string.shopping_list_list, config.shoppingList),
    ServerFeature(
        Res.string.settings_server_feature_multiple_lists,
        config.shoppingListMultipleLists,
    ),
    ServerFeature(Res.string.recipes_title, config.recipes),
    ServerFeature(Res.string.meal_plan_title, config.recipesMealPlan),
    ServerFeature(Res.string.chores_overview_title, config.chores),
    ServerFeature(Res.string.settings_server_feature_chore_assignments, config.choresAssignments),
    ServerFeature(Res.string.tasks_title, config.tasks),
    ServerFeature(Res.string.batteries_overview_title, config.batteries),
    ServerFeature(Res.string.equipment_title, config.equipment),
    ServerFeature(Res.string.calendar_title, config.calendar),
    ServerFeature(Res.string.settings_server_feature_label_printer, config.labelPrinter),
    ServerFeature(Res.string.shopping_list_thermal_printer, config.thermalPrinter),
)

/** What the configuration calls its Grocycodes. */
internal fun grocycodeTypeOf(config: ServerConfig): String =
    if (config.grocycode2D) "DataMatrix (2D)" else "Code 128 (1D)"

/**
 * The configuration as `FormattedText` markup: each part a headline over its bullet list, parts
 * without entries left out.
 */
internal fun serverConfigMarkup(parts: List<Pair<String, List<String>>>): String =
    parts.filter { (_, entries) -> entries.isNotEmpty() }
        .joinToString("\n\n") { (title, entries) ->
            "### $title\n\n" + entries.joinToString("\n") { "- $it" }
        }
