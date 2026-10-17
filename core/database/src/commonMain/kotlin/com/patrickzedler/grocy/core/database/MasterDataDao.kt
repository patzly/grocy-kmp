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

    @Query("SELECT * FROM product_groups ORDER BY name COLLATE NOCASE")
    fun observeProductGroups(): Flow<List<ProductGroupEntity>>

    @Query("SELECT * FROM locations ORDER BY name COLLATE NOCASE")
    fun observeLocations(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM quantity_units ORDER BY name COLLATE NOCASE")
    fun observeQuantityUnits(): Flow<List<QuantityUnitEntity>>

    @Query("SELECT * FROM stores ORDER BY name COLLATE NOCASE")
    fun observeStores(): Flow<List<StoreEntity>>

    @Query("SELECT * FROM product_barcodes ORDER BY barcode")
    fun observeProductBarcodes(): Flow<List<ProductBarcodeEntity>>

    @Query("SELECT * FROM stock")
    fun observeStock(): Flow<List<StockEntity>>

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

    @Query("DELETE FROM userfields WHERE entity = :entity")
    suspend fun deleteUserfields(entity: String)

    @Insert
    suspend fun insertUserfields(userfields: List<UserfieldEntity>)
}
