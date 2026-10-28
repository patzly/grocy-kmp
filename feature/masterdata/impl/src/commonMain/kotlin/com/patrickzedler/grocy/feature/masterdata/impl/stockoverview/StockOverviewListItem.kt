@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)

package com.patrickzedler.grocy.feature.masterdata.impl.stockoverview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.ListItemOverflowMenu
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.StatusColor
import com.patrickzedler.grocy.core.design.component.StatusColors
import com.patrickzedler.grocy.core.design.component.SwipeAction
import com.patrickzedler.grocy.core.design.component.SwipeActionBox
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.rememberStatusColors
import com.patrickzedler.grocy.core.model.stock.DueStatus
import com.patrickzedler.grocy.core.model.stock.StockOverviewField
import com.patrickzedler.grocy.core.model.text.formatCurrency
import com.patrickzedler.grocy.core.model.text.formatDate
import com.patrickzedler.grocy.core.model.text.formatRelativeTime
import com.patrickzedler.grocy.core.model.text.formatRelativeTimeLabel
import com.patrickzedler.grocy.core.model.text.timeSpanBetween
import com.patrickzedler.grocy.core.model.text.todayEpochDay
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.data_never_overdue
import com.patrickzedler.grocy.core.resources.ic_rounded_add_shopping_cart
import com.patrickzedler.grocy.core.resources.ic_rounded_box
import com.patrickzedler.grocy.core.resources.ic_rounded_calculate
import com.patrickzedler.grocy.core.resources.ic_rounded_category
import com.patrickzedler.grocy.core.resources.ic_rounded_dataset
import com.patrickzedler.grocy.core.resources.ic_rounded_functions
import com.patrickzedler.grocy.core.resources.ic_rounded_inventory_2
import com.patrickzedler.grocy.core.resources.ic_rounded_local_fire_department
import com.patrickzedler.grocy.core.resources.ic_rounded_location_on
import com.patrickzedler.grocy.core.resources.ic_rounded_payments
import com.patrickzedler.grocy.core.resources.ic_rounded_restaurant
import com.patrickzedler.grocy.core.resources.ic_rounded_sell
import com.patrickzedler.grocy.core.resources.ic_rounded_shopping_cart
import com.patrickzedler.grocy.core.resources.ic_rounded_storefront
import com.patrickzedler.grocy.core.resources.product_edit
import com.patrickzedler.grocy.core.resources.product_overview_add_to_shopping_list
import com.patrickzedler.grocy.core.resources.product_overview_opened
import com.patrickzedler.grocy.core.resources.product_overview_with_sub_products
import com.patrickzedler.grocy.core.resources.products_has_picture
import com.patrickzedler.grocy.core.resources.stock_entry_due_soon
import com.patrickzedler.grocy.core.resources.stock_entry_expired
import com.patrickzedler.grocy.core.resources.stock_entry_overdue
import com.patrickzedler.grocy.core.resources.stock_entry_price
import com.patrickzedler.grocy.core.resources.stock_overview_below_min_stock
import com.patrickzedler.grocy.core.resources.stock_overview_consume
import com.patrickzedler.grocy.core.resources.stock_overview_consume_all
import com.patrickzedler.grocy.core.resources.stock_overview_consume_spoiled
import com.patrickzedler.grocy.core.resources.stock_overview_field_value
import com.patrickzedler.grocy.core.resources.stock_overview_on_shopping_list
import com.patrickzedler.grocy.core.resources.stock_overview_open
import com.patrickzedler.grocy.core.resources.stock_overview_search_recipes
import com.patrickzedler.grocy.feature.masterdata.impl.products.ProductThumbnail
import com.patrickzedler.grocy.feature.masterdata.impl.stock.EntryPrice
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** What the rows of the stock overview do, Grocy's buttons and menu of a row. */
internal class StockOverviewItemActions(
    /** Opens the product overview, like a click on Grocy's product name. */
    val onClick: (StockOverviewItem) -> Unit,
    val onConsume: (StockOverviewItem) -> Unit,
    val onOpen: (StockOverviewItem) -> Unit,
    val onConsumeAll: (StockOverviewItem) -> Unit,
    val onConsumeSpoiled: (StockOverviewItem) -> Unit,
    val onAddToShoppingList: (StockOverviewItem) -> Unit,
    val onSearchRecipes: (StockOverviewItem) -> Unit,
    val onEditProduct: (StockOverviewItem) -> Unit,
)

/** How a row looks: the shown fields and what the server's features offer. */
@Immutable
internal class StockOverviewItemStyle(
    val fields: List<StockOverviewField>,
    val currency: String,
    val priceDecimals: Int,
    val canOpen: Boolean,
    val hasShoppingList: Boolean,
    val hasRecipes: Boolean,
)

/**
 * A row of the stock overview: the product's name as title, its shown fields as chips below,
 * colored like Grocy's row highlighting (blue below the min. stock amount, yellow due soon,
 * red overdue or expired). Swiping towards the end consumes, towards the start opens, like
 * Grocy's buttons in front of the row; both are also in the menu and accessibility actions.
 */
@Composable
internal fun StockOverviewListItem(
    item: StockOverviewItem,
    style: StockOverviewItemStyle,
    index: Int,
    count: Int,
    actions: StockOverviewItemActions,
    loadThumbnail: suspend (fileName: String) -> ByteArray?,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val consumeText = stringResource(Res.string.stock_overview_consume, item.quickConsume)
    val openText = stringResource(Res.string.stock_overview_open, item.quickOpen)
    val canOpen = style.canOpen && item.canOpen
    val pictureDescription = stringResource(Res.string.products_has_picture)
    val showsPicture = StockOverviewField.PICTURE in style.fields && item.pictureFileName != null

    SwipeActionBox(
        startAction = SwipeAction(
            icon = painterResource(Res.drawable.ic_rounded_restaurant),
            enabled = item.canConsume,
        ) { actions.onConsume(item) },
        endAction = if (style.canOpen) {
            // Another color than consuming, so the two sides are told apart at a glance
            SwipeAction(
                icon = painterResource(Res.drawable.ic_rounded_box),
                enabled = canOpen,
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
            ) { actions.onOpen(item) }
        } else {
            null
        },
        modifier = modifier,
    ) {
        SegmentedListItem(
            onClick = {
                haptic.confirm()
                actions.onClick(item)
            },
            shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
            colors = SegmentedListDefaults.colors(),
            modifier = Modifier
                .fillMaxWidth()
                .clickCursor()
                .semantics {
                    customActions = listOfNotNull(
                        CustomAccessibilityAction(consumeText) {
                            actions.onConsume(item)
                            true
                        }.takeIf { item.canConsume },
                        CustomAccessibilityAction(openText) {
                            actions.onOpen(item)
                            true
                        }.takeIf { canOpen },
                    )
                },
            verticalAlignment = Alignment.CenterVertically,
            supportingContent = {
                FieldChips(item = item, style = style, pictureDescription = pictureDescription)
            },
            // Button and picture bring enough space of their own, unlike icons and switches
            trailingContent = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (showsPicture) {
                        ProductThumbnail(item.pictureFileName, loadThumbnail)
                    }
                    ListItemOverflowMenu(
                        groups = menuGroups(item, style, actions, consumeText, openText),
                    )
                }
            },
        ) {
            Title(item)
        }
    }
}

/** The name, with Grocy's cart icon in blue after it while the product is on a shopping list. */
@Composable
private fun Title(item: StockOverviewItem) {
    if (!item.onShoppingList) {
        Text(item.name)
        return
    }
    val onShoppingList = stringResource(Res.string.stock_overview_on_shopping_list)
    Row(
        horizontalArrangement = Arrangement.spacedBy(ShoppingListIconGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(item.name, modifier = Modifier.weight(1f, fill = false))
        Icon(
            painter = painterResource(Res.drawable.ic_rounded_shopping_cart),
            contentDescription = null,
            tint = rememberStatusColors().info.color,
            modifier = Modifier
                .size(ShoppingListIconSize)
                // As text like the chips: a description would replace the item's name for
                // screen readers, which read a merged item's descriptions instead of its texts
                .clearAndSetSemantics { text = AnnotatedString(onShoppingList) },
        )
    }
}

/** Grocy's actions of a row: the bookings first, then what leads elsewhere. */
@Composable
private fun menuGroups(
    item: StockOverviewItem,
    style: StockOverviewItemStyle,
    actions: StockOverviewItemActions,
    consumeText: String,
    openText: String,
): List<List<AppBarMenuItem>> = listOf(
    listOfNotNull(
        AppBarMenuItem(consumeText, enabled = item.canConsume) { actions.onConsume(item) },
        AppBarMenuItem(openText, enabled = item.canOpen) { actions.onOpen(item) }
            .takeIf { style.canOpen },
        AppBarMenuItem(
            stringResource(Res.string.stock_overview_consume_all),
            enabled = item.canConsumeAll,
        ) { actions.onConsumeAll(item) },
        AppBarMenuItem(
            stringResource(Res.string.stock_overview_consume_spoiled, item.quickConsume),
            enabled = item.canConsume,
        ) { actions.onConsumeSpoiled(item) },
    ),
    listOfNotNull(
        AppBarMenuItem(stringResource(Res.string.product_overview_add_to_shopping_list)) {
            actions.onAddToShoppingList(item)
        }.takeIf { style.hasShoppingList },
        AppBarMenuItem(stringResource(Res.string.stock_overview_search_recipes)) {
            actions.onSearchRecipes(item)
        }.takeIf { style.hasRecipes },
    ),
    listOf(
        AppBarMenuItem(stringResource(Res.string.product_edit)) { actions.onEditProduct(item) },
    ),
)

/** A field shown as chip: its text, how screen readers name it, its icon and colors. */
private class FieldChip(
    val text: String,
    val description: String,
    val icon: DrawableResource? = null,
    val status: StatusColor? = null,
)

/**
 * The shown fields below the name, wrapping into more rows; the description as text above them.
 * A new layout for other chips: Material sizes the item by the baselines of this slot, which a
 * reused layout kept from its former rows.
 */
@Composable
private fun FieldChips(
    item: StockOverviewItem,
    style: StockOverviewItemStyle,
    pictureDescription: String,
) {
    val chips = fieldChips(item, style)
    val description = item.description.takeIf { StockOverviewField.DESCRIPTION in style.fields }
    val picture = pictureDescription
        .takeIf { StockOverviewField.PICTURE in style.fields && item.pictureFileName != null }
    key(chips.map { it.text }, description) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .padding(top = 4.dp)
                .semantics { picture?.let { text = AnnotatedString(it) } },
        ) {
            description?.let {
                Text(it, maxLines = DESCRIPTION_LINES, overflow = TextOverflow.Ellipsis)
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

@Composable
private fun fieldChips(item: StockOverviewItem, style: StockOverviewItemStyle): List<FieldChip> {
    val statusColors = rememberStatusColors()
    val today = remember { todayEpochDay() }
    fun price(price: EntryPrice?) = price?.let {
        formatCurrency(it.price, style.currency, style.priceDecimals) to it.unit
    }
    val chips = mutableListOf<FieldChip>()
    for (field in style.fields) {
        val label = fieldLabel(field, "")
        when (field) {
            StockOverviewField.AMOUNT -> chips += amountChips(item, statusColors.info)
            StockOverviewField.NEXT_DUE_DATE -> dueChip(item, today, statusColors)?.let {
                chips += it
            }
            StockOverviewField.PRODUCT_GROUP -> item.productGroupName?.let {
                chips += FieldChip(it, labelled(label, it), Res.drawable.ic_rounded_category)
            }
            StockOverviewField.VALUE -> item.value?.let {
                val text = formatCurrency(it, style.currency, style.priceDecimals)
                chips += FieldChip(text, labelled(label, text), Res.drawable.ic_rounded_payments)
            }
            StockOverviewField.CALORIES_PER_UNIT -> item.caloriesPerUnit?.let {
                val text = stringResource(Res.string.stock_entry_price, it, item.stockUnitName)
                chips += FieldChip(text, text, Res.drawable.ic_rounded_local_fire_department)
            }
            StockOverviewField.CALORIES -> item.calories?.let {
                chips += FieldChip(it, it, Res.drawable.ic_rounded_local_fire_department)
            }
            StockOverviewField.LAST_PURCHASED -> item.lastPurchasedDay?.let { day ->
                val span = timeSpanBetween(today, day)
                val relative = formatRelativeTime(span)
                chips += FieldChip(
                    text = formatRelativeTimeLabel(span),
                    description = labelled(label, "${formatDate(day)}, $relative"),
                    icon = Res.drawable.ic_rounded_add_shopping_cart,
                )
            }
            StockOverviewField.LAST_PRICE -> price(item.lastPrice)?.let { (value, unit) ->
                val text = stringResource(Res.string.stock_entry_price, value, unit)
                chips += FieldChip(text, labelled(label, text), Res.drawable.ic_rounded_sell)
            }
            StockOverviewField.MIN_STOCK_AMOUNT -> item.minStockAmount?.let {
                chips += FieldChip(it, labelled(label, it), Res.drawable.ic_rounded_inventory_2)
            }
            StockOverviewField.PARENT_PRODUCT -> item.parentProductName?.let {
                chips += FieldChip(it, labelled(label, it), Res.drawable.ic_rounded_dataset)
            }
            StockOverviewField.DEFAULT_LOCATION -> item.defaultLocationName?.let {
                chips += FieldChip(it, labelled(label, it), Res.drawable.ic_rounded_location_on)
            }
            StockOverviewField.AVERAGE_PRICE -> price(item.averagePrice)?.let { (value, unit) ->
                val text = stringResource(Res.string.stock_entry_price, value, unit)
                chips += FieldChip(text, labelled(label, text), Res.drawable.ic_rounded_calculate)
            }
            StockOverviewField.DEFAULT_STORE -> item.defaultStoreName?.let {
                chips += FieldChip(it, labelled(label, it), Res.drawable.ic_rounded_storefront)
            }
            // The name is the title, the description a text, the picture a thumbnail
            StockOverviewField.NAME,
            StockOverviewField.DESCRIPTION,
            StockOverviewField.PICTURE,
            -> Unit
        }
    }
    return chips
}

/** Own amount and the one with sub products, like Grocy's column. */
@Composable
private fun amountChips(item: StockOverviewItem, belowMinStock: StatusColor): List<FieldChip> {
    val below = stringResource(Res.string.stock_overview_below_min_stock)
    val status = belowMinStock.takeIf { item.isBelowMinStock }
    val opened = item.amountOpened?.let { stringResource(Res.string.product_overview_opened, it) }
    val openedAggregated = item.amountOpenedAggregated
        ?.let { stringResource(Res.string.product_overview_opened, it) }
    fun withOpened(amount: String, opened: String?) = opened?.let { "$amount ($it)" } ?: amount
    return listOfNotNull(
        item.amount?.let { amount ->
            val text = withOpened(amount, opened)
            FieldChip(
                text = text,
                description = if (status != null) "$text, $below" else text,
                status = status,
            )
        },
        item.amountAggregated?.let { amount ->
            val text = withOpened(amount, openedAggregated)
            FieldChip(
                text = text,
                description = stringResource(Res.string.product_overview_with_sub_products, text),
                // Grocy's sigma before the amount including sub products
                icon = Res.drawable.ic_rounded_functions,
                // Products without stock of their own show the status here
                status = status.takeIf { item.amount == null },
            )
        },
    )
}

/** The next due date relative to today, colored by its status like Grocy's rows. */
@Composable
private fun dueChip(
    item: StockOverviewItem,
    today: Long,
    colors: StatusColors,
): FieldChip? {
    val label = fieldLabel(StockOverviewField.NEXT_DUE_DATE, "")
    if (item.neverOverdue) {
        val text = stringResource(Res.string.data_never_overdue)
        return FieldChip(text, labelled(label, text))
    }
    val day = item.dueDay ?: return null
    val span = timeSpanBetween(today, day)
    val relative = formatRelativeTime(span)
    val statusText = when (item.dueStatus) {
        DueStatus.DUE_SOON -> stringResource(Res.string.stock_entry_due_soon)
        DueStatus.OVERDUE -> stringResource(Res.string.stock_entry_overdue)
        DueStatus.EXPIRED -> stringResource(Res.string.stock_entry_expired)
        null -> null
    }
    return FieldChip(
        text = formatRelativeTimeLabel(span),
        description = labelled(
            label,
            listOfNotNull(formatDate(day), relative, statusText).joinToString(", "),
        ),
        status = when (item.dueStatus) {
            DueStatus.DUE_SOON -> colors.warning
            DueStatus.OVERDUE, DueStatus.EXPIRED -> colors.critical
            null -> null
        },
    )
}

@Composable
private fun labelled(label: String, value: String) =
    stringResource(Res.string.stock_overview_field_value, label, value)

/**
 * A chip that only shows a value: no outline or elevation, surfaceContainer when neutral, a
 * status color otherwise.
 */
@Composable
private fun FieldChipView(chip: FieldChip) {
    val container = chip.status?.container ?: MaterialTheme.colorScheme.surfaceContainer
    val content = chip.status?.content ?: MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = MaterialTheme.shapes.small,
        color = container,
        contentColor = content,
        // As text, not description: the item merges them, and a description would hide the
        // product's name from screen readers
        modifier = Modifier.clearAndSetSemantics { text = AnnotatedString(chip.description) },
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = ChipHeight)
                .padding(start = if (chip.icon != null) 8.dp else 12.dp, end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            chip.icon?.let {
                Icon(
                    painter = painterResource(it),
                    contentDescription = null,
                    modifier = Modifier.size(ChipIconSize),
                    tint = content,
                )
            }
            Text(
                text = chip.text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private const val DESCRIPTION_LINES = 2

private val ShoppingListIconSize = 18.dp
private val ShoppingListIconGap = 6.dp

// Material's chip height and icon size
private val ChipHeight = 32.dp
private val ChipIconSize = 18.dp
