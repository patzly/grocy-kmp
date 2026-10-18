@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.patrickzedler.grocy.feature.masterdata.impl.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.data.masterdata.PictureChange
import com.patrickzedler.grocy.core.design.component.DropdownOption
import com.patrickzedler.grocy.core.design.component.FloatingToolbarIconButton
import com.patrickzedler.grocy.core.design.component.FloatingToolbarWithFab
import com.patrickzedler.grocy.core.design.component.HapticTooltipBox
import com.patrickzedler.grocy.core.design.component.LeadingTrailingContentWrapper
import com.patrickzedler.grocy.core.design.component.NavigateBackButton
import com.patrickzedler.grocy.core.design.component.RoundedTextField
import com.patrickzedler.grocy.core.design.component.SegmentedContainer
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.SegmentedListScreen
import com.patrickzedler.grocy.core.design.component.SegmentedSwitchItem
import com.patrickzedler.grocy.core.design.component.SegmentedTexts
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
import com.patrickzedler.grocy.core.model.masterdata.UserfieldType
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_close_scanner
import com.patrickzedler.grocy.core.resources.action_save
import com.patrickzedler.grocy.core.resources.action_scan_barcode
import com.patrickzedler.grocy.core.resources.data_date_invalid
import com.patrickzedler.grocy.core.resources.data_datetime_invalid
import com.patrickzedler.grocy.core.resources.data_number_invalid
import com.patrickzedler.grocy.core.resources.data_save_failed
import com.patrickzedler.grocy.core.resources.data_save_failed_details
import com.patrickzedler.grocy.core.resources.data_save_offline
import com.patrickzedler.grocy.core.resources.data_saved
import com.patrickzedler.grocy.core.resources.ic_rounded_arrow_right
import com.patrickzedler.grocy.core.resources.ic_rounded_barcode
import com.patrickzedler.grocy.core.resources.ic_rounded_barcode_scanner
import com.patrickzedler.grocy.core.resources.ic_rounded_close
import com.patrickzedler.grocy.core.resources.ic_rounded_conversion_path
import com.patrickzedler.grocy.core.resources.ic_rounded_image
import com.patrickzedler.grocy.core.resources.ic_rounded_inventory_2
import com.patrickzedler.grocy.core.resources.ic_rounded_receipt_long
import com.patrickzedler.grocy.core.resources.ic_rounded_save
import com.patrickzedler.grocy.core.resources.product_active
import com.patrickzedler.grocy.core.resources.product_auto_reprint_label
import com.patrickzedler.grocy.core.resources.product_auto_reprint_label_hint
import com.patrickzedler.grocy.core.resources.product_barcodes
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
import com.patrickzedler.grocy.core.resources.product_group_related
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
import com.patrickzedler.grocy.core.resources.product_userfield_file
import com.patrickzedler.grocy.core.resources.product_userfield_link
import com.patrickzedler.grocy.core.resources.product_userfield_required
import com.patrickzedler.grocy.core.resources.product_userfield_title
import com.patrickzedler.grocy.core.resources.product_userfields
import com.patrickzedler.grocy.core.scanner.BarcodeScanner
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ProductScreen(
    viewModel: ProductViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val options by viewModel.options.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showPictureDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ProductEvent.Saved -> onBackClick()
                ProductEvent.Created ->
                    snackbarHostState.showSnackbar(getString(Res.string.data_saved))
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
        navigationIcon = { NavigateBackButton(onClick = onBackClick) },
        snackbarHostState = snackbarHostState,
        floatingToolbar = { scrollBehavior, toolbarModifier ->
            FloatingToolbarWithFab(
                scrollBehavior = scrollBehavior,
                modifier = toolbarModifier,
                fabPainter = painterResource(Res.drawable.ic_rounded_save),
                fabDescription = stringResource(Res.string.action_save),
                onFabClick = viewModel::save,
            ) {
                // Their screens follow, they exist once the product is saved
                FloatingToolbarIconButton(
                    painter = painterResource(Res.drawable.ic_rounded_inventory_2),
                    contentDescription = stringResource(Res.string.product_stock_entries),
                    onClick = {},
                    enabled = !viewModel.isNew,
                )
                FloatingToolbarIconButton(
                    painter = painterResource(Res.drawable.ic_rounded_receipt_long),
                    contentDescription = stringResource(Res.string.product_stock_journal),
                    onClick = {},
                    enabled = !viewModel.isNew,
                )
            }
        },
    ) {
        val state = viewModel.loadState
        val currentOptions = options
        when {
            state is ProductLoadState.Ready && currentOptions != null -> productForm(
                state = state,
                options = currentOptions,
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                onPictureClick = { showPictureDialog = true },
            )

            state is ProductLoadState.NotFound -> Unit

            else -> insetItem(key = "loading") {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            }
        }
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
    onPictureClick: () -> Unit,
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

    segmentedGroup(key = "related", title = Res.string.product_group_related) {
        val notSaved = stringResource(Res.string.product_save_hint).takeIf { viewModel.isNew }
        // Their screens follow; until then the items show where they will be
        NavigationItem(
            text = stringResource(Res.string.product_barcodes),
            supporting = notSaved,
            iconPainter = painterResource(Res.drawable.ic_rounded_barcode),
            index = 0,
            count = 3,
            enabled = !viewModel.isNew,
            onClick = {},
        )
        NavigationItem(
            text = stringResource(Res.string.product_qu_conversions),
            supporting = notSaved,
            iconPainter = painterResource(Res.drawable.ic_rounded_conversion_path),
            index = 1,
            count = 3,
            enabled = !viewModel.isNew,
            onClick = {},
        )
        NavigationItem(
            text = stringResource(Res.string.product_picture),
            supporting = when (viewModel.pictureChange) {
                PictureChange.Delete -> stringResource(Res.string.product_picture_will_delete)
                is PictureChange.Replace -> stringResource(Res.string.product_picture_will_upload)
                PictureChange.Keep -> viewModel.pictureFileName?.let { null }
                    ?: stringResource(Res.string.product_picture_none)
            },
            iconPainter = painterResource(Res.drawable.ic_rounded_image),
            index = 2,
            count = 3,
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
    val haptic = LocalHapticFeedback.current
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
                    val description = stringResource(
                        if (showScanner) {
                            Res.string.action_close_scanner
                        } else {
                            Res.string.action_scan_barcode
                        },
                    )
                    HapticTooltipBox(text = description) {
                        IconButton(
                            onClick = {
                                haptic.confirm()
                                showScanner = !showScanner
                            },
                            enabled = !hasSubProducts,
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (showScanner) {
                                        Res.drawable.ic_rounded_close
                                    } else {
                                        Res.drawable.ic_rounded_barcode_scanner
                                    },
                                ),
                                contentDescription = description,
                            )
                        }
                    }
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
private fun UserfieldItem(
    input: UserfieldInput,
    error: UserfieldError?,
    index: Int,
    count: Int,
) {
    val haptic = LocalHapticFeedback.current
    val field = input.userfield
    val errorText = when (error) {
        null -> null
        UserfieldError.REQUIRED -> stringResource(Res.string.product_userfield_required)
        UserfieldError.INVALID_NUMBER -> stringResource(Res.string.data_number_invalid, "0", "4")
        UserfieldError.INVALID_DATE -> stringResource(Res.string.data_date_invalid)
        UserfieldError.INVALID_DATETIME -> stringResource(Res.string.data_datetime_invalid)
    }
    when (field.type) {
        UserfieldType.CHECKBOX -> SegmentedSwitchItem(
            headline = field.caption,
            supporting = errorText,
            checked = input.checked,
            onCheckedChange = { input.checked = it },
            index = index,
            count = count,
        )

        UserfieldType.PRESET_LIST -> DropdownItem(
            label = field.caption,
            options = field.options.mapIndexed { optionIndex, option ->
                DropdownOption(optionIndex, option)
            },
            selectedId = field.options.indexOf(input.selectedOption).takeIf { it >= 0 },
            onSelectedIdChange = { id -> input.selectedOption = id?.let { field.options[it] } },
            error = errorText,
            optional = true,
            index = index,
            count = count,
        )

        UserfieldType.PRESET_CHECKLIST -> SegmentedContainer(index = index, count = count) {
            SegmentedTexts(headline = field.caption, supporting = errorText)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                field.options.forEach { option ->
                    val selected = option in input.checklist
                    FilterChip(
                        selected = selected,
                        onClick = {
                            haptic.confirm()
                            input.checklist = if (selected) {
                                input.checklist - option
                            } else {
                                input.checklist + option
                            }
                        },
                        label = { Text(option) },
                    )
                }
            }
        }

        UserfieldType.LINK_WITH_TITLE -> SegmentedContainer(index = index, count = count) {
            SegmentedTexts(headline = field.caption, supporting = errorText)
            Spacer(Modifier.height(8.dp))
            RoundedTextField(
                state = input.title,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.product_userfield_title)) },
            )
            Spacer(Modifier.height(8.dp))
            RoundedTextField(
                state = input.link,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.product_userfield_link)) },
            )
        }

        UserfieldType.FILE, UserfieldType.IMAGE -> SegmentedContainer(index, count) {
            SegmentedTexts(
                headline = field.caption,
                supporting = stringResource(Res.string.product_userfield_file),
            )
        }

        else -> TextItem(
            state = input.text,
            label = field.caption,
            error = errorText,
            hint = when (field.type) {
                UserfieldType.DATE -> "YYYY-MM-DD"
                UserfieldType.DATETIME -> "YYYY-MM-DD HH:MM:SS"
                else -> null
            },
            keyboardType = when (field.type) {
                UserfieldType.NUMBER_INTEGRAL -> KeyboardType.Number
                UserfieldType.NUMBER_DECIMAL, UserfieldType.NUMBER_CURRENCY -> KeyboardType.Decimal
                UserfieldType.LINK -> KeyboardType.Uri
                else -> KeyboardType.Text
            },
            multiLine = field.type == UserfieldType.TEXT_MULTI_LINE,
            index = index,
            count = count,
        )
    }
}

@Composable
private fun NavigationItem(
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
