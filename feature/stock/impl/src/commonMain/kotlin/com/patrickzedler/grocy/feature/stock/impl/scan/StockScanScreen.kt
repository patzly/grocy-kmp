package com.patrickzedler.grocy.feature.stock.impl.scan

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarScrollBehavior
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.ConnectedButtonGroup
import com.patrickzedler.grocy.core.design.component.FloatingToolbarIconButton
import com.patrickzedler.grocy.core.design.component.FloatingToolbarWithFab
import com.patrickzedler.grocy.core.design.component.LocalFormFocus
import com.patrickzedler.grocy.core.design.component.NavigateBackButton
import com.patrickzedler.grocy.core.design.component.RoundedTextField
import com.patrickzedler.grocy.core.design.component.SegmentedContainer
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.SegmentedListScreen
import com.patrickzedler.grocy.core.design.component.SegmentedTexts
import com.patrickzedler.grocy.core.design.component.rememberFormFocus
import com.patrickzedler.grocy.core.design.component.segmentedGroup
import com.patrickzedler.grocy.core.forms.ItemButton
import com.patrickzedler.grocy.core.forms.ItemButtons
import com.patrickzedler.grocy.core.forms.ProductFormRequest
import com.patrickzedler.grocy.core.model.stock.BookingKind
import com.patrickzedler.grocy.core.model.text.platformDecimalSeparator
import com.patrickzedler.grocy.core.productcard.LabelPrintEffect
import com.patrickzedler.grocy.core.productcard.ProductOverviewHost
import com.patrickzedler.grocy.core.productcard.StockPageActions
import com.patrickzedler.grocy.core.productcard.title
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.booking_scan_again
import com.patrickzedler.grocy.core.resources.ic_rounded_barcode_scanner
import com.patrickzedler.grocy.core.resources.ic_rounded_check
import com.patrickzedler.grocy.core.resources.ic_rounded_info
import com.patrickzedler.grocy.core.resources.product_overview
import com.patrickzedler.grocy.core.resources.stock_overview_scan
import com.patrickzedler.grocy.core.resources.stock_scan_choose
import com.patrickzedler.grocy.core.resources.stock_scan_code
import com.patrickzedler.grocy.core.resources.stock_scan_stock_amount
import com.patrickzedler.grocy.core.resources.stock_scan_unknown
import com.patrickzedler.grocy.core.scanner.BarcodeScanner
import com.patrickzedler.grocy.feature.stock.impl.booking.BookingEvents
import com.patrickzedler.grocy.feature.stock.impl.booking.BookingUnknownProductDialog
import com.patrickzedler.grocy.feature.stock.impl.booking.BookingViewModel
import com.patrickzedler.grocy.feature.stock.impl.booking.bookingForm
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Scanning first, the stock overview's main action: the camera and a field for codes typed or
 * from a barcode reader; then the product with the bookings it allows as button group; then
 * the chosen booking's form below, booked with the FAB. After a booking the camera is back for
 * the next code, the booking's message with "Undo" stays.
 */
@Composable
internal fun StockScanScreen(
    viewModel: StockScanViewModel,
    actions: StockPageActions,
    menuItems: List<AppBarMenuItem>,
    onBackClick: () -> Unit,
    onOpenProduct: (ProductFormRequest) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options by viewModel.options.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val booking = viewModel.booking
    val bookingOptions = booking?.options?.collectAsStateWithLifecycle()?.value
    val formFocus = rememberFormFocus(listState) {
        val form = booking?.form ?: return@rememberFormFocus true
        booking.book() || !form.hasInvalidFields
    }
    // The next code starts at the top with the camera
    LaunchedEffect(viewModel.code == null) {
        if (viewModel.code == null && listState.canScrollBackward) listState.animateScrollToItem(0)
    }

    LabelPrintEffect(actions.labels, snackbarHostState)
    // The booked page's messages stay until the next booking is chosen, its "Undo" too
    viewModel.previousBooking?.let { previous ->
        key(previous) {
            BookingEvents(previous, snackbarHostState, onOpenProduct, onDone = {})
        }
    }
    booking?.let { current ->
        key(current) {
            BookingEvents(
                viewModel = current,
                snackbarHostState = snackbarHostState,
                onOpenProduct = onOpenProduct,
                onDone = {},
                onBooked = viewModel::onBooked,
            )
            BookingUnknownProductDialog(current, bookingOptions, onOpenProduct)
        }
    }

    CompositionLocalProvider(LocalFormFocus provides formFocus) {
        SegmentedListScreen(
            title = stringResource(Res.string.stock_overview_scan),
            modifier = modifier,
            navigationIcon = { NavigateBackButton(onClick = onBackClick) },
            menuItems = menuItems,
            listState = listState,
            snackbarHostState = snackbarHostState,
            floatingToolbar = booking?.let { current ->
                { scrollBehavior, toolbarModifier ->
                    BookingToolbar(
                        booking = current,
                        actions = actions,
                        onSubmit = formFocus::submit,
                        scrollBehavior = scrollBehavior,
                        modifier = toolbarModifier,
                    )
                }
            },
        ) {
            val currentOptions = options ?: return@SegmentedListScreen
            val code = viewModel.code
            if (code == null) {
                scannerGroup(viewModel)
                return@SegmentedListScreen
            }
            val scanned = scannedCodeOf(code, currentOptions)
            segmentedGroup(key = "scanned") {
                ScannedItem(
                    scanned = scanned,
                    // A code assigned or a product created for it in the booking's form
                    chosenName = booking?.form?.product?.name,
                    options = currentOptions,
                    onScanAgainClick = viewModel::scanAgain,
                )
                SegmentedContainer(index = 1, count = 2) {
                    SegmentedTexts(headline = stringResource(Res.string.stock_scan_choose))
                    val kinds = offeredKinds(scanned, currentOptions)
                    ConnectedButtonGroup(
                        options = kinds.map<BookingKind, BookingKind?> { it },
                        checked = viewModel.kind,
                        onCheckedChange = { it?.let(viewModel::choose) },
                        label = { kind -> kind?.let { stringResource(it.title) }.orEmpty() },
                    )
                }
            }
            if (booking != null && bookingOptions != null) {
                bookingForm(
                    viewModel = booking,
                    options = bookingOptions,
                    showsScanMode = false,
                    // Only to assign an unknown code, a found product shows above
                    showsProductPicker = booking.form.product == null,
                )
            }
        }
    }

    ProductOverviewHost(
        overview = actions.overview,
        shoppingList = actions.shoppingList,
        labels = actions.labels,
        snackbarHostState = snackbarHostState,
        links = actions.links,
        nameOf = { id -> options?.products?.firstOrNull { it.id == id }?.name.orEmpty() },
    )
}

/** The camera, always on while waiting for a code, and the field for typed codes. */
private fun LazyListScope.scannerGroup(viewModel: StockScanViewModel) {
    segmentedGroup(key = "scanner") {
        BarcodeScanner(
            onBarcodeScanned = { barcode -> viewModel.onScanned(barcode.value) },
            shape = SegmentedListDefaults.shape(index = 0, count = 2),
        )
        SegmentedContainer(index = 1, count = 2) {
            RoundedTextField(
                state = viewModel.codeField,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.stock_scan_code)) },
                keyboardOptions = KeyboardOptions(
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done,
                ),
                onKeyboardAction = { viewModel.onCodeSubmitted() },
                // A barcode reader ends its code with Enter
                onEnterKey = {
                    viewModel.onCodeSubmitted()
                    true
                },
            )
        }
    }
}

/** The scanned code: its product with the stock amount, or that it is unknown; scan again. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ScannedItem(
    scanned: ScannedCode,
    chosenName: String?,
    options: ScanOptions,
    onScanAgainClick: () -> Unit,
) {
    val language = LocalLocale.current.toLanguageTag()
    val found = scanned as? ScannedCode.Found
    ListItem(
        modifier = Modifier.fillMaxWidth(),
        shapes = ListItemDefaults.segmentedShapes(index = 0, count = 2),
        colors = SegmentedListDefaults.colors(),
        overlineContent = { Text(scanned.code) },
        supportingContent = found?.let { product ->
            {
                val amount = stockAmountText(product, options, language, platformDecimalSeparator())
                Text(stringResource(Res.string.stock_scan_stock_amount, amount))
            }
        },
        trailingContent = {
            ItemButtons(
                buttons = listOf(
                    ItemButton(
                        painter = painterResource(Res.drawable.ic_rounded_barcode_scanner),
                        description = stringResource(Res.string.booking_scan_again),
                        onClick = onScanAgainClick,
                    ),
                ),
                menuGroups = emptyList(),
            )
        },
    ) {
        Text(
            text = found?.product?.name ?: chosenName
                ?: stringResource(Res.string.stock_scan_unknown),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun BookingToolbar(
    booking: BookingViewModel,
    actions: StockPageActions,
    onSubmit: () -> Unit,
    scrollBehavior: FloatingToolbarScrollBehavior,
    modifier: Modifier,
) {
    FloatingToolbarWithFab(
        scrollBehavior = scrollBehavior,
        modifier = modifier,
        fabPainter = painterResource(Res.drawable.ic_rounded_check),
        fabDescription = stringResource(booking.kind.title),
        onFabClick = onSubmit,
    ) {
        // Grocy's product card, once a product is chosen
        val product = booking.form.product
        FloatingToolbarIconButton(
            painter = painterResource(Res.drawable.ic_rounded_info),
            contentDescription = stringResource(Res.string.product_overview),
            onClick = { product?.let { actions.overview.show(it.id) } },
            enabled = product != null,
        )
    }
}
