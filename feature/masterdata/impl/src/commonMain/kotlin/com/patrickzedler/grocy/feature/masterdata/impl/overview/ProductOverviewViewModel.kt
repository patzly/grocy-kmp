package com.patrickzedler.grocy.feature.masterdata.impl.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.util.RefreshController
import com.patrickzedler.grocy.core.data.util.RefreshState
import com.patrickzedler.grocy.core.model.masterdata.Location
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.ProductOverview
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.Store
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.server.UserSettings
import com.patrickzedler.grocy.core.model.text.platformDecimalSeparator
import com.patrickzedler.grocy.core.network.GrocyApiException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import androidx.compose.ui.text.intl.Locale

/**
 * Grocy's "Product overview" of the product the list [show]s: the stored overview at once (also
 * offline), the server's current one downloaded every time it opens.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class ProductOverviewViewModel(
    private val repository: MasterDataRepository,
    private val timeSource: TimeSource = TimeSource.Monotonic,
    private val decimalSeparator: Char = platformDecimalSeparator(),
    /** The app language, whose rules name the units of amounts. */
    private val language: String = Locale.current.toLanguageTag(),
) : ViewModel() {

    private val shownProductId = MutableStateFlow<Int?>(null)

    /** The product whose overview is open, null while closed; kept across rotations. */
    val productId: StateFlow<Int?> = shownProductId.asStateFlow()

    /**
     * Null while closed, while the database is read and when the product does not exist
     * (anymore); briefly the previous product's after [show], so compare [productId].
     */
    val state: StateFlow<ProductOverviewUiState?> = shownProductId
        .flatMapLatest { id -> if (id == null) flowOf(null) else overview(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    // One controller per opening: closing cancels its download, the next opening starts fresh
    private val controller = MutableStateFlow<RefreshController?>(null)
    private var controllerScope: CoroutineScope? = null

    /** Errors stay until the next download: the dialog shows them as long as it is open. */
    val refresh: StateFlow<RefreshState> = controller
        .flatMapLatest { it?.state ?: flowOf(RefreshState()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, RefreshState())

    /** Opens the overview of [productId] and downloads it again. */
    fun show(productId: Int) {
        hide()
        shownProductId.value = productId
        val scope = viewModelScope + Job(viewModelScope.coroutineContext[Job])
        controllerScope = scope
        controller.value = RefreshController(scope, timeSource) {
            val config = repository.serverConfig.first()
            repository.refreshProductOverview(productId, config.stockPriceTracking)
        }.also { it.refresh() }
    }

    fun hide() {
        controllerScope?.cancel()
        controllerScope = null
        controller.value = null
        shownProductId.value = null
    }

    fun refresh() {
        controller.value?.refresh()
    }

    /**
     * The product's picture in the dialog's size; offline the smaller list picture, null when
     * neither can be loaded.
     */
    suspend fun picture(fileName: String): ByteArray? = try {
        repository.productPictureOrThumbnail(fileName, PICTURE_WIDTH)
    } catch (e: CancellationException) {
        throw e
    } catch (_: GrocyApiException) {
        null
    }

    private fun overview(productId: Int) = combine(
        repository.product(productId),
        repository.productOverview(productId),
        repository.quantityUnits,
        repository.locations,
        repository.stores,
        repository.serverConfig,
        repository.userSettings,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        (values[0] as Product?)?.let { product ->
            buildProductOverview(
                product = product,
                overview = values[1] as ProductOverview?,
                quantityUnits = values[2] as List<QuantityUnit>,
                locations = values[3] as List<Location>,
                stores = values[4] as List<Store>,
                config = values[5] as ServerConfig,
                settings = values[6] as UserSettings,
                decimalSeparator = decimalSeparator,
                language = language,
            )
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L

        // The dialog's width on screens of up to 2.5x density, Grocy's web frontend uses 400
        const val PICTURE_WIDTH = 1_200
    }
}
