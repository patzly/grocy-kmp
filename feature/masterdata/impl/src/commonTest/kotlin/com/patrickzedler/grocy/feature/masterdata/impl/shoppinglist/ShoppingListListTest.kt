package com.patrickzedler.grocy.feature.masterdata.impl.shoppinglist

import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.ProductBarcode
import com.patrickzedler.grocy.core.model.masterdata.ProductGroup
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.ResolvedQuantityUnitConversion
import com.patrickzedler.grocy.core.model.masterdata.Store
import com.patrickzedler.grocy.core.model.masterdata.Userfield
import com.patrickzedler.grocy.core.model.masterdata.UserfieldType
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.server.UserSettings
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingList
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListField
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListGrouping
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListItem
import com.patrickzedler.grocy.core.model.stock.ProductStockInfo
import com.patrickzedler.grocy.feature.masterdata.impl.stock.EntryPrice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShoppingListListTest {

    private val piece = QuantityUnit(1, "Piece", namePlural = "Pieces")
    private val pack = QuantityUnit(2, "Pack", namePlural = "Packs")
    private val milk = Product(
        id = 1,
        name = "Milk",
        productGroupId = 1,
        storeId = 1,
        quIdStock = 1,
        quIdPurchase = 2,
        userfields = mapOf("brand" to "Acme"),
    )
    private val bread = Product(id = 2, name = "bread", quIdStock = 1, quIdPurchase = 1)
    private val apples = Product(id = 3, name = "Apples", productGroupId = 2, quIdStock = 1)

    private val data = ShoppingListData(
        items = listOf(
            // 9 pieces shown in packs of 6
            ShoppingListItem(5, 1, 1, "Low fat", amount = 9.0, quId = 2, done = false),
            ShoppingListItem(6, 1, null, "Batteries", amount = 2.0, quId = null, done = true),
            ShoppingListItem(7, 1, 2, null, amount = 1.0, quId = 1, done = false),
            ShoppingListItem(8, 1, 3, null, amount = 3.0, quId = 1, done = true),
            ShoppingListItem(9, 2, 1, null, amount = 100.0, quId = 1, done = false),
        ),
        products = listOf(milk, bread, apples),
        productGroups = listOf(ProductGroup(1, "Dairy"), ProductGroup(2, "Fruit")),
        quantityUnits = listOf(piece, pack),
        stores = listOf(Store(1, "Supermarket")),
        barcodes = listOf(ProductBarcode(1, 1, "4006381333931")),
        resolvedConversions = listOf(
            ResolvedQuantityUnitConversion(productId = 1, fromQuId = 1, toQuId = 1, 1.0),
            ResolvedQuantityUnitConversion(productId = 1, fromQuId = 1, toQuId = 2, 1.0 / 6),
        ),
        info = listOf(
            ProductStockInfo(1, amountMissing = 2.0, lastPrice = 0.5),
            ProductStockInfo(2, lastPrice = 2.0),
        ),
        userfields = listOf(
            Userfield(
                id = 1,
                entity = MasterDataRepository.PRODUCTS,
                name = "brand",
                caption = "Brand",
                type = UserfieldType.TEXT_SINGLE_LINE,
                showAsColumnInTables = true,
            ),
        ),
    )

    private fun build(
        status: ShoppingListStatus? = null,
        query: String = "",
        grouping: ShoppingListGrouping = ShoppingListGrouping.PRODUCT_GROUP,
        config: ServerConfig = ServerConfig(),
        settings: UserSettings = UserSettings(stockDecimalPlacesAmounts = 2),
        listId: Int = 1,
    ) = buildShoppingList(
        data = data,
        listId = listId,
        status = status,
        query = query,
        grouping = grouping,
        config = config,
        settings = settings,
        decimalSeparator = '.',
        language = "en",
    )

    private fun ShoppingListContent.rows() = groups.flatMap { it.rows }

    @Test
    fun itemsAreGroupedByProductGroupWithUngroupedLast() {
        val list = build()

        assertEquals(
            listOf(
                ShoppingListGroupTitle.Text("Dairy"),
                ShoppingListGroupTitle.Text("Fruit"),
                ShoppingListGroupTitle.Ungrouped,
            ),
            list.groups.map { it.title },
        )
        // By product name or note, without regard to case
        assertEquals(listOf("Batteries", "bread"), list.groups.last().rows.map { it.title })
    }

    @Test
    fun withoutGroupingTheRowsAreOneGroupByName() {
        val list = build(grouping = ShoppingListGrouping.NONE)

        assertNull(list.groups.single().title)
        assertEquals(
            listOf("Apples", "Batteries", "bread", "Milk"),
            list.rows().map { it.title },
        )
    }

    @Test
    fun groupingByStoreNamesItemsWithoutStoreNotSet() {
        val list = build(grouping = ShoppingListGrouping.DEFAULT_STORE)

        assertEquals(
            listOf(ShoppingListGroupTitle.Text("Supermarket"), ShoppingListGroupTitle.NotSet),
            list.groups.map { it.title },
        )
    }

    @Test
    fun amountIsShownInTheItemsUnitAndNotesAsPlainNumber() {
        val rows = build().rows().associateBy { it.id }

        assertEquals("1.5 Packs", rows.getValue(5).amount)
        assertEquals("1 Piece", rows.getValue(7).amount)
        assertEquals("2", rows.getValue(6).amount)
        assertEquals("Low fat", rows.getValue(5).note)
        assertNull(rows.getValue(6).note)
    }

    @Test
    fun roundingUpOnlyChangesWhatIsShown() {
        val rows = build(settings = UserSettings(shoppingListRoundUp = true)).rows()
            .associateBy { it.id }

        assertEquals("2 Packs", rows.getValue(5).amount)
        // The total stays per stock unit times the stored amount
        assertEquals(4.5, rows.getValue(5).lastPriceTotal)
    }

    @Test
    fun pricesArePerShownUnitAndForTheWholeAmount() {
        val milkRow = build().rows().first { it.id == 5 }

        assertEquals(EntryPrice(3.0, "Pack"), milkRow.lastPriceUnit)
        assertEquals(4.5, milkRow.lastPriceTotal)
    }

    @Test
    fun totalValueSumsOnlyTheShownListDoneItemsIncluded() {
        assertEquals(4.5 + 2.0, build().totalValue)
        assertEquals(50.0, build(listId = 2).totalValue)
    }

    @Test
    fun withoutPriceTrackingThereAreNoPricesOrStores() {
        val milkRow = build(config = ServerConfig(stockPriceTracking = false)).rows()
            .first { it.id == 5 }

        assertNull(milkRow.lastPriceUnit)
        assertNull(milkRow.defaultStoreName)
    }

    @Test
    fun statusFilterAndItsCountsFollowTheList() {
        val list = build(status = ShoppingListStatus.DONE)

        assertEquals(listOf(6, 8), list.rows().map { it.id }.sorted())
        assertEquals(
            mapOf(
                ShoppingListStatus.BELOW_MIN_STOCK to 1,
                ShoppingListStatus.DONE to 2,
                ShoppingListStatus.UNDONE to 2,
            ),
            list.statusCounts,
        )
        assertEquals(4, list.itemCount)
    }

    @Test
    fun belowMinStockNeedsTheStock() {
        val list = build(config = ServerConfig(stock = false))

        assertFalse(list.rows().any { it.isBelowMinStock })
        assertFalse(ShoppingListStatus.BELOW_MIN_STOCK in list.statusCounts)
        assertTrue(list.rows().all { it.purchase == null })
    }

    @Test
    fun productItemsCanBeAddedToTheStockInTheirStockAmount() {
        val rows = build().rows().associateBy { it.id }

        assertEquals(ShoppingListPurchase(5, 1, 9.0, 2), rows.getValue(5).purchase)
        assertNull(rows.getValue(6).purchase)
    }

    @Test
    fun searchFindsNotesGroupsUserfieldsAndBarcodes() {
        assertEquals(listOf(6), build(query = "batteris").rows().map { it.id })
        assertEquals(listOf(5), build(query = "acme").rows().map { it.id })
        assertEquals(listOf(5), build(query = "4006381333931").rows().map { it.id })
        assertEquals(listOf(8), build(query = "fruit").rows().map { it.id })
    }

    @Test
    fun shownUserfieldsComeWithTheirCaption() {
        val milkRow = build().rows().first { it.id == 5 }

        assertEquals(listOf("Brand" to "Acme"), milkRow.userfields)
        assertEquals(listOf("4006381333931"), milkRow.barcodes)
    }

    @Test
    fun userfieldValuesAreShownLikeGrocysCells() {
        fun field(type: UserfieldType) = Userfield(1, "shopping_list", "f", "Urgent", type)

        assertEquals("Urgent", userfieldText(field(UserfieldType.CHECKBOX), "1"))
        assertNull(userfieldText(field(UserfieldType.CHECKBOX), "0"))
        assertEquals(
            "Grocy",
            userfieldText(
                field(UserfieldType.LINK_WITH_TITLE),
                """{"title":"Grocy","link":"https://grocy.info"}""",
            ),
        )
        assertEquals("a, b", userfieldText(field(UserfieldType.PRESET_CHECKLIST), "a,b"))
        assertNull(userfieldText(field(UserfieldType.IMAGE), "x.jpg"))
        assertNull(userfieldText(field(UserfieldType.TEXT_SINGLE_LINE), " "))
    }

    @Test
    fun shownListIsTheChosenOneWhileItExists() {
        val lists = listOf(ShoppingList(2, "Hardware store"), ShoppingList(1, "Shopping list"))

        assertEquals(2, shownList(lists, selectedId = 2, ServerConfig())?.id)
        assertEquals(1, shownList(lists, selectedId = 3, ServerConfig())?.id)
        assertEquals(1, shownList(lists, selectedId = null, ServerConfig())?.id)
        // Without multiple lists always the first
        val single = ServerConfig(shoppingListMultipleLists = false)
        assertEquals(1, shownList(lists, selectedId = 2, single)?.id)
        assertNull(shownList(emptyList(), selectedId = 2, ServerConfig()))
    }

    @Test
    fun availableFieldsAndGroupingsFollowPriceTracking() {
        val config = ServerConfig(stockPriceTracking = false)

        assertEquals(
            listOf(
                ShoppingListField.AMOUNT,
                ShoppingListField.PRODUCT_GROUP,
                ShoppingListField.BARCODES,
                ShoppingListField.USERFIELDS,
            ),
            availableShoppingListFields(config),
        )
        assertFalse(ShoppingListGrouping.DEFAULT_STORE in availableShoppingListGroupings(config))
    }
}
