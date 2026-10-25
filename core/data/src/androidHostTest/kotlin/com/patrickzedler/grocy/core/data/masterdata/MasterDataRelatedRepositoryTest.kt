package com.patrickzedler.grocy.core.data.masterdata

import com.patrickzedler.grocy.core.database.createInMemoryGrocyDatabase
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.masterdata.ProductBarcode
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnitConversion
import com.patrickzedler.grocy.core.model.server.UserSettingValue
import com.patrickzedler.grocy.core.testing.FakeGrocyServer
import com.patrickzedler.grocy.core.testing.MasterDataJson
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

/** What belongs to products and units: barcodes, conversions, shopping lists, user settings. */
class MasterDataRelatedRepositoryTest {

    private val server = FakeGrocyServer()
    private val database = createInMemoryGrocyDatabase()
    private val repository = MasterDataRepository(
        api = server.api,
        database = database,
        connection = { ServerConnection.ApiKey("https://grocy.test", "k") },
    )

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun refreshStoresConversionsAndShoppingLists() = runTest {
        repository.refresh()

        val conversion = repository.quantityUnitConversions.first().single()
        assertEquals(6.0, conversion.factor)
        assertNull(conversion.productId)
        val resolved = repository.resolvedQuantityUnitConversions.first()
        assertEquals(5, resolved.size)
        assertEquals(4, resolved.count { it.productId == 1 })
        val lists = repository.shoppingLists.first().map { it.name }
        assertEquals(listOf("Hardware store", "Shopping list"), lists)
    }

    @Test
    fun newBarcodeIsCreatedWithItsUserfieldsAndStoredAsSaved() = runTest {
        repository.refresh()
        server.respond(
            "/api/objects/product_barcodes",
            body = """{"created_object_id":${MasterDataJson.CREATED_ID}}""",
            method = HttpMethod.Post,
        )
        server.respond(
            "/api/objects/product_barcodes/${MasterDataJson.CREATED_ID}",
            body = """{"id":${MasterDataJson.CREATED_ID},"product_id":1,"barcode":"777",""" +
                """"userfields":{"size":"XL"}}""",
        )

        val id = repository.saveProductBarcode(
            ProductBarcode(id = 0, productId = 1, barcode = "777"),
            userfields = mapOf("size" to "XL"),
        )

        assertEquals(MasterDataJson.CREATED_ID, id)
        val userfields = server.requestsTo(
            "/api/userfields/product_barcodes/${MasterDataJson.CREATED_ID}",
            HttpMethod.Put,
        )
        assertEquals("""{"size":"XL"}""", (userfields.single().body as TextContent).text)
        val stored = repository.productBarcodes.first().first { it.id == id }
        assertEquals("777", stored.barcode)
        assertEquals(mapOf("size" to "XL"), stored.userfields)
    }

    @Test
    fun savedConversionDownloadsTheDerivedConversionsAgain() = runTest {
        repository.refresh()
        server.respond(
            "/api/objects/quantity_unit_conversions/1",
            body = """{"id":1,"from_qu_id":2,"to_qu_id":1,"factor":"10"}""",
        )
        server.respond(
            "/api/objects/quantity_unit_conversions_resolved",
            body = """[{"product_id":null,"from_qu_id":2,"to_qu_id":1,"factor":10}]""",
        )

        repository.saveQuantityUnitConversion(
            QuantityUnitConversion(id = 1, fromQuId = 2, toQuId = 1, factor = 10.0),
            userfields = emptyMap(),
        )

        assertEquals(10.0, repository.quantityUnitConversions.first().single().factor)
        assertEquals(10.0, repository.resolvedQuantityUnitConversions.first().single().factor)
    }

    @Test
    fun deletedConversionIsRemovedWithItsDerivedConversions() = runTest {
        repository.refresh()
        server.respond("/api/objects/quantity_unit_conversions_resolved", body = "[]")

        repository.deleteObject(MasterDataRepository.QU_CONVERSIONS, 1)

        assertTrue(repository.quantityUnitConversions.first().isEmpty())
        assertTrue(repository.resolvedQuantityUnitConversions.first().isEmpty())
    }

    @Test
    fun deletedBarcodeIsRemovedLocally() = runTest {
        repository.refresh()

        repository.deleteObject(MasterDataRepository.PRODUCT_BARCODES, 1)

        assertTrue(repository.productBarcodes.first().isEmpty())
    }

    @Test
    fun savedUserSettingStoresTheServersSettings() = runTest {
        server.respond("/api/user/settings", body = """{"stock_due_soon_days":"7"}""")

        repository.saveUserSetting("stock_due_soon_days", UserSettingValue.Whole(7))

        val saved = server.requestsTo("/api/user/settings/stock_due_soon_days", HttpMethod.Put)
        assertEquals("""{"value":7}""", (saved.single().body as TextContent).text)
        assertEquals(7, repository.userSettings.first().stockDueSoonDays)
    }
}
