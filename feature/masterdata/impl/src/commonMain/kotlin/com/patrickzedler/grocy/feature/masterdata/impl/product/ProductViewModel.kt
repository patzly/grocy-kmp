package com.patrickzedler.grocy.feature.masterdata.impl.product

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.masterdata.PictureChange
import com.patrickzedler.grocy.core.model.masterdata.Location
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.ProductBarcode
import com.patrickzedler.grocy.core.model.masterdata.ProductGroup
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.Store
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.text.platformDecimalSeparator
import com.patrickzedler.grocy.core.network.GrocyApiException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Choices of the form's pickers, from the database. */
internal data class ProductOptions(
    val products: List<Product>,
    val productGroups: List<ProductGroup>,
    val locations: List<Location>,
    val quantityUnits: List<QuantityUnit>,
    val stores: List<Store>,
    val barcodes: List<ProductBarcode>,
    val config: ServerConfig,
)

internal sealed interface ProductLoadState {
    data object Loading : ProductLoadState

    /** The product does not exist (anymore), e.g. deleted on the server. */
    data object NotFound : ProductLoadState

    data class Ready(
        val form: ProductFormState,
        val userfields: List<UserfieldInput>,
        val features: ProductFormFeatures,
        val decimals: Int,
        val energyUnit: String,
    ) : ProductLoadState
}

internal sealed interface ProductEvent {
    /** An existing product was saved, the screen closes like Grocy's "Save & return". */
    data object Saved : ProductEvent

    /** A new product was created; the form now edits it, so barcodes can follow. */
    data object Created : ProductEvent

    data object Offline : ProductEvent

    data class Failed(val serverMessage: String?) : ProductEvent
}

/**
 * Grocy's product form: edits the product with [productId] or creates one. Reads the
 * database, saving is online only and stores what the server saved.
 */
internal class ProductViewModel(
    productId: Int?,
    private val repository: MasterDataRepository,
    private val decimalSeparator: Char = platformDecimalSeparator(),
) : ViewModel() {

    /** The edited product's id, [MasterDataRepository.NEW_PRODUCT_ID] until it is created. */
    var productId by mutableStateOf(productId ?: MasterDataRepository.NEW_PRODUCT_ID)
        private set

    var loadState by mutableStateOf<ProductLoadState>(ProductLoadState.Loading)
        private set

    var errors by mutableStateOf<Map<ProductField, FieldError>>(emptyMap())
        private set

    var userfieldErrors by mutableStateOf<Map<String, UserfieldError>>(emptyMap())
        private set

    var isSaving by mutableStateOf(false)
        private set

    var pictureChange by mutableStateOf<PictureChange>(PictureChange.Keep)

    private var original: Product? = null
    private var originalDescriptionText = ""

    private val eventChannel = Channel<ProductEvent>(Channel.BUFFERED)
    val events: Flow<ProductEvent> = eventChannel.receiveAsFlow()

    val options: StateFlow<ProductOptions?> = combine(
        repository.products,
        repository.productGroups,
        repository.locations,
        repository.quantityUnits,
        repository.stores,
        repository.productBarcodes,
        repository.serverConfig,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        ProductOptions(
            products = values[0] as List<Product>,
            productGroups = values[1] as List<ProductGroup>,
            locations = values[2] as List<Location>,
            quantityUnits = values[3] as List<QuantityUnit>,
            stores = values[4] as List<Store>,
            barcodes = values[5] as List<ProductBarcode>,
            config = values[6] as ServerConfig,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isNew: Boolean
        get() = productId == MasterDataRepository.NEW_PRODUCT_ID

    /** The saved picture, null without one or until it is saved. */
    val pictureFileName: String?
        get() = original?.pictureFileName

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        var product = if (isNew) null else repository.product(productId).first()
        if (!isNew && product == null) {
            // Opened from a link before the products were downloaded
            try {
                repository.refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (_: GrocyApiException) {
                // Not found either way
            }
            product = repository.product(productId).first()
        }
        if (!isNew && product == null) {
            loadState = ProductLoadState.NotFound
            return
        }
        val config = repository.serverConfig.first()
        val settings = repository.userSettings.first()
        val features = ProductFormFeatures.of(config)
        val decimals = settings.stockDecimalPlacesAmounts
        val values = product?.toFormValues(decimals, decimalSeparator)
            ?: newProductValues(settings.productPresets, features)
        original = product
        originalDescriptionText = values.description
        val userfields = repository.userfields(MasterDataRepository.PRODUCTS).first().map { field ->
            val value = if (product != null) {
                product.userfields[field.name]
            } else {
                defaultUserfieldValue(field)
            }
            UserfieldInput(field, value)
        }
        loadState = ProductLoadState.Ready(
            form = ProductFormState(values),
            userfields = userfields,
            features = features,
            decimals = decimals,
            energyUnit = config.energyUnit,
        )
    }

    /** Products that can be the parent: active ones without a parent, not this one. */
    fun parentCandidates(options: ProductOptions): List<Product> = options.products.filter {
        it.active && it.parentProductId == null && it.id != productId
    }

    /** True when other products use this one as parent; then it cannot get a parent itself. */
    fun hasSubProducts(options: ProductOptions): Boolean =
        !isNew && options.products.any { it.parentProductId == productId }

    /** The min. stock amount belongs to the parent when it accumulates its sub products. */
    fun isMinStockEditable(options: ProductOptions, parentId: Int?): Boolean =
        options.products.firstOrNull { it.id == parentId }
            ?.cumulateMinStockAmountOfSubProducts != true

    /**
     * The product a scanned [code] stands for: a grocycode ("grcy:p:12") or a barcode of a
     * product; null if none is known.
     */
    fun productForCode(options: ProductOptions, code: String): Product? {
        val id = GROCYCODE.matchEntire(code.trim())?.groupValues?.get(1)?.toIntOrNull()
            ?: options.barcodes.firstOrNull { it.barcode == code.trim() }?.productId
        return options.products.firstOrNull { it.id == id }
    }

    fun save() {
        val state = loadState as? ProductLoadState.Ready ?: return
        val options = options.value ?: return
        if (isSaving) return
        val values = state.form.values()
        errors = values.validate(
            features = state.features,
            decimals = state.decimals,
            minStockEditable = isMinStockEditable(options, values.parentProductId),
        )
        userfieldErrors = state.userfields.mapNotNull { input ->
            validateUserfield(input.userfield, input.value)?.let { input.userfield.name to it }
        }.toMap()
        if (errors.isNotEmpty() || userfieldErrors.isNotEmpty()) return

        val product = values.toProduct(productId, original, originalDescriptionText, state.features)
        val changedUserfields = state.userfields.filter { it.isChanged }
            .associate { it.userfield.name to it.value }
        isSaving = true
        viewModelScope.launch {
            val event = try {
                val wasNew = isNew
                val id = repository.saveProduct(product, changedUserfields, pictureChange)
                val saved = repository.product(id).first()
                if (wasNew && saved != null) {
                    productId = id
                    original = saved
                    originalDescriptionText = values.description
                    pictureChange = PictureChange.Keep
                    ProductEvent.Created
                } else {
                    ProductEvent.Saved
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: GrocyApiException.Unreachable) {
                ProductEvent.Offline
            } catch (e: GrocyApiException.HttpError) {
                ProductEvent.Failed(e.serverMessage)
            } catch (_: GrocyApiException) {
                ProductEvent.Failed(null)
            }
            isSaving = false
            eventChannel.send(event)
        }
    }

    /** The saved picture as image bytes, scaled by the server; null without one. */
    suspend fun loadPicture(maxWidth: Int): ByteArray? {
        val fileName = pictureFileName ?: return null
        return repository.productPicture(fileName, maxWidth)
    }

    private companion object {
        val GROCYCODE = Regex("""grcy:p:(\d+)(:.*)?""")
    }
}
