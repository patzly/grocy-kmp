package com.patrickzedler.grocy.core.database

import app.cash.turbine.test
import com.patrickzedler.grocy.core.model.masterdata.DueType
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.StockLabelType
import com.patrickzedler.grocy.core.model.masterdata.Userfield
import com.patrickzedler.grocy.core.model.masterdata.UserfieldType
import androidx.room3.withWriteTransaction
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class MasterDataDaoTest {

    private val database = createInMemoryGrocyDatabase()
    private val dao = database.masterDataDao()

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun productKeepsEveryPropertyThroughTheDatabase() = runTest {
        val product = Product(
            id = 4,
            name = "Milk",
            description = "<p>Fresh</p>",
            parentProductId = 2,
            locationId = 3,
            minStockAmount = 1.5,
            dueType = DueType.EXPIRATION,
            defaultDueDays = -1,
            calories = 64.0,
            defaultStockLabelType = StockLabelType.PER_UNIT,
            pictureFileName = "ab_milk.jpg",
            userfields = mapOf("brand" to "Acme", "note" to null),
        )

        dao.upsertProduct(product.toEntity())

        assertEquals(product, dao.product(4)?.toModel())
    }

    @Test
    fun productsAreSortedByNameIgnoringCase() = runTest {
        dao.insertProducts(
            listOf("banana", "Apple", "cherry").mapIndexed { index, name ->
                Product(id = index + 1, name = name).toEntity()
            },
        )

        val names = dao.observeProducts().first().map { it.name }

        assertEquals(listOf("Apple", "banana", "cherry"), names)
    }

    @Test
    fun replacingInATransactionEmitsOnlyTheNewList() = runTest {
        dao.insertProducts(listOf(Product(id = 1, name = "Old").toEntity()))

        dao.observeProducts().test {
            assertEquals(listOf("Old"), awaitItem().map { it.name })

            database.withWriteTransaction {
                dao.deleteProducts()
                dao.insertProducts(listOf(Product(id = 2, name = "New").toEntity()))
            }

            assertEquals(listOf("New"), awaitItem().map { it.name })
        }
    }

    @Test
    fun userfieldsAreSortedByNumberThenNameAndKeepTheirOptions() = runTest {
        dao.insertUserfields(
            listOf(
                userfield(id = 1, name = "zeta", sortNumber = null),
                userfield(id = 2, name = "beta", sortNumber = 2),
                userfield(id = 3, name = "alpha", sortNumber = 2),
                userfield(id = 4, name = "first", sortNumber = 1),
            ).map { it.toEntity() },
        )

        val fields = dao.observeUserfields("products").first().map { it.toModel() }

        assertEquals(listOf("first", "alpha", "beta", "zeta"), fields.map { it.name })
        assertEquals(listOf("A", "B"), fields.first().options)
    }

    @Test
    fun overviewsOfRemovedProductsAreDeleted() = runTest {
        dao.insertProducts(listOf(Product(id = 1, name = "Milk").toEntity()))
        dao.upsertProductOverview(ProductOverviewEntity(productId = 1, json = "{}"))
        dao.upsertProductOverview(ProductOverviewEntity(productId = 2, json = "{}"))

        dao.deleteUnusedProductOverviews()

        assertEquals("{}", dao.observeProductOverview(1).first()?.json)
        assertNull(dao.observeProductOverview(2).first())
    }

    private fun userfield(id: Int, name: String, sortNumber: Int?) = Userfield(
        id = id,
        entity = "products",
        name = name,
        caption = name,
        type = UserfieldType.PRESET_LIST,
        options = listOf("A", "B"),
        sortNumber = sortNumber,
    )
}
