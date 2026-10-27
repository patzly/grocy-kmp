package com.patrickzedler.grocy.feature.masterdata.impl.barcode

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.model.masterdata.ProductBarcode
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.Store
import com.patrickzedler.grocy.core.model.masterdata.nameFor
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.server.UserSettings
import com.patrickzedler.grocy.core.model.text.formatCurrency
import com.patrickzedler.grocy.core.model.text.formatDecimal
import com.patrickzedler.grocy.core.model.text.platformDecimalSeparator
import com.patrickzedler.grocy.feature.masterdata.impl.product.FieldError
import com.patrickzedler.grocy.feature.masterdata.impl.product.UserfieldError
import com.patrickzedler.grocy.feature.masterdata.impl.product.UserfieldInput
import com.patrickzedler.grocy.feature.masterdata.impl.related.AmountUnit
import com.patrickzedler.grocy.feature.masterdata.impl.related.ChangeError
import com.patrickzedler.grocy.feature.masterdata.impl.related.changeErrorOf
import com.patrickzedler.grocy.feature.masterdata.impl.related.changedValues
import com.patrickzedler.grocy.feature.masterdata.impl.related.productAmountUnits
import com.patrickzedler.grocy.feature.masterdata.impl.related.userfieldInputs
import com.patrickzedler.grocy.feature.masterdata.impl.related.validationErrors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One barcode of the product form's list, with the columns of Grocy's barcode table. */
internal data class BarcodeItem(
    val id: Int,
    val barcode: String,
    /** Amount and unit, e.g. "6 Pieces"; null without amount and unit. */
    val amount: String?,
    val storeName: String?,
    /** Formatted in the server's currency, null without price tracking or price. */
    val lastPrice: String?,
    val note: String?,
)

/**
 * The rows of Grocy's barcode table of [productId], by barcode; stores and prices only with
 * price tracking.
 */
internal fun buildBarcodeItems(
    productId: Int,
    barcodes: List<ProductBarcode>,
    units: List<QuantityUnit>,
    stores: List<Store>,
    config: ServerConfig,
    settings: UserSettings,
    decimalSeparator: Char,
    language: String,
    formatPrice: (Double, String, Int) -> String = ::formatCurrency,
): List<BarcodeItem> {
    val unitsById = units.associateBy { it.id }
    val storesById = stores.associateBy { it.id }
    val decimals = settings.stockDecimalPlacesAmounts
    return barcodes.filter { it.productId == productId }.sortedBy { it.barcode }.map { barcode ->
        val unit = barcode.quId?.let { unitsById[it] }
        val amount = barcode.amount
        BarcodeItem(
            id = barcode.id,
            barcode = barcode.barcode,
            amount = listOfNotNull(
                amount?.let { formatDecimal(it, decimals, decimalSeparator) },
                unit?.nameFor(amount ?: 1.0, language),
            ).joinToString(" ").ifEmpty { null },
            storeName = barcode.storeId?.let { storesById[it]?.name }
                ?.takeIf { config.stockPriceTracking },
            lastPrice = barcode.lastPrice?.takeIf { config.stockPriceTracking }?.let {
                formatPrice(it, config.currency, settings.stockDecimalPlacesPricesDisplay)
            },
            note = barcode.note?.takeIf { it.isNotBlank() },
        )
    }
}

/** The open barcode form of the dialog; [id] is 0 for a new barcode. */
@Stable
internal class BarcodeDialogState(
    val id: Int,
    val original: ProductBarcode?,
    initial: BarcodeFormValues,
    val productName: String,
    /** The units of the amount, converted from the product's [purchaseUnit]. */
    val units: List<AmountUnit>,
    val purchaseUnit: QuantityUnit?,
    val stores: List<Store>,
    val showsStore: Boolean,
    val decimals: Int,
    val userfields: List<UserfieldInput>,
) {
    val barcode = TextFieldState(initial.barcode)
    val amount = TextFieldState(initial.amount)
    val note = TextFieldState(initial.note)
    var quId by mutableStateOf(initial.quId)
    var storeId by mutableStateOf(initial.storeId)

    /** True while a picker's text names none of its options; nothing is saved then. */
    var isUnitUnmatched by mutableStateOf(false)
    var isStoreUnmatched by mutableStateOf(false)

    var errors by mutableStateOf<Map<BarcodeField, FieldError>>(emptyMap())
    var userfieldErrors by mutableStateOf<Map<String, UserfieldError>>(emptyMap())
    var isSaving by mutableStateOf(false)
    var error by mutableStateOf<ChangeError?>(null)

    val isNew: Boolean
        get() = id == MasterDataRepository.NEW_OBJECT_ID

    fun values() = BarcodeFormValues(
        barcode = barcode.text.toString(),
        amount = amount.text.toString(),
        quId = quId,
        storeId = storeId,
        note = note.text.toString(),
    )
}

/**
 * The barcodes of a product in its form: Grocy's barcode table, its barcode form as dialog
 * ([dialog]) and, through the object actions, deleting. Saving is online only. The product is
 * set by [showProduct], a new one gets its id once created.
 */
internal class ProductBarcodesViewModel(
    private val repository: MasterDataRepository,
    private val decimalSeparator: Char = platformDecimalSeparator(),
    language: String,
) : ViewModel() {

    private val productId = MutableStateFlow<Int?>(null)

    val items: StateFlow<List<BarcodeItem>> = combine(
        productId,
        repository.productBarcodes,
        repository.quantityUnits,
        repository.stores,
        // Paired, so each part keeps its type: combine takes at most five typed flows
        combine(repository.serverConfig, repository.userSettings, ::Pair),
    ) { id, barcodes, units, stores, (config, settings) ->
        if (id == null) return@combine emptyList()
        buildBarcodeItems(
            productId = id,
            barcodes = barcodes,
            units = units,
            stores = stores,
            config = config,
            settings = settings,
            decimalSeparator = decimalSeparator,
            language = language,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    var dialog by mutableStateOf<BarcodeDialogState?>(null)
        private set

    fun showProduct(id: Int) {
        productId.value = id
    }

    /** Opens the form of the barcode [id], or of a new barcode without one. */
    fun open(id: Int? = null) {
        val productId = productId.value ?: return
        viewModelScope.launch {
            val original = id?.let { barcodeId ->
                repository.productBarcodes.first().firstOrNull { it.id == barcodeId }
            }
            if (id != null && original == null) return@launch
            val product = repository.product(productId).first() ?: return@launch
            val config = repository.serverConfig.first()
            val settings = repository.userSettings.first()
            val decimals = settings.stockDecimalPlacesAmounts
            val units = productAmountUnits(
                productId = productId,
                destinationQuId = product.quIdPurchase,
                resolved = repository.resolvedQuantityUnitConversions.first(),
                units = repository.quantityUnits.first(),
            )
            val stores = repository.stores.first()
                .filter { it.active || it.id == original?.storeId }
            val fields = repository.userfields(MasterDataRepository.PRODUCT_BARCODES).first()
            val initial = original?.toFormValues(decimals, decimalSeparator)
                // With one unit only, Grocy chooses it
                ?: BarcodeFormValues(quId = units.singleOrNull()?.unit?.id)
            dialog = BarcodeDialogState(
                id = original?.id ?: MasterDataRepository.NEW_OBJECT_ID,
                original = original,
                initial = initial,
                productName = product.name,
                units = units,
                purchaseUnit = units.firstOrNull { it.unit.id == product.quIdPurchase }?.unit,
                stores = stores,
                showsStore = config.stockPriceTracking,
                decimals = decimals,
                userfields = userfieldInputs(fields, original?.userfields),
            )
        }
    }

    fun dismissDialog() {
        if (dialog?.isSaving != true) dialog = null
    }

    /** Saves the open form; it closes once the server saved the barcode. */
    fun save() {
        val form = dialog ?: return
        if (form.isSaving) return
        val values = form.values()
        form.errors = values.validate(form.decimals)
        form.userfieldErrors = form.userfields.validationErrors()
        val hasUnmatched = form.isUnitUnmatched || form.isStoreUnmatched ||
            form.userfields.any { it.isUnmatched }
        if (form.errors.isNotEmpty() || form.userfieldErrors.isNotEmpty() || hasUnmatched) return

        val productId = productId.value ?: return
        val barcode = values.toBarcode(form.id, productId, form.original, form.showsStore)
        form.isSaving = true
        form.error = null
        viewModelScope.launch {
            val error = changeErrorOf {
                repository.saveProductBarcode(barcode, form.userfields.changedValues())
            }
            form.isSaving = false
            if (error == null) {
                if (dialog === form) dialog = null
            } else {
                form.error = error
            }
        }
    }
}
