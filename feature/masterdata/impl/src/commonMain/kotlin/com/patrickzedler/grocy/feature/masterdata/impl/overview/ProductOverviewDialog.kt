@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.patrickzedler.grocy.feature.masterdata.impl.overview

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ButtonGroupMenuState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.data.util.RefreshError
import com.patrickzedler.grocy.core.data.util.RefreshState
import com.patrickzedler.grocy.core.design.GrocyTheme
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.HapticTooltipBox
import com.patrickzedler.grocy.core.design.component.LeadingTrailingContentWrapper
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialog
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialogContent
import com.patrickzedler.grocy.core.design.component.SegmentedContainer
import com.patrickzedler.grocy.core.design.component.SegmentedGroup
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.TonalOverflowMenu
import com.patrickzedler.grocy.core.design.component.animatedItem
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.smallIconButtonSize
import com.patrickzedler.grocy.core.model.text.durationOf
import com.patrickzedler.grocy.core.model.text.formatCurrency
import com.patrickzedler.grocy.core.model.text.formatDate
import com.patrickzedler.grocy.core.model.text.formatDuration
import com.patrickzedler.grocy.core.model.text.formatPercent
import com.patrickzedler.grocy.core.model.text.formatRelativeTime
import com.patrickzedler.grocy.core.model.text.timeSpanBetween
import com.patrickzedler.grocy.core.model.text.todayEpochDay
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_close
import com.patrickzedler.grocy.core.resources.action_more_options
import com.patrickzedler.grocy.core.resources.action_retry
import com.patrickzedler.grocy.core.resources.data_load_failed
import com.patrickzedler.grocy.core.resources.data_offline
import com.patrickzedler.grocy.core.resources.grocycode_download_of
import com.patrickzedler.grocy.core.resources.grocycode_print_of
import com.patrickzedler.grocy.core.resources.grocycode_product
import com.patrickzedler.grocy.core.resources.ic_rounded_add_shopping_cart
import com.patrickzedler.grocy.core.resources.ic_rounded_calculate
import com.patrickzedler.grocy.core.resources.ic_rounded_delete
import com.patrickzedler.grocy.core.resources.ic_rounded_edit
import com.patrickzedler.grocy.core.resources.ic_rounded_history
import com.patrickzedler.grocy.core.resources.ic_rounded_hourglass_empty
import com.patrickzedler.grocy.core.resources.ic_rounded_inventory_2
import com.patrickzedler.grocy.core.resources.ic_rounded_label
import com.patrickzedler.grocy.core.resources.ic_rounded_location_on
import com.patrickzedler.grocy.core.resources.ic_rounded_more_vert
import com.patrickzedler.grocy.core.resources.ic_rounded_no_photography
import com.patrickzedler.grocy.core.resources.ic_rounded_payments
import com.patrickzedler.grocy.core.resources.ic_rounded_receipt_long
import com.patrickzedler.grocy.core.resources.ic_rounded_sell
import com.patrickzedler.grocy.core.resources.ic_rounded_shopping_cart
import com.patrickzedler.grocy.core.resources.product_edit
import com.patrickzedler.grocy.core.resources.product_location
import com.patrickzedler.grocy.core.resources.product_overview
import com.patrickzedler.grocy.core.resources.product_overview_add_to_shopping_list
import com.patrickzedler.grocy.core.resources.product_overview_average_price
import com.patrickzedler.grocy.core.resources.product_overview_average_shelf_life
import com.patrickzedler.grocy.core.resources.product_overview_last_price
import com.patrickzedler.grocy.core.resources.product_overview_last_purchased
import com.patrickzedler.grocy.core.resources.product_overview_last_used
import com.patrickzedler.grocy.core.resources.product_overview_never
import com.patrickzedler.grocy.core.resources.product_overview_no_price_history
import com.patrickzedler.grocy.core.resources.product_overview_offline
import com.patrickzedler.grocy.core.resources.product_overview_opened
import com.patrickzedler.grocy.core.resources.product_overview_price_history
import com.patrickzedler.grocy.core.resources.product_overview_price_per_unit
import com.patrickzedler.grocy.core.resources.product_overview_product_name
import com.patrickzedler.grocy.core.resources.product_overview_show_less
import com.patrickzedler.grocy.core.resources.product_overview_show_more
import com.patrickzedler.grocy.core.resources.product_overview_spoil_rate
import com.patrickzedler.grocy.core.resources.product_overview_stock_amount
import com.patrickzedler.grocy.core.resources.product_overview_stock_value
import com.patrickzedler.grocy.core.resources.product_overview_unknown
import com.patrickzedler.grocy.core.resources.product_overview_unlimited
import com.patrickzedler.grocy.core.resources.product_overview_with_sub_products
import com.patrickzedler.grocy.core.resources.product_picture
import com.patrickzedler.grocy.core.resources.product_picture_load_failed
import com.patrickzedler.grocy.core.resources.product_stock_entries
import com.patrickzedler.grocy.core.resources.product_stock_journal
import com.patrickzedler.grocy.core.resources.product_stock_journal_summary
import com.patrickzedler.grocy.core.resources.stock_consume
import com.patrickzedler.grocy.core.resources.stock_inventory
import com.patrickzedler.grocy.core.resources.stock_purchase
import com.patrickzedler.grocy.core.resources.stock_transfer
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Grocy's "Product overview" as dialog: its actions on top, the product's name, then every value
 * the web frontend shows, its picture and the price history. [name] is the product's name from
 * the list, so it is there in the first frame.
 */
@Composable
internal fun ProductOverviewDialog(
    productId: Int,
    name: String,
    viewModel: ProductOverviewViewModel,
    actions: OverviewActions,
    onDismissRequest: () -> Unit,
) {
    // Right after opening it may still be the previously opened product's
    val state = viewModel.state.collectAsStateWithLifecycle().value
        ?.takeIf { it.productId == productId }
    val refresh by viewModel.refresh.collectAsStateWithLifecycle()

    ScrollableAlertDialog(
        onDismissRequest = onDismissRequest,
        paneTitle = stringResource(Res.string.product_overview),
    ) {
        ProductOverviewContent(
            name = name,
            state = state,
            refresh = refresh,
            loadPicture = viewModel::picture,
            onRetryClick = viewModel::refresh,
            actions = actions.of(productId),
            onCloseClick = onDismissRequest,
        )
    }
}

@Composable
private fun ProductOverviewContent(
    name: String,
    state: ProductOverviewUiState?,
    refresh: RefreshState,
    loadPicture: suspend (fileName: String) -> ByteArray?,
    onRetryClick: () -> Unit,
    actions: ProductActions,
    onCloseClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val details = state?.details

    ScrollableAlertDialogContent(
        title = { Text(stringResource(Res.string.product_overview)) },
        confirmButton = {
            TextButton(
                onClick = {
                    haptic.confirm()
                    onCloseClick()
                },
                modifier = Modifier.clickCursor(),
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(stringResource(Res.string.action_close))
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SegmentedListDefaults.GroupSpacing),
        ) {
            ActionGroup(state = state, actions = actions)
            if (details != null && refresh.error != null) {
                Text(
                    text = stringResource(
                        if (refresh.error == RefreshError.OFFLINE) {
                            Res.string.data_offline
                        } else {
                            Res.string.data_load_failed
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            val nameRow = OverviewRow(
                title = stringResource(Res.string.product_overview_product_name),
                lines = listOf(state?.name ?: name),
                icon = Res.drawable.ic_rounded_label,
                // Downloads with stored values show here, the values stay readable meanwhile
                isLoading = refresh.isRefreshing && details != null,
            )
            if (state == null) {
                SegmentedGroup { OverviewItem(row = nameRow, index = 0, count = 1) }
                return@Column
            }
            if (state.description.isNotEmpty()) {
                DescriptionItem(state.description)
            }
            OverviewGroups(state, details, nameRow)
            when {
                details != null -> Unit
                refresh.isRefreshing -> Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingIndicator()
                }

                else -> refresh.error?.let { ErrorItem(error = it, onRetryClick = onRetryClick) }
            }
            state.pictureFileName?.let { PictureItem(fileName = it, load = loadPicture) }
            if (state.showPrices && details != null) {
                PriceHistoryGroup(state, details.priceHistory)
            }
        }
    }
}

/** What the product overview's actions do with the product [of] gets. */
internal class OverviewActions(
    val onEditClick: (productId: Int) -> Unit,
    val onAddToShoppingListClick: (productId: Int) -> Unit,
    val onStockEntriesClick: (productId: Int) -> Unit,
    val onStockJournalClick: (productId: Int) -> Unit,
    val onStockJournalSummaryClick: (productId: Int) -> Unit,
    val onGrocycodeDownload: (productId: Int, twoDimensional: Boolean) -> Unit,
    val onGrocycodePrint: (productId: Int) -> Unit,
) {
    fun of(productId: Int) = ProductActions(
        onEditClick = { onEditClick(productId) },
        onAddToShoppingListClick = { onAddToShoppingListClick(productId) },
        onStockEntriesClick = { onStockEntriesClick(productId) },
        onStockJournalClick = { onStockJournalClick(productId) },
        onStockJournalSummaryClick = { onStockJournalSummaryClick(productId) },
        onGrocycodeDownload = { onGrocycodeDownload(productId, it) },
        onGrocycodePrint = { onGrocycodePrint(productId) },
    )
}

/** [OverviewActions] of the shown product. */
internal class ProductActions(
    val onEditClick: () -> Unit,
    val onAddToShoppingListClick: () -> Unit,
    val onStockEntriesClick: () -> Unit,
    val onStockJournalClick: () -> Unit,
    val onStockJournalSummaryClick: () -> Unit,
    val onGrocycodeDownload: (twoDimensional: Boolean) -> Unit,
    val onGrocycodePrint: () -> Unit,
)

/**
 * The buttons of Grocy's card header as button group, the pressed one widens: editing and the
 * shopping list as wide buttons, the stock pages, Grocy's bookings and the grocycode in the
 * menu of a narrow tonal button after them.
 */
@Composable
private fun ActionGroup(state: ProductOverviewUiState?, actions: ProductActions) {
    val menuGroups = actionMenuGroups(state, actions)

    // Always fits, a dialog is at least 280.dp wide
    ButtonGroup(
        overflowIndicator = {},
        // The guidelines' 12.dp between buttons of a small standard group
        horizontalArrangement = ButtonGroupDefaults.HorizontalArrangement,
    ) {
        animatedItem { itemModifier, interactionSource ->
            ActionButton(
                icon = Res.drawable.ic_rounded_edit,
                label = stringResource(Res.string.product_edit),
                onClick = actions.onEditClick,
                modifier = itemModifier,
                interactionSource = interactionSource,
            )
        }
        if (state?.showShoppingList == true) {
            animatedItem { itemModifier, interactionSource ->
                ActionButton(
                    icon = Res.drawable.ic_rounded_add_shopping_cart,
                    label = stringResource(Res.string.product_overview_add_to_shopping_list),
                    onClick = actions.onAddToShoppingListClick,
                    modifier = itemModifier,
                    interactionSource = interactionSource,
                )
            }
        }
        animatedItem { itemModifier, interactionSource ->
            TonalOverflowMenu(
                groups = menuGroups,
                modifier = itemModifier,
                interactionSource = interactionSource,
            )
        }
    }
}

/**
 * The menu of the product overview: the stock pages, Grocy's bookings (their pages do not
 * exist yet, so they are disabled) and the grocycode like Grocy's product card menu.
 */
@Composable
private fun actionMenuGroups(
    state: ProductOverviewUiState?,
    actions: ProductActions,
): List<List<AppBarMenuItem>> {
    val product = stringResource(Res.string.grocycode_product)
    return listOf(
        listOf(
            AppBarMenuItem(stringResource(Res.string.product_stock_entries)) {
                actions.onStockEntriesClick()
            },
            AppBarMenuItem(stringResource(Res.string.product_stock_journal)) {
                actions.onStockJournalClick()
            },
            AppBarMenuItem(stringResource(Res.string.product_stock_journal_summary)) {
                actions.onStockJournalSummaryClick()
            },
        ),
        listOf(
            Res.string.stock_purchase,
            Res.string.stock_consume,
            Res.string.stock_transfer,
            Res.string.stock_inventory,
        ).map { AppBarMenuItem(stringResource(it), enabled = false) {} },
        listOfNotNull(
            AppBarMenuItem(stringResource(Res.string.grocycode_download_of, product)) {
                actions.onGrocycodeDownload(state?.grocycode2D == true)
            },
            AppBarMenuItem(stringResource(Res.string.grocycode_print_of, product)) {
                actions.onGrocycodePrint()
            }.takeIf { state?.showLabelPrinter == true },
        ),
    )
}

@Composable
private fun ActionButton(
    icon: DrawableResource,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier,
    interactionSource: MutableInteractionSource,
) {
    val haptic = LocalHapticFeedback.current

    HapticTooltipBox(text = label, modifier = modifier) {
        FilledIconButton(
            onClick = {
                haptic.confirm()
                onClick()
            },
            modifier = smallIconButtonSize(IconButtonDefaults.IconButtonWidthOption.Wide)
                .clickCursor(),
            shapes = IconButtonDefaults.shapes(),
            interactionSource = interactionSource,
        ) {
            Icon(painter = painterResource(icon), contentDescription = label)
        }
    }
}

@Composable
private fun DescriptionItem(text: String) {
    val haptic = LocalHapticFeedback.current
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    var isOverflowing by remember { mutableStateOf(false) }

    SegmentedContainer(index = 0, count = 1) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (isExpanded) Int.MAX_VALUE else COLLAPSED_DESCRIPTION_LINES,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!isExpanded) isOverflowing = it.hasVisualOverflow },
            modifier = Modifier.fillMaxWidth().animateContentSize(),
        )
        if (isOverflowing) {
            TextButton(
                onClick = {
                    haptic.confirm()
                    isExpanded = !isExpanded
                },
                modifier = Modifier.padding(top = 4.dp).clickCursor(),
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(
                    stringResource(
                        if (isExpanded) {
                            Res.string.product_overview_show_less
                        } else {
                            Res.string.product_overview_show_more
                        },
                    ),
                )
            }
        }
    }
}

/** One value of the overview, the label above it like Grocy's bold labels. */
private class OverviewRow(
    val title: String,
    val lines: List<String>,
    val icon: DrawableResource,
    val isLoading: Boolean = false,
)

@Composable
private fun OverviewGroups(
    state: ProductOverviewUiState,
    details: OverviewDetails?,
    nameRow: OverviewRow,
) {
    val today = remember { todayEpochDay() }
    val unknown = stringResource(Res.string.product_overview_unknown)
    val never = stringResource(Res.string.product_overview_never)
    val opened = stringResource(Res.string.product_overview_opened)
    val withSubProducts = stringResource(Res.string.product_overview_with_sub_products)
    val perUnit = stringResource(Res.string.product_overview_price_per_unit)
    fun money(value: Double) = formatCurrency(value, state.currency, state.priceDecimals)
    fun amount(text: AmountText) = listOfNotNull(
        text.amount,
        text.opened?.let { opened.fill(it) },
    ).joinToString(SEPARATOR)
    fun date(day: Long?) = day?.let {
        formatDate(it) + SEPARATOR + formatRelativeTime(timeSpanBetween(today, it))
    } ?: never
    fun price(price: UnitPrice?) = price?.let {
        listOfNotNull(
            perUnit.fill(money(it.price), it.unit),
            it.pricePerStockUnit?.let { stockPrice ->
                perUnit.fill(money(stockPrice), it.stockUnit.orEmpty())
            },
        )
    } ?: listOf(unknown)

    val stock = buildList {
        add(nameRow)
        if (details == null) return@buildList
        val amounts = listOfNotNull(
            details.stockAmount?.let(::amount),
            details.aggregatedAmount?.let { withSubProducts.fill(amount(it)) },
        )
        add(
            OverviewRow(
                title = stringResource(Res.string.product_overview_stock_amount),
                lines = amounts.ifEmpty { listOf(unknown) },
                icon = Res.drawable.ic_rounded_inventory_2,
            ),
        )
        if (state.showPrices) {
            add(
                OverviewRow(
                    title = stringResource(Res.string.product_overview_stock_value),
                    lines = listOf(money(details.stockValue)),
                    icon = Res.drawable.ic_rounded_payments,
                ),
            )
        }
        if (state.showLocation) {
            add(
                OverviewRow(
                    title = stringResource(Res.string.product_location),
                    lines = listOf(state.locationName ?: unknown),
                    icon = Res.drawable.ic_rounded_location_on,
                ),
            )
        }
    }
    val history = buildList {
        if (details == null) return@buildList
        add(
            OverviewRow(
                title = stringResource(Res.string.product_overview_last_purchased),
                lines = listOf(date(details.lastPurchased)),
                icon = Res.drawable.ic_rounded_shopping_cart,
            ),
        )
        add(
            OverviewRow(
                title = stringResource(Res.string.product_overview_last_used),
                lines = listOf(date(details.lastUsed)),
                icon = Res.drawable.ic_rounded_history,
            ),
        )
        if (state.showPrices) {
            add(
                OverviewRow(
                    title = stringResource(Res.string.product_overview_last_price),
                    lines = price(details.lastPrice),
                    icon = Res.drawable.ic_rounded_sell,
                ),
            )
            add(
                OverviewRow(
                    title = stringResource(Res.string.product_overview_average_price),
                    lines = price(details.averagePrice),
                    icon = Res.drawable.ic_rounded_calculate,
                ),
            )
        }
        if (state.showShelfLife) {
            add(
                OverviewRow(
                    title = stringResource(Res.string.product_overview_average_shelf_life),
                    lines = listOf(
                        when (val shelfLife = details.averageShelfLife) {
                            ShelfLife.Unknown -> unknown
                            ShelfLife.Unlimited ->
                                stringResource(Res.string.product_overview_unlimited)
                            is ShelfLife.Days -> formatDuration(durationOf(shelfLife.days))
                        },
                    ),
                    icon = Res.drawable.ic_rounded_hourglass_empty,
                ),
            )
        }
        add(
            OverviewRow(
                title = stringResource(Res.string.product_overview_spoil_rate),
                lines = listOf(formatPercent(details.spoilRate)),
                icon = Res.drawable.ic_rounded_delete,
            ),
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(SegmentedListDefaults.GroupSpacing),
    ) {
        listOf(stock, history).filter { it.isNotEmpty() }.forEach { rows ->
            SegmentedGroup {
                rows.forEachIndexed { index, row ->
                    OverviewItem(row = row, index = index, count = rows.size)
                }
            }
        }
    }
}

@Composable
private fun OverviewItem(row: OverviewRow, index: Int, count: Int) {
    ListItem(
        modifier = Modifier.fillMaxWidth(),
        leadingContent = {
            LeadingTrailingContentWrapper {
                Icon(painter = painterResource(row.icon), contentDescription = null)
            }
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                row.lines.forEach { Text(it) }
            }
        },
        trailingContent = if (row.isLoading) {
            { LoadingIndicator(Modifier.size(IndicatorSize)) }
        } else {
            null
        },
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = SegmentedListDefaults.colors(),
        content = { Text(row.title) },
    )
}

@Composable
private fun ErrorItem(error: RefreshError, onRetryClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current

    SegmentedContainer(index = 0, count = 1) {
        Text(
            text = stringResource(
                if (error == RefreshError.OFFLINE) {
                    Res.string.product_overview_offline
                } else {
                    Res.string.data_load_failed
                },
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
        Button(
            onClick = {
                haptic.confirm()
                onRetryClick()
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 16.dp)
                .clickCursor(),
            shapes = ButtonDefaults.shapes(),
        ) {
            Text(stringResource(Res.string.action_retry))
        }
    }
}

/** The picture in the dialog's width, offline the smaller list picture. */
@Composable
private fun PictureItem(
    fileName: String,
    load: suspend (fileName: String) -> ByteArray?,
) {
    val picture by produceState<PictureState>(PictureState.Loading, fileName) {
        value = load(fileName)
            ?.let { bytes ->
                try {
                    bytes.decodeToImageBitmap()
                } catch (_: Exception) {
                    // Not an image Compose can show
                    null
                }
            }
            ?.let { PictureState.Loaded(it) }
            ?: PictureState.Failed
    }

    SegmentedContainer(index = 0, count = 1) {
        when (val current = picture) {
            is PictureState.Loaded -> Image(
                bitmap = current.bitmap,
                contentDescription = stringResource(Res.string.product_picture),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = MaxPictureHeight)
                    .clip(MaterialTheme.shapes.medium),
            )

            PictureState.Loading -> Box(
                modifier = Modifier.fillMaxWidth().height(PlaceholderHeight),
                contentAlignment = Alignment.Center,
            ) {
                LoadingIndicator()
            }

            PictureState.Failed -> Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_rounded_no_photography),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(Res.string.product_picture_load_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private sealed interface PictureState {
    data object Loading : PictureState

    data object Failed : PictureState

    class Loaded(val bitmap: ImageBitmap) : PictureState
}

@Composable
private fun PriceHistoryGroup(state: ProductOverviewUiState, history: PriceHistory) {
    SegmentedGroup(title = stringResource(Res.string.product_overview_price_history)) {
        SegmentedContainer(index = 0, count = 1) {
            if (history.isEmpty) {
                Text(
                    text = stringResource(Res.string.product_overview_no_price_history),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                PriceHistoryChart(
                    history = history,
                    currency = state.currency,
                    priceDecimals = state.priceDecimals,
                )
            }
        }
    }
}

/** Fills `%1$s`, `%2$s`, ... of a string resource read in composition, for many values. */
internal fun String.fill(vararg args: String): String =
    args.foldIndexed(this) { index, text, arg -> text.replace("%${index + 1}\$s", arg) }

// Grocy shows the opened amount and the relative date next to the value
private const val SEPARATOR = " · "
private const val COLLAPSED_DESCRIPTION_LINES = 3
private val IndicatorSize = 40.dp
private val MaxPictureHeight = 320.dp
private val PlaceholderHeight = 160.dp

@Preview
@Composable
private fun ProductOverviewPreview() {
    GrocyTheme {
        ProductOverviewContent(
            actions = ProductActions({}, {}, {}, {}, {}, {}, {}),
            name = "Milk",
            state = ProductOverviewUiState(
                productId = 1,
                name = "Milk",
                description = "Fresh whole milk from the farm next door.",
                pictureFileName = null,
                locationName = "Fridge",
                showShoppingList = true,
                showLabelPrinter = false,
                grocycode2D = true,
                showPrices = true,
                showLocation = true,
                showShelfLife = true,
                currency = "EUR",
                priceDecimals = 2,
                details = OverviewDetails(
                    stockAmount = AmountText("3 Pieces", opened = "1"),
                    aggregatedAmount = null,
                    stockValue = 3.6,
                    lastPurchased = 20_740,
                    lastUsed = null,
                    lastPrice = UnitPrice(7.2, "Pack", 1.2, "Piece"),
                    averagePrice = UnitPrice(6.9, "Pack", 1.15, "Piece"),
                    averageShelfLife = ShelfLife.Days(12.0),
                    spoilRate = 0.1,
                    priceHistory = PriceHistory(
                        series = listOf(
                            PriceSeries(
                                SeriesStore.Named("Supermarket"),
                                colorIndex = 0,
                                points = listOf(PricePoint(20_600, 6.0), PricePoint(20_740, 7.2)),
                            ),
                            PriceSeries(
                                SeriesStore.Unknown,
                                colorIndex = 1,
                                points = listOf(PricePoint(20_680, 6.6)),
                            ),
                        ),
                        trend = PriceTrend(20_600, 6.0, 20_740, 7.1),
                    ),
                ),
            ),
            refresh = RefreshState(),
            loadPicture = { null },
            onRetryClick = {},
            onCloseClick = {},
        )
    }
}
