@file:OptIn(ExperimentalLayoutApi::class)

package com.patrickzedler.grocy.feature.shoppinglist.impl

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.LeadingTrailingContentWrapper
import com.patrickzedler.grocy.core.design.component.ListItemOverflowMenu
import com.patrickzedler.grocy.core.design.component.SwipeAction
import com.patrickzedler.grocy.core.design.component.SwipeActionBox
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.rememberStatusColors
import com.patrickzedler.grocy.core.forms.FieldChip
import com.patrickzedler.grocy.core.forms.FieldChipView
import com.patrickzedler.grocy.core.forms.ListDetailItem
import com.patrickzedler.grocy.core.forms.labelled
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListField
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListItemSync
import com.patrickzedler.grocy.core.model.text.formatCurrency
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.data_amount
import com.patrickzedler.grocy.core.resources.data_product_group
import com.patrickzedler.grocy.core.resources.ic_rounded_barcode
import com.patrickzedler.grocy.core.resources.ic_rounded_category
import com.patrickzedler.grocy.core.resources.ic_rounded_check
import com.patrickzedler.grocy.core.resources.ic_rounded_inventory_2
import com.patrickzedler.grocy.core.resources.ic_rounded_label
import com.patrickzedler.grocy.core.resources.ic_rounded_payments
import com.patrickzedler.grocy.core.resources.ic_rounded_sell
import com.patrickzedler.grocy.core.resources.ic_rounded_storefront
import com.patrickzedler.grocy.core.resources.ic_rounded_sync
import com.patrickzedler.grocy.core.resources.ic_rounded_sync_problem
import com.patrickzedler.grocy.core.resources.product_barcodes
import com.patrickzedler.grocy.core.resources.product_overview
import com.patrickzedler.grocy.core.resources.products_sort_store
import com.patrickzedler.grocy.core.resources.shopping_list_add_to_stock
import com.patrickzedler.grocy.core.resources.shopping_list_delete_item
import com.patrickzedler.grocy.core.resources.shopping_list_edit_item
import com.patrickzedler.grocy.core.resources.shopping_list_item_not_sent
import com.patrickzedler.grocy.core.resources.shopping_list_item_sending
import com.patrickzedler.grocy.core.resources.shopping_list_last_price_total
import com.patrickzedler.grocy.core.resources.shopping_list_last_price_unit
import com.patrickzedler.grocy.core.resources.shopping_list_mark_done
import com.patrickzedler.grocy.core.resources.shopping_list_mark_undone
import com.patrickzedler.grocy.core.resources.stock_entry_price
import com.patrickzedler.grocy.core.resources.stock_overview_below_min_stock
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** What the rows of the shopping list do, Grocy's buttons of a row and its product's card. */
internal class ShoppingListRowActions(
    /** Grocy's edit button; the item's form. */
    val onClick: (ShoppingListRow) -> Unit,
    val onToggleDone: (ShoppingListRow) -> Unit,
    val onDelete: (ShoppingListRow) -> Unit,
    val onAddToStock: (ShoppingListRow) -> Unit,
    /** Grocy's product card, opened by its product name. */
    val onProductOverview: (productId: Int) -> Unit,
)

/** How a row looks: the shown fields and what the server's features offer. */
@Immutable
internal class ShoppingListRowStyle(
    val fields: List<ShoppingListField>,
    val currency: String,
    val priceDecimals: Int,
    val hasStock: Boolean,
)

/**
 * A row of the shopping list: a checkbox checks the item off (offline too), the product (or
 * the note of an item without product) as title, struck through once done, its note and the
 * shown fields as chips below, blue amount below the min. stock amount like Grocy's row. A tap
 * edits the item. Swiping towards the end checks it off, towards the start adds it to the
 * stock, like Grocy's first and last button; both are also in the menu and the accessibility
 * actions. A check off the server has not taken yet ([sync]) shows an icon after the title.
 *
 * @param selected beside the item's form: whether this one is shown there, null otherwise.
 */
@Composable
internal fun ShoppingListRowItem(
    row: ShoppingListRow,
    style: ShoppingListRowStyle,
    selected: Boolean?,
    sync: ShoppingListItemSync?,
    index: Int,
    count: Int,
    actions: ShoppingListRowActions,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val doneText = stringResource(
        if (row.done) Res.string.shopping_list_mark_undone else Res.string.shopping_list_mark_done,
    )
    val addToStockText = stringResource(Res.string.shopping_list_add_to_stock)
    val canAddToStock = style.hasStock && row.purchase != null

    SwipeActionBox(
        startAction = SwipeAction(icon = painterResource(Res.drawable.ic_rounded_check)) {
            actions.onToggleDone(row)
        },
        endAction = if (style.hasStock) {
            // Another color than checking off, so the two sides are told apart at a glance
            SwipeAction(
                icon = painterResource(Res.drawable.ic_rounded_inventory_2),
                enabled = canAddToStock,
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
            ) { actions.onAddToStock(row) }
        } else {
            null
        },
        modifier = modifier,
    ) {
        ListDetailItem(
            selected = selected,
            onClick = { actions.onClick(row) },
            index = index,
            count = count,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    customActions = listOfNotNull(
                        CustomAccessibilityAction(doneText) {
                            actions.onToggleDone(row)
                            true
                        },
                        CustomAccessibilityAction(addToStockText) {
                            actions.onAddToStock(row)
                            true
                        }.takeIf { canAddToStock },
                    )
                },
            // Keeps the checkbox clear of the top edge of items aligned to the top
            leadingContent = {
                LeadingTrailingContentWrapper {
                    Checkbox(
                        checked = row.done,
                        onCheckedChange = {
                            haptic.performHapticFeedback(
                                if (it) {
                                    HapticFeedbackType.ToggleOn
                                } else {
                                    HapticFeedbackType.ToggleOff
                                },
                            )
                            actions.onToggleDone(row)
                        },
                        modifier = Modifier
                            .clickCursor()
                            .semantics { contentDescription = doneText },
                    )
                }
            },
            supportingContent = supporting(row, style),
            trailingContent = {
                ListItemOverflowMenu(
                    groups = menuGroups(
                        row = row,
                        actions = actions,
                        doneText = doneText,
                        addToStockText = addToStockText,
                        canAddToStock = canAddToStock,
                        hasStock = style.hasStock,
                    ),
                )
            },
        ) {
            Title(row, sync)
        }
    }
}

/**
 * The product's name or the note, struck through and muted once done like Grocy's row; after
 * it the state of a check off still to send.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Title(row: ShoppingListRow, sync: ShoppingListItemSync?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = row.title,
            modifier = Modifier.weight(1f, fill = false),
            // Grocy writes notes in italics
            fontStyle = if (row.productId == null) FontStyle.Italic else null,
            textDecoration = if (row.done) TextDecoration.LineThrough else null,
            color = if (row.done) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                LocalContentColor.current
            },
        )
        Crossfade(
            targetState = shownSync(sync),
            animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        ) { shown ->
            shown?.let { SyncIcon(it) }
        }
    }
}

/**
 * [sync] as it shows: sending only once it takes a moment, so a check off the server takes
 * right away does not flash an icon; a failure shows at once.
 */
@Composable
private fun shownSync(sync: ShoppingListItemSync?): ShoppingListItemSync? {
    var shown by remember { mutableStateOf(sync.takeIf { it == ShoppingListItemSync.FAILED }) }
    LaunchedEffect(sync) {
        if (sync == ShoppingListItemSync.SENDING && shown == null) delay(SendingIconDelay)
        shown = sync
    }
    return shown
}

/** Being sent in the muted color, failed in the error color, read as text like the chips. */
@Composable
private fun SyncIcon(sync: ShoppingListItemSync) {
    val (icon, tint, label) = when (sync) {
        ShoppingListItemSync.SENDING -> Triple(
            Res.drawable.ic_rounded_sync,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Res.string.shopping_list_item_sending,
        )
        ShoppingListItemSync.FAILED -> Triple(
            Res.drawable.ic_rounded_sync_problem,
            MaterialTheme.colorScheme.error,
            Res.string.shopping_list_item_not_sent,
        )
    }
    val description = stringResource(label)
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        tint = tint,
        modifier = Modifier
            .padding(start = SyncIconGap)
            .size(SyncIconSize)
            // A description would replace the item's name for screen readers
            .clearAndSetSemantics { text = AnnotatedString(description) },
    )
}

/** Grocy's buttons of a row: done and edit, then the stock, then delete. */
@Composable
private fun menuGroups(
    row: ShoppingListRow,
    actions: ShoppingListRowActions,
    doneText: String,
    addToStockText: String,
    canAddToStock: Boolean,
    hasStock: Boolean,
): List<List<AppBarMenuItem>> = listOf(
    listOfNotNull(
        row.productId?.let { productId ->
            AppBarMenuItem(stringResource(Res.string.product_overview)) {
                actions.onProductOverview(productId)
            }
        },
    ),
    listOf(
        AppBarMenuItem(doneText) { actions.onToggleDone(row) },
        AppBarMenuItem(stringResource(Res.string.shopping_list_edit_item)) {
            actions.onClick(row)
        },
    ),
    listOfNotNull(
        // Disabled for items without product, like Grocy's button
        AppBarMenuItem(addToStockText, enabled = canAddToStock) { actions.onAddToStock(row) }
            .takeIf { hasStock },
    ),
    listOf(
        AppBarMenuItem(stringResource(Res.string.shopping_list_delete_item)) {
            actions.onDelete(row)
        },
    ),
)

/**
 * The note in italics and the shown fields as chips below the title; a new layout for other
 * chips, Material sizes the item by the baselines of this slot.
 */
@Composable
private fun supporting(
    row: ShoppingListRow,
    style: ShoppingListRowStyle,
): (@Composable () -> Unit)? {
    val chips = fieldChips(row, style)
    if (row.note == null && chips.isEmpty()) return null
    return {
        key(chips.map { it.text }, row.note) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                row.note?.let {
                    Text(
                        text = it,
                        fontStyle = FontStyle.Italic,
                        maxLines = NOTE_LINES,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (chips.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        itemVerticalAlignment = Alignment.CenterVertically,
                    ) {
                        chips.forEach { FieldChipView(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun fieldChips(row: ShoppingListRow, style: ShoppingListRowStyle): List<FieldChip> {
    val info = rememberStatusColors().info
    val below = stringResource(Res.string.stock_overview_below_min_stock)
    val chips = mutableListOf<FieldChip>()
    fun currency(value: Double) = formatCurrency(value, style.currency, style.priceDecimals)
    for (field in style.fields) {
        when (field) {
            ShoppingListField.AMOUNT -> {
                val label = stringResource(Res.string.data_amount)
                chips += if (row.isBelowMinStock) {
                    FieldChip(row.amount, "${labelled(label, row.amount)}, $below", status = info)
                } else {
                    FieldChip(row.amount, labelled(label, row.amount))
                }
            }
            ShoppingListField.PRODUCT_GROUP -> row.productGroupName?.let {
                val label = stringResource(Res.string.data_product_group)
                chips += FieldChip(it, labelled(label, it), Res.drawable.ic_rounded_category)
            }
            ShoppingListField.LAST_PRICE_UNIT -> row.lastPriceUnit?.let { price ->
                val text = stringResource(
                    Res.string.stock_entry_price,
                    currency(price.price),
                    price.unit,
                )
                val label = stringResource(Res.string.shopping_list_last_price_unit)
                chips += FieldChip(text, labelled(label, text), Res.drawable.ic_rounded_sell)
            }
            ShoppingListField.LAST_PRICE_TOTAL -> row.lastPriceTotal?.let {
                val text = currency(it)
                val label = stringResource(Res.string.shopping_list_last_price_total)
                chips += FieldChip(text, labelled(label, text), Res.drawable.ic_rounded_payments)
            }
            ShoppingListField.DEFAULT_STORE -> row.defaultStoreName?.let {
                val label = stringResource(Res.string.products_sort_store)
                chips += FieldChip(it, labelled(label, it), Res.drawable.ic_rounded_storefront)
            }
            ShoppingListField.BARCODES -> {
                val label = stringResource(Res.string.product_barcodes)
                row.barcodes.forEach {
                    chips += FieldChip(it, labelled(label, it), Res.drawable.ic_rounded_barcode)
                }
            }
            ShoppingListField.USERFIELDS -> row.userfields.forEach { (caption, value) ->
                val text = if (caption == value) value else labelled(caption, value)
                chips += FieldChip(text, text, Res.drawable.ic_rounded_label)
            }
        }
    }
    return chips
}

private const val NOTE_LINES = 3

// Like the cart icon after a product's name in the stock overview
private val SyncIconSize = 18.dp
private val SyncIconGap = 6.dp

private val SendingIconDelay = 500.milliseconds
