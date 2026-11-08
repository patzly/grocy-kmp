package com.patrickzedler.grocy.core.wear

import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.ProductGroup
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.ResolvedQuantityUnitConversion
import com.patrickzedler.grocy.core.model.masterdata.Store
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.server.UserSettings
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingList
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListGrouping
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListItem
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListItemSync
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListOptions
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListStatus
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingModeSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WearShoppingStateTest {

    private val names = WearShoppingNames(
        products = listOf(
            Product(id = 1, name = "Bananas", productGroupId = 1, storeId = 1, quIdStock = 1),
            Product(id = 2, name = "Apple", productGroupId = 1, quIdStock = 1),
            Product(id = 3, name = "Bread", productGroupId = 2, storeId = 1, quIdStock = 1),
            Product(id = 4, name = "Cake", quIdStock = 1),
        ),
        productGroups = listOf(
            ProductGroup(id = 1, name = "Fruit"),
            ProductGroup(id = 2, name = "Bakery"),
        ),
        quantityUnits = listOf(
            QuantityUnit(id = 1, name = "Piece", namePlural = "Pieces"),
            QuantityUnit(id = 2, name = "Pack", namePlural = "Packs"),
        ),
        stores = listOf(Store(id = 1, name = "Market")),
    )

    private fun item(
        id: Int,
        productId: Int?,
        done: Boolean = false,
        listId: Int = 1,
        note: String? = null,
        amount: Double = 1.0,
        quId: Int? = 1,
    ) = ShoppingListItem(id, listId, productId, note, amount, quId, done)

    private val items = listOf(
        item(1, productId = 1),
        item(2, productId = 2, done = true),
        item(3, productId = 3),
        item(4, productId = 4),
    )

    private fun state(
        items: List<ShoppingListItem> = this.items,
        grouping: ShoppingListGrouping = ShoppingListGrouping.PRODUCT_GROUP,
        doneLast: Boolean = true,
        status: ShoppingListStatus? = null,
        config: ServerConfig = ServerConfig(),
        userSettings: UserSettings = UserSettings(),
        names: WearShoppingNames = this.names,
        lists: List<ShoppingList> = listOf(ShoppingList(1, "Shop")),
        selected: Int? = null,
        syncStates: Map<Int, ShoppingListItemSync> = emptyMap(),
    ) = wearShoppingState(
        data = WearShoppingData(
            lists = lists,
            selectedListId = selected,
            items = items,
            syncStates = syncStates,
            names = names,
            config = config,
            userSettings = userSettings,
        ),
        options = ShoppingListOptions(grouping = grouping),
        settings = ShoppingModeSettings(doneItemsLast = doneLast),
        status = status,
        language = "en",
        decimalSeparator = '.',
    )

    private fun WearShoppingState.titles() = groups.map { group -> group.items.map { it.title } }

    private fun text(value: String) = WearShoppingGroupTitle.Text(value)

    @Test
    fun groupsComeByNameUngroupedThenDone() {
        val state = state()

        assertEquals(
            listOf(
                text("Bakery"),
                text("Fruit"),
                WearShoppingGroupTitle.Ungrouped,
                WearShoppingGroupTitle.Done,
            ),
            state.groups.map { it.title },
        )
        assertEquals(
            listOf(listOf("Bread"), listOf("Bananas"), listOf("Cake"), listOf("Apple")),
            state.titles(),
        )
    }

    @Test
    fun theDoneGroupExistsOnlyWhileAnItemIsDone() {
        val state = state(items = listOf(item(1, productId = 1)))

        assertEquals(listOf<WearShoppingGroupTitle?>(text("Fruit")), state.groups.map { it.title })
    }

    @Test
    fun withoutDoneLastDoneItemsStayInTheirGroups() {
        val state = state(doneLast = false)

        assertEquals(
            listOf(listOf("Bread"), listOf("Apple", "Bananas"), listOf("Cake")),
            state.titles(),
        )
    }

    @Test
    fun noGroupingIsOneGroupWithoutHeading() {
        val state = state(grouping = ShoppingListGrouping.NONE, doneLast = false)

        assertNull(state.groups.single().title)
        assertEquals(listOf("Apple", "Bananas", "Bread", "Cake"), state.titles().single())
    }

    @Test
    fun itemsWithoutStoreAreNotSetWhenGroupedByStore() {
        val state = state(grouping = ShoppingListGrouping.DEFAULT_STORE, doneLast = false)

        assertEquals(
            listOf(text("Market"), WearShoppingGroupTitle.NotSet),
            state.groups.map { it.title },
        )
    }

    @Test
    fun withoutPriceTrackingTheStoreGroupingIsNotOffered() {
        val state = state(
            grouping = ShoppingListGrouping.DEFAULT_STORE,
            config = ServerConfig(stockPriceTracking = false),
        )

        assertEquals(ShoppingListGrouping.NONE, state.grouping)
        assertEquals(
            listOf(ShoppingListGrouping.NONE, ShoppingListGrouping.PRODUCT_GROUP),
            state.availableGroupings,
        )
    }

    @Test
    fun theStatusFilterShowsOnlyItsItemsAndKeepsTheCounts() {
        val done = state(status = ShoppingListStatus.DONE)
        val undone = state(status = ShoppingListStatus.UNDONE)

        assertEquals(listOf("Apple"), done.titles().flatten())
        assertEquals(listOf("Bread", "Bananas", "Cake"), undone.titles().flatten())
        assertEquals(1, undone.doneCount)
        assertEquals(3, done.undoneCount)
    }

    @Test
    fun rowsNameTheProductWithAmountUnitAndNote() {
        val state = state(
            items = listOf(
                item(1, productId = 1, note = "ripe", amount = 2.0),
                item(2, productId = null, note = "Stamps", amount = 0.0, quId = null),
                item(3, productId = 3, amount = 1.0),
            ),
            grouping = ShoppingListGrouping.NONE,
            syncStates = mapOf(3 to ShoppingListItemSync.SENDING),
        )

        val rows = state.groups.single().items
        assertEquals(listOf("Bananas", "Bread", "Stamps"), rows.map { it.title })
        assertEquals("2 Pieces · ripe", rows[0].details)
        assertEquals("1 Piece", rows[1].details)
        assertEquals(ShoppingListItemSync.SENDING, rows[1].sync)
        assertNull(rows[2].details)
    }

    @Test
    fun theAmountIsShownInTheUnitOfTheItem() {
        val state = state(
            items = listOf(item(1, productId = 1, amount = 6.0, quId = 2)),
            grouping = ShoppingListGrouping.NONE,
            names = names.copy(
                conversions = listOf(
                    ResolvedQuantityUnitConversion(
                        productId = 1,
                        fromQuId = 1,
                        toQuId = 2,
                        factor = 1.0 / 6.0,
                    ),
                ),
            ),
        )

        assertEquals("1 Pack", state.groups.single().items.single().details)
    }

    @Test
    fun theUserSettingRoundsAmountsUp() {
        val state = state(
            items = listOf(item(1, productId = 1, amount = 1.5)),
            grouping = ShoppingListGrouping.NONE,
            userSettings = UserSettings(shoppingListRoundUp = true),
        )

        assertEquals("2 Pieces", state.groups.single().items.single().details)
    }

    @Test
    fun itShowsTheChosenListOtherwiseGrocysFirst() {
        val lists = listOf(ShoppingList(2, "Another"), ShoppingList(1, "Shop"))

        assertEquals(1, state(lists = lists).listId)
        assertEquals(2, state(lists = lists, selected = 2).listId)
        assertEquals(1, state(lists = lists, selected = 9).listId)
    }

    @Test
    fun withoutMultipleListsOnlyTheFirstIsOffered() {
        val state = state(
            lists = listOf(ShoppingList(1, "Shop"), ShoppingList(2, "Another")),
            selected = 2,
            config = ServerConfig(shoppingListMultipleLists = false),
        )

        assertEquals(1, state.listId)
        assertEquals(listOf(1), state.lists.map { it.id })
    }

    @Test
    fun onlyItemsOfTheShownListCount() {
        val state = state(items = items + item(9, productId = 1, listId = 2))

        assertEquals(1, state.doneCount)
        assertEquals(3, state.undoneCount)
    }

    @Test
    fun theIntervalsKeepOneSetElsewhereInOrder() {
        assertEquals(listOf(5, 7, 10, 15, 30, 60), wearSyncIntervals(7))
        assertEquals(listOf(5, 10, 15, 30, 60), wearSyncIntervals(10))
    }

    @Test
    fun theTileSaysWhatIsLeftAndWhatComesNext() {
        val summary = wearTileSummary(state())

        assertEquals(
            WearTileSummary(
                listName = "Shop",
                total = 4,
                left = 3,
                nextItems = listOf("Bread", "Bananas", "Cake"),
            ),
            summary,
        )
    }

    @Test
    fun theTileNamesAtMostThreeItemsAndSkipsUnknownOnes() {
        val many = (1..5).map { item(it, productId = 1) } + item(6, productId = 999)

        val summary = wearTileSummary(state(items = many))

        assertEquals(3, summary.nextItems.size)
        assertEquals(6, summary.left)
    }

    @Test
    fun theTileOfADoneListNamesNothing() {
        val summary = wearTileSummary(state(items = listOf(item(1, productId = 1, done = true))))

        assertEquals(0, summary.left)
        assertEquals(1, summary.total)
        assertEquals(emptyList(), summary.nextItems)
    }
}
