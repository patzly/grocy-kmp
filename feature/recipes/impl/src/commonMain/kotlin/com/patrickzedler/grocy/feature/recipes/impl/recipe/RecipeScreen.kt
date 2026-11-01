@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)

package com.patrickzedler.grocy.feature.recipes.impl.recipe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.FloatingToolbarIconButton
import com.patrickzedler.grocy.core.design.component.FloatingToolbarWithFab
import com.patrickzedler.grocy.core.design.component.LeadingTrailingContentWrapper
import com.patrickzedler.grocy.core.design.component.NavigateBackButton
import com.patrickzedler.grocy.core.design.component.NumberStepper
import com.patrickzedler.grocy.core.design.component.ScreenPane
import com.patrickzedler.grocy.core.design.component.SegmentedContainer
import com.patrickzedler.grocy.core.design.component.SegmentedGroup
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.SegmentedListScreen
import com.patrickzedler.grocy.core.design.component.StatusColors
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.insetItem
import com.patrickzedler.grocy.core.design.component.rememberStatusColors
import com.patrickzedler.grocy.core.design.component.segmentedGroup
import com.patrickzedler.grocy.core.forms.FieldChip
import com.patrickzedler.grocy.core.forms.FieldChipView
import com.patrickzedler.grocy.core.forms.GroupItems
import com.patrickzedler.grocy.core.forms.TextItem
import com.patrickzedler.grocy.core.forms.actionMessage
import com.patrickzedler.grocy.core.forms.htmlToPlainText
import com.patrickzedler.grocy.core.model.stock.DueStatus
import com.patrickzedler.grocy.core.model.text.formatCurrency
import com.patrickzedler.grocy.core.model.text.formatDecimal
import com.patrickzedler.grocy.core.model.text.platformDecimalSeparator
import com.patrickzedler.grocy.core.productcard.LabelPrintEffect
import com.patrickzedler.grocy.core.productcard.ProductOverviewHost
import com.patrickzedler.grocy.core.productcard.StockPageActions
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_scroll_to_top
import com.patrickzedler.grocy.core.resources.ic_rounded_add_shopping_cart
import com.patrickzedler.grocy.core.resources.ic_rounded_cooking
import com.patrickzedler.grocy.core.resources.ic_rounded_edit
import com.patrickzedler.grocy.core.resources.ic_rounded_local_fire_department
import com.patrickzedler.grocy.core.resources.ic_rounded_payments
import com.patrickzedler.grocy.core.resources.ic_rounded_restaurant
import com.patrickzedler.grocy.core.resources.ic_rounded_swap_horiz
import com.patrickzedler.grocy.core.resources.ic_rounded_vertical_align_top
import com.patrickzedler.grocy.core.resources.products_disabled
import com.patrickzedler.grocy.core.resources.recipe_add_missing
import com.patrickzedler.grocy.core.resources.recipe_add_missing_failed
import com.patrickzedler.grocy.core.resources.recipe_consume
import com.patrickzedler.grocy.core.resources.recipe_consume_failed
import com.patrickzedler.grocy.core.resources.recipe_consumed
import com.patrickzedler.grocy.core.resources.recipe_costs_value
import com.patrickzedler.grocy.core.resources.recipe_edit
import com.patrickzedler.grocy.core.resources.recipe_energy_per_serving
import com.patrickzedler.grocy.core.resources.recipe_ingredient_done
import com.patrickzedler.grocy.core.resources.recipe_ingredient_enough
import com.patrickzedler.grocy.core.resources.recipe_ingredient_missing
import com.patrickzedler.grocy.core.resources.recipe_ingredient_missing_amount
import com.patrickzedler.grocy.core.resources.recipe_ingredient_on_list_amount
import com.patrickzedler.grocy.core.resources.recipe_ingredient_substitute
import com.patrickzedler.grocy.core.resources.recipe_ingredient_variable_amount
import com.patrickzedler.grocy.core.resources.recipe_ingredients
import com.patrickzedler.grocy.core.resources.recipe_missing_added
import com.patrickzedler.grocy.core.resources.recipe_preparation
import com.patrickzedler.grocy.core.resources.recipe_preparation_mode
import com.patrickzedler.grocy.core.resources.recipe_servings_failed
import com.patrickzedler.grocy.core.resources.recipe_servings_saving
import com.patrickzedler.grocy.core.resources.recipe_title
import com.patrickzedler.grocy.core.resources.recipes_desired_servings
import com.patrickzedler.grocy.core.resources.recipes_status_enough
import com.patrickzedler.grocy.core.resources.stock_entry_due_soon
import com.patrickzedler.grocy.core.resources.stock_entry_expired
import com.patrickzedler.grocy.core.resources.stock_entry_overdue
import com.patrickzedler.grocy.feature.recipes.impl.RecipePicture
import com.patrickzedler.grocy.feature.recipes.impl.actions.RecipeActionEvent
import com.patrickzedler.grocy.feature.recipes.impl.actions.RecipeActionsHost
import com.patrickzedler.grocy.feature.recipes.impl.actions.RecipeActionsViewModel
import com.patrickzedler.grocy.feature.recipes.impl.actions.recipeMenuGroups
import com.patrickzedler.grocy.feature.recipes.impl.actions.rememberRecipeGrocycodeDownload
import com.patrickzedler.grocy.feature.recipes.impl.list.StatusChip
import com.patrickzedler.grocy.feature.recipes.impl.list.recipeStatusChips
import com.patrickzedler.grocy.feature.recipes.impl.list.statusChipsOf
import com.patrickzedler.grocy.feature.recipes.impl.list.statusFieldChip
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Where the recipe card leads. */
internal class RecipeLinks(
    val onEditClick: (recipeId: Int) -> Unit,
    val onPreparationModeClick: (recipeId: Int) -> Unit,
    /** Gets the copies and deletions, e.g. to open the copy or close a deleted recipe. */
    val onActionDone: (RecipeActionEvent) -> Unit,
)

/**
 * Grocy's recipe card: the picture across the top below the app bar, fully rounded, then the
 * name with the requirements as chips, Grocy's desired servings (saved while typing), energy and
 * costs (a tap on the costs explains them), the ingredients
 * by group (a tap opens the product's overview) and the preparation, the included recipes after
 * it. Consuming the ingredients is the toolbar's FAB, the preparation mode its last action
 * before "Scroll to top"; the menu has Grocy's recipe actions.
 * Beside the list ([pane] Detail) it has no back button of its own.
 */
@Composable
internal fun RecipeScreen(
    viewModel: RecipeViewModel,
    actions: RecipeActionsViewModel,
    stockPageActions: StockPageActions,
    links: RecipeLinks,
    menuItems: List<AppBarMenuItem>,
    pane: ScreenPane?,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val current = state
    val downloadGrocycode = rememberRecipeGrocycodeDownload(snackbarHostState) {
        current?.config?.grocycode2D ?: true
    }

    RecipeEvents(viewModel, snackbarHostState)
    RecipeActionsHost(actions, snackbarHostState, links.onActionDone)
    LabelPrintEffect(stockPageActions.labels, snackbarHostState)
    RecipeDialogs(viewModel, current)

    SegmentedListScreen(
        // The name heads the first item, like the picture it belongs to the content
        title = stringResource(Res.string.recipe_title),
        modifier = modifier,
        navigationIcon = {
            // Beside the list its own back button leaves both
            if (pane != ScreenPane.Detail) NavigateBackButton(onClick = onBackClick)
        },
        menuItems = menuItems,
        actionMenuGroups = current?.let {
            recipeMenuGroups(
                recipeId = viewModel.recipeId,
                config = it.config,
                actions = actions,
                onEditClick = links.onEditClick,
                onGrocycodeDownload = downloadGrocycode,
            )
        }.orEmpty(),
        pane = pane,
        listState = listState,
        snackbarHostState = snackbarHostState,
        floatingToolbar = { scrollBehavior, toolbarModifier ->
            val config = current?.config
            // Grocy consumes only with the stock feature, editing takes its place then
            val consumes = config?.stock != false
            FloatingToolbarWithFab(
                scrollBehavior = scrollBehavior,
                modifier = toolbarModifier,
                fabPainter = painterResource(
                    if (consumes) {
                        Res.drawable.ic_rounded_restaurant
                    } else {
                        Res.drawable.ic_rounded_edit
                    },
                ),
                fabDescription = stringResource(
                    if (consumes) Res.string.recipe_consume else Res.string.recipe_edit,
                ),
                onFabClick = {
                    when {
                        current == null || viewModel.isRunning -> Unit
                        consumes -> viewModel.requestConsume()
                        else -> links.onEditClick(viewModel.recipeId)
                    }
                },
            ) {
                if (config == null || config.stock && config.shoppingList) {
                    FloatingToolbarIconButton(
                        painter = painterResource(Res.drawable.ic_rounded_add_shopping_cart),
                        contentDescription = stringResource(Res.string.recipe_add_missing),
                        onClick = viewModel::requestAddMissing,
                        // Like Grocy, off while the shopping list covers what is missing
                        enabled = current?.fulfillment?.needFulfilledWithShoppingList == false &&
                            current.missingProducts.isNotEmpty() && !viewModel.isRunning,
                    )
                }
                if (consumes) {
                    FloatingToolbarIconButton(
                        painter = painterResource(Res.drawable.ic_rounded_edit),
                        contentDescription = stringResource(Res.string.recipe_edit),
                        onClick = { links.onEditClick(viewModel.recipeId) },
                        enabled = current != null,
                    )
                }
                FloatingToolbarIconButton(
                    painter = painterResource(Res.drawable.ic_rounded_cooking),
                    contentDescription = stringResource(Res.string.recipe_preparation_mode),
                    onClick = { links.onPreparationModeClick(viewModel.recipeId) },
                    enabled = current != null,
                )
                FloatingToolbarIconButton(
                    painter = painterResource(Res.drawable.ic_rounded_vertical_align_top),
                    contentDescription = stringResource(Res.string.action_scroll_to_top),
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                    enabled = listState.canScrollBackward,
                )
            }
        },
    ) {
        if (current == null) return@SegmentedListScreen
        recipeHeader(current, viewModel)
        current.sections.forEachIndexed { index, section ->
            recipeIngredients(section, index, current, viewModel, stockPageActions.overview::show)
            recipePreparation(section, index)
        }
    }

    IngredientOverviewHost(stockPageActions, snackbarHostState, current)
}

/** The dialogs of the card's actions and chips. */
@Composable
internal fun RecipeDialogs(viewModel: RecipeViewModel, current: RecipeUiState?) {
    when (val dialog = viewModel.dialog) {
        RecipeDialog.Consume -> ConsumeDialog(
            recipeName = current?.recipe?.name.orEmpty(),
            onConsume = viewModel::consume,
            onDismissRequest = viewModel::dismissDialog,
        )
        is RecipeDialog.AddMissing -> AddMissingDialog(
            recipeName = current?.recipe?.name.orEmpty(),
            products = dialog.products,
            onAdd = viewModel::addMissing,
            onDismissRequest = viewModel::dismissDialog,
        )
        RecipeDialog.Costs -> current?.fulfillment?.let { fulfillment ->
            CostsDialog(
                costs = costsText(current),
                pricesIncomplete = fulfillment.pricesIncomplete,
                onDismissRequest = viewModel::dismissDialog,
            )
        }
        null -> Unit
    }
}

/** The picture right below the app bar, then the name, servings, energy and costs. */
internal fun LazyListScope.recipeHeader(state: RecipeUiState, viewModel: RecipeViewModel) {
    state.recipe.pictureFileName?.let { fileName ->
        // Right below the app bar, as wide as the content and fully rounded
        insetItem(key = "picture") {
            RecipePicture(
                fileName = fileName,
                load = viewModel::picture,
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth().aspectRatio(PICTURE_RATIO),
            )
        }
    }
    overviewGroup(state, viewModel)
}

/** The ingredients of [section]: the recipe's own at [index] 0, then the included recipes. */
internal fun LazyListScope.recipeIngredients(
    section: RecipeSection,
    index: Int,
    state: RecipeUiState,
    viewModel: RecipeViewModel,
    onIngredientClick: (productId: Int) -> Unit,
) {
    section.groups.forEach { group ->
        ingredientGroup(
            key = "ingredients-${section.recipeId}-${group.title}",
            title = { groupTitle(index == 0, section.name, group.title) },
            lines = group.lines,
            state = state,
            viewModel = viewModel,
            onIngredientClick = onIngredientClick,
        )
    }
}

/** The preparation of [section] at [index], if it has one. */
internal fun LazyListScope.recipePreparation(section: RecipeSection, index: Int) {
    val text = section.description?.let(::htmlToPlainText)?.takeIf { it.isNotBlank() } ?: return
    preparationGroup(
        key = "preparation-${section.recipeId}",
        title = { preparationTitle(index == 0, section.name) },
        text = text,
    )
}

/** The product overview an ingredient opens. */
@Composable
internal fun IngredientOverviewHost(
    stockPageActions: StockPageActions,
    snackbarHostState: SnackbarHostState,
    current: RecipeUiState?,
) {
    ProductOverviewHost(
        overview = stockPageActions.overview,
        shoppingList = stockPageActions.shoppingList,
        labels = stockPageActions.labels,
        snackbarHostState = snackbarHostState,
        links = stockPageActions.links,
        nameOf = { id ->
            current?.sections?.firstNotNullOfOrNull { section ->
                section.groups.firstNotNullOfOrNull { group ->
                    group.lines.firstOrNull { it.productId == id }?.productName
                }
            }.orEmpty()
        },
    )
}

/** Grocy's messages of the card's actions. */
@Composable
internal fun RecipeEvents(viewModel: RecipeViewModel, snackbarHostState: SnackbarHostState) {
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            snackbarHostState.showSnackbar(
                when (event) {
                    is RecipeEvent.Consumed ->
                        getString(Res.string.recipe_consumed, event.recipeName)
                    RecipeEvent.MissingAdded -> getString(Res.string.recipe_missing_added)
                    is RecipeEvent.ConsumeFailed ->
                        event.error.actionMessage(Res.string.recipe_consume_failed)
                    is RecipeEvent.AddMissingFailed ->
                        event.error.actionMessage(Res.string.recipe_add_missing_failed)
                    is RecipeEvent.ServingsFailed ->
                        event.error.actionMessage(Res.string.recipe_servings_failed)
                },
            )
        }
    }
}

/**
 * What Grocy shows above the ingredients: the name with the requirements, desired servings,
 * energy and costs.
 */
private fun LazyListScope.overviewGroup(state: RecipeUiState, viewModel: RecipeViewModel) {
    segmentedGroup(key = "overview") {
        val fulfillment = state.fulfillment
        val config = state.config
        GroupItems(
            { index, count -> NameItem(state, viewModel.isSavingServings, index, count) },
            { index, count ->
                TextItem(
                    state = viewModel.servings,
                    label = stringResource(Res.string.recipes_desired_servings),
                    keyboardType = KeyboardType.Decimal,
                    stepper = NumberStepper.Positive,
                    index = index,
                    count = count,
                )
            },
            if (fulfillment != null && (fulfillment.calories > 0 || config.stockPriceTracking)) {
                { index, count ->
                    EnergyAndCostsItem(state, viewModel::showCosts, index, count)
                }
            } else {
                null
            },
        )
    }
}

/**
 * The recipe's name as the card's heading, Grocy's requirements as chips below it. While changed
 * servings are saved ([isSavingServings]) an indicator at its end tells the amounts are not
 * scaled yet.
 */
@Composable
private fun NameItem(state: RecipeUiState, isSavingServings: Boolean, index: Int, count: Int) {
    val fulfillment = state.fulfillment?.takeIf { state.config.stock }
    val chips = fulfillment?.let {
        recipeStatusChips(it.status, it.missingProductsCount, rememberStatusColors())
    }.orEmpty()
    SegmentedContainer(index = index, count = count) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.recipe.name,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                )
                if (isSavingServings) {
                    val saving = stringResource(Res.string.recipe_servings_saving)
                    LoadingIndicator(
                        modifier = Modifier
                            .size(SavingIndicatorSize)
                            .semantics {
                                contentDescription = saving
                                liveRegion = LiveRegionMode.Polite
                            },
                    )
                }
            }
            if (chips.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    chips.forEach { FieldChipView(it) }
                }
            }
        }
    }
}

/** "Costs: 2.50 €" of the recipe at its desired servings. */
@Composable
private fun costsText(state: RecipeUiState): String {
    val costs = state.fulfillment?.costs ?: 0.0
    return stringResource(
        Res.string.recipe_costs_value,
        formatCurrency(costs, state.config.currency, state.priceDecimals),
    )
}

/**
 * Grocy's energy per serving and costs as chips; the costs are yellow while prices are missing,
 * a tap on them shows Grocy's notes ([onCostsClick]).
 */
@Composable
private fun EnergyAndCostsItem(
    state: RecipeUiState,
    onCostsClick: () -> Unit,
    index: Int,
    count: Int,
) {
    val fulfillment = state.fulfillment ?: return
    val config = state.config
    val colors = rememberStatusColors()
    val separator = platformDecimalSeparator()
    val energy = if (fulfillment.calories > 0) {
        val text = stringResource(
            Res.string.recipe_energy_per_serving,
            formatDecimal(fulfillment.calories, ENERGY_DECIMALS, separator),
            config.energyUnit,
        )
        FieldChip(text, text, Res.drawable.ic_rounded_local_fire_department)
    } else {
        null
    }
    val costs = if (config.stockPriceTracking) {
        val text = costsText(state)
        FieldChip(
            text = text,
            description = text,
            icon = Res.drawable.ic_rounded_payments,
            status = colors.warning.takeIf { fulfillment.pricesIncomplete },
        )
    } else {
        null
    }
    SegmentedContainer(index = index, count = count) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            // The costs are a button with its larger touch target
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            energy?.let { FieldChipView(it) }
            costs?.let { FieldChipView(it, onClick = onCostsClick) }
        }
    }
}

private fun LazyListScope.ingredientGroup(
    key: String,
    title: @Composable () -> String,
    lines: List<IngredientLine>,
    state: RecipeUiState,
    viewModel: RecipeViewModel,
    onIngredientClick: (productId: Int) -> Unit,
) {
    insetItem(key = key) {
        SegmentedGroup(title = title()) {
            lines.forEachIndexed { index, line ->
                IngredientItem(
                    line = line,
                    state = state,
                    done = line.ingredientId in viewModel.doneIngredients,
                    onDoneChange = { viewModel.setIngredientDone(line.ingredientId, it) },
                    onClick = { onIngredientClick(line.productId) },
                    index = index,
                    count = lines.size,
                )
            }
        }
    }
}

private fun LazyListScope.preparationGroup(
    key: String,
    title: @Composable () -> String,
    text: String,
) {
    insetItem(key = key) {
        SegmentedGroup(title = title()) {
            SegmentedContainer(index = 0, count = 1) {
                Text(text, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

/** The recipe's own ingredients under Grocy's "Ingredients", an included one's under its name. */
@Composable
private fun groupTitle(isOwn: Boolean, recipeName: String, group: String?): String {
    val base = if (isOwn) stringResource(Res.string.recipe_ingredients) else recipeName
    return when {
        group == null -> base
        isOwn -> group
        else -> "$recipeName · $group"
    }
}

@Composable
private fun preparationTitle(isOwn: Boolean, recipeName: String): String {
    val preparation = stringResource(Res.string.recipe_preparation)
    return if (isOwn) preparation else "$recipeName · $preparation"
}

/**
 * An ingredient like Grocy's line: amount, unit and product, below it the stock status as chips
 * and what else Grocy notes (due status, the sub product used, a variable amount, energy and costs, the
 * note). A tap opens the product overview; with Grocy's checkboxes it can be marked as done.
 */
@Composable
private fun IngredientItem(
    line: IngredientLine,
    state: RecipeUiState,
    done: Boolean,
    onDoneChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    index: Int,
    count: Int,
) {
    val haptic = LocalHapticFeedback.current
    val colors = rememberStatusColors()
    val chips = ingredientStatusChips(line, colors) + ingredientChips(line, state, colors)
    val doneText = stringResource(Res.string.recipe_ingredient_done)
    // Done ones are crossed out and muted like Grocy's
    val textModifier = if (done) Modifier.alpha(DoneAlpha) else Modifier

    SegmentedListItem(
        onClick = {
            haptic.confirm()
            onClick()
        },
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = SegmentedListDefaults.colors(),
        modifier = Modifier.fillMaxWidth().clickCursor(),
        leadingContent = if (state.showsIngredientCheckboxes) {
            {
                LeadingTrailingContentWrapper {
                    Checkbox(
                        checked = done,
                        onCheckedChange = {
                            haptic.confirm()
                            onDoneChange(it)
                        },
                        modifier = Modifier
                            .clickCursor()
                            .semantics { contentDescription = doneText },
                    )
                }
            }
        } else {
            null
        },
        overlineContent = if (!line.productActive) {
            { Text(stringResource(Res.string.products_disabled)) }
        } else {
            null
        },
        supportingContent = if (chips.isNotEmpty() || line.note != null) {
            {
                Column(
                    modifier = textModifier.padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    line.note?.let { Text(it) }
                    if (chips.isNotEmpty()) {
                        key(chips.map { it.text }) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                chips.forEach { FieldChipView(it) }
                            }
                        }
                    }
                }
            }
        } else {
            null
        },
    ) {
        Text(
            text = line.text,
            modifier = textModifier,
            textDecoration = if (done) TextDecoration.LineThrough else null,
        )
    }
}

/**
 * An ingredient's stock status as chips like the recipe's: "Enough in stock", or what is
 * missing and, after it, what of it is on the shopping list.
 */
@Composable
private fun ingredientStatusChips(line: IngredientLine, colors: StatusColors): List<FieldChip> {
    val status = line.status ?: return emptyList()
    val enough = stringResource(Res.string.recipes_status_enough)
    val enoughDescription = stringResource(Res.string.recipe_ingredient_enough, line.stockAmount)
    val missing = stringResource(Res.string.recipe_ingredient_missing_amount, line.missingAmount)
    val missingDescription = stringResource(
        Res.string.recipe_ingredient_missing,
        line.missingAmount,
        line.amountOnShoppingList,
    )
    val onList =
        stringResource(Res.string.recipe_ingredient_on_list_amount, line.amountOnShoppingList)
    return statusChipsOf(status, line.isOnShoppingList).map { chip ->
        when (chip) {
            StatusChip.ENOUGH -> statusFieldChip(chip, enough, enoughDescription, colors)
            StatusChip.MISSING_ON_LIST, StatusChip.MISSING ->
                statusFieldChip(chip, missing, missingDescription, colors)
            StatusChip.ON_SHOPPING_LIST -> statusFieldChip(chip, onList, onList, colors)
        }
    }
}

@Composable
private fun ingredientChips(
    line: IngredientLine,
    state: RecipeUiState,
    colors: StatusColors,
): List<FieldChip> = buildList {
    when (line.dueStatus) {
        DueStatus.DUE_SOON -> stringResource(Res.string.stock_entry_due_soon).let {
            add(FieldChip(it, it, status = colors.warning))
        }
        DueStatus.OVERDUE -> stringResource(Res.string.stock_entry_overdue).let {
            add(FieldChip(it, it, status = colors.critical))
        }
        DueStatus.EXPIRED -> stringResource(Res.string.stock_entry_expired).let {
            add(FieldChip(it, it, status = colors.critical))
        }
        null -> Unit
    }
    line.substitute?.let { (_, name) ->
        add(
            FieldChip(
                text = name,
                description = stringResource(
                    Res.string.recipe_ingredient_substitute,
                    line.productName,
                    name,
                ),
                icon = Res.drawable.ic_rounded_swap_horiz,
            ),
        )
    }
    if (line.isVariableAmount) {
        val text = stringResource(Res.string.recipe_ingredient_variable_amount)
        add(FieldChip(text, text))
    }
    if (line.calories > 0) {
        val energy = formatDecimal(line.calories, ENERGY_DECIMALS, platformDecimalSeparator())
        val text = "$energy ${state.config.energyUnit}"
        add(FieldChip(text, text, Res.drawable.ic_rounded_local_fire_department))
    }
    if (state.config.stockPriceTracking) {
        val text = formatCurrency(line.costs, state.config.currency, state.priceDecimals)
        add(FieldChip(text, text, Res.drawable.ic_rounded_payments))
    }
}

/** Wide like a banner, so the ingredients stay close below it. */
private const val PICTURE_RATIO = 2f

// Grocy shows energy as whole numbers
private const val ENERGY_DECIMALS = 0

private const val DoneAlpha = 0.6f

// As high as a line of the name, so the item keeps its height
private val SavingIndicatorSize = 32.dp
