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
    ],
    version = 3,
    autoMigrations = [
        // 2 adds product_thumbnails
        AutoMigration(from = 1, to = 2),
        // 3 adds product_overviews
        AutoMigration(from = 2, to = 3),
    ],
)
@ConstructedBy(GrocyDatabaseConstructor::class)
abstract class GrocyDatabase : RoomDatabase() {
    abstract fun masterDataDao(): MasterDataDao
}

// Room generates the actual objects for every platform
@Suppress("KotlinNoActualForExpect")
expect object GrocyDatabaseConstructor : RoomDatabaseConstructor<GrocyDatabase> {
    override fun initialize(): GrocyDatabase
}

internal const val DATABASE_NAME = "grocy.db"
