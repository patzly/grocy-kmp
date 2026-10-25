@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.patrickzedler.grocy.feature.masterdata.impl.product

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.patrickzedler.grocy.core.data.masterdata.PictureChange
import com.patrickzedler.grocy.core.design.component.DropdownOption
import com.patrickzedler.grocy.core.design.component.FloatingToolbarIconButton
import com.patrickzedler.grocy.core.design.component.FloatingToolbarWithFab
import com.patrickzedler.grocy.core.design.component.LeadingTrailingContentWrapper
import com.patrickzedler.grocy.core.design.component.NavigateBackButton
import com.patrickzedler.grocy.core.design.component.ScreenPane
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.SegmentedListScreen
import com.patrickzedler.grocy.core.design.component.SegmentedSwitchItem
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.insetItem
import com.patrickzedler.grocy.core.design.component.segmentedGroup
import com.patrickzedler.grocy.core.model.masterdata.DueType
import com.patrickzedler.grocy.core.model.masterdata.Location
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.PurchasePriceType
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.StockLabelType
import com.patrickzedler.grocy.core.model.masterdata.Store
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_save
import com.patrickzedler.grocy.core.resources.data_save_failed
import com.patrickzedler.grocy.core.resources.data_save_failed_details
import com.patrickzedler.grocy.core.resources.data_save_offline
import com.patrickzedler.grocy.core.resources.data_saved
import com.patrickzedler.grocy.core.resources.grocycode_product
import com.patrickzedler.grocy.core.resources.grocycode_title
import com.patrickzedler.grocy.core.resources.ic_rounded_arrow_right
import com.patrickzedler.grocy.core.resources.ic_rounded_image
import com.patrickzedler.grocy.core.resources.ic_rounded_inventory_2
import com.patrickzedler.grocy.core.resources.ic_rounded_receipt_long
import com.patrickzedler.grocy.core.resources.ic_rounded_save
import com.patrickzedler.grocy.core.resources.product_active
import com.patrickzedler.grocy.core.resources.product_auto_reprint_label
import com.patrickzedler.grocy.core.resources.product_auto_reprint_label_hint
import com.patrickzedler.grocy.core.resources.product_consume_location
import com.patrickzedler.grocy.core.resources.product_consume_location_hint
import com.patrickzedler.grocy.core.resources.product_create
import com.patrickzedler.grocy.core.resources.product_cumulate_min_stock
import com.patrickzedler.grocy.core.resources.product_cumulate_min_stock_hint
import com.patrickzedler.grocy.core.resources.product_description
import com.patrickzedler.grocy.core.resources.product_disable_open
import com.patrickzedler.grocy.core.resources.product_due_days
import com.patrickzedler.grocy.core.resources.product_due_days_after_freezing
import com.patrickzedler.grocy.core.resources.product_due_days_after_freezing_hint
import com.patrickzedler.grocy.core.resources.product_due_days_after_open
import com.patrickzedler.grocy.core.resources.product_due_days_after_open_hint
import com.patrickzedler.grocy.core.resources.product_due_days_after_thawing
import com.patrickzedler.grocy.core.resources.product_due_days_after_thawing_hint
import com.patrickzedler.grocy.core.resources.product_due_days_hint
import com.patrickzedler.grocy.core.resources.product_due_type
import com.patrickzedler.grocy.core.resources.product_due_type_best_before
import com.patrickzedler.grocy.core.resources.product_due_type_best_before_hint
import com.patrickzedler.grocy.core.resources.product_due_type_expiration
import com.patrickzedler.grocy.core.resources.product_due_type_expiration_hint
import com.patrickzedler.grocy.core.resources.product_due_type_hint
import com.patrickzedler.grocy.core.resources.product_edit
import com.patrickzedler.grocy.core.resources.product_energy
import com.patrickzedler.grocy.core.resources.product_energy_hint
import com.patrickzedler.grocy.core.resources.product_group_due_date
import com.patrickzedler.grocy.core.resources.product_group_location
import com.patrickzedler.grocy.core.resources.product_group_more
import com.patrickzedler.grocy.core.resources.product_group_quantity_units
import com.patrickzedler.grocy.core.resources.product_group_stock
import com.patrickzedler.grocy.core.resources.product_group_tare_weight
import com.patrickzedler.grocy.core.resources.product_hide_on_stock_overview
import com.patrickzedler.grocy.core.resources.product_hide_on_stock_overview_hint
import com.patrickzedler.grocy.core.resources.product_location
import com.patrickzedler.grocy.core.resources.product_location_required
import com.patrickzedler.grocy.core.resources.product_min_stock_amount
import com.patrickzedler.grocy.core.resources.product_move_on_open
import com.patrickzedler.grocy.core.resources.product_move_on_open_hint
import com.patrickzedler.grocy.core.resources.product_name
import com.patrickzedler.grocy.core.resources.product_name_required
import com.patrickzedler.grocy.core.resources.product_no_own_stock
import com.patrickzedler.grocy.core.resources.product_no_own_stock_hint
import com.patrickzedler.grocy.core.resources.product_not_check_stock_fulfillment
import com.patrickzedler.grocy.core.resources.product_not_check_stock_fulfillment_hint
import com.patrickzedler.grocy.core.resources.product_parent
import com.patrickzedler.grocy.core.resources.product_parent_disabled
import com.patrickzedler.grocy.core.resources.product_parent_not_allowed
import com.patrickzedler.grocy.core.resources.product_parent_not_found
import com.patrickzedler.grocy.core.resources.product_picture
import com.patrickzedler.grocy.core.resources.product_picture_none
import com.patrickzedler.grocy.core.resources.product_picture_will_delete
import com.patrickzedler.grocy.core.resources.product_picture_will_upload
import com.patrickzedler.grocy.core.resources.product_price_type
import com.patrickzedler.grocy.core.resources.product_price_type_hint
import com.patrickzedler.grocy.core.resources.product_price_type_total
import com.patrickzedler.grocy.core.resources.product_price_type_unit
import com.patrickzedler.grocy.core.resources.product_price_type_unspecified
import com.patrickzedler.grocy.core.resources.product_product_group
import com.patrickzedler.grocy.core.resources.product_qu_consume
import com.patrickzedler.grocy.core.resources.product_qu_consume_hint
import com.patrickzedler.grocy.core.resources.product_qu_conversions
import com.patrickzedler.grocy.core.resources.product_qu_price
import com.patrickzedler.grocy.core.resources.product_qu_price_hint
import com.patrickzedler.grocy.core.resources.product_qu_purchase
import com.patrickzedler.grocy.core.resources.product_qu_purchase_hint
import com.patrickzedler.grocy.core.resources.product_qu_required
import com.patrickzedler.grocy.core.resources.product_qu_stock
import com.patrickzedler.grocy.core.resources.product_qu_stock_hint
import com.patrickzedler.grocy.core.resources.product_quick_consume_amount
import com.patrickzedler.grocy.core.resources.product_quick_consume_amount_hint
import com.patrickzedler.grocy.core.resources.product_quick_open_amount
import com.patrickzedler.grocy.core.resources.product_quick_open_amount_hint
import com.patrickzedler.grocy.core.resources.product_save_hint
import com.patrickzedler.grocy.core.resources.product_should_not_be_frozen
import com.patrickzedler.grocy.core.resources.product_should_not_be_frozen_hint
import com.patrickzedler.grocy.core.resources.product_stock_entries
import com.patrickzedler.grocy.core.resources.product_stock_journal
import com.patrickzedler.grocy.core.resources.product_stock_label
import com.patrickzedler.grocy.core.resources.product_stock_label_hint
import com.patrickzedler.grocy.core.resources.product_stock_label_none
import com.patrickzedler.grocy.core.resources.product_stock_label_per_unit
import com.patrickzedler.grocy.core.resources.product_stock_label_single
import com.patrickzedler.grocy.core.resources.product_store
import com.patrickzedler.grocy.core.resources.product_tare_weight
import com.patrickzedler.grocy.core.resources.product_tare_weight_handling
import com.patrickzedler.grocy.core.resources.product_tare_weight_handling_hint
import com.patrickzedler.grocy.core.resources.product_treat_opened_as_out_of_stock
import com.patrickzedler.grocy.core.resources.product_treat_opened_as_out_of_stock_hint
import com.patrickzedler.grocy.core.resources.product_userfields
import com.patrickzedler.grocy.core.scanner.BarcodeScanner
import com.patrickzedler.grocy.core.scanner.writer.Grocycode
import com.patrickzedler.grocy.feature.masterdata.impl.barcode.BarcodeDialog
import com.patrickzedler.grocy.feature.masterdata.impl.barcode.BarcodeItem
import com.patrickzedler.grocy.feature.masterdata.impl.barcode.ProductBarcodesViewModel
import com.patrickzedler.grocy.feature.masterdata.impl.conversion.ConversionDialog
import com.patrickzedler.grocy.feature.masterdata.impl.conversion.ConversionItem
import com.patrickzedler.grocy.feature.masterdata.impl.conversion.ConversionOwner
import com.patrickzedler.grocy.feature.masterdata.impl.conversion.ConversionsViewModel
import com.patrickzedler.grocy.feature.masterdata.impl.conversion.ResolvedConversionsDialog
import com.patrickzedler.grocy.feature.masterdata.impl.objects.FormFrame
import com.patrickzedler.grocy.feature.masterdata.impl.objects.ObjectAction
import com.patrickzedler.grocy.feature.masterdata.impl.objects.ObjectActionEvent
import com.patrickzedler.grocy.feature.masterdata.impl.objects.ObjectActionsHost
import com.patrickzedler.grocy.feature.masterdata.impl.objects.ObjectActionsViewModel
import com.patrickzedler.grocy.feature.masterdata.impl.objects.objectActionItems
import com.patrickzedler.grocy.feature.masterdata.impl.related.GrocycodeItem
import com.patrickzedler.grocy.feature.masterdata.impl.related.LabelPrintEffect
import com.patrickzedler.grocy.feature.masterdata.impl.related.LabelPrintViewModel
import com.patrickzedler.grocy.feature.masterdata.impl.related.ScannerToggleButton
import com.patrickzedler.grocy.feature.masterdata.impl.related.barcodesGroup
import com.patrickzedler.grocy.feature.masterdata.impl.related.conversionsGroup
import com.patrickzedler.grocy.feature.masterdata.impl.related.rememberGrocycodeDownload
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * What belongs to the product besides its form: barcodes, conversions, its stock entries and
 * journal, each with what acts on them.
 */
internal class ProductRelated(
    val barcodes: ProductBarcodesViewModel,
    val barcodeActions: ObjectActionsViewModel,
    val conversions: ConversionsViewModel,
    val conversionActions: ObjectActionsViewModel,
    val labels: LabelPrintViewModel,
    val onStockEntriesClick: (productId: Int) -> Unit,
    val onStockJournalClick: (productId: Int) -> Unit,
)

@Composable
internal fun ProductScreen(
    viewModel: ProductViewModel,
    frame: FormFrame,
    related: ProductRelated,
    modifier: Modifier = Modifier,
) {
    val onBackClick = frame.onBackClick
    val leaveGuard = frame.leaveGuard
    val options by viewModel.options.collectAsStateWithLifecycle()
    val barcodes by related.barcodes.items.collectAsStateWithLifecycle()
    val conversions by related.conversions.items.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showPictureDialog by rememberSaveable { mutableStateOf(false) }
    // Saveable, so the question and the pending leave survive rotating the device
    var showUnsavedDialog by rememberSaveable { mutableStateOf(false) }
    var leaveAfterSave by rememberSaveable { mutableStateOf(false) }
    // Choosing another product of the list beside the form leaves differently than back
    var pendingLeave by remember { mutableStateOf<(() -> Unit)?>(null) }
    val currentOnBackClick by rememberUpdatedState(onBackClick)
    val leave = { (pendingLeave ?: currentOnBackClick)() }
    val requestBack = {
        pendingLeave = null
        if (viewModel.hasUnsavedChanges) showUnsavedDialog = true else onBackClick()
    }

    DisposableEffect(leaveGuard, viewModel) {
        leaveGuard.onLeaveRequest = { action ->
            if (viewModel.hasUnsavedChanges) {
                pendingLeave = action
                showUnsavedDialog = true
            } else {
                action()
            }
        }
        onDispose { leaveGuard.onLeaveRequest = null }
    }
    // The back gesture and the browser's back ask too
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = viewModel.hasUnsavedChanges && !viewModel.isSaving,
        onBackCompleted = {
            pendingLeave = null
            showUnsavedDialog = true
        },
    )
    if (showUnsavedDialog) {
        UnsavedChangesDialog(
            onSave = { leaveAfterSave = viewModel.save() },
            onDiscard = leave,
            onDismissRequest = { showUnsavedDialog = false },
        )
    }

    // Barcodes and conversions belong to the saved product, a created one gets them now
    LaunchedEffect(viewModel.productId, viewModel.isNew) {
        if (!viewModel.isNew) {
            related.barcodes.showProduct(viewModel.productId)
            related.conversions.showOwner(ConversionOwner.Product(viewModel.productId))
        }
    }
    ObjectActionsHost(related.barcodeActions, snackbarHostState) {}
    LabelPrintEffect(related.labels, snackbarHostState)
    val downloadGrocycode = rememberGrocycodeDownload(snackbarHostState)
    ObjectActionsHost(related.conversionActions, snackbarHostState) {}

    ObjectActionsHost(frame.objectActions, snackbarHostState) { event ->
        val id = viewModel.productId
        val isGone = event is ObjectActionEvent.Deleted && event.id == id ||
            event is ObjectActionEvent.Merged && event.removeId == id
        // Its changes went with it, nothing to ask
        if (isGone) onBackClick()
        frame.onActionDone(event)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val leaving = leaveAfterSave
            leaveAfterSave = false
            when (event) {
                ProductEvent.Saved -> leave()
                ProductEvent.Created -> if (leaving) {
                    leave()
                } else {
                    snackbarHostState.showSnackbar(getString(Res.string.data_saved))
                }
                ProductEvent.Offline ->
                    snackbarHostState.showSnackbar(getString(Res.string.data_save_offline))
                is ProductEvent.Failed -> snackbarHostState.showSnackbar(
                    listOfNotNull(
                        getString(Res.string.data_save_failed),
                        event.serverMessage?.let {
                            getString(Res.string.data_save_failed_details, it)
                        },
                    ).joinToString("\n"),
                )
            }
        }
    }

    SegmentedListScreen(
        title = stringResource(
            if (viewModel.isNew) Res.string.product_create else Res.string.product_edit,
        ),
        modifier = modifier,
        navigationIcon = {
            // Beside the list its own back button leaves both
            if (!frame.isBesideList) NavigateBackButton(onClick = requestBack)
        },
        menuItems = frame.menuItems,
        actionMenuItems = if (viewModel.isNew) {
            emptyList()
        } else {
            objectActionItems(frame.objectActions, viewModel.productId)
        },
        pane = ScreenPane.Detail.takeIf { frame.isBesideList },
        snackbarHostState = snackbarHostState,
        floatingToolbar = { scrollBehavior, toolbarModifier ->
            FloatingToolbarWithFab(
                scrollBehavior = scrollBehavior,
                modifier = toolbarModifier,
                fabPainter = painterResource(Res.drawable.ic_rounded_save),
                fabDescription = stringResource(Res.string.action_save),
                onFabClick = { viewModel.save() },
            ) {
                // They exist once the product is saved
                FloatingToolbarIconButton(
                    painter = painterResource(Res.drawable.ic_rounded_inventory_2),
                    contentDescription = stringResource(Res.string.product_stock_entries),
                    onClick = { related.onStockEntriesClick(viewModel.productId) },
                    enabled = !viewModel.isNew,
                )
                FloatingToolbarIconButton(
                    painter = painterResource(Res.drawable.ic_rounded_receipt_long),
                    contentDescription = stringResource(Res.string.product_stock_journal),
                    onClick = { related.onStockJournalClick(viewModel.productId) },
                    enabled = !viewModel.isNew,
                )
            }
        },
    ) {
        val state = viewModel.loadState
        val currentOptions = options
        when {
            state is ProductLoadState.Ready && currentOptions != null -> {
                productForm(
                    state = state,
                    options = currentOptions,
                    viewModel = viewModel,
                    snackbarHostState = snackbarHostState,
                )
                relatedGroups(
                    viewModel = viewModel,
                    related = related,
                    config = currentOptions.config,
                    barcodes = barcodes,
                    conversions = conversions,
                    onGrocycodeDownload = downloadGrocycode,
                    onPictureClick = { showPictureDialog = true },
                )
            }

            state is ProductLoadState.NotFound -> Unit

            else -> insetItem(key = "loading") {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            }
        }
    }

    related.barcodes.dialog?.let { dialog ->
        BarcodeDialog(
            state = dialog,
            onSave = related.barcodes::save,
            onDismissRequest = related.barcodes::dismissDialog,
        )
    }
    related.conversions.dialog?.let { dialog ->
        ConversionDialog(
            state = dialog,
            onSave = related.conversions::save,
            onDismissRequest = related.conversions::dismissDialog,
        )
    }
    if (related.conversions.showsResolved) {
        val groups by related.conversions.resolvedGroups.collectAsStateWithLifecycle()
        ResolvedConversionsDialog(
            groups = groups,
            onDismissRequest = { related.conversions.showResolved(false) },
        )
    }

    if (showPictureDialog) {
        ProductPictureDialog(
            savedFileName = viewModel.pictureFileName,
            change = viewModel.pictureChange,
            loadSavedPicture = { viewModel.loadPicture(maxWidth = PICTURE_WIDTH) },
            onChange = { viewModel.pictureChange = it },
            onDismissRequest = { showPictureDialog = false },
        )
    }
}

private fun LazyListScope.productForm(
    state: ProductLoadState.Ready,
    options: ProductOptions,
    viewModel: ProductViewModel,
    snackbarHostState: SnackbarHostState,
) {
    val form = state.form
    val features = state.features
    val errors = viewModel.errors
    val units = options.quantityUnits
    val locations = options.locations.toOptions(form.locationId, form.defaultConsumeLocationId) {
        it.id to it.name
    }

    segmentedGroup(key = "general") {
        GeneralItems(state, options, viewModel, snackbarHostState)
    }

    if (features.locations || features.stores) {
        segmentedGroup(key = "location", title = Res.string.product_group_location) {
            GroupItems(
                if (features.locations) {
                    { index, count ->
                        DropdownItem(
                            label = stringResource(Res.string.product_location),
                            options = locations,
                            selectedId = form.locationId,
                            onSelectedIdChange = { form.locationId = it },
                            onUnmatchedChange = { form.setUnmatched(ProductChoice.LOCATION, it) },
                            error = fieldErrorText(
                                errors[ProductField.LOCATION],
                                Res.string.product_location_required,
                            ),
                            index = index,
                            count = count,
                        )
                    }
                } else {
                    null
                },
                if (features.locations) {
                    { index, count ->
                        DropdownItem(
                            label = stringResource(Res.string.product_consume_location),
                            options = locations,
                            selectedId = form.defaultConsumeLocationId,
                            onSelectedIdChange = { form.defaultConsumeLocationId = it },
                            onUnmatchedChange = {
                                form.setUnmatched(ProductChoice.CONSUME_LOCATION, it)
                            },
                            hint = stringResource(Res.string.product_consume_location_hint),
                            optional = true,
                            index = index,
                            count = count,
                        )
                    }
                } else {
                    null
                },
                if (features.locations && features.opening) {
                    { index, count ->
                        SegmentedSwitchItem(
                            headline = stringResource(Res.string.product_move_on_open),
                            supporting = stringResource(Res.string.product_move_on_open_hint),
                            checked = form.moveOnOpen,
                            onCheckedChange = { form.moveOnOpen = it },
                            index = index,
                            count = count,
                        )
                    }
                } else {
                    null
                },
                if (features.stores) {
                    { index, count ->
                        DropdownItem(
                            label = stringResource(Res.string.product_store),
                            options = options.stores.toOptions(form.storeId) { it.id to it.name },
                            selectedId = form.storeId,
                            onSelectedIdChange = { form.storeId = it },
                            onUnmatchedChange = { form.setUnmatched(ProductChoice.STORE, it) },
                            optional = true,
                            index = index,
                            count = count,
                        )
                    }
                } else {
                    null
                },
            )
        }
    }

    segmentedGroup(key = "stock", title = Res.string.product_group_stock) {
        val stockUnit = units.firstOrNull { it.id == form.quIdStock }
        GroupItems(
            { index, count ->
                TextItem(
                    state = form.minStockAmount,
                    label = stringResource(Res.string.product_min_stock_amount),
                    error = fieldErrorText(errors[ProductField.MIN_STOCK_AMOUNT]),
                    suffix = stockUnit?.pluralName,
                    enabled = viewModel.isMinStockEditable(options, form.parentProductId),
                    keyboardType = KeyboardType.Decimal,
                    index = index,
                    count = count,
                )
            },
            { index, count ->
                SegmentedSwitchItem(
                    headline = stringResource(Res.string.product_cumulate_min_stock),
                    supporting = stringResource(Res.string.product_cumulate_min_stock_hint),
                    checked = form.cumulateMinStockAmountOfSubProducts,
                    onCheckedChange = { form.cumulateMinStockAmountOfSubProducts = it },
                    index = index,
                    count = count,
                )
            },
            if (features.opening) {
                { index, count ->
                    SegmentedSwitchItem(
                        headline = stringResource(Res.string.product_treat_opened_as_out_of_stock),
                        supporting =
                            stringResource(Res.string.product_treat_opened_as_out_of_stock_hint),
                        checked = form.treatOpenedAsOutOfStock,
                        onCheckedChange = { form.treatOpenedAsOutOfStock = it },
                        index = index,
                        count = count,
                    )
                }
            } else {
                null
            },
        )
    }

    if (features.dueDates || features.freezing) {
        segmentedGroup(key = "due", title = Res.string.product_group_due_date) {
            DueDateItems(state, errors)
        }
    }

    segmentedGroup(key = "units", title = Res.string.product_group_quantity_units) {
        val unitOptions = units.toOptions(
            form.quIdStock, form.quIdPurchase, form.quIdConsume, form.quIdPrice,
        ) { it.id to it.name }
        val required = Res.string.product_qu_required
        GroupItems(
            { index, count ->
                DropdownItem(
                    label = stringResource(Res.string.product_qu_stock),
                    options = unitOptions,
                    selectedId = form.quIdStock,
                    onSelectedIdChange = form::changeStockUnit,
                    onUnmatchedChange = { form.setUnmatched(ProductChoice.QU_STOCK, it) },
                    hint = stringResource(Res.string.product_qu_stock_hint),
                    error = fieldErrorText(errors[ProductField.QU_STOCK], required),
                    index = index,
                    count = count,
                )
            },
            { index, count ->
                DropdownItem(
                    label = stringResource(Res.string.product_qu_purchase),
                    options = unitOptions,
                    selectedId = form.quIdPurchase,
                    onSelectedIdChange = { form.quIdPurchase = it },
                    onUnmatchedChange = { form.setUnmatched(ProductChoice.QU_PURCHASE, it) },
                    hint = stringResource(Res.string.product_qu_purchase_hint),
                    error = fieldErrorText(errors[ProductField.QU_PURCHASE], required),
                    index = index,
                    count = count,
                )
            },
            { index, count ->
                DropdownItem(
                    label = stringResource(Res.string.product_qu_consume),
                    options = unitOptions,
                    selectedId = form.quIdConsume,
                    onSelectedIdChange = { form.quIdConsume = it },
                    onUnmatchedChange = { form.setUnmatched(ProductChoice.QU_CONSUME, it) },
                    hint = stringResource(Res.string.product_qu_consume_hint),
                    error = fieldErrorText(errors[ProductField.QU_CONSUME], required),
                    index = index,
                    count = count,
                )
            },
            { index, count ->
                DropdownItem(
                    label = stringResource(Res.string.product_qu_price),
                    options = unitOptions,
                    selectedId = form.quIdPrice,
                    onSelectedIdChange = { form.quIdPrice = it },
                    onUnmatchedChange = { form.setUnmatched(ProductChoice.QU_PRICE, it) },
                    hint = stringResource(Res.string.product_qu_price_hint),
                    error = fieldErrorText(errors[ProductField.QU_PRICE], required),
                    index = index,
                    count = count,
                )
            },
        )
    }

    segmentedGroup(key = "tare", title = Res.string.product_group_tare_weight) {
        GroupItems(
            { index, count ->
                SegmentedSwitchItem(
                    headline = stringResource(Res.string.product_tare_weight_handling),
                    supporting = stringResource(Res.string.product_tare_weight_handling_hint),
                    checked = form.enableTareWeightHandling,
                    onCheckedChange = { form.enableTareWeightHandling = it },
                    index = index,
                    count = count,
                )
            },
            { index, count ->
                TextItem(
                    state = form.tareWeight,
                    label = stringResource(Res.string.product_tare_weight),
                    error = fieldErrorText(errors[ProductField.TARE_WEIGHT]),
                    suffix = units.firstOrNull { it.id == form.quIdStock }?.pluralName,
                    enabled = form.enableTareWeightHandling,
                    keyboardType = KeyboardType.Decimal,
                    index = index,
                    count = count,
                )
            },
        )
    }

    segmentedGroup(key = "more", title = Res.string.product_group_more) {
        MoreItems(state, units, errors)
    }

    if (state.userfields.isNotEmpty()) {
        segmentedGroup(key = "userfields", title = Res.string.product_userfields) {
            val userfieldErrors = viewModel.userfieldErrors
            state.userfields.forEachIndexed { index, input ->
                UserfieldItem(
                    input = input,
                    error = userfieldErrors[input.userfield.name],
                    index = index,
                    count = state.userfields.size,
                )
            }
        }
    }
}

/**
 * The right column of Grocy's product form: barcodes, conversion overrides and the picture;
 * barcodes and conversions once the product is saved.
 */
private fun LazyListScope.relatedGroups(
    viewModel: ProductViewModel,
    related: ProductRelated,
    config: ServerConfig,
    barcodes: List<BarcodeItem>,
    conversions: List<ConversionItem>,
    onGrocycodeDownload: (code: String, twoDimensional: Boolean) -> Unit,
    onPictureClick: () -> Unit,
) {
    val canAdd = !viewModel.isNew
    barcodesGroup(
        items = barcodes,
        canAdd = canAdd,
        disabledHint = Res.string.product_save_hint,
        onBarcodeClick = related.barcodes::open,
        onDeleteClick = { related.barcodeActions.request(ObjectAction.DELETE, it) },
        onAddClick = { related.barcodes.open() },
    )
    // Grocy shows a product's grocycode once it exists
    if (!viewModel.isNew) {
        val productId = viewModel.productId
        val code = Grocycode.product(productId)
        segmentedGroup(key = "grocycode", title = Res.string.grocycode_title) {
            GrocycodeItem(
                code = code,
                objectName = stringResource(Res.string.grocycode_product),
                twoDimensional = config.grocycode2D,
                onDownloadClick = { onGrocycodeDownload(code, config.grocycode2D) },
                onPrintClick = if (config.labelPrinter && config.labelPrinterRunsOnServer) {
                    { related.labels.printProduct(productId) }
                } else {
                    null
                },
                index = 0,
                count = 1,
            )
        }
    }
    conversionsGroup(
        title = Res.string.product_qu_conversions,
        items = conversions,
        canAdd = canAdd,
        disabledHint = Res.string.product_save_hint,
        onConversionClick = related.conversions::open,
        onDeleteClick = { related.conversionActions.request(ObjectAction.DELETE, it) },
        onAddClick = { related.conversions.open() },
        onShowResolvedClick = { related.conversions.showResolved(true) },
    )
    segmentedGroup(key = "picture", title = Res.string.product_picture) {
        NavigationItem(
            text = stringResource(Res.string.product_picture),
            supporting = when (viewModel.pictureChange) {
                PictureChange.Delete -> stringResource(Res.string.product_picture_will_delete)
                is PictureChange.Replace -> stringResource(Res.string.product_picture_will_upload)
                // A saved picture needs no note, the dialog shows it
                PictureChange.Keep -> if (viewModel.pictureFileName == null) {
                    stringResource(Res.string.product_picture_none)
                } else {
                    null
                }
            },
            iconPainter = painterResource(Res.drawable.ic_rounded_image),
            index = 0,
            count = 1,
            onClick = onPictureClick,
        )
    }
}

@Composable
private fun GeneralItems(
    state: ProductLoadState.Ready,
    options: ProductOptions,
    viewModel: ProductViewModel,
    snackbarHostState: SnackbarHostState,
) {
    val form = state.form
    val scope = rememberCoroutineScope()
    var showScanner by rememberSaveable { mutableStateOf(false) }
    val hasSubProducts = viewModel.hasSubProducts(options)
    val candidates = viewModel.parentCandidates(options)
    val notFound = stringResource(Res.string.product_parent_not_found)
    val notAllowed = stringResource(Res.string.product_parent_not_allowed)

    GroupItems(
        { index, count ->
            TextItem(
                state = form.name,
                label = stringResource(Res.string.product_name),
                error = fieldErrorText(
                    viewModel.errors[ProductField.NAME],
                    Res.string.product_name_required,
                ),
                index = index,
                count = count,
            )
        },
        { index, count ->
            SegmentedSwitchItem(
                headline = stringResource(Res.string.product_active),
                checked = form.active,
                onCheckedChange = { form.active = it },
                index = index,
                count = count,
            )
        },
        { index, count ->
            DropdownItem(
                label = stringResource(Res.string.product_parent),
                options = candidates.toOptions(form.parentProductId) { it.id to it.name },
                selectedId = form.parentProductId,
                onSelectedIdChange = { form.parentProductId = it },
                onUnmatchedChange = { form.setUnmatched(ProductChoice.PARENT, it) },
                hint = stringResource(Res.string.product_parent_disabled).takeIf { hasSubProducts },
                optional = true,
                enabled = !hasSubProducts,
                index = index,
                count = count,
                below = if (showScanner && !hasSubProducts) {
                    {
                        BarcodeScanner(
                            onBarcodeScanned = { barcode ->
                                val product = viewModel.productForCode(options, barcode.value)
                                when {
                                    product == null ->
                                        scope.launch { snackbarHostState.showSnackbar(notFound) }
                                    candidates.none { it.id == product.id } ->
                                        scope.launch { snackbarHostState.showSnackbar(notAllowed) }
                                    else -> {
                                        form.parentProductId = product.id
                                        showScanner = false
                                    }
                                }
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
                        onClick = { showScanner = !showScanner },
                        enabled = !hasSubProducts,
                    )
                },
            )
        },
        { index, count ->
            TextItem(
                state = form.description,
                label = stringResource(Res.string.product_description),
                multiLine = true,
                index = index,
                count = count,
            )
        },
        { index, count ->
            DropdownItem(
                label = stringResource(Res.string.product_product_group),
                options = options.productGroups
                    .filter { it.active || it.id == form.productGroupId }
                    .map { DropdownOption(it.id, it.name) },
                selectedId = form.productGroupId,
                onSelectedIdChange = { form.productGroupId = it },
                onUnmatchedChange = { form.setUnmatched(ProductChoice.PRODUCT_GROUP, it) },
                optional = true,
                index = index,
                count = count,
            )
        },
    )
}

@Composable
private fun DueDateItems(
    state: ProductLoadState.Ready,
    errors: Map<ProductField, FieldError>,
) {
    val form = state.form
    val features = state.features
    GroupItems(
        if (features.dueDates) {
            { index, count ->
                ChoiceItem(
                    label = stringResource(Res.string.product_due_type),
                    hint = stringResource(Res.string.product_due_type_hint) + "\n" +
                        stringResource(
                            if (form.dueType == DueType.BEST_BEFORE) {
                                Res.string.product_due_type_best_before_hint
                            } else {
                                Res.string.product_due_type_expiration_hint
                            },
                        ),
                    options = DueType.entries,
                    selected = form.dueType,
                    onSelect = { form.dueType = it },
                    optionLabel = {
                        stringResource(
                            if (it == DueType.BEST_BEFORE) {
                                Res.string.product_due_type_best_before
                            } else {
                                Res.string.product_due_type_expiration
                            },
                        )
                    },
                    index = index,
                    count = count,
                )
            }
        } else {
            null
        },
        if (features.dueDates) {
            { index, count ->
                DaysItem(
                    state = form.defaultDueDays,
                    label = Res.string.product_due_days,
                    hint = Res.string.product_due_days_hint,
                    error = errors[ProductField.DUE_DAYS],
                    index = index,
                    count = count,
                )
            }
        } else {
            null
        },
        if (features.dueDates && features.opening) {
            { index, count ->
                DaysItem(
                    state = form.defaultDueDaysAfterOpen,
                    label = Res.string.product_due_days_after_open,
                    hint = Res.string.product_due_days_after_open_hint,
                    error = errors[ProductField.DUE_DAYS_AFTER_OPEN],
                    index = index,
                    count = count,
                )
            }
        } else {
            null
        },
        if (features.freezing) {
            { index, count ->
                DaysItem(
                    state = form.defaultDueDaysAfterFreezing,
                    label = Res.string.product_due_days_after_freezing,
                    hint = Res.string.product_due_days_after_freezing_hint,
                    error = errors[ProductField.DUE_DAYS_AFTER_FREEZING],
                    index = index,
                    count = count,
                )
            }
        } else {
            null
        },
        if (features.freezing) {
            { index, count ->
                DaysItem(
                    state = form.defaultDueDaysAfterThawing,
                    label = Res.string.product_due_days_after_thawing,
                    hint = Res.string.product_due_days_after_thawing_hint,
                    error = errors[ProductField.DUE_DAYS_AFTER_THAWING],
                    index = index,
                    count = count,
                )
            }
        } else {
            null
        },
        if (features.freezing) {
            { index, count ->
                SegmentedSwitchItem(
                    headline = stringResource(Res.string.product_should_not_be_frozen),
                    supporting = stringResource(Res.string.product_should_not_be_frozen_hint),
                    checked = form.shouldNotBeFrozen,
                    onCheckedChange = { form.shouldNotBeFrozen = it },
                    index = index,
                    count = count,
                )
            }
        } else {
            null
        },
    )
}

@Composable
private fun DaysItem(
    state: TextFieldState,
    label: StringResource,
    hint: StringResource,
    error: FieldError?,
    index: Int,
    count: Int,
) {
    TextItem(
        state = state,
        label = stringResource(label),
        hint = stringResource(hint),
        error = fieldErrorText(error),
        keyboardType = KeyboardType.Number,
        index = index,
        count = count,
    )
}

@Composable
private fun MoreItems(
    state: ProductLoadState.Ready,
    units: List<QuantityUnit>,
    errors: Map<ProductField, FieldError>,
) {
    val form = state.form
    val features = state.features
    val stockUnit = units.firstOrNull { it.id == form.quIdStock }
    GroupItems(
        if (features.recipes) {
            { index, count ->
                SegmentedSwitchItem(
                    headline = stringResource(Res.string.product_not_check_stock_fulfillment),
                    supporting =
                        stringResource(Res.string.product_not_check_stock_fulfillment_hint),
                    checked = form.notCheckStockFulfillmentForRecipes,
                    onCheckedChange = { form.notCheckStockFulfillmentForRecipes = it },
                    index = index,
                    count = count,
                )
            }
        } else {
            null
        },
        { index, count ->
            TextItem(
                state = form.calories,
                label = stringResource(Res.string.product_energy),
                hint = stringResource(Res.string.product_energy_hint),
                error = fieldErrorText(errors[ProductField.CALORIES]),
                suffix = listOfNotNull(state.energyUnit, stockUnit?.name).joinToString(" / "),
                keyboardType = KeyboardType.Decimal,
                index = index,
                count = count,
            )
        },
        { index, count ->
            TextItem(
                state = form.quickConsumeAmount,
                label = stringResource(Res.string.product_quick_consume_amount),
                hint = stringResource(Res.string.product_quick_consume_amount_hint),
                error = fieldErrorText(errors[ProductField.QUICK_CONSUME_AMOUNT]),
                suffix = stockUnit?.pluralName,
                keyboardType = KeyboardType.Decimal,
                index = index,
                count = count,
            )
        },
        { index, count ->
            TextItem(
                state = form.quickOpenAmount,
                label = stringResource(Res.string.product_quick_open_amount),
                hint = stringResource(Res.string.product_quick_open_amount_hint),
                error = fieldErrorText(errors[ProductField.QUICK_OPEN_AMOUNT]),
                suffix = stockUnit?.pluralName,
                keyboardType = KeyboardType.Decimal,
                index = index,
                count = count,
            )
        },
        if (features.labelPrinter) {
            { index, count ->
                ChoiceItem(
                    label = stringResource(Res.string.product_stock_label),
                    hint = stringResource(Res.string.product_stock_label_hint),
                    options = StockLabelType.entries,
                    selected = form.defaultStockLabelType,
                    onSelect = { form.defaultStockLabelType = it },
                    optionLabel = {
                        stringResource(
                            when (it) {
                                StockLabelType.NONE -> Res.string.product_stock_label_none
                                StockLabelType.SINGLE -> Res.string.product_stock_label_single
                                StockLabelType.PER_UNIT -> Res.string.product_stock_label_per_unit
                            },
                        )
                    },
                    index = index,
                    count = count,
                )
            }
        } else {
            null
        },
        if (features.labelPrinter) {
            { index, count ->
                SegmentedSwitchItem(
                    headline = stringResource(Res.string.product_auto_reprint_label),
                    supporting = stringResource(Res.string.product_auto_reprint_label_hint),
                    checked = form.autoReprintStockLabel,
                    onCheckedChange = { form.autoReprintStockLabel = it },
                    index = index,
                    count = count,
                )
            }
        } else {
            null
        },
        if (features.stores) {
            { index, count ->
                ChoiceItem(
                    label = stringResource(Res.string.product_price_type),
                    hint = stringResource(Res.string.product_price_type_hint),
                    options = PurchasePriceType.entries,
                    selected = form.defaultPurchasePriceType,
                    onSelect = { form.defaultPurchasePriceType = it },
                    optionLabel = {
                        stringResource(
                            when (it) {
                                PurchasePriceType.UNSPECIFIED ->
                                    Res.string.product_price_type_unspecified
                                PurchasePriceType.UNIT_PRICE -> Res.string.product_price_type_unit
                                PurchasePriceType.TOTAL_PRICE ->
                                    Res.string.product_price_type_total
                            },
                        )
                    },
                    index = index,
                    count = count,
                )
            }
        } else {
            null
        },
        if (features.opening) {
            { index, count ->
                SegmentedSwitchItem(
                    headline = stringResource(Res.string.product_disable_open),
                    checked = form.disableOpen,
                    onCheckedChange = { form.disableOpen = it },
                    index = index,
                    count = count,
                )
            }
        } else {
            null
        },
        { index, count ->
            SegmentedSwitchItem(
                headline = stringResource(Res.string.product_hide_on_stock_overview),
                supporting = stringResource(Res.string.product_hide_on_stock_overview_hint),
                checked = form.hideOnStockOverview,
                onCheckedChange = { form.hideOnStockOverview = it },
                index = index,
                count = count,
            )
        },
        { index, count ->
            SegmentedSwitchItem(
                headline = stringResource(Res.string.product_no_own_stock),
                supporting = stringResource(Res.string.product_no_own_stock_hint),
                checked = form.noOwnStock,
                onCheckedChange = { form.noOwnStock = it },
                index = index,
                count = count,
            )
        },
    )
}

@Composable
internal fun NavigationItem(
    text: String,
    supporting: String?,
    iconPainter: Painter,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val haptic = LocalHapticFeedback.current
    SegmentedListItem(
        onClick = {
            haptic.confirm()
            onClick()
        },
        modifier = Modifier.clickCursor(enabled = enabled),
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = SegmentedListDefaults.colors(),
        enabled = enabled,
        leadingContent = {
            LeadingTrailingContentWrapper { Icon(iconPainter, contentDescription = null) }
        },
        supportingContent = supporting?.let { { Text(it) } },
        trailingContent = {
            LeadingTrailingContentWrapper {
                Icon(
                    painter = painterResource(Res.drawable.ic_rounded_arrow_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    ) {
        Text(text)
    }
}

/** Plural name for amounts, as Grocy shows the stock unit next to amount fields. */
private val QuantityUnit.pluralName: String
    get() = namePlural?.takeIf { it.isNotBlank() } ?: name

/**
 * Picker options of active objects; inactive ones only while chosen, so an existing choice
 * stays visible.
 */
private fun <T> List<T>.toOptions(
    vararg chosen: Int?,
    idAndName: (T) -> Pair<Int, String>,
): List<DropdownOption> = mapNotNull { item ->
    val (id, name) = idAndName(item)
    val active = when (item) {
        is Location -> item.active
        is Store -> item.active
        is QuantityUnit -> item.active
        is Product -> item.active
        else -> true
    }
    if (active || id in chosen) DropdownOption(id, name) else null
}

private const val PICTURE_WIDTH = 800
