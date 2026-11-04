package com.patrickzedler.grocy.core.network

import com.patrickzedler.grocy.core.model.ProductFactsSource
import com.patrickzedler.grocy.core.model.productfacts.ProductFacts
import com.patrickzedler.grocy.core.model.productfacts.ProductNutrient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

/** Thrown when a product database cannot be reached or answers in an unexpected way. */
class ProductFactsException(cause: Throwable? = null) :
    Exception("The product database could not be read", cause)

/**
 * The public API shared by Open Food Facts and its sister databases. Sends no credentials and
 * nothing but the barcode.
 *
 * @param userAgent identifies the app as the databases ask for ("Name/Version (contact)"); on the
 *   web the browser decides what it sends.
 */
class ProductFactsApi(
    private val httpClient: HttpClient,
    private val userAgent: String,
) {

    /** What [source] knows about [barcode] in [language] where it can; null if nothing. */
    suspend fun product(
        source: ProductFactsSource,
        barcode: String,
        language: String,
    ): ProductFacts? {
        val body = try {
            val response = httpClient.get(
                "https://world.${source.domain}/api/v2/product/$barcode.json",
            ) {
                header(HttpHeaders.UserAgent, userAgent)
                parameter("fields", fieldsFor(language))
            }
            when {
                response.status == HttpStatusCode.NotFound -> return null
                !response.status.isSuccess() -> throw ProductFactsException()
                else -> response.body<JsonObject>()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ProductFactsException) {
            throw e
        } catch (e: Throwable) {
            throw ProductFactsException(e)
        }
        val product = (body["product"] as? JsonObject) ?: return null
        return product.toFacts(source, barcode, language).takeIf { it.hasContent }
    }

    /** The picture of [facts], null when it has none or it cannot be downloaded. */
    suspend fun picture(facts: ProductFacts): ByteArray? {
        val url = facts.imageUrl?.takeIf { isPictureOf(facts.source, it) } ?: return null
        return try {
            val response = httpClient.get(url) { header(HttpHeaders.UserAgent, userAgent) }
            response.takeIf { it.status.isSuccess() }?.bodyAsBytes()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            null
        }
    }

    private companion object {
        const val PRODUCT_QUANTITY_UNIT = "product_quantity_unit"
        const val PER_100 = "_100g"

        val nutrientKeys = mapOf(
            ProductNutrient.FAT to "fat",
            ProductNutrient.SATURATED_FAT to "saturated-fat",
            ProductNutrient.CARBOHYDRATES to "carbohydrates",
            ProductNutrient.SUGARS to "sugars",
            ProductNutrient.FIBER to "fiber",
            ProductNutrient.PROTEIN to "proteins",
            ProductNutrient.SALT to "salt",
        )

        // Kilojoules per kilocalorie, for entries that only name the former
        const val KJ_PER_KCAL = 4.184
        val LANGUAGE = Regex("[a-z]{2}")
    }

    private fun fieldsFor(language: String): String {
        val code = language.take(2).lowercase().takeIf { LANGUAGE.matches(it) }
        val localized = listOf("product_name", "generic_name", "ingredients_text")
            .flatMap { field -> listOfNotNull(code?.let { "${field}_$it" }, field) }
        return (
            localized + listOf(
                "brands", "quantity", "product_quantity", PRODUCT_QUANTITY_UNIT,
                "image_front_url", "image_url", "nutriments", "categories", "stores",
            )
            ).joinToString(",")
    }

    private fun JsonObject.toFacts(
        source: ProductFactsSource,
        barcode: String,
        language: String,
    ): ProductFacts {
        val code = language.take(2).lowercase()
        fun localized(field: String) = text("${field}_$code") ?: text(field)
        val nutriments = this["nutriments"] as? JsonObject ?: JsonObject(emptyMap())
        return ProductFacts(
            source = source,
            barcode = barcode,
            name = localized("product_name"),
            genericName = localized("generic_name"),
            brands = text("brands"),
            quantity = text("quantity"),
            packageAmount = number("product_quantity"),
            packageUnit = text(PRODUCT_QUANTITY_UNIT)
                ?: "g".takeIf { number("product_quantity") != null },
            energyKcalPer100 = nutriments.number("energy-kcal$PER_100")
                ?: nutriments.number("energy-kj$PER_100")?.div(KJ_PER_KCAL),
            nutrition = nutrientKeys.mapNotNull { (nutrient, key) ->
                nutriments.number(key + PER_100)?.let { nutrient to it }
            }.toMap(),
            ingredients = localized("ingredients_text"),
            categories = list("categories"),
            stores = list("stores"),
            imageUrl = text("image_front_url") ?: text("image_url"),
        )
    }

    private val ProductFacts.hasContent: Boolean
        get() = name != null || genericName != null || brands != null || imageUrl != null

    private fun JsonObject.text(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.contentOrNull?.trim()
            ?.ifEmpty { null }

    private fun JsonObject.number(key: String): Double? =
        text(key)?.toDoubleOrNull()?.takeIf { it >= 0 }

    // Entries are comma separated, those of a language other than the database's "fr:Goûter"
    private fun JsonObject.list(key: String): List<String> = text(key).orEmpty().split(',')
        .map { it.substringAfter(':').trim() }
        .filter { it.isNotEmpty() }
}

/**
 * Whether [url] is a picture of [source] itself: what a database answers comes from its users, so
 * it must not make the app download from anywhere else.
 */
internal fun isPictureOf(source: ProductFactsSource, url: String): Boolean = try {
    val parsed = Url(url)
    parsed.protocol.name == "https" && parsed.host.endsWith(".${source.domain}")
} catch (_: IllegalArgumentException) {
    false
}
