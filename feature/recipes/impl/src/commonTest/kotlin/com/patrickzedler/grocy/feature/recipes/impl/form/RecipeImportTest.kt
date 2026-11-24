package com.patrickzedler.grocy.feature.recipes.impl.form

import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.ResolvedQuantityUnitConversion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecipeImportTest {

    private val gram = QuantityUnit(1, "Gram", "Grams")
    private val pack = QuantityUnit(2, "Pack", "Packs")
    private val piece = QuantityUnit(3, "Piece", "Pieces")
    private val flour = Product(id = 1, name = "Mehl", quIdStock = 2)
    private val egg = Product(id = 2, name = "Ei", quIdStock = 3)
    // A pack of flour is 1000 grams
    private val conversions = listOf(
        ResolvedQuantityUnitConversion(productId = 1, fromQuId = 2, toQuId = 2, factor = 1.0),
        ResolvedQuantityUnitConversion(productId = 1, fromQuId = 2, toQuId = 1, factor = 1000.0),
        ResolvedQuantityUnitConversion(productId = 2, fromQuId = 3, toQuId = 3, factor = 1.0),
    )

    private fun lines(vararg texts: String) = importLinesOf(
        lines = texts.toList(),
        products = listOf(flour, egg),
        units = listOf(gram, pack, piece),
        conversions = conversions,
        decimals = 2,
        decimalSeparator = ',',
    )

    @Test
    fun aKnownProductInAUnitItConvertsIntoIsReady() {
        val line = lines("300 g Mehl").single()

        assertEquals(1, line.productId)
        assertEquals("300", line.amount)
        assertEquals(1, line.quId)
        assertTrue(line.isReady)
        assertEquals("Mehl · 300 Gram", line.proposal())
    }

    @Test
    fun withoutUnitTheProductsStockUnitIsTaken() {
        val line = lines("2 Eier").single()

        assertEquals(2, line.productId)
        assertEquals(3, line.quId)
        assertTrue(line.isReady)
    }

    @Test
    fun aUnitTheProductDoesNotConvertIntoNeedsAChoice() {
        val line = lines("2 EL Mehl").single()

        assertEquals(1, line.productId)
        assertNull(line.quId)
        assertFalse(line.isReady)
    }

    @Test
    fun aLineWithoutAmountNeedsAChoice() {
        assertFalse(lines("Mehl zum Bestäuben").single().isReady)
    }

    @Test
    fun aLineWithoutProductHasNoProposal() {
        val line = lines("1 Prise Salz").single()

        assertNull(line.productId)
        assertNull(line.proposal())
    }

    @Test
    fun decimalsAreWrittenLikeTheApp() {
        assertEquals("0,5", lines("0.5 kg Mehl").single().amount)
    }

    @Test
    fun theLinesKeepTheirPlaceAsKey() {
        assertEquals(listOf(0, 1), lines("300 g Mehl", "2 Eier").map { it.key })
    }

    @Test
    fun theFormOfALineHasItsProposalAndNote() {
        val values = lines("2 Eier (Größe M)").single().toFormValues()

        assertEquals(
            IngredientFormValues(productId = 2, amount = "2", quId = 3, note = "Größe M"),
            values,
        )
    }

    @Test
    fun thePictureKeepsItsNameWithAnExtensionGrocyShows() {
        assertEquals("kuchen.jpg", pictureName("https://img.x.de/a/kuchen.jpg?w=960"))
        assertEquals("crop-960x540.jpg", pictureName("https://img.x.de/crop-960x540"))
        assertEquals("recipe.jpg", pictureName("https://img.x.de/"))
    }

    @Test
    fun theSiteIsNamedWithoutWww() {
        assertEquals("chefkoch.de", hostOf("https://www.chefkoch.de/rezepte/1.html"))
    }

    @Test
    fun minutesReadLikeAClock() {
        assertEquals("45 min", formatMinutes(45, "%1\$d min", "%1\$d h", "%1\$d h %2\$d min"))
        assertEquals("2 h", formatMinutes(120, "%1\$d min", "%1\$d h", "%1\$d h %2\$d min"))
        assertEquals("1 h 15 min", formatMinutes(75, "%1\$d min", "%1\$d h", "%1\$d h %2\$d min"))
    }
}
