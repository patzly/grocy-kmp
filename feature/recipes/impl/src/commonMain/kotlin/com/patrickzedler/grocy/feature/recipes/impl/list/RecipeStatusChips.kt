package com.patrickzedler.grocy.feature.recipes.impl.list

import androidx.compose.runtime.Composable
import com.patrickzedler.grocy.core.design.component.StatusColors
import com.patrickzedler.grocy.core.forms.FieldChip
import com.patrickzedler.grocy.core.model.recipes.RecipeStatus
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.ic_rounded_dangerous
import com.patrickzedler.grocy.core.resources.ic_rounded_priority
import com.patrickzedler.grocy.core.resources.ic_rounded_shopping_cart
import com.patrickzedler.grocy.core.resources.ic_rounded_warning
import com.patrickzedler.grocy.core.resources.recipes_missing
import com.patrickzedler.grocy.core.resources.recipes_missing_on_list
import com.patrickzedler.grocy.core.resources.recipes_missing_short
import com.patrickzedler.grocy.core.resources.recipes_on_shopping_list
import com.patrickzedler.grocy.core.resources.recipes_status_enough
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** The chips a status of Grocy's "Requirements fulfilled" shows, in their order. */
internal enum class StatusChip {
    /** Green: enough in stock. */
    ENOUGH,

    /** Yellow: something is missing, but the shopping list covers it. */
    MISSING_ON_LIST,

    /** Red: something is missing and not on the shopping list. */
    MISSING,

    /** Blue, after the others: something is on the shopping list. */
    ON_SHOPPING_LIST,
}

/**
 * The chips of [status]: its color first, then "On shopping list" while the shopping list
 * covers the missing part ([isOnShoppingList] for an ingredient that is only partly on it).
 */
internal fun statusChipsOf(
    status: RecipeStatus,
    isOnShoppingList: Boolean = status == RecipeStatus.ON_SHOPPING_LIST,
): List<StatusChip> = listOfNotNull(
    when (status) {
        RecipeStatus.ENOUGH_IN_STOCK -> StatusChip.ENOUGH
        RecipeStatus.ON_SHOPPING_LIST -> StatusChip.MISSING_ON_LIST
        RecipeStatus.NOT_ENOUGH_IN_STOCK -> StatusChip.MISSING
    },
    StatusChip.ON_SHOPPING_LIST.takeIf {
        isOnShoppingList && status != RecipeStatus.ENOUGH_IN_STOCK
    },
)

/**
 * A chip of [statusChipsOf] with its text and the icon of its color: priority, warning,
 * dangerous, the cart. [description] tells screen readers Grocy's whole status.
 */
internal fun statusFieldChip(
    chip: StatusChip,
    text: String,
    description: String,
    colors: StatusColors,
): FieldChip = when (chip) {
    StatusChip.ENOUGH ->
        FieldChip(text, description, Res.drawable.ic_rounded_priority, colors.positive)
    StatusChip.MISSING_ON_LIST ->
        FieldChip(text, description, Res.drawable.ic_rounded_warning, colors.warning)
    StatusChip.MISSING ->
        FieldChip(text, description, Res.drawable.ic_rounded_dangerous, colors.critical)
    StatusChip.ON_SHOPPING_LIST ->
        FieldChip(text, description, Res.drawable.ic_rounded_shopping_cart, colors.info)
}

/** A recipe's "Requirements fulfilled" as chips: the status, "On shopping list" after it. */
@Composable
internal fun recipeStatusChips(
    status: RecipeStatus,
    missingCount: Int,
    colors: StatusColors,
): List<FieldChip> {
    val onList = stringResource(Res.string.recipes_on_shopping_list)
    // Short for the chips, screen readers get the whole sentence
    val missing =
        pluralStringResource(Res.plurals.recipes_missing_short, missingCount, missingCount)
    val description = when (status) {
        RecipeStatus.ENOUGH_IN_STOCK -> stringResource(Res.string.recipes_status_enough)
        RecipeStatus.ON_SHOPPING_LIST ->
            pluralStringResource(Res.plurals.recipes_missing_on_list, missingCount, missingCount)
        RecipeStatus.NOT_ENOUGH_IN_STOCK ->
            pluralStringResource(Res.plurals.recipes_missing, missingCount, missingCount)
    }
    return statusChipsOf(status).map { chip ->
        when (chip) {
            StatusChip.ENOUGH -> statusFieldChip(chip, description, description, colors)
            StatusChip.MISSING_ON_LIST, StatusChip.MISSING ->
                statusFieldChip(chip, missing, description, colors)
            StatusChip.ON_SHOPPING_LIST -> statusFieldChip(chip, onList, onList, colors)
        }
    }
}
