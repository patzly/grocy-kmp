package com.patrickzedler.grocy.core.data.masterdata

import androidx.room3.withWriteTransaction
import com.patrickzedler.grocy.core.database.GrocyDatabase
import com.patrickzedler.grocy.core.database.ProductOverviewEntity
import com.patrickzedler.grocy.core.database.ProductThumbnailEntity
import com.patrickzedler.grocy.core.database.ServerValueEntity
import com.patrickzedler.grocy.core.database.toEntity
import com.patrickzedler.grocy.core.database.toModel
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.SystemInfo
import com.patrickzedler.grocy.core.model.User
import com.patrickzedler.grocy.core.model.masterdata.Battery
import com.patrickzedler.grocy.core.model.masterdata.Chore
import com.patrickzedler.grocy.core.model.masterdata.Location
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.ProductBarcode
import com.patrickzedler.grocy.core.model.masterdata.ProductGroup
import com.patrickzedler.grocy.core.model.masterdata.ProductOverview
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnitConversion
import com.patrickzedler.grocy.core.model.masterdata.ResolvedQuantityUnitConversion
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingList
import com.patrickzedler.grocy.core.model.masterdata.StockAmount
import com.patrickzedler.grocy.core.model.masterdata.Store
import com.patrickzedler.grocy.core.model.masterdata.TaskCategory
import com.patrickzedler.grocy.core.model.masterdata.Userentity
import com.patrickzedler.grocy.core.model.masterdata.Userfield
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.server.UserSettingValue
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

    /** The defined conversions, default ones and the overrides of products. */
    val quantityUnitConversions: Flow<List<QuantityUnitConversion>> =
        dao.observeQuantityUnitConversions().map { list -> list.map { it.toModel() } }

    /** Every conversion Grocy derived from the defined ones. */
    val resolvedQuantityUnitConversions: Flow<List<ResolvedQuantityUnitConversion>> =
        dao.observeResolvedQuantityUnitConversions().map { list -> list.map { it.toModel() } }

    val shoppingLists: Flow<List<ShoppingList>> =
        dao.observeShoppingLists().map { list -> list.map { it.toModel() } }

    val chores: Flow<List<Chore>> = dao.observeChores().map { list -> list.map { it.toModel() } }

    val batteries: Flow<List<Battery>> =
        dao.observeBatteries().map { list -> list.map { it.toModel() } }

    val taskCategories: Flow<List<TaskCategory>> =
        dao.observeTaskCategories().map { list -> list.map { it.toModel() } }

    val userentities: Flow<List<Userentity>> =
        dao.observeUserentities().map { list -> list.map { it.toModel() } }

    /** The server's users; empty when the API key's user may not read them. */
    val users: Flow<List<User>> = dao.observeUsers().map { list -> list.map { it.toModel() } }

    /** The userfields of every entity, ordered by entity and like Grocy's forms. */
    val allUserfields: Flow<List<Userfield>> =
        dao.observeAllUserfields().map { list -> list.map { it.toModel() } }

    /** Grocy's defaults until the server's configuration was downloaded once. */
    val serverConfig: Flow<ServerConfig> =
        observeValue(SERVER_CONFIG, ServerConfig.serializer(), ServerConfig())

    val userSettings: Flow<UserSettings> =
        observeValue(USER_SETTINGS, UserSettings.serializer(), UserSettings())

    /** Version of the server, null until it was downloaded once. */
    val systemInfo: Flow<SystemInfo?> =
        observeValue(SYSTEM_INFO, SystemInfo.serializer().nullable, null)

    fun product(id: Int): Flow<Product?> = dao.observeProduct(id).map { it?.toModel() }

    /** The stored "Product overview" of a product, null until it was downloaded once. */
    fun productOverview(productId: Int): Flow<ProductOverview?> =
        dao.observeProductOverview(productId).map { entity ->
            entity?.let { decodeOrNull(ProductOverview.serializer(), it.json) }
        }

    /**
     * Downloads the "Product overview" of an active product and stores it for offline use.
     *
     * @param withPriceHistory false when the server does not track prices.
     * @throws GrocyApiException e.g. [GrocyApiException.Unreachable] when offline.
     */
    suspend fun refreshProductOverview(productId: Int, withPriceHistory: Boolean) {
        val overview = api.productOverview(requireConnection(), productId, withPriceHistory)
        val json = Json.encodeToString(ProductOverview.serializer(), overview)
        dao.upsertProductOverview(ProductOverviewEntity(productId, json))
    }

    fun userfields(entity: String): Flow<List<Userfield>> =
        dao.observeUserfields(entity).map { list -> list.map { it.toModel() } }

    /**
     * Downloads all master data and replaces the local copy at once.
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
            val conversions = async { api.quantityUnitConversions(connection) }
            val resolved = async { api.resolvedQuantityUnitConversions(connection) }
            val shoppingLists = async { api.shoppingLists(connection) }
            val stock = async { api.stock(connection) }
            val chores = async { api.chores(connection) }
            val batteries = async { api.batteries(connection) }
            val taskCategories = async { api.taskCategories(connection) }
            val userentities = async { api.userentities(connection) }
            // Reading users needs a permission of its own, the rest works without them
            val users = async { usersOrNull(connection) }
            val userfields = async { api.allUserfields(connection) }
            val config = async { api.serverConfig(connection) }
            val settings = async { api.userSettings(connection) }
            val systemInfo = async { api.systemInfo(connection) }
            Download(
                products = products.await(),
                productGroups = groups.await(),
                locations = locations.await(),
                quantityUnits = units.await(),
                stores = stores.await(),
                barcodes = barcodes.await(),
                conversions = conversions.await(),
                resolvedConversions = resolved.await(),
                shoppingLists = shoppingLists.await(),
                stock = stock.await(),
                chores = chores.await(),
                batteries = batteries.await(),
                taskCategories = taskCategories.await(),
                userentities = userentities.await(),
                users = users.await(),
                userfields = userfields.await(),
                config = config.await(),
                settings = settings.await(),
                systemInfo = systemInfo.await(),
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
            dao.deleteQuantityUnitConversions()
            dao.insertQuantityUnitConversions(download.conversions.map { it.toEntity() })
            storeResolvedConversions(download.resolvedConversions)
            dao.deleteShoppingLists()
            dao.insertShoppingLists(download.shoppingLists.map { it.toEntity() })
            dao.deleteStock()
            dao.insertStock(download.stock.map { it.toEntity() })
            dao.deleteChores()
            dao.insertChores(download.chores.map { it.toEntity() })
            dao.deleteBatteries()
            dao.insertBatteries(download.batteries.map { it.toEntity() })
            dao.deleteTaskCategories()
            dao.insertTaskCategories(download.taskCategories.map { it.toEntity() })
            dao.deleteUserentities()
            dao.insertUserentities(download.userentities.map { it.toEntity() })
            // Without the permission the stored users stay, they still name assignments
            download.users?.let { users ->
                dao.deleteUsers()
                dao.insertUsers(users.map { it.toEntity() })
            }
            dao.deleteUserfields()
            dao.insertUserfields(download.userfields.map { it.toEntity() })
            storeConfiguration(download.config, download.settings, download.systemInfo)
            dao.deleteUnusedThumbnails()
            dao.deleteUnusedProductOverviews()
        }
    }

    private suspend fun storeResolvedConversions(resolved: List<ResolvedQuantityUnitConversion>) {
        dao.deleteResolvedQuantityUnitConversions()
        dao.insertResolvedQuantityUnitConversions(resolved.map { it.toEntity() })
    }

    private suspend fun usersOrNull(connection: ServerConnection): List<User>? = try {
        api.users(connection)
    } catch (e: GrocyApiException.HttpError) {
        if (e.statusCode == HTTP_FORBIDDEN) null else throw e
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

    /**
     * Saves [newValue] as one of the user's settings ([key] like Grocy's, e.g.
     * "stock_due_soon_days"), then stores the user's settings as the server has them now.
     * Online only.
     *
     * @throws GrocyApiException if the server cannot be reached or rejects the value.
     */
    suspend fun saveUserSetting(key: String, newValue: UserSettingValue) {
        val connection = requireConnection()
        api.saveUserSetting(connection, key, newValue)
        val settings = api.userSettings(connection)
        dao.upsertServerValue(value(USER_SETTINGS, UserSettings.serializer(), settings))
    }

    /** Downloads only the server's version, which an update of the server changes. */
    suspend fun refreshSystemInfo(): SystemInfo {
        val systemInfo = api.systemInfo(requireConnection())
        dao.upsertServerValue(value(SYSTEM_INFO, SystemInfo.serializer(), systemInfo))
        return systemInfo
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

        val id = if (product.id == NEW_OBJECT_ID) {
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

    /**
     * Creates the location (id [NEW_OBJECT_ID]) or updates it like Grocy's form does, then
     * stores what the server saved. Online only, like all following saves.
     *
     * @param userfields changed userfield values by name; untouched fields are not sent.
     * @return the id of the location, new for a created one.
     * @throws GrocyApiException if the server cannot be reached or rejects the location.
     */
    suspend fun saveLocation(location: Location, userfields: Map<String, String?>): Int =
        saveObject(LOCATIONS, userfields, { api.saveLocation(it, location) }) { connection, id ->
            dao.upsertLocation(api.location(connection, id).toEntity())
        }

    suspend fun saveStore(store: Store, userfields: Map<String, String?>): Int =
        saveObject(STORES, userfields, { api.saveStore(it, store) }) { connection, id ->
            dao.upsertStore(api.store(connection, id).toEntity())
        }

    suspend fun saveQuantityUnit(unit: QuantityUnit, userfields: Map<String, String?>): Int =
        saveObject(QUANTITY_UNITS, userfields, { api.saveQuantityUnit(it, unit) }) { c, id ->
            dao.upsertQuantityUnit(api.quantityUnit(c, id).toEntity())
        }

    suspend fun saveProductGroup(group: ProductGroup, userfields: Map<String, String?>): Int =
        saveObject(PRODUCT_GROUPS, userfields, { api.saveProductGroup(it, group) }) { c, id ->
            dao.upsertProductGroup(api.productGroup(c, id).toEntity())
        }

    suspend fun saveChore(chore: Chore, userfields: Map<String, String?>): Int =
        saveObject(CHORES, userfields, { api.saveChore(it, chore) }) { connection, id ->
            dao.upsertChore(api.chore(connection, id).toEntity())
        }

    suspend fun saveBattery(battery: Battery, userfields: Map<String, String?>): Int =
        saveObject(BATTERIES, userfields, { api.saveBattery(it, battery) }) { connection, id ->
            dao.upsertBattery(api.battery(connection, id).toEntity())
        }

    suspend fun saveTaskCategory(category: TaskCategory, userfields: Map<String, String?>): Int =
        saveObject(TASK_CATEGORIES, userfields, { api.saveTaskCategory(it, category) }) { c, id ->
            dao.upsertTaskCategory(api.taskCategory(c, id).toEntity())
        }

    /** Creates or updates a product's barcode like Grocy's barcode form. */
    suspend fun saveProductBarcode(barcode: ProductBarcode, userfields: Map<String, String?>): Int =
        saveObject(PRODUCT_BARCODES, userfields, { api.saveProductBarcode(it, barcode) }) { c, id ->
            dao.upsertProductBarcode(api.productBarcode(c, id).toEntity())
        }

    /**
     * Creates or updates a conversion like Grocy's conversion form; the conversions Grocy
     * derives change with it, so they are downloaded again.
     */
    suspend fun saveQuantityUnitConversion(
        conversion: QuantityUnitConversion,
        userfields: Map<String, String?>,
    ): Int = saveObject(
        entity = QU_CONVERSIONS,
        userfields = userfields,
        save = { api.saveQuantityUnitConversion(it, conversion) },
    ) { connection, id ->
        val saved = api.quantityUnitConversion(connection, id)
        val resolved = api.resolvedQuantityUnitConversions(connection)
        database.withWriteTransaction {
            dao.upsertQuantityUnitConversion(saved.toEntity())
            storeResolvedConversions(resolved)
        }
    }

    /** Userentities have no userfields of their own. */
    suspend fun saveUserentity(userentity: Userentity): Int =
        saveObject(USERENTITIES, emptyMap(), { api.saveUserentity(it, userentity) }) { c, id ->
            dao.upsertUserentity(api.userentity(c, id).toEntity())
        }

    suspend fun saveUserfield(userfield: Userfield): Int =
        saveObject(USERFIELDS, emptyMap(), { api.saveUserfield(it, userfield) }) { c, id ->
            dao.upsertUserfield(api.userfield(c, id).toEntity())
        }

    /** Saves an object with [save], then its changed [userfields], then stores the result. */
    private suspend fun saveObject(
        entity: String,
        userfields: Map<String, String?>,
        save: suspend (ServerConnection) -> Int,
        store: suspend (ServerConnection, id: Int) -> Unit,
    ): Int {
        val connection = requireConnection()
        val id = save(connection)
        if (userfields.isNotEmpty()) {
            api.setUserfields(connection, entity, id, userfields)
        }
        store(connection, id)
        return id
    }

    /**
     * Deletes the object [id] of [entity] (one of the entity names below) on the server, which
     * removes everything that refers to it there, and its local copy. Online only.
     */
    suspend fun deleteObject(entity: String, id: Int) {
        api.deleteObject(requireConnection(), entity, id)
        when (entity) {
            PRODUCTS -> {
                dao.deleteProduct(id)
                dao.deleteProductOverview(id)
            }
            LOCATIONS -> dao.deleteLocation(id)
            STORES -> dao.deleteStore(id)
            QUANTITY_UNITS -> dao.deleteQuantityUnit(id)
            PRODUCT_GROUPS -> dao.deleteProductGroup(id)
            CHORES -> dao.deleteChore(id)
            BATTERIES -> dao.deleteBattery(id)
            TASK_CATEGORIES -> dao.deleteTaskCategory(id)
            USERENTITIES -> dao.deleteUserentity(id)
            USERFIELDS -> dao.deleteUserfield(id)
            PRODUCT_BARCODES -> dao.deleteProductBarcode(id)
            QU_CONVERSIONS -> {
                dao.deleteQuantityUnitConversion(id)
                // Grocy derives the others anew
                val resolved = api.resolvedQuantityUnitConversions(requireConnection())
                database.withWriteTransaction { storeResolvedConversions(resolved) }
            }
        }
    }

    /** Copies the product on the server like Grocy's "Copy" and returns the copy's id. */
    suspend fun copyProduct(id: Int): Int {
        val connection = requireConnection()
        val copyId = api.copyProduct(connection, id)
        dao.upsertProduct(api.product(connection, copyId).toEntity())
        return copyId
    }

    /**
     * Merges product [removeId] into [keepId] on the server, then downloads everything again:
     * the merge changes whatever referred to the removed product. Online only.
     */
    suspend fun mergeProducts(keepId: Int, removeId: Int) {
        api.mergeProducts(requireConnection(), keepId, removeId)
        refresh()
    }

    /** Merges chore [removeId] into [keepId] like [mergeProducts]. */
    suspend fun mergeChores(keepId: Int, removeId: Int) {
        api.mergeChores(requireConnection(), keepId, removeId)
        refresh()
    }

    /**
     * The picture [fileName] of a product, scaled by the server to [maxWidth] pixels; offline
     * the stored list picture, which is smaller.
     *
     * @throws GrocyApiException when it is neither stored nor downloadable.
     */
    suspend fun productPictureOrThumbnail(fileName: String, maxWidth: Int): ByteArray = try {
        productPicture(fileName, maxWidth)
    } catch (e: GrocyApiException) {
        dao.thumbnail(fileName) ?: throw e
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
        lenientJson.decodeFromString(serializer, json)
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
        val conversions: List<QuantityUnitConversion>,
        val resolvedConversions: List<ResolvedQuantityUnitConversion>,
        val shoppingLists: List<ShoppingList>,
        val stock: List<StockAmount>,
        val chores: List<Chore>,
        val batteries: List<Battery>,
        val taskCategories: List<TaskCategory>,
        val userentities: List<Userentity>,
        /** Null without the permission to read users. */
        val users: List<User>?,
        val userfields: List<Userfield>,
        val config: ServerConfig,
        val settings: UserSettings,
        val systemInfo: SystemInfo,
    )

    companion object {
        /** Id of an object that does not exist on the server yet. */
        const val NEW_OBJECT_ID = GrocyApi.NEW_OBJECT_ID

        /** Id of a product that does not exist on the server yet. */
        const val NEW_PRODUCT_ID = NEW_OBJECT_ID

        // Grocy's entity names, e.g. of userfields
        const val PRODUCTS = "products"
        const val LOCATIONS = "locations"
        const val STORES = "shopping_locations"
        const val QUANTITY_UNITS = "quantity_units"
        const val PRODUCT_GROUPS = "product_groups"
        const val CHORES = "chores"
        const val BATTERIES = "batteries"
        const val TASK_CATEGORIES = "task_categories"
        const val USERENTITIES = "userentities"
        const val USERFIELDS = "userfields"
        const val PRODUCT_BARCODES = "product_barcodes"
        const val QU_CONVERSIONS = "quantity_unit_conversions"
        const val SHOPPING_LIST = "shopping_list"

        /** Userfields of stock entries belong to their `stock_id`. */
        const val STOCK = "stock"

        /** Prefix of the userfield entity of a userentity, e.g. "userentity-books". */
        const val USERENTITY_PREFIX = "userentity-"

        private const val HTTP_FORBIDDEN = 403

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

// Stored values of older versions may hold keys this version no longer knows
private val lenientJson = Json { ignoreUnknownKeys = true }
