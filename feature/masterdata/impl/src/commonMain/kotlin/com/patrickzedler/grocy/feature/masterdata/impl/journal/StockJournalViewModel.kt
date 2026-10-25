package com.patrickzedler.grocy.feature.masterdata.impl.journal

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.intl.Locale
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.stock.StockRepository
import com.patrickzedler.grocy.core.data.util.RefreshController
import com.patrickzedler.grocy.core.data.util.RefreshState
import com.patrickzedler.grocy.core.model.User
import com.patrickzedler.grocy.core.model.masterdata.Location
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.server.UserSettings
import com.patrickzedler.grocy.core.model.stock.StockLogEntry
import com.patrickzedler.grocy.core.model.text.platformDecimalSeparator
import com.patrickzedler.grocy.core.model.text.todayEpochDay
import com.patrickzedler.grocy.feature.masterdata.impl.related.ChangeError
import com.patrickzedler.grocy.feature.masterdata.impl.related.changeErrorOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What a journal page shows. */
internal data class JournalUiState(
    /** The bookings of the journal, empty on the summary. */
    val items: List<JournalItem>,
    /** The sums of the summary, empty on the journal. */
    val summary: List<JournalSummaryItem>,
    /** Products with bookings, the product filter's choices. */
    val products: List<Product>,
    val locations: List<Location>,
    val users: List<User>,
    val filter: JournalFilter,
    val config: ServerConfig,
    val hasBookings: Boolean,
)

internal sealed interface JournalEvent {
    data object Undone : JournalEvent

    data class Failed(val error: ChangeError) : JournalEvent
}

/**
 * Grocy's "Stock journal" or, with [summary], its "Stock journal summary": the bookings (of
 * [productId] if given) as stored, downloaded for the chosen date range, with undoing them.
 * The summary always sums every booking.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class StockJournalViewModel(
    productId: Int?,
    val summary: Boolean,
    private val masterData: MasterDataRepository,
    private val stock: StockRepository,
    private val decimalSeparator: Char = platformDecimalSeparator(),
    private val language: String = Locale.current.toLanguageTag(),
    private val today: () -> Long = ::todayEpochDay,
) : ViewModel() {

    val searchState = TextFieldState()

    var isSearchVisible by mutableStateOf(false)
        private set

    private val filter = MutableStateFlow(
        JournalFilter(
            productId = productId,
            months = if (summary) JOURNAL_ALL_MONTHS else JOURNAL_DEFAULT_MONTHS,
        ),
    )

    private val since = filter.map { journalSince(today(), it.months) }.distinctUntilChanged()

    private val refreshController = RefreshController(viewModelScope) {
        stock.refreshStockLog(journalSince(today(), filter.value.months))
    }
    val refresh: StateFlow<RefreshState> = refreshController.state

    private val query = snapshotFlow { searchState.text.toString() }.distinctUntilChanged()

    private val names = combine(
        masterData.products,
        masterData.quantityUnits,
        masterData.locations,
        masterData.users,
    ) { products, units, locations, users -> JournalNames(products, units, locations, users) }

    val state: StateFlow<JournalUiState?> = combine(
        since.flatMapLatest { stock.stockLog(it.orEmpty()) },
        names,
        masterData.serverConfig,
        masterData.userSettings,
        filter,
        query,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val bookings = values[0] as List<StockLogEntry>
        val names = values[1] as JournalNames
        val config = values[2] as ServerConfig
        val settings = values[3] as UserSettings
        val currentFilter = values[4] as JournalFilter
        val search = values[5] as String
        val decimals = settings.stockDecimalPlacesAmounts
        JournalUiState(
            items = if (summary) {
                emptyList()
            } else {
                buildJournalItems(
                    bookings, names, currentFilter, search, decimals, decimalSeparator, language,
                )
            },
            summary = if (summary) {
                buildJournalSummary(
                    bookings, names, currentFilter, search, decimals, decimalSeparator, language,
                )
            } else {
                emptyList()
            },
            products = names.products.filter { product ->
                bookings.any { it.productId == product.id } ||
                    product.id == currentFilter.productId
            },
            locations = names.locations,
            users = names.users,
            filter = currentFilter,
            config = config,
            hasBookings = bookings.isNotEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    var isUndoing by mutableStateOf(false)
        private set

    private val eventChannel = Channel<JournalEvent>(Channel.BUFFERED)
    val events: Flow<JournalEvent> = eventChannel.receiveAsFlow()

    init {
        refresh()
    }

    fun refresh() = refreshController.refresh()

    fun onErrorShown() = refreshController.onErrorShown()

    /** A longer date range downloads the bookings it adds. */
    fun setFilter(update: (JournalFilter) -> JournalFilter) {
        val months = filter.value.months
        filter.update(update)
        if (filter.value.months != months) refresh()
    }

    fun clearFilter() = setFilter {
        JournalFilter(months = if (summary) JOURNAL_ALL_MONTHS else JOURNAL_DEFAULT_MONTHS)
    }

    fun showSearch() {
        isSearchVisible = true
    }

    fun hideSearch() {
        isSearchVisible = false
        searchState.clearText()
    }

    /** Grocy's "Undo transaction", which undoes the bookings of the same action too. */
    fun undo(booking: StockLogEntry) {
        if (isUndoing || booking.undone) return
        isUndoing = true
        viewModelScope.launch {
            val error = changeErrorOf {
                stock.undo(booking.id)
                stock.refreshStockLog(journalSince(today(), filter.value.months))
            }
            isUndoing = false
            eventChannel.send(
                if (error == null) JournalEvent.Undone else JournalEvent.Failed(error),
            )
        }
    }
}
