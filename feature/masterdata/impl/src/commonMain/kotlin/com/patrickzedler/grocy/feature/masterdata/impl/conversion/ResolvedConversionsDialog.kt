package com.patrickzedler.grocy.feature.masterdata.impl.conversion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import com.patrickzedler.grocy.core.design.GrocyTheme
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialog
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialogContent
import com.patrickzedler.grocy.core.design.component.SegmentedGroup
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.withTabularNumbers
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_close
import com.patrickzedler.grocy.core.resources.conversion_equation
import com.patrickzedler.grocy.core.resources.conversion_resolved
import com.patrickzedler.grocy.core.resources.conversion_resolved_none
import com.patrickzedler.grocy.core.resources.conversion_show_resolved_hint
import org.jetbrains.compose.resources.stringResource

/**
 * Grocy's "QU conversions resolved" of a product: every conversion it derived from the defined
 * ones, directly or through other units, grouped by the unit converted from.
 */
@Composable
internal fun ResolvedConversionsDialog(
    groups: List<ResolvedGroup>,
    onDismissRequest: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    ScrollableAlertDialog(onDismissRequest = onDismissRequest) {
        ResolvedConversionsContent(
            groups = groups,
            onCloseClick = {
                haptic.confirm()
                onDismissRequest()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ResolvedConversionsContent(
    groups: List<ResolvedGroup>,
    onCloseClick: () -> Unit = {},
) {
    ScrollableAlertDialogContent(
        title = { Text(stringResource(Res.string.conversion_resolved)) },
        subtitle = { Text(stringResource(Res.string.conversion_show_resolved_hint)) },
        confirmButton = {
            TextButton(
                onClick = onCloseClick,
                modifier = Modifier.clickCursor(),
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(stringResource(Res.string.action_close))
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SegmentedListDefaults.GroupSpacing),
        ) {
            if (groups.isEmpty()) {
                Text(
                    text = stringResource(Res.string.conversion_resolved_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            groups.forEach { group ->
                SegmentedGroup(title = group.from) {
                    group.conversions.forEachIndexed { index, meaning ->
                        ListItem(
                            modifier = Modifier.fillMaxWidth(),
                            shapes = ListItemDefaults.segmentedShapes(
                                index = index,
                                count = group.conversions.size,
                            ),
                            colors = SegmentedListDefaults.colors(),
                            content = { ConversionEquation(meaning) },
                        )
                    }
                }
            }
        }
    }
}

/** "1 Pack = 6 Pieces", the numbers in equally wide digits. */
@Composable
internal fun ConversionEquation(meaning: ConversionMeaning) {
    Text(
        text = stringResource(
            Res.string.conversion_equation,
            meaning.from,
            meaning.amount,
            meaning.to,
        ),
        style = LocalTextStyle.current.withTabularNumbers(),
    )
}

@Preview
@Composable
private fun ResolvedConversionsPreview() {
    GrocyTheme {
        ResolvedConversionsContent(
            groups = listOf(
                ResolvedGroup(
                    from = "Pack",
                    conversions = listOf(
                        ConversionMeaning("Pack", "6", "Pieces"),
                        ConversionMeaning("Pack", "1.5", "Liters"),
                    ),
                ),
            ),
        )
    }
}
