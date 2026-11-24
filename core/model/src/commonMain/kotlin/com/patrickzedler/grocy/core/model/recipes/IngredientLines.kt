package com.patrickzedler.grocy.core.model.recipes

import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.text.FuzzySearch
import com.patrickzedler.grocy.core.model.text.normalizedForSearch

/**
 * An ingredient line of a recipe site taken apart: "2 EL Olivenöl, kalt gepresst" is amount 2,
 * unit "EL", name "Olivenöl" and note "kalt gepresst".
 */
data class IngredientLine(
    val text: String,
    /** The larger number of a range ("1-2" is 2), null without a number. */
    val amount: Double?,
    /** The unit as the site wrote it. */
    val unit: String?,
    val name: String,
    val note: String?,
)

/**
 * Takes [line] apart: a leading amount (decimals with point or comma, fractions also as "½",
 * ranges), then a unit if the next words are one ([unitWords] adds Grocy's own unit names to
 * the common ones of several languages), the name, and as note what follows a comma or stands
 * in brackets. "de", "of" and the like between unit and name are left out.
 */
fun parseIngredientLine(line: String, unitWords: Set<String> = emptySet()): IngredientLine {
    val text = line.replace(Regex("\\s+"), " ").trim()
    var rest = replaceFractionCharacters(text).replace(APPROXIMATELY, "")
    val amountMatch = AMOUNT.find(rest)
    val amount = amountMatch?.let { match ->
        // A range needs as much as its larger end
        listOfNotNull(parseAmount(match.groupValues[1]), parseAmount(match.groupValues[2]))
            .maxOrNull()
    }
    if (amountMatch != null) rest = rest.substring(amountMatch.range.last + 1).trim()
    // A bracket right after a word is its plural, "Ei(er)" and "Prise(n)", no note
    rest = rest.replace(PLURAL_SUFFIX, "")

    val notes = mutableListOf<String>()
    var unit: String? = null
    if (amount != null) {
        val words = rest.split(' ')
        // The longest phrase first, "cuillère à soupe" before "cuillère"
        for (count in minOf(MAX_UNIT_WORDS, words.size) downTo 1) {
            val phrase = words.take(count).joinToString(" ")
            val normalized = phrase.normalizedForSearch()
            if (normalized.isNotEmpty() && (isUnitWord(normalized) || normalized in unitWords)) {
                unit = phrase.trimEnd(',')
                rest = words.drop(count).joinToString(" ")
                // Chefkoch's "1 TL, gestr. Zimt": how full the spoon is belongs to the note
                val next = rest.substringBefore(' ')
                if (phrase.endsWith(',') && next.endsWith('.') && ' ' in rest) {
                    notes += next
                    rest = rest.substringAfter(' ')
                }
                break
            }
        }
        rest = rest.replace(CONNECTOR, "")
    }

    rest = BRACKETS.replace(rest) { match ->
        notes += match.groupValues[1].trim()
        " "
    }
    val comma = rest.indexOf(',')
    if (comma >= 0) {
        notes += rest.substring(comma + 1).trim()
        rest = rest.substring(0, comma)
    }
    val name = rest.replace(Regex("\\s+"), " ").trim().ifEmpty { text }
    return IngredientLine(
        text = text,
        amount = amount,
        unit = unit,
        name = name,
        note = notes.filter { it.isNotEmpty() }.joinToString(", ").ifEmpty { null },
    )
}

/** The searchable names of Grocy's [units]: singular, plural and every plural form. */
fun unitWordsOf(units: List<QuantityUnit>): Set<String> =
    units.flatMap { it.names() }.toSet()

/**
 * Grocy's unit for the [unit] a site wrote: one named like it, or one named like a common
 * spelling of the same unit ("EL" finds a unit "Esslöffel", "g" one named "Gram").
 */
fun matchUnit(unit: String?, units: List<QuantityUnit>): QuantityUnit? {
    val normalized = unit?.normalizedForSearch()?.takeIf { it.isNotEmpty() } ?: return null
    val active = units.filter { it.active }
    active.firstOrNull { normalized in it.names() }?.let { return it }
    val spellings = UNIT_ALIASES.firstOrNull { normalized in it } ?: return null
    return active.firstOrNull { candidate -> candidate.names().any { it in spellings } }
}

/** A product proposed for an ingredient; only [isConfident] ones are added without asking. */
data class ProductSuggestion(val product: Product, val isConfident: Boolean)

/**
 * The product of [products] that [name] most likely means: the same name, the same words in
 * singular or plural ("Eier" and "Ei"), all of the product's words in the name ("frische
 * Petersilie" is "Petersilie"); then, not confident, a product found in the name or the name
 * found in a product, typos forgiven. Inactive products are never proposed.
 */
fun suggestProduct(name: String, products: List<Product>): ProductSuggestion? {
    val normalized = name.normalizedForSearch()
    if (normalized.isEmpty()) return null
    val words = normalized.split(' ')
    val stems = words.map(::stem)
    return products.asSequence()
        .filter { it.active }
        .mapNotNull { product ->
            val productName = product.name.normalizedForSearch()
            val productWords = productName.split(' ').filter { it.isNotEmpty() }
            val productStems = productWords.map(::stem)
            val rank = when {
                productName == normalized -> Rank(0, 0)
                productStems == stems -> Rank(1, 0)
                productStems.isNotEmpty() && productStems.all { it in stems } ->
                    Rank(2, words.size - productWords.size)
                else -> {
                    val found = FuzzySearch.score(product.name, name)?.let { Rank(3, it) }
                    found ?: FuzzySearch.score(name, product.name)?.let { Rank(4, it) }
                }
            }
            rank?.let { product to it }
        }
        .sortedWith(
            compareBy<Pair<Product, Rank>> { it.second.level }
                .thenBy { it.second.distance }
                // The more specific product, "Olivenöl" before "Öl"
                .thenByDescending { it.first.name.length },
        )
        .firstOrNull()
        ?.let { (product, rank) -> ProductSuggestion(product, isConfident = rank.level <= 2) }
}

private class Rank(val level: Int, val distance: Int)

private fun QuantityUnit.names(): List<String> = buildList {
    add(name)
    namePlural?.let(::add)
    pluralForms?.lines()?.let(::addAll)
}.map { it.normalizedForSearch() }.filter { it.isNotEmpty() }

private fun isUnitWord(normalized: String) = UNIT_ALIASES.any { normalized in it }

/** Singular and plural compare equal: common endings of German, English and Romance plurals. */
private fun stem(word: String): String {
    for (ending in PLURAL_ENDINGS) {
        if (word.endsWith(ending) && word.length - ending.length >= MIN_STEM) {
            return word.dropLast(ending.length)
        }
    }
    return word
}

private fun replaceFractionCharacters(text: String): String {
    var result = text
    for ((char, fraction) in FRACTIONS) {
        // "1½" is one and a half, a lone "½" a half
        result = result.replace(Regex("(\\d)$char"), "$1 $fraction")
            .replace(char.toString(), fraction)
    }
    return result
}

private fun parseAmount(text: String): Double? {
    if (text.isBlank()) return null
    val parts = text.trim().split(' ')
    return parts.sumOf { part ->
        if ('/' in part) {
            val (numerator, denominator) = part.split('/')
                .map { it.toDoubleOrNull() ?: return null }
            if (denominator == 0.0) return null
            numerator / denominator
        } else {
            part.replace(',', '.').toDoubleOrNull() ?: return null
        }
    }
}

private const val MAX_UNIT_WORDS = 3
private const val MIN_STEM = 2

private const val NUMBER = "\\d+(?:[.,]\\d+)?"
private const val QUANTITY = "\\d+ \\d+/\\d+|\\d+/\\d+|$NUMBER"

private val AMOUNT = Regex(
    "^($QUANTITY)(?:\\s*(?:-|–|—|bis|to|à|a)\\s*($QUANTITY))?(?=\\s|[^\\d.,/]|$)",
    RegexOption.IGNORE_CASE,
)
private val APPROXIMATELY = Regex(
    "^(?:ca\\.?|etwa|circa|about|approx\\.?|environ)\\s+",
    RegexOption.IGNORE_CASE,
)
private val CONNECTOR = Regex("^(?:de |d'|d’|of |di |del |von )", RegexOption.IGNORE_CASE)
private val PLURAL_SUFFIX = Regex("(?<=[A-Za-zÀ-ÿ])\\((?:n|en|e|er|s|es|nen)\\)")
private val BRACKETS = Regex("\\(([^)]*)\\)")

private val FRACTIONS = mapOf(
    '½' to "1/2",
    '⅓' to "1/3",
    '⅔' to "2/3",
    '¼' to "1/4",
    '¾' to "3/4",
    '⅛' to "1/8",
)

private val PLURAL_ENDINGS = listOf("nen", "en", "er", "es", "n", "e", "s")

/**
 * Common spellings of the units recipe sites use, in the languages of the app; each set is one
 * unit, written as [normalizedForSearch] makes it (lowercase, no accents, no dots).
 */
private val UNIT_ALIASES: List<Set<String>> = listOf(
    setOf("g", "gr", "gramm", "gram", "grams", "gramme", "grammes", "grammi", "gramos", "grammo"),
    setOf("kg", "kilo", "kilos", "kilogramm", "kilogram", "kilograms", "kilogramme", "chilo"),
    setOf("mg", "milligramm", "milligram", "milligrams"),
    setOf(
        "ml", "milliliter", "millilitre", "milliliters", "millilitres", "millilitro", "mililitros",
    ),
    setOf("cl", "zentiliter", "centiliter", "centilitre", "centilitres"),
    setOf("dl", "deziliter", "deciliter", "decilitre"),
    setOf("l", "liter", "litre", "liters", "litres", "litro", "litri", "litros"),
    setOf(
        "el", "essl", "essloffel", "tbsp", "tbs", "tblsp", "tablespoon", "tablespoons", "cs",
        "c a s", "cas", "cuillere a soupe", "cuilleres a soupe", "cucchiaio", "cucchiai",
        "cucharada", "cucharadas",
    ),
    setOf(
        "tl", "teel", "teeloffel", "tsp", "teaspoon", "teaspoons", "cc", "c a c", "cac",
        "cuillere a cafe", "cuilleres a cafe", "cucchiaino", "cucchiaini", "cucharadita",
        "cucharaditas",
    ),
    setOf("stk", "st", "stuck", "piece", "pieces", "pc", "pcs", "pezzo", "pezzi"),
    setOf("prise", "prisen", "pinch", "pinches", "pincee", "pincees", "pizzico"),
    setOf("zehe", "zehen", "clove", "cloves", "gousse", "gousses", "spicchio", "spicchi"),
    setOf("tasse", "tassen", "cup", "cups", "tazza", "tazze", "taza", "tazas"),
    setOf("lb", "lbs", "pound", "pounds"),
    setOf("oz", "ounce", "ounces"),
    setOf("fl oz", "fluid ounce", "fluid ounces"),
    setOf("dose", "dosen", "can", "cans", "boite", "boites", "lattina"),
    setOf(
        "pck", "pkg", "pack", "packs", "packung", "packungen", "packchen", "package", "packages",
        "packet", "packets", "sachet", "sachets", "bustina", "bustine",
    ),
    setOf("bund", "bunde", "bunch", "bunches", "botte", "bottes", "mazzo"),
    setOf("scheibe", "scheiben", "slice", "slices", "tranche", "tranches", "fetta", "fette"),
    setOf("becher", "pot", "pots", "vasetto"),
    setOf("glas", "glaser", "jar", "jars", "bocal"),
    setOf("handvoll", "handful", "handfuls", "poignee", "poignees"),
    setOf("msp", "messerspitze", "messerspitzen"),
    setOf("schuss", "dash", "dashes", "trait", "goccio"),
    setOf("tropfen", "drop", "drops", "goutte", "gouttes"),
    setOf("blatt", "blatter", "leaf", "leaves", "feuille", "feuilles", "foglia", "foglie"),
    setOf("zweig", "zweige", "sprig", "sprigs", "brin", "brins", "rametto"),
    setOf("stange", "stangen", "stalk", "stalks", "stick", "sticks", "baton"),
    setOf("wurfel", "cube", "cubes"),
    setOf("knolle", "knollen", "bulb", "bulbs", "head", "heads"),
)
