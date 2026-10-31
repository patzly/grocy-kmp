package com.patrickzedler.grocy.feature.shoppinglist.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.GrocyTheme
import com.patrickzedler.grocy.core.design.component.LeadingTrailingContentWrapper
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialog
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialogContent
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.dialogListScrollbar
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_close
import com.patrickzedler.grocy.core.resources.shopping_list_choose
import com.patrickzedler.grocy.core.resources.shopping_list_list_with_count
import org.jetbrains.compose.resources.stringResource

/**
 * Grocy's list selector as single choice like the language dialog: every list by name with
 * the number of its items, "Name (count)" like Grocy's options. Choosing one closes it.
 */
@Composable
internal fun ShoppingListChoiceDialog(
    lists: List<ShoppingListChoice>,
    selectedId: Int?,
    onSelect: (Int) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    ScrollableAlertDialog(onDismissRequest = onDismissRequest) {
        ShoppingListChoiceContent(
            lists = lists,
            selectedId = selectedId,
            onListClick = { id ->
                haptic.confirm()
                if (id == selectedId) onDismissRequest() else onSelect(id)
            },
            onCloseClick = {
                haptic.confirm()
                onDismissRequest()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ShoppingListChoiceContent(
    lists: List<ShoppingListChoice>,
    selectedId: Int?,
    modifier: Modifier = Modifier,
    onListClick: (Int) -> Unit = {},
    onCloseClick: () -> Unit = {},
) {
    ScrollableAlertDialogContent(
        modifier = modifier,
        title = { Text(stringResource(Res.string.shopping_list_choose)) },
        confirmButton = {
            TextButton(
                onClick = onCloseClick,
                modifier = Modifier.clickCursor(),
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(stringResource(Res.string.action_close))
            }
        },
        isScrollableControlledByContent = true,
    ) {
        val colors = SegmentedListDefaults.colors()
        // The shown list is in view right away, also far down the list
        val listState = rememberLazyListState(
            initialFirstVisibleItemIndex = lists.indexOfFirst { it.id == selectedId }
                .coerceAtLeast(0),
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .dialogListScrollbar(listState),
            state = listState,
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            itemsIndexed(items = lists, key = { _, list -> list.id }) { index, list ->
                val selected = list.id == selectedId
                SegmentedListItem(
                    selected = selected,
                    onClick = { onListClick(list.id) },
                    shapes = ListItemDefaults.segmentedShapes(index = index, count = lists.size),
                    colors = colors,
                    modifier = Modifier.fillMaxWidth().clickCursor(),
                    verticalAlignment = Alignment.CenterVertically,
                    leadingContent = {
                        LeadingTrailingContentWrapper {
                            // The item handles the click, the radio button only shows the choice
                            RadioButton(
                                selected = selected,
                                modifier = Modifier.clickCursor(),
                                onClick = null,
                            )
                        }
                    },
                    content = {
                        Text(
                            stringResource(
                                Res.string.shopping_list_list_with_count,
                                list.name,
                                list.itemCount,
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Preview
@Composable
private fun ShoppingListChoiceDialogPreview() {
    GrocyTheme {
        ShoppingListChoiceContent(
            lists = listOf(
                ShoppingListChoice(2, "Hardware store", 0),
                ShoppingListChoice(1, "Shopping list", 4),
            ),
            selectedId = 1,
        )
    }
}
