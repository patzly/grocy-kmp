@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)

package com.patrickzedler.grocy.feature.recipes.impl.mealplan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.ListItemMenuButton
import com.patrickzedler.grocy.core.design.component.ListItemOverflowMenu
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.StatusColors
import com.patrickzedler.grocy.core.design.component.SwipeAction
import com.patrickzedler.grocy.core.design.component.SwipeActionBox
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.rememberStatusColors
import com.patrickzedler.grocy.core.forms.FieldChip
import com.patrickzedler.grocy.core.forms.FieldChipView
import com.patrickzedler.grocy.core.forms.ListDetailItem
import com.patrickzedler.grocy.core.forms.ProductThumbnail
import com.patrickzedler.grocy.core.forms.labelled
import com.patrickzedler.grocy.core.model.masterdata.nameFor
import com.patrickzedler.grocy.core.model.recipes.MealPlanEntryType
import com.patrickzedler.grocy.core.model.recipes.MealPlanSection
import com.patrickzedler.grocy.core.model.recipes.RecipeFulfillment
import com.patrickzedler.grocy.core.model.text.formatCurrency
import com.patrickzedler.grocy.core.model.text.formatDayAndMonth
import com.patrickzedler.grocy.core.model.text.formatDecimal
import com.patrickzedler.grocy.core.model.text.formatWeekday
import com.patrickzedler.grocy.core.model.text.platformDecimalSeparator
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.data_amount
import com.patrickzedler.grocy.core.resources.ic_rounded_add
import com.patrickzedler.grocy.core.resources.ic_rounded_check
import com.patrickzedler.grocy.core.resources.ic_rounded_group
import com.patrickzedler.grocy.core.resources.ic_rounded_local_fire_department
import com.patrickzedler.grocy.core.resources.ic_rounded_payments
import com.patrickzedler.grocy.core.resources.ic_rounded_undo
import com.patrickzedler.grocy.core.resources.meal_plan_add_note
import com.patrickzedler.grocy.core.resources.meal_plan_add_product
import com.patrickzedler.grocy.core.resources.meal_plan_add_recipe
import com.patrickzedler.grocy.core.resources.meal_plan_copy_day
import com.patrickzedler.grocy.core.resources.meal_plan_day_actions
import com.patrickzedler.grocy.core.resources.meal_plan_done
import com.patrickzedler.grocy.core.resources.meal_plan_energy_per_day
import com.patrickzedler.grocy.core.resources.meal_plan_mark_done
import com.patrickzedler.grocy.core.resources.meal_plan_mark_undone
import com.patrickzedler.grocy.core.resources.meal_plan_nothing_planned
import com.patrickzedler.grocy.core.resources.meal_plan_servings
import com.patrickzedler.grocy.core.resources.meal_plan_today
import com.patrickzedler.grocy.core.resources.product_energy
import com.patrickzedler.grocy.core.resources.recipe_costs_value
import com.patrickzedler.grocy.core.resources.recipe_energy_per_serving
import com.patrickzedler.grocy.core.resources.recipes_status_enough
import com.patrickzedler.grocy.core.resources.recipes_status_not_enough
import com.patrickzedler.grocy.feature.recipes.impl.RecipePicture
import com.patrickzedler.grocy.feature.recipes.impl.list.StatusChip
import com.patrickzedler.grocy.feature.recipes.impl.list.centeredOnFirstLine
import com.patrickzedler.grocy.feature.recipes.impl.list.recipeStatusChips
import com.patrickzedler.grocy.feature.recipes.impl.list.servingsText
import com.patrickzedler.grocy.feature.recipes.impl.list.statusFieldChip
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** What the items of the meal plan do, the same for every item. */
internal class MealPlanItemActions(
    /** Grocy's "Display recipe", the product overview or, for notes, "Edit this item". */
    val onClick: (MealPlanItem) -> Unit,
    val onDoneChange: (MealPlanItem, done: Boolean) -> Unit,
    val onAdd: (MealPlanEntryType, day: String) -> Unit,
    val onCopyDay: (day: String) -> Unit,
    val loadRecipePicture: suspend (fileName: String) -> ByteArray?,
    val loadProductPicture: suspend (fileName: String) -> ByteArray?,
)

/**
 * The header of a day: its weekday as heading, the date and what Grocy computed for the day
 * (costs and energy), "Nothing planned" without entries; today in secondary container. Its
 * button adds like Grocy's day header: a recipe, a product, a note, or copies the day.
 */
@Composable
internal fun MealPlanDayHeader(
    day: MealPlanDay,
    state: MealPlanUiState,
    index: Int,
    count: Int,
    actions: MealPlanItemActions,
    modifier: Modifier = Modifier,
) {
    val colors = if (day.isToday) {
        val content = MaterialTheme.colorScheme.onSecondaryContainer
        ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = content,
            supportingContentColor = content,
            trailingContentColor = content,
        )
    } else {
        SegmentedListDefaults.colors()
    }
    val weekday = formatWeekday(day.epochDay)
    val date = formatDayAndMonth(day.epochDay)
    val chips = daySummaryChips(day.summary, state)
    ListItem(
        modifier = modifier.fillMaxWidth(),
        overlineContent = if (day.isToday) {
            { Text(stringResource(Res.string.meal_plan_today)) }
        } else {
            null
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (day.items.isEmpty()) "$date · ${nothingPlanned()}" else date)
                if (chips.isNotEmpty()) ChipRow(chips)
            }
        },
        trailingContent = {
            DayMenuButton(day = day.day, label = "$weekday, $date", actions = actions)
        },
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = colors,
        content = {
            Text(
                text = weekday,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
        },
    )
}

/**
 * A day's header in the week's grid: weekday (with "Today" beside it) and date stacked, the add
 * button centered at their end, the summary below; as high as the row's other headers.
 */
@Composable
internal fun MealPlanDayCard(
    day: MealPlanDay,
    state: MealPlanUiState,
    actions: MealPlanItemActions,
    modifier: Modifier = Modifier,
) {
    val container = if (day.isToday) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        SegmentedListDefaults.containerColor
    }
    val content = if (day.isToday) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val weekday = formatWeekday(day.epochDay)
    val date = formatDayAndMonth(day.epochDay)
    val chips = daySummaryChips(day.summary, state)
    Surface(
        shape = SegmentedListDefaults.shape(index = 0, count = 1),
        color = container,
        contentColor = content,
        modifier = modifier,
    ) {
        Column(Modifier.padding(start = CardPadding, bottom = CardPadding)) {
            // The add button centered on weekday and date, as wide as the entries' menus
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = CardPadding),
            ) {
                Column(Modifier.weight(1f)) {
                    // "Today" beside the weekday, so weekday and date line up with the other days
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = weekday,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .alignByBaseline()
                                .semantics { heading() },
                        )
                        if (day.isToday) {
                            Text(
                                text = stringResource(Res.string.meal_plan_today),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                modifier = Modifier.padding(start = 8.dp).alignByBaseline(),
                            )
                        }
                    }
                    Text(
                        text = date,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (day.isToday) content else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DayMenuButton(
                    day = day.day,
                    label = "$weekday, $date",
                    actions = actions,
                    modifier = Modifier.padding(end = CardMenuPadding),
                )
            }
            if (chips.isNotEmpty()) {
                Column(Modifier.padding(top = 8.dp, end = CardPadding)) { ChipRow(chips) }
            }
        }
    }
}

/**
 * A planned entry as row of a day: the section as overline where [showsSection], the name
 * (crossed out once done), its chips, picture and menu. Swiping it towards the end marks it
 * as done or undone, also through its menu and an accessibility action.
 */
@Composable
internal fun MealPlanEntryRow(
    item: MealPlanItem,
    state: MealPlanUiState,
    index: Int,
    count: Int,
    menuGroups: List<List<AppBarMenuItem>>,
    actions: MealPlanItemActions,
    showsSection: Boolean,
) {
    val done = item.entry.done
    val chips = entryChips(item, state)
    val section = sectionLabel(item.section).takeIf { showsSection }
    SwipeActionBox(
        startAction = SwipeAction(
            icon = painterResource(
                if (done) Res.drawable.ic_rounded_undo else Res.drawable.ic_rounded_check,
            ),
            enabled = true,
            onSwipe = { actions.onDoneChange(item, !done) },
        ),
        endAction = null,
    ) {
        ListDetailItem(
            selected = null,
            onClick = { actions.onClick(item) },
            index = index,
            count = count,
            modifier = Modifier.fillMaxWidth().doneSemantics(item, actions),
            overlineContent = section?.let { { Text(it) } },
            supportingContent = if (chips.isEmpty()) null else { { ChipRow(chips) } },
            // Button and picture bring enough space of their own, unlike icons and switches
            trailingContent = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    pictureOf(item, actions)?.let { (fileName, load) ->
                        ProductThumbnail(fileName, load)
                    }
                    ListItemOverflowMenu(groups = menuGroups)
                }
            },
        ) {
            EntryName(item)
        }
    }
}

/**
 * A planned entry as card of a day in the week's grid: the picture across its top, the name
 * with the menu at its end and chips below, like the recipe gallery's cards.
 */
@Composable
internal fun MealPlanEntryCard(
    item: MealPlanItem,
    state: MealPlanUiState,
    index: Int,
    count: Int,
    menuGroups: List<List<AppBarMenuItem>>,
    actions: MealPlanItemActions,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val chips = entryChips(item, state)
    Surface(
        onClick = {
            haptic.confirm()
            actions.onClick(item)
        },
        shape = SegmentedListDefaults.shape(index = index, count = count),
        color = SegmentedListDefaults.containerColor,
        modifier = modifier.clickCursor().doneSemantics(item, actions),
    ) {
        Column(Modifier.padding(horizontal = CardPadding)) {
            pictureOf(item, actions)?.let { (fileName, load) ->
                RecipePicture(
                    fileName = fileName,
                    load = load,
                    shape = RoundedCornerShape(CardPictureCornerSize),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = CardPadding)
                        .aspectRatio(CARD_PICTURE_RATIO),
                )
            }
            // Like the recipe gallery's cards: the menu at the name's end, 16.dp around the name
            val nameStyle = MaterialTheme.typography.bodyLargeEmphasized
            Row(Modifier.padding(top = CardPadding)) {
                Box(Modifier.weight(1f)) { EntryName(item, maxLines = 3, style = nameStyle) }
                ListItemOverflowMenu(
                    groups = menuGroups,
                    modifier = Modifier.centeredOnFirstLine(
                        lineHeight = nameStyle.lineHeight,
                        endOverhang = CardPadding - CardMenuPadding,
                    ),
                )
            }
            if (chips.isNotEmpty()) {
                Column(Modifier.padding(top = CardPadding)) { ChipRow(chips) }
            }
            Spacer(Modifier.height(CardPadding))
        }
    }
}

/** A section's name with its time, like Grocy's row titles; null for the default section. */
internal fun sectionLabel(section: MealPlanSection): String? {
    if (section.name.isBlank()) return null
    return listOfNotNull(section.name, section.timeInfo).joinToString(" · ")
}

@Composable
private fun EntryName(
    item: MealPlanItem,
    maxLines: Int = Int.MAX_VALUE,
    style: TextStyle = LocalTextStyle.current,
) {
    val done = item.entry.done
    Text(
        text = item.name,
        // Grocy crosses done entries out and mutes them
        textDecoration = if (done) TextDecoration.LineThrough else null,
        color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else LocalContentColor.current,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = style,
    )
}

/** "Done" for screen readers and Grocy's done button as accessibility action. */
@Composable
private fun Modifier.doneSemantics(item: MealPlanItem, actions: MealPlanItemActions): Modifier {
    val done = item.entry.done
    val doneText = stringResource(Res.string.meal_plan_done)
    val toggleText = stringResource(
        if (done) Res.string.meal_plan_mark_undone else Res.string.meal_plan_mark_done,
    )
    return semantics {
        if (done) stateDescription = doneText
        customActions = listOf(
            CustomAccessibilityAction(toggleText) {
                actions.onDoneChange(item, !done)
                true
            },
        )
    }
}

/** Grocy's day button and its menu: add a recipe, a product or a note, copy the day. */
@Composable
private fun DayMenuButton(
    day: String,
    label: String,
    actions: MealPlanItemActions,
    modifier: Modifier = Modifier,
) {
    ListItemMenuButton(
        painter = painterResource(Res.drawable.ic_rounded_add),
        description = stringResource(Res.string.meal_plan_day_actions, label),
        groups = listOf(
            listOf(
                AppBarMenuItem(stringResource(Res.string.meal_plan_add_recipe)) {
                    actions.onAdd(MealPlanEntryType.RECIPE, day)
                },
                AppBarMenuItem(stringResource(Res.string.meal_plan_add_product)) {
                    actions.onAdd(MealPlanEntryType.PRODUCT, day)
                },
                AppBarMenuItem(stringResource(Res.string.meal_plan_add_note)) {
                    actions.onAdd(MealPlanEntryType.NOTE, day)
                },
            ),
            listOf(
                AppBarMenuItem(stringResource(Res.string.meal_plan_copy_day)) {
                    actions.onCopyDay(day)
                },
            ),
        ),
        modifier = modifier,
    )
}

/** Chips that wrap into more rows; a new layout for other chips, see `recipeDetails`. */
@Composable
private fun ChipRow(chips: List<FieldChip>) {
    key(chips.map { it.text }) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            chips.forEach { FieldChipView(it) }
        }
    }
}

/** The picture of a recipe or product with what loads it, null for notes and none. */
private fun pictureOf(
    item: MealPlanItem,
    actions: MealPlanItemActions,
): Pair<String, suspend (String) -> ByteArray?>? = when (val content = item.content) {
    is MealPlanContent.Recipe -> content.pictureFileName?.let { it to actions.loadRecipePicture }
    is MealPlanContent.Product ->
        content.pictureFileName?.let { it to actions.loadProductPicture }
    is MealPlanContent.Note -> null
}

/**
 * What Grocy shows of an entry below its name: a recipe's servings, whether the stock has
 * enough (with the stock feature), its costs (with price tracking) and energy per serving; a
 * product's amount, the same stock check, the costs by its last price and its energy.
 */
@Composable
private fun entryChips(item: MealPlanItem, state: MealPlanUiState): List<FieldChip> {
    val config = state.config
    val colors = rememberStatusColors()
    val separator = platformDecimalSeparator()
    val language = LocalLocale.current.toLanguageTag()
    val chips = mutableListOf<FieldChip>()
    when (val content = item.content) {
        is MealPlanContent.Recipe -> {
            val servings = servingsText(content.servings, state.amountDecimals)
            chips += FieldChip(
                text = servings,
                description = labelled(stringResource(Res.string.meal_plan_servings), servings),
                icon = Res.drawable.ic_rounded_group,
            )
            val fulfillment = content.fulfillment
            if (config.stock && fulfillment != null) {
                chips += recipeStatusChips(
                    fulfillment.status,
                    fulfillment.missingProductsCount,
                    colors,
                )
            }
            if (config.stockPriceTracking && fulfillment != null) {
                chips += costsChip(fulfillment.costs, state)
            }
            content.caloriesPerServing?.takeIf { it > 0 }?.let { calories ->
                val text = stringResource(
                    Res.string.recipe_energy_per_serving,
                    formatDecimal(calories, ENERGY_DECIMALS, separator),
                    config.energyUnit,
                )
                chips += FieldChip(text, text, Res.drawable.ic_rounded_local_fire_department)
            }
        }
        is MealPlanContent.Product -> {
            val amount = listOfNotNull(
                formatDecimal(content.amount, state.amountDecimals, separator),
                content.unit?.nameFor(content.amount, language),
            ).joinToString(" ")
            chips += FieldChip(
                text = amount,
                description = labelled(stringResource(Res.string.data_amount), amount),
            )
            if (config.stock) chips += productStatusChip(content.enoughInStock, colors)
            if (config.stockPriceTracking) {
                content.costs?.let { chips += costsChip(it, state) }
            }
            content.calories?.let { calories ->
                val text = "${formatDecimal(calories, ENERGY_DECIMALS, separator)} " +
                    config.energyUnit
                chips += FieldChip(
                    text = text,
                    description = labelled(stringResource(Res.string.product_energy), text),
                    icon = Res.drawable.ic_rounded_local_fire_department,
                )
            }
        }
        is MealPlanContent.Note -> Unit
    }
    return chips
}

/** Grocy's day summary: its costs (with price tracking) and energy "per day". */
@Composable
private fun daySummaryChips(summary: RecipeFulfillment?, state: MealPlanUiState): List<FieldChip> {
    summary ?: return emptyList()
    val chips = mutableListOf<FieldChip>()
    if (state.config.stockPriceTracking) chips += costsChip(summary.costs, state)
    if (summary.calories > 0) {
        val text = stringResource(
            Res.string.meal_plan_energy_per_day,
            formatDecimal(summary.calories, ENERGY_DECIMALS, platformDecimalSeparator()),
            state.config.energyUnit,
        )
        chips += FieldChip(text, text, Res.drawable.ic_rounded_local_fire_department)
    }
    return chips
}

@Composable
private fun costsChip(costs: Double, state: MealPlanUiState): FieldChip {
    val text = formatCurrency(costs, state.config.currency, state.priceDecimals)
    return FieldChip(
        text = text,
        description = stringResource(Res.string.recipe_costs_value, text),
        icon = Res.drawable.ic_rounded_payments,
    )
}

/** Grocy's check of a planned product: the stock has its amount or not. */
@Composable
private fun productStatusChip(enough: Boolean, colors: StatusColors): FieldChip {
    val chip = if (enough) StatusChip.ENOUGH else StatusChip.MISSING
    val text = stringResource(
        if (enough) Res.string.recipes_status_enough else Res.string.recipes_status_not_enough,
    )
    return statusFieldChip(chip, text, text, colors)
}

@Composable
private fun nothingPlanned(): String = stringResource(Res.string.meal_plan_nothing_planned)

// Grocy shows energy as whole numbers
private const val ENERGY_DECIMALS = 0

/** Wide like a photo, as the gallery's cards. */
private const val CARD_PICTURE_RATIO = 16f / 10f

private val CardPadding = 16.dp

// A step rounder than the inner corners of segmented items
private val CardPictureCornerSize = 8.dp

// Around the menu button's 48.dp room, so its icon sits 16.dp from the card's edges
private val CardMenuPadding = 4.dp
