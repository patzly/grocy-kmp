package com.patrickzedler.grocy.core.model.recipes

import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IngredientLinesTest {

    @Test
    fun amountUnitAndNameAreTakenApart() {
        val line = parseIngredientLine("500 g Spaghetti")

        assertEquals(IngredientLine("500 g Spaghetti", 500.0, "g", "Spaghetti", null), line)
    }

    @Test
    fun aUnitWrittenRightAfterTheNumberIsFound() {
        val line = parseIngredientLine("100g pancetta")

        assertEquals(100.0, line.amount)
        assertEquals("g", line.unit)
        assertEquals("pancetta", line.name)
    }

    @Test
    fun whatFollowsACommaIsTheNote() {
        val line = parseIngredientLine("2 tablespoons olive oil, divided, or as needed")

        assertEquals(2.0, line.amount)
        assertEquals("tablespoons", line.unit)
        assertEquals("olive oil", line.name)
        assertEquals("divided, or as needed", line.note)
    }

    @Test
    fun bracketsAreTheNote() {
        val line = parseIngredientLine("2 Zehen Knoblauch (optional)")

        assertEquals("Zehen", line.unit)
        assertEquals("Knoblauch", line.name)
        assertEquals("optional", line.note)
    }

    @Test
    fun aBracketRightAfterAWordIsItsPlural() {
        val line = parseIngredientLine("1 Ei(er)")

        assertEquals(1.0, line.amount)
        assertNull(line.unit)
        assertEquals("Ei", line.name)
        assertNull(line.note)
    }

    @Test
    fun aUnitWithItsPluralInBracketsIsFound() {
        val line = parseIngredientLine("1 Prise(n) Salz")

        assertEquals("Prise", line.unit)
        assertEquals("Salz", line.name)
    }

    @Test
    fun howFullTheSpoonIsBelongsToTheNote() {
        val line = parseIngredientLine("1 TL, gestr. Zimt")

        assertEquals("TL", line.unit)
        assertEquals("Zimt", line.name)
        assertEquals("gestr.", line.note)
    }

    @Test
    fun aWordThatIsNoUnitBelongsToTheName() {
        val line = parseIngredientLine("6 Eigelbe")

        assertEquals(6.0, line.amount)
        assertNull(line.unit)
        assertEquals("Eigelbe", line.name)
    }

    @Test
    fun decimalCommasAndFractionsAreAmounts() {
        assertEquals(1.5, parseIngredientLine("1,5 l Milch").amount)
        assertEquals(0.5, parseIngredientLine("1/2 TL Salz").amount)
        assertEquals(1.5, parseIngredientLine("1 1/2 cups flour").amount)
        assertEquals(0.5, parseIngredientLine("½ Zitrone").amount)
        assertEquals(1.5, parseIngredientLine("1½ EL Zucker").amount)
    }

    @Test
    fun aRangeNeedsItsLargerEnd() {
        assertEquals(3.0, parseIngredientLine("2-3 EL Öl").amount)
        assertEquals(3.0, parseIngredientLine("2 – 3 EL Öl").amount)
    }

    @Test
    fun wordsBetweenUnitAndNameAreLeftOut() {
        val line = parseIngredientLine("50 cl de crème fraîche")

        assertEquals("cl", line.unit)
        assertEquals("crème fraîche", line.name)
    }

    @Test
    fun aUnitOfSeveralWordsIsFoundWhole() {
        val line = parseIngredientLine("2 cuillères à soupe d'huile")

        assertEquals("cuillères à soupe", line.unit)
        assertEquals("huile", line.name)
    }

    @Test
    fun aLineWithoutNumberIsOnlyAName() {
        val line = parseIngredientLine("Salz und Pfeffer")

        assertNull(line.amount)
        assertNull(line.unit)
        assertEquals("Salz und Pfeffer", line.name)
    }

    @Test
    fun aboutBeforeTheAmountIsLeftOut() {
        val line = parseIngredientLine("ca. 200 g Mehl")

        assertEquals(200.0, line.amount)
        assertEquals("Mehl", line.name)
    }

    @Test
    fun grocysOwnUnitNamesCountAsUnits() {
        val words = unitWordsOf(listOf(QuantityUnit(1, "Kugel", "Kugeln")))

        val line = parseIngredientLine("2 Kugeln Eis", words)

        assertEquals("Kugeln", line.unit)
        assertEquals("Eis", line.name)
    }

    @Test
    fun aUnitIsMatchedByItsNameOrACommonSpelling() {
        val gram = QuantityUnit(1, "Gramm", "Gramm")
        val spoon = QuantityUnit(2, "Esslöffel", "Esslöffel")
        val units = listOf(gram, spoon)

        assertEquals(gram, matchUnit("g", units))
        assertEquals(spoon, matchUnit("EL", units))
        assertEquals(spoon, matchUnit("tbsp", units))
        assertEquals(gram, matchUnit("Gramm", units))
        assertNull(matchUnit("Prise", units))
    }

    @Test
    fun anInactiveUnitIsNeverMatched() {
        assertNull(matchUnit("g", listOf(QuantityUnit(1, "g", active = false))))
    }

    @Test
    fun theSameProductNameIsAConfidentSuggestion() {
        val flour = product(1, "Mehl")

        val suggestion = suggestProduct("Mehl", listOf(product(2, "Mehlspeise"), flour))

        assertEquals(flour, suggestion?.product)
        assertTrue(suggestion!!.isConfident)
    }

    @Test
    fun singularAndPluralAreTheSameProduct() {
        val egg = product(1, "Ei")

        val suggestion = suggestProduct("Eier", listOf(egg))

        assertEquals(egg, suggestion?.product)
        assertTrue(suggestion!!.isConfident)
    }

    @Test
    fun aProductWhoseWordsAreAllInTheNameIsConfident() {
        val parsley = product(1, "Petersilie")

        val suggestion = suggestProduct("frische Petersilie", listOf(parsley))

        assertEquals(parsley, suggestion?.product)
        assertTrue(suggestion!!.isConfident)
    }

    @Test
    fun theMoreSpecificProductWins() {
        val oil = product(1, "Öl")
        val oliveOil = product(2, "Olivenöl")

        assertEquals(oliveOil, suggestProduct("Olivenöl", listOf(oil, oliveOil))?.product)
    }

    @Test
    fun aProductOnlyFoundInsideTheNameIsNoConfidentSuggestion() {
        val suggestion = suggestProduct("Weizenmehl", listOf(product(1, "Mehl")))

        assertFalse(suggestion!!.isConfident)
    }

    @Test
    fun inactiveProductsAreNeverSuggested() {
        assertNull(suggestProduct("Mehl", listOf(product(1, "Mehl", active = false))))
    }

    @Test
    fun anUnrelatedNameHasNoSuggestion() {
        assertNull(suggestProduct("Spaghetti", listOf(product(1, "Milch"))))
    }

    private fun product(id: Int, name: String, active: Boolean = true) =
        Product(id = id, name = name, active = active)
}
