package com.patrickzedler.grocy.feature.stock.impl.booking

import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.stock.StockRepository
import com.patrickzedler.grocy.core.forms.ChangeError
import com.patrickzedler.grocy.core.forms.changeErrorOf
import com.patrickzedler.grocy.core.forms.changedValues
import com.patrickzedler.grocy.core.forms.productAmountUnits
import com.patrickzedler.grocy.core.forms.userfieldInputs
import com.patrickzedler.grocy.core.forms.validationErrors
import com.patrickzedler.grocy.core.model.masterdata.Location
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.ProductBarcode
import com.patrickzedler.grocy.core.model.masterdata.PurchasePriceType
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.ResolvedQuantityUnitConversion
import com.patrickzedler.grocy.core.model.masterdata.StockAmount
import com.patrickzedler.grocy.core.model.masterdata.Store
import com.patrickzedler.grocy.core.model.masterdata.Userfield
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.server.UserSettingValue
import com.patrickzedler.grocy.core.model.server.UserSettings
import com.patrickzedler.grocy.core.model.stock.BookingKind
import com.patrickzedler.grocy.core.model.stock.BookingResult
import com.patrickzedler.grocy.core.model.stock.ConsumeBooking
import com.patrickzedler.grocy.core.model.stock.InventoryBooking
import com.patrickzedler.grocy.core.model.stock.OpenBooking
import com.patrickzedler.grocy.core.model.stock.PurchaseBooking
import com.patrickzedler.grocy.core.model.stock.TransferBooking
import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.core.model.text.formatDecimal
import com.patrickzedler.grocy.core.model.text.isoDateOf
import com.patrickzedler.grocy.core.model.text.parseDecimal
import com.patrickzedler.grocy.core.model.text.platformDecimalSeparator
import com.patrickzedler.grocy.core.model.text.todayEpochDay
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What a stock page opens with, from its route. */
internal data class BookingPreset(
    val productId: Int? = null,
    /** In the stock unit, shown in [quId] (a shopping list item's). */
    val amount: Double? = null,
    val quId: Int? = null,
    val locationId: Int? = null,
    val stockId: String? = null,
    /** The shopping list item this purchase puts into stock, removed once it is booked. */
    val shoppingListItemId: Int? = null,
    /** "Add all list items to stock" books item [shoppingListPosition] of [shoppingListCount]. */
    val shoppingListPosition: Int? = null,
    val shoppingListCount: Int? = null,
)

/**
 * A purchase that puts a shopping list item into stock, explained on the page; item [position]
 * of [count] while "Add all list items to stock" books one after the other.
 */
internal data class ShoppingListItemPurchase(val position: Int?, val count: Int?)

/** What a booking says afterwards, Grocy's messages; amounts in the stock unit. */
internal sealed interface BookingEvent {
    data class Booked(
        val kind: BookingKind,
        val transactionId: String?,
        val amount: Double,
        val unit: QuantityUnit?,
        val productName: String,
        val fromLocation: String? = null,
        val toLocation: String? = null,
    ) : BookingEvent

    data class Opened(
        val transactionId: String?,
        val amount: Double,
        val unit: QuantityUnit?,
        val productName: String,
        /** The location the opened part moved to (`move_on_open`). */
        val movedTo: String?,
    ) : BookingEvent

    /** "This is due earlier than already in stock items". */
    data object DueEarlier : BookingEvent

    /** Moved into a freezer, [shouldNotBeFrozen] warns about it. */
    data class Frozen(val shouldNotBeFrozen: Boolean) : BookingEvent

    data object Thawed : BookingEvent

    /** "This product is not in stock". */
    data object NotOffered : BookingEvent

    data object ScanModeIncomplete : BookingEvent

    data class Failed(val error: ChangeError) : BookingEvent

    data object Undone : BookingEvent

    data class UndoFailed(val error: ChangeError) : BookingEvent

    /** The shopping list item is in stock now, the page is done. */
    data object Done : BookingEvent
}

/**
 * One of Grocy's stock pages ([kind]): Purchase, Consume (with "Mark as opened"), Transfer or
 * Inventory, with the prefills of Grocy's pages when a product is chosen. Online only: the
 * product's stock is loaded from the server and every booking is the server's.
 *
 * @param removeShoppingListItem removes the item of a purchase from a shopping list item.
 */
internal class BookingViewModel(
    val kind: BookingKind,
    private val masterData: MasterDataRepository,
    private val stock: StockRepository,
    private val preset: BookingPreset = BookingPreset(),
    private val removeShoppingListItem: (suspend (Int) -> Unit)? = null,
    private val today: () -> Long = ::todayEpochDay,
    private val decimalSeparator: Char = platformDecimalSeparator(),
) : ViewModel() {

    /** Set while this purchase puts a shopping list item into stock. */
    val shoppingListItem: ShoppingListItemPurchase? = preset.takeIf {
        kind == BookingKind.PURCHASE && it.shoppingListItemId != null
    }?.let { ShoppingListItemPurchase(it.shoppingListPosition, it.shoppingListCount) }

    val options: StateFlow<BookingOptions?> = combine(
        combine(
            masterData.products,
            masterData.stock,
            masterData.productBarcodes,
            masterData.locations,
            masterData.stores,
            ::Catalog,
        ),
        combine(
            masterData.quantityUnits,
            masterData.resolvedQuantityUnitConversions,
            masterData.serverConfig,
            masterData.userSettings,
            masterData.userfields(MasterDataRepository.STOCK),
            ::Setup,
        ),
    ) { catalog, setup ->
        BookingOptions(
            products = catalog.products,
            stock = catalog.stock,
            barcodes = catalog.barcodes,
            locations = catalog.locations,
            stores = catalog.stores,
            units = setup.units,
            resolved = setup.resolved,
            config = setup.config,
            settings = setup.settings,
            userfields = setup.userfields,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val form = BookingForm(isoDateOf(today()))

    /** Grocy's recipes for the consume page's statistics, empty until (or unless) loaded. */
    var recipes by mutableStateOf<Map<Int, String>>(emptyMap())
        private set

    /** A code that names no product, for Grocy's "Create or assign product" question. */
    var unknownCode by mutableStateOf<String?>(null)
        private set

    private val eventChannel = Channel<BookingEvent>(Channel.BUFFERED)
    val events: Flow<BookingEvent> = eventChannel.receiveAsFlow()

    private var contextJob: Job? = null

    /** Book as soon as the chosen product's stock is loaded (scan mode, shopping list flow). */
    private var submitWhenReady = false

    init {
        viewModelScope.launch {
            val options = options.filterNotNull().first()
            form.userfields = userfieldInputs(options.userfields, values = null)
            if (kind == BookingKind.CONSUME && options.config.recipes) loadRecipes()
            preset.productId?.let { id ->
                options.products.firstOrNull { it.id == id }?.let { product ->
                    choose(options, product, stockId = preset.stockId)
                }
            }
        }
    }

    /** The products this page offers. */
    fun offeredProducts(options: BookingOptions) =
        bookableProducts(kind, options.products, options.stock)

    /** Grocy's scan mode of this page (purchase and consume only). */
    fun isScanMode(options: BookingOptions): Boolean = when (kind) {
        BookingKind.PURCHASE -> options.settings.scanModePurchase
        BookingKind.CONSUME -> options.settings.scanModeConsume
        else -> false
    }

    /** Turns Grocy's scan mode of this page on or off; saved for the user like in Grocy. */
    fun setScanMode(enabled: Boolean) {
        val key = when (kind) {
            BookingKind.PURCHASE -> "scan_mode_purchase_enabled"
            BookingKind.CONSUME -> "scan_mode_consume_enabled"
            else -> return
        }
        viewModelScope.launch {
            changeErrorOf { masterData.saveUserSetting(key, UserSettingValue.Flag(enabled)) }
                ?.let { eventChannel.send(BookingEvent.Failed(it)) }
        }
    }

    /** The product chosen in the picker, null when it was cleared. */
    fun selectProduct(productId: Int?) {
        val options = options.value ?: return
        if (productId == form.product?.id) return
        val product = options.products.firstOrNull { it.id == productId }
        if (product == null) {
            clearProduct()
            return
        }
        form.usedBarcode = null
        choose(options, product)
    }

    /**
     * A typed or scanned [code], like Grocy's product picker: a product's barcode or grocycode
     * chooses it (with the barcode's defaults); in [scanMode] the page books by itself once
     * every field is filled.
     */
    fun onCode(code: String, scanMode: Boolean) {
        val options = options.value ?: return
        when (
            val match = matchCode(
                code,
                offeredProducts(options),
                options.products,
                options.barcodes,
            )
        ) {
            is CodeMatch.Found -> {
                form.usedBarcode = match.barcode
                choose(options, match.product, match.barcode, match.stockId, scanMode)
            }

            CodeMatch.NotOffered -> {
                clearProduct()
                viewModelScope.launch { eventChannel.send(BookingEvent.NotOffered) }
            }

            CodeMatch.Unknown -> unknownCode = code.trim()
        }
    }

    fun dismissUnknownCode() {
        unknownCode = null
    }

    /**
     * Grocy's "Add as barcode to existing product": the user chooses the product, the booking
     * adds the barcode to it.
     */
    fun assignUnknownCode() {
        form.pendingBarcode = unknownCode
        unknownCode = null
        clearProduct()
    }

    /** Grocy's "Add as new product": the barcode is added to the product once it is booked. */
    fun keepUnknownCodeForNewProduct() {
        form.pendingBarcode = unknownCode
        unknownCode = null
    }

    fun setUnit(quId: Int?) {
        form.quId = quId
    }

    /** The location of a purchase or inventory, the source of a consumption or transfer. */
    fun setLocation(locationId: Int?) {
        val options = options.value ?: return
        form.locationId = locationId
        form.stockId = null
        form.useSpecificEntry = false
        val product = form.product ?: return
        when (kind) {
            BookingKind.PURCHASE -> form.dueDate.setTextAndPlaceCursorAtEnd(
                purchaseDueDate(
                    current = form.dueDate.text.toString(),
                    product = product,
                    location = options.locations.firstOrNull { it.id == locationId },
                    freezing = options.config.stockProductFreezing,
                    today = today(),
                ),
            )

            BookingKind.TRANSFER -> if (form.toLocationId == locationId) form.toLocationId = null
            else -> Unit
        }
    }

    fun setUseSpecificEntry(use: Boolean) {
        form.useSpecificEntry = use
        if (!use) form.stockId = null
    }

    /** The due date "never overdue" (Grocy's 2999-12-31) or back to an empty field. */
    fun setNeverOverdue(never: Boolean) {
        if (never) {
            form.dueDate.setTextAndPlaceCursorAtEnd(NEVER_OVERDUE_DATE)
        } else {
            form.dueDate.clearText()
        }
    }

    /** Books the page's transaction (consume: "Consume"); false when the fields are wrong. */
    fun book(): Boolean = submit(open = false)

    /** Grocy's "Mark as opened" of the consume page. */
    fun markAsOpened(): Boolean = submit(open = true)

    /** Undoes the transaction of a booking, from its message's "Undo". */
    fun undo(transactionId: String) {
        viewModelScope.launch {
            val withShoppingLists = options.value?.config?.shoppingList == true
            val error = changeErrorOf { stock.undoTransaction(transactionId, withShoppingLists) }
            eventChannel.send(error?.let { BookingEvent.UndoFailed(it) } ?: BookingEvent.Undone)
            form.product?.let { loadContext(it.id, form.locationId, null) }
        }
    }

    /** Whether "Mark as opened" can open the [amount] typed in the chosen unit now. */
    fun canOpen(options: BookingOptions): Boolean {
        val product = form.product ?: return false
        val context = form.context as? ProductContext.Ready ?: return false
        if (!options.config.stockProductOpenedTracking || product.disableOpen ||
            product.enableTareWeightHandling
        ) {
            return false
        }
        val available = availableStock(context)
        val unopened = if (form.stockId != null) {
            context.entries.firstOrNull { it.stockId == form.stockId }
                ?.takeIf { !it.open }?.amount ?: 0.0
        } else {
            available.unopened
        }
        val amount = parseDecimal(form.amount.text.toString())?.let { stockAmountOf(it) }
        return unopened > 0 && (amount == null || amount <= unopened + EPSILON)
    }

    /** The allowed amounts of the amount field in the chosen unit, null without product. */
    fun limits(options: BookingOptions): AmountLimits? {
        val product = form.product ?: return null
        val context = form.context as? ProductContext.Ready
        val available = context?.let { ready ->
            form.stockId?.let { id -> ready.entries.firstOrNull { it.stockId == id }?.amount }
                ?: availableStock(ready).amount
        }
        return amountLimits(
            kind = kind,
            product = product,
            details = context?.details,
            factor = form.unit?.factor ?: 1.0,
            decimals = options.settings.stockDecimalPlacesAmounts,
            available = available,
            exactAmount = form.exactAmount,
        )
    }

    /** What an inventory of the typed amount changes, null without a valid amount. */
    fun inventoryChange(): InventoryChange? {
        val product = form.product ?: return null
        val details = form.details ?: return null
        val amount = parseDecimal(form.amount.text.toString())?.let { stockAmountOf(it) }
            ?: return null
        val tare = if (product.enableTareWeightHandling) product.tareWeight else 0.0
        return inventoryChange(amount, details.stockAmount, tare)
    }

    /** The typed amount in the stock unit. */
    fun stockAmountOf(displayAmount: Double): Double = displayAmount / (form.unit?.factor ?: 1.0)

    private fun availableStock(context: ProductContext.Ready): LocationStock =
        locationStockOf(context.entries, form.locationId.takeIf { trackLocations() })

    private fun trackLocations(): Boolean = options.value?.config?.stockLocationTracking == true

    private fun choose(
        options: BookingOptions,
        product: Product,
        barcode: ProductBarcode? = null,
        stockId: String? = null,
        scanMode: Boolean = false,
    ) {
        form.product = product
        form.isProductUnmatched = false
        form.errors = emptyMap()
        val units =
            productAmountUnits(product.id, product.quIdStock, options.resolved, options.units)
        form.units = units
        form.stockUnit = units.firstOrNull { it.unit.id == product.quIdStock }?.unit
        val unitId = when (kind) {
            BookingKind.PURCHASE ->
                if (product.enableTareWeightHandling) product.quIdStock else product.quIdPurchase
            BookingKind.CONSUME -> product.quIdConsume
            BookingKind.TRANSFER, BookingKind.INVENTORY -> product.quIdStock
        }
        form.quId = units.firstOrNull { it.unit.id == unitId }?.unit?.id ?: product.quIdStock
        val settings = options.settings
        val amount = when (kind) {
            BookingKind.PURCHASE -> settings.stockDefaultPurchaseAmount.takeIf { it > 0 }
            BookingKind.CONSUME -> if (settings.stockDefaultConsumeAmountUseQuickConsumeAmount) {
                product.quickConsumeAmount * (form.unit?.factor ?: 1.0)
            } else {
                settings.stockDefaultConsumeAmount
            }
            // Grocy's transfer amount setting does not exist, the inventory shows the stock
            BookingKind.TRANSFER, BookingKind.INVENTORY -> null
        }
        setAmount(amount)
        when (kind) {
            BookingKind.PURCHASE, BookingKind.INVENTORY -> prefillAddition(options, product)
            BookingKind.CONSUME, BookingKind.TRANSFER -> {
                form.locationId = null
                form.toLocationId = null
                form.stockId = null
                form.useSpecificEntry = false
            }
        }
        if (barcode != null) applyBarcode(barcode)
        // Grocy's shopping list flow: the item's amount in its unit
        if (preset.productId == product.id && preset.amount != null) {
            preset.quId?.let { id -> units.firstOrNull { it.unit.id == id } }?.let {
                form.quId = it.unit.id
            }
            setAmount(preset.amount * (form.unit?.factor ?: 1.0))
        }
        // Scan mode books one unit, a barcode's amount stays
        if (scanMode && barcode?.amount == null) setAmount(1.0)
        submitWhenReady = scanMode || (
            preset.shoppingListItemId != null && preset.productId == product.id &&
                settings.shoppingListToStockAutoSubmit
            )
        loadContext(product.id, preset.locationId, stockId ?: preset.stockId)
    }

    /** Grocy's prefills of what a purchase or an inventory adds. */
    private fun prefillAddition(
        options: BookingOptions,
        product: Product,
    ) {
        val config = options.config
        form.storeId = product.storeId
        if (config.stockLocationTracking) form.locationId = product.locationId
        if (config.stockBestBeforeDateTracking) {
            val location = options.locations.firstOrNull { it.id == form.locationId }
            val date = if (kind == BookingKind.PURCHASE) {
                purchaseDueDate(
                    current = form.dueDate.text.toString(),
                    product = product,
                    location = location,
                    freezing = config.stockProductFreezing,
                    today = today(),
                )
            } else {
                dueDateAfter(product.defaultDueDays, today()).orEmpty()
            }
            form.dueDate.setTextAndPlaceCursorAtEnd(date)
        }
        form.labelType = product.defaultStockLabelType.value
        when (product.defaultPurchasePriceType) {
            PurchasePriceType.UNIT_PRICE -> form.isTotalPrice = false
            PurchasePriceType.TOTAL_PRICE -> form.isTotalPrice = true
            PurchasePriceType.UNSPECIFIED -> Unit
        }
        form.price.clearText()
    }

    /** A barcode's amount, unit, store, last price (as total price) and note, like Grocy. */
    private fun applyBarcode(barcode: ProductBarcode) {
        barcode.quId?.let { id ->
            if (form.units.any { it.unit.id == id }) form.quId = id
        }
        barcode.amount?.let { setAmount(it) }
        if (kind == BookingKind.PURCHASE || kind == BookingKind.INVENTORY) {
            barcode.storeId?.let { form.storeId = it }
            barcode.note?.let { form.note.setTextAndPlaceCursorAtEnd(it) }
        }
        if (kind == BookingKind.PURCHASE) {
            barcode.lastPrice?.let {
                form.price.setTextAndPlaceCursorAtEnd(
                    formatDecimal(it, options.value?.settings?.stockDecimalPlacesPricesInput ?: 2),
                )
                form.isTotalPrice = true
            }
        }
    }

    private fun setAmount(amount: Double?) {
        val decimals = options.value?.settings?.stockDecimalPlacesAmounts ?: DEFAULT_DECIMALS
        if (amount == null) {
            form.amount.clearText()
        } else {
            form.amount.setTextAndPlaceCursorAtEnd(
                formatDecimal(amount, decimals, decimalSeparator),
            )
        }
    }

    /** Loads what the server says about the product; with [stockId] that entry is chosen. */
    private fun loadContext(productId: Int, locationId: Int?, stockId: String?) {
        contextJob?.cancel()
        form.context = ProductContext.Loading
        contextJob = viewModelScope.launch {
            var context: ProductContext.Ready? = null
            val error = changeErrorOf {
                val details = stock.productStockDetails(productId)
                context = when (kind) {
                    BookingKind.CONSUME, BookingKind.TRANSFER -> {
                        val subProducts = kind == BookingKind.CONSUME
                        ProductContext.Ready(
                            details = details,
                            locationAmounts = stock.productLocationAmounts(productId, subProducts),
                            entries = stock.productStockEntries(productId, subProducts),
                        )
                    }
                    else -> ProductContext.Ready(details)
                }
            }
            val ready = context
            if (form.product?.id != productId) return@launch
            if (ready == null) {
                form.context = ProductContext.Failed(error ?: ChangeError.Failed(null))
                submitWhenReady = false
                return@launch
            }
            form.context = ready
            applyContext(ready, locationId, stockId)
            if (submitWhenReady) {
                submitWhenReady = false
                if (!submit(open = false)) eventChannel.send(BookingEvent.ScanModeIncomplete)
            }
        }
    }

    private fun applyContext(context: ProductContext.Ready, locationId: Int?, stockId: String?) {
        val options = options.value ?: return
        val product = form.product ?: return
        val details = context.details
        val factor = form.unit?.factor ?: 1.0
        when (kind) {
            BookingKind.PURCHASE, BookingKind.INVENTORY -> {
                // A barcode's store wins over the last one, like on Grocy's page
                if (form.usedBarcode?.storeId == null) {
                    details.lastStoreId?.let { form.storeId = it }
                }
                if (form.price.text.isEmpty()) {
                    details.lastPrice?.takeIf { it > 0 }?.let {
                        form.price.setTextAndPlaceCursorAtEnd(
                            formatDecimal(
                                it / factor,
                                options.settings.stockDecimalPlacesPricesDisplay,
                                decimalSeparator,
                            ),
                        )
                    }
                }
                if (kind == BookingKind.INVENTORY && form.usedBarcode?.amount == null) {
                    setAmount(details.stockAmount * factor)
                }
            }

            BookingKind.CONSUME, BookingKind.TRANSFER -> {
                val entry = stockId?.let { id -> context.entries.firstOrNull { it.stockId == id } }
                form.locationId = when {
                    !options.config.stockLocationTracking -> null
                    entry != null -> entry.locationId
                    locationId != null &&
                        context.locationAmounts.any { it.locationId == locationId } -> locationId
                    kind == BookingKind.CONSUME -> consumeLocation(product, context.locationAmounts)
                    else -> transferSource(product, context.locationAmounts)
                }
                if (entry != null) {
                    form.useSpecificEntry = true
                    form.stockId = entry.stockId
                    // A stock entry's grocycode consumes or moves the whole entry
                    setAmount(entry.amount * factor)
                }
            }
        }
    }

    private fun submit(open: Boolean): Boolean {
        val options = options.value ?: return false
        if (form.isBooking) return true
        val errors = validateBooking(input(options))
        form.errors = errors
        val userfieldErrors = if (hasAdditions()) form.userfields.validationErrors() else emptyMap()
        form.userfieldErrors = userfieldErrors
        val context = form.context as? ProductContext.Ready
        if (errors.isNotEmpty() || userfieldErrors.isNotEmpty() || form.hasUnmatchedChoice ||
            context == null
        ) {
            return false
        }
        val product = form.product ?: return false
        val displayAmount = parseDecimal(form.amount.text.toString()) ?: return false
        val amount = stockAmountOf(displayAmount)
        form.isBooking = true
        viewModelScope.launch {
            var result: BookingResult? = null
            val error = changeErrorOf {
                result = when {
                    open -> stock.open(product.id, openBooking(amount))
                    kind == BookingKind.PURCHASE -> stock.purchase(
                        product.id,
                        purchaseBooking(options, amount, displayAmount),
                        form.userfields.changedValues(),
                    )
                    kind == BookingKind.CONSUME -> stock.consume(product.id, consumeBooking(amount))
                    kind == BookingKind.TRANSFER ->
                        stock.transfer(product.id, transferBooking(amount))
                    else -> stock.inventory(
                        product.id,
                        inventoryBooking(options, amount),
                        form.userfields.changedValues(),
                    )
                }
            }
            form.isBooking = false
            val booked = result
            if (booked == null) {
                eventChannel.send(BookingEvent.Failed(error ?: ChangeError.Failed(null)))
                return@launch
            }
            afterBooking(options, product, context, amount, booked, open)
        }
        return true
    }

    private suspend fun afterBooking(
        options: BookingOptions,
        product: Product,
        context: ProductContext.Ready,
        amount: Double,
        result: BookingResult,
        open: Boolean,
    ) {
        saveBarcodes(product.id)
        val tare = if (product.enableTareWeightHandling) product.tareWeight else 0.0
        val details = context.details
        val locationName = { id: Int? -> options.locations.firstOrNull { it.id == id }?.name }
        val event = when {
            open -> BookingEvent.Opened(
                transactionId = result.transactionId,
                amount = amount,
                unit = form.stockUnit,
                productName = product.name,
                movedTo = product.defaultConsumeLocationId
                    ?.takeIf { product.moveOnOpen }
                    ?.let(locationName),
            )
            else -> BookingEvent.Booked(
                kind = kind,
                transactionId = result.transactionId,
                amount = when (kind) {
                    BookingKind.PURCHASE ->
                        if (tare > 0) amount - details.stockAmount - tare else amount
                    BookingKind.CONSUME -> if (tare > 0 && !form.exactAmount) {
                        abs(amount - (tare + details.stockAmount))
                    } else {
                        amount
                    }
                    BookingKind.TRANSFER -> amount
                    BookingKind.INVENTORY -> amount - tare
                },
                unit = form.stockUnit,
                productName = product.name,
                fromLocation = locationName(form.locationId),
                toLocation = locationName(form.toLocationId),
            )
        }
        eventChannel.send(event)
        if (kind == BookingKind.PURCHASE && !open) {
            val dueDate = form.dueDate.text.toString()
            val next = details.nextDueDate
            if (options.config.stockBestBeforeDateTracking &&
                options.settings.showWarningOnPurchaseWhenDueDateIsEarlierThanNext &&
                next != null && dueDate.isNotBlank() && dueDate < next
            ) {
                eventChannel.send(BookingEvent.DueEarlier)
            }
        }
        if (kind == BookingKind.TRANSFER) {
            val from = options.locations.firstOrNull { it.id == form.locationId }
            val to = options.locations.firstOrNull { it.id == form.toLocationId }
            if (from != null && to != null) {
                if (!from.isFreezer && to.isFreezer) {
                    eventChannel.send(BookingEvent.Frozen(product.shouldNotBeFrozen))
                } else if (from.isFreezer && !to.isFreezer) {
                    eventChannel.send(BookingEvent.Thawed)
                }
            }
        }
        val itemId = preset.shoppingListItemId
        if (itemId != null && kind == BookingKind.PURCHASE && product.id == preset.productId) {
            changeErrorOf { removeShoppingListItem?.invoke(itemId) }
            eventChannel.send(BookingEvent.Done)
            return
        }
        reset(open)
    }

    /** The barcode of Grocy's "Add as barcode" flow, and the used barcode's last price. */
    private suspend fun saveBarcodes(productId: Int) {
        form.pendingBarcode?.let { code ->
            val withDefaults = kind == BookingKind.PURCHASE
            changeErrorOf {
                masterData.saveProductBarcode(
                    ProductBarcode(
                        id = 0,
                        productId = productId,
                        barcode = code,
                        quId = form.quId.takeIf { withDefaults },
                        amount = parseDecimal(form.amount.text.toString())
                            .takeIf { withDefaults },
                        storeId = form.storeId.takeIf { withDefaults || isInventory() },
                        note = form.note.text.toString()
                            .takeIf { (withDefaults || isInventory()) && it.isNotBlank() },
                    ),
                    userfields = emptyMap(),
                )
            }?.let { eventChannel.send(BookingEvent.Failed(it)) }
            form.pendingBarcode = null
        }
        val used = form.usedBarcode
        val price = parseDecimal(form.price.text.toString())
        if (kind == BookingKind.PURCHASE && used != null && price != null) {
            // Grocy keeps the price as typed, for the barcode's amount and unit
            changeErrorOf {
                masterData.saveProductBarcode(used.copy(lastPrice = price), emptyMap())
            }
        }
    }

    private fun isInventory() = kind == BookingKind.INVENTORY

    /** Grocy's reset after a booking; what it keeps differs per page. */
    private fun reset(open: Boolean) {
        val options = options.value
        clearProduct()
        if (open) return
        when (kind) {
            BookingKind.PURCHASE, BookingKind.INVENTORY -> {
                form.dueDate.clearText()
                form.price.clearText()
                form.note.clearText()
                form.storeId = null
                form.labelType = 0
                if (kind == BookingKind.PURCHASE) form.locationId = null
                form.userfields = userfieldInputs(options?.userfields.orEmpty(), values = null)
            }
            BookingKind.CONSUME -> form.recipeId = null
            BookingKind.TRANSFER -> form.toLocationId = null
        }
    }

    private fun clearProduct() {
        contextJob?.cancel()
        submitWhenReady = false
        form.product = null
        form.context = null
        form.units = emptyList()
        form.stockUnit = null
        form.quId = null
        form.amount.clearText()
        form.usedBarcode = null
        form.stockId = null
        form.useSpecificEntry = false
        form.exactAmount = false
        if (kind == BookingKind.CONSUME || kind == BookingKind.TRANSFER) form.locationId = null
    }

    private fun loadRecipes() {
        viewModelScope.launch {
            var loaded: Map<Int, String> = emptyMap()
            changeErrorOf { loaded = stock.recipes() }
            recipes = loaded
        }
    }

    private fun hasAdditions(): Boolean =
        kind == BookingKind.PURCHASE ||
            (kind == BookingKind.INVENTORY && inventoryChange() is InventoryChange.Added)

    private fun input(options: BookingOptions): BookingInput {
        val config = options.config
        val adds = hasAdditions()
        val context = form.context as? ProductContext.Ready
        val noStock = (kind == BookingKind.CONSUME || kind == BookingKind.TRANSFER) &&
            context != null && form.stockId == null &&
            context.details.hasSubProducts.not() &&
            availableStock(context).amount <= 0
        return BookingInput(
            kind = kind,
            productId = form.product?.id,
            amount = form.amount.text.toString(),
            quId = form.quId,
            decimals = options.settings.stockDecimalPlacesAmounts,
            limits = limits(options),
            noStockAtLocation = noStock,
            purchasedDate = form.purchasedDate.text.toString()
                .takeIf { adds && options.settings.showPurchasedDateOnPurchase },
            dueDate = form.dueDate.text.toString()
                .takeIf { adds && config.stockBestBeforeDateTracking },
            price = if (adds && config.stockPriceTracking) form.price.text.toString() else "",
            priceDecimals = options.settings.stockDecimalPlacesPricesInput,
            locationRequired = config.stockLocationTracking && when (kind) {
                BookingKind.CONSUME, BookingKind.TRANSFER -> true
                BookingKind.INVENTORY -> adds
                BookingKind.PURCHASE -> false
            },
            locationId = form.locationId,
            toLocationRequired = kind == BookingKind.TRANSFER,
            toLocationId = form.toLocationId,
            stockEntryRequired = form.useSpecificEntry,
            stockId = form.stockId,
        )
    }

    private fun purchaseBooking(
        options: BookingOptions,
        amount: Double,
        displayAmount: Double,
    ): PurchaseBooking {
        val config = options.config
        val product = form.product
        val tare = if (product?.enableTareWeightHandling == true) product.tareWeight else 0.0
        return PurchaseBooking(
            amount = amount,
            price = if (config.stockPriceTracking) {
                parseDecimal(form.price.text.toString())?.let {
                    pricePerStockUnit(
                        price = it,
                        factor = form.unit?.factor ?: 1.0,
                        isTotalPrice = form.isTotalPrice,
                        displayAmount = displayAmount,
                        tareWeight = tare,
                    )
                }
            } else {
                null
            },
            bestBeforeDate = form.dueDate.text.toString()
                .takeIf { config.stockBestBeforeDateTracking && it.isNotBlank() },
            purchasedDate = form.purchasedDate.text.toString()
                .takeIf { options.settings.showPurchasedDateOnPurchase },
            storeId = form.storeId.takeIf { config.stockPriceTracking },
            locationId = form.locationId.takeIf { config.stockLocationTracking },
            labelType = form.labelType.takeIf { config.labelPrinter },
            note = form.note.text.toString(),
        )
    }

    private fun consumeBooking(amount: Double) = ConsumeBooking(
        amount = amount,
        spoiled = form.spoiled,
        exactAmount = form.exactAmount,
        stockEntryId = form.stockId.takeIf { form.useSpecificEntry },
        locationId = form.locationId.takeIf { trackLocations() },
        recipeId = form.recipeId.takeIf { options.value?.config?.recipes == true },
    )

    private fun openBooking(amount: Double) = OpenBooking(
        amount = amount,
        stockEntryId = form.stockId.takeIf { form.useSpecificEntry },
        locationId = form.locationId.takeIf { trackLocations() },
    )

    private fun transferBooking(amount: Double) = TransferBooking(
        amount = amount,
        fromLocationId = requireNotNull(form.locationId),
        toLocationId = requireNotNull(form.toLocationId),
        stockEntryId = form.stockId.takeIf { form.useSpecificEntry },
    )

    private fun inventoryBooking(options: BookingOptions, amount: Double): InventoryBooking {
        val config = options.config
        return InventoryBooking(
            newAmount = amount,
            bestBeforeDate = form.dueDate.text.toString()
                .takeIf { config.stockBestBeforeDateTracking && it.isNotBlank() },
            purchasedDate = form.purchasedDate.text.toString()
                .takeIf { options.settings.showPurchasedDateOnPurchase },
            price = if (config.stockPriceTracking) {
                parseDecimal(form.price.text.toString())?.let { it * (form.unit?.factor ?: 1.0) }
            } else {
                null
            },
            storeId = form.storeId.takeIf { config.stockPriceTracking },
            locationId = form.locationId.takeIf { config.stockLocationTracking },
            labelType = form.labelType.takeIf { config.labelPrinter },
            note = form.note.text.toString(),
        )
    }

    /** Whether [date] ("YYYY-MM-DD") lies before today, Grocy's hint below the due date. */
    fun isBeforeToday(date: String): Boolean =
        epochDayOf(date)?.let { it < today() } == true

    private class Catalog(
        val products: List<Product>,
        val stock: List<StockAmount>,
        val barcodes: List<ProductBarcode>,
        val locations: List<Location>,
        val stores: List<Store>,
    )

    private class Setup(
        val units: List<QuantityUnit>,
        val resolved: List<ResolvedQuantityUnitConversion>,
        val config: ServerConfig,
        val settings: UserSettings,
        val userfields: List<Userfield>,
    )

    private companion object {
        const val EPSILON = 1e-9
        const val DEFAULT_DECIMALS = 4
    }
}
