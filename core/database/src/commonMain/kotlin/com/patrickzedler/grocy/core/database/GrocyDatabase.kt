package com.patrickzedler.grocy.core.database

import androidx.room3.AutoMigration
import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

/**
 * The local copy of the Grocy data of the logged in server; screens read from here, downloads
 * write here. Every schema change needs a new version with a migration (schemas/ keeps the
 * exported versions).
 */
@Database(
    entities = [
        ProductEntity::class,
        ProductGroupEntity::class,
        LocationEntity::class,
        QuantityUnitEntity::class,
        StoreEntity::class,
        ProductBarcodeEntity::class,
        StockEntity::class,
        UserfieldEntity::class,
        ServerValueEntity::class,
        ProductThumbnailEntity::class,
        ProductOverviewEntity::class,
        ChoreEntity::class,
        BatteryEntity::class,
        TaskCategoryEntity::class,
        UserentityEntity::class,
        UserEntity::class,
        QuantityUnitConversionEntity::class,
        ResolvedQuantityUnitConversionEntity::class,
        StockEntryEntity::class,
        StockLogEntity::class,
        ShoppingListEntity::class,
        ProductStockInfoEntity::class,
        StockLocationEntity::class,
        ShoppingListItemEntity::class,
        PendingShoppingListDoneEntity::class,
        PendingShoppingListNotesEntity::class,
    ],
    version = 8,
    autoMigrations = [
        // 2 adds product_thumbnails
        AutoMigration(from = 1, to = 2),
        // 3 adds product_overviews
        AutoMigration(from = 2, to = 3),
        // 4 adds the other master data and userfield values of locations, stores, units, groups
        AutoMigration(from = 3, to = 4),
        // 5 adds quantity unit conversions, stock entries, the stock journal, shopping lists
        // and the userfield values of barcodes
        AutoMigration(from = 4, to = 5),
        // 6 adds the stock overview: opened amounts, value and next due date of the stock,
        // product_stock_info and stock_locations
        AutoMigration(from = 5, to = 6),
        // 7 adds the shopping list items, checked off items not sent yet and the notes and
        // userfield values of shopping lists
        AutoMigration(from = 6, to = 7),
        // 8 adds the notes of shopping lists edited offline and not sent yet
        AutoMigration(from = 7, to = 8),
    ],
)
@ConstructedBy(GrocyDatabaseConstructor::class)
abstract class GrocyDatabase : RoomDatabase() {
    abstract fun masterDataDao(): MasterDataDao

    abstract fun stockDao(): StockDao

    abstract fun shoppingListDao(): ShoppingListDao
}

// Room generates the actual objects for every platform
@Suppress("KotlinNoActualForExpect")
expect object GrocyDatabaseConstructor : RoomDatabaseConstructor<GrocyDatabase> {
    override fun initialize(): GrocyDatabase
}

internal const val DATABASE_NAME = "grocy.db"
