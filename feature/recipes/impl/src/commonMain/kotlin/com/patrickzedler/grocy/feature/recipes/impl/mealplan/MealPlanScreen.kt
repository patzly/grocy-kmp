@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.patrickzedler.grocy.feature.recipes.impl.mealplan

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonDefaults.IconButtonWidthOption
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.FloatingToolbarIconButton
import com.patrickzedler.grocy.core.design.component.FloatingToolbarWithFab
import com.patrickzedler.grocy.core.design.component.GroupTitle
import com.patrickzedler.grocy.core.design.component.HapticTooltipBox
import com.patrickzedler.grocy.core.design.component.LocalShowsPanes
import com.patrickzedler.grocy.core.design.component.NavigateBackButton
import com.patrickzedler.grocy.core.design.component.SegmentedContainer
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.SegmentedListScreen
import com.patrickzedler.grocy.core.design.component.animatedItem
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.insetItem
import com.patrickzedler.grocy.core.design.component.insetItems
import com.patrickzedler.grocy.core.design.component.smallIconButtonSize
import com.patrickzedler.grocy.core.forms.actionMessage
import com.patrickzedler.grocy.core.model.masterdata.nameFor
import com.patrickzedler.grocy.core.model.recipes.MealPlanEntryType
import com.patrickzedler.grocy.core.model.recipes.RecipeStatus
import com.patrickzedler.grocy.core.model.text.formatCurrency
import com.patrickzedler.grocy.core.model.text.formatDateRange
import com.patrickzedler.grocy.core.model.text.formatDecimal
import com.patrickzedler.grocy.core.model.text.platformDecimalSeparator
import com.patrickzedler.grocy.core.productcard.LabelPrintEffect
import com.patrickzedler.grocy.core.productcard.ProductOverviewHost
import com.patrickzedler.grocy.core.productcard.StockPageActions
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_delete
import com.patrickzedler.grocy.core.resources.action_undo
import com.patrickzedler.grocy.core.resources.booking_consumed
import com.patrickzedler.grocy.core.resources.booking_undone
import com.patrickzedler.grocy.core.resources.data_save_failed
import com.patrickzedler.grocy.core.resources.ic_rounded_add
import com.patrickzedler.grocy.core.resources.ic_rounded_add_shopping_cart
import com.patrickzedler.grocy.core.resources.ic_rounded_chevron_left
import com.patrickzedler.grocy.core.resources.ic_rounded_chevron_right
import com.patrickzedler.grocy.core.resources.ic_rounded_restaurant
import com.patrickzedler.grocy.core.resources.ic_rounded_today
import com.patrickzedler.grocy.core.resources.meal_plan_add_recipe
import com.patrickzedler.grocy.core.resources.meal_plan_configure_sections
import com.patrickzedler.grocy.core.resources.meal_plan_consume_product
import com.patrickzedler.grocy.core.resources.meal_plan_consume_week
import com.patrickzedler.grocy.core.resources.meal_plan_day_copied
import com.patrickzedler.grocy.core.resources.meal_plan_deleted
import com.patrickzedler.grocy.core.resources.meal_plan_edit_entry
import com.patrickzedler.grocy.core.resources.meal_plan_mark_done
import com.patrickzedler.grocy.core.resources.meal_plan_mark_undone
import com.patrickzedler.grocy.core.resources.meal_plan_next_week
import com.patrickzedler.grocy.core.resources.meal_plan_previous_week
import com.patrickzedler.grocy.core.resources.meal_plan_title
import com.patrickzedler.grocy.core.resources.meal_plan_today
import com.patrickzedler.grocy.core.resources.meal_plan_week_consumed
import com.patrickzedler.grocy.core.resources.meal_plan_week_costs_value
import com.patrickzedler.grocy.core.resources.objects_delete_failed
import com.patrickzedler.grocy.core.resources.product_overview_add_to_shopping_list
import com.patrickzedler.grocy.core.resources.recipe_add_missing
import com.patrickzedler.grocy.core.resources.recipe_add_missing_failed
import com.patrickzedler.grocy.core.resources.recipe_consume
import com.patrickzedler.grocy.core.resources.recipe_consume_failed
import com.patrickzedler.grocy.core.resources.recipe_consumed
import com.patrickzedler.grocy.core.resources.recipe_missing_added
import com.patrickzedler.grocy.core.ui.RefreshErrorEffect
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Where the meal plan leads. */
internal class MealPlanLinks(
    /** Grocy's "Display recipe", at the planned servings. */
    val onRecipeOpen: (recipeId: Int) -> Unit,
    /** Grocy's "Configure sections". */
    val onSectionsClick: () -> Unit,
)

/**
 * Grocy's "Meal plan", a week at a time: on wide windows Grocy's grid, a column per day and a
 * row per section ([showsWeekGrid]), on narrower ones the days below each other, today's at
 * the top of the current week. The week's costs and actions lead the days; the toolbar moves
 * between weeks and adds a recipe, each day's button adds anything or copies the day.
 */
@Composable
internal fun MealPlanScreen(
    viewModel: MealPlanViewModel,
    stockPageActions: StockPageActions,
    links: MealPlanLinks,
    menuItems: List<AppBarMenuItem>,
    onBackClick: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refresh by viewModel.refresh.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val current = state

    RefreshErrorEffect(
        error = refresh.error,
        hasData = current != null && !current.week.isEmpty,
        snackbarHostState = snackbarHostState,
        onErrorShown = viewModel::onErrorShown,
    )
    MealPlanEvents(viewModel, snackbarHostState, links.onRecipeOpen)
    MealPlanDialogHost(viewModel)
    LabelPrintEffect(stockPageActions.labels, snackbarHostState)
    ProductOverviewHost(
        overview = stockPageActions.overview,
        shoppingList = stockPageActions.shoppingList,
        labels = stockPageActions.labels,
        snackbarHostState = snackbarHostState,
        links = stockPageActions.links,
        nameOf = { id ->
            current?.week?.days.orEmpty().firstNotNullOfOrNull { day ->
                day.items.firstNotNullOfOrNull { item ->
                    (item.content as? MealPlanContent.Product)?.takeIf { it.productId == id }?.name
                }
            }.orEmpty()
        },
    )

    val density = LocalDensity.current
    val windowWidth = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }
    val isGrid = showsWeekGrid(windowWidth, isSinglePane = LocalShowsPanes.current)
    WeekScrollEffect(current, isGrid, listState)

    val actions = MealPlanItemActions(
        onClick = { item ->
            when (val content = item.content) {
                is MealPlanContent.Recipe -> viewModel.openRecipe(item)
                is MealPlanContent.Product -> stockPageActions.overview.show(content.productId)
                is MealPlanContent.Note -> viewModel.edit(item.entry)
            }
        },
        onDoneChange = { item, done -> viewModel.setDone(item.entry, done) },
        onAdd = viewModel::add,
        onCopyDay = viewModel::requestCopyDay,
        loadRecipePicture = viewModel::recipePicture,
        loadProductPicture = viewModel::productPicture,
    )
    val menuGroups: @Composable (MealPlanItem) -> List<List<AppBarMenuItem>> = { item ->
        entryMenuGroups(item, current, viewModel, stockPageActions)
    }

    SegmentedListScreen(
        title = stringResource(Res.string.meal_plan_title),
        navigationIcon = { NavigateBackButton(onClick = onBackClick) },
        actionMenuItems = listOf(
            AppBarMenuItem(stringResource(Res.string.meal_plan_configure_sections)) {
                links.onSectionsClick()
            },
        ),
        menuItems = menuItems,
        listState = listState,
        snackbarHostState = snackbarHostState,
        isRefreshing = refresh.isRefreshing,
        onRefresh = viewModel::refresh,
        // Days are groups of their own, the group gap comes with each day
        itemSpacing = ListItemDefaults.SegmentedGap,
        floatingToolbar = { scrollBehavior, toolbarModifier ->
            FloatingToolbarWithFab(
                scrollBehavior = scrollBehavior,
                modifier = toolbarModifier,
                fabPainter = painterResource(Res.drawable.ic_rounded_add),
                fabDescription = stringResource(Res.string.meal_plan_add_recipe),
                onFabClick = {
                    current?.let { viewModel.add(MealPlanEntryType.RECIPE, addDayOf(it)) }
                },
            ) {
                FloatingToolbarIconButton(
                    painter = painterResource(Res.drawable.ic_rounded_chevron_left),
                    contentDescription = stringResource(Res.string.meal_plan_previous_week),
                    onClick = viewModel::showPreviousWeek,
                )
                FloatingToolbarIconButton(
                    painter = painterResource(Res.drawable.ic_rounded_today),
                    contentDescription = stringResource(Res.string.meal_plan_today),
                    onClick = viewModel::showToday,
                    enabled = current?.isCurrentWeek == false,
                )
                FloatingToolbarIconButton(
                    painter = painterResource(Res.drawable.ic_rounded_chevron_right),
                    contentDescription = stringResource(Res.string.meal_plan_next_week),
                    onClick = viewModel::showNextWeek,
                )
            }
        },
    ) {
        if (current == null) return@SegmentedListScreen
        val maxWidth = if (isGrid) Dp.Infinity else ListMaxWidth
        insetItem(key = "week", maxWidth = maxWidth) {
            WeekItem(current, viewModel)
        }
        if (isGrid) {
            weekGrid(current, actions, menuGroups)
        } else {
            weekAgenda(current, actions, menuGroups)
        }
    }
}

/**
 * The days below each other, each a group of its header and its entries in the order of their
 * sections, which name themselves above each entry.
 */
private fun LazyListScope.weekAgenda(
    state: MealPlanUiState,
    actions: MealPlanItemActions,
    menuGroups: @Composable (MealPlanItem) -> List<List<AppBarMenuItem>>,
) {
    state.week.days.forEach { day ->
        val count = day.items.size + 1
        insetItem(key = "day-${day.day}", contentType = "day") {
            MealPlanDayHeader(
                day = day,
                state = state,
                index = 0,
                count = count,
                actions = actions,
                modifier = Modifier.padding(top = GroupTopPadding),
            )
        }
        insetItems(
            items = day.items.withIndex().toList(),
            key = { "entry-${it.value.entry.id}" },
            contentType = { "entry" },
        ) { (index, item) ->
            MealPlanEntryRow(
                item = item,
                state = state,
                index = index + 1,
                count = count,
                menuGroups = menuGroups(item),
                actions = actions,
                showsSection = true,
            )
        }
    }
}

/**
 * Grocy's week: a column per day, the days' headers in one row, then a row per section the
 * week uses with its name above it and each day's entries of it below the day's header.
 */
private fun LazyListScope.weekGrid(
    state: MealPlanUiState,
    actions: MealPlanItemActions,
    menuGroups: @Composable (MealPlanItem) -> List<List<AppBarMenuItem>>,
) {
    val days = state.week.days
    insetItem(key = "days", contentType = "days", maxWidth = Dp.Infinity) {
        // The headers are as high as the highest, so the sections below start in one line
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(top = GroupTopPadding),
            horizontalArrangement = Arrangement.spacedBy(DayColumnGap),
        ) {
            days.forEach { day ->
                MealPlanDayCard(
                    day = day,
                    state = state,
                    actions = actions,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
    }
    state.week.sections.forEach { section ->
        val title = sectionLabel(section)
        insetItem(key = "section-${section.id}", contentType = "section", maxWidth = Dp.Infinity) {
            Column(Modifier.padding(top = GroupTopPadding)) {
                title?.let { GroupTitle(it) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DayColumnGap),
                    verticalAlignment = Alignment.Top,
                ) {
                    days.forEach { day ->
                        val items = day.itemsOf(section)
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                        ) {
                            items.forEachIndexed { index, item ->
                                MealPlanEntryCard(
                                    item = item,
                                    state = state,
                                    index = index,
                                    count = items.size,
                                    menuGroups = menuGroups(item),
                                    actions = actions,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The shown week as heading with Grocy's week costs (with price tracking) and its buttons:
 * put what the week misses on the shopping list (off while the list covers it) and consume
 * what it needs, both with the stock feature and while anything is planned.
 */
@Composable
private fun WeekItem(state: MealPlanUiState, viewModel: MealPlanViewModel) {
    val config = state.config
    val weekRecipe = state.week.weekRecipe
    val fulfillment = weekRecipe?.fulfillment
    SegmentedContainer(index = 0, count = 1) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = formatDateRange(state.week.start, state.week.end),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
                if (config.stockPriceTracking && fulfillment != null) {
                    Text(
                        text = stringResource(
                            Res.string.meal_plan_week_costs_value,
                            formatCurrency(fulfillment.costs, config.currency, state.priceDecimals),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (weekRecipe != null && config.stock) {
                // Material's 12.dp between the wide buttons, the pressed one grows
                ButtonGroup(overflowIndicator = {}) {
                    if (config.shoppingList) {
                        animatedItem { itemModifier, interactionSource ->
                            WeekButton(
                                painter = painterResource(Res.drawable.ic_rounded_add_shopping_cart),
                                description = stringResource(Res.string.recipe_add_missing),
                                enabled = fulfillment?.status ==
                                    RecipeStatus.NOT_ENOUGH_IN_STOCK && !viewModel.isRunning,
                                onClick = { viewModel.putOnShoppingList(weekRecipe.id) },
                                interactionSource = interactionSource,
                                modifier = itemModifier,
                            )
                        }
                    }
                    animatedItem { itemModifier, interactionSource ->
                        WeekButton(
                            painter = painterResource(Res.drawable.ic_rounded_restaurant),
                            description = stringResource(Res.string.meal_plan_consume_week),
                            enabled = !viewModel.isRunning,
                            onClick = { viewModel.requestConsumeWeek(weekRecipe.id) },
                            interactionSource = interactionSource,
                            modifier = itemModifier,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekButton(
    painter: Painter,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    HapticTooltipBox(text = description, modifier = modifier) {
        FilledTonalIconButton(
            onClick = {
                haptic.confirm()
                onClick()
            },
            enabled = enabled,
            modifier = smallIconButtonSize(IconButtonWidthOption.Wide).clickCursor(enabled),
            shapes = IconButtonDefaults.shapes(),
            interactionSource = interactionSource,
        ) {
            Icon(painter = painter, contentDescription = description)
        }
    }
}

/**
 * Grocy's buttons of an entry as its menu: edit and delete; consume and the shopping list for
 * recipes and products (with the stock and shopping list features, off where Grocy turns them
 * off); marking it as done or undone.
 */
@Composable
private fun entryMenuGroups(
    item: MealPlanItem,
    state: MealPlanUiState?,
    viewModel: MealPlanViewModel,
    stockPageActions: StockPageActions,
): List<List<AppBarMenuItem>> {
    val config = state?.config ?: return emptyList()
    val entry = item.entry
    val stockActions = when (val content = item.content) {
        is MealPlanContent.Recipe -> listOfNotNull(
            AppBarMenuItem(
                stringResource(Res.string.recipe_consume),
                enabled = content.shadowRecipeId != null,
            ) { viewModel.requestConsume(item) }.takeIf { config.stock },
            AppBarMenuItem(
                stringResource(Res.string.recipe_add_missing),
                enabled = content.shadowRecipeId != null &&
                    content.fulfillment?.status == RecipeStatus.NOT_ENOUGH_IN_STOCK,
            ) {
                content.shadowRecipeId?.let(viewModel::putOnShoppingList)
            }.takeIf { config.stock && config.shoppingList },
        )
        is MealPlanContent.Product -> {
            val language = LocalLocale.current.toLanguageTag()
            val amount = listOfNotNull(
                formatDecimal(content.amount, state.amountDecimals, platformDecimalSeparator()),
                content.unit?.nameFor(content.amount, language),
            ).joinToString(" ")
            listOfNotNull(
                AppBarMenuItem(
                    stringResource(Res.string.meal_plan_consume_product, amount, content.name),
                    enabled = content.enoughInStock,
                ) { viewModel.consumeProduct(item) }.takeIf { config.stock },
                AppBarMenuItem(
                    stringResource(Res.string.product_overview_add_to_shopping_list),
                    enabled = !content.enoughInStock,
                ) {
                    stockPageActions.shoppingList.open(content.productId, content.amount)
                }.takeIf { config.shoppingList },
            )
        }
        is MealPlanContent.Note -> emptyList()
    }
    return listOf(
        listOf(
            AppBarMenuItem(stringResource(Res.string.meal_plan_edit_entry)) {
                viewModel.edit(entry)
            },
            AppBarMenuItem(stringResource(Res.string.action_delete)) { viewModel.delete(entry) },
        ),
        stockActions,
        listOf(
            AppBarMenuItem(
                stringResource(
                    if (entry.done) Res.string.meal_plan_mark_undone else Res.string.meal_plan_mark_done,
                ),
            ) { viewModel.setDone(entry, !entry.done) },
        ),
    )
}

/**
 * Where the list stands when a week shows: the current week at today on narrow screens, where
 * the days are below each other, every other week at its start. A week shown before (e.g.
 * after rotating) keeps its place.
 */
@Composable
private fun WeekScrollEffect(state: MealPlanUiState?, isGrid: Boolean, listState: LazyListState) {
    var scrolledWeek by rememberSaveable { mutableStateOf<Long?>(null) }
    val weekStart = state?.week?.start
    LaunchedEffect(weekStart) {
        if (state == null || weekStart == null || weekStart == scrolledWeek) return@LaunchedEffect
        val isFirst = scrolledWeek == null
        scrolledWeek = weekStart
        val todayIndex = state.week.days.indexOfFirst { it.isToday }
        when {
            !isGrid && todayIndex > 0 -> listState.scrollToItem(agendaIndexOfDay(state, todayIndex))
            !isFirst -> listState.scrollToItem(0)
        }
    }
}

/** The index of day [dayIndex]'s header in the narrow list: after the week and earlier days. */
internal fun agendaIndexOfDay(state: MealPlanUiState, dayIndex: Int): Int =
    1 + state.week.days.take(dayIndex).sumOf { it.items.size + 1 }

/** Grocy's messages of the meal plan's actions in [snackbarHostState]. */
@Composable
private fun MealPlanEvents(
    viewModel: MealPlanViewModel,
    snackbarHostState: SnackbarHostState,
    onRecipeOpen: (Int) -> Unit,
) {
    val currentOnRecipeOpen by rememberUpdatedState(onRecipeOpen)
    val language = LocalLocale.current.toLanguageTag()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            // Messages with "Undo" wait for it, the next ones queue in the host meanwhile
            launch {
                when (event) {
                    is MealPlanEvent.Deleted -> {
                        val result = snackbarHostState.showSnackbar(
                            message = getString(Res.string.meal_plan_deleted),
                            actionLabel = getString(Res.string.action_undo),
                            duration = SnackbarDuration.Long,
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.restore(event.entry)
                        }
                    }
                    is MealPlanEvent.ProductConsumed -> {
                        val amount = listOfNotNull(
                            formatDecimal(event.amount, MAX_DECIMALS, platformDecimalSeparator()),
                            event.unit?.nameFor(event.amount, language),
                        ).joinToString(" ")
                        val result = snackbarHostState.showSnackbar(
                            message = getString(Res.string.booking_consumed, amount, event.name),
                            actionLabel = event.transactionId?.let {
                                getString(Res.string.action_undo)
                            },
                            duration = SnackbarDuration.Long,
                        )
                        val transaction = event.transactionId
                        if (result == SnackbarResult.ActionPerformed && transaction != null) {
                            viewModel.undoTransaction(transaction)
                        }
                    }
                    is MealPlanEvent.RecipeConsumed -> snackbarHostState.showSnackbar(
                        getString(Res.string.recipe_consumed, event.name),
                    )
                    MealPlanEvent.WeekConsumed ->
                        snackbarHostState.showSnackbar(getString(Res.string.meal_plan_week_consumed))
                    MealPlanEvent.PutOnShoppingList ->
                        snackbarHostState.showSnackbar(getString(Res.string.recipe_missing_added))
                    MealPlanEvent.DayCopied ->
                        snackbarHostState.showSnackbar(getString(Res.string.meal_plan_day_copied))
                    MealPlanEvent.Undone ->
                        snackbarHostState.showSnackbar(getString(Res.string.booking_undone))
                    MealPlanEvent.Restored -> Unit
                    is MealPlanEvent.OpenRecipe -> currentOnRecipeOpen(event.recipeId)
                    is MealPlanEvent.Failed -> snackbarHostState.showSnackbar(
                        event.error.actionMessage(event.action.failure),
                    )
                }
            }
        }
    }
}

private val MealPlanAction.failure: StringResource
    get() = when (this) {
        MealPlanAction.SAVE, MealPlanAction.DONE, MealPlanAction.COPY_DAY, MealPlanAction.UNDO ->
            Res.string.data_save_failed
        MealPlanAction.DELETE -> Res.string.objects_delete_failed
        MealPlanAction.CONSUME -> Res.string.recipe_consume_failed
        MealPlanAction.SHOPPING_LIST -> Res.string.recipe_add_missing_failed
    }

// Messages show amounts like Grocy's toasts, without trailing zeros
private const val MAX_DECIMALS = 4

// The width of the list's items where the days are below each other
private val ListMaxWidth = 640.dp

// The list puts the segmented gap between its items, a group adds the rest of a group gap
private val GroupTopPadding = SegmentedListDefaults.GroupSpacing - ListItemDefaults.SegmentedGap

