@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.patrickzedler.grocy.feature.recipes.impl.recipe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import com.patrickzedler.grocy.core.design.component.LeadingTrailingContentWrapper
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialog
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialogContent
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.forms.DeleteDialog
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_cancel
import com.patrickzedler.grocy.core.resources.recipe_add_missing_confirm
import com.patrickzedler.grocy.core.resources.recipe_add_missing_hint
import com.patrickzedler.grocy.core.resources.recipe_add_missing_question
import com.patrickzedler.grocy.core.resources.recipe_consume_confirm
import com.patrickzedler.grocy.core.resources.recipe_consume_hint
import com.patrickzedler.grocy.core.resources.recipe_consume_question
import org.jetbrains.compose.resources.stringResource

/** Grocy's question before consuming every ingredient of the recipe. */
@Composable
internal fun ConsumeDialog(
    recipeName: String,
    onConsume: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    DeleteDialog(
        question = stringResource(Res.string.recipe_consume_question, recipeName),
        hint = stringResource(Res.string.recipe_consume_hint),
        onDelete = onConsume,
        onDismissRequest = onDismissRequest,
        confirmText = stringResource(Res.string.recipe_consume_confirm),
    )
}

/**
 * Grocy's question before putting the missing products on the shopping list, each of them a
 * checked choice; unchecked ones stay off the list.
 */
@Composable
internal fun AddMissingDialog(
    recipeName: String,
    products: List<MissingProduct>,
    onAdd: (excludedProductIds: List<Int>) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    // Saveable as a list, so the choice survives rotating the device
    var excluded by rememberSaveable { mutableStateOf(emptyList<Int>()) }

    ScrollableAlertDialog(onDismissRequest = onDismissRequest) {
        ScrollableAlertDialogContent(
            title = { Text(stringResource(Res.string.recipe_add_missing_question, recipeName)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptic.confirm()
                        onAdd(excluded)
                    },
                    // Nothing to put on the list once every product is unchecked
                    enabled = excluded.size < products.size,
                    modifier = Modifier.clickCursor(enabled = excluded.size < products.size),
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(Res.string.recipe_add_missing_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        haptic.confirm()
                        onDismissRequest()
                    },
                    modifier = Modifier.clickCursor(),
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                Text(
                    text = stringResource(Res.string.recipe_add_missing_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                products.forEachIndexed { index, product ->
                    val checked = product.productId !in excluded
                    val toggle = {
                        haptic.confirm()
                        excluded = if (checked) {
                            excluded + product.productId
                        } else {
                            excluded - product.productId
                        }
                    }
                    SegmentedListItem(
                        checked = checked,
                        onCheckedChange = { toggle() },
                        shapes = ListItemDefaults.segmentedShapes(index, products.size),
                        colors = SegmentedListDefaults.colors(),
                        modifier = Modifier.fillMaxWidth().clickCursor(),
                        verticalAlignment = Alignment.CenterVertically,
                        leadingContent = {
                            LeadingTrailingContentWrapper {
                                // The item toggles, the box only shows it
                                Checkbox(checked = checked, onCheckedChange = null)
                            }
                        },
                    ) {
                        Text(product.name)
                    }
                }
            }
        }
    }
}

