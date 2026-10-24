package com.patrickzedler.grocy.core.design.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Several lines in the supporting slot of a list item, each cut off with an ellipsis. Use it for
 * every multi-line supporting content of lazy lists.
 *
 * Material sizes an item by the first and last baseline of that slot. A lazy list reuses items,
 * and a reused column kept the baselines of its former line count, so items stayed too high;
 * another count therefore gets a column of its own.
 */
@Composable
fun SupportingLines(
    lines: List<String>,
    modifier: Modifier = Modifier,
) {
    key(lines.size) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            lines.forEach { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
    }
}
