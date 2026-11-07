package com.patrickzedler.grocy.feature.stock.impl.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.stock.StockRepository
import com.patrickzedler.grocy.core.data.util.RefreshController
import com.patrickzedler.grocy.core.data.util.RefreshState
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.nameFor
import com.patrickzedler.grocy.core.model.stock.LocationContent
import com.patrickzedler.grocy.core.model.stock.locationContentOf
import com.patrickzedler.grocy.core.model.text.formatDecimal
import com.patrickzedler.grocy.core.model.text.platformDecimalSeparator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** A product of a location: its amount with the stock unit, the opened part if any. */
internal data class LocationSheetItem(
    val productId: Int,
    val name: String,
    val amount: String,
    val opened: String?,
)

internal data class LocationSheetGroup(
    val locationId: Int,
    val name: String,
    val items: List<LocationSheetItem>,
)

internal data class LocationContentSheetUiState(
    val groups: List<LocationSheetGroup>,
    /** Grocy's "Show only in stock products", on by default. */
    val onlyInStock: Boolean,
    val hasStock: Boolean,
)

/** [contents] as Grocy's sheet writes them: "3 Pieces", amounts with [decimals] at most. */
internal fun locationSheetGroupsOf(
    contents: List<LocationContent>,
    units: Map<Int, QuantityUnit>,
    decimals: Int,
    decimalSeparator: Char,
    language: String,
): List<LocationSheetGroup> = contents.map { content ->
    LocationSheetGroup(
        locationId = content.location.id,
        name = content.location.name,
        items = content.items.map { item ->
            val unit = item.product.quIdStock?.let(units::get)
            LocationSheetItem(
                productId = item.product.id,
                name = item.product.name,
                amount = listOfNotNull(
                    formatDecimal(item.amount, decimals, decimalSeparator),
                    unit?.nameFor(item.amount, language),
                ).joinToString(" "),
                opened = item.amountOpened.takeIf { it > 0.0 }
                    ?.let { formatDecimal(it, decimals, decimalSeparator) },
            )
        },
    )
}

/**
 * Grocy's "Location Content Sheet": every location with the products there and their amounts,
 * from the stored stock entries, which it downloads again when it opens.
 */
internal class LocationContentSheetViewModel(
    masterData: MasterDataRepository,
    private val stock: StockRepository,
    language: String,
    decimalSeparator: Char = platformDecimalSeparator(),
) : ViewModel() {

    private val onlyInStock = MutableStateFlow(true)

    private val refreshController = RefreshController(viewModelScope) {
        stock.refreshStockEntries()
    }
    val refresh: StateFlow<RefreshState> = refreshController.state

    val state: StateFlow<LocationContentSheetUiState?> = combine(
        stock.stockEntries,
        masterData.products,
        masterData.locations,
        combine(masterData.quantityUnits, masterData.userSettings, ::Pair),
        onlyInStock,
    ) { entries, products, locations, (units, settings), inStockOnly ->
        LocationContentSheetUiState(
            groups = locationSheetGroupsOf(
                contents = locationContentOf(
                    entries = entries,
                    products = products,
                    locations = locations,
                    includeOutOfStock = !inStockOnly,
                ),
                units = units.associateBy { it.id },
                decimals = settings.stockDecimalPlacesAmounts,
                decimalSeparator = decimalSeparator,
                language = language,
            ),
            onlyInStock = inStockOnly,
            hasStock = entries.isNotEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        refresh()
    }

    fun refresh() = refreshController.refresh()

    fun onErrorShown() = refreshController.onErrorShown()

    fun setOnlyInStock(only: Boolean) {
        onlyInStock.value = only
    }
}
