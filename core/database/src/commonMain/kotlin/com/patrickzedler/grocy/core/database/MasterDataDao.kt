package com.patrickzedler.grocy.core.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

/**
 * Grocy's master data. Lists are replaced as a whole after every download, inside a write
 * transaction of [GrocyDatabase], so readers never see half of an update.
 */
@Dao
interface MasterDataDao {

    @Query("SELECT * FROM products ORDER BY name COLLATE NOCASE")
    fun observeProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    fun observeProduct(id: Int): Flow<ProductEntity?>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun product(id: Int): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProduct(id: Int)

    @Query("SELECT * FROM product_overviews WHERE product_id = :productId")
    fun observeProductOverview(productId: Int): Flow<ProductOverviewEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProductOverview(overview: ProductOverviewEntity)

    @Query("DELETE FROM product_overviews WHERE product_id = :productId")
    suspend fun deleteProductOverview(productId: Int)

    /** Removes overviews of products the server no longer has. */
    @Query("DELETE FROM product_overviews WHERE product_id NOT IN (SELECT id FROM products)")
    suspend fun deleteUnusedProductOverviews()

    @Query("SELECT * FROM product_groups ORDER BY name COLLATE NOCASE")
    fun observeProductGroups(): Flow<List<ProductGroupEntity>>

    @Query("SELECT * FROM locations ORDER BY name COLLATE NOCASE")
    fun observeLocations(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM quantity_units ORDER BY name COLLATE NOCASE")
    fun observeQuantityUnits(): Flow<List<QuantityUnitEntity>>

    @Query("SELECT * FROM stores ORDER BY name COLLATE NOCASE")
    fun observeStores(): Flow<List<StoreEntity>>

    @Query("SELECT * FROM chores ORDER BY name COLLATE NOCASE")
    fun observeChores(): Flow<List<ChoreEntity>>

    @Query("SELECT * FROM batteries ORDER BY name COLLATE NOCASE")
    fun observeBatteries(): Flow<List<BatteryEntity>>

    @Query("SELECT * FROM task_categories ORDER BY name COLLATE NOCASE")
    fun observeTaskCategories(): Flow<List<TaskCategoryEntity>>

    @Query("SELECT * FROM userentities ORDER BY name COLLATE NOCASE")
    fun observeUserentities(): Flow<List<UserentityEntity>>

    @Query("SELECT * FROM users ORDER BY display_name COLLATE NOCASE")
    fun observeUsers(): Flow<List<UserEntity>>

    @Query(
        "SELECT * FROM userfields " +
            "ORDER BY entity, sort_number IS NULL, sort_number, name COLLATE NOCASE",
    )
    fun observeAllUserfields(): Flow<List<UserfieldEntity>>

    @Query("SELECT * FROM product_barcodes ORDER BY barcode")
    fun observeProductBarcodes(): Flow<List<ProductBarcodeEntity>>

    @Query("SELECT * FROM stock")
    fun observeStock(): Flow<List<StockEntity>>

    @Query("SELECT * FROM quantity_unit_conversions")
    fun observeQuantityUnitConversions(): Flow<List<QuantityUnitConversionEntity>>

    @Query("SELECT * FROM quantity_unit_conversions_resolved")
    fun observeResolvedQuantityUnitConversions(): Flow<List<ResolvedQuantityUnitConversionEntity>>

    @Query("SELECT * FROM shopping_lists ORDER BY name COLLATE NOCASE")
    fun observeShoppingLists(): Flow<List<ShoppingListEntity>>

    @Query(
        "SELECT * FROM userfields WHERE entity = :entity " +
            "ORDER BY sort_number IS NULL, sort_number, name COLLATE NOCASE",
    )
    fun observeUserfields(entity: String): Flow<List<UserfieldEntity>>

    @Query("SELECT * FROM server_values WHERE `key` = :key")
    fun observeServerValue(key: String): Flow<ServerValueEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertServerValue(value: ServerValueEntity)

    @Query("DELETE FROM products")
    suspend fun deleteProducts()

    @Insert
    suspend fun insertProducts(products: List<ProductEntity>)

    @Query("DELETE FROM product_groups")
    suspend fun deleteProductGroups()

    @Insert
    suspend fun insertProductGroups(groups: List<ProductGroupEntity>)

    @Query("DELETE FROM locations")
    suspend fun deleteLocations()

    @Insert
    suspend fun insertLocations(locations: List<LocationEntity>)

    @Query("DELETE FROM quantity_units")
    suspend fun deleteQuantityUnits()

    @Insert
    suspend fun insertQuantityUnits(units: List<QuantityUnitEntity>)

    @Query("DELETE FROM stores")
    suspend fun deleteStores()

    @Insert
    suspend fun insertStores(stores: List<StoreEntity>)

    @Query("DELETE FROM product_barcodes")
    suspend fun deleteProductBarcodes()

    @Insert
    suspend fun insertProductBarcodes(barcodes: List<ProductBarcodeEntity>)

    @Query("DELETE FROM stock")
    suspend fun deleteStock()

    @Insert
    suspend fun insertStock(stock: List<StockEntity>)

    @Query("DELETE FROM userfields")
    suspend fun deleteUserfields()

    @Insert
    suspend fun insertUserfields(userfields: List<UserfieldEntity>)

    @Query("DELETE FROM chores")
    suspend fun deleteChores()

    @Insert
    suspend fun insertChores(chores: List<ChoreEntity>)

    @Query("DELETE FROM batteries")
    suspend fun deleteBatteries()

    @Insert
    suspend fun insertBatteries(batteries: List<BatteryEntity>)

    @Query("DELETE FROM task_categories")
    suspend fun deleteTaskCategories()

    @Insert
    suspend fun insertTaskCategories(categories: List<TaskCategoryEntity>)

    @Query("DELETE FROM userentities")
    suspend fun deleteUserentities()

    @Insert
    suspend fun insertUserentities(userentities: List<UserentityEntity>)

    @Query("DELETE FROM users")
    suspend fun deleteUsers()

    @Insert
    suspend fun insertUsers(users: List<UserEntity>)

    @Query("DELETE FROM quantity_unit_conversions")
    suspend fun deleteQuantityUnitConversions()

    @Insert
    suspend fun insertQuantityUnitConversions(conversions: List<QuantityUnitConversionEntity>)

    @Query("DELETE FROM quantity_unit_conversions_resolved")
    suspend fun deleteResolvedQuantityUnitConversions()

    @Insert
    suspend fun insertResolvedQuantityUnitConversions(
        conversions: List<ResolvedQuantityUnitConversionEntity>,
    )

    @Query("DELETE FROM shopping_lists")
    suspend fun deleteShoppingLists()

    @Insert
    suspend fun insertShoppingLists(lists: List<ShoppingListEntity>)

    // Single objects as the server saved them after a change

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProductGroup(group: ProductGroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocation(location: LocationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuantityUnit(unit: QuantityUnitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStore(store: StoreEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChore(chore: ChoreEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBattery(battery: BatteryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTaskCategory(category: TaskCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserentity(userentity: UserentityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserfield(userfield: UserfieldEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProductBarcode(barcode: ProductBarcodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuantityUnitConversion(conversion: QuantityUnitConversionEntity)

    // Single objects deleted on the server

    @Query("DELETE FROM product_groups WHERE id = :id")
    suspend fun deleteProductGroup(id: Int)

    @Query("DELETE FROM locations WHERE id = :id")
    suspend fun deleteLocation(id: Int)

    @Query("DELETE FROM quantity_units WHERE id = :id")
    suspend fun deleteQuantityUnit(id: Int)

    @Query("DELETE FROM stores WHERE id = :id")
    suspend fun deleteStore(id: Int)

    @Query("DELETE FROM chores WHERE id = :id")
    suspend fun deleteChore(id: Int)

    @Query("DELETE FROM batteries WHERE id = :id")
    suspend fun deleteBattery(id: Int)

    @Query("DELETE FROM task_categories WHERE id = :id")
    suspend fun deleteTaskCategory(id: Int)

    @Query("DELETE FROM userentities WHERE id = :id")
    suspend fun deleteUserentity(id: Int)

    @Query("DELETE FROM userfields WHERE id = :id")
    suspend fun deleteUserfield(id: Int)

    @Query("DELETE FROM product_barcodes WHERE id = :id")
    suspend fun deleteProductBarcode(id: Int)

    @Query("DELETE FROM quantity_unit_conversions WHERE id = :id")
    suspend fun deleteQuantityUnitConversion(id: Int)

    @Query("SELECT bytes FROM product_thumbnails WHERE file_name = :fileName")
    suspend fun thumbnail(fileName: String): ByteArray?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertThumbnail(thumbnail: ProductThumbnailEntity)

    /** File names of product pictures that have no stored thumbnail yet. */
    @Query(
        "SELECT DISTINCT picture_file_name FROM products WHERE picture_file_name != '' AND " +
            "picture_file_name NOT IN (SELECT file_name FROM product_thumbnails)",
    )
    suspend fun missingThumbnails(): List<String>

    /** Removes thumbnails of pictures no product shows anymore. */
    @Query(
        "DELETE FROM product_thumbnails WHERE file_name NOT IN " +
            "(SELECT picture_file_name FROM products WHERE picture_file_name IS NOT NULL)",
    )
    suspend fun deleteUnusedThumbnails()
}
