package com.patrickzedler.grocy.core.database

import com.patrickzedler.grocy.core.model.masterdata.ProductBarcode
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnitConversion
import com.patrickzedler.grocy.core.model.stock.StockEntry
import com.patrickzedler.grocy.core.model.stock.StockLogEntry
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class StockDaoTest {

    private val database = createInMemoryGrocyDatabase()
    private val dao = database.stockDao()

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun stockEntryKeepsEveryPropertyThroughTheDatabase() = runTest {
        val entry = StockEntry(
            id = 3,
            productId = 1,
            amount = 1.5,
            bestBeforeDate = StockEntry.NEVER_OVERDUE,
            purchasedDate = "2026-10-01",
            stockId = "6f1a2b",
            price = 0.99,
            open = true,
            openedDate = "2026-10-02",
            locationId = 2,
            storeId = 1,
            note = "Half",
            createdTimestamp = "2026-10-01 10:00:00",
            userfields = mapOf("batch" to "7"),
        )

        dao.insertStockEntries(listOf(entry.toEntity()))

        assertEquals(entry, dao.observeStockEntries().first().single().toModel())
    }

    @Test
    fun journalShowsTheBookingsOfItsTimeNewestFirst() = runTest {
        dao.insertStockLog(
            listOf(
                booking(1, "2026-03-01 10:00:00"),
                booking(2, "2026-10-01 10:00:00"),
                booking(3, "2026-10-21 08:00:00"),
            ).map { it.toEntity() },
        )

        val ids = dao.observeStockLog(since = "2026-04-25").first().map { it.id }

        assertEquals(listOf(3, 2), ids)
        assertEquals(3, dao.observeStockLog(since = "").first().size)
    }

    @Test
    fun downloadOfATimeReplacesOnlyItsBookings() = runTest {
        dao.insertStockLog(
            listOf(booking(1, "2026-03-01 10:00:00"), booking(2, "2026-10-01 10:00:00"))
                .map { it.toEntity() },
        )

        dao.deleteStockLog(since = "2026-04-25")
        dao.insertStockLog(listOf(booking(4, "2026-10-22 10:00:00").toEntity()))

        assertEquals(listOf(4, 1), dao.observeStockLog(since = "").first().map { it.id })
    }

    @Test
    fun barcodesAndConversionsKeepTheirUserfields() = runTest {
        val masterData = database.masterDataDao()
        val barcode = ProductBarcode(1, 2, "123", userfields = mapOf("size" to "XL"))
        val conversion = QuantityUnitConversion(1, 2, 1, 6.0, null, mapOf("source" to "Box"))

        masterData.upsertProductBarcode(barcode.toEntity())
        masterData.upsertQuantityUnitConversion(conversion.toEntity())

        assertEquals(barcode, masterData.observeProductBarcodes().first().single().toModel())
        val stored = masterData.observeQuantityUnitConversions().first().single().toModel()
        assertEquals(conversion, stored)
    }

    private fun booking(id: Int, timestamp: String) = StockLogEntry(
        id = id,
        productId = 1,
        amount = 1.0,
        transactionType = "purchase",
        createdTimestamp = timestamp,
    )
}
