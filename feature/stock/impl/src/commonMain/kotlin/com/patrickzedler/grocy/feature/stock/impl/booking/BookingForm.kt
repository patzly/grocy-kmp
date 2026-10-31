package com.patrickzedler.grocy.feature.stock.impl.booking

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.patrickzedler.grocy.core.forms.AmountUnit
import com.patrickzedler.grocy.core.forms.ChangeError
import com.patrickzedler.grocy.core.forms.UserfieldError
import com.patrickzedler.grocy.core.forms.UserfieldInput
import com.patrickzedler.grocy.core.model.masterdata.Location
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.ProductBarcode
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.ResolvedQuantityUnitConversion
import com.patrickzedler.grocy.core.model.masterdata.StockAmount
import com.patrickzedler.grocy.core.model.masterdata.Store
import com.patrickzedler.grocy.core.model.masterdata.Userfield
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.server.UserSettings
import com.patrickzedler.grocy.core.model.stock.ProductLocationAmount
import com.patrickzedler.grocy.core.model.stock.ProductStockDetails
import com.patrickzedler.grocy.core.model.stock.StockEntry

/** What the stock pages choose from, from the database. */
internal data class BookingOptions(
    val products: List<Product>,
    val stock: List<StockAmount>,
    val barcodes: List<ProductBarcode>,
    val locations: List<Location>,
    val stores: List<Store>,
    val units: List<QuantityUnit>,
    val resolved: List<ResolvedQuantityUnitConversion>,
    val config: ServerConfig,
    val settings: UserSettings,
    /** The userfields of stock entries, which purchases and inventories fill. */
    val userfields: List<Userfield>,
)

/** What the server says about the chosen product, loaded once it is chosen. */
internal sealed interface ProductContext {
    data object Loading : ProductContext

    /** Offline or rejected: the page cannot book now. */
    data class Failed(val error: ChangeError) : ProductContext

    data class Ready(
        val details: ProductStockDetails,
        /** Locations with stock (consume, transfer). */
        val locationAmounts: List<ProductLocationAmount> = emptyList(),
        /** Stock entries in Grocy's consume order (consume, transfer). */
        val entries: List<StockEntry> = emptyList(),
    ) : ProductContext
}

/**
 * The fields of one of Grocy's stock pages; which of them a page shows depends on its
 * [BookingKind]. Amounts are typed in the chosen unit, sent in the stock unit.
 */
@Stable
internal class BookingForm(today: String) {
    var product by mutableStateOf<Product?>(null)
    var isProductUnmatched by mutableStateOf(false)
    var context by mutableStateOf<ProductContext?>(null)

    /** The product's units with their amount per stock unit, the stock unit first. */
    var units by mutableStateOf<List<AmountUnit>>(emptyList())
    var stockUnit by mutableStateOf<QuantityUnit?>(null)
    val amount = TextFieldState()
    var quId by mutableStateOf<Int?>(null)
    var isUnitUnmatched by mutableStateOf(false)

    // Purchase and inventory
    val purchasedDate = TextFieldState(today)
    val dueDate = TextFieldState()
    val price = TextFieldState()
    var isTotalPrice by mutableStateOf(false)
    var storeId by mutableStateOf<Int?>(null)
    var labelType by mutableStateOf(0)
    val note = TextFieldState()
    var userfields by mutableStateOf<List<UserfieldInput>>(emptyList())

    /** The location of a purchase or an inventory, the source of a consumption or transfer. */
    var locationId by mutableStateOf<Int?>(null)
    var toLocationId by mutableStateOf<Int?>(null)
    var isLocationUnmatched by mutableStateOf(false)
    var isStoreUnmatched by mutableStateOf(false)

    // Consume and transfer
    var useSpecificEntry by mutableStateOf(false)
    var stockId by mutableStateOf<String?>(null)
    var spoiled by mutableStateOf(false)
    var exactAmount by mutableStateOf(false)
    var recipeId by mutableStateOf<Int?>(null)
    var isRecipeUnmatched by mutableStateOf(false)

    /** The barcode the product was found by, whose defaults it took (purchase). */
    var usedBarcode by mutableStateOf<ProductBarcode?>(null)

    /** A barcode that will be added to the chosen product with the booking. */
    var pendingBarcode by mutableStateOf<String?>(null)

    var errors by mutableStateOf<Map<BookingField, BookingError>>(emptyMap())
    var userfieldErrors by mutableStateOf<Map<String, UserfieldError>>(emptyMap())
    var isBooking by mutableStateOf(false)

    /** The chosen unit with its amount per stock unit. */
    val unit: AmountUnit? get() = units.firstOrNull { it.unit.id == quId }

    val details: ProductStockDetails? get() = (context as? ProductContext.Ready)?.details

    /** True while any picker's text names none of its options, which blocks booking. */
    val hasUnmatchedChoice: Boolean
        get() = isProductUnmatched || isUnitUnmatched || isLocationUnmatched ||
            isStoreUnmatched || isRecipeUnmatched || userfields.any { it.isUnmatched }
}
