package com.patrickzedler.grocy.feature.masterdata.impl.booking

import com.patrickzedler.grocy.core.model.masterdata.Location
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.ProductBarcode
import com.patrickzedler.grocy.core.model.masterdata.StockAmount
import com.patrickzedler.grocy.core.model.stock.ProductLocationAmount
import com.patrickzedler.grocy.core.model.stock.ProductStockDetails
import com.patrickzedler.grocy.core.model.stock.StockEntry
import com.patrickzedler.grocy.core.model.text.epochDayOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BookingLogicTest {

    private val today = epochDayOf("2026-10-30")!!
    private val milk = Product(id = 1, name = "Milk", locationId = 2, defaultDueDays = 7)
    private val parent = Product(id = 2, name = "Chocolate", noOwnStock = true)
    private val bread = Product(id = 3, name = "Bread")
    private val old = Product(id = 4, name = "Old", active = false)
    private val products = listOf(milk, parent, bread, old)
    private val stock = listOf(StockAmount(1, 2.0, 2.0), StockAmount(2, 0.0, 3.0))

    @Test
    fun purchaseAndInventoryOfferActiveProductsWithOwnStock() {
        assertEquals(
            listOf(bread, milk),
            bookableProducts(BookingKind.PURCHASE, products, stock),
        )
    }

    @Test
    fun consumeOffersProductsInStockSubProductsIncluded() {
        assertEquals(
            listOf(parent, milk),
            bookableProducts(BookingKind.CONSUME, products, stock),
        )
    }

    @Test
    fun transferOffersProductsWithOwnStockInStock() {
        assertEquals(listOf(milk), bookableProducts(BookingKind.TRANSFER, products, stock))
    }

    @Test
    fun barcodesAndGrocycodesFindTheirProduct() {
        val barcode = ProductBarcode(id = 9, productId = 1, barcode = "400123")
        val offered = listOf(milk, bread)

        assertEquals(
            CodeMatch.Found(milk, barcode),
            matchCode(" 400123 ", offered, products, listOf(barcode)),
        )
        assertEquals(
            CodeMatch.Found(bread, stockId = "abc"),
            matchCode("grcy:p:3:abc", offered, products, emptyList()),
        )
    }

    @Test
    fun knownProductsThePageDoesNotOfferAreNotOffered() {
        val offered = listOf(milk)

        assertEquals(CodeMatch.NotOffered, matchCode("grcy:p:4", offered, products, emptyList()))
        assertEquals(CodeMatch.NotOffered, matchCode("bread", offered, products, emptyList()))
    }

    @Test
    fun unknownCodesStartGrocysWorkflow() {
        assertEquals(CodeMatch.Unknown, matchCode("123", products, products, emptyList()))
        assertEquals(CodeMatch.Unknown, matchCode("grcy:p:99", products, products, emptyList()))
    }

    @Test
    fun unitPricesAreConvertedToTheStockUnit() {
        // A pack of 6 pieces for 3.00: 0.5 per piece, the factor is 1/6 packs per piece
        assertEquals(0.5, pricePerStockUnit(3.0, 1.0 / 6, false, 2.0), 1e-9)
    }

    @Test
    fun totalPricesAreSharedByTheAmount() {
        // 2 packs for 6.00 together
        assertEquals(0.5, pricePerStockUnit(6.0, 1.0 / 6, true, 2.0), 1e-9)
    }

    @Test
    fun totalPricesOfWeighedContainersLeaveOutTheContainer() {
        assertEquals(0.01, pricePerStockUnit(5.0, 1.0, true, 600.0, tareWeight = 100.0), 1e-9)
    }

    @Test
    fun dueDatesFollowTheProductsDefaultDays() {
        assertEquals("2026-11-06", dueDateAfter(7, today))
        assertEquals(NEVER_OVERDUE_DATE, dueDateAfter(-1, today))
        assertNull(dueDateAfter(0, today))
    }

    @Test
    fun purchaseDueDateFillsOnlyAnEmptyField() {
        assertEquals("2026-11-06", purchaseDueDate("", milk, null, freezing = true, today))
        assertEquals("2026-12-24", purchaseDueDate("2026-12-24", milk, null, true, today))
    }

    @Test
    fun freezersReplaceTheDefaultButNotATypedDate() {
        val freezer = Location(id = 5, name = "Freezer", isFreezer = true)
        val frozenMilk = milk.copy(defaultDueDaysAfterFreezing = 60)

        assertEquals("2026-12-29", purchaseDueDate("2026-11-06", frozenMilk, freezer, true, today))
        assertEquals("2026-12-24", purchaseDueDate("2026-12-24", frozenMilk, freezer, true, today))
        assertEquals("2026-11-06", purchaseDueDate("2026-11-06", frozenMilk, freezer, false, today))
    }

    @Test
    fun inventorySaysWhatItAddsOrRemoves() {
        assertEquals(InventoryChange.Added(2.0), inventoryChange(5.0, 3.0))
        assertEquals(InventoryChange.Removed(1.0), inventoryChange(2.0, 3.0))
        assertEquals(InventoryChange.None, inventoryChange(3.0, 3.0))
        // The weighed container is not stock
        assertEquals(InventoryChange.Added(50.0), inventoryChange(450.0, 300.0, tareWeight = 100.0))
    }

    @Test
    fun consumeTakesTheDefaultConsumeLocationWithStock() {
        val amounts = listOf(
            ProductLocationAmount(4, "Pantry", 1.0),
            ProductLocationAmount(2, "Fridge", 1.0),
        )

        assertEquals(2, consumeLocation(milk, amounts))
        assertEquals(4, consumeLocation(milk.copy(defaultConsumeLocationId = 4), amounts))
        assertEquals(4, consumeLocation(milk, amounts.take(1)))
        assertNull(consumeLocation(milk, emptyList()))
    }

    @Test
    fun stockAtALocationKnowsItsOpenedPart() {
        val entries = listOf(
            entry("a", 2.0, location = 1, open = true),
            entry("b", 3.0, location = 1),
            entry("c", 4.0, location = 2),
        )

        assertEquals(LocationStock(5.0, 2.0), locationStockOf(entries, 1))
        assertEquals(9.0, locationStockOf(entries, null).amount)
        assertEquals(3.0, locationStockOf(entries, 1).unopened)
    }

    @Test
    fun purchaseAmountsStartAtTheSmallestStep() {
        val limits = amountLimits(BookingKind.PURCHASE, milk, null, 1.0, 2, available = null)

        assertEquals(AmountLimits(0.01, null), limits)
    }

    @Test
    fun weighedPurchasesMustOutweighContainerAndStock() {
        val weighed = milk.copy(enableTareWeightHandling = true, tareWeight = 100.0)

        val limits = amountLimits(
            BookingKind.PURCHASE,
            weighed,
            ProductStockDetails(1, stockAmount = 50.0),
            1.0,
            0,
            available = null,
        )

        assertEquals(151.0, limits.min)
    }

    @Test
    fun consumptionIsLimitedToTheStockInTheChosenUnit() {
        val limits = amountLimits(
            BookingKind.CONSUME,
            milk,
            ProductStockDetails(1, stockAmount = 6.0),
            factor = 0.5,
            decimals = 2,
            available = 6.0,
        )

        assertEquals(3.0, limits.max)
    }

    @Test
    fun parentsConsumeTheirSubProductsWithoutLimit() {
        val limits = amountLimits(
            BookingKind.CONSUME,
            parent,
            ProductStockDetails(2, hasSubProducts = true),
            1.0,
            2,
            available = 0.0,
        )

        assertNull(limits.max)
    }

    @Test
    fun inventoryCannotEqualTheCurrentStock() {
        val limits = amountLimits(
            BookingKind.INVENTORY,
            milk,
            ProductStockDetails(1, stockAmount = 3.0),
            1.0,
            2,
            available = null,
        )

        assertEquals(AmountLimits(0.0, null, notEqual = 3.0), limits)
    }

    @Test
    fun aValidPurchaseHasNoErrors() {
        assertTrue(validateBooking(input()).isEmpty())
    }

    @Test
    fun missingProductAndUnitAreRequired() {
        val errors = validateBooking(input(productId = null, quId = null))

        assertEquals(BookingError.Required, errors[BookingField.PRODUCT])
        // Without product no unit is expected yet
        assertNull(errors[BookingField.UNIT])
    }

    @Test
    fun amountsOutsideTheLimitsOrWithTooManyDecimalsAreWrong() {
        assertIs<BookingError.InvalidAmount>(
            validateBooking(input(amount = "0"))[BookingField.AMOUNT],
        )
        assertIs<BookingError.InvalidAmount>(
            validateBooking(input(amount = "1.234"))[BookingField.AMOUNT],
        )
        assertIs<BookingError.InvalidAmount>(
            validateBooking(input(amount = "5", limits = AmountLimits(0.01, 4.0)))[
                BookingField.AMOUNT,
            ],
        )
        assertIs<BookingError.InvalidAmount>(
            validateBooking(input(amount = "3", limits = AmountLimits(0.0, null, 3.0)))[
                BookingField.AMOUNT,
            ],
        )
    }

    @Test
    fun anEmptyLocationHasNoStockToBook() {
        val errors = validateBooking(input(noStockAtLocation = true))

        assertEquals(BookingError.NoStockAtLocation, errors[BookingField.AMOUNT])
    }

    @Test
    fun dueDatesMustBeDatesWhereNeeded() {
        assertEquals(
            BookingError.Required,
            validateBooking(input(dueDate = ""))[BookingField.DUE_DATE],
        )
        assertEquals(
            BookingError.InvalidDate,
            validateBooking(input(dueDate = "2026-13-01"))[BookingField.DUE_DATE],
        )
        assertNull(validateBooking(input(dueDate = null))[BookingField.DUE_DATE])
    }

    @Test
    fun pricesAreOptionalButValid() {
        assertNull(validateBooking(input(price = ""))[BookingField.PRICE])
        assertEquals(
            BookingError.InvalidPrice(2),
            validateBooking(input(price = "1.999"))[BookingField.PRICE],
        )
        assertEquals(
            BookingError.InvalidPrice(2),
            validateBooking(input(price = "-1"))[BookingField.PRICE],
        )
    }

    @Test
    fun requiredLocationsAndEntriesAreChecked() {
        val errors = validateBooking(
            input(locationRequired = true, toLocationRequired = true, stockEntryRequired = true),
        )

        assertEquals(BookingError.Required, errors[BookingField.LOCATION])
        assertEquals(BookingError.Required, errors[BookingField.TO_LOCATION])
        assertEquals(BookingError.Required, errors[BookingField.STOCK_ENTRY])
    }

    private fun input(
        productId: Int? = 1,
        amount: String = "1",
        quId: Int? = 1,
        limits: AmountLimits = AmountLimits(0.01, null),
        noStockAtLocation: Boolean = false,
        dueDate: String? = "2026-11-06",
        price: String = "",
        locationRequired: Boolean = false,
        toLocationRequired: Boolean = false,
        stockEntryRequired: Boolean = false,
    ) = BookingInput(
        kind = BookingKind.PURCHASE,
        productId = productId,
        amount = amount,
        quId = quId,
        decimals = 2,
        limits = limits,
        noStockAtLocation = noStockAtLocation,
        dueDate = dueDate,
        price = price,
        priceDecimals = 2,
        locationRequired = locationRequired,
        toLocationRequired = toLocationRequired,
        stockEntryRequired = stockEntryRequired,
    )

    private fun entry(stockId: String, amount: Double, location: Int, open: Boolean = false) =
        StockEntry(
            id = stockId.hashCode(),
            productId = 1,
            amount = amount,
            bestBeforeDate = null,
            purchasedDate = null,
            stockId = stockId,
            open = open,
            locationId = location,
        )
}
