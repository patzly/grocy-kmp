package com.patrickzedler.grocy.core.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

/**
 * What the server only knows about stock: its entries, the journal of bookings and what the
 * stock overview shows, stored as last downloaded, so their screens open offline too.
 */
@Dao
interface StockDao {

    @Query("SELECT * FROM stock_entries ORDER BY product_id, id")
    fun observeStockEntries(): Flow<List<StockEntryEntity>>

    @Query("DELETE FROM stock_entries")
    suspend fun deleteStockEntries()

    @Insert
    suspend fun insertStockEntries(entries: List<StockEntryEntity>)

    /** Bookings since [since] ("YYYY-MM-DD"), newest first. */
    @Query(
        "SELECT * FROM stock_log WHERE created_timestamp >= :since " +
            "ORDER BY created_timestamp DESC, id DESC",
    )
    fun observeStockLog(since: String): Flow<List<StockLogEntity>>

    /** The bookings of [since] and later, which a download of that range replaces. */
    @Query("DELETE FROM stock_log WHERE created_timestamp >= :since")
    suspend fun deleteStockLog(since: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockLog(entries: List<StockLogEntity>)

    @Query("SELECT * FROM product_stock_info")
    fun observeProductStockInfo(): Flow<List<ProductStockInfoEntity>>

    @Query("DELETE FROM product_stock_info")
    suspend fun deleteProductStockInfo()

    @Insert
    suspend fun insertProductStockInfo(info: List<ProductStockInfoEntity>)

    @Query("SELECT * FROM stock_locations")
    fun observeStockLocations(): Flow<List<StockLocationEntity>>

    @Query("DELETE FROM stock_locations")
    suspend fun deleteStockLocations()

    @Insert
    suspend fun insertStockLocations(locations: List<StockLocationEntity>)
}
