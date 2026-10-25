package com.patrickzedler.grocy.feature.masterdata.impl.journal

import com.patrickzedler.grocy.core.model.User
import com.patrickzedler.grocy.core.model.masterdata.Location
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.stock.StockLogEntry
import com.patrickzedler.grocy.core.model.stock.StockTransactionType
import com.patrickzedler.grocy.core.model.text.epochDayOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StockJournalTest {

    private val names = JournalNames(
        products = listOf(
            Product(1, "Milk", quIdStock = 1),
            Product(2, "Bread", quIdStock = 1),
        ),
        units = listOf(QuantityUnit(1, "Piece", "Pieces")),
        locations = listOf(Location(1, "Fridge"), Location(2, "Pantry")),
        users = listOf(User(1, "alice", "Alice"), User(2, "bob", "Bob")),
    )

    private fun booking(
        id: Int,
        productId: Int = 1,
        amount: Double = 1.0,
        type: StockTransactionType = StockTransactionType.PURCHASE,
        time: String = "2026-10-0$id 10:00:00",
        userId: Int = 1,
        locationId: Int = 1,
        undone: Boolean = false,
        note: String? = null,
    ) = StockLogEntry(
        id = id,
        productId = productId,
        amount = amount,
        transactionType = type.value,
        createdTimestamp = time,
        undone = undone,
        locationId = locationId,
        userId = userId,
        note = note,
    )

    private fun items(
        bookings: List<StockLogEntry>,
        filter: JournalFilter = JournalFilter(),
        query: String = "",
    ) = buildJournalItems(bookings, names, filter, query, 2, '.', "en")

    @Test
    fun rangeStartsTheMonthsBeforeTodayLikeGrocy() {
        val today = epochDayOf("2026-10-25")!!

        assertEquals("2026-04-25", journalSince(today, 6))
        assertEquals("2025-10-25", journalSince(today, 12))
        assertNull(journalSince(today, JOURNAL_ALL_MONTHS))
    }

    @Test
    fun dayBeyondTheMonthsEndMovesIntoTheNextMonthLikeSqlite() {
        assertEquals("2026-03-03", journalSince(epochDayOf("2026-03-31")!!, 1))
    }

    @Test
    fun bookingsAreNewestFirstWithTheirNames() {
        val items = items(listOf(booking(1), booking(3), booking(2, amount = -1.0)))

        assertEquals(listOf(3, 2, 1), items.map { it.booking.id })
        assertEquals("-1 Piece", items[1].amount)
        assertEquals("Alice", items[0].userName)
        assertEquals("Fridge", items[0].locationName)
        assertEquals(StockTransactionType.PURCHASE, items[0].type)
    }

    @Test
    fun filtersKeepTheirBookings() {
        val bookings = listOf(
            booking(1),
            booking(2, productId = 2),
            booking(3, type = StockTransactionType.CONSUME),
            booking(4, userId = 2),
            booking(5, locationId = 2),
        )

        assertEquals(listOf(2), items(bookings, JournalFilter(productId = 2)).map { it.booking.id })
        assertEquals(
            listOf(3),
            items(bookings, JournalFilter(transactionType = StockTransactionType.CONSUME))
                .map { it.booking.id },
        )
        assertEquals(listOf(4), items(bookings, JournalFilter(userId = 2)).map { it.booking.id })
        assertEquals(
            listOf(5),
            items(bookings, JournalFilter(locationId = 2)).map { it.booking.id },
        )
    }

    @Test
    fun searchFindsProductsUsersLocationsAndNotes() {
        val bookings = listOf(
            booking(1),
            booking(2, productId = 2),
            booking(3, userId = 2),
            booking(4, note = "Party"),
        )

        assertEquals(listOf(2), items(bookings, query = "brea").map { it.booking.id })
        assertEquals(listOf(3), items(bookings, query = "BOB").map { it.booking.id })
        assertEquals(listOf(4), items(bookings, query = "party").map { it.booking.id })
    }

    @Test
    fun filterCountsALongerRangeAsFilter() {
        assertEquals(false, JournalFilter().isActive)
        assertEquals(true, JournalFilter(months = 12).isActive)
    }

    @Test
    fun summarySumsWhatWasNotUndonePerProductTypeAndUser() {
        val bookings = listOf(
            booking(1, amount = 3.0),
            booking(2, amount = 2.0),
            booking(3, amount = 5.0, undone = true),
            booking(4, amount = -1.0, type = StockTransactionType.CONSUME),
            booking(5, amount = 1.0, userId = 2),
        )

        val summary = buildJournalSummary(
            bookings, names, JournalFilter(), "", 2, '.', "en",
        )

        assertEquals(
            listOf(
                Triple(StockTransactionType.CONSUME, "Alice", "-1 Piece"),
                Triple(StockTransactionType.PURCHASE, "Alice", "5 Pieces"),
                Triple(StockTransactionType.PURCHASE, "Bob", "1 Piece"),
            ),
            summary.map { Triple(it.type, it.userName, it.amount) },
        )
    }
}
