package com.patrickzedler.grocy.core.model.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FuzzySearchTest {

    @Test
    fun caseAndAccentsDoNotMatter() {
        assertEquals(0, FuzzySearch.score("creme", "Crème fraîche"))
        assertEquals(0, FuzzySearch.score("KASE", "Käse"))
    }

    @Test
    fun wordsMatchInAnyOrder() {
        assertEquals(0, FuzzySearch.score("milk whole", "Whole milk"))
    }

    @Test
    fun partOfAWordMatches() {
        assertEquals(1, FuzzySearch.score("ghurt", "Yoghurt"))
    }

    @Test
    fun smallTyposStillMatch() {
        assertNotNull(FuzzySearch.score("chese", "Cheese"))
        assertNotNull(FuzzySearch.score("tomatoe", "Tomato"))
        // Two letters swapped count as one typo
        assertNotNull(FuzzySearch.score("mlik", "Milk"))
    }

    @Test
    fun tooManyTyposDoNotMatch() {
        assertNull(FuzzySearch.score("chxxe", "Cheese"))
    }

    @Test
    fun shortWordsMustMatchExactly() {
        assertNull(FuzzySearch.score("tea", "Tee"))
    }

    @Test
    fun everyWordMustMatch() {
        assertNull(FuzzySearch.score("milk bread", "Whole milk"))
    }

    @Test
    fun blankQueryMatchesEverything() {
        assertEquals(0, FuzzySearch.score("  ", "Anything"))
    }

    @Test
    fun punctuationSeparatesWords() {
        assertEquals(0, FuzzySearch.score("cola", "Coca-Cola"))
    }

    @Test
    fun filterPutsBetterMatchesFirstAndKeepsTheOrderOfEqualOnes() {
        val items = listOf("Cheesecake", "Cream cheese", "Chese sticks", "Bread")

        val result = FuzzySearch.filter(items, "chees") { listOf(it) }

        assertEquals(listOf("Cheesecake", "Cream cheese", "Chese sticks"), result)
    }

    @Test
    fun filterSearchesEveryTextOfAnItem() {
        val items = listOf("Milk" to "Dairy", "Bread" to "Bakery")

        val result = FuzzySearch.filter(items, "bakery") { listOf(it.first, it.second) }

        assertEquals(listOf("Bread" to "Bakery"), result)
    }

    @Test
    fun editDistanceCountsSwapsAsOne() {
        assertEquals(1, editDistance("ab", "ba"))
        assertEquals(1, editDistance("milk", "mlik"))
        assertEquals(2, editDistance("kitten", "sitting").coerceAtMost(2))
    }
}
