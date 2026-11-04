package com.patrickzedler.grocy.core.network

import com.patrickzedler.grocy.core.model.ProductFactsSource
import com.patrickzedler.grocy.core.model.productfacts.ProductFacts
import com.patrickzedler.grocy.core.model.productfacts.ProductNutrient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class ProductFactsApiTest {

    private val requests = mutableListOf<HttpRequestData>()

    private fun api(
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = "{}",
    ) = ProductFactsApi(
        httpClient = createGrocyHttpClient(
            MockEngine { request ->
                requests += request
                respond(
                    body,
                    status,
                    headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        ),
        userAgent = "Grocy/test",
    )

    @Test
    fun readsWhatTheDatabaseKnowsInTheLanguageOfTheApp() = runTest {
        val facts = api(body = SPREAD).product(ProductFactsSource.FOOD, "3017620422003", "de-DE")

        assertEquals(
            ProductFacts(
                source = ProductFactsSource.FOOD,
                barcode = "3017620422003",
                name = "Nuss-Nougat-Creme",
                genericName = "Brotaufstrich",
                brands = "Ferrero",
                quantity = "400 g",
                packageAmount = 400.0,
                packageUnit = "g",
                energyKcalPer100 = 539.0,
                nutrition = mapOf(ProductNutrient.FAT to 30.9, ProductNutrient.SUGARS to 56.3),
                ingredients = "Zucker, Palmöl",
                categories = listOf("Snacks", "Goûter"),
                stores = listOf("Rewe", "Edeka"),
                imageUrl = "https://images.openfoodfacts.org/images/front.jpg",
            ),
            facts,
        )
    }

    @Test
    fun asksTheDatabaseOfTheSourceAndIdentifiesTheApp() = runTest {
        api(body = SPREAD).product(ProductFactsSource.BEAUTY, "123", "en")

        val request = requests.single()
        assertEquals("world.openbeautyfacts.org", request.url.host)
        assertEquals("/api/v2/product/123.json", request.url.encodedPath)
        assertEquals("Grocy/test", request.headers[HttpHeaders.UserAgent])
        assertTrue("product_name_en" in request.url.parameters["fields"].orEmpty())
    }

    @Test
    fun sendsNoCredentials() = runTest {
        api(body = SPREAD).product(ProductFactsSource.FOOD, "123", "en")

        val request = requests.single()
        assertNull(request.headers["GROCY-API-KEY"])
        assertNull(request.url.parameters["GROCY-API-KEY"])
    }

    @Test
    fun anUnknownBarcodeIsNull() = runTest {
        assertNull(
            api(status = HttpStatusCode.NotFound)
                .product(ProductFactsSource.FOOD, "123", "en"),
        )
        assertNull(
            api(body = """{"code":"123","status":0}""")
                .product(ProductFactsSource.FOOD, "123", "en"),
        )
        assertNull(
            api(body = """{"code":"123","status":1,"product":{"quantity":"1"}}""")
                .product(ProductFactsSource.FOOD, "123", "en"),
        )
    }

    @Test
    fun aServerProblemIsAnError() = runTest {
        assertFailsWith<ProductFactsException> {
            api(status = HttpStatusCode.ServiceUnavailable)
                .product(ProductFactsSource.FOOD, "123", "en")
        }
    }

    @Test
    fun energyInKilojoulesAloneIsConverted() = runTest {
        val facts = api(
            body = """{"product":{"product_name":"X","nutriments":{"energy-kj_100g":418.4}}}""",
        ).product(ProductFactsSource.FOOD, "123", "en")

        assertEquals(100.0, facts?.energyKcalPer100 ?: 0.0, absoluteTolerance = 0.001)
    }

    @Test
    fun picturesComeOnlyFromTheDatabaseItself() {
        val source = ProductFactsSource.FOOD

        assertTrue(isPictureOf(source, "https://images.openfoodfacts.org/images/a.jpg"))
        assertFalse(isPictureOf(source, "http://images.openfoodfacts.org/images/a.jpg"))
        assertFalse(isPictureOf(source, "https://evil.example/a.jpg"))
        assertFalse(isPictureOf(source, "https://openfoodfacts.org.evil.example/a.jpg"))
        assertFalse(isPictureOf(source, "https://images.openbeautyfacts.org/a.jpg"))
    }

    @Test
    fun aPictureFromElsewhereIsNotDownloaded() = runTest {
        val facts = ProductFacts(
            source = ProductFactsSource.FOOD,
            barcode = "123",
            imageUrl = "http://192.168.1.1/secret",
        )

        assertNull(api().picture(facts))
        assertTrue(requests.isEmpty())
    }

    private companion object {
        val SPREAD = """
            {"code":"3017620422003","status":1,"product":{
              "product_name":"Nutella","product_name_de":"Nuss-Nougat-Creme",
              "generic_name":"Spread","generic_name_de":"Brotaufstrich","brands":"Ferrero",
              "quantity":"400 g","product_quantity":"400","product_quantity_unit":"g",
              "ingredients_text":"sugar","ingredients_text_de":"Zucker, Palmöl",
              "categories":"Snacks, fr:Goûter","stores":"Rewe,Edeka",
              "image_front_url":"https://images.openfoodfacts.org/images/front.jpg",
              "nutriments":{"energy-kcal_100g":539,"fat_100g":30.9,"sugars_100g":"56.3"}
            }}
        """.trimIndent()
    }
}
