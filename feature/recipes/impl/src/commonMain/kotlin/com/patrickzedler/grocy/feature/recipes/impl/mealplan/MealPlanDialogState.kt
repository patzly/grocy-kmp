package com.patrickzedler.grocy.feature.recipes.impl.mealplan

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.forms.AmountUnit
import com.patrickzedler.grocy.core.forms.ChangeError
import com.patrickzedler.grocy.core.forms.FieldError
import com.patrickzedler.grocy.core.forms.FormFocusTarget
import com.patrickzedler.grocy.core.forms.ScannerFieldState
import com.patrickzedler.grocy.core.forms.UnknownProductFlow
import com.patrickzedler.grocy.core.forms.minimumAmount
import com.patrickzedler.grocy.core.forms.numberError
import com.patrickzedler.grocy.core.forms.productAmountUnits
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.ProductBarcode
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.ResolvedQuantityUnitConversion
import com.patrickzedler.grocy.core.model.recipes.MealPlanEntry
import com.patrickzedler.grocy.core.model.recipes.MealPlanEntryType
import com.patrickzedler.grocy.core.model.recipes.MealPlanSection
import com.patrickzedler.grocy.core.model.recipes.Recipe
import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.core.model.text.formatDecimal
import com.patrickzedler.grocy.core.model.text.parseDecimal
import com.patrickzedler.grocy.core.productcard.productIdForCode

/** The inputs of Grocy's meal plan dialogs that can be invalid. */
internal enum class MealPlanEntryField { DAY, RECIPE, SERVINGS, PRODUCT, AMOUNT, UNIT }

/** What the meal plan dialogs choose from, as stored. */
internal class MealPlanChoices(
    val recipes: List<Recipe>,
    val products: List<Product>,
    /** Also the inactive ones, which Grocy's picker names as not in stock. */
    val allProducts: List<Product> = products,
    val barcodes: List<ProductBarcode> = emptyList(),
    val sections: List<MealPlanSection>,
    val units: List<QuantityUnit>,
    val conversions: List<ResolvedQuantityUnitConversion>,
    val decimals: Int,
    val decimalSeparator: Char,
)

/**
 * Grocy's dialog of a meal plan entry of [type]: creates one ([original] null) on the day it
 * starts with or edits [original]. A recipe gets its servings, a product its amount in one of
 * its units (Grocy keeps it in the stock unit), a note its text; all of them a day and a
 * section.
 */
@Stable
internal class MealPlanEntryDialogState(
    val type: MealPlanEntryType,
    val original: MealPlanEntry?,
    day: String,
    val choices: MealPlanChoices,
    /** The product's scanner, as it was left in the last new product entry. */
    val scanner: ScannerFieldState = ScannerFieldState(hasValue = true),
) {
    private val decimals = choices.decimals

    val day = TextFieldState(original?.day ?: day)
    var recipeId by mutableStateOf(original?.recipeId)
        private set
    val servings = TextFieldState(number(original?.recipeServings ?: 1.0))
    var productId by mutableStateOf(original?.productId)
        private set

    // Grocy shows an entry's amount in the stock unit it keeps it in
    val amount = TextFieldState(number(original?.productAmount ?: 1.0))
    var quId by mutableStateOf(original?.productId?.let(::stockUnitOf))
    val note = TextFieldState(original?.note.orEmpty())
    var sectionId by mutableStateOf(original?.sectionId ?: MealPlanSection.DEFAULT_ID)

    var isRecipeUnmatched by mutableStateOf(false)
    var isProductUnmatched by mutableStateOf(false)

    /** Grocy's "Create or assign product"; its meal plan only assigns. */
    val unknownProduct = UnknownProductFlow(canCreate = false)
    var isUnitUnmatched by mutableStateOf(false)
    var isSectionUnmatched by mutableStateOf(false)
    var errors by mutableStateOf<Map<MealPlanEntryField, FieldError>>(emptyMap())
    var isSaving by mutableStateOf(false)
    var error by mutableStateOf<ChangeError?>(null)

    val isNew: Boolean
        get() = original == null

    val hasUnmatchedChoices: Boolean
        get() = isRecipeUnmatched || isProductUnmatched || isUnitUnmatched || isSectionUnmatched

    /** The units of the chosen product's amount, its stock unit and those it converts into. */
    val units: List<AmountUnit>
        get() {
            val product = choices.products.firstOrNull { it.id == productId } ?: return emptyList()
            return productAmountUnits(
                product.id,
                product.quIdStock,
                choices.conversions,
                choices.units,
            )
        }

    /** Like Grocy's picker: a chosen recipe brings its servings. */
    fun selectRecipe(id: Int?) {
        recipeId = id
        val recipe = choices.recipes.firstOrNull { it.id == id } ?: return
        servings.setTextAndPlaceCursorAtEnd(number(recipe.baseServings))
    }

    private fun productForCode(code: String): Product? = productIdForCode(code, choices.barcodes)
        ?.let { id -> choices.products.firstOrNull { it.id == id } }

    /** A scanned code: its product is chosen and the amount is next, else like Grocy's picker. */
    fun onScanned(code: String) {
        val product = productForCode(code)
        if (product == null) {
            unknownProduct.onTextWithoutProduct(
                code,
                false,
                choices.allProducts,
                choices.barcodes,
            )
            // A code the form does not offer keeps scanning, a question needs its answer
            if (unknownProduct.input != null) scanner.closeWithoutFocus()
            return
        }
        selectProduct(product.id)
        scanner.onScanned()
        scanner.requestFocus(FormFocusTarget.NEXT_FIELD)
    }

    /**
     * Text submitted or left standing in the product field, which a barcode reader types like a
     * keyboard: a code chooses its product, other text goes the way of Grocy's picker. The scanner
     * opening only takes the focus away, which asks nothing.
     */
    fun onProductTextSubmitted(text: String, hasSuggestion: Boolean): Boolean {
        if (scanner.isOpen) return false
        val product = productForCode(text)
        if (product != null) {
            selectProduct(product.id)
            scanner.requestFocus(FormFocusTarget.NEXT_FIELD)
            return true
        }
        val used = unknownProduct.onTextWithoutProduct(
            text,
            hasSuggestion,
            choices.allProducts,
            choices.barcodes,
        )
        // Grocy's error leaves the focus in the field for the next try
        if (unknownProduct.isNotOffered) scanner.requestFocus(FormFocusTarget.CODE_FIELD)
        return used
    }

    fun dismissUnknownProduct() {
        unknownProduct.dismiss()
        scanner.onFormReset()
    }

    /** Grocy's "Add as barcode to existing product": the user chooses the product now. */
    fun onBarcodeAssigned() {
        selectProduct(null)
        scanner.requestFocus(FormFocusTarget.CODE_FIELD)
    }

    /** Grocy's camera button in the question: on with the next code. */
    fun scanAgain() {
        selectProduct(null)
        if (!scanner.isOpen) scanner.toggle()
    }

    /** Like Grocy's picker: a chosen product starts with 1 in its stock unit. */
    fun selectProduct(id: Int?) {
        val changed = id != productId
        if (id != null) {
            unknownProduct.stopWaiting()
            scanner.onValueChosen()
        }
        productId = id
        if (id == null || !changed) return
        quId = stockUnitOf(id)
        if (isNew) amount.setTextAndPlaceCursorAtEnd(number(1.0))
    }

    /** Checks the inputs like Grocy's form; returns the entry to save, null while invalid. */
    fun validated(): MealPlanEntry? {
        val dayText = day.text.toString().trim()
        val errors = buildMap {
            when {
                dayText.isBlank() -> put(MealPlanEntryField.DAY, FieldError.Required)
                epochDayOf(dayText) == null -> put(MealPlanEntryField.DAY, FieldError.InvalidDate)
            }
            when (type) {
                MealPlanEntryType.RECIPE -> {
                    if (recipeId == null) put(MealPlanEntryField.RECIPE, FieldError.Required)
                    amountError(servings.text.toString())?.let {
                        put(MealPlanEntryField.SERVINGS, it)
                    }
                }
                MealPlanEntryType.PRODUCT -> {
                    if (productId == null) put(MealPlanEntryField.PRODUCT, FieldError.Required)
                    amountError(amount.text.toString())?.let { put(MealPlanEntryField.AMOUNT, it) }
                    if (productId != null && quId == null) {
                        put(MealPlanEntryField.UNIT, FieldError.Required)
                    }
                }
                MealPlanEntryType.NOTE -> Unit
            }
        }
        this.errors = errors
        if (errors.isNotEmpty() || hasUnmatchedChoices) return null
        val base = (original ?: MealPlanEntry(id = MasterDataRepository.NEW_OBJECT_ID, day = dayText, type = type))
            .copy(day = dayText, sectionId = sectionId)
        return when (type) {
            MealPlanEntryType.RECIPE -> base.copy(
                recipeId = recipeId,
                recipeServings = parseDecimal(servings.text.toString()) ?: return null,
            )
            MealPlanEntryType.PRODUCT -> {
                val unit = units.firstOrNull { it.unit.id == quId } ?: return null
                val typed = parseDecimal(amount.text.toString()) ?: return null
                base.copy(
                    productId = productId,
                    productAmount = typed / unit.factor,
                    productQuId = unit.unit.id,
                )
            }
            MealPlanEntryType.NOTE -> base.copy(note = note.text.toString().trim())
        }
    }

    private fun amountError(text: String) =
        numberError(text, minimumAmount(decimals), decimals, required = true)

    private fun stockUnitOf(productId: Int): Int? =
        choices.products.firstOrNull { it.id == productId }?.quIdStock

    private fun number(value: Double) = formatDecimal(value, decimals, choices.decimalSeparator)
}

/** Grocy's "Copy this day": every entry of [fromDay] once more on the chosen [toDay]. */
@Stable
internal class CopyDayDialogState(val fromDay: String) {
    val toDay = TextFieldState()
    var error by mutableStateOf<FieldError?>(null)
    var isSaving by mutableStateOf(false)
    var saveError by mutableStateOf<ChangeError?>(null)

    /** The chosen day, null with an error shown. */
    fun validated(): String? {
        val text = toDay.text.toString().trim()
        error = when {
            text.isBlank() -> FieldError.Required
            epochDayOf(text) == null -> FieldError.InvalidDate
            else -> null
        }
        return text.takeIf { error == null }
    }
}

/** Grocy's form of a meal plan section, [original] null for a new one. */
@Stable
internal class MealPlanSectionDialogState(val original: MealPlanSection?) {
    val name = TextFieldState(original?.name.orEmpty())
    val sortNumber = TextFieldState(original?.sortNumber?.toString().orEmpty())
    val timeInfo = TextFieldState(original?.timeInfo.orEmpty())
    var nameError by mutableStateOf<FieldError?>(null)
    var sortNumberError by mutableStateOf<FieldError?>(null)
    var isSaving by mutableStateOf(false)
    var error by mutableStateOf<ChangeError?>(null)

    val isNew: Boolean
        get() = original == null

    /** Grocy's checks: a name, a whole sort number from 0 on if any. */
    fun validated(): MealPlanSection? {
        val name = name.text.toString().trim()
        val sortText = sortNumber.text.toString().trim()
        val sort = sortText.toIntOrNull()?.takeIf { it >= 0 }
        nameError = FieldError.Required.takeIf { name.isEmpty() }
        sortNumberError = FieldError.InvalidNumber(min = "0", decimals = 0)
            .takeIf { sortText.isNotEmpty() && sort == null }
        if (nameError != null || sortNumberError != null) return null
        return MealPlanSection(
            id = original?.id ?: MasterDataRepository.NEW_OBJECT_ID,
            name = name,
            sortNumber = sort,
            timeInfo = timeInfo.text.toString().trim().ifEmpty { null },
        )
    }
}
