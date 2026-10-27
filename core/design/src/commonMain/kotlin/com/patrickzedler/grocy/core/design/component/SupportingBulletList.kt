package com.patrickzedler.grocy.core.design.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

/**
 * A short text and a bulleted list below it in the supporting slot of a list item, in the slot's
 * style. Lines wrap; like [SupportingLines] another number of lines gets a layout of its own, so
 * a reused item never keeps the baselines of its former content.
 */
@Composable
fun SupportingBulletList(
    intro: String?,
    bullets: List<String>,
    modifier: Modifier = Modifier,
) {
    key(intro != null, bullets.size) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            intro?.let { Text(it) }
            bullets.forEach { bullet ->
                Row {
                    // Screen readers read the lines as they are, without a "bullet" before each
                    Text(
                        text = BULLET,
                        modifier = Modifier
                            .padding(start = 4.dp, end = 8.dp)
                            .clearAndSetSemantics {},
                    )
                    Text(bullet, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private const val BULLET = "•"
