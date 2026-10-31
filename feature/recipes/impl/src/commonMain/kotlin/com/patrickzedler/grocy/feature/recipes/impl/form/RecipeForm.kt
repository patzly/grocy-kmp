package com.patrickzedler.grocy.feature.recipes.impl.form

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.patrickzedler.grocy.core.data.masterdata.PictureChange
import com.patrickzedler.grocy.core.forms.AmountUnit
import com.patrickzedler.grocy.core.forms.FieldError
import com.patrickzedler.grocy.core.forms.ObjectField
import com.patrickzedler.grocy.core.forms.ObjectFormState
import com.patrickzedler.grocy.core.forms.htmlToPlainText
import com.patrickzedler.grocy.core.forms.minimumAmount
import com.patrickzedler.grocy.core.forms.numberError
import com.patrickzedler.grocy.core.forms.plainTextToHtml
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.recipes.Recipe
import com.patrickzedler.grocy.core.model.recipes.RecipeIngredient
import com.patrickzedler.grocy.core.model.recipes.RecipeNesting
import com.patrickzedler.grocy.core.model.text.formatDecimal
import com.patrickzedler.grocy.core.model.text.parseDecimal
import kotlin.math.pow
import kotlin.math.roundToLong

/** The fields of Grocy's recipe form as typed. */
internal data class RecipeFormValues(
    val name: String = "",
    val baseServings: String = "1",
    val notCheckShoppingList: Boolean = false,
    val productId: Int? = null,
    /** Grocy's "Preparation" as plain text. */
    val preparation: String = "",
    val picture: PictureChange = PictureChange.Keep,
)

/** Grocy's checks of the recipe form: a name, servings above zero. */
internal fun RecipeFormValues.validate(decimals: Int): Map<ObjectField, FieldError> = buildMap {
    if (name.isBlank()) put(ObjectField.NAME, FieldError.Required)
    numberError(baseServings, minimumAmount(decimals), decimals, required = true)?.let {
        put(ObjectField.AMOUNT, it)
    }
}

/**
 * The recipe the form saves as [id]; an unchanged preparation keeps its HTML (the form edits
 * plain text), the desired servings and the picture stay as they are, a new recipe gets
 * Grocy's single desired serving.
 */
internal fun RecipeFormValues.toRecipe(id: Int, original: Recipe?): Recipe {
    val keepsDescription = original != null &&
        htmlToPlainText(original.description.orEmpty()) == preparation.trim()
    return Recipe(
        id = id,
        name = name.trim(),
        description = if (keepsDescription) original.description else plainTextToHtml(preparation),
        pictureFileName = original?.pictureFileName,
        baseServings = parseDecimal(baseServings) ?: 1.0,
        desiredServings = original?.desiredServings ?: 1.0,
        notCheckShoppingList = notCheckShoppingList,
        productId = productId,
        userfields = original?.userfields.orEmpty(),
    )
}

internal fun Recipe.toFormValues(decimals: Int, decimalSeparator: Char) = RecipeFormValues(
    name = name,
    baseServings = formatDecimal(baseServings, decimals, decimalSeparator),
    notCheckShoppingList = notCheckShoppingList,
    productId = productId,
    preparation = htmlToPlainText(description.orEmpty()),
)

/** The editable recipe form; [original] is the saved recipe, null for a new one. */
@Stable
internal class RecipeFormState(
    initial: RecipeFormValues,
    original: Recipe?,
    /** The products Grocy's "Produces product" offers. */
    val products: List<Product>,
) : ObjectFormState<RecipeFormValues> {
    var original by mutableStateOf(original)

    val name = TextFieldState(initial.name)
    val baseServings = TextFieldState(initial.baseServings)
    var notCheckShoppingList by mutableStateOf(initial.notCheckShoppingList)
    var productId by mutableStateOf(initial.productId)
    var isProductUnmatched by mutableStateOf(false)
    val preparation = TextFieldState(initial.preparation)

    /** The picture the recipe will have, applied when it is saved like in Grocy. */
    var picture by mutableStateOf(initial.picture)

    override val hasUnmatchedChoices: Boolean
        get() = isProductUnmatched

    override fun values() = RecipeFormValues(
        name = name.text.toString(),
        baseServings = baseServings.text.toString(),
        notCheckShoppingList = notCheckShoppingList,
        productId = productId,
        preparation = preparation.text.toString(),
        picture = picture,
    )
}

/** The inputs of Grocy's ingredient form that can be invalid. */
internal enum class IngredientField { PRODUCT, AMOUNT, UNIT, PRICE_FACTOR }

/** The fields of Grocy's ingredient form as typed; [amount] is in unit [quId]. */
internal data class IngredientFormValues(
    val productId: Int? = null,
    val onlyCheckSingleUnitInStock: Boolean = false,
    val amount: String = "",
    val quId: Int? = null,
    val variableAmount: String = "",
    val roundUp: Boolean = false,
    val notCheckStockFulfillment: Boolean = false,
    val group: String = "",
    val note: String = "",
    val priceFactor: String = "1",
)

/**
 * Grocy's checks of its ingredient form: a product and a unit, an amount (above zero only for
 * "Only check if any amount is in stock", Grocy's picker allows 0 otherwise) and a price factor
 * above zero.
 */
internal fun IngredientFormValues.validate(decimals: Int): Map<IngredientField, FieldError> =
    buildMap {
        if (productId == null) put(IngredientField.PRODUCT, FieldError.Required)
        val min = if (onlyCheckSingleUnitInStock) minimumAmount(decimals) else 0.0
        numberError(amount, min, decimals, required = true)?.let {
            put(IngredientField.AMOUNT, it)
        }
        if (quId == null) put(IngredientField.UNIT, FieldError.Required)
        numberError(priceFactor, minimumAmount(decimals), decimals, required = true)?.let {
            put(IngredientField.PRICE_FACTOR, it)
        }
    }

/**
 * The ingredient the form saves: Grocy keeps the amount in the product's stock unit and the
 * unit only to show it, so the typed amount is divided by the unit's [AmountUnit.factor] and
 * rounded to the [decimals] like Grocy's picker (at least one); with "Only check if any amount
 * is in stock" any unit is allowed and the amount stays as typed.
 * Round up is only sent where the server knows it ([original] has it, or it is switched on).
 */
internal fun IngredientFormValues.toIngredient(
    id: Int,
    recipeId: Int,
    units: List<AmountUnit>,
    original: RecipeIngredient?,
    supportsRoundUp: Boolean,
    decimals: Int,
): RecipeIngredient {
    val typed = parseDecimal(amount) ?: 0.0
    val factor = if (onlyCheckSingleUnitInStock) {
        1.0
    } else {
        units.firstOrNull { it.unit.id == quId }?.factor ?: 1.0
    }
    return RecipeIngredient(
        id = id,
        recipeId = recipeId,
        productId = productId ?: 0,
        amount = roundedTo(typed / factor, decimals.coerceAtLeast(1)),
        quId = quId,
        note = note.trim().ifEmpty { null },
        onlyCheckSingleUnitInStock = onlyCheckSingleUnitInStock,
        ingredientGroup = group.trim().ifEmpty { null },
        notCheckStockFulfillment = notCheckStockFulfillment,
        variableAmount = variableAmount.trim().ifEmpty { null },
        priceFactor = parseDecimal(priceFactor) ?: 1.0,
        roundUp = roundUp.takeIf { supportsRoundUp || original?.roundUp != null || it },
    )
}

private fun roundedTo(value: Double, decimals: Int): Double {
    val scale = 10.0.pow(decimals)
    return (value * scale).roundToLong() / scale
}

/** The form of a saved ingredient, its amount in its own unit (see [toIngredient]). */
internal fun RecipeIngredient.toFormValues(
    units: List<AmountUnit>,
    decimals: Int,
    decimalSeparator: Char,
): IngredientFormValues {
    val factor = if (onlyCheckSingleUnitInStock) {
        1.0
    } else {
        units.firstOrNull { it.unit.id == quId }?.factor ?: 1.0
    }
    return IngredientFormValues(
        productId = productId,
        onlyCheckSingleUnitInStock = onlyCheckSingleUnitInStock,
        amount = formatDecimal(amount * factor, decimals, decimalSeparator),
        quId = quId,
        variableAmount = variableAmount.orEmpty(),
        roundUp = roundUp ?: false,
        notCheckStockFulfillment = notCheckStockFulfillment,
        group = ingredientGroup.orEmpty(),
        note = note.orEmpty(),
        priceFactor = formatDecimal(priceFactor, decimals, decimalSeparator),
    )
}

/** The fields of Grocy's included recipe form as typed. */
internal data class NestingFormValues(val recipeId: Int? = null, val servings: String = "1")

/** Grocy's checks: a recipe (another one than the edited) and servings above zero. */
internal fun NestingFormValues.validate(decimals: Int, ownRecipeId: Int) = buildMap {
    if (recipeId == null || recipeId == ownRecipeId) put(ObjectField.NAME, FieldError.Required)
    numberError(servings, minimumAmount(decimals), decimals, required = true)?.let {
        put(ObjectField.AMOUNT, it)
    }
}

internal fun NestingFormValues.toNesting(id: Int, ownRecipeId: Int) = RecipeNesting(
    id = id,
    recipeId = ownRecipeId,
    includesRecipeId = recipeId ?: 0,
    servings = parseDecimal(servings) ?: 1.0,
)
