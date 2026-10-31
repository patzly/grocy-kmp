package com.patrickzedler.grocy.feature.stock.impl.booking

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.design.component.AppBarAction
import com.patrickzedler.grocy.core.design.component.AppBarIconButton
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.ConnectedButtonGroup
import com.patrickzedler.grocy.core.design.component.DropdownOption
import com.patrickzedler.grocy.core.design.component.FloatingToolbarIconButton
import com.patrickzedler.grocy.core.design.component.FloatingToolbarWithFab
import com.patrickzedler.grocy.core.design.component.NavigateBackButton
import com.patrickzedler.grocy.core.design.component.NumberStepper
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialog
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialogContent
import com.patrickzedler.grocy.core.design.component.SegmentedContainer
import com.patrickzedler.grocy.core.design.component.SegmentedListScreen
import com.patrickzedler.grocy.core.design.component.SegmentedSwitchItem
import com.patrickzedler.grocy.core.design.component.SegmentedTexts
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.segmentedGroup
import com.patrickzedler.grocy.core.forms.ChangeError
import com.patrickzedler.grocy.core.forms.DatePickerButton
import com.patrickzedler.grocy.core.forms.DropdownItem
import com.patrickzedler.grocy.core.forms.GroupItem
import com.patrickzedler.grocy.core.forms.GroupItems
import com.patrickzedler.grocy.core.forms.ScannerToggleButton
import com.patrickzedler.grocy.core.forms.TextItem
import com.patrickzedler.grocy.core.forms.UserfieldItem
import com.patrickzedler.grocy.core.forms.actionMessage
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.nameFor
import com.patrickzedler.grocy.core.model.stock.BookingKind
import com.patrickzedler.grocy.core.model.stock.StockEntry
import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.core.model.text.formatCurrency
import com.patrickzedler.grocy.core.model.text.formatDate
import com.patrickzedler.grocy.core.model.text.formatDecimal
import com.patrickzedler.grocy.core.model.text.parseDecimal
import com.patrickzedler.grocy.core.model.text.platformDecimalSeparator
import com.patrickzedler.grocy.core.productcard.LabelPrintEffect
import com.patrickzedler.grocy.core.productcard.ProductOverviewHost
import com.patrickzedler.grocy.core.productcard.StockPageActions
import com.patrickzedler.grocy.core.productcard.title
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_cancel
import com.patrickzedler.grocy.core.resources.action_scroll_to_top
import com.patrickzedler.grocy.core.resources.action_undo
import com.patrickzedler.grocy.core.resources.booking_add_as_barcode
import com.patrickzedler.grocy.core.resources.booking_add_as_new_product
import com.patrickzedler.grocy.core.resources.booking_add_barcode_hint
import com.patrickzedler.grocy.core.resources.booking_amount_not_equal
import com.patrickzedler.grocy.core.resources.booking_applies_to_added
import com.patrickzedler.grocy.core.resources.booking_consumed
import com.patrickzedler.grocy.core.resources.booking_date_before_today
import com.patrickzedler.grocy.core.resources.booking_due_date_required
import com.patrickzedler.grocy.core.resources.booking_due_earlier
import com.patrickzedler.grocy.core.resources.booking_entry
import com.patrickzedler.grocy.core.resources.booking_entry_required
import com.patrickzedler.grocy.core.resources.booking_exact_amount
import com.patrickzedler.grocy.core.resources.booking_from_location
import com.patrickzedler.grocy.core.resources.booking_frozen
import com.patrickzedler.grocy.core.resources.booking_inventoried
import com.patrickzedler.grocy.core.resources.booking_inventory_adds
import com.patrickzedler.grocy.core.resources.booking_inventory_removes
import com.patrickzedler.grocy.core.resources.booking_label
import com.patrickzedler.grocy.core.resources.booking_location_required
import com.patrickzedler.grocy.core.resources.booking_mark_as_opened
import com.patrickzedler.grocy.core.resources.booking_moved_to
import com.patrickzedler.grocy.core.resources.booking_new_amount
import com.patrickzedler.grocy.core.resources.booking_no_stock_at_location
import com.patrickzedler.grocy.core.resources.booking_not_in_stock
import com.patrickzedler.grocy.core.resources.booking_not_opened
import com.patrickzedler.grocy.core.resources.booking_opened
import com.patrickzedler.grocy.core.resources.booking_price_means
import com.patrickzedler.grocy.core.resources.booking_product_required
import com.patrickzedler.grocy.core.resources.booking_purchased
import com.patrickzedler.grocy.core.resources.booking_purchased_date_required
import com.patrickzedler.grocy.core.resources.booking_recipe
import com.patrickzedler.grocy.core.resources.booking_recipe_hint
import com.patrickzedler.grocy.core.resources.booking_scan_mode
import com.patrickzedler.grocy.core.resources.booking_scan_mode_hint
import com.patrickzedler.grocy.core.resources.booking_scan_mode_incomplete
import com.patrickzedler.grocy.core.resources.booking_should_not_be_frozen
import com.patrickzedler.grocy.core.resources.booking_tare_info
import com.patrickzedler.grocy.core.resources.booking_thawed
import com.patrickzedler.grocy.core.resources.booking_to_location
import com.patrickzedler.grocy.core.resources.booking_total_price
import com.patrickzedler.grocy.core.resources.booking_transferred
import com.patrickzedler.grocy.core.resources.booking_undone
import com.patrickzedler.grocy.core.resources.booking_unit_price
import com.patrickzedler.grocy.core.resources.booking_unit_required
import com.patrickzedler.grocy.core.resources.booking_unknown_code_message
import com.patrickzedler.grocy.core.resources.booking_unknown_code_title
import com.patrickzedler.grocy.core.resources.booking_use_specific_entry
import com.patrickzedler.grocy.core.resources.booking_use_specific_entry_hint
import com.patrickzedler.grocy.core.resources.data_amount
import com.patrickzedler.grocy.core.resources.data_date_invalid
import com.patrickzedler.grocy.core.resources.data_due_date
import com.patrickzedler.grocy.core.resources.data_load_failed
import com.patrickzedler.grocy.core.resources.data_location
import com.patrickzedler.grocy.core.resources.data_never_overdue
import com.patrickzedler.grocy.core.resources.data_note
import com.patrickzedler.grocy.core.resources.data_number_invalid
import com.patrickzedler.grocy.core.resources.data_number_range_invalid
import com.patrickzedler.grocy.core.resources.data_offline
import com.patrickzedler.grocy.core.resources.data_price
import com.patrickzedler.grocy.core.resources.data_product
import com.patrickzedler.grocy.core.resources.data_purchased_date
import com.patrickzedler.grocy.core.resources.data_quantity_unit
import com.patrickzedler.grocy.core.resources.data_store
import com.patrickzedler.grocy.core.resources.data_this_equals
import com.patrickzedler.grocy.core.resources.grocycode_stock_entry
import com.patrickzedler.grocy.core.resources.ic_rounded_box
import com.patrickzedler.grocy.core.resources.ic_rounded_check
import com.patrickzedler.grocy.core.resources.ic_rounded_info
import com.patrickzedler.grocy.core.resources.ic_rounded_vertical_align_top
import com.patrickzedler.grocy.core.resources.product_overview
import com.patrickzedler.grocy.core.resources.product_stock_label_none
import com.patrickzedler.grocy.core.resources.product_stock_label_per_unit
import com.patrickzedler.grocy.core.resources.product_stock_label_single
import com.patrickzedler.grocy.core.resources.product_userfields
import com.patrickzedler.grocy.core.resources.stock_booking_failed
import com.patrickzedler.grocy.core.resources.stock_entry_opened
import com.patrickzedler.grocy.core.resources.stock_entry_spoiled
import com.patrickzedler.grocy.core.resources.stock_overview_default_location
import com.patrickzedler.grocy.core.scanner.BarcodeScanner
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * One of Grocy's stock pages: the form in Grocy's field order as segmented groups, the product
 * chosen by name, barcode or grocycode (typed or scanned), the booking as FAB and its message
 * with "Undo" as snackbar. [actions] show the chosen product's overview like Grocy's card.
 */
@Composable
internal fun BookingScreen(
    viewModel: BookingViewModel,
    actions: StockPageActions,
    menuItems: List<AppBarMenuItem>,
    onBackClick: () -> Unit,
    onNewProductClick: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val options by viewModel.options.collectAsStateWithLifecycle()
    val form = viewModel.form
    val kind = viewModel.kind
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showScanner by rememberSaveable { mutableStateOf(false) }
    val currentOptions = options

    BookingEvents(viewModel, snackbarHostState, onDone)
    LabelPrintEffect(actions.labels, snackbarHostState)
    viewModel.unknownCode?.let { code ->
        UnknownCodeDialog(
            code = code,
            canCreate = kind == BookingKind.PURCHASE || kind == BookingKind.INVENTORY,
            onAssign = viewModel::assignUnknownCode,
            onCreate = {
                viewModel.keepUnknownCodeForNewProduct()
                onNewProductClick()
            },
            onDismissRequest = viewModel::dismissUnknownCode,
        )
    }

    val overviewDescription = stringResource(Res.string.product_overview)
    SegmentedListScreen(
        title = stringResource(kind.title),
        modifier = modifier,
        navigationIcon = { NavigateBackButton(onClick = onBackClick) },
        actions = listOfNotNull(
            form.product?.let { product ->
                AppBarAction { actionModifier, interactionSource ->
                    AppBarIconButton(
                        painter = painterResource(Res.drawable.ic_rounded_info),
                        contentDescription = overviewDescription,
                        onClick = { actions.overview.show(product.id) },
                        modifier = actionModifier,
                        interactionSource = interactionSource,
                    )
                }
            },
        ),
        menuItems = menuItems,
        listState = listState,
        snackbarHostState = snackbarHostState,
        floatingToolbar = { scrollBehavior, toolbarModifier ->
            FloatingToolbarWithFab(
                scrollBehavior = scrollBehavior,
                modifier = toolbarModifier,
                fabPainter = painterResource(Res.drawable.ic_rounded_check),
                fabDescription = stringResource(kind.title),
                onFabClick = { viewModel.book() },
            ) {
                if (kind == BookingKind.CONSUME && currentOptions != null &&
                    currentOptions.config.stockProductOpenedTracking
                ) {
                    FloatingToolbarIconButton(
                        painter = painterResource(Res.drawable.ic_rounded_box),
                        contentDescription = stringResource(Res.string.booking_mark_as_opened),
                        onClick = { viewModel.markAsOpened() },
                        enabled = viewModel.canOpen(currentOptions),
                    )
                }
                FloatingToolbarIconButton(
                    painter = painterResource(Res.drawable.ic_rounded_vertical_align_top),
                    contentDescription = stringResource(Res.string.action_scroll_to_top),
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                    enabled = listState.canScrollBackward,
                )
            }
        },
    ) {
        if (currentOptions == null) return@SegmentedListScreen
        bookingForm(
            viewModel = viewModel,
            options = currentOptions,
            showScanner = showScanner,
            onScannerToggle = { showScanner = it },
        )
    }

    ProductOverviewHost(
        overview = actions.overview,
        shoppingList = actions.shoppingList,
        labels = actions.labels,
        snackbarHostState = snackbarHostState,
        links = actions.links,
        nameOf = { id -> currentOptions?.products?.firstOrNull { it.id == id }?.name.orEmpty() },
    )
}

private fun LazyListScope.bookingForm(
    viewModel: BookingViewModel,
    options: BookingOptions,
    showScanner: Boolean,
    onScannerToggle: (Boolean) -> Unit,
) {
    val kind = viewModel.kind
    val form = viewModel.form
    if (kind == BookingKind.PURCHASE || kind == BookingKind.CONSUME) {
        segmentedGroup(key = "scan-mode") {
            SegmentedSwitchItem(
                headline = stringResource(Res.string.booking_scan_mode),
                supporting = stringResource(Res.string.booking_scan_mode_hint),
                checked = viewModel.isScanMode(options),
                onCheckedChange = viewModel::setScanMode,
                index = 0,
                count = 1,
            )
        }
    }
    segmentedGroup(key = "product") {
        GroupItems(
            productItem(viewModel, options, showScanner, onScannerToggle),
            exactAmountItem(viewModel),
            amountItem(viewModel, options),
            unitItem(viewModel),
        )
    }
    when (kind) {
        BookingKind.CONSUME, BookingKind.TRANSFER -> segmentedGroup(key = "source") {
            GroupItems(
                sourceLocationItem(viewModel, options),
                toLocationItem(viewModel, options),
                specificEntrySwitchItem(viewModel),
                specificEntryItem(viewModel, options),
                spoiledItem(viewModel),
                recipeItem(viewModel, options),
            )
        }

        BookingKind.PURCHASE, BookingKind.INVENTORY -> {
            val title = Res.string.booking_applies_to_added.takeIf { kind == BookingKind.INVENTORY }
            segmentedGroup(key = "additions", title = title) {
                GroupItems(
                    purchasedDateItem(viewModel, options),
                    dueDateItem(viewModel, options),
                    neverOverdueItem(viewModel, options),
                    priceItem(viewModel, options),
                    priceTypeItem(viewModel, options),
                    storeItem(viewModel, options),
                    locationItem(viewModel, options),
                    labelTypeItem(viewModel, options),
                    noteItem(viewModel),
                )
            }
            if (form.userfields.isNotEmpty()) {
                segmentedGroup(key = "userfields", title = Res.string.product_userfields) {
                    form.userfields.forEachIndexed { index, input ->
                        UserfieldItem(
                            input = input,
                            error = form.userfieldErrors[input.userfield.name],
                            index = index,
                            count = form.userfields.size,
                        )
                    }
                }
            }
        }
    }
}

private fun productItem(
    viewModel: BookingViewModel,
    options: BookingOptions,
    showScanner: Boolean,
    onScannerToggle: (Boolean) -> Unit,
): GroupItem = { index, count ->
    val form = viewModel.form
    val scanMode = viewModel.isScanMode(options)
    val context = form.context
    DropdownItem(
        label = stringResource(Res.string.data_product),
        options = viewModel.offeredProducts(options).map { DropdownOption(it.id, it.name) },
        selectedId = form.product?.id,
        onSelectedIdChange = viewModel::selectProduct,
        onUnmatchedChange = { form.isProductUnmatched = it },
        hint = form.pendingBarcode?.let { stringResource(Res.string.booking_add_barcode_hint, it) },
        error = when {
            context is ProductContext.Failed -> stringResource(
                if (context.error == ChangeError.Offline) {
                    Res.string.data_offline
                } else {
                    Res.string.data_load_failed
                },
            )
            form.errors[BookingField.PRODUCT] != null ->
                stringResource(Res.string.booking_product_required)
            else -> null
        },
        index = index,
        count = count,
        below = if (showScanner) {
            {
                BarcodeScanner(
                    onBarcodeScanned = { barcode ->
                        viewModel.onCode(barcode.value, scanMode)
                        // Scan mode books one product after the other
                        if (!scanMode) onScannerToggle(false)
                    },
                    shape = MaterialTheme.shapes.small,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
            }
        } else {
            null
        },
        trailingAction = {
            ScannerToggleButton(
                isScannerOpen = showScanner,
                onClick = { onScannerToggle(!showScanner) },
            )
        },
    )
}

private fun exactAmountItem(viewModel: BookingViewModel): GroupItem? {
    val product = viewModel.form.product
    if (viewModel.kind != BookingKind.CONSUME || product?.enableTareWeightHandling != true) {
        return null
    }
    return { index, count ->
        SegmentedSwitchItem(
            headline = stringResource(Res.string.booking_exact_amount),
            checked = viewModel.form.exactAmount,
            onCheckedChange = { viewModel.form.exactAmount = it },
            index = index,
            count = count,
        )
    }
}

private fun amountItem(viewModel: BookingViewModel, options: BookingOptions): GroupItem =
    { index, count ->
        val form = viewModel.form
        val limits = viewModel.limits(options)
        val language = LocalLocale.current.toLanguageTag()
        val decimals = options.settings.stockDecimalPlacesAmounts
        val separator = platformDecimalSeparator()
        val typed = parseDecimal(form.amount.text.toString())
        val stockUnit = form.stockUnit
        val hint = when {
            form.product?.enableTareWeightHandling == true &&
                !(viewModel.kind == BookingKind.CONSUME && form.exactAmount) ->
                stringResource(Res.string.booking_tare_info)

            viewModel.kind == BookingKind.INVENTORY -> when (
                val change = viewModel.inventoryChange()
            ) {
                is InventoryChange.Added -> stringResource(
                    Res.string.booking_inventory_adds,
                    amountText(change.amount, stockUnit, decimals, language),
                )
                is InventoryChange.Removed -> stringResource(
                    Res.string.booking_inventory_removes,
                    amountText(change.amount, stockUnit, decimals, language),
                )
                else -> null
            }

            typed != null && stockUnit != null && form.unit?.unit?.id != stockUnit.id -> {
                val converted = viewModel.stockAmountOf(typed)
                stringResource(
                    Res.string.data_this_equals,
                    formatDecimal(converted, decimals, separator),
                    stockUnit.nameFor(converted, language),
                )
            }

            else -> null
        }
        TextItem(
            state = form.amount,
            label = stringResource(
                if (viewModel.kind == BookingKind.INVENTORY) {
                    Res.string.booking_new_amount
                } else {
                    Res.string.data_amount
                },
            ),
            hint = hint,
            error = form.errors[BookingField.AMOUNT]?.let { bookingErrorText(it, decimals) },
            enabled = form.product != null,
            keyboardType = KeyboardType.Decimal,
            stepper = NumberStepper(min = limits?.min, max = limits?.max),
            index = index,
            count = count,
        )
    }

private fun unitItem(viewModel: BookingViewModel): GroupItem = { index, count ->
    val form = viewModel.form
    DropdownItem(
        label = stringResource(Res.string.data_quantity_unit),
        options = form.units.map { DropdownOption(it.unit.id, it.unit.name) },
        selectedId = form.quId,
        onSelectedIdChange = viewModel::setUnit,
        onUnmatchedChange = { form.isUnitUnmatched = it },
        error = form.errors[BookingField.UNIT]?.let {
            stringResource(Res.string.booking_unit_required)
        },
        // Tare weight handling weighs in the stock unit, a single unit leaves no choice
        enabled = form.units.size > 1 && form.product?.enableTareWeightHandling != true,
        index = index,
        count = count,
    )
}

private fun sourceLocationItem(viewModel: BookingViewModel, options: BookingOptions): GroupItem? {
    if (!options.config.stockLocationTracking) return null
    return { index, count ->
        val form = viewModel.form
        val product = form.product
        val amounts = (form.context as? ProductContext.Ready)?.locationAmounts.orEmpty()
        val default = product?.let { defaultLocationOf(it, viewModel.kind == BookingKind.CONSUME) }
        val defaultSuffix = stringResource(Res.string.stock_overview_default_location)
        DropdownItem(
            label = stringResource(
                if (viewModel.kind == BookingKind.TRANSFER) {
                    Res.string.booking_from_location
                } else {
                    Res.string.data_location
                },
            ),
            options = amounts.map {
                val name = if (it.locationId == default) {
                    "${it.locationName} ($defaultSuffix)"
                } else {
                    it.locationName
                }
                DropdownOption(it.locationId, name)
            },
            selectedId = form.locationId,
            onSelectedIdChange = viewModel::setLocation,
            onUnmatchedChange = { form.isLocationUnmatched = it },
            error = form.errors[BookingField.LOCATION]?.let {
                stringResource(Res.string.booking_location_required)
            },
            enabled = product != null,
            index = index,
            count = count,
        )
    }
}

private fun toLocationItem(viewModel: BookingViewModel, options: BookingOptions): GroupItem? {
    if (viewModel.kind != BookingKind.TRANSFER) return null
    return { index, count ->
        val form = viewModel.form
        DropdownItem(
            label = stringResource(Res.string.booking_to_location),
            options = options.locations
                .filter { it.active && it.id != form.locationId }
                .map { DropdownOption(it.id, it.name) },
            selectedId = form.toLocationId,
            onSelectedIdChange = { form.toLocationId = it },
            onUnmatchedChange = { form.isLocationUnmatched = it },
            error = form.errors[BookingField.TO_LOCATION]?.let {
                stringResource(Res.string.booking_location_required)
            },
            enabled = form.product != null,
            index = index,
            count = count,
        )
    }
}

private fun specificEntrySwitchItem(viewModel: BookingViewModel): GroupItem = { index, count ->
    SegmentedSwitchItem(
        headline = stringResource(Res.string.booking_use_specific_entry),
        supporting = stringResource(Res.string.booking_use_specific_entry_hint),
        checked = viewModel.form.useSpecificEntry,
        onCheckedChange = viewModel::setUseSpecificEntry,
        enabled = viewModel.form.context is ProductContext.Ready,
        index = index,
        count = count,
    )
}

private fun specificEntryItem(viewModel: BookingViewModel, options: BookingOptions): GroupItem? {
    val form = viewModel.form
    if (!form.useSpecificEntry) return null
    return { index, count ->
        val ready = form.context as? ProductContext.Ready
        val trackLocations = options.config.stockLocationTracking
        val entries = ready?.entries.orEmpty()
            .filter { !trackLocations || it.locationId == form.locationId }
        DropdownItem(
            label = stringResource(Res.string.grocycode_stock_entry),
            options = entries.map { DropdownOption(it.id, entryText(it, options)) },
            selectedId = entries.firstOrNull { it.stockId == form.stockId }?.id,
            onSelectedIdChange = { id ->
                form.stockId = entries.firstOrNull { it.id == id }?.stockId
            },
            onUnmatchedChange = {},
            error = form.errors[BookingField.STOCK_ENTRY]?.let {
                stringResource(Res.string.booking_entry_required)
            },
            index = index,
            count = count,
        )
    }
}

/** Grocy's text of a stock entry in its consume and transfer choices. */
@Composable
private fun entryText(entry: StockEntry, options: BookingOptions): String {
    val decimals = options.settings.stockDecimalPlacesAmounts
    val never = stringResource(Res.string.data_never_overdue)
    fun date(value: String?): String = when {
        value == null -> ""
        value == StockEntry.NEVER_OVERDUE -> never
        else -> epochDayOf(value)?.let { formatDate(it) } ?: value
    }
    val opened = stringResource(
        if (entry.open) Res.string.stock_entry_opened else Res.string.booking_not_opened,
    )
    return listOfNotNull(
        stringResource(
            Res.string.booking_entry,
            formatDecimal(entry.amount, decimals, platformDecimalSeparator()),
            date(entry.bestBeforeDate),
            date(entry.purchasedDate),
        ),
        opened,
        entry.note,
    ).joinToString("; ")
}

private fun spoiledItem(viewModel: BookingViewModel): GroupItem? {
    if (viewModel.kind != BookingKind.CONSUME) return null
    return { index, count ->
        SegmentedSwitchItem(
            headline = stringResource(Res.string.stock_entry_spoiled),
            checked = viewModel.form.spoiled,
            onCheckedChange = { viewModel.form.spoiled = it },
            index = index,
            count = count,
        )
    }
}

private fun recipeItem(viewModel: BookingViewModel, options: BookingOptions): GroupItem? {
    if (viewModel.kind != BookingKind.CONSUME || !options.config.recipes) return null
    return { index, count ->
        val form = viewModel.form
        DropdownItem(
            label = stringResource(Res.string.booking_recipe),
            options = viewModel.recipes.entries
                .sortedBy { it.value.lowercase() }
                .map { DropdownOption(it.key, it.value) },
            selectedId = form.recipeId,
            onSelectedIdChange = { form.recipeId = it },
            onUnmatchedChange = { form.isRecipeUnmatched = it },
            hint = stringResource(Res.string.booking_recipe_hint),
            optional = true,
            index = index,
            count = count,
        )
    }
}

private fun purchasedDateItem(viewModel: BookingViewModel, options: BookingOptions): GroupItem? {
    if (!options.settings.showPurchasedDateOnPurchase) return null
    return { index, count ->
        val form = viewModel.form
        TextItem(
            state = form.purchasedDate,
            label = stringResource(Res.string.data_purchased_date),
            error = form.errors[BookingField.PURCHASED_DATE]?.let {
                dateErrorText(it, Res.string.booking_purchased_date_required)
            },
            trailingAction = { DatePickerButton(form.purchasedDate, withTime = false) },
            index = index,
            count = count,
        )
    }
}

private fun dueDateItem(viewModel: BookingViewModel, options: BookingOptions): GroupItem? {
    if (!options.config.stockBestBeforeDateTracking) return null
    return { index, count ->
        val form = viewModel.form
        val date = form.dueDate.text.toString()
        val neverOverdue = date == NEVER_OVERDUE_DATE
        TextItem(
            state = form.dueDate,
            label = stringResource(Res.string.data_due_date),
            hint = stringResource(Res.string.booking_date_before_today)
                .takeIf { viewModel.isBeforeToday(date) },
            error = form.errors[BookingField.DUE_DATE]?.let {
                dateErrorText(it, Res.string.booking_due_date_required)
            },
            enabled = !neverOverdue,
            trailingAction = if (neverOverdue) {
                null
            } else {
                { DatePickerButton(form.dueDate, withTime = false) }
            },
            index = index,
            count = count,
        )
    }
}

private fun neverOverdueItem(viewModel: BookingViewModel, options: BookingOptions): GroupItem? {
    if (!options.config.stockBestBeforeDateTracking) return null
    return { index, count ->
        SegmentedSwitchItem(
            headline = stringResource(Res.string.data_never_overdue),
            checked = viewModel.form.dueDate.text.toString() == NEVER_OVERDUE_DATE,
            onCheckedChange = viewModel::setNeverOverdue,
            index = index,
            count = count,
        )
    }
}

private fun priceItem(viewModel: BookingViewModel, options: BookingOptions): GroupItem? {
    if (!options.config.stockPriceTracking) return null
    return { index, count ->
        val form = viewModel.form
        val decimals = options.settings.stockDecimalPlacesPricesInput
        val price = parseDecimal(form.price.text.toString())
        val amount = parseDecimal(form.amount.text.toString())
        val stockUnit = form.stockUnit
        val language = LocalLocale.current.toLanguageTag()
        val otherUnit = form.unit?.unit?.id != stockUnit?.id
        val showsMeaning = price != null && price > 0 && stockUnit != null &&
            (otherUnit || (form.isTotalPrice && viewModel.kind == BookingKind.PURCHASE)) &&
            (amount ?: 0.0) > 0
        val hint = if (showsMeaning) {
            val product = form.product
            val perStockUnit = pricePerStockUnit(
                price = price,
                factor = form.unit?.factor ?: 1.0,
                isTotalPrice = form.isTotalPrice && viewModel.kind == BookingKind.PURCHASE,
                displayAmount = amount ?: 0.0,
                tareWeight = product?.tareWeight?.takeIf { product.enableTareWeightHandling }
                    ?: 0.0,
            )
            stringResource(
                Res.string.booking_price_means,
                formatCurrency(
                    perStockUnit,
                    options.config.currency,
                    options.settings.stockDecimalPlacesPricesDisplay,
                ),
                stockUnit.nameFor(1.0, language),
            )
        } else {
            null
        }
        TextItem(
            state = form.price,
            label = stringResource(Res.string.data_price),
            hint = hint,
            error = form.errors[BookingField.PRICE]?.let {
                stringResource(Res.string.data_number_invalid, "0", decimals.toString())
            },
            suffix = options.config.currency,
            keyboardType = KeyboardType.Decimal,
            index = index,
            count = count,
        )
    }
}

private fun priceTypeItem(viewModel: BookingViewModel, options: BookingOptions): GroupItem? {
    if (!options.config.stockPriceTracking || viewModel.kind != BookingKind.PURCHASE) return null
    return { index, count ->
        SegmentedContainer(index = index, count = count) {
            ConnectedButtonGroup(
                options = listOf(false, true),
                checked = viewModel.form.isTotalPrice,
                onCheckedChange = { viewModel.form.isTotalPrice = it },
                modifier = Modifier.fillMaxWidth(),
                label = { total ->
                    stringResource(
                        if (total) {
                            Res.string.booking_total_price
                        } else {
                            Res.string.booking_unit_price
                        },
                    )
                },
            )
        }
    }
}

private fun storeItem(viewModel: BookingViewModel, options: BookingOptions): GroupItem? {
    if (!options.config.stockPriceTracking) return null
    return { index, count ->
        val form = viewModel.form
        DropdownItem(
            label = stringResource(Res.string.data_store),
            options = options.stores
                .filter { it.active || it.id == form.storeId }
                .map { DropdownOption(it.id, it.name) },
            selectedId = form.storeId,
            onSelectedIdChange = { form.storeId = it },
            onUnmatchedChange = { form.isStoreUnmatched = it },
            optional = true,
            index = index,
            count = count,
        )
    }
}

private fun locationItem(viewModel: BookingViewModel, options: BookingOptions): GroupItem? {
    if (!options.config.stockLocationTracking) return null
    return { index, count ->
        val form = viewModel.form
        DropdownItem(
            label = stringResource(Res.string.data_location),
            options = options.locations
                .filter { it.active || it.id == form.locationId }
                .map { DropdownOption(it.id, it.name) },
            selectedId = form.locationId,
            onSelectedIdChange = viewModel::setLocation,
            onUnmatchedChange = { form.isLocationUnmatched = it },
            error = form.errors[BookingField.LOCATION]?.let {
                stringResource(Res.string.booking_location_required)
            },
            // Grocy's purchase takes the product's default location without one
            optional = viewModel.kind == BookingKind.PURCHASE,
            index = index,
            count = count,
        )
    }
}

private fun labelTypeItem(viewModel: BookingViewModel, options: BookingOptions): GroupItem? {
    if (!options.config.labelPrinter) return null
    return { index, count ->
        SegmentedContainer(index = index, count = count) {
            SegmentedTexts(headline = stringResource(Res.string.booking_label))
            Spacer(Modifier.height(12.dp))
            ConnectedButtonGroup(
                options = LabelTypes,
                checked = viewModel.form.labelType,
                onCheckedChange = { viewModel.form.labelType = it },
                modifier = Modifier.fillMaxWidth(),
                label = { type ->
                    stringResource(
                        when (type) {
                            1 -> Res.string.product_stock_label_single
                            2 -> Res.string.product_stock_label_per_unit
                            else -> Res.string.product_stock_label_none
                        },
                    )
                },
            )
        }
    }
}

private fun noteItem(viewModel: BookingViewModel): GroupItem = { index, count ->
    TextItem(
        state = viewModel.form.note,
        label = stringResource(Res.string.data_note),
        index = index,
        count = count,
    )
}

/** Grocy's question for a code that names no product. */
@Composable
private fun UnknownCodeDialog(
    code: String,
    canCreate: Boolean,
    onAssign: () -> Unit,
    onCreate: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    @Composable
    fun Action(text: StringResource, onClick: () -> Unit) {
        TextButton(
            onClick = {
                haptic.confirm()
                onClick()
            },
            modifier = Modifier.clickCursor(),
            shapes = ButtonDefaults.shapes(),
        ) {
            Text(stringResource(text))
        }
    }

    ScrollableAlertDialog(onDismissRequest = onDismissRequest) {
        ScrollableAlertDialogContent(
            title = { Text(stringResource(Res.string.booking_unknown_code_title)) },
            confirmButton = { Action(Res.string.booking_add_as_barcode, onAssign) },
            dismissButton = if (canCreate) {
                { Action(Res.string.booking_add_as_new_product, onCreate) }
            } else {
                null
            },
            extraButton = { Action(Res.string.action_cancel, onDismissRequest) },
        ) {
            Text(
                text = stringResource(Res.string.booking_unknown_code_message, code),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Shows the page's messages: bookings with "Undo", Grocy's warnings and errors. */
@Composable
private fun BookingEvents(
    viewModel: BookingViewModel,
    snackbarHostState: SnackbarHostState,
    onDone: () -> Unit,
) {
    val language = LocalLocale.current.toLanguageTag()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val decimals = viewModel.options.value?.settings?.stockDecimalPlacesAmounts ?: 4
            fun amount(value: Double, unit: QuantityUnit?) =
                amountText(value, unit, decimals, language)
            suspend fun withUndo(message: String, transactionId: String?) {
                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = transactionId?.let { getString(Res.string.action_undo) },
                    withDismissAction = true,
                )
                if (result == SnackbarResult.ActionPerformed && transactionId != null) {
                    viewModel.undo(transactionId)
                }
            }
            when (event) {
                is BookingEvent.Booked -> withUndo(
                    when (event.kind) {
                        BookingKind.PURCHASE -> getString(
                            Res.string.booking_purchased,
                            amount(event.amount, event.unit),
                            event.productName,
                        )
                        BookingKind.CONSUME -> getString(
                            Res.string.booking_consumed,
                            amount(event.amount, event.unit),
                            event.productName,
                        )
                        BookingKind.TRANSFER -> getString(
                            Res.string.booking_transferred,
                            amount(event.amount, event.unit),
                            event.productName,
                            event.fromLocation.orEmpty(),
                            event.toLocation.orEmpty(),
                        )
                        BookingKind.INVENTORY -> getString(
                            Res.string.booking_inventoried,
                            event.productName,
                            amount(event.amount, event.unit),
                        )
                    },
                    event.transactionId,
                )

                is BookingEvent.Opened -> {
                    val opened = getString(
                        Res.string.booking_opened,
                        amount(event.amount, event.unit),
                        event.productName,
                    )
                    val moved = event.movedTo?.let { getString(Res.string.booking_moved_to, it) }
                    withUndo(listOfNotNull(opened, moved).joinToString("\n"), event.transactionId)
                }

                BookingEvent.DueEarlier ->
                    snackbarHostState.showSnackbar(getString(Res.string.booking_due_earlier))
                is BookingEvent.Frozen -> snackbarHostState.showSnackbar(
                    listOfNotNull(
                        getString(Res.string.booking_frozen),
                        getString(Res.string.booking_should_not_be_frozen)
                            .takeIf { event.shouldNotBeFrozen },
                    ).joinToString("\n"),
                )
                BookingEvent.Thawed ->
                    snackbarHostState.showSnackbar(getString(Res.string.booking_thawed))
                BookingEvent.NotOffered ->
                    snackbarHostState.showSnackbar(getString(Res.string.booking_not_in_stock))
                BookingEvent.ScanModeIncomplete -> snackbarHostState.showSnackbar(
                    getString(Res.string.booking_scan_mode_incomplete),
                )
                is BookingEvent.Failed -> snackbarHostState.showSnackbar(
                    event.error.actionMessage(Res.string.stock_booking_failed),
                )
                BookingEvent.Undone ->
                    snackbarHostState.showSnackbar(getString(Res.string.booking_undone))
                is BookingEvent.UndoFailed -> snackbarHostState.showSnackbar(
                    event.error.actionMessage(Res.string.stock_booking_failed),
                )
                BookingEvent.Done -> onDone()
            }
        }
    }
}

/** [amount] with the unit's name for it, like Grocy's messages. */
private fun amountText(amount: Double, unit: QuantityUnit?, decimals: Int, language: String) =
    listOfNotNull(
        formatDecimal(amount, decimals, platformDecimalSeparator()),
        unit?.nameFor(amount, language),
    ).joinToString(" ")

@Composable
private fun bookingErrorText(error: BookingError, decimals: Int): String? {
    val separator = platformDecimalSeparator()
    fun number(value: Double) = formatDecimal(value, decimals, separator)
    return when (error) {
        is BookingError.InvalidAmount -> when {
            error.notEqual != null -> stringResource(
                Res.string.booking_amount_not_equal,
                number(error.min),
                number(error.notEqual),
                error.decimals.toString(),
            )
            error.max != null -> stringResource(
                Res.string.data_number_range_invalid,
                number(error.min),
                number(error.max),
                error.decimals.toString(),
            )
            else -> stringResource(
                Res.string.data_number_invalid,
                number(error.min),
                error.decimals.toString(),
            )
        }
        BookingError.NoStockAtLocation -> stringResource(Res.string.booking_no_stock_at_location)
        else -> null
    }
}

@Composable
private fun dateErrorText(error: BookingError, required: StringResource): String? = when (error) {
    BookingError.Required -> stringResource(required)
    BookingError.InvalidDate -> stringResource(Res.string.data_date_invalid)
    else -> null
}

// Grocy's label types: none, single, per unit
private val LabelTypes = listOf(0, 1, 2)
