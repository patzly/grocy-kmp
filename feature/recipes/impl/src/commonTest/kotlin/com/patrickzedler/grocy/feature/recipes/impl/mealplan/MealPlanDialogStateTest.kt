package com.patrickzedler.grocy.feature.recipes.impl.mealplan

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.forms.FieldError
import com.patrickzedler.grocy.core.forms.FormFocusTarget
import com.patrickzedler.grocy.core.forms.ScannerFieldState
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.ProductBarcode
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.ResolvedQuantityUnitConversion
import com.patrickzedler.grocy.core.model.recipes.MealPlanEntry
import com.patrickzedler.grocy.core.model.recipes.MealPlanEntryType
import com.patrickzedler.grocy.core.model.recipes.MealPlanSection
import com.patrickzedler.grocy.core.model.recipes.Recipe
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MealPlanDialogStateTest {

    private val piece = QuantityUnit(1, "Piece", "Pieces")
    private val pack = QuantityUnit(2, "Pack", "Packs")
    private val choices = MealPlanChoices(
        recipes = listOf(Recipe(1, "Pancakes", baseServings = 4.0)),
        products = listOf(Product(id = 1, name = "Milk", quIdStock = 1)),
        allProducts = listOf(
            Product(id = 1, name = "Milk", quIdStock = 1),
            Product(id = 2, name = "Old", quIdStock = 1, active = false),
        ),
        barcodes = listOf(
            ProductBarcode(1, productId = 1, barcode = "4001"),
            ProductBarcode(2, productId = 2, barcode = "4002"),
        ),
        sections = listOf(MealPlanSection(MealPlanSection.DEFAULT_ID, "", -1)),
        units = listOf(piece, pack),
        // 1 Piece is 1/6 Pack
        conversions = listOf(
            ResolvedQuantityUnitConversion(productId = 1, fromQuId = 1, toQuId = 2, factor = 1.0 / 6),
        ),
        decimals = 2,
        decimalSeparator = '.',
    )

    private fun dialog(type: MealPlanEntryType, original: MealPlanEntry? = null) =
        MealPlanEntryDialogState(type, original, day = "2026-11-02", choices = choices)

    private fun scanning() = MealPlanEntryDialogState(
        MealPlanEntryType.PRODUCT,
        original = null,
        day = "2026-11-02",
        choices = choices,
        scanner = ScannerFieldState(hasValue = false).also { it.toggle() },
    )

    @Test
    fun aBarcodeTypedIntoTheProductFieldChoosesItsProduct() {
        val state = dialog(MealPlanEntryType.PRODUCT)

        assertTrue(state.onProductTextSubmitted("4001", hasSuggestion = false))

        assertEquals(1, state.productId)
        assertNull(state.unknownProduct.input)
    }

    @Test
    fun unknownTextAsksGrocysQuestionWhichOnlyAssigns() {
        val state = dialog(MealPlanEntryType.PRODUCT)

        assertTrue(state.onProductTextSubmitted("Brand new", hasSuggestion = false))

        assertEquals("Brand new", state.unknownProduct.input)
        assertFalse(state.unknownProduct.canCreate)
    }

    @Test
    fun anInactiveProductsNameIsGrocysErrorNotTheQuestion() {
        val state = dialog(MealPlanEntryType.PRODUCT)

        assertTrue(state.onProductTextSubmitted("Old", hasSuggestion = false))

        assertTrue(state.unknownProduct.isNotOffered)
        assertNull(state.unknownProduct.input)
        assertNull(state.productId)
    }

    @Test
    fun aScannedInactiveProductKeepsScanningWithGrocysError() {
        val state = scanning()

        state.onScanned("4002")

        assertTrue(state.unknownProduct.isNotOffered)
        assertTrue(state.scanner.isOpen)
    }

    @Test
    fun aScannedProductClosesTheScannerForTheAmount() {
        val state = scanning()

        state.onScanned("4001")

        assertEquals(1, state.productId)
        assertFalse(state.scanner.isOpen)
        assertEquals(FormFocusTarget.NEXT_FIELD, state.scanner.focusTarget)
    }

    @Test
    fun anUnknownScanClosesTheScannerForTheQuestion() {
        val state = scanning()

        state.onScanned("999")

        assertEquals("999", state.unknownProduct.input)
        assertFalse(state.scanner.isOpen)
    }

    @Test
    fun scanAgainOpensTheScannerWithoutTheText() {
        val state = scanning()
        state.onScanned("999")

        state.unknownProduct.scanAgain()
        state.scanAgain()

        assertNull(state.unknownProduct.input)
        assertTrue(state.scanner.isOpen)
    }

    @Test
    fun textLeftBehindForTheScannerAsksNothing() {
        val state = scanning()

        assertFalse(state.onProductTextSubmitted("Unfinis", hasSuggestion = false))

        assertNull(state.unknownProduct.input)
    }

    @Test
    fun textASuggestionWouldTakeIsLeftToIt() {
        val state = dialog(MealPlanEntryType.PRODUCT)

        assertFalse(state.onProductTextSubmitted("Mil", hasSuggestion = true))

        assertNull(state.unknownProduct.input)
    }

    @Test
    fun aNewEntryStartsOnItsDayInTheDefaultSection() {
        val state = dialog(MealPlanEntryType.NOTE)

        assertEquals("2026-11-02", state.day.text.toString())
        assertEquals(MealPlanSection.DEFAULT_ID, state.sectionId)
    }

    @Test
    fun aChosenRecipeBringsItsServingsLikeGrocysPicker() {
        val state = dialog(MealPlanEntryType.RECIPE)

        state.selectRecipe(1)

        assertEquals("4", state.servings.text.toString())
        assertEquals(4.0, state.validated()?.recipeServings)
    }

    @Test
    fun aRecipeIsRequired() {
        val state = dialog(MealPlanEntryType.RECIPE)

        assertNull(state.validated())

        assertEquals(FieldError.Required, state.errors[MealPlanEntryField.RECIPE])
    }

    @Test
    fun anInvalidDayIsRefused() {
        val state = dialog(MealPlanEntryType.NOTE)
        state.day.setTextAndPlaceCursorAtEnd("2.11.2026")

        assertNull(state.validated())

        assertEquals(FieldError.InvalidDate, state.errors[MealPlanEntryField.DAY])
    }

    @Test
    fun aProductsAmountIsKeptInItsStockUnit() {
        val state = dialog(MealPlanEntryType.PRODUCT)
        state.selectProduct(1)
        state.amount.setTextAndPlaceCursorAtEnd("2")
        state.quId = pack.id

        val entry = state.validated()

        // 2 Packs are 12 Pieces, Grocy keeps the unit they were entered in
        assertEquals(12.0, entry?.productAmount!!, 1e-9)
        assertEquals(pack.id, entry.productQuId)
        assertEquals(MasterDataRepository.NEW_OBJECT_ID, entry.id)
    }

    @Test
    fun aChosenProductStartsWithOneOfItsStockUnit() {
        val state = dialog(MealPlanEntryType.PRODUCT)

        state.selectProduct(1)

        assertEquals("1", state.amount.text.toString())
        assertEquals(piece.id, state.quId)
        assertEquals(listOf("Pack", "Piece"), state.units.map { it.unit.name })
    }

    @Test
    fun anEditedEntryKeepsWhatTheDialogDoesNotShow() {
        val original = MealPlanEntry(
            id = 4,
            day = "2026-11-03",
            type = MealPlanEntryType.NOTE,
            note = "Eat out",
            sectionId = 1,
            done = true,
        )
        val state = dialog(MealPlanEntryType.NOTE, original)
        state.note.setTextAndPlaceCursorAtEnd("Eat at home")

        val entry = state.validated()

        assertEquals(original.copy(note = "Eat at home"), entry)
    }

    @Test
    fun anEditedProductShowsItsAmountInTheStockUnit() {
        val original = MealPlanEntry(
            id = 5,
            day = "2026-11-03",
            type = MealPlanEntryType.PRODUCT,
            productId = 1,
            productAmount = 12.0,
            productQuId = 2,
        )

        val state = dialog(MealPlanEntryType.PRODUCT, original)

        assertEquals("12", state.amount.text.toString())
        assertEquals(piece.id, state.quId)
    }

    @Test
    fun anUnmatchedChoiceKeepsTheDialogOpen() {
        val state = dialog(MealPlanEntryType.NOTE)
        state.isSectionUnmatched = true

        assertNull(state.validated())
    }

    @Test
    fun copyingADayNeedsAValidDay() {
        val state = CopyDayDialogState("2026-11-02")

        assertNull(state.validated())
        assertEquals(FieldError.Required, state.error)

        state.toDay.setTextAndPlaceCursorAtEnd("2026-11-09")
        assertEquals("2026-11-09", state.validated())
        assertNull(state.error)
    }

    @Test
    fun aSectionNeedsANameAndAWholeSortNumber() {
        val state = MealPlanSectionDialogState(original = null)
        state.sortNumber.setTextAndPlaceCursorAtEnd("1.5")

        assertNull(state.validated())

        assertEquals(FieldError.Required, state.nameError)
        assertEquals(FieldError.InvalidNumber("0", 0), state.sortNumberError)
    }

    @Test
    fun aSectionWithoutTimeHasNone() {
        val state = MealPlanSectionDialogState(MealPlanSection(3, "Dinner", 2, "19:00"))
        state.timeInfo.setTextAndPlaceCursorAtEnd(" ")
        state.sortNumber.setTextAndPlaceCursorAtEnd("")

        assertEquals(MealPlanSection(3, "Dinner", null, null), state.validated())
    }
}
