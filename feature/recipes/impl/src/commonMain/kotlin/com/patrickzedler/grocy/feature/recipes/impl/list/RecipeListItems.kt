@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)

package com.patrickzedler.grocy.feature.recipes.impl.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.ListItemOverflowMenu
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.rememberStatusColors
import com.patrickzedler.grocy.core.forms.FieldChip
import com.patrickzedler.grocy.core.forms.FieldChipView
import com.patrickzedler.grocy.core.forms.ListDetailItem
import com.patrickzedler.grocy.core.forms.labelled
import com.patrickzedler.grocy.core.model.recipes.RecipeField
import com.patrickzedler.grocy.core.model.recipes.RecipeStatus
import com.patrickzedler.grocy.core.model.text.formatDecimal
import com.patrickzedler.grocy.core.model.text.platformDecimalSeparator
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.ic_rounded_check
import com.patrickzedler.grocy.core.resources.ic_rounded_close
import com.patrickzedler.grocy.core.resources.ic_rounded_group
import com.patrickzedler.grocy.core.resources.ic_rounded_hourglass_empty
import com.patrickzedler.grocy.core.resources.ic_rounded_priority_high
import com.patrickzedler.grocy.core.resources.products_has_picture
import com.patrickzedler.grocy.core.resources.recipes_desired_servings
import com.patrickzedler.grocy.core.resources.recipes_due_score
import com.patrickzedler.grocy.core.resources.recipes_due_score_value
import com.patrickzedler.grocy.core.resources.recipes_missing
import com.patrickzedler.grocy.core.resources.recipes_missing_on_list
import com.patrickzedler.grocy.core.resources.recipes_servings
import com.patrickzedler.grocy.core.resources.recipes_status_enough
import com.patrickzedler.grocy.feature.recipes.impl.RecipePicture
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * A row of Grocy's recipes table: the name as title, the shown fields as chips below it, the
 * menu at its end. Beside the recipe ([selected] not null) it marks the recipe shown there.
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
) {
    ListDetailItem(
        selected = selected,
        onClick = onClick,
        index = index,
        count = count,
        modifier = Modifier.fillMaxWidth(),
        supportingContent = recipeDetails(item, fields, decimals),
        // The button brings enough space of its own, unlike icons and switches
        trailingContent = { ListItemOverflowMenu(groups = menuGroups) },
    ) {
        Text(item.name)
    }
}

/**
 * A card of Grocy's recipe gallery: the picture across its top, the name and the shown fields
 * below, the menu beside the name. Beside the recipe ([selected] not null) it marks the recipe
 * shown there with an outline.
 */
@Composable
internal fun RecipeCard(
    item: RecipeListItem,
    fields: List<RecipeField>,
    decimals: Int,
    selected: Boolean?,
    onClick: () -> Unit,
    menuGroups: List<List<AppBarMenuItem>>,
    loadPicture: suspend (fileName: String) -> ByteArray?,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val pictureDescription = stringResource(Res.string.products_has_picture)
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
        .semantics { if (item.pictureFileName != null) stateDescription = pictureDescription }
    val content = @Composable {
        Column {
            item.pictureFileName?.let { fileName ->
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
 * A recipe's shown fields: servings and due score as chips that wrap into more rows, Grocy's
 * long requirements text as a status line below them; null without any. A new layout for other
 * chips: Material sizes the item by the baselines of this slot, which a reused layout kept.
 */
@Composable
private fun recipeDetails(
    item: RecipeListItem,
    fields: List<RecipeField>,
    decimals: Int,
): (@Composable () -> Unit)? {
    val chips = recipeChips(item, fields, decimals)
    val status = item.status.takeIf { RecipeField.REQUIREMENTS_FULFILLED in fields }
    if (chips.isEmpty() && status == null) return null
    return {
        key(chips.map { it.text }, status) {
            Column(
                modifier = Modifier.padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (chips.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        chips.forEach { FieldChipView(it) }
                    }
                }
                status?.let { RecipeStatusLine(it, item.missingProductsCount) }
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
            // The name is the title, the requirements are a line of their own
            RecipeField.NAME, RecipeField.REQUIREMENTS_FULFILLED -> Unit
        }
    }
    return chips
}

/** Grocy's "Requirements fulfilled" of a recipe as status line, its text with the count. */
@Composable
internal fun RecipeStatusLine(status: RecipeStatus, missingCount: Int) {
    val text = when (status) {
        RecipeStatus.ENOUGH_IN_STOCK -> stringResource(Res.string.recipes_status_enough)
        RecipeStatus.ON_SHOPPING_LIST ->
            pluralStringResource(Res.plurals.recipes_missing_on_list, missingCount, missingCount)
        RecipeStatus.NOT_ENOUGH_IN_STOCK ->
            pluralStringResource(Res.plurals.recipes_missing, missingCount, missingCount)
    }
    StatusLine(status, text)
}

/**
 * A status like Grocy's: its icon (check, exclamation, cross) in the status color before the
 * text, which wraps instead of being cut off like in a chip.
 */
@Composable
internal fun StatusLine(status: RecipeStatus, text: String, modifier: Modifier = Modifier) {
    val colors = rememberStatusColors()
    val (icon, tint) = when (status) {
        RecipeStatus.ENOUGH_IN_STOCK ->
            Res.drawable.ic_rounded_check to MaterialTheme.colorScheme.primary
        RecipeStatus.ON_SHOPPING_LIST ->
            Res.drawable.ic_rounded_priority_high to colors.warning.color
        RecipeStatus.NOT_ENOUGH_IN_STOCK -> Res.drawable.ic_rounded_close to colors.critical.color
    }
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(StatusIconSize),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// As high as a line of the body text beside it
private val StatusIconSize = 20.dp

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
