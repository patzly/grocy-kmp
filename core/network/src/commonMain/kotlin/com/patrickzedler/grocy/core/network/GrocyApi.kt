package com.patrickzedler.grocy.core.network

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
import com.patrickzedler.grocy.core.model.masterdata.StockAmount
import com.patrickzedler.grocy.core.model.masterdata.Store
import com.patrickzedler.grocy.core.model.masterdata.TaskCategory
import com.patrickzedler.grocy.core.model.masterdata.Userentity
import com.patrickzedler.grocy.core.model.masterdata.Userfield
import com.patrickzedler.grocy.core.model.recipes.MealPlanSection
import com.patrickzedler.grocy.core.model.recipes.Recipe
import com.patrickzedler.grocy.core.model.recipes.RecipeFulfillment
import com.patrickzedler.grocy.core.model.recipes.RecipeIngredient
import com.patrickzedler.grocy.core.model.recipes.RecipeNesting
import com.patrickzedler.grocy.core.model.recipes.ResolvedRecipeIngredient
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.server.UserSettingValue
import com.patrickzedler.grocy.core.model.server.UserSettings
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingList
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListItem
import com.patrickzedler.grocy.core.model.stock.BookingResult
import com.patrickzedler.grocy.core.model.stock.ConsumeBooking
import com.patrickzedler.grocy.core.model.stock.InventoryBooking
import com.patrickzedler.grocy.core.model.stock.OpenBooking
import com.patrickzedler.grocy.core.model.stock.ProductLocationAmount
import com.patrickzedler.grocy.core.model.stock.ProductPurchase
import com.patrickzedler.grocy.core.model.stock.PurchaseBooking
import com.patrickzedler.grocy.core.model.stock.StockEntry
import com.patrickzedler.grocy.core.model.stock.StockLocationAmount
import com.patrickzedler.grocy.core.model.stock.StockLogEntry
import com.patrickzedler.grocy.core.model.stock.TransferBooking
import com.patrickzedler.grocy.core.network.dto.AveragePriceDto
import com.patrickzedler.grocy.core.network.dto.BatteryDto
import com.patrickzedler.grocy.core.network.dto.ChoreDto
import com.patrickzedler.grocy.core.network.dto.CreatedObjectDto
import com.patrickzedler.grocy.core.network.dto.ErrorDto
import com.patrickzedler.grocy.core.network.dto.LastPurchaseDto
import com.patrickzedler.grocy.core.network.dto.LocationDto
import com.patrickzedler.grocy.core.network.dto.MealPlanSectionDto
import com.patrickzedler.grocy.core.network.dto.PriceHistoryDto
import com.patrickzedler.grocy.core.network.dto.ProductBarcodeDto
import com.patrickzedler.grocy.core.network.dto.ProductDetailsDto
import com.patrickzedler.grocy.core.network.dto.ProductDto
import com.patrickzedler.grocy.core.network.dto.ProductGroupDto
import com.patrickzedler.grocy.core.network.dto.ProductLocationAmountDto
import com.patrickzedler.grocy.core.network.dto.ProductStockDetailsDto
import com.patrickzedler.grocy.core.network.dto.QuantityUnitConversionDto
import com.patrickzedler.grocy.core.network.dto.QuantityUnitDto
import com.patrickzedler.grocy.core.network.dto.RecipeDto
import com.patrickzedler.grocy.core.network.dto.RecipeFulfillmentDto
import com.patrickzedler.grocy.core.network.dto.RecipeIngredientDto
import com.patrickzedler.grocy.core.network.dto.RecipeNestingDto
import com.patrickzedler.grocy.core.network.dto.RecipeObjectDto
import com.patrickzedler.grocy.core.network.dto.ResolvedQuantityUnitConversionDto
import com.patrickzedler.grocy.core.network.dto.ResolvedRecipeIngredientDto
import com.patrickzedler.grocy.core.network.dto.ShoppingListDto
import com.patrickzedler.grocy.core.network.dto.ShoppingListItemDto
import com.patrickzedler.grocy.core.network.dto.StockDto
import com.patrickzedler.grocy.core.network.dto.StockEntryDto
import com.patrickzedler.grocy.core.network.dto.StockLocationDto
import com.patrickzedler.grocy.core.network.dto.StockLogDto
import com.patrickzedler.grocy.core.network.dto.StoreDto
import com.patrickzedler.grocy.core.network.dto.SystemInfoDto
import com.patrickzedler.grocy.core.network.dto.TaskCategoryDto
import com.patrickzedler.grocy.core.network.dto.UserDto
import com.patrickzedler.grocy.core.network.dto.UserentityDto
import com.patrickzedler.grocy.core.network.dto.UserfieldDto
import com.patrickzedler.grocy.core.network.dto.VolatileStockDto
import com.patrickzedler.grocy.core.network.dto.flag
import com.patrickzedler.grocy.core.network.dto.toJson
import com.patrickzedler.grocy.core.network.dto.toServerConfig
import com.patrickzedler.grocy.core.network.dto.toUserSettings
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.appendPathSegments
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException
import kotlin.io.encoding.Base64
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * The Grocy REST API.
 *
 * [ingressSessionSource] creates Home Assistant ingress sessions, [apiKeyAsQueryParameter]
 * decides where the API key goes; both have platform defaults and are replaced in tests.
 */
class GrocyApi(
    private val httpClient: HttpClient,
    ingressSessionSource: IngressSessionSource = WebSocketIngressSessionSource(httpClient),
    private val apiKeyAsQueryParameter: Boolean = sendApiKeyAsQueryParameter,
) {

    private val ingress = HomeAssistantIngress(ingressSessionSource)

    suspend fun systemInfo(connection: ServerConnection): SystemInfo =
        get<SystemInfoDto>(connection, "system", "info").toModel()

    /** Grocy returns the current user as a list with exactly one entry. */
    suspend fun currentUser(connection: ServerConnection): User =
        get<List<UserDto>>(connection, "user").singleOrNull()?.toModel()
            ?: throw GrocyApiException.NotGrocyServer()

    /** Feature flags and the energy unit of the server. */
    suspend fun serverConfig(connection: ServerConnection): ServerConfig =
        get<JsonObject>(connection, "system", "config").toServerConfig()

    suspend fun userSettings(connection: ServerConnection): UserSettings =
        get<JsonObject>(connection, "user", "settings").toUserSettings()

    /** All products, disabled ones included, with their userfield values. */
    suspend fun products(connection: ServerConnection): List<Product> =
        get<List<ProductDto>>(connection, "objects", PRODUCTS).map { it.toModel() }

    suspend fun product(connection: ServerConnection, id: Int): Product =
        get<ProductDto>(connection, "objects", PRODUCTS, id.toString()).toModel()

    suspend fun productGroups(connection: ServerConnection): List<ProductGroup> =
        get<List<ProductGroupDto>>(connection, "objects", PRODUCT_GROUPS).map { it.toModel() }

    suspend fun productGroup(connection: ServerConnection, id: Int): ProductGroup =
        get<ProductGroupDto>(connection, "objects", PRODUCT_GROUPS, id.toString()).toModel()

    /** Creates the group without id or updates it; returns its id. */
    suspend fun saveProductGroup(connection: ServerConnection, group: ProductGroup): Int =
        saveObject(connection, PRODUCT_GROUPS, group.id, group.toJson())

    suspend fun locations(connection: ServerConnection): List<Location> =
        get<List<LocationDto>>(connection, "objects", LOCATIONS).map { it.toModel() }

    suspend fun location(connection: ServerConnection, id: Int): Location =
        get<LocationDto>(connection, "objects", LOCATIONS, id.toString()).toModel()

    suspend fun saveLocation(connection: ServerConnection, location: Location): Int =
        saveObject(connection, LOCATIONS, location.id, location.toJson())

    suspend fun quantityUnits(connection: ServerConnection): List<QuantityUnit> =
        get<List<QuantityUnitDto>>(connection, "objects", QUANTITY_UNITS).map { it.toModel() }

    suspend fun quantityUnit(connection: ServerConnection, id: Int): QuantityUnit =
        get<QuantityUnitDto>(connection, "objects", QUANTITY_UNITS, id.toString()).toModel()

    suspend fun saveQuantityUnit(connection: ServerConnection, unit: QuantityUnit): Int =
        saveObject(connection, QUANTITY_UNITS, unit.id, unit.toJson())

    suspend fun stores(connection: ServerConnection): List<Store> =
        get<List<StoreDto>>(connection, "objects", STORES).map { it.toModel() }

    suspend fun store(connection: ServerConnection, id: Int): Store =
        get<StoreDto>(connection, "objects", STORES, id.toString()).toModel()

    suspend fun saveStore(connection: ServerConnection, store: Store): Int =
        saveObject(connection, STORES, store.id, store.toJson())

    suspend fun chores(connection: ServerConnection): List<Chore> =
        get<List<ChoreDto>>(connection, "objects", CHORES).map { it.toModel() }

    suspend fun chore(connection: ServerConnection, id: Int): Chore =
        get<ChoreDto>(connection, "objects", CHORES, id.toString()).toModel()

    /**
     * Saves the chore and lets Grocy assign its next execution again, like its form does.
     */
    suspend fun saveChore(connection: ServerConnection, chore: Chore): Int {
        val id = saveObject(connection, CHORES, chore.id, chore.toJson())
        execute(
            connection,
            HttpMethod.Post,
            listOf("chores", "executions", "calculate-next-assignments"),
            body = jsonBody(buildJsonObject { put("chore_id", id) }),
        )
        return id
    }

    suspend fun batteries(connection: ServerConnection): List<Battery> =
        get<List<BatteryDto>>(connection, "objects", BATTERIES).map { it.toModel() }

    suspend fun battery(connection: ServerConnection, id: Int): Battery =
        get<BatteryDto>(connection, "objects", BATTERIES, id.toString()).toModel()

    suspend fun saveBattery(connection: ServerConnection, battery: Battery): Int =
        saveObject(connection, BATTERIES, battery.id, battery.toJson())

    suspend fun taskCategories(connection: ServerConnection): List<TaskCategory> =
        get<List<TaskCategoryDto>>(connection, "objects", TASK_CATEGORIES).map { it.toModel() }

    suspend fun taskCategory(connection: ServerConnection, id: Int): TaskCategory =
        get<TaskCategoryDto>(connection, "objects", TASK_CATEGORIES, id.toString()).toModel()

    suspend fun saveTaskCategory(connection: ServerConnection, category: TaskCategory): Int =
        saveObject(connection, TASK_CATEGORIES, category.id, category.toJson())

    suspend fun userentities(connection: ServerConnection): List<Userentity> =
        get<List<UserentityDto>>(connection, "objects", USERENTITIES).map { it.toModel() }

    suspend fun userentity(connection: ServerConnection, id: Int): Userentity =
        get<UserentityDto>(connection, "objects", USERENTITIES, id.toString()).toModel()

    suspend fun saveUserentity(connection: ServerConnection, userentity: Userentity): Int =
        saveObject(connection, USERENTITIES, userentity.id, userentity.toJson())

    /** All users; needs Grocy's permission to read users. */
    suspend fun users(connection: ServerConnection): List<User> =
        get<List<UserDto>>(connection, "users").map { it.toModel() }

    suspend fun productBarcodes(connection: ServerConnection): List<ProductBarcode> =
        get<List<ProductBarcodeDto>>(connection, "objects", PRODUCT_BARCODES)
            .map { it.toModel() }

    suspend fun productBarcode(connection: ServerConnection, id: Int): ProductBarcode =
        get<ProductBarcodeDto>(connection, "objects", PRODUCT_BARCODES, id.toString()).toModel()

    /** Creates the barcode without id or updates it; returns its id. */
    suspend fun saveProductBarcode(connection: ServerConnection, barcode: ProductBarcode): Int =
        saveObject(connection, PRODUCT_BARCODES, barcode.id, barcode.toJson())

    /** The defined conversions: default ones and the overrides of products. */
    suspend fun quantityUnitConversions(
        connection: ServerConnection,
    ): List<QuantityUnitConversion> =
        get<List<QuantityUnitConversionDto>>(connection, "objects", QU_CONVERSIONS)
            .map { it.toModel() }

    suspend fun quantityUnitConversion(
        connection: ServerConnection,
        id: Int,
    ): QuantityUnitConversion =
        get<QuantityUnitConversionDto>(connection, "objects", QU_CONVERSIONS, id.toString())
            .toModel()

    suspend fun saveQuantityUnitConversion(
        connection: ServerConnection,
        conversion: QuantityUnitConversion,
    ): Int = saveObject(connection, QU_CONVERSIONS, conversion.id, conversion.toJson())

    /** Every conversion Grocy derived, per product and for all products. */
    suspend fun resolvedQuantityUnitConversions(
        connection: ServerConnection,
    ): List<ResolvedQuantityUnitConversion> =
        get<List<ResolvedQuantityUnitConversionDto>>(
            connection,
            "objects",
            "quantity_unit_conversions_resolved",
        ).map { it.toModel() }

    suspend fun shoppingLists(connection: ServerConnection): List<ShoppingList> =
        get<List<ShoppingListDto>>(connection, "objects", "shopping_lists").map { it.toModel() }

    /**
     * Puts [productAmount] (in the stock unit) of the product on shopping list [listId], shown
     * in unit [quId], like Grocy's "Add to shopping list": an item of the product on that list
     * gets the amount added. Returns the id of the product's item, null if it is not found.
     */
    suspend fun addProductToShoppingList(
        connection: ServerConnection,
        productId: Int,
        listId: Int,
        productAmount: Double,
        quId: Int?,
        note: String?,
    ): Int? {
        execute(
            connection,
            HttpMethod.Post,
            listOf("stock", "shoppinglist", "add-product"),
            body = jsonBody(
                buildJsonObject {
                    put("product_id", productId)
                    put("list_id", listId)
                    put("product_amount", productAmount)
                    quId?.let { put("qu_id", it) }
                    note?.takeIf { it.isNotBlank() }?.let { put("note", it) }
                },
            ),
        )
        // Grocy answers without the item, its userfields need its id
        return get<List<ShoppingListItemDto>>(
            connection,
            "objects",
            "shopping_list",
            query = listOf(
                "query[]" to "product_id=$productId",
                "query[]" to "shopping_list_id=$listId",
            ),
        ).firstOrNull()?.id
    }

    /** Every item of every shopping list, with their userfields. */
    suspend fun shoppingListItems(connection: ServerConnection): List<ShoppingListItem> =
        get<List<ShoppingListItemDto>>(connection, "objects", SHOPPING_LIST_ITEMS)
            .map { it.toModel() }

    suspend fun shoppingListItem(connection: ServerConnection, id: Int): ShoppingListItem =
        get<ShoppingListItemDto>(connection, "objects", SHOPPING_LIST_ITEMS, id.toString())
            .toModel()

    /** Creates the item (id [NEW_OBJECT_ID]) or saves it like Grocy's item form; returns its id. */
    suspend fun saveShoppingListItem(connection: ServerConnection, item: ShoppingListItem): Int =
        saveObject(connection, SHOPPING_LIST_ITEMS, item.id, item.toJson())

    /**
     * Checks the item off, or on again; only `done` is sent, so what others changed on the item
     * in the meantime stays. [GrocyApiException.HttpError] once the item is gone.
     */
    suspend fun setShoppingListItemDone(connection: ServerConnection, id: Int, done: Boolean) {
        execute(
            connection,
            HttpMethod.Put,
            listOf("objects", SHOPPING_LIST_ITEMS, id.toString()),
            body = jsonBody(buildJsonObject { put("done", flag(done)) }),
        )
    }

    suspend fun shoppingList(connection: ServerConnection, id: Int): ShoppingList =
        get<ShoppingListDto>(connection, "objects", SHOPPING_LISTS, id.toString()).toModel()

    /** Creates the list (id [NEW_OBJECT_ID]) or renames it; returns its id. */
    suspend fun saveShoppingList(connection: ServerConnection, list: ShoppingList): Int =
        saveObject(connection, SHOPPING_LISTS, list.id, list.toJson())

    /** Saves the list's "Notes" as HTML, which Grocy cleans up itself. */
    suspend fun saveShoppingListDescription(
        connection: ServerConnection,
        listId: Int,
        description: String,
    ) {
        execute(
            connection,
            HttpMethod.Put,
            listOf("objects", SHOPPING_LISTS, listId.toString()),
            body = jsonBody(buildJsonObject { put("description", description) }),
        )
    }

    /** Removes the items of list [listId], only the done ones with [doneOnly]. */
    suspend fun clearShoppingList(connection: ServerConnection, listId: Int, doneOnly: Boolean) {
        execute(
            connection,
            HttpMethod.Post,
            listOf("stock", "shoppinglist", "clear"),
            body = jsonBody(
                buildJsonObject {
                    put("list_id", listId)
                    put("done_only", doneOnly)
                },
            ),
        )
    }

    /**
     * Grocy's "Add products that are below defined min. stock amount": the server raises or
     * adds the items of those products.
     */
    suspend fun addMissingProductsToShoppingList(connection: ServerConnection, listId: Int) {
        shoppingListAction(connection, "add-missing-products", listId)
    }

    /** Adds the products with overdue stock that are on no list yet. */
    suspend fun addOverdueProductsToShoppingList(connection: ServerConnection, listId: Int) {
        shoppingListAction(connection, "add-overdue-products", listId)
    }

    /** Adds the products with expired stock that are on no list yet. */
    suspend fun addExpiredProductsToShoppingList(connection: ServerConnection, listId: Int) {
        shoppingListAction(connection, "add-expired-products", listId)
    }

    private suspend fun shoppingListAction(
        connection: ServerConnection,
        action: String,
        listId: Int,
    ) {
        execute(
            connection,
            HttpMethod.Post,
            listOf("stock", "shoppinglist", action),
            body = jsonBody(buildJsonObject { put("list_id", listId) }),
        )
    }

    /** Prints the list on the server's thermal printer, with Grocy's header if [printHeader]. */
    suspend fun printShoppingList(connection: ServerConnection, listId: Int, printHeader: Boolean) {
        execute(
            connection,
            HttpMethod.Get,
            listOf("print", "shoppinglist", "thermal"),
            query = listOf("list" to listId.toString(), "printHeader" to printHeader.toString()),
        )
    }

    /** Every stock entry of every product, with the userfields of entries. */
    suspend fun stockEntries(connection: ServerConnection): List<StockEntry> =
        get<List<StockEntryDto>>(connection, "objects", STOCK).map { it.toModel() }

    /** The stock entry [id]; [GrocyApiException.HttpError] once it is used up. */
    suspend fun stockEntry(connection: ServerConnection, id: Int): StockEntry =
        get<StockEntryDto>(connection, "objects", STOCK, id.toString()).toModel()

    /** Changes the stock entry like Grocy's "Edit stock entry"; returns its booking's id. */
    suspend fun editStockEntry(connection: ServerConnection, entry: StockEntry): Int? {
        val response = execute(
            connection,
            HttpMethod.Put,
            listOf("stock", "entry", entry.id.toString()),
            body = jsonBody(
                buildJsonObject {
                    put("amount", entry.amount)
                    put("best_before_date", entry.bestBeforeDate)
                    put("purchased_date", entry.purchasedDate)
                    put("price", entry.price)
                    put("open", entry.open)
                    put("location_id", entry.locationId)
                    put("shopping_location_id", entry.storeId)
                    put("note", entry.note.orEmpty())
                },
            ),
        )
        return decode<List<StockLogDto>>(response).firstOrNull()?.id
    }

    /**
     * Consumes exactly the whole stock entry, as spoiled if [spoiled], like Grocy's "Consume
     * this stock entry"; returns the booking's id for undoing it.
     */
    suspend fun consumeStockEntry(
        connection: ServerConnection,
        entry: StockEntry,
        spoiled: Boolean,
    ): Int? {
        val response = execute(
            connection,
            HttpMethod.Post,
            listOf("stock", PRODUCTS, entry.productId.toString(), "consume"),
            body = jsonBody(
                buildJsonObject {
                    put("amount", entry.amount)
                    put("spoiled", spoiled)
                    entry.locationId?.let { put("location_id", it) }
                    put("stock_entry_id", entry.stockId)
                    put("exact_amount", true)
                },
            ),
        )
        return decode<List<StockLogDto>>(response).firstOrNull()?.id
    }

    /** Marks the whole stock entry as opened; returns the booking's id for undoing it. */
    suspend fun openStockEntry(connection: ServerConnection, entry: StockEntry): Int? {
        val response = execute(
            connection,
            HttpMethod.Post,
            listOf("stock", PRODUCTS, entry.productId.toString(), "open"),
            body = jsonBody(
                buildJsonObject {
                    put("amount", entry.amount)
                    put("stock_entry_id", entry.stockId)
                },
            ),
        )
        return decode<List<StockLogDto>>(response).firstOrNull()?.id
    }

    /**
     * Consumes [amount] (in the stock unit) of the product like the buttons of Grocy's stock
     * overview, sub products standing in for a parent; returns the transaction for undoing it.
     */
    suspend fun consumeProduct(
        connection: ServerConnection,
        productId: Int,
        amount: Double,
        spoiled: Boolean,
    ): String? {
        val response = execute(
            connection,
            HttpMethod.Post,
            listOf("stock", PRODUCTS, productId.toString(), "consume"),
            body = jsonBody(
                buildJsonObject {
                    put("amount", amount)
                    put("spoiled", spoiled)
                    put("allow_subproduct_substitution", true)
                },
            ),
        )
        return decode<List<StockLogDto>>(response).firstOrNull()?.transactionId
    }

    /**
     * Marks [amount] (in the stock unit) of the product as opened like the button of Grocy's
     * stock overview; returns the transaction for undoing it.
     */
    suspend fun openProduct(connection: ServerConnection, productId: Int, amount: Double): String? {
        val response = execute(
            connection,
            HttpMethod.Post,
            listOf("stock", PRODUCTS, productId.toString(), "open"),
            body = jsonBody(
                buildJsonObject {
                    put("amount", amount)
                    put("allow_subproduct_substitution", true)
                },
            ),
        )
        return decode<List<StockLogDto>>(response).firstOrNull()?.transactionId
    }

    /** What Grocy's stock pages read of a product: amounts, last purchase, next due date. */
    suspend fun productStockDetails(connection: ServerConnection, productId: Int) =
        get<ProductStockDetailsDto>(connection, "stock", PRODUCTS, productId.toString())
            .toModel(productId)

    /**
     * The locations where the product has stock, with [includeSubProducts] also those of its
     * sub products, like Grocy's consume page.
     */
    suspend fun productLocationAmounts(
        connection: ServerConnection,
        productId: Int,
        includeSubProducts: Boolean,
    ): List<ProductLocationAmount> = get<List<ProductLocationAmountDto>>(
        connection,
        "stock",
        PRODUCTS,
        productId.toString(),
        "locations",
        query = subProductsQuery(includeSubProducts),
    ).map { it.toModel() }

    /** The product's stock entries in Grocy's consume order, see [productLocationAmounts]. */
    suspend fun productStockEntries(
        connection: ServerConnection,
        productId: Int,
        includeSubProducts: Boolean,
    ): List<StockEntry> = get<List<StockEntryDto>>(
        connection,
        "stock",
        PRODUCTS,
        productId.toString(),
        "entries",
        query = subProductsQuery(includeSubProducts),
    ).map { it.toModel() }

    /** The normal recipes (no meal plan entries), id to name, for the consume page. */
    suspend fun recipes(connection: ServerConnection): Map<Int, String> =
        get<List<RecipeDto>>(
            connection,
            "objects",
            "recipes",
            query = listOf("query[]" to "type=normal"),
        ).associate { it.id to it.name.orEmpty() }

    /** Grocy's normal recipes with their userfields, meal plan entries left out. */
    suspend fun recipeObjects(connection: ServerConnection): List<Recipe> =
        get<List<RecipeObjectDto>>(
            connection,
            "objects",
            RECIPES,
            query = listOf("query[]" to "type=normal"),
        ).map { it.toModel() }

    suspend fun recipe(connection: ServerConnection, id: Int): Recipe =
        get<RecipeObjectDto>(connection, "objects", RECIPES, id.toString()).toModel()

    /** Creates the recipe (id [NEW_OBJECT_ID]) or saves it like Grocy's form; returns its id. */
    suspend fun saveRecipe(connection: ServerConnection, recipe: Recipe): Int =
        saveObject(connection, RECIPES, recipe.id, recipe.toJson())

    /** Only the desired servings, like the servings field of Grocy's recipe page. */
    suspend fun saveDesiredServings(connection: ServerConnection, recipeId: Int, servings: Double) {
        execute(
            connection,
            HttpMethod.Put,
            listOf("objects", RECIPES, recipeId.toString()),
            body = jsonBody(buildJsonObject { put("desired_servings", servings) }),
        )
    }

    /** What Grocy computed for every recipe at its desired servings. */
    suspend fun recipesFulfillment(connection: ServerConnection): List<RecipeFulfillment> =
        get<List<RecipeFulfillmentDto>>(connection, RECIPES, "fulfillment").map { it.toModel() }

    suspend fun recipeIngredients(connection: ServerConnection): List<RecipeIngredient> =
        get<List<RecipeIngredientDto>>(connection, "objects", RECIPE_INGREDIENTS)
            .map { it.toModel() }

    suspend fun recipeIngredient(connection: ServerConnection, id: Int): RecipeIngredient =
        get<RecipeIngredientDto>(connection, "objects", RECIPE_INGREDIENTS, id.toString())
            .toModel()

    /** Creates the ingredient (id [NEW_OBJECT_ID]) or saves it; returns its id. */
    suspend fun saveRecipeIngredient(
        connection: ServerConnection,
        ingredient: RecipeIngredient,
    ): Int = saveObject(connection, RECIPE_INGREDIENTS, ingredient.id, ingredient.toJson())

    suspend fun recipeNestings(connection: ServerConnection): List<RecipeNesting> =
        get<List<RecipeNestingDto>>(connection, "objects", RECIPE_NESTINGS).map { it.toModel() }

    suspend fun recipeNesting(connection: ServerConnection, id: Int): RecipeNesting =
        get<RecipeNestingDto>(connection, "objects", RECIPE_NESTINGS, id.toString()).toModel()

    /** Creates the included recipe (id [NEW_OBJECT_ID]) or saves it; returns its id. */
    suspend fun saveRecipeNesting(connection: ServerConnection, nesting: RecipeNesting): Int =
        saveObject(connection, RECIPE_NESTINGS, nesting.id, nesting.toJson())

    /** The ingredients of the normal recipes as Grocy resolved them, included recipes' too. */
    suspend fun resolvedRecipeIngredients(
        connection: ServerConnection,
    ): List<ResolvedRecipeIngredient> =
        get<List<ResolvedRecipeIngredientDto>>(
            connection,
            "objects",
            "recipes_pos_resolved",
            query = listOf("query[]" to "recipe_type=normal"),
        ).map { it.toModel() }

    /** Copies the recipe like Grocy's "Copy" (named "Copy of ...") and returns the copy's id. */
    suspend fun copyRecipe(connection: ServerConnection, id: Int): Int {
        val response = execute(
            connection,
            HttpMethod.Post,
            listOf(RECIPES, id.toString(), "copy"),
            body = jsonBody(JsonObject(emptyMap())),
        )
        return decode<CreatedObjectDto>(response).createdObjectId
            ?: throw GrocyApiException.NotGrocyServer()
    }

    /** Grocy's "Consume all ingredients needed by this recipe". */
    suspend fun consumeRecipe(connection: ServerConnection, id: Int) {
        execute(
            connection,
            HttpMethod.Post,
            listOf(RECIPES, id.toString(), "consume"),
            body = jsonBody(JsonObject(emptyMap())),
        )
    }

    /** Grocy's "Put missing products on shopping list", but those of [excludedProductIds]. */
    suspend fun addMissingRecipeProductsToShoppingList(
        connection: ServerConnection,
        id: Int,
        excludedProductIds: List<Int>,
    ) {
        execute(
            connection,
            HttpMethod.Post,
            listOf(RECIPES, id.toString(), "add-not-fulfilled-products-to-shoppinglist"),
            body = jsonBody(
                buildJsonObject {
                    put("excludedProductIds", JsonArray(excludedProductIds.map(::JsonPrimitive)))
                },
            ),
        )
    }

    /** Prints the recipe's grocycode on the label printer, from the server. */
    suspend fun printRecipeLabel(connection: ServerConnection, id: Int) {
        execute(connection, HttpMethod.Get, listOf(RECIPES, id.toString(), "printlabel"))
    }

    /** The sections of Grocy's meal plan, in their order. */
    suspend fun mealPlanSections(connection: ServerConnection): List<MealPlanSection> =
        get<List<MealPlanSectionDto>>(connection, "objects", "meal_plan_sections")
            .map { it.toModel() }

    /**
     * Grocy's "Add to meal plan": [servings] of the recipe on [day] ("YYYY-MM-DD") in section
     * [sectionId].
     */
    suspend fun addRecipeToMealPlan(
        connection: ServerConnection,
        recipeId: Int,
        day: String,
        servings: Double,
        sectionId: Int,
    ) {
        execute(
            connection,
            HttpMethod.Post,
            listOf("objects", "meal_plan"),
            body = jsonBody(
                buildJsonObject {
                    put("day", day)
                    put("type", "recipe")
                    put("recipe_id", recipeId)
                    put("recipe_servings", servings)
                    put("section_id", sectionId)
                },
            ),
        )
    }

    /** The recipe picture [fileName], scaled by Grocy to at most [maxWidth] pixels. */
    suspend fun recipePicture(
        connection: ServerConnection,
        fileName: String,
        maxWidth: Int,
    ): ByteArray = picture(connection, RECIPE_PICTURES, fileName, maxWidth)

    /** Uploads a recipe picture; Grocy never overwrites a file, so [fileName] must be new. */
    suspend fun uploadRecipePicture(
        connection: ServerConnection,
        fileName: String,
        bytes: ByteArray,
    ) {
        execute(
            connection,
            HttpMethod.Put,
            listOf("files", RECIPE_PICTURES, Base64.encode(fileName.encodeToByteArray())),
            body = ByteArrayContent(bytes, ContentType.Application.OctetStream),
        )
    }

    suspend fun deleteRecipePicture(connection: ServerConnection, fileName: String) {
        execute(
            connection,
            HttpMethod.Delete,
            listOf("files", RECIPE_PICTURES, Base64.encode(fileName.encodeToByteArray())),
        )
    }

    /** Grocy's "Purchase" of the product; returns its transaction for undoing it. */
    suspend fun purchaseProduct(
        connection: ServerConnection,
        productId: Int,
        booking: PurchaseBooking,
    ): BookingResult = bookProduct(connection, productId, "add", booking.toJson())

    /** Grocy's "Consume" page's booking; returns its transaction for undoing it. */
    suspend fun consumeProduct(
        connection: ServerConnection,
        productId: Int,
        booking: ConsumeBooking,
    ): BookingResult = bookProduct(connection, productId, "consume", booking.toJson())

    /** Grocy's "Mark as opened" of the consume page; returns its transaction. */
    suspend fun openProduct(
        connection: ServerConnection,
        productId: Int,
        booking: OpenBooking,
    ): BookingResult = bookProduct(connection, productId, "open", booking.toJson())

    /** Grocy's "Transfer"; returns its transaction for undoing it. */
    suspend fun transferProduct(
        connection: ServerConnection,
        productId: Int,
        booking: TransferBooking,
    ): BookingResult = bookProduct(connection, productId, "transfer", booking.toJson())

    /** Grocy's "Inventory"; returns its transaction for undoing it. */
    suspend fun inventoryProduct(
        connection: ServerConnection,
        productId: Int,
        booking: InventoryBooking,
    ): BookingResult = bookProduct(connection, productId, "inventory", booking.toJson())

    /** Posts a booking of the product; Grocy answers with the stock log rows it wrote. */
    private suspend fun bookProduct(
        connection: ServerConnection,
        productId: Int,
        action: String,
        body: JsonObject,
    ): BookingResult {
        val response = execute(
            connection,
            HttpMethod.Post,
            listOf("stock", PRODUCTS, productId.toString(), action),
            body = jsonBody(body),
        )
        val first = decode<List<StockLogDto>>(response).firstOrNull()
        return BookingResult(first?.transactionId, first?.stockId)
    }

    private fun subProductsQuery(include: Boolean): List<Pair<String, String>> =
        if (include) listOf("include_sub_products" to "true") else emptyList()

    /** Undoes every booking of the transaction [transactionId], like Grocy's "Undo" messages. */
    suspend fun undoTransaction(connection: ServerConnection, transactionId: String) {
        execute(
            connection,
            HttpMethod.Post,
            listOf("stock", "transactions", transactionId, "undo"),
            body = jsonBody(JsonObject(emptyMap())),
        )
    }

    /** Undoes the booking [bookingId] and the ones of the same action, like Grocy's "Undo". */
    suspend fun undoBooking(connection: ServerConnection, bookingId: Int) {
        execute(
            connection,
            HttpMethod.Post,
            listOf("stock", "bookings", bookingId.toString(), "undo"),
            body = jsonBody(JsonObject(emptyMap())),
        )
    }

    /**
     * The stock journal, newest first: bookings since [since] ("YYYY-MM-DD", all without), of
     * [productId] only if given.
     */
    suspend fun stockLog(
        connection: ServerConnection,
        since: String?,
        productId: Int? = null,
    ): List<StockLogEntry> = get<List<StockLogDto>>(
        connection,
        "objects",
        "stock_log",
        query = buildList {
            since?.let { add("query[]" to "row_created_timestamp>=$it") }
            productId?.let { add("query[]" to "product_id=$it") }
            add("order" to "row_created_timestamp:desc")
        },
    ).map { it.toModel() }

    /** Saves one user setting, e.g. a stock setting, as Grocy's settings pages do. */
    suspend fun saveUserSetting(
        connection: ServerConnection,
        key: String,
        value: UserSettingValue,
    ) {
        val json = when (value) {
            is UserSettingValue.Flag -> JsonPrimitive(value.value)
            is UserSettingValue.Whole -> JsonPrimitive(value.value)
            is UserSettingValue.Number -> JsonPrimitive(value.value)
            is UserSettingValue.Text -> JsonPrimitive(value.value)
        }
        execute(
            connection,
            HttpMethod.Put,
            listOf("user", "settings", key),
            body = jsonBody(buildJsonObject { put("value", json) }),
        )
    }

    /** Prints the product's grocycode on the label printer, from the server. */
    suspend fun printProductLabel(connection: ServerConnection, productId: Int) {
        execute(
            connection,
            HttpMethod.Get,
            listOf("stock", PRODUCTS, productId.toString(), "printlabel"),
        )
    }

    /** Prints the stock entry's label on the label printer, from the server. */
    suspend fun printStockEntryLabel(connection: ServerConnection, entryId: Int) {
        execute(
            connection,
            HttpMethod.Get,
            listOf("stock", "entry", entryId.toString(), "printlabel"),
        )
    }

    /** Current stock, one entry per product that is in stock. */
    suspend fun stock(connection: ServerConnection): List<StockAmount> =
        get<List<StockDto>>(connection, "stock").map { it.toModel() }

    /** How much is missing of each product below its min. stock amount, by product id. */
    suspend fun missingProducts(connection: ServerConnection): Map<Int, Double> =
        get<VolatileStockDto>(connection, "stock", "volatile").missingProducts
            .associate { it.id to (it.amountMissing ?: 0.0) }

    /** The last purchase of every product purchased once. */
    suspend fun lastPurchases(connection: ServerConnection): List<ProductPurchase> =
        get<List<LastPurchaseDto>>(connection, "objects", "products_last_purchased")
            .map { it.toModel() }

    /** The average purchase price per stock unit of every product with prices, by product. */
    suspend fun averagePrices(connection: ServerConnection): Map<Int, Double> =
        get<List<AveragePriceDto>>(connection, "objects", "products_average_price")
            .mapNotNull { dto -> dto.price?.let { dto.productId to it } }
            .toMap()

    /** The amount of every product in stock per location. */
    suspend fun stockLocations(connection: ServerConnection): List<StockLocationAmount> =
        get<List<StockLocationDto>>(connection, "objects", "stock_current_locations")
            .map { it.toModel() }

    /** The products on any shopping list. */
    suspend fun productsOnShoppingLists(connection: ServerConnection): Set<Int> =
        get<List<ShoppingListItemDto>>(connection, "objects", "shopping_list")
            .mapNotNull { it.productId }
            .toSet()

    /**
     * Grocy's "Product overview" of an active product (disabled ones are rejected with
     * [GrocyApiException.HttpError]); [withPriceHistory] also downloads its purchase prices.
     */
    suspend fun productOverview(
        connection: ServerConnection,
        productId: Int,
        withPriceHistory: Boolean,
    ): ProductOverview = coroutineScope {
        val id = productId.toString()
        val priceHistory = if (withPriceHistory) {
            async {
                get<List<PriceHistoryDto>>(connection, "stock", PRODUCTS, id, "price-history")
                    .mapNotNull { it.toModel() }
            }
        } else {
            null
        }
        val details = get<ProductDetailsDto>(connection, "stock", PRODUCTS, id)
        details.toModel(productId, priceHistory?.await().orEmpty())
    }

    /** Definitions of the userfields of [entity], e.g. "products". */
    suspend fun userfields(connection: ServerConnection, entity: String): List<Userfield> =
        get<List<UserfieldDto>>(
            connection,
            "objects",
            USERFIELDS,
            query = listOf("query[]" to "entity=$entity"),
        ).map { it.toModel() }

    /** Definitions of the userfields of every entity. */
    suspend fun allUserfields(connection: ServerConnection): List<Userfield> =
        get<List<UserfieldDto>>(connection, "objects", USERFIELDS).map { it.toModel() }

    suspend fun userfield(connection: ServerConnection, id: Int): Userfield =
        get<UserfieldDto>(connection, "objects", USERFIELDS, id.toString()).toModel()

    suspend fun saveUserfield(connection: ServerConnection, userfield: Userfield): Int =
        saveObject(connection, USERFIELDS, userfield.id, userfield.toJson())

    /** Creates an object of [entity] for [id] 0 (returning Grocy's new id) or updates it. */
    private suspend fun saveObject(
        connection: ServerConnection,
        entity: String,
        id: Int,
        json: JsonObject,
    ): Int {
        if (id != NEW_OBJECT_ID) {
            execute(
                connection,
                HttpMethod.Put,
                listOf("objects", entity, id.toString()),
                body = jsonBody(json),
            )
            return id
        }
        val response =
            execute(connection, HttpMethod.Post, listOf("objects", entity), body = jsonBody(json))
        return decode<CreatedObjectDto>(response).createdObjectId
            ?: throw GrocyApiException.NotGrocyServer()
    }

    /** Creates [product] (its id is ignored) and returns the id Grocy gave it. */
    suspend fun addProduct(connection: ServerConnection, product: Product): Int {
        val response = execute(
            connection,
            HttpMethod.Post,
            listOf("objects", PRODUCTS),
            body = jsonBody(product.toJson()),
        )
        return decode<CreatedObjectDto>(response).createdObjectId
            ?: throw GrocyApiException.NotGrocyServer()
    }

    suspend fun editProduct(connection: ServerConnection, product: Product) {
        execute(
            connection,
            HttpMethod.Put,
            listOf("objects", PRODUCTS, product.id.toString()),
            body = jsonBody(product.toJson()),
        )
    }

    /** Deletes the object [id] of [entity] (the name of Grocy's `objects` path). */
    suspend fun deleteObject(connection: ServerConnection, entity: String, id: Int) {
        execute(connection, HttpMethod.Delete, listOf("objects", entity, id.toString()))
    }

    /** Copies the product like Grocy's "Copy" (named "Copy of ...") and returns the copy's id. */
    suspend fun copyProduct(connection: ServerConnection, id: Int): Int {
        val response = execute(
            connection,
            HttpMethod.Post,
            listOf("stock", PRODUCTS, id.toString(), "copy"),
            body = jsonBody(JsonObject(emptyMap())),
        )
        return decode<CreatedObjectDto>(response).createdObjectId
            ?: throw GrocyApiException.NotGrocyServer()
    }

    /** Replaces every use of product [removeId] by [keepId] and deletes [removeId]. */
    suspend fun mergeProducts(connection: ServerConnection, keepId: Int, removeId: Int) {
        merge(connection, listOf("stock", PRODUCTS), keepId, removeId)
    }

    /** Replaces every use of chore [removeId] by [keepId] and deletes [removeId]. */
    suspend fun mergeChores(connection: ServerConnection, keepId: Int, removeId: Int) {
        merge(connection, listOf(CHORES), keepId, removeId)
    }

    private suspend fun merge(
        connection: ServerConnection,
        path: List<String>,
        keepId: Int,
        removeId: Int,
    ) {
        execute(
            connection,
            HttpMethod.Post,
            path + listOf(keepId.toString(), "merge", removeId.toString()),
            body = jsonBody(JsonObject(emptyMap())),
        )
    }

    /** Saves the userfield [values] by field name of one object of [entity]. */
    suspend fun setUserfields(
        connection: ServerConnection,
        entity: String,
        objectId: Int,
        values: Map<String, String?>,
    ) {
        setUserfields(connection, entity, objectId.toString(), values)
    }

    /** Like the other [setUserfields] for objects with text ids, e.g. a stock entry's stock id. */
    suspend fun setUserfields(
        connection: ServerConnection,
        entity: String,
        objectId: String,
        values: Map<String, String?>,
    ) {
        execute(
            connection,
            HttpMethod.Put,
            listOf("userfields", entity, objectId),
            body = jsonBody(JsonObject(values.mapValues { JsonPrimitive(it.value) })),
        )
    }

    /** The product picture [fileName], scaled by Grocy to at most [maxWidth] pixels. */
    suspend fun productPicture(
        connection: ServerConnection,
        fileName: String,
        maxWidth: Int,
    ): ByteArray = picture(connection, PRODUCT_PICTURES, fileName, maxWidth)

    private suspend fun picture(
        connection: ServerConnection,
        group: String,
        fileName: String,
        maxWidth: Int,
    ): ByteArray {
        val response = execute(
            connection,
            HttpMethod.Get,
            listOf("files", group, Base64.encode(fileName.encodeToByteArray())),
            query = listOf("force_serve_as" to "picture", "best_fit_width" to maxWidth.toString()),
        )
        return response.bodyAsBytes()
    }

    /** Uploads a product picture; Grocy never overwrites a file, so [fileName] must be new. */
    suspend fun uploadProductPicture(
        connection: ServerConnection,
        fileName: String,
        bytes: ByteArray,
    ) {
        execute(
            connection,
            HttpMethod.Put,
            listOf("files", PRODUCT_PICTURES, Base64.encode(fileName.encodeToByteArray())),
            body = ByteArrayContent(bytes, ContentType.Application.OctetStream),
        )
    }

    suspend fun deleteProductPicture(connection: ServerConnection, fileName: String) {
        execute(
            connection,
            HttpMethod.Delete,
            listOf("files", PRODUCT_PICTURES, Base64.encode(fileName.encodeToByteArray())),
        )
    }

    private suspend inline fun <reified T> get(
        connection: ServerConnection,
        vararg path: String,
        query: List<Pair<String, String>> = emptyList(),
    ): T = decode(execute(connection, HttpMethod.Get, path.toList(), query))

    private suspend inline fun <reified T> decode(response: HttpResponse): T =
        try {
            response.body<T>()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw GrocyApiException.NotGrocyServer(e)
        }

    /** Sends a request and maps failures to [GrocyApiException]s. */
    private suspend fun execute(
        connection: ServerConnection,
        method: HttpMethod,
        path: List<String>,
        query: List<Pair<String, String>> = emptyList(),
        body: OutgoingContent? = null,
    ): HttpResponse {
        var response = send(connection, method, path, query, body)
        if (response.status == HttpStatusCode.Unauthorized &&
            connection is ServerConnection.HomeAssistant
        ) {
            // Also the answer to an expired ingress session, so one fresh session decides
            ingress.invalidate(connection)
            response = send(connection, method, path, query, body)
        }
        when {
            response.status == HttpStatusCode.Unauthorized -> throw GrocyApiException.Unauthorized()
            !response.status.isSuccess() -> throw GrocyApiException.HttpError(
                statusCode = response.status.value,
                serverMessage = errorMessageOf(response),
            )
        }
        if (connection is ServerConnection.HomeAssistant) {
            ingress.markUsed(connection)
        }
        return response
    }

    /** Grocy explains rejected requests in `error_message`, e.g. a name that exists already. */
    private suspend fun errorMessageOf(response: HttpResponse): String? =
        try {
            GrocyJson.decodeFromString<ErrorDto>(response.bodyAsText()).errorMessage
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            null
        }

    private suspend fun send(
        connection: ServerConnection,
        method: HttpMethod,
        path: List<String>,
        query: List<Pair<String, String>>,
        body: OutgoingContent?,
    ): HttpResponse {
        val ingressSession = (connection as? ServerConnection.HomeAssistant)
            ?.let { ingress.session(it) }
        val baseUrl = connection.serverUrl + ingressSession?.ingressPath.orEmpty()
        val apiKey = connection.apiKey
        return request {
            httpClient.request(baseUrl) {
                this.method = method
                url {
                    appendPathSegments("api", *path.toTypedArray())
                    query.forEach { (name, value) -> parameters.append(name, value) }
                    if (apiKey != null && apiKeyAsQueryParameter) {
                        parameters.append(API_KEY_HEADER, apiKey)
                    }
                }
                if (apiKey != null && !apiKeyAsQueryParameter) {
                    header(API_KEY_HEADER, apiKey)
                }
                if (ingressSession != null) {
                    header(HttpHeaders.Cookie, "$INGRESS_COOKIE=${ingressSession.sessionId}")
                }
                if (body != null) {
                    setBody(body)
                }
            }
        }
    }

    private suspend inline fun request(block: () -> HttpResponse): HttpResponse =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // Throwable, not Exception: on Wasm a failed fetch arrives as a Kotlin Error
            throw GrocyApiException.Unreachable(e)
        }

    companion object {
        /** Header name, and query parameter name where the header cannot be used. */
        const val API_KEY_HEADER = "GROCY-API-KEY"

        /** Id of objects that do not exist on the server yet. */
        const val NEW_OBJECT_ID = 0

        private const val PRODUCTS = "products"
        private const val PRODUCT_GROUPS = "product_groups"
        private const val LOCATIONS = "locations"
        private const val QUANTITY_UNITS = "quantity_units"
        private const val STORES = "shopping_locations"
        private const val CHORES = "chores"
        private const val BATTERIES = "batteries"
        private const val TASK_CATEGORIES = "task_categories"
        private const val USERENTITIES = "userentities"
        private const val USERFIELDS = "userfields"
        private const val PRODUCT_PICTURES = "productpictures"
        private const val RECIPES = "recipes"
        private const val RECIPE_INGREDIENTS = "recipes_pos"
        private const val RECIPE_NESTINGS = "recipes_nestings"
        private const val RECIPE_PICTURES = "recipepictures"
        private const val PRODUCT_BARCODES = "product_barcodes"
        private const val QU_CONVERSIONS = "quantity_unit_conversions"
        private const val STOCK = "stock"
        private const val SHOPPING_LIST_ITEMS = "shopping_list"
        private const val SHOPPING_LISTS = "shopping_lists"

        private const val INGRESS_COOKIE = "ingress_session"
    }
}

/**
 * JSON with exactly `application/json` as content type: Grocy compares the header as string and
 * rejects it with a charset parameter.
 */
private fun jsonBody(json: JsonObject): OutgoingContent =
    TextContent(json.toString(), ContentType.Application.Json)

private val ServerConnection.apiKey: String?
    get() = when (this) {
        is ServerConnection.ApiKey -> apiKey
        is ServerConnection.HomeAssistant -> apiKey
        is ServerConnection.Demo -> null
    }
