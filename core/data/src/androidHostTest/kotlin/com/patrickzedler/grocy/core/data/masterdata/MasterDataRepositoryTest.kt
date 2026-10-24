package com.patrickzedler.grocy.core.data.masterdata

import com.patrickzedler.grocy.core.database.ProductOverviewEntity
import com.patrickzedler.grocy.core.database.ServerValueEntity
import com.patrickzedler.grocy.core.database.createInMemoryGrocyDatabase
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.SystemInfo
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.ProductOverview
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
    fun thumbnailIsStoredForOfflineUse() = runTest {
        repository.refresh()
        server.respond(breadPicture, body = "jpeg")
        repository.productThumbnail("x1bread.jpg")
        server.unreachable = true

        val thumbnail = repository.productThumbnail("x1bread.jpg")

        assertEquals("jpeg", thumbnail.decodeToString())
    }

    @Test
    fun missingThumbnailsAreDownloaded() = runTest {
        repository.refresh()
        server.respond(breadPicture, body = "jpeg")

        repository.downloadMissingThumbnails()
        repository.downloadMissingThumbnails()

        assertEquals(1, server.requestedPaths.count { it == breadPicture })
    }

    @Test
    fun thumbnailOfARemovedPictureIsDeleted() = runTest {
        repository.refresh()
        server.respond(breadPicture, body = "jpeg")
        repository.productThumbnail("x1bread.jpg")
        server.respond("/api/objects/products", body = "[]")

        repository.refresh()

        server.unreachable = true
        assertFailsWith<GrocyApiException.Unreachable> {
            repository.productThumbnail("x1bread.jpg")
        }
    }

    @Test
    fun configurationHasGrocysDefaultsBeforeTheFirstDownload() = runTest {
        assertTrue(repository.serverConfig.first().stockLocationTracking)
        assertEquals(4, repository.userSettings.first().stockDecimalPlacesAmounts)
    }

    @Test
    fun refreshStoresTheServersVersion() = runTest {
        repository.refresh()

        assertEquals("4.7.1", repository.systemInfo.first()?.grocyVersion)
    }

    @Test
    fun versionIsUnknownBeforeTheFirstDownload() = runTest {
        assertNull(repository.systemInfo.first())
    }

    @Test
    fun storedVersionOfEarlierInstallationsIsRead() = runTest {
        // Literal of the stored format, so existing installations keep reading it
        database.masterDataDao().upsertServerValue(
            ServerValueEntity(
                key = "system_info",
                json = """{"grocy_version":"4.5.0","grocy_release_date":"2025-04-01"}""",
            ),
        )

        assertEquals(
            SystemInfo(grocyVersion = "4.5.0", grocyReleaseDate = "2025-04-01"),
            repository.systemInfo.first(),
        )
    }

    @Test
    fun reloadConfigurationStoresConfigSettingsAndVersion() = runTest {
        server.respond(
            "/api/system/config",
            body = """{"FEATURE_FLAG_STOCK_PRICE_TRACKING":false}""",
        )
        server.respond("/api/user/settings", body = """{"stock_decimal_places_amounts":"1"}""")

        repository.reloadConfiguration()

        assertFalse(repository.serverConfig.first().stockPriceTracking)
        assertEquals(1, repository.userSettings.first().stockDecimalPlacesAmounts)
        assertEquals("4.7.1", repository.systemInfo.first()?.grocyVersion)
        assertTrue(server.requestedPaths.none { it.startsWith("/api/objects") })
    }

    @Test
    fun offlineReloadKeepsTheStoredConfiguration() = runTest {
        repository.refresh()
        server.unreachable = true

        assertFailsWith<GrocyApiException.Unreachable> { repository.reloadConfiguration() }

        assertEquals(2, repository.userSettings.first().stockDecimalPlacesAmounts)
        assertEquals("4.7.1", repository.systemInfo.first()?.grocyVersion)
    }

    @Test
    fun refreshedVersionReplacesTheStoredOne() = runTest {
        repository.refresh()
        server.respond(
            FakeGrocyServer.SYSTEM_INFO_PATH,
            body = """{"grocy_version":{"Version":"4.8.0","ReleaseDate":"2026-10-01"}}""",
        )

        repository.refreshSystemInfo()

        assertEquals("4.8.0", repository.systemInfo.first()?.grocyVersion)
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

        repository.deleteObject(MasterDataRepository.PRODUCTS, 2)

        assertNull(repository.product(2).first())
        assertEquals(1, server.requestsTo("/api/objects/products/2", HttpMethod.Delete).size)
    }

    @Test
    fun productOverviewIsStoredForOfflineUse() = runTest {
        repository.refreshProductOverview(1, withPriceHistory = true)
        server.unreachable = true

        assertFailsWith<GrocyApiException.Unreachable> {
            repository.refreshProductOverview(1, withPriceHistory = true)
        }

        val overview = repository.productOverview(1).first()
        assertEquals(3.0, overview?.stockAmount)
        assertEquals(listOf(1, null), overview?.priceHistory?.map { it.storeId })
    }

    @Test
    fun productOverviewIsUnknownBeforeTheFirstDownload() = runTest {
        assertNull(repository.productOverview(1).first())
    }

    @Test
    fun storedOverviewOfEarlierVersionsIsRead() = runTest {
        // Persisted format: later fields must have defaults
        database.masterDataDao().upsertProductOverview(
            ProductOverviewEntity(1, """{"productId":1,"stockAmount":2.0,"unknown":true}"""),
        )

        val overview = repository.productOverview(1).first()

        assertEquals(ProductOverview(productId = 1, stockAmount = 2.0), overview)
    }

    @Test
    fun overviewOfARemovedProductIsDeletedOnRefresh() = runTest {
        server.respond(
            "/api/objects/products",
            body = "[${productJson(1, "Milk")}]",
        )
        repository.refreshProductOverview(1, withPriceHistory = false)
        database.masterDataDao().upsertProductOverview(ProductOverviewEntity(2, "{}"))

        repository.refresh()

        assertEquals(3.0, repository.productOverview(1).first()?.stockAmount)
        assertNull(database.masterDataDao().observeProductOverview(2).first())
    }

    @Test
    fun offlinePictureFallsBackToTheStoredThumbnail() = runTest {
        repository.refresh()
        server.respond(breadPicture, body = "small")
        repository.productThumbnail("x1bread.jpg")
        server.unreachable = true

        val picture = repository.productPictureOrThumbnail("x1bread.jpg", maxWidth = 1000)

        assertEquals("small", picture.decodeToString())
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

    private val breadPicture = "/api/files/productpictures/${"x1bread.jpg".base64()}"

    private fun productJson(id: Int, name: String) =
        """{"id":$id,"name":"$name","active":1,"qu_id_stock":1}"""
}
