package com.patrickzedler.grocy.core.data.masterdata

import com.patrickzedler.grocy.core.database.createInMemoryGrocyDatabase
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.network.GrocyApiException
import com.patrickzedler.grocy.core.testing.FakeGrocyServer
import com.patrickzedler.grocy.core.testing.MasterDataJson
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class MasterDataRepositoryTest {

    private val server = FakeGrocyServer()
    private val database = createInMemoryGrocyDatabase()
    private var connection: ServerConnection? = ServerConnection.ApiKey("https://grocy.test", "k")
    private val repository = MasterDataRepository(
        api = server.api,
        database = database,
        connection = { connection },
        random = Random(seed = 1),
    )

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun refreshStoresEverythingTheProductScreensShow() = runTest {
        repository.refresh()

        assertEquals(
            listOf("Bread", "Milk", "Old cheese", "Yoghurt"),
            repository.products.first().map { it.name },
        )
        assertEquals(listOf("Dairy", "Unused"), repository.productGroups.first().map { it.name })
        assertEquals(listOf(1), repository.stock.first().map { it.productId })
        assertEquals("4006381333931", repository.productBarcodes.first().single().barcode)
        assertEquals("brand", repository.userfields("products").first().single().name)
        assertEquals(2, repository.userSettings.first().stockDecimalPlacesAmounts)
        assertEquals(2, repository.userSettings.first().productPresets.locationId)
    }

    @Test
    fun configurationHasGrocysDefaultsBeforeTheFirstDownload() = runTest {
        assertTrue(repository.serverConfig.first().stockLocationTracking)
        assertEquals(4, repository.userSettings.first().stockDecimalPlacesAmounts)
    }

    @Test
    fun offlineRefreshKeepsTheLocalData() = runTest {
        repository.refresh()
        server.unreachable = true

        assertFailsWith<GrocyApiException.Unreachable> { repository.refresh() }

        assertEquals(4, repository.products.first().size)
    }

    @Test
    fun refreshReplacesRemovedObjects() = runTest {
        repository.refresh()
        server.respond("/api/objects/products", body = "[]")

        repository.refresh()

        assertTrue(repository.products.first().isEmpty())
    }

    @Test
    fun newProductIsCreatedAndStoredAsTheServerSavedIt() = runTest {
        val product = Product(id = MasterDataRepository.NEW_PRODUCT_ID, name = "X")

        val id = repository.saveProduct(product)

        assertEquals(MasterDataJson.CREATED_ID, id)
        assertEquals(1, server.requestsTo("/api/objects/products", HttpMethod.Post).size)
        assertEquals("Created", repository.product(id).first()?.name)
    }

    @Test
    fun editedProductIsPutAndChangedUserfieldsFollow() = runTest {
        server.respond("/api/objects/products/1", body = productJson(id = 1, name = "Milk"))

        repository.saveProduct(Product(id = 1, name = "Milk"), userfields = mapOf("brand" to "B"))

        assertEquals(1, server.requestsTo("/api/objects/products/1", HttpMethod.Put).size)
        val userfields = server.requestsTo("/api/userfields/products/1", HttpMethod.Put).single()
        assertEquals("B", userfields.jsonBody["brand"]?.jsonPrimitive?.content)
    }

    @Test
    fun untouchedUserfieldsAreNotSent() = runTest {
        server.respond("/api/objects/products/1", body = productJson(id = 1, name = "Milk"))

        repository.saveProduct(Product(id = 1, name = "Milk"))

        assertTrue(server.requestsTo("/api/userfields/products/1", HttpMethod.Put).isEmpty())
    }

    @Test
    fun replacedPictureGetsANewNameAndTheOldOneIsRemovedAfterwards() = runTest {
        server.respond("/api/objects/products/2", body = productJson(id = 2, name = "Bread"))
        val product = Product(id = 2, name = "Bread", pictureFileName = "x1bread.jpg")

        val picture = PictureChange.Replace(byteArrayOf(1), "Brot ä.JPG")

        repository.saveProduct(product, picture = picture)

        val put = server.requestsTo("/api/objects/products/2", HttpMethod.Put).single()
        val newName = put.jsonBody["picture_file_name"]!!.jsonPrimitive.content
        assertTrue(newName.endsWith("brotae.jpg"), newName)
        val methods = server.requests.map { it.method to it.url.encodedPath }
        val upload = HttpMethod.Put to "/api/files/productpictures/${newName.base64()}"
        val deletion = HttpMethod.Delete to "/api/files/productpictures/${"x1bread.jpg".base64()}"
        assertTrue(methods.indexOf(upload) in 0 until methods.indexOf(deletion))
    }

    @Test
    fun deletedPictureIsClearedOnTheProduct() = runTest {
        server.respond("/api/objects/products/2", body = productJson(id = 2, name = "Bread"))
        val product = Product(id = 2, name = "Bread", pictureFileName = "x1bread.jpg")

        repository.saveProduct(product, picture = PictureChange.Delete)

        val put = server.requestsTo("/api/objects/products/2", HttpMethod.Put).single()
        assertEquals("null", put.jsonBody["picture_file_name"].toString())
    }

    @Test
    fun rejectedProductLeavesTheLocalDataUnchanged() = runTest {
        repository.refresh()
        server.respond(
            path = "/api/objects/products",
            status = HttpStatusCode.BadRequest,
            body = """{"error_message":"UNIQUE constraint failed: products.name"}""",
            method = HttpMethod.Post,
        )

        val error = assertFailsWith<GrocyApiException.HttpError> {
            repository.saveProduct(Product(id = 0, name = "Milk"))
        }

        assertEquals("UNIQUE constraint failed: products.name", error.serverMessage)
        assertEquals(4, repository.products.first().size)
    }

    @Test
    fun deletedProductLeavesTheLocalCopy() = runTest {
        repository.refresh()
        server.respond("/api/objects/products/2", status = HttpStatusCode.NoContent)

        repository.deleteProduct(2)

        assertNull(repository.product(2).first())
        assertEquals(1, server.requestsTo("/api/objects/products/2", HttpMethod.Delete).size)
    }

    @Test
    fun clearForgetsEverything() = runTest {
        repository.refresh()

        repository.clear()

        assertTrue(repository.products.first().isEmpty())
    }

    @Test
    fun withoutLoginNothingIsRequested() = runTest {
        connection = null

        assertFailsWith<GrocyApiException.Unauthorized> { repository.refresh() }
        assertTrue(server.requests.isEmpty())
    }

    @Test
    fun fileNamesAreCleanedLikeGrocyDoes() {
        assertEquals("kaesebroetchen.png", cleanFileName("Käse Brötchen.PNG"))
        assertEquals("picture", cleanFileName("日本"))
        assertFalse("/" in cleanFileName("../../etc/passwd"))
    }

    private val io.ktor.client.request.HttpRequestData.jsonBody
        get() = Json.parseToJsonElement((body as TextContent).text).jsonObject

    @OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
    private fun String.base64() = kotlin.io.encoding.Base64.encode(encodeToByteArray())

    private fun productJson(id: Int, name: String) =
        """{"id":$id,"name":"$name","active":1,"qu_id_stock":1}"""
}
