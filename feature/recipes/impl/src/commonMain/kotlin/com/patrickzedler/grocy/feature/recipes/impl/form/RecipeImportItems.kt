package com.patrickzedler.grocy.feature.recipes.impl.form

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.LeadingTrailingContentWrapper
import com.patrickzedler.grocy.core.design.component.ListItemOverflowMenu
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.segmentedGroup
import com.patrickzedler.grocy.core.model.recipes.ImportTexts
import com.patrickzedler.grocy.core.model.recipes.RecipeNutrient
import com.patrickzedler.grocy.core.network.RecipeImportFailure
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_retry
import com.patrickzedler.grocy.core.resources.ic_rounded_add
import com.patrickzedler.grocy.core.resources.ic_rounded_error
import com.patrickzedler.grocy.core.resources.ic_rounded_info
import com.patrickzedler.grocy.core.resources.product_facts_carbohydrates
import com.patrickzedler.grocy.core.resources.product_facts_fat
import com.patrickzedler.grocy.core.resources.product_facts_fiber
import com.patrickzedler.grocy.core.resources.product_facts_filled
import com.patrickzedler.grocy.core.resources.product_facts_protein
import com.patrickzedler.grocy.core.resources.product_facts_saturated_fat
import com.patrickzedler.grocy.core.resources.product_facts_sugars
import com.patrickzedler.grocy.core.resources.recipe_import_add_ready
import com.patrickzedler.grocy.core.resources.recipe_import_author
import com.patrickzedler.grocy.core.resources.recipe_import_category
import com.patrickzedler.grocy.core.resources.recipe_import_cook_time
import com.patrickzedler.grocy.core.resources.recipe_import_cuisine
import com.patrickzedler.grocy.core.resources.recipe_import_hours
import com.patrickzedler.grocy.core.resources.recipe_import_hours_minutes
import com.patrickzedler.grocy.core.resources.recipe_import_ingredients
import com.patrickzedler.grocy.core.resources.recipe_import_ingredients_hint
import com.patrickzedler.grocy.core.resources.recipe_import_leave_out
import com.patrickzedler.grocy.core.resources.recipe_import_loading
import com.patrickzedler.grocy.core.resources.recipe_import_minutes
import com.patrickzedler.grocy.core.resources.recipe_import_no_product
import com.patrickzedler.grocy.core.resources.recipe_import_no_recipe
import com.patrickzedler.grocy.core.resources.recipe_import_nutrition
import com.patrickzedler.grocy.core.resources.recipe_import_prep_time
import com.patrickzedler.grocy.core.resources.recipe_import_sodium
import com.patrickzedler.grocy.core.resources.recipe_import_source
import com.patrickzedler.grocy.core.resources.recipe_import_total_time
import com.patrickzedler.grocy.core.resources.recipe_import_unreachable
import com.patrickzedler.grocy.core.resources.recipe_import_url_invalid
import com.patrickzedler.grocy.core.resources.recipe_ingredients
import com.patrickzedler.grocy.core.resources.recipe_preparation
import com.patrickzedler.grocy.core.resources.recipe_save_hint
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** The words of the imported preparation in the app's language. */
@Composable
internal fun rememberImportTexts(): ImportTexts {
    val minutes = stringResource(Res.string.recipe_import_minutes)
    val hours = stringResource(Res.string.recipe_import_hours)
    val hoursMinutes = stringResource(Res.string.recipe_import_hours_minutes)
    val nutrients = mapOf(
        RecipeNutrient.FAT to stringResource(Res.string.product_facts_fat),
        RecipeNutrient.SATURATED_FAT to stringResource(Res.string.product_facts_saturated_fat),
        RecipeNutrient.CARBOHYDRATES to stringResource(Res.string.product_facts_carbohydrates),
        RecipeNutrient.SUGARS to stringResource(Res.string.product_facts_sugars),
        RecipeNutrient.FIBER to stringResource(Res.string.product_facts_fiber),
        RecipeNutrient.PROTEIN to stringResource(Res.string.product_facts_protein),
        RecipeNutrient.SODIUM to stringResource(Res.string.recipe_import_sodium),
    )
    val ingredients = stringResource(Res.string.recipe_ingredients)
    val preparation = stringResource(Res.string.recipe_preparation)
    val prepTime = stringResource(Res.string.recipe_import_prep_time)
    val cookTime = stringResource(Res.string.recipe_import_cook_time)
    val totalTime = stringResource(Res.string.recipe_import_total_time)
    val nutrition = stringResource(Res.string.recipe_import_nutrition)
    val category = stringResource(Res.string.recipe_import_category)
    val cuisine = stringResource(Res.string.recipe_import_cuisine)
    val author = stringResource(Res.string.recipe_import_author)
    val source = stringResource(Res.string.recipe_import_source)
    return remember(minutes, hours, hoursMinutes, nutrients, ingredients, preparation, source) {
        ImportTexts(
            ingredients = ingredients,
            preparation = preparation,
            prepTime = prepTime,
            cookTime = cookTime,
            totalTime = totalTime,
            nutrition = nutrition,
            nutrients = nutrients,
            category = category,
            cuisine = cuisine,
            author = author,
            source = source,
            minutes = { formatMinutes(it, minutes, hours, hoursMinutes) },
        )
    }
}

/** "45 min", "2 h" or "1 h 15 min" from the app's patterns of them. */
internal fun formatMinutes(total: Int, minutes: String, hours: String, hoursMinutes: String) =
    when {
        total < MINUTES_PER_HOUR -> minutes.replace("%1\$d", total.toString())
        total % MINUTES_PER_HOUR == 0 ->
            hours.replace("%1\$d", (total / MINUTES_PER_HOUR).toString())
        else -> hoursMinutes
            .replace("%1\$d", (total / MINUTES_PER_HOUR).toString())
            .replace("%2\$d", (total % MINUTES_PER_HOUR).toString())
    }

/** Where reading the recipe site's page at [host] stands: reading, failed with retry, done. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun LazyListScope.importStatusGroup(
    state: RecipeImportState?,
    host: String,
    onRetryClick: () -> Unit,
) {
    state ?: return
    segmentedGroup(key = "import") {
        val haptic = LocalHapticFeedback.current
        val text = when (state) {
            RecipeImportState.Loading -> stringResource(Res.string.recipe_import_loading, host)
            RecipeImportState.Done -> stringResource(Res.string.product_facts_filled, host)
            is RecipeImportState.Failed -> when (state.failure) {
                RecipeImportFailure.INVALID_URL ->
                    stringResource(Res.string.recipe_import_url_invalid)
                RecipeImportFailure.UNREACHABLE ->
                    stringResource(Res.string.recipe_import_unreachable, host)
                RecipeImportFailure.NO_RECIPE ->
                    stringResource(Res.string.recipe_import_no_recipe, host)
            }
        }
        val isError = state is RecipeImportState.Failed
        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite },
            shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
            colors = SegmentedListDefaults.colors(),
            verticalAlignment = Alignment.CenterVertically,
            leadingContent = {
                LeadingTrailingContentWrapper {
                    when (state) {
                        RecipeImportState.Loading ->
                            LoadingIndicator(modifier = Modifier.size(IndicatorSize))
                        RecipeImportState.Done ->
                            Icon(painterResource(Res.drawable.ic_rounded_info), null)
                        is RecipeImportState.Failed -> Icon(
                            painter = painterResource(Res.drawable.ic_rounded_error),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            trailingContent = if (isError) {
                {
                    TextButton(
                        onClick = {
                            haptic.confirm()
                            onRetryClick()
                        },
                        modifier = Modifier.clickCursor(),
                        shapes = ButtonDefaults.shapes(),
                    ) {
                        Text(stringResource(Res.string.action_retry))
                    }
                }
            } else {
                null
            },
            content = { Text(text) },
        )
    }
}

/**
 * The recipe site's ingredient lines that are no ingredient yet: a tap opens the ingredient form
 * with what was proposed, the menu leaves a line out; "Add all proposed" adds the lines that
 * need no choice at once. Until the recipe is saved, nothing can be added.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun LazyListScope.importLinesGroup(
    lines: List<ImportLine>,
    canAdd: Boolean,
    isAdding: Boolean,
    onLineClick: (key: Int) -> Unit,
    onLeaveOut: (key: Int) -> Unit,
    onAddReadyClick: () -> Unit,
) {
    if (lines.isEmpty()) return
    segmentedGroup(key = "importLines", title = Res.string.recipe_import_ingredients) {
        val haptic = LocalHapticFeedback.current
        val count = lines.size + 1
        val leaveOut = stringResource(Res.string.recipe_import_leave_out)
        val noProduct = stringResource(Res.string.recipe_import_no_product)
        lines.forEachIndexed { index, line ->
            SegmentedListItem(
                onClick = {
                    haptic.confirm()
                    onLineClick(line.key)
                },
                enabled = canAdd,
                modifier = Modifier.fillMaxWidth().clickCursor(enabled = canAdd),
                shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
                colors = SegmentedListDefaults.colors(),
                verticalAlignment = Alignment.CenterVertically,
                supportingContent = { Text(line.proposal() ?: noProduct) },
                trailingContent = {
                    ListItemOverflowMenu(
                        groups = listOf(listOf(AppBarMenuItem(leaveOut) { onLeaveOut(line.key) })),
                    )
                },
            ) {
                Text(line.line.text)
            }
        }
        val readyCount = lines.count { it.isReady }
        val enabled = canAdd && readyCount > 0 && !isAdding
        SegmentedListItem(
            onClick = {
                haptic.confirm()
                onAddReadyClick()
            },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().clickCursor(enabled = enabled),
            shapes = ListItemDefaults.segmentedShapes(index = count - 1, count = count),
            colors = SegmentedListDefaults.colors(),
            verticalAlignment = Alignment.CenterVertically,
            leadingContent = {
                LeadingTrailingContentWrapper {
                    if (isAdding) {
                        LoadingIndicator(modifier = Modifier.size(IndicatorSize))
                    } else {
                        Icon(painterResource(Res.drawable.ic_rounded_add), null)
                    }
                }
            },
            supportingContent = {
                Text(
                    stringResource(
                        if (canAdd) {
                            Res.string.recipe_import_ingredients_hint
                        } else {
                            Res.string.recipe_save_hint
                        },
                    ),
                )
            },
        ) {
            Text(stringResource(Res.string.recipe_import_add_ready, readyCount))
        }
    }
}

/** "Mehl · 200 g": the proposed product with amount and unit, null without product. */
internal fun ImportLine.proposal(): String? {
    val name = productName ?: return null
    val amountText = listOf(amount, unitName.orEmpty()).filter { it.isNotBlank() }.joinToString(" ")
    return listOf(name, amountText).filter { it.isNotBlank() }.joinToString(" · ")
}

private const val MINUTES_PER_HOUR = 60
private val IndicatorSize = 24.dp
