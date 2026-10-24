package com.patrickzedler.grocy.core.data.masterdata

import androidx.room3.withWriteTransaction
import com.patrickzedler.grocy.core.database.GrocyDatabase
import com.patrickzedler.grocy.core.database.ProductThumbnailEntity
import com.patrickzedler.grocy.core.database.ServerValueEntity
import com.patrickzedler.grocy.core.database.toEntity
import com.patrickzedler.grocy.core.database.toModel
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.SystemInfo
import com.patrickzedler.grocy.core.model.masterdata.Location
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.ProductBarcode
import com.patrickzedler.grocy.core.model.masterdata.ProductGroup
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.StockAmount
import com.patrickzedler.grocy.core.model.masterdata.Store
import com.patrickzedler.grocy.core.model.masterdata.Userfield
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.server.UserSettings
import com.patrickzedler.grocy.core.network.GrocyApi
import com.patrickzedler.grocy.core.network.GrocyApiException
import kotlin.random.Random
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.json.Json

/**
 * Grocy's master data, offline first: screens read the local [GrocyDatabase], [refresh]
 * downloads the server's state into it. Changes are online only and update the database with
 * what the server saved.
 *
 * @param connection the logged in server, null after a logout.
 */
class MasterDataRepository(
    private val api: GrocyApi,
    private val database: GrocyDatabase,
    private val connection: suspend () -> ServerConnection?,
    private val random: Random = Random.Default,
) {

    private val dao = database.masterDataDao()

    val products: Flow<List<Product>> =
        dao.observeProducts().map { list -> list.map { it.toModel() } }

    val productGroups: Flow<List<ProductGroup>> =
        dao.observeProductGroups().map { list -> list.map { it.toModel() } }

    val locations: Flow<List<Location>> =
        dao.observeLocations().map { list -> list.map { it.toModel() } }

    val quantityUnits: Flow<List<QuantityUnit>> =
        dao.observeQuantityUnits().map { list -> list.map { it.toModel() } }

    val stores: Flow<List<Store>> = dao.observeStores().map { list -> list.map { it.toModel() } }

    val productBarcodes: Flow<List<ProductBarcode>> =
        dao.observeProductBarcodes().map { list -> list.map { it.toModel() } }

    val stock: Flow<List<StockAmount>> =
        dao.observeStock().map { list -> list.map { it.toModel() } }

    /** Grocy's defaults until the server's configuration was downloaded once. */
    val serverConfig: Flow<ServerConfig> =
        observeValue(SERVER_CONFIG, ServerConfig.serializer(), ServerConfig())

    val userSettings: Flow<UserSettings> =
        observeValue(USER_SETTINGS, UserSettings.serializer(), UserSettings())

    /** Version of the server, null until it was downloaded once. */
    val systemInfo: Flow<SystemInfo?> =
        observeValue(SYSTEM_INFO, SystemInfo.serializer().nullable, null)

    fun product(id: Int): Flow<Product?> = dao.observeProduct(id).map { it?.toModel() }

    fun userfields(entity: String): Flow<List<Userfield>> =
        dao.observeUserfields(entity).map { list -> list.map { it.toModel() } }

    /**
     * Downloads everything the product screens show and replaces the local copy at once.
     *
     * @throws GrocyApiException e.g. [GrocyApiException.Unreachable] when offline; the local
     *   data stays as it was.
     */
    suspend fun refresh() {
        val connection = requireConnection()
        val download = coroutineScope {
            val products = async { api.products(connection) }
            val groups = async { api.productGroups(connection) }
            val locations = async { api.locations(connection) }
            val units = async { api.quantityUnits(connection) }
            val stores = async { api.stores(connection) }
            val barcodes = async { api.productBarcodes(connection) }
            val stock = async { api.stock(connection) }
            val userfields = async { api.userfields(connection, PRODUCTS) }
            val config = async { api.serverConfig(connection) }
            val settings = async { api.userSettings(connection) }
            val systemInfo = async { api.systemInfo(connection) }
            Download(
                products.await(), groups.await(), locations.await(), units.await(),
                stores.await(), barcodes.await(), stock.await(), userfields.await(),
                config.await(), settings.await(), systemInfo.await(),
            )
        }
        database.withWriteTransaction {
            dao.deleteProducts()
            dao.insertProducts(download.products.map { it.toEntity() })
            dao.deleteProductGroups()
            dao.insertProductGroups(download.productGroups.map { it.toEntity() })
            dao.deleteLocations()
            dao.insertLocations(download.locations.map { it.toEntity() })
            dao.deleteQuantityUnits()
            dao.insertQuantityUnits(download.quantityUnits.map { it.toEntity() })
            dao.deleteStores()
            dao.insertStores(download.stores.map { it.toEntity() })
            dao.deleteProductBarcodes()
            dao.insertProductBarcodes(download.barcodes.map { it.toEntity() })
            dao.deleteStock()
            dao.insertStock(download.stock.map { it.toEntity() })
            dao.deleteUserfields(PRODUCTS)
            dao.insertUserfields(download.userfields.map { it.toEntity() })
            storeConfiguration(download.config, download.settings, download.systemInfo)
            dao.deleteUnusedThumbnails()
        }
    }

    /**
     * Downloads the server's version, configuration and the user's settings again, e.g. after
     * the server was updated or reconfigured.
     *
     * @throws GrocyApiException e.g. [GrocyApiException.Unreachable] when offline.
     */
    suspend fun reloadConfiguration() {
        val connection = requireConnection()
        val (config, settings, systemInfo) = coroutineScope {
            val config = async { api.serverConfig(connection) }
            val settings = async { api.userSettings(connection) }
            val systemInfo = async { api.systemInfo(connection) }
            Triple(config.await(), settings.await(), systemInfo.await())
        }
        database.withWriteTransaction { storeConfiguration(config, settings, systemInfo) }
    }

    /** Downloads only the server's version, which an update of the server changes. */
    suspend fun refreshSystemInfo() {
        val systemInfo = api.systemInfo(requireConnection())
        dao.upsertServerValue(value(SYSTEM_INFO, SystemInfo.serializer(), systemInfo))
    }

    private suspend fun storeConfiguration(
        config: ServerConfig,
        settings: UserSettings,
        systemInfo: SystemInfo,
    ) {
        dao.upsertServerValue(value(SERVER_CONFIG, ServerConfig.serializer(), config))
        dao.upsertServerValue(value(USER_SETTINGS, UserSettings.serializer(), settings))
        dao.upsertServerValue(value(SYSTEM_INFO, SystemInfo.serializer(), systemInfo))
    }

    /**
     * The picture [fileName] of a product, scaled for the product list: stored for offline use,
     * downloaded once when missing.
     */
    suspend fun productThumbnail(fileName: String): ByteArray {
        dao.thumbnail(fileName)?.let { return it }
        val bytes = api.productPicture(requireConnection(), fileName, THUMBNAIL_WIDTH)
        dao.upsertThumbnail(ProductThumbnailEntity(fileName, bytes))
        return bytes
    }

    /** Downloads the list pictures not stored yet, so the list shows them offline too. */
    suspend fun downloadMissingThumbnails() {
        dao.missingThumbnails().forEach { productThumbnail(it) }
    }

    /**
     * Creates or updates [product] on the server like Grocy's product form does, then stores
     * what the server saved. Online only.
     *
     * @param userfields changed userfield values by name; untouched fields are not sent.
     * @return the id of the product, new for a created one.
     * @throws GrocyApiException if the server cannot be reached or rejects the product.
     */
    suspend fun saveProduct(
        product: Product,
        userfields: Map<String, String?> = emptyMap(),
        picture: PictureChange = PictureChange.Keep,
    ): Int {
        val connection = requireConnection()
        val oldPicture = product.pictureFileName
        val newPicture = (picture as? PictureChange.Replace)?.let { pictureFileName(it.fileName) }
        val saved = when (picture) {
            PictureChange.Keep -> product
            PictureChange.Delete -> product.copy(pictureFileName = null)
            is PictureChange.Replace -> product.copy(pictureFileName = newPicture)
        }

        val id = if (product.id == NEW_PRODUCT_ID) {
            api.addProduct(connection, saved)
        } else {
            api.editProduct(connection, saved)
            product.id
        }
        if (userfields.isNotEmpty()) {
            api.setUserfields(connection, PRODUCTS, id, userfields)
        }
        if (picture is PictureChange.Replace && newPicture != null) {
            api.uploadProductPicture(connection, newPicture, picture.bytes)
        }
        // Old pictures are removed only once the product no longer refers to them
        if (picture != PictureChange.Keep && oldPicture != null) {
            runCatchingApi { api.deleteProductPicture(connection, oldPicture) }
        }
        dao.upsertProduct(api.product(connection, id).toEntity())
        return id
    }

    /** Deletes the product with everything that refers to it on the server. Online only. */
    suspend fun deleteProduct(id: Int) {
        api.deleteProduct(requireConnection(), id)
        dao.deleteProduct(id)
    }

    /** The picture [fileName] of a product, scaled by the server to [maxWidth] pixels. */
    suspend fun productPicture(fileName: String, maxWidth: Int): ByteArray =
        api.productPicture(requireConnection(), fileName, maxWidth)

    /** Forgets the data of the server, e.g. on logout. */
    suspend fun clear() {
        database.clearAllTables()
    }

    private suspend fun requireConnection(): ServerConnection =
        connection() ?: throw GrocyApiException.Unauthorized()

    /** Like Grocy: random prefix (it never overwrites files) plus the cleaned original name. */
    private fun pictureFileName(originalName: String): String {
        val prefix = (1..2).joinToString("") { random.nextLong(0, Long.MAX_VALUE).toString(36) }
        return prefix + cleanFileName(originalName)
    }

    private fun <T> observeValue(key: String, serializer: KSerializer<T>, default: T): Flow<T> =
        dao.observeServerValue(key).map { value ->
            value?.let { decodeOrNull(serializer, it.json) } ?: default
        }

    private fun <T> value(key: String, serializer: KSerializer<T>, value: T) =
        ServerValueEntity(key, Json.encodeToString(serializer, value))

    private fun <T> decodeOrNull(serializer: KSerializer<T>, json: String): T? = try {
        Json { ignoreUnknownKeys = true }.decodeFromString(serializer, json)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    private class Download(
        val products: List<Product>,
        val productGroups: List<ProductGroup>,
        val locations: List<Location>,
        val quantityUnits: List<QuantityUnit>,
        val stores: List<Store>,
        val barcodes: List<ProductBarcode>,
        val stock: List<StockAmount>,
        val userfields: List<Userfield>,
        val config: ServerConfig,
        val settings: UserSettings,
        val systemInfo: SystemInfo,
    )

    companion object {
        /** Id of a product that does not exist on the server yet. */
        const val NEW_PRODUCT_ID = 0

        const val PRODUCTS = "products"

        /** Pixels of a list picture: the 56.dp of the list on screens of up to 3.5x density. */
        const val THUMBNAIL_WIDTH = 196
        private const val SERVER_CONFIG = "server_config"
        private const val USER_SETTINGS = "user_settings"
        private const val SYSTEM_INFO = "system_info"
    }
}

/** What happens to a product's picture when it is saved. */
sealed interface PictureChange {
    data object Keep : PictureChange

    data object Delete : PictureChange

    /** A new picture, uploaded under a new name derived from [fileName]. */
    class Replace(val bytes: ByteArray, val fileName: String) : PictureChange
}

/** File name without spaces and characters other than ASCII, as Grocy's web frontend does it. */
internal fun cleanFileName(name: String): String = name.lowercase()
    .replace("ä", "ae")
    .replace("ö", "oe")
    .replace("ü", "ue")
    .replace("ß", "ss")
    .filter { it.code in 0x21..0x7E && it !in "/\\?%*:|\"<>" }
    .ifEmpty { "picture" }

/** Leftovers such as an old picture must not fail an otherwise saved change. */
private suspend fun runCatchingApi(block: suspend () -> Unit) {
    try {
        block()
    } catch (_: GrocyApiException) {
        // The file stays on the server, nothing refers to it anymore
    }
}
