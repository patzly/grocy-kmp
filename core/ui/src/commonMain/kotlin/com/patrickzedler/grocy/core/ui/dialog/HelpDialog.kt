package com.patrickzedler.grocy.core.ui.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.GrocyTheme
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialog
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialogContent
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_close
import com.patrickzedler.grocy.core.resources.help_placeholder
import com.patrickzedler.grocy.core.resources.help_placeholder_answer
import com.patrickzedler.grocy.core.resources.title_help
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private class HelpItem(
    val question: StringResource,
    val answer: StringResource,
)

private val HelpItems = listOf(
    HelpItem(Res.string.help_placeholder, Res.string.help_placeholder_answer),
)

@Composable
internal fun HelpDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current

    ScrollableAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        HelpDialogContent(
            onItemClick = { haptic.confirm() },
            onCloseClick = {
                haptic.confirm()
                onDismissRequest()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HelpDialogContent(
    modifier: Modifier = Modifier,
    onItemClick: () -> Unit = {},
    onCloseClick: () -> Unit = {},
) {
    var expandedIndices by rememberSaveable { mutableStateOf(setOf<Int>()) }

    ScrollableAlertDialogContent(
        modifier = modifier,
        title = { Text(stringResource(Res.string.title_help)) },
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
        val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<androidx.compose.ui.unit.IntSize>()
        val colors = SegmentedListDefaults.colors()

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            itemsIndexed(
                items = HelpItems,
                key = { _, item -> item.question.key },
            ) { index, item ->
                val expanded = index in expandedIndices
                SegmentedListItem(
                    onClick = {
                        onItemClick()
                        expandedIndices = if (expanded) {
                            expandedIndices - index
                        } else {
                            expandedIndices + index
                        }
                    },
                    shapes = ListItemDefaults.segmentedShapes(
                        index = index,
                        count = HelpItems.size,
                    ),
                    colors = colors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickCursor(),
                    contentPadding = PaddingValues(16.dp),
                    supportingContent = {
                        AnimatedVisibility(
                            visible = expanded,
                            enter = expandVertically(
                                animationSpec = spatialSpec,
                                expandFrom = Alignment.Top,
                            ),
                            exit = shrinkVertically(
                                animationSpec = spatialSpec,
                                shrinkTowards = Alignment.Top,
                            ),
                        ) {
                            Text(
                                text = stringResource(item.answer),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    },
                    content = { Text(stringResource(item.question)) },
                )
            }
        }
    }
}

@Preview
@Composable
private fun HelpDialogPreview() {
    GrocyTheme {
        HelpDialogContent()
    }
}
