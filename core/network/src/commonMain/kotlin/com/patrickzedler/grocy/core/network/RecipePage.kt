package com.patrickzedler.grocy.core.network

import com.patrickzedler.grocy.core.model.recipes.ImportedRecipe
import com.patrickzedler.grocy.core.model.recipes.InstructionSection
import com.patrickzedler.grocy.core.model.recipes.RecipeNutrient
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.takeFrom
import kotlin.math.roundToInt
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * The recipe of a recipe site's page [html] (at [pageUrl]), from the schema.org `Recipe` the
 * page embeds as JSON-LD for search engines: Chefkoch, Allrecipes, BBC Good Food, Kitchen
 * Stories, Marmiton and most others. Null if the page has none.
 */
fun parseRecipePage(html: String, pageUrl: String): ImportedRecipe? {
    val documents = JSON_LD.findAll(html).mapNotNull { parseJson(it.groupValues[1]) }.toList()
    // Objects of the page by their "@id", which others point to, Chefkoch's picture and author
    val nodes = buildMap { documents.forEach { collectNodes(it, this) } }
    val reader = RecipeReader(nodes)
    val recipe = documents.firstNotNullOfOrNull(::findRecipe) ?: return null
    val imported = reader.recipeOf(recipe, pageUrl) ?: return null
    if (imported.imageUrl != null) return imported
    // The picture shared links show, where the recipe names none
    val sharedPicture = OPEN_GRAPH_IMAGE.find(html)?.groupValues?.get(1)
        ?.let { absoluteUrl(decodeEntities(it), pageUrl) }
    return imported.copy(imageUrl = sharedPicture)
}

private fun collectNodes(element: JsonElement, nodes: MutableMap<String, JsonObject>) {
    when (element) {
        is JsonArray -> element.forEach { collectNodes(it, nodes) }
        is JsonObject -> {
            val id = (element["@id"] as? JsonPrimitive)?.contentOrNull
            if (id != null && element.size > 1 && id !in nodes) nodes[id] = element
            element.values.forEach { collectNodes(it, nodes) }
        }
        else -> Unit
    }
}

private fun parseJson(text: String): JsonElement? =
    try {
        LenientJson.parseToJsonElement(text.trim())
    } catch (_: IllegalArgumentException) {
        // Some sites put raw line breaks into strings, which JSON does not allow
        try {
            LenientJson.parseToJsonElement(text.replace(CONTROL_CHARACTERS, " ").trim())
        } catch (_: IllegalArgumentException) {
            null
        }
    }

/** The recipe in a JSON-LD document: itself, in a list, an `@graph` or as `mainEntity`. */
private fun findRecipe(element: JsonElement): JsonObject? = when (element) {
    is JsonArray -> element.firstNotNullOfOrNull(::findRecipe)
    is JsonObject -> when {
        element.isRecipe() -> element
        else -> listOfNotNull(element["@graph"], element["mainEntity"])
            .firstNotNullOfOrNull(::findRecipe)
    }
    else -> null
}

private fun JsonObject.isRecipe(): Boolean = when (val type = this["@type"]) {
    is JsonPrimitive -> type.contentOrNull == RECIPE
    is JsonArray -> type.any { (it as? JsonPrimitive)?.contentOrNull == RECIPE }
    else -> false
}

/** Reads a recipe whose values may point to other objects of the page by their "@id". */
private class RecipeReader(private val nodes: Map<String, JsonObject>) {

    fun recipeOf(recipe: JsonObject, pageUrl: String): ImportedRecipe? {
        val author = joined(recipe["author"])
        val name = recipe.text("name")?.let { withoutAuthor(it, author) } ?: return null
        val nutrition = resolved(recipe["nutrition"]) as? JsonObject
        return ImportedRecipe(
            name = name,
            sourceUrl = pageUrl,
            description = recipe.text("description")?.takeIf { it != name },
            servings = servingsOf(textsOf(recipe["recipeYield"])),
            ingredients = textsOf(recipe["recipeIngredient"] ?: recipe["ingredients"]),
            instructions = instructionsOf(recipe["recipeInstructions"]),
            imageUrl = imageOf(recipe["image"])?.let { absoluteUrl(it, pageUrl) },
            prepMinutes = minutesOf(recipe.text("prepTime")),
            cookMinutes = minutesOf(recipe.text("cookTime")),
            totalMinutes = minutesOf(recipe.text("totalTime")),
            nutrition = nutrition?.let(::nutritionOf).orEmpty(),
            category = joined(recipe["recipeCategory"]),
            cuisine = joined(recipe["recipeCuisine"]),
            author = author,
        )
    }

    /** The object a reference ({"@id": ...} alone) points to, everything else as it is. */
    private fun resolved(element: JsonElement?): JsonElement? {
        val reference = element as? JsonObject ?: return element
        if (reference.size != 1) return element
        val id = (reference["@id"] as? JsonPrimitive)?.contentOrNull ?: return element
        return nodes[id] ?: element
    }

    private fun JsonObject.text(key: String): String? = textsOf(this[key]).firstOrNull()

    private fun joined(element: JsonElement?): String? =
        textsOf(element).distinct().joinToString(", ").ifEmpty { null }

    /** Plain texts of a string, a list of strings or of objects with a name. */
    private fun textsOf(element: JsonElement?): List<String> =
        when (val value = resolved(element)) {
            is JsonPrimitive -> listOfNotNull(value.contentOrNull?.let { cleanText(it) })
            is JsonArray -> value.flatMap(::textsOf)
            is JsonObject -> textsOf(value["name"] ?: value["text"])
            else -> emptyList()
        }.filter { it.isNotEmpty() }

    /**
     * The steps as text, a list of texts, `HowToStep`s or `HowToSection`s of steps; a text of
     * several lines is one step per line.
     */
    private fun instructionsOf(element: JsonElement?): List<InstructionSection> {
        val sections = mutableListOf<InstructionSection>()
        val loose = mutableListOf<String>()
        fun flushLoose() {
            if (loose.isNotEmpty()) sections += InstructionSection(null, loose.toList())
            loose.clear()
        }
        fun add(item: JsonElement?) {
            when (val value = resolved(item)) {
                is JsonPrimitive -> value.contentOrNull?.let { text ->
                    loose += cleanText(text, keepLines = true).lines()
                        .map(String::trim)
                        .filter(String::isNotEmpty)
                }
                is JsonArray -> value.forEach(::add)
                is JsonObject -> {
                    val steps = value["itemListElement"]
                    if (steps != null) {
                        flushLoose()
                        val inner = instructionsOf(steps).flatMap { it.steps }
                        val headline = textsOf(value["name"]).firstOrNull()
                        if (inner.isNotEmpty()) sections += InstructionSection(headline, inner)
                    } else {
                        // A step's text, its name repeats the text's beginning on many sites
                        textsOf(value["text"] ?: value["name"]).firstOrNull()?.let(loose::add)
                    }
                }
                else -> Unit
            }
        }
        add(element)
        flushLoose()
        return sections
    }

    private fun imageOf(element: JsonElement?): String? = when (val value = resolved(element)) {
        is JsonPrimitive -> value.contentOrNull
        is JsonArray -> value.firstNotNullOfOrNull(::imageOf)
        is JsonObject -> imageOf(value["url"] ?: value["contentUrl"])
        else -> null
    }?.trim()?.takeIf { it.isNotEmpty() }

    private fun nutritionOf(nutrition: JsonObject): Map<RecipeNutrient, String> =
        NUTRITION_KEYS.mapNotNull { (nutrient, key) ->
            textsOf(nutrition[key]).firstOrNull()?.let { nutrient to nutritionValue(it) }
        }.toMap()
}

/** Chefkoch names its recipes "Apfelkuchen von heresbach", the author is kept apart. */
private fun withoutAuthor(name: String, author: String?): String {
    if (author == null) return name
    val shortened = AUTHOR_SUFFIXES.firstNotNullOfOrNull { word ->
        name.removeSuffix(" $word $author").takeIf { it != name }
    }
    return shortened?.trim()?.ifEmpty { null } ?: name
}

private val AUTHOR_SUFFIXES = listOf("von", "by", "de", "di", "par")

/** The first whole number of the yield: "4", 4, "4 Portionen" or ["4", "4 servings"]. */
private fun servingsOf(texts: List<String>): Int? = texts
    .firstNotNullOfOrNull { NUMBER.find(it)?.value?.replace(',', '.')?.toDoubleOrNull() }
    ?.roundToInt()
    ?.takeIf { it > 0 }

private fun absoluteUrl(url: String, pageUrl: String): String? = try {
    val resolved = URLBuilder(Url(pageUrl)).takeFrom(url).buildString()
    resolved.takeIf { it.startsWith("https://") || it.startsWith("http://") }
} catch (_: Exception) {
    null
}

/**
 * A nutrition value with its short unit: "30.03 grams fat" is "30.03 g", "656 calories" is
 * "656 kcal"; values in another form stay as the site wrote them.
 */
internal fun nutritionValue(text: String): String {
    val match = NUTRITION_VALUE.find(text) ?: return text
    val unit = when (match.groupValues[2].lowercase()) {
        "kcal", "cal", "calorie", "calories", "kalorien" -> "kcal"
        "g", "gram", "grams", "gramm", "gr" -> "g"
        "mg", "milligram", "milligrams", "milligramm" -> "mg"
        else -> return text
    }
    return "${match.groupValues[1]} $unit"
}

/**
 * Minutes of an ISO 8601 duration like "PT1H15M" or "P0DT0H20M", null for none or zero.
 * Some sites leave the "T" out ("P20M"), which then means minutes too.
 */
internal fun minutesOf(duration: String?): Int? {
    val match = duration?.let { DURATION.matchEntire(it.trim().uppercase()) } ?: return null
    fun part(index: Int) = match.groupValues[index].replace(',', '.').toDoubleOrNull() ?: 0.0
    val minutes = part(1) * MINUTES_PER_DAY + part(2) * MINUTES_PER_HOUR + part(3) +
        part(4) / SECONDS_PER_MINUTE
    return minutes.roundToInt().takeIf { it > 0 }
}

/** Texts of JSON-LD may hold HTML and its entities; plain text with single spaces is kept. */
internal fun cleanText(text: String, keepLines: Boolean = false): String {
    val withBreaks = text.replace(LINE_TAGS, "\n")
    val plain = decodeEntities(withBreaks.replace(TAG, ""))
    return if (keepLines) {
        plain.lines().joinToString("\n") { it.replace(SPACES, " ").trim() }.trim()
    } else {
        plain.replace(WHITESPACE, " ").trim()
    }
}

private fun decodeEntities(text: String): String = ENTITY.replace(text) { match ->
    val entity = match.groupValues[1]
    when {
        entity.startsWith("#x", ignoreCase = true) ->
            entity.drop(2).toIntOrNull(16)?.let(::codePointToString)
        entity.startsWith("#") -> entity.drop(1).toIntOrNull()?.let(::codePointToString)
        else -> NAMED_ENTITIES[entity]
    } ?: match.value
}

private fun codePointToString(codePoint: Int): String? = when {
    codePoint in 0..0xFFFF -> codePoint.toChar().toString()
    codePoint <= MAX_CODE_POINT -> {
        val offset = codePoint - 0x10000
        charArrayOf(
            (0xD800 + (offset shr 10)).toChar(),
            (0xDC00 + (offset and 0x3FF)).toChar(),
        ).concatToString()
    }
    else -> null
}

private const val RECIPE = "Recipe"
private const val MINUTES_PER_DAY = 1440
private const val MINUTES_PER_HOUR = 60
private const val SECONDS_PER_MINUTE = 60.0
private const val MAX_CODE_POINT = 0x10FFFF

private val LenientJson = Json { isLenient = true }

private val JSON_LD = Regex(
    "<script[^>]*type\\s*=\\s*[\"']?application/ld\\+json[\"']?[^>]*>([\\s\\S]*?)</script>",
    RegexOption.IGNORE_CASE,
)
private val CONTROL_CHARACTERS = Regex("[\\u0000-\\u001F]")
private val NUMBER = Regex("\\d+(?:[.,]\\d+)?")
private val NUTRITION_VALUE = Regex(
    "^(\\d+(?:[.,]\\d+)?)\\s*(kcal|cal|calories|calorie|kalorien|grams|gram|gramm|gr|g|" +
        "milligrams|milligram|milligramm|mg)\\b",
    RegexOption.IGNORE_CASE,
)
private val OPEN_GRAPH_IMAGE = Regex(
    "<meta[^>]+property\\s*=\\s*[\"']og:image[\"'][^>]+content\\s*=\\s*[\"']([^\"']+)[\"']",
    RegexOption.IGNORE_CASE,
)
private val DURATION = Regex(
    "P(?:(\\d+(?:[.,]\\d+)?)D)?(?:T?(?:(\\d+(?:[.,]\\d+)?)H)?(?:(\\d+(?:[.,]\\d+)?)M)?" +
        "(?:(\\d+(?:[.,]\\d+)?)S)?)?",
)
private val LINE_TAGS = Regex("(?i)<br\\s*/?>|</(?:p|li|div|h[1-6])>")
private val TAG = Regex("<[^>]*>")
private val ENTITY = Regex("&(#[xX]?[0-9a-fA-F]+|[a-zA-Z]+);")
private val SPACES = Regex("[ \\t\\u00A0]+")
private val WHITESPACE = Regex("\\s+")

private val NAMED_ENTITIES = mapOf(
    "amp" to "&",
    "lt" to "<",
    "gt" to ">",
    "quot" to "\"",
    "apos" to "'",
    "nbsp" to " ",
    "ndash" to "–",
    "mdash" to "—",
    "hellip" to "…",
    "frac12" to "½",
    "frac14" to "¼",
    "frac34" to "¾",
    "deg" to "°",
    "auml" to "ä",
    "ouml" to "ö",
    "uuml" to "ü",
    "Auml" to "Ä",
    "Ouml" to "Ö",
    "Uuml" to "Ü",
    "szlig" to "ß",
    "eacute" to "é",
    "egrave" to "è",
    "agrave" to "à",
    "ccedil" to "ç",
)

private val NUTRITION_KEYS = listOf(
    RecipeNutrient.CALORIES to "calories",
    RecipeNutrient.FAT to "fatContent",
    RecipeNutrient.SATURATED_FAT to "saturatedFatContent",
    RecipeNutrient.CARBOHYDRATES to "carbohydrateContent",
    RecipeNutrient.SUGARS to "sugarContent",
    RecipeNutrient.FIBER to "fiberContent",
    RecipeNutrient.PROTEIN to "proteinContent",
    RecipeNutrient.SODIUM to "sodiumContent",
)
