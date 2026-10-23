package com.patrickzedler.grocy.feature.masterdata.impl.products

import androidx.compose.runtime.Immutable
import com.patrickzedler.grocy.core.model.masterdata.Location
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.ProductBarcode
import com.patrickzedler.grocy.core.model.masterdata.ProductGroup
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.StockAmount
import com.patrickzedler.grocy.core.model.masterdata.Store
import com.patrickzedler.grocy.core.model.text.FuzzySearch
import com.patrickzedler.grocy.core.model.text.formatDecimal
import com.patrickzedler.grocy.core.model.text.normalizedForSearch

/** The filters of Grocy's product list; the defaults show every active product. */
@Immutable
internal data class ProductFilter(
    val productGroupId: Int? = null,
    val stockStatus: StockStatusFilter = StockStatusFilter.ALL,
    val showDisabled: Boolean = false,
) {
    val isActive: Boolean
        get() = this != ProductFilter()
}

internal enum class StockStatusFilter { ALL, IN_STOCK, OUT_OF_STOCK }

internal enum class ProductSortField {
    NAME,
    LOCATION,
    MIN_STOCK_AMOUNT,
    PRODUCT_GROUP,
    QU_STOCK,
    STORE,
}

@Immutable
internal data class ProductSort(
    val field: ProductSortField = ProductSortField.NAME,
    val ascending: Boolean = true,
)

/** A row of the product list with the columns of Grocy's table. */
@Immutable
internal data class ProductListItem(
    val id: Int,
    val name: String,
    val active: Boolean,
    /** Grocy's file name of the picture, null without one. */
    val pictureFileName: String?,
    val locationName: String?,
    val productGroupName: String?,
    /** Formatted, null when it is 0. */
    val minStockAmount: String?,
    /** Stock unit for [minStockAmount], plural unless the amount is 1. */
    val minStockUnit: String?,
    val quantityUnitStockName: String?,
    val quantityUnitPurchaseName: String?,
    val storeName: String?,
)

/** Everything the list is built from, as the database has it. */
internal data class ProductListData(
    val products: List<Product> = emptyList(),
    val productGroups: List<ProductGroup> = emptyList(),
    val locations: List<Location> = emptyList(),
    val quantityUnits: List<QuantityUnit> = emptyList(),
    val stores: List<Store> = emptyList(),
    val barcodes: List<ProductBarcode> = emptyList(),
    val stock: List<StockAmount> = emptyList(),
)

/**
 * The products Grocy's list shows for [filter] and [query], sorted by [sort]. Like Grocy:
 * disabled products only with [ProductFilter.showDisabled], a product is in stock when its
 * amount including sub products is above 0 (disabled ones never are), and the search looks at
 * the visible columns; a barcode is found when typed completely.
 */
internal fun buildProductList(
    data: ProductListData,
    filter: ProductFilter,
    sort: ProductSort,
    query: String,
    decimalPlaces: Int,
    decimalSeparator: Char,
    showLocation: Boolean,
    showStore: Boolean,
): List<ProductListItem> {
    val groups = data.productGroups.associateBy { it.id }
    val locations = data.locations.associateBy { it.id }
    val units = data.quantityUnits.associateBy { it.id }
    val stores = data.stores.associateBy { it.id }
    val inStock = data.stock.filter { it.amountAggregated > 0 }.map { it.productId }.toSet()

    val filtered = data.products.filter { product ->
        (filter.showDisabled || product.active) &&
            (filter.productGroupId == null || product.productGroupId == filter.productGroupId) &&
            when (filter.stockStatus) {
                StockStatusFilter.ALL -> true
                StockStatusFilter.IN_STOCK -> product.active && product.id in inStock
                StockStatusFilter.OUT_OF_STOCK -> !product.active || product.id !in inStock
            }
    }

    val items = filtered.map { product ->
        ProductListItem(
            id = product.id,
            name = product.name,
            active = product.active,
            pictureFileName = product.pictureFileName?.takeIf { it.isNotBlank() },
            locationName = product.locationId?.let { locations[it]?.name }.takeIf { showLocation },
            // Grocy only names active groups in the list
            productGroupName = product.productGroupId
                ?.let { groups[it] }
                ?.takeIf { it.active }
                ?.name,
            minStockAmount = product.minStockAmount.takeIf { it != 0.0 }
                ?.let { formatDecimal(it, decimalPlaces, decimalSeparator) },
            minStockUnit = product.quIdStock?.let { units[it] }?.let { unit ->
                if (product.minStockAmount == 1.0) unit.name else unit.namePlural ?: unit.name
            },
            quantityUnitStockName = product.quIdStock?.let { units[it]?.name },
            quantityUnitPurchaseName = product.quIdPurchase?.let { units[it]?.name },
            storeName = product.storeId?.let { stores[it]?.name }.takeIf { showStore },
        )
    }

    val searched = search(items, query, data.barcodes)
    // The search orders by relevance, otherwise the chosen sort applies
    return if (query.isNotBlank()) searched else searched.sortedWith(comparator(sort, data))
}

private fun search(
    items: List<ProductListItem>,
    query: String,
    barcodes: List<ProductBarcode>,
): List<ProductListItem> {
    if (query.isBlank()) return items
    val code = query.trim()
    val barcodeMatches = barcodes.filter { it.barcode == code }.map { it.productId }.toSet()
    val textMatches = FuzzySearch.filter(items, query) { item ->
        listOfNotNull(item.name, item.productGroupName, item.locationName, item.storeName)
    }
    return items.filter { it.id in barcodeMatches } +
        textMatches.filter { it.id !in barcodeMatches }
}

private fun comparator(sort: ProductSort, data: ProductListData): Comparator<ProductListItem> {
    val minStock = data.products.associate { it.id to it.minStockAmount }
    val byName = compareBy<ProductListItem> { it.name.normalizedForSearch() }
    // Empty values last in both directions, like a table sorted by text puts them together
    fun text(selector: (ProductListItem) -> String?): Comparator<ProductListItem> {
        val values = compareBy<ProductListItem> { selector(it)?.normalizedForSearch() }
        val directed = if (sort.ascending) values else values.reversed()
        return compareBy<ProductListItem> { selector(it).isNullOrBlank() }.then(directed)
    }
    val primary: Comparator<ProductListItem> = when (sort.field) {
        ProductSortField.NAME -> if (sort.ascending) byName else byName.reversed()
        ProductSortField.LOCATION -> text { it.locationName }
        ProductSortField.PRODUCT_GROUP -> text { it.productGroupName }
        ProductSortField.QU_STOCK -> text { it.quantityUnitStockName }
        ProductSortField.STORE -> text { it.storeName }
        ProductSortField.MIN_STOCK_AMOUNT -> {
            val values = compareBy<ProductListItem> { minStock[it.id] ?: 0.0 }
            if (sort.ascending) values else values.reversed()
        }
    }
    return primary.then(byName)
}
