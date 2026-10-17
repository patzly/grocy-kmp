package com.patrickzedler.grocy.core.model.text

import kotlin.math.abs
import kotlin.math.min

/**
 * Search that forgives small typos, for lists and pickers: every word of the query has to be
 * found in the text, as part of a word or with up to one typo (two in long words). Case and
 * accents do not matter, like in Grocy's web frontend ("creme" finds "Crème").
 */
object FuzzySearch {

    /**
     * How well [query] matches [text], lower is better; null if it does not match. A blank query
     * matches everything with 0.
     */
    fun score(query: String, text: String): Int? {
        val queryWords = query.normalizedForSearch().split(' ').filter { it.isNotEmpty() }
        if (queryWords.isEmpty()) return 0
        val normalizedText = text.normalizedForSearch()
        val textWords = normalizedText.split(' ').filter { it.isNotEmpty() }
        var total = 0
        for (word in queryWords) {
            total += wordScore(word, normalizedText, textWords) ?: return null
        }
        return total
    }

    /** The items that match [query], best first; equal matches keep their order. */
    fun <T> filter(items: List<T>, query: String, texts: (T) -> List<String>): List<T> {
        if (query.isBlank()) return items
        return items
            .mapNotNull { item ->
                texts(item).mapNotNull { score(query, it) }.minOrNull()?.let { item to it }
            }
            .sortedBy { it.second }
            .map { it.first }
    }

    private fun wordScore(word: String, text: String, textWords: List<String>): Int? {
        if (textWords.any { it.startsWith(word) }) return 0
        if (word in text) return 1
        val allowedTypos = when {
            word.length <= 3 -> 0
            word.length <= 6 -> 1
            else -> 2
        }
        if (allowedTypos == 0) return null
        val typos = textWords.minOfOrNull { textWord ->
            // The word may be the start of a longer one, so compare with prefixes around its length
            (word.length - 1..word.length + 1)
                .filter { it in 1..textWord.length }
                .minOfOrNull { length -> editDistance(word, textWord.take(length)) }
                ?: Int.MAX_VALUE
        } ?: return null
        return if (typos <= allowedTypos) 1 + typos else null
    }
}

/** Lowercase, without accents and with single spaces, for comparing search terms. */
fun String.normalizedForSearch(): String {
    val builder = StringBuilder(length)
    for (char in lowercase()) {
        val folded = foldedCharacters[char]
        when {
            folded != null -> builder.append(folded)
            char.isLetterOrDigit() -> builder.append(char)
            // Punctuation separates words like spaces do ("Coca-Cola" finds "cola")
            else -> if (builder.isNotEmpty() && builder.last() != ' ') builder.append(' ')
        }
    }
    return builder.toString().trimEnd()
}

/**
 * Optimal string alignment distance: insertions, deletions, substitutions and swaps of two
 * neighbouring letters each count as one typo.
 */
internal fun editDistance(a: String, b: String): Int {
    if (abs(a.length - b.length) > 2 && a.isNotEmpty() && b.isNotEmpty()) {
        return abs(a.length - b.length)
    }
    val rows = Array(a.length + 1) { IntArray(b.length + 1) }
    for (i in 0..a.length) rows[i][0] = i
    for (j in 0..b.length) rows[0][j] = j
    for (i in 1..a.length) {
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            var value = min(min(rows[i - 1][j] + 1, rows[i][j - 1] + 1), rows[i - 1][j - 1] + cost)
            if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                value = min(value, rows[i - 2][j - 2] + 1)
            }
            rows[i][j] = value
        }
    }
    return rows[a.length][b.length]
}

// Lowercase letters with accents and their plain form; Wasm has no Unicode normalization
private val foldedCharacters: Map<Char, String> = buildMap {
    fun fold(letters: String, plain: String) = letters.forEach { put(it, plain) }
    fold("àáâãäåāăą", "a")
    fold("æ", "ae")
    fold("çćĉċč", "c")
    fold("ďđ", "d")
    fold("èéêëēĕėęě", "e")
    fold("ĝğġģ", "g")
    fold("ĥħ", "h")
    fold("ìíîïĩīĭįı", "i")
    fold("ĵ", "j")
    fold("ķ", "k")
    fold("ĺļľŀł", "l")
    fold("ñńņňŉ", "n")
    fold("òóôõöøōŏő", "o")
    fold("œ", "oe")
    fold("ŕŗř", "r")
    fold("śŝşšș", "s")
    fold("ß", "ss")
    fold("ţťŧț", "t")
    fold("ùúûüũūŭůűų", "u")
    fold("ŵ", "w")
    fold("ýÿŷ", "y")
    fold("źżž", "z")
}
