package com.patrickzedler.grocy.core.forms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.component.StatusColor
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.stock_overview_field_value
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** A field shown as chip: its text, how screen readers name it, its icon and colors. */
class FieldChip(
    val text: String,
    val description: String,
    val icon: DrawableResource? = null,
    val status: StatusColor? = null,
)

/** [value] named by [label] for screen readers, e.g. "Location: Fridge". */
@Composable
fun labelled(label: String, value: String) =
    stringResource(Res.string.stock_overview_field_value, label, value)

/**
 * A chip that only shows a value: no outline or elevation, surfaceContainer when neutral, a
 * status color otherwise.
 */
@Composable
fun FieldChipView(chip: FieldChip) {
    val container = chip.status?.container ?: MaterialTheme.colorScheme.surfaceContainer
    val content = chip.status?.content ?: MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = MaterialTheme.shapes.small,
        color = container,
        contentColor = content,
        // As text, not description: the item merges them, and a description would hide the
        // product's name from screen readers
        modifier = Modifier.clearAndSetSemantics { text = AnnotatedString(chip.description) },
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = ChipHeight)
                .padding(start = if (chip.icon != null) 8.dp else 12.dp, end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            chip.icon?.let {
                Icon(
                    painter = painterResource(it),
                    contentDescription = null,
                    modifier = Modifier.size(ChipIconSize),
                    tint = content,
                )
            }
            Text(
                text = chip.text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// Material's chip height and icon size
private val ChipHeight = 32.dp
private val ChipIconSize = 18.dp
