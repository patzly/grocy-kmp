package com.patrickzedler.grocy.feature.stock.impl.stockoverview

import com.patrickzedler.grocy.core.forms.EntryPrice
import com.patrickzedler.grocy.core.model.masterdata.DueType
import com.patrickzedler.grocy.core.model.masterdata.Location
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.ProductBarcode
import com.patrickzedler.grocy.core.model.masterdata.ProductGroup
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.ResolvedQuantityUnitConversion
import com.patrickzedler.grocy.core.model.masterdata.StockAmount
import com.patrickzedler.grocy.core.model.masterdata.Store
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.server.UserSettings
import com.patrickzedler.grocy.core.model.stock.DueStatus
import com.patrickzedler.grocy.core.model.stock.ProductStockInfo
import com.patrickzedler.grocy.core.model.stock.StockEntry
import com.patrickzedler.grocy.core.model.stock.StockLocationAmount
import com.patrickzedler.grocy.core.model.stock.StockOverviewField
import com.patrickzedler.grocy.core.model.stock.StockOverviewGrouping
import com.patrickzedler.grocy.core.model.text.epochDayOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StockOverviewListTest {

    private val today = epochDayOf("2026-10-25")!!

    // Milk expires soon, cheese is overdue, eggs are fine, bread is out of stock but missing,
    // jam is neither in stock nor missing, chips are hidden on the stock overview
    private val milk = Product(
        id = 1,
        name = "Milk",
        productGroupId = 1,
        locationId = 1,
        quIdStock = 1,
        quIdConsume = 1,
        quIdPrice = 2,
        calories = 60.0,
        dueType = DueType.EXPIRATION,
    )
    private val cheese = Product(id = 2, name = "Cheese", productGroupId = 1, quIdStock = 1)
    private val eggs = Product(
        id = 3,
        name = "Eggs",
        quIdStock = 1,
        storeId = 1,
        pictureFileName = "eggs.jpg",
        description = "<p>Free range</p>",
    )
    private val bread = Product(id = 4, name = "Bread", quIdStock = 1, minStockAmount = 2.0)
    private val jam = Product(id = 5, name = "Jam", quIdStock = 1)
    private val chips = Product(id = 6, name = "Chips", quIdStock = 1, hideOnStockOverview = true)
    private val units = listOf(QuantityUnit(1, "Piece", "Pieces"), QuantityUnit(2, "Pack", "Packs"))

    private val data = StockOverviewData(
        products = listOf(milk, cheese, eggs, bread, jam, chips),
        productGroups = listOf(ProductGroup(1, "Dairy")),
        locations = listOf(Location(1, "Fridge"), Location(2, "Pantry")),
        quantityUnits = units,
        stores = listOf(Store(1, "Farm")),
        barcodes = listOf(ProductBarcode(1, productId = 3, barcode = "4001")),
        stock = listOf(
            StockAmount(
                productId = 1,
                amount = 2.0,
                amountAggregated = 2.0,
                amountOpened = 1.0,
                amountOpenedAggregated = 1.0,
                value = 3.0,
                bestBeforeDate = "2026-10-27",
            ),
            StockAmount(2, 1.0, 1.0, value = 4.5, bestBeforeDate = "2026-10-20"),
            StockAmount(3, 6.0, 6.0, value = 2.0, bestBeforeDate = "2026-12-01"),
            StockAmount(6, 1.0, 1.0, value = 1.0, bestBeforeDate = "2026-11-01"),
        ),
        info = listOf(
            ProductStockInfo(
                productId = 3,
                lastPurchased = "2026-10-01",
                lastPrice = 0.3,
                averagePrice = 0.25,
            ),
            ProductStockInfo(4, amountMissing = 2.0, onShoppingList = true),
        ),
        stockLocations = listOf(
            StockLocationAmount(1, 1, 2.0),
            StockLocationAmount(2, 1, 1.0),
            StockLocationAmount(3, 2, 6.0),
        ),
        resolvedConversions = listOf(ResolvedQuantityUnitConversion(1, 2, 1, 6.0)),
    )

    private fun build(
        filter: StockOverviewFilter = StockOverviewFilter(),
        sort: StockOverviewSort = StockOverviewSort(StockOverviewField.NEXT_DUE_DATE),
        grouping: StockOverviewGrouping = StockOverviewGrouping.NONE,
        query: String = "",
        config: ServerConfig = ServerConfig(),
        settings: UserSettings = UserSettings(stockDecimalPlacesAmounts = 2),
        data: StockOverviewData = this.data,
    ) = buildStockOverview(
        data = data,
        filter = filter,
        sort = sort,
        grouping = grouping,
        query = query,
        config = config,
        settings = settings,
        today = today,
        decimalSeparator = '.',
        language = "en",
    )

    private fun StockOverviewList.names() = groups.flatMap { group -> group.items.map { it.name } }

    private fun StockOverviewList.item(name: String) =
        groups.flatMap { it.items }.single { it.name == name }

    @Test
    fun productsInStockOrMissingAreListedByNextDueDateLikeGrocy() {
        // Unknown due dates (bread has no stock) come after the known ones
        assertEquals(listOf("Cheese", "Milk", "Eggs", "Bread"), build().names())
    }

    @Test
    fun everyActiveProductShowsWithTheUserSettingToShowAll() {
        val list = build(settings = UserSettings(stockOverviewShowAllOutOfStockProducts = true))

        assertEquals(listOf("Cheese", "Milk", "Eggs", "Bread", "Jam"), list.names())
    }

    @Test
    fun headerCountsTheProductsInStockAndTheirValueLikeGrocy() {
        val list = build()

        // Hidden products count for the value only, like Grocy's sums
        assertEquals(3, list.productCount)
        assertEquals(10.5, list.totalValue)
    }

    @Test
    fun statusesAreCountedWithoutTheFilters() {
        val list = build(filter = StockOverviewFilter(status = StockStatus.EXPIRED))

        assertEquals(
            mapOf(
                StockStatus.NOT_FRESH to 2,
                StockStatus.DUE_SOON to 1,
                StockStatus.OVERDUE to 1,
                StockStatus.EXPIRED to 0,
                StockStatus.BELOW_MIN_STOCK to 1,
                StockStatus.IN_STOCK to 3,
                StockStatus.OPENED to 1,
            ),
            list.statusCounts,
        )
    }

    @Test
    fun statusFilterKeepsTheMatchingProducts() {
        fun names(status: StockStatus) = build(StockOverviewFilter(status = status)).names()

        assertEquals(listOf("Cheese", "Milk"), names(StockStatus.NOT_FRESH))
        assertEquals(listOf("Milk"), names(StockStatus.DUE_SOON))
        assertEquals(listOf("Cheese"), names(StockStatus.OVERDUE))
        assertEquals(listOf("Bread"), names(StockStatus.BELOW_MIN_STOCK))
        assertEquals(listOf("Cheese", "Milk", "Eggs"), names(StockStatus.IN_STOCK))
        assertEquals(listOf("Milk"), names(StockStatus.OPENED))
    }

    @Test
    fun dueStatusFollowsTheDueTypeAndTheDueSoonDays() {
        val list = build()

        assertEquals(DueStatus.DUE_SOON, list.item("Milk").dueStatus)
        // A best before date in the past is overdue, an expiration date expired
        assertEquals(DueStatus.OVERDUE, list.item("Cheese").dueStatus)
        assertNull(list.item("Eggs").dueStatus)
        val products = data.products.map {
            if (it.id == 2) it.copy(dueType = DueType.EXPIRATION) else it
        }
        val expired = build(data = data.copy(products = products))
        assertEquals(DueStatus.EXPIRED, expired.item("Cheese").dueStatus)
    }

    @Test
    fun locationFilterKeepsProductsWithStockThere() {
        val list = build(StockOverviewFilter(locationId = 2))

        assertEquals(listOf("Eggs"), list.names())
    }

    @Test
    fun productGroupFilterKeepsTheGroupsProducts() {
        assertEquals(
            listOf("Cheese", "Milk"),
            build(StockOverviewFilter(productGroupId = 1)).names(),
        )
    }

    @Test
    fun searchFindsTextsAndCompleteBarcodes() {
        assertEquals(setOf("Milk", "Cheese"), build(query = "dairy").names().toSet())
        assertEquals(listOf("Eggs"), build(query = "4001").names())
    }

    @Test
    fun rowsShowAmountsUnitsAndPricesLikeGrocysColumns() {
        val list = build()
        val milk = list.item("Milk")
        val eggs = list.item("Eggs")

        assertEquals("2 Pieces", milk.amount)
        assertEquals("1", milk.amountOpened)
        assertNull(milk.amountAggregated)
        assertEquals("60 kcal", milk.caloriesPerUnit)
        assertEquals("120 kcal", milk.calories)
        assertEquals("Dairy", milk.productGroupName)
        assertEquals("Fridge", milk.defaultLocationName)
        assertEquals(3.0, milk.value)
        assertEquals("Farm", eggs.defaultStoreName)
        assertEquals(epochDayOf("2026-10-01"), eggs.lastPurchasedDay)
        assertEquals(EntryPrice(0.3, "Piece"), eggs.lastPrice)
        assertEquals("Free range", eggs.description)
        assertEquals("eggs.jpg", eggs.pictureFileName)
    }

    @Test
    fun pricesAreShownPerPriceUnit() {
        val list = build(
            data = data.copy(
                info = data.info + ProductStockInfo(1, lastPrice = 0.5, averagePrice = 0.4),
            ),
        )

        assertEquals(EntryPrice(3.0, "Pack"), list.item("Milk").lastPrice)
        val average = list.item("Milk").averagePrice
        assertEquals(2.4, average?.price ?: 0.0, absoluteTolerance = 1e-9)
        assertEquals("Pack", average?.unit)
    }

    @Test
    fun missingProductIsMarkedAndOnTheShoppingListWithTheUserSetting() {
        val bread = build().item("Bread")

        assertTrue(bread.isBelowMinStock)
        assertTrue(bread.onShoppingList)
        assertEquals("2 Pieces", bread.minStockAmount)
        val hidden = build(
            settings = UserSettings(showIconOnStockOverviewWhenOnShoppingList = false),
        )
        assertFalse(hidden.item("Bread").onShoppingList)
    }

    @Test
    fun parentProductsShowTheAmountIncludingTheirSubProducts() {
        val parent = Product(id = 7, name = "Fruit", quIdStock = 2, noOwnStock = true)
        val list = build(
            data = data.copy(
                products = data.products + parent,
                stock = data.stock + StockAmount(7, 0.0, 3.0, isAggregatedAmount = true),
            ),
        )

        val fruit = list.item("Fruit")
        assertNull(fruit.amount)
        assertEquals("3 Packs", fruit.amountAggregated)
        assertTrue(fruit.isInStock)
    }

    @Test
    fun quickActionsFollowGrocysRowButtons() {
        val list = build(
            data = data.copy(
                products = data.products.map {
                    if (it.id == 3) it.copy(quickConsumeAmount = 10.0, disableOpen = true) else it
                },
            ),
        )

        val milk = list.item("Milk")
        assertEquals("1 Piece", milk.quickConsume)
        assertTrue(milk.canConsume && milk.canConsumeAll && milk.canOpen)
        // More than in stock, opening turned off
        val eggs = list.item("Eggs")
        assertFalse(eggs.canConsume)
        assertTrue(eggs.canConsumeAll)
        assertFalse(eggs.canOpen)
        // Nothing in stock
        val bread = list.item("Bread")
        assertFalse(bread.canConsume || bread.canConsumeAll || bread.canOpen)
    }

    @Test
    fun bookingsUseGrocysAmountsInTheStockUnit() {
        val parent = Product(id = 7, name = "Fruit", quIdStock = 2, noOwnStock = true)
        val list = build(
            data = data.copy(
                products = data.products.map {
                    when (it.id) {
                        1 -> it.copy(moveOnOpen = true, defaultConsumeLocationId = 2)
                        3 -> it.copy(enableTareWeightHandling = true, tareWeight = 0.5)
                        else -> it
                    }
                } + parent,
                stock = data.stock + StockAmount(7, 0.0, 3.0, isAggregatedAmount = true),
            ),
        )

        val milk = list.item("Milk")
        assertEquals(1.0, milk.quickConsumeAmount)
        assertEquals(2.0, milk.consumeAllAmount)
        assertEquals("2 Pieces", milk.consumeAll)
        assertEquals("Pantry", milk.openMovesTo)
        // With tare weight handling "All" consumes down to the container's weight
        assertEquals(0.5, list.item("Eggs").consumeAllAmount)
        // A parent without stock of its own consumes its sub products
        assertEquals(3.0, list.item("Fruit").consumeAllAmount)
        assertNull(list.item("Cheese").openMovesTo)
    }

    @Test
    fun openingMovesNowhereWithoutLocationTracking() {
        val list = build(
            config = ServerConfig(stockLocationTracking = false),
            data = data.copy(
                products = data.products.map {
                    if (it.id == 1) it.copy(moveOnOpen = true, defaultConsumeLocationId = 2) else it
                },
            ),
        )

        assertNull(list.item("Milk").openMovesTo)
    }

    @Test
    fun everythingOpenedCannotBeOpenedAgain() {
        val list = build(
            data = data.copy(
                stock = data.stock.map {
                    if (it.productId == 2) {
                        it.copy(amountOpened = 1.0, amountOpenedAggregated = 1.0)
                    } else {
                        it
                    }
                },
            ),
        )

        assertFalse(list.item("Cheese").canOpen)
    }

    @Test
    fun fieldsOfTurnedOffFeaturesAreLeftOut() {
        val config = ServerConfig(
            stockPriceTracking = false,
            stockLocationTracking = false,
            stockBestBeforeDateTracking = false,
        )

        val list = build(config = config)

        assertNull(list.item("Milk").value)
        assertNull(list.item("Milk").defaultLocationName)
        assertNull(list.item("Milk").dueDay)
        assertNull(list.item("Eggs").lastPrice)
        val withoutFeatures = setOf(
            StockOverviewField.NAME,
            StockOverviewField.VALUE,
            StockOverviewField.NEXT_DUE_DATE,
            StockOverviewField.LAST_PRICE,
            StockOverviewField.DEFAULT_LOCATION,
            StockOverviewField.AVERAGE_PRICE,
            StockOverviewField.DEFAULT_STORE,
        )
        assertEquals(StockOverviewField.entries - withoutFeatures, availableFields(config))
        assertEquals(
            listOf(
                StockOverviewGrouping.NONE,
                StockOverviewGrouping.PRODUCT_GROUP,
                StockOverviewGrouping.LAST_PURCHASED,
                StockOverviewGrouping.MIN_STOCK_AMOUNT,
                StockOverviewGrouping.PARENT_PRODUCT,
            ),
            availableGroupings(config),
        )
        assertEquals(
            listOf(StockStatus.BELOW_MIN_STOCK, StockStatus.IN_STOCK, StockStatus.OPENED),
            availableStatuses(config),
        )
    }

    @Test
    fun sortByAFieldPutsEmptyValuesLastInBothDirections() {
        val ascending = build(sort = StockOverviewSort(StockOverviewField.PRODUCT_GROUP))
        val descending = build(sort = StockOverviewSort(StockOverviewField.PRODUCT_GROUP, false))

        // Ties by name
        assertEquals(listOf("Cheese", "Milk", "Bread", "Eggs"), ascending.names())
        assertEquals(listOf("Cheese", "Milk", "Bread", "Eggs"), descending.names())
        assertEquals(
            listOf("Eggs", "Milk", "Cheese", "Bread"),
            build(sort = StockOverviewSort(StockOverviewField.AMOUNT, false)).names(),
        )
    }

    @Test
    fun hiddenSortFieldFallsBackToGrocysDefaultOrder() {
        val shown = listOf(StockOverviewField.AMOUNT, StockOverviewField.NEXT_DUE_DATE)

        assertEquals(
            StockOverviewSort(StockOverviewField.AMOUNT, false),
            effectiveSort(StockOverviewField.AMOUNT, false, shown),
        )
        assertEquals(
            StockOverviewSort(StockOverviewField.NEXT_DUE_DATE),
            effectiveSort(StockOverviewField.VALUE, false, shown),
        )
        assertEquals(
            StockOverviewSort(StockOverviewField.NAME),
            effectiveSort(StockOverviewField.VALUE, false, listOf(StockOverviewField.AMOUNT)),
        )
        // The name is always the title and can always sort
        assertEquals(
            StockOverviewSort(StockOverviewField.NAME, false),
            effectiveSort(StockOverviewField.NAME, false, emptyList()),
        )
    }

    @Test
    fun pictureCannotSort() {
        assertEquals(
            listOf(StockOverviewField.NAME, StockOverviewField.AMOUNT),
            sortFields(listOf(StockOverviewField.AMOUNT, StockOverviewField.PICTURE)),
        )
    }

    @Test
    fun groupsAreOrderedByTheirValueWithRowsWithoutOneLast() {
        val list = build(grouping = StockOverviewGrouping.PRODUCT_GROUP)

        assertEquals(
            listOf(StockOverviewGroupTitle.Text("Dairy"), StockOverviewGroupTitle.None),
            list.groups.map { it.title },
        )
        // The rows keep the chosen order inside their group
        assertEquals(listOf("Cheese", "Milk"), list.groups[0].items.map { it.name })
        assertEquals(listOf("Eggs", "Bread"), list.groups[1].items.map { it.name })
    }

    @Test
    fun dueDateGroupsAreDaysWithNeverOverdueAfterThem() {
        val list = build(
            grouping = StockOverviewGrouping.NEXT_DUE_DATE,
            data = data.copy(
                stock = data.stock.map {
                    if (it.productId == 3) {
                        it.copy(bestBeforeDate = StockEntry.NEVER_OVERDUE)
                    } else {
                        it
                    }
                },
            ),
        )

        assertEquals(
            listOf(
                StockOverviewGroupTitle.Day(epochDayOf("2026-10-20")!!),
                StockOverviewGroupTitle.Day(epochDayOf("2026-10-27")!!),
                StockOverviewGroupTitle.NeverOverdue,
                StockOverviewGroupTitle.None,
            ),
            list.groups.map { it.title },
        )
    }

    @Test
    fun ungroupedListIsOneGroupWithoutTitle() {
        val list = build()

        assertEquals(1, list.groups.size)
        assertNull(list.groups.single().title)
        assertTrue(build(query = "nothing like this").groups.isEmpty())
    }
}
