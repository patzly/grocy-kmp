package com.patrickzedler.grocy.feature.stock.impl.scan

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.ProductBarcode
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.StockAmount
import com.patrickzedler.grocy.core.model.masterdata.nameFor
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.stock.BookingKind
import com.patrickzedler.grocy.core.model.text.formatDecimal
import com.patrickzedler.grocy.feature.stock.impl.booking.BookingViewModel
import com.patrickzedler.grocy.feature.stock.impl.booking.CodeMatch
import com.patrickzedler.grocy.feature.stock.impl.booking.bookableProducts
import com.patrickzedler.grocy.feature.stock.impl.booking.matchCode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** What the scan page needs to tell what a code is. */
internal data class ScanOptions(
    val products: List<Product>,
    val barcodes: List<ProductBarcode>,
    val stock: List<StockAmount>,
    val config: ServerConfig,
    val units: List<QuantityUnit> = emptyList(),
    /** Grocy's decimal places of amounts. */
    val decimals: Int = 4,
)

/**
 * The stock amount of a found product as Grocy writes amounts, "3 Pieces": the unit's name for
 * the amount in [language], at most [ScanOptions.decimals] decimals.
 */
internal fun stockAmountText(
    found: ScannedCode.Found,
    options: ScanOptions,
    language: String,
    decimalSeparator: Char,
): String {
    val unit = options.units.firstOrNull { it.id == found.product.quIdStock }
    return listOfNotNull(
        formatDecimal(found.stockAmount, options.decimals, decimalSeparator),
        unit?.nameFor(found.stockAmount, language),
    ).joinToString(" ")
}

/** What a scanned code is: a product with its amount in stock, or nothing known. */
internal sealed interface ScannedCode {
    val code: String

    data class Found(
        override val code: String,
        val product: Product,
        /** In the stock unit, the sub products included like Grocy's aggregated amount. */
        val stockAmount: Double,
    ) : ScannedCode

    /** Grocy's "Create or assign product" answers it once a booking is chosen. */
    data class Unknown(override val code: String) : ScannedCode
}

/**
 * What [code] stands for among [options]' products: a barcode or grocycode like on Grocy's
 * stock pages, else a typed product name.
 */
internal fun scannedCodeOf(code: String, options: ScanOptions): ScannedCode {
    val active = options.products.filter { it.active }
    val product = when (val match = matchCode(code, active, options.products, options.barcodes)) {
        is CodeMatch.Found -> match.product
        // A disabled product's code is no code to book with
        CodeMatch.NotOffered, CodeMatch.Unknown ->
            active.firstOrNull { it.name.equals(code.trim(), ignoreCase = true) }
    } ?: return ScannedCode.Unknown(code)
    return ScannedCode.Found(
        code = code,
        product = product,
        stockAmount = options.stock.firstOrNull { it.productId == product.id }
            ?.amountAggregated ?: 0.0,
    )
}

/**
 * The bookings offered for [scanned], in Grocy's menu order: consume and transfer only what is
 * in stock, transfer only with locations, nothing a product without stock of its own cannot do.
 */
internal fun offeredKinds(scanned: ScannedCode, options: ScanOptions): List<BookingKind> =
    BookingKind.entries.filter { kind ->
        when {
            kind == BookingKind.TRANSFER && !options.config.stockLocationTracking -> false
            scanned is ScannedCode.Found ->
                bookableProducts(kind, listOf(scanned.product), options.stock).isNotEmpty()
            // Consume and transfer can only assign an unknown code to an existing product
            else -> true
        }
    }

/**
 * Scanning first: the camera until a code, then what it is with the bookings for it, then the
 * chosen booking's form with the product chosen through the code, like the page of that
 * booking. After a booking it scans again.
 *
 * Each booking's ViewModel ([booking]) lives in a store of this one, so it survives configuration
 * changes; the booked one stays ([previousBooking]) until the next is chosen, so "Undo" of its
 * message still works meanwhile. [createBooking] creates the page of a kind for a code.
 */
internal class StockScanViewModel(
    masterData: MasterDataRepository,
    private val createBooking: (kind: BookingKind, code: String) -> BookingViewModel,
) : ViewModel() {

    val options: StateFlow<ScanOptions?> = combine(
        masterData.products,
        masterData.productBarcodes,
        masterData.stock,
        masterData.serverConfig,
        combine(masterData.quantityUnits, masterData.userSettings, ::Pair),
    ) { products, barcodes, stock, config, (units, settings) ->
        ScanOptions(products, barcodes, stock, config, units, settings.stockDecimalPlacesAmounts)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** A code typed or entered by a barcode reader, instead of the camera. */
    val codeField = TextFieldState()

    /** The scanned code, null while scanning. */
    var code by mutableStateOf<String?>(null)
        private set

    var kind by mutableStateOf<BookingKind?>(null)
        private set

    /** The chosen booking's page, null until one is chosen. */
    var booking by mutableStateOf<BookingViewModel?>(null)
        private set

    /** The last booked page, whose message may still offer "Undo". */
    var previousBooking by mutableStateOf<BookingViewModel?>(null)
        private set

    private var bookingStore = ViewModelStore()
    private var previousStore = ViewModelStore()
    private var created = 0

    /** A code from the camera or the field; the next scan replaces one not booked yet. */
    fun onScanned(code: String) {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) return
        discardBooking()
        codeField.clearText()
        this.code = trimmed
        // A code with a single possible booking needs no choice
        val options = options.value ?: return
        offeredKinds(scannedCodeOf(trimmed, options), options).singleOrNull()?.let(::choose)
    }

    /** The keyboard's action of the code field, a barcode reader's Enter. */
    fun onCodeSubmitted() = onScanned(codeField.text.toString())

    /** Opens the page of [kind] for the scanned code, replacing another one not booked. */
    fun choose(kind: BookingKind) {
        val code = code ?: return
        if (kind == this.kind) return
        discardBooking()
        this.kind = kind
        val key = "booking-${created++}"
        booking = ViewModelProvider.create(
            bookingStore,
            viewModelFactory { initializer { createBooking(kind, code) } },
        )[key, BookingViewModel::class]
    }

    /** The booking went through: on with the next code, its page kept for its "Undo". */
    fun onBooked() {
        previousStore.clear()
        previousStore = bookingStore.also { bookingStore = previousStore }
        previousBooking = booking
        booking = null
        kind = null
        code = null
    }

    /** Back to the camera without booking. */
    fun scanAgain() {
        discardBooking()
        code = null
    }

    private fun discardBooking() {
        bookingStore.clear()
        booking = null
        kind = null
    }

    override fun onCleared() {
        bookingStore.clear()
        previousStore.clear()
    }
}
