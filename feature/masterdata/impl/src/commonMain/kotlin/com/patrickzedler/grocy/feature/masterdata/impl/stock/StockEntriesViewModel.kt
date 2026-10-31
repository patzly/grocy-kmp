package com.patrickzedler.grocy.feature.masterdata.impl.stock

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.stock.StockRepository
import com.patrickzedler.grocy.core.data.util.RefreshController
import com.patrickzedler.grocy.core.data.util.RefreshState
import com.patrickzedler.grocy.core.forms.ChangeError
import com.patrickzedler.grocy.core.forms.FieldError
import com.patrickzedler.grocy.core.forms.UserfieldError
import com.patrickzedler.grocy.core.forms.UserfieldInput
import com.patrickzedler.grocy.core.forms.changeErrorOf
import com.patrickzedler.grocy.core.forms.changedValues
import com.patrickzedler.grocy.core.forms.userfieldInputs
import com.patrickzedler.grocy.core.forms.validationErrors
import com.patrickzedler.grocy.core.model.masterdata.Location
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.Store
import com.patrickzedler.grocy.core.model.masterdata.nameFor
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.stock.StockEntry
import com.patrickzedler.grocy.core.model.text.formatDecimal
import com.patrickzedler.grocy.core.model.text.platformDecimalSeparator
import com.patrickzedler.grocy.core.model.text.todayEpochDay
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What a booking on an entry did, for Grocy's messages; [bookingId] undoes it. */
internal sealed interface StockEntryEvent {
    data class Consumed(
        val amount: String,
        val productName: String,
        val spoiled: Boolean,
        val bookingId: Int?,
    ) : StockEntryEvent

    data class Opened(
        val amount: String,
        val productName: String,
        /** Where the product moves on opening, null if it stays. */
        val movedTo: String?,
        val bookingId: Int?,
    ) : StockEntryEvent

    data class Edited(val bookingId: Int?) : StockEntryEvent

    data object Undone : StockEntryEvent

    data class Failed(val error: ChangeError) : StockEntryEvent
}

/** The open "Edit stock entry" form. */
@Stable
internal class StockEntryDialogState(
    val entry: StockEntry,
    initial: StockEntryValues,
    val productName: String,
    val stockUnitName: String?,
    val locations: List<Location>,
    val stores: List<Store>,
    val config: ServerConfig,
    val decimals: Int,
    val priceDecimals: Int,
    val userfields: List<UserfieldInput>,
) {
    val amount = TextFieldState(initial.amount)
    val purchasedDate = TextFieldState(initial.purchasedDate)
    val dueDate = TextFieldState(initial.dueDate)
    var neverOverdue by mutableStateOf(initial.neverOverdue)
    val price = TextFieldState(initial.price)
    var storeId by mutableStateOf(initial.storeId)
    var locationId by mutableStateOf(initial.locationId)
    val note = TextFieldState(initial.note)
    var open by mutableStateOf(initial.open)

    /** Grocy's "Reprint stock entry label", checked once the due date changes. */
    var reprintLabel by mutableStateOf(false)

    var isStoreUnmatched by mutableStateOf(false)
    var isLocationUnmatched by mutableStateOf(false)

    var errors by mutableStateOf<Map<StockEntryField, FieldError>>(emptyMap())
    var userfieldErrors by mutableStateOf<Map<String, UserfieldError>>(emptyMap())
    var isSaving by mutableStateOf(false)
    var error by mutableStateOf<ChangeError?>(null)

    /** The server prints labels itself, so reprinting one works. */
    val canPrintLabel: Boolean
        get() = config.labelPrinter && config.labelPrinterRunsOnServer

    fun values() = StockEntryValues(
        amount = amount.text.toString(),
        purchasedDate = purchasedDate.text.toString(),
        dueDate = dueDate.text.toString(),
        neverOverdue = neverOverdue,
        price = price.text.toString(),
        storeId = storeId,
        locationId = locationId,
        note = note.text.toString(),
        open = open,
    )
}

/**
 * Grocy's "Stock entries" page: every entry of the active products, filtered by product
 * (preset with [productId]) and location, with its bookings (consume, open, edit) and their
 * undo. The entries are stored for offline use, bookings are online only.
 */
internal class StockEntriesViewModel(
    productId: Int?,
    private val masterData: MasterDataRepository,
    private val stock: StockRepository,
    private val decimalSeparator: Char = platformDecimalSeparator(),
    private val language: String,
    private val today: () -> Long = ::todayEpochDay,
) : ViewModel() {

    private val filter = MutableStateFlow(StockEntryFilter(productId = productId))

    private val refreshController = RefreshController(viewModelScope) {
        stock.refreshStockEntries()
    }
    val refresh: StateFlow<RefreshState> = refreshController.state

    // Grouped, so each part keeps its type: combine takes at most five typed flows
    private val stockData = combine(
        stock.stockEntries,
        masterData.products,
        masterData.quantityUnits,
        masterData.locations,
        masterData.stores,
        ::StockData,
    )

    val state: StateFlow<StockEntriesUiState?> = combine(
        stockData,
        masterData.resolvedQuantityUnitConversions,
        masterData.serverConfig,
        masterData.userSettings,
        filter,
    ) { data, resolved, config, settings, currentFilter ->
        val entries = data.entries
        val products = data.products
        val locations = data.locations
        StockEntriesUiState(
            items = buildStockEntryItems(
                entries = entries,
                products = products,
                units = data.units,
                locations = locations,
                stores = data.stores,
                resolved = resolved,
                config = config,
                settings = settings,
                filter = currentFilter,
                today = today(),
                decimalSeparator = decimalSeparator,
                language = language,
            ),
            products = products.filter { product ->
                product.active && entries.any { it.productId == product.id } ||
                    product.id == currentFilter.productId
            },
            locations = locations.filter { it.active || it.id == currentFilter.locationId },
            filter = currentFilter,
            config = config,
            currency = config.currency,
            priceDecimals = settings.stockDecimalPlacesPricesDisplay,
            hasEntries = entries.isNotEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private class StockData(
        val entries: List<StockEntry>,
        val products: List<Product>,
        val units: List<QuantityUnit>,
        val locations: List<Location>,
        val stores: List<Store>,
    )

    var dialog by mutableStateOf<StockEntryDialogState?>(null)
        private set

    /** True while a booking runs; entries take no other booking meanwhile. */
    var isBooking by mutableStateOf(false)
        private set

    private val eventChannel = Channel<StockEntryEvent>(Channel.BUFFERED)
    val events: Flow<StockEntryEvent> = eventChannel.receiveAsFlow()

    init {
        refresh()
    }

    fun refresh() = refreshController.refresh()

    fun onErrorShown() = refreshController.onErrorShown()

    fun setFilter(update: (StockEntryFilter) -> StockEntryFilter) {
        filter.update(update)
    }

    fun clearFilter() {
        filter.value = StockEntryFilter()
    }

    /** Grocy's "Consume this stock entry", as spoiled if [spoiled]. */
    fun consume(entry: StockEntry, spoiled: Boolean) = book {
        val booking = stock.consume(entry, spoiled)
        StockEntryEvent.Consumed(
            amount = amountText(entry),
            productName = productOf(entry)?.name.orEmpty(),
            spoiled = spoiled,
            bookingId = booking,
        )
    }

    /** Grocy's "Mark this stock entry as open". */
    fun open(entry: StockEntry) = book {
        val booking = stock.open(entry)
        val product = productOf(entry)
        val movedTo = product?.takeIf { it.moveOnOpen }?.defaultConsumeLocationId
            ?.takeIf { it != entry.locationId }
            ?.let { id -> masterData.locations.first().firstOrNull { it.id == id }?.name }
        StockEntryEvent.Opened(
            amount = amountText(entry),
            productName = product?.name.orEmpty(),
            movedTo = movedTo,
            bookingId = booking,
        )
    }

    /** Grocy's "Undo" of a booking's message. */
    fun undo(bookingId: Int) = book {
        stock.undo(bookingId)
        StockEntryEvent.Undone
    }

    /** Opens Grocy's "Edit stock entry" form of [entry]. */
    fun edit(entry: StockEntry) {
        viewModelScope.launch {
            val product = productOf(entry) ?: return@launch
            val config = masterData.serverConfig.first()
            val settings = masterData.userSettings.first()
            val decimals = settings.stockDecimalPlacesAmounts
            val priceDecimals = settings.stockDecimalPlacesPricesInput
            val fields = masterData.userfields(MasterDataRepository.STOCK).first()
            dialog = StockEntryDialogState(
                entry = entry,
                initial = entry.toValues(decimals, priceDecimals, decimalSeparator),
                productName = product.name,
                stockUnitName = product.quIdStock?.let { id ->
                    masterData.quantityUnits.first().firstOrNull { it.id == id }?.name
                },
                locations = masterData.locations.first()
                    .filter { it.active || it.id == entry.locationId },
                stores = masterData.stores.first().filter { it.active || it.id == entry.storeId },
                config = config,
                decimals = decimals,
                priceDecimals = priceDecimals,
                userfields = userfieldInputs(fields, entry.userfields),
            )
        }
    }

    fun dismissDialog() {
        if (dialog?.isSaving != true) dialog = null
    }

    /** Saves the open form; it closes once the server booked the change. */
    fun save() {
        val form = dialog ?: return
        if (form.isSaving) return
        val values = form.values()
        form.errors = values.validate(form.config, form.decimals, form.priceDecimals)
        form.userfieldErrors = form.userfields.validationErrors()
        val hasUnmatched = form.isStoreUnmatched || form.isLocationUnmatched ||
            form.userfields.any { it.isUnmatched }
        if (form.errors.isNotEmpty() || form.userfieldErrors.isNotEmpty() || hasUnmatched) return

        val entry = values.toEntry(form.entry, form.config)
        form.isSaving = true
        form.error = null
        viewModelScope.launch {
            var booking: Int? = null
            val error = changeErrorOf {
                booking = stock.edit(entry, form.userfields.changedValues())
                if (form.reprintLabel && form.canPrintLabel) {
                    stock.printStockEntryLabel(entry.id)
                }
            }
            form.isSaving = false
            if (error == null) {
                if (dialog === form) dialog = null
                eventChannel.send(StockEntryEvent.Edited(booking))
            } else {
                form.error = error
            }
        }
    }

    private fun book(block: suspend () -> StockEntryEvent) {
        if (isBooking) return
        isBooking = true
        viewModelScope.launch {
            var event: StockEntryEvent? = null
            val error = changeErrorOf { event = block() }
            isBooking = false
            eventChannel.send(event ?: StockEntryEvent.Failed(requireNotNull(error)))
        }
    }

    private suspend fun productOf(entry: StockEntry): Product? =
        masterData.product(entry.productId).first()

    /** The whole amount of [entry] with its stock unit, e.g. "2 Pieces". */
    private suspend fun amountText(entry: StockEntry): String {
        val decimals = masterData.userSettings.first().stockDecimalPlacesAmounts
        val unit = productOf(entry)?.quIdStock?.let { id ->
            masterData.quantityUnits.first().firstOrNull { it.id == id }
        }
        return listOfNotNull(
            formatDecimal(entry.amount, decimals, decimalSeparator),
            unit?.nameFor(entry.amount, language),
        ).joinToString(" ")
    }
}
