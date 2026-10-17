package com.patrickzedler.grocy.core.network

import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.SystemInfo
import com.patrickzedler.grocy.core.model.User
import com.patrickzedler.grocy.core.model.masterdata.Location
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.ProductBarcode
import com.patrickzedler.grocy.core.model.masterdata.ProductGroup
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.StockAmount
import com.patrickzedler.grocy.core.model.masterdata.Store
import com.patrickzedler.grocy.core.model.masterdata.Userfield
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.server.UserSettings
import com.patrickzedler.grocy.core.network.dto.CreatedObjectDto
import com.patrickzedler.grocy.core.network.dto.ErrorDto
import com.patrickzedler.grocy.core.network.dto.LocationDto
import com.patrickzedler.grocy.core.network.dto.ProductBarcodeDto
import com.patrickzedler.grocy.core.network.dto.ProductDto
import com.patrickzedler.grocy.core.network.dto.ProductGroupDto
import com.patrickzedler.grocy.core.network.dto.QuantityUnitDto
import com.patrickzedler.grocy.core.network.dto.StockDto
import com.patrickzedler.grocy.core.network.dto.StoreDto
import com.patrickzedler.grocy.core.network.dto.SystemInfoDto
import com.patrickzedler.grocy.core.network.dto.UserDto
import com.patrickzedler.grocy.core.network.dto.UserfieldDto
import com.patrickzedler.grocy.core.network.dto.toJson
import com.patrickzedler.grocy.core.network.dto.toServerConfig
import com.patrickzedler.grocy.core.network.dto.toUserSettings
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.appendPathSegments
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import io.ktor.util.encodeBase64
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * The Grocy REST API.
 *
 * [ingressSessionSource] creates Home Assistant ingress sessions, [apiKeyAsQueryParameter]
 * decides where the API key goes; both have platform defaults and are replaced in tests.
 */
class GrocyApi(
    private val httpClient: HttpClient,
    ingressSessionSource: IngressSessionSource = WebSocketIngressSessionSource(httpClient),
    private val apiKeyAsQueryParameter: Boolean = sendApiKeyAsQueryParameter,
) {

    private val ingress = HomeAssistantIngress(ingressSessionSource)

    suspend fun systemInfo(connection: ServerConnection): SystemInfo =
        get<SystemInfoDto>(connection, "system", "info").toModel()

    /** Grocy returns the current user as a list with exactly one entry. */
    suspend fun currentUser(connection: ServerConnection): User =
        get<List<UserDto>>(connection, "user").singleOrNull()?.toModel()
            ?: throw GrocyApiException.NotGrocyServer()

    /** Feature flags and the energy unit of the server. */
    suspend fun serverConfig(connection: ServerConnection): ServerConfig =
        get<JsonObject>(connection, "system", "config").toServerConfig()

    suspend fun userSettings(connection: ServerConnection): UserSettings =
        get<JsonObject>(connection, "user", "settings").toUserSettings()

    /** All products, disabled ones included, with their userfield values. */
    suspend fun products(connection: ServerConnection): List<Product> =
        get<List<ProductDto>>(connection, "objects", PRODUCTS).map { it.toModel() }

    suspend fun product(connection: ServerConnection, id: Int): Product =
        get<ProductDto>(connection, "objects", PRODUCTS, id.toString()).toModel()

    suspend fun productGroups(connection: ServerConnection): List<ProductGroup> =
        get<List<ProductGroupDto>>(connection, "objects", "product_groups").map { it.toModel() }

    suspend fun locations(connection: ServerConnection): List<Location> =
        get<List<LocationDto>>(connection, "objects", "locations").map { it.toModel() }

    suspend fun quantityUnits(connection: ServerConnection): List<QuantityUnit> =
        get<List<QuantityUnitDto>>(connection, "objects", "quantity_units").map { it.toModel() }

    suspend fun stores(connection: ServerConnection): List<Store> =
        get<List<StoreDto>>(connection, "objects", "shopping_locations").map { it.toModel() }

    suspend fun productBarcodes(connection: ServerConnection): List<ProductBarcode> =
        get<List<ProductBarcodeDto>>(connection, "objects", "product_barcodes")
            .map { it.toModel() }

    /** Current stock, one entry per product that is in stock. */
    suspend fun stock(connection: ServerConnection): List<StockAmount> =
        get<List<StockDto>>(connection, "stock").map { it.toModel() }

    /** Definitions of the userfields of [entity], e.g. "products". */
    suspend fun userfields(connection: ServerConnection, entity: String): List<Userfield> =
        get<List<UserfieldDto>>(
            connection,
            "objects",
            "userfields",
            query = listOf("query[]" to "entity=$entity"),
        ).map { it.toModel() }

    /** Creates [product] (its id is ignored) and returns the id Grocy gave it. */
    suspend fun addProduct(connection: ServerConnection, product: Product): Int {
        val response = execute(
            connection,
            HttpMethod.Post,
            listOf("objects", PRODUCTS),
            body = jsonBody(product.toJson()),
        )
        return decode<CreatedObjectDto>(response).createdObjectId
            ?: throw GrocyApiException.NotGrocyServer()
    }

    suspend fun editProduct(connection: ServerConnection, product: Product) {
        execute(
            connection,
            HttpMethod.Put,
            listOf("objects", PRODUCTS, product.id.toString()),
            body = jsonBody(product.toJson()),
        )
    }

    suspend fun deleteProduct(connection: ServerConnection, id: Int) {
        execute(connection, HttpMethod.Delete, listOf("objects", PRODUCTS, id.toString()))
    }

    /** Saves the userfield [values] by field name of one object of [entity]. */
    suspend fun setUserfields(
        connection: ServerConnection,
        entity: String,
        objectId: Int,
        values: Map<String, String?>,
    ) {
        execute(
            connection,
            HttpMethod.Put,
            listOf("userfields", entity, objectId.toString()),
            body = jsonBody(JsonObject(values.mapValues { JsonPrimitive(it.value) })),
        )
    }

    /** The product picture [fileName], scaled by Grocy to at most [maxWidth] pixels. */
    suspend fun productPicture(
        connection: ServerConnection,
        fileName: String,
        maxWidth: Int,
    ): ByteArray {
        val response = execute(
            connection,
            HttpMethod.Get,
            listOf("files", PRODUCT_PICTURES, fileName.encodeBase64()),
            query = listOf("force_serve_as" to "picture", "best_fit_width" to maxWidth.toString()),
        )
        return response.bodyAsBytes()
    }

    /** Uploads a product picture; Grocy never overwrites a file, so [fileName] must be new. */
    suspend fun uploadProductPicture(
        connection: ServerConnection,
        fileName: String,
        bytes: ByteArray,
    ) {
        execute(
            connection,
            HttpMethod.Put,
            listOf("files", PRODUCT_PICTURES, fileName.encodeBase64()),
            body = ByteArrayContent(bytes, ContentType.Application.OctetStream),
        )
    }

    suspend fun deleteProductPicture(connection: ServerConnection, fileName: String) {
        execute(
            connection,
            HttpMethod.Delete,
            listOf("files", PRODUCT_PICTURES, fileName.encodeBase64()),
        )
    }

    private suspend inline fun <reified T> get(
        connection: ServerConnection,
        vararg path: String,
        query: List<Pair<String, String>> = emptyList(),
    ): T = decode(execute(connection, HttpMethod.Get, path.toList(), query))

    private suspend inline fun <reified T> decode(response: HttpResponse): T =
        try {
            response.body<T>()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw GrocyApiException.NotGrocyServer(e)
        }

    /** Sends a request and maps failures to [GrocyApiException]s. */
    private suspend fun execute(
        connection: ServerConnection,
        method: HttpMethod,
        path: List<String>,
        query: List<Pair<String, String>> = emptyList(),
        body: OutgoingContent? = null,
    ): HttpResponse {
        var response = send(connection, method, path, query, body)
        if (response.status == HttpStatusCode.Unauthorized &&
            connection is ServerConnection.HomeAssistant
        ) {
            // Also the answer to an expired ingress session, so one fresh session decides
            ingress.invalidate(connection)
            response = send(connection, method, path, query, body)
        }
        when {
            response.status == HttpStatusCode.Unauthorized -> throw GrocyApiException.Unauthorized()
            !response.status.isSuccess() -> throw GrocyApiException.HttpError(
                statusCode = response.status.value,
                serverMessage = errorMessageOf(response),
            )
        }
        if (connection is ServerConnection.HomeAssistant) {
            ingress.markUsed(connection)
        }
        return response
    }

    /** Grocy explains rejected requests in `error_message`, e.g. a name that exists already. */
    private suspend fun errorMessageOf(response: HttpResponse): String? =
        try {
            GrocyJson.decodeFromString<ErrorDto>(response.bodyAsText()).errorMessage
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            null
        }

    private suspend fun send(
        connection: ServerConnection,
        method: HttpMethod,
        path: List<String>,
        query: List<Pair<String, String>>,
        body: OutgoingContent?,
    ): HttpResponse {
        val ingressSession = (connection as? ServerConnection.HomeAssistant)
            ?.let { ingress.session(it) }
        val baseUrl = connection.serverUrl + ingressSession?.ingressPath.orEmpty()
        val apiKey = connection.apiKey
        return request {
            httpClient.request(baseUrl) {
                this.method = method
                url {
                    appendPathSegments("api", *path.toTypedArray())
                    query.forEach { (name, value) -> parameters.append(name, value) }
                    if (apiKey != null && apiKeyAsQueryParameter) {
                        parameters.append(API_KEY_HEADER, apiKey)
                    }
                }
                if (apiKey != null && !apiKeyAsQueryParameter) {
                    header(API_KEY_HEADER, apiKey)
                }
                if (ingressSession != null) {
                    header(HttpHeaders.Cookie, "$INGRESS_COOKIE=${ingressSession.sessionId}")
                }
                if (body != null) {
                    setBody(body)
                }
            }
        }
    }

    private suspend inline fun request(block: () -> HttpResponse): HttpResponse =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // Throwable, not Exception: on Wasm a failed fetch arrives as a Kotlin Error
            throw GrocyApiException.Unreachable(e)
        }

    companion object {
        /** Header name, and query parameter name where the header cannot be used. */
        const val API_KEY_HEADER = "GROCY-API-KEY"

        private const val PRODUCTS = "products"
        private const val PRODUCT_PICTURES = "productpictures"

        private const val INGRESS_COOKIE = "ingress_session"
    }
}

/**
 * JSON with exactly `application/json` as content type: Grocy compares the header as string and
 * rejects it with a charset parameter.
 */
private fun jsonBody(json: JsonObject): OutgoingContent =
    TextContent(json.toString(), ContentType.Application.Json)

private val ServerConnection.apiKey: String?
    get() = when (this) {
        is ServerConnection.ApiKey -> apiKey
        is ServerConnection.HomeAssistant -> apiKey
        is ServerConnection.Demo -> null
    }
