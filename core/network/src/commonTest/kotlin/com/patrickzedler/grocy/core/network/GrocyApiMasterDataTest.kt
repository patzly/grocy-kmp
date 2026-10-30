package com.patrickzedler.grocy.core.network

import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.masterdata.DueType
import com.patrickzedler.grocy.core.model.masterdata.PriceHistoryEntry
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.ProductOverview
import com.patrickzedler.grocy.core.model.masterdata.UserfieldType
import com.patrickzedler.grocy.core.model.server.ProductPresets
import com.patrickzedler.grocy.core.model.server.UserSettingValue
import com.patrickzedler.grocy.core.model.server.UserSettings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class GrocyApiMasterDataTest {

    private val requests = mutableListOf<HttpRequestData>()

    private fun api(
        apiKeyAsQueryParameter: Boolean = false,
        handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) = GrocyApi(
        httpClient = createGrocyHttpClient(
            MockEngine { request ->
                requests += request
                handler(request)
            },
        ),
        ingressSessionSource = { error("No ingress in these tests") },
        apiKeyAsQueryParameter = apiKeyAsQueryParameter,
    )

    private fun MockRequestHandleScope.json(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) = respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    private val HttpRequestData.json
        get() = Json.parseToJsonElement((body as TextContent).text).jsonObject

    @Test
    fun productsAcceptNumbersAsStringsAndEmptyStringsAsNull() = runTest {
        val api = api {
            json(
                """[{"id":"3","name":"Milk","active":"1","location_id":"2",""" +
                    """"min_stock_amount":"1.5",""" +
                    """"due_type":2,"parent_product_id":"","calories":null,"qu_id_stock":4,""" +
                    """"treat_opened_as_out_of_stock":0,""" +
                    """"userfields":{"brand":"Acme","note":null}}]""",
            )
        }

        val product = api.products(CONNECTION).single()

        assertEquals(3, product.id)
        assertEquals(2, product.locationId)
        assertEquals(1.5, product.minStockAmount)
        assertEquals(DueType.EXPIRATION, product.dueType)
        assertNull(product.parentProductId)
        assertNull(product.calories)
        assertEquals(4, product.quIdStock)
        assertFalse(product.treatOpenedAsOutOfStock)
        assertEquals(mapOf("brand" to "Acme", "note" to null), product.userfields)
    }

    @Test
    fun newProductIsPostedAsExactJson() = runTest {
        val api = api { json("""{"created_object_id":"12"}""") }

        val id = api.addProduct(CONNECTION, Product(id = 0, name = "Bread", quIdStock = 2))

        val request = requests.single()
        assertEquals(12, id)
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/api/objects/products", request.url.encodedPath)
        // Grocy rejects "application/json; charset=UTF-8"
        assertEquals("application/json", request.body.contentType.toString())
        assertEquals("Bread", request.json["name"]?.jsonPrimitive?.content)
        assertEquals(2, request.json["qu_id_stock"]?.jsonPrimitive?.int)
        assertEquals(1, request.json["active"]?.jsonPrimitive?.int)
        assertFalse("id" in request.json)
    }

    @Test
    fun editedProductIsPutToItsPath() = runTest {
        val api = api { json("", HttpStatusCode.NoContent) }

        api.editProduct(CONNECTION, Product(id = 7, name = "Bread"))

        assertEquals(HttpMethod.Put, requests.single().method)
        assertEquals("/api/objects/products/7", requests.single().url.encodedPath)
    }

    @Test
    fun writesSendTheApiKeyAsQueryParameterInBrowsers() = runTest {
        val api = api(apiKeyAsQueryParameter = true) { json("", HttpStatusCode.NoContent) }

        api.deleteObject(CONNECTION, "products", 7)

        assertEquals("key", requests.single().url.parameters[GrocyApi.API_KEY_HEADER])
        assertEquals(HttpMethod.Delete, requests.single().method)
    }

    @Test
    fun rejectedWriteCarriesGrocysMessage() = runTest {
        val api = api {
            json(
                """{"error_message":"UNIQUE constraint failed: products.name"}""",
                HttpStatusCode.BadRequest,
            )
        }

        val error = assertFailsWith<GrocyApiException.HttpError> {
            api.addProduct(CONNECTION, Product(id = 0, name = "Milk"))
        }

        assertEquals(400, error.statusCode)
        assertEquals("UNIQUE constraint failed: products.name", error.serverMessage)
    }

    @Test
    fun serverConfigReadsFeatureFlagsInEveryForm() = runTest {
        val api = api {
            json(
                """{"FEATURE_FLAG_STOCK_LOCATION_TRACKING":false,""" +
                    """"FEATURE_FLAG_STOCK_PRICE_TRACKING":"0",""" +
                    """"FEATURE_FLAG_LABEL_PRINTER":"1",""" +
                    """"FEATURE_FLAG_DISABLE_BROWSER_BARCODE_CAMERA_SCANNING":true,""" +
                    """"ENERGY_UNIT":"kJ","LABEL_PRINTER_WEBHOOK":"https://secret.example"}""",
            )
        }

        val config = api.serverConfig(CONNECTION)

        assertFalse(config.stockLocationTracking)
        assertFalse(config.stockPriceTracking)
        assertTrue(config.labelPrinter)
        assertFalse(config.browserBarcodeCameraScanning)
        assertEquals("kJ", config.energyUnit)
        assertEquals("USD", config.currency)
        // Missing flags keep Grocy's defaults
        assertTrue(config.stockProductFreezing)
        assertTrue(config.shoppingListMultipleLists)
        assertTrue(config.labelPrinterRunsOnServer)
        assertTrue(config.grocycode2D)
    }

    @Test
    fun serverConfigReadsGrocycodesAndShoppingListsAndLabelPrinting() = runTest {
        val api = api {
            json(
                """{"FEATURE_FLAG_SHOPPINGLIST_MULTIPLE_LISTS":"0",""" +
                    """"LABEL_PRINTER_RUN_SERVER":false,"GROCYCODE_TYPE":"1d"}""",
            )
        }

        val config = api.serverConfig(CONNECTION)

        assertFalse(config.shoppingListMultipleLists)
        assertFalse(config.labelPrinterRunsOnServer)
        assertFalse(config.grocycode2D)
    }

    @Test
    fun userSettingsTurnMinusOneIntoNoPreset() = runTest {
        val api = api {
            json(
                """{"stock_decimal_places_amounts":"2","product_presets_location_id":"-1",""" +
                    """"product_presets_qu_id":3,"product_presets_default_due_days":"-1",""" +
                    """"product_presets_treat_opened_as_out_of_stock":"0"}""",
            )
        }

        val settings = api.userSettings(CONNECTION)

        assertEquals(2, settings.stockDecimalPlacesAmounts)
        // Missing settings keep Grocy's defaults
        assertEquals(2, settings.stockDecimalPlacesPricesDisplay)
        assertEquals(
            ProductPresets(
                locationId = null,
                quantityUnitId = 3,
                defaultDueDays = -1,
                treatOpenedAsOutOfStock = false,
            ),
            settings.productPresets,
        )
    }

    @Test
    fun userSettingsReadTheStockSettings() = runTest {
        val api = api {
            json(
                """{"stock_due_soon_days":"7","stock_default_purchase_amount":"0.5",""" +
                    """"stock_default_consume_amount":2,""" +
                    """"stock_default_consume_amount_use_quick_consume_amount":"1",""" +
                    """"show_icon_on_stock_overview_page_when_product_is_on_shopping_list":0,""" +
                    """"stock_overview_show_all_out_of_stock_products":true,""" +
                    """"show_purchased_date_on_purchase":"1",""" +
                    """"show_warning_on_purchase_when_due_date_is_earlier_than_next":"0",""" +
                    """"stock_decimal_places_prices_input":"3",""" +
                    """"stock_auto_decimal_separator_prices":"1"}""",
            )
        }

        val settings = api.userSettings(CONNECTION)

        assertEquals(7, settings.stockDueSoonDays)
        assertEquals(0.5, settings.stockDefaultPurchaseAmount)
        assertEquals(2.0, settings.stockDefaultConsumeAmount)
        assertTrue(settings.stockDefaultConsumeAmountUseQuickConsumeAmount)
        assertFalse(settings.showIconOnStockOverviewWhenOnShoppingList)
        assertTrue(settings.stockOverviewShowAllOutOfStockProducts)
        assertTrue(settings.showPurchasedDateOnPurchase)
        assertFalse(settings.showWarningOnPurchaseWhenDueDateIsEarlierThanNext)
        assertEquals(3, settings.stockDecimalPlacesPricesInput)
        assertTrue(settings.stockAutoDecimalSeparatorPrices)
    }

    @Test
    fun userSettingsReadTheSettingsOfGrocysOtherPages() = runTest {
        val api = api {
            json(
                """{"shopping_list_show_calendar":"1","shopping_list_round_up":true,""" +
                    """"shopping_list_auto_add_below_min_stock_amount":"1",""" +
                    """"shopping_list_auto_add_below_min_stock_amount_list_id":"3",""" +
                    """"shopping_list_to_stock_workflow_auto_submit_when_prefilled":1,""" +
                    """"recipes_show_list_side_by_side":"0",""" +
                    """"recipes_show_ingredient_checkbox":"1",""" +
                    """"recipe_ingredients_group_by_product_group":"1",""" +
                    """"chores_due_soon_days":"0","chores_overview_swap_tracking_buttons":"1",""" +
                    """"tasks_due_soon_days":"3","batteries_due_soon_days":9,"locale":"de",""" +
                    """"scan_mode_purchase_enabled":"1","scan_mode_consume_enabled":true}""",
            )
        }

        val settings = api.userSettings(CONNECTION)

        assertTrue(settings.shoppingListShowCalendar)
        assertTrue(settings.shoppingListRoundUp)
        assertTrue(settings.shoppingListAutoAddBelowMinStock)
        assertEquals(3, settings.shoppingListAutoAddListId)
        assertTrue(settings.shoppingListToStockAutoSubmit)
        assertFalse(settings.recipesShowListSideBySide)
        assertTrue(settings.recipesShowIngredientCheckbox)
        assertTrue(settings.recipeIngredientsGroupByProductGroup)
        assertEquals(0, settings.choresDueSoonDays)
        assertTrue(settings.choresOverviewSwapTrackingButtons)
        assertEquals(3, settings.tasksDueSoonDays)
        assertEquals(9, settings.batteriesDueSoonDays)
        assertEquals("de", settings.locale)
        assertTrue(settings.scanModePurchase)
        assertTrue(settings.scanModeConsume)
    }

    @Test
    fun missingSettingsOfGrocysOtherPagesKeepItsDefaults() = runTest {
        val api = api { json("""{"locale":null}""") }

        val settings = api.userSettings(CONNECTION)

        assertEquals(UserSettings(), settings)
    }

    @Test
    fun textSettingsAreSentAsText() = runTest {
        val api = api { json("", HttpStatusCode.NoContent) }

        api.saveUserSetting(CONNECTION, "locale", UserSettingValue.Text("pt_BR"))

        val request = requests.single()
        assertEquals("/api/user/settings/locale", request.url.encodedPath)
        assertEquals("pt_BR", request.json["value"]?.jsonPrimitive?.content)
    }

    @Test
    fun userfieldsAreFilteredByEntityAndSplitTheirOptions() = runTest {
        val api = api {
            json(
                """[{"id":1,"entity":"products","name":"origin","caption":"Origin",""" +
                    """"type":"preset-list","config":"DE\nCH\n\nAT",""" +
                    """"show_as_column_in_tables":"1"}]""",
            )
        }

        val field = api.userfields(CONNECTION, "products").single()

        assertEquals("entity=products", requests.single().url.parameters["query[]"])
        assertEquals(UserfieldType.PRESET_LIST, field.type)
        assertEquals(listOf("DE", "CH", "AT"), field.options)
        assertTrue(field.showAsColumnInTables)
    }

    @Test
    fun userfieldValuesArePutByName() = runTest {
        val api = api { json("", HttpStatusCode.NoContent) }

        api.setUserfields(CONNECTION, "products", 7, mapOf("origin" to "CH", "note" to null))

        val request = requests.single()
        assertEquals("/api/userfields/products/7", request.url.encodedPath)
        assertEquals("CH", request.json["origin"]?.jsonPrimitive?.content)
    }

    @Test
    fun picturesAreUploadedAsBytesUnderTheirBase64Name() = runTest {
        val api = api { json("", HttpStatusCode.NoContent) }

        api.uploadProductPicture(CONNECTION, "ab12milk.jpg", byteArrayOf(1, 2, 3))

        val request = requests.single()
        assertEquals("/api/files/productpictures/YWIxMm1pbGsuanBn", request.url.encodedPath)
        assertEquals("application/octet-stream", request.body.contentType.toString())
        assertEquals(listOf<Byte>(1, 2, 3), (request.body as ByteArrayContent).bytes().toList())
    }

    @Test
    fun stockFallsBackToTheOwnAmount() = runTest {
        val api = api { json("""[{"product_id":"5","amount":"2"}]""") }

        val stock = api.stock(CONNECTION).single()

        assertEquals(5, stock.productId)
        assertEquals(2.0, stock.amountAggregated)
    }

    @Test
    fun currencyAndPriceDecimalsAreRead() = runTest {
        val configApi = api { json("""{"CURRENCY":" chf "}""") }
        val settingsApi = api { json("""{"stock_decimal_places_prices_display":"3"}""") }

        assertEquals("CHF", configApi.serverConfig(CONNECTION).currency)
        assertEquals(3, settingsApi.userSettings(CONNECTION).stockDecimalPlacesPricesDisplay)
    }

    @Test
    fun invalidCurrencyFallsBackToGrocysDefault() = runTest {
        val api = api { json("""{"CURRENCY":"Euro"}""") }

        assertEquals("USD", api.serverConfig(CONNECTION).currency)
    }

    @Test
    fun productOverviewCombinesDetailsAndPriceHistory() = runTest {
        val api = api { request ->
            if (request.url.encodedPath.endsWith("price-history")) {
                json(
                    """[{"date":"2026-10-01 00:00:00","price":"1.5",""" +
                        """"shopping_location":{"id":1,"name":"Supermarket"}},""" +
                        """{"date":"2026-09-01","price":1.2,"shopping_location":null},""" +
                        """{"date":"","price":1}]""",
                )
            } else {
                json(
                    """{"product":{"id":7,"name":"Milk"},"last_purchased":"2026-10-01",""" +
                        """"last_used":null,"stock_amount":"3","stock_amount_opened":"1",""" +
                        """"stock_amount_aggregated":"5","is_aggregated_amount":"1",""" +
                        """"stock_value":"4.5","last_price":"1.5","avg_price":null,""" +
                        """"qu_conversion_factor_price_to_stock":"6",""" +
                        """"average_shelf_life_days":"-1","spoil_rate_percent":"12.5"}""",
                )
            }
        }

        val overview = api.productOverview(CONNECTION, productId = 7, withPriceHistory = true)

        assertEquals(
            setOf("/api/stock/products/7", "/api/stock/products/7/price-history"),
            requests.map { it.url.encodedPath }.toSet(),
        )
        assertEquals(
            ProductOverview(
                productId = 7,
                stockAmount = 3.0,
                stockAmountOpened = 1.0,
                stockAmountAggregated = 5.0,
                isAggregatedAmount = true,
                stockValue = 4.5,
                lastPurchased = "2026-10-01",
                lastPrice = 1.5,
                priceToStockFactor = 6.0,
                spoilRatePercent = 12.5,
                priceHistory = listOf(
                    PriceHistoryEntry("2026-10-01", 1.5, storeId = 1),
                    PriceHistoryEntry("2026-09-01", 1.2),
                ),
            ),
            overview,
        )
    }

    @Test
    fun productOverviewWithoutPriceTrackingSkipsThePriceHistory() = runTest {
        val api = api { json("""{"stock_amount":0}""") }

        val overview = api.productOverview(CONNECTION, productId = 7, withPriceHistory = false)

        assertEquals(listOf("/api/stock/products/7"), requests.map { it.url.encodedPath })
        assertTrue(overview.priceHistory.isEmpty())
        assertEquals(ProductOverview.UNKNOWN_SHELF_LIFE, overview.averageShelfLifeDays)
    }

    private companion object {
        val CONNECTION = ServerConnection.ApiKey("https://grocy.example.com", "key")
    }
}
