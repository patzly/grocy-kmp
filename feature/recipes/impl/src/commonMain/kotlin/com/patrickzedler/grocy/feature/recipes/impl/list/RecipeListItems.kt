@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)

package com.patrickzedler.grocy.feature.recipes.impl.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.ListItemOverflowMenu
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.SwipeAction
import com.patrickzedler.grocy.core.design.component.SwipeActionBox
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.rememberStatusColors
import com.patrickzedler.grocy.core.forms.FieldChip
import com.patrickzedler.grocy.core.forms.FieldChipView
import com.patrickzedler.grocy.core.forms.ListDetailItem
import com.patrickzedler.grocy.core.forms.ProductThumbnail
import com.patrickzedler.grocy.core.forms.labelled
import com.patrickzedler.grocy.core.model.recipes.RecipeField
import com.patrickzedler.grocy.core.model.text.formatDecimal
import com.patrickzedler.grocy.core.model.text.platformDecimalSeparator
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.ic_rounded_add_shopping_cart
import com.patrickzedler.grocy.core.resources.ic_rounded_group
import com.patrickzedler.grocy.core.resources.ic_rounded_hourglass_empty
import com.patrickzedler.grocy.core.resources.products_has_picture
import com.patrickzedler.grocy.core.resources.recipe_add_missing_confirm
import com.patrickzedler.grocy.core.resources.recipes_desired_servings
import com.patrickzedler.grocy.core.resources.recipes_due_score
import com.patrickzedler.grocy.core.resources.recipes_due_score_value
import com.patrickzedler.grocy.core.resources.recipes_servings
import com.patrickzedler.grocy.feature.recipes.impl.RecipePicture
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * What the items offer for Grocy's "Put missing products on shopping list": [enabled] while
 * something is missing, then swiping a row towards the end runs [onClick], also its menu item and
 * accessibility action. Null without the stock or shopping list feature.
 */
internal class PutOnShoppingList(val enabled: Boolean, val onClick: () -> Unit)

/**
 * A row of Grocy's recipes table: the name as title, the shown fields as chips below it, the
 * picture (with the field shown) and the menu at its end. Swiping it towards the end puts what
 * is missing on the shopping list ([putOnShoppingList]). Beside the recipe ([selected] not null)
 * it marks the recipe shown there.
 */
@Composable
internal fun RecipeRow(
    item: RecipeListItem,
    fields: List<RecipeField>,
    decimals: Int,
    selected: Boolean?,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    menuGroups: List<List<AppBarMenuItem>>,
    putOnShoppingList: PutOnShoppingList?,
    loadPicture: suspend (fileName: String) -> ByteArray?,
) {
    val pictureDescription = stringResource(Res.string.products_has_picture)
    val putText = stringResource(Res.string.recipe_add_missing_confirm)
    val picture = item.pictureFileName?.takeIf { RecipeField.PICTURE in fields }
    SwipeActionBox(
        startAction = putOnShoppingList?.let { action ->
            SwipeAction(
                icon = painterResource(Res.drawable.ic_rounded_add_shopping_cart),
                enabled = action.enabled,
                onSwipe = action.onClick,
            )
        },
        endAction = null,
    ) {
        ListDetailItem(
            selected = selected,
            onClick = onClick,
            index = index,
            count = count,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    if (picture != null) stateDescription = pictureDescription
                    customActions = listOfNotNull(
                        CustomAccessibilityAction(putText) {
                            putOnShoppingList?.onClick?.invoke()
                            true
                        }.takeIf { putOnShoppingList?.enabled == true },
                    )
                },
            supportingContent = recipeDetails(item, fields, decimals),
            // Button and picture bring enough space of their own, unlike icons and switches
            trailingContent = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    picture?.let { ProductThumbnail(it, loadPicture) }
                    ListItemOverflowMenu(groups = menuGroups)
                }
            },
        ) {
            Text(item.name)
        }
    }
}

/**
 * A card of Grocy's recipe gallery: the picture across its top (with the field shown), the name
 * and the shown fields below, the menu beside the name. Beside the recipe ([selected] not null)
 * it marks the recipe shown there with an outline.
 */
@Composable
internal fun RecipeCard(
    item: RecipeListItem,
    fields: List<RecipeField>,
    decimals: Int,
    selected: Boolean?,
    onClick: () -> Unit,
    menuGroups: List<List<AppBarMenuItem>>,
    putOnShoppingList: PutOnShoppingList?,
    loadPicture: suspend (fileName: String) -> ByteArray?,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val pictureDescription = stringResource(Res.string.products_has_picture)
    val putText = stringResource(Res.string.recipe_add_missing_confirm)
    val picture = item.pictureFileName?.takeIf { RecipeField.PICTURE in fields }
    val details = recipeDetails(item, fields, decimals)
    val shape = MaterialTheme.shapes.large
    val click = {
        haptic.confirm()
        onClick()
    }
    val color = if (selected == true) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        SegmentedListDefaults.containerColor
    }
    val border = BorderStroke(2.dp, MaterialTheme.colorScheme.secondary).takeIf { selected == true }
    // The picture hint belongs to the card, not to a node of its own
    val cardModifier = modifier
        .clickCursor()
        .semantics {
            if (picture != null) stateDescription = pictureDescription
            // The gallery has no swipe action, but the same shortcut as the rows
            customActions = listOfNotNull(
                CustomAccessibilityAction(putText) {
                    putOnShoppingList?.onClick?.invoke()
                    true
                }.takeIf { putOnShoppingList?.enabled == true },
            )
        }
    val content = @Composable {
        Column {
            picture?.let { fileName ->
                RecipePicture(
                    fileName = fileName,
                    load = loadPicture,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 4.dp, end = 4.dp)
                        .aspectRatio(GALLERY_PICTURE_RATIO),
                )
            }
            Row(
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 4.dp, bottom = 12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f).padding(top = 10.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                    details?.invoke()
                }
                ListItemOverflowMenu(groups = menuGroups)
            }
        }
    }
    // Beside the recipe a single choice marking it, like the rows
    if (selected != null) {
        Surface(
            selected = selected,
            onClick = click,
            shape = shape,
            color = color,
            border = border,
            modifier = cardModifier,
            content = content,
        )
    } else {
        Surface(onClick = click, shape = shape, color = color, modifier = cardModifier) {
            content()
        }
    }
}

/**
 * A recipe's shown fields as chips that wrap into more rows: servings, due score, then Grocy's
 * requirements as status chips; null without any. A new layout for other chips: Material sizes
 * the item by the baselines of this slot, which a reused layout kept.
 */
@Composable
private fun recipeDetails(
    item: RecipeListItem,
    fields: List<RecipeField>,
    decimals: Int,
): (@Composable () -> Unit)? {
    val chips = recipeChips(item, fields, decimals)
    if (chips.isEmpty()) return null
    return {
        key(chips.map { it.text }) {
            FlowRow(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                chips.forEach { FieldChipView(it) }
            }
        }
    }
}

@Composable
private fun recipeChips(
    item: RecipeListItem,
    fields: List<RecipeField>,
    decimals: Int,
): List<FieldChip> {
    val colors = rememberStatusColors()
    val chips = mutableListOf<FieldChip>()
    for (field in fields) {
        when (field) {
            RecipeField.DESIRED_SERVINGS -> {
                val text = servingsText(item.desiredServings, decimals)
                chips += FieldChip(
                    text = text,
                    description = labelled(
                        stringResource(Res.string.recipes_desired_servings),
                        text,
                    ),
                    icon = Res.drawable.ic_rounded_group,
                )
            }
            RecipeField.DUE_SCORE -> item.dueScore?.let { score ->
                chips += FieldChip(
                    text = stringResource(Res.string.recipes_due_score_value, score),
                    description = labelled(
                        stringResource(Res.string.recipes_due_score),
                        score.toString(),
                    ),
                    icon = Res.drawable.ic_rounded_hourglass_empty,
                )
            }
            RecipeField.REQUIREMENTS_FULFILLED -> item.status?.let { status ->
                chips += recipeStatusChips(status, item.missingProductsCount, colors)
            }
            // The name is the title, the picture has a place of its own
            RecipeField.NAME, RecipeField.PICTURE -> Unit
        }
    }
    return chips
}

/** "2 servings", with the user's decimal places like Grocy's amounts. */
@Composable
internal fun servingsText(servings: Double, decimals: Int): String {
    val number = formatDecimal(servings, decimals, platformDecimalSeparator())
    // Fractions take the plural, like "1.5 servings"
    val quantity = if (servings == 1.0) 1 else 2
    return pluralStringResource(Res.plurals.recipes_servings, quantity, number)
}

/** Wide like a photo, so the cards of a row line up. */
private const val GALLERY_PICTURE_RATIO = 16f / 10f
