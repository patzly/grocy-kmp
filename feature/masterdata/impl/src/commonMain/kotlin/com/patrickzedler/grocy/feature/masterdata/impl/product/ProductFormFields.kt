package com.patrickzedler.grocy.feature.masterdata.impl.product

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.component.ConnectedButtonGroup
import com.patrickzedler.grocy.core.design.component.DropdownOption
import com.patrickzedler.grocy.core.design.component.RoundedTextField
import com.patrickzedler.grocy.core.design.component.SearchableDropdownField
import com.patrickzedler.grocy.core.design.component.SegmentedContainer
import com.patrickzedler.grocy.core.design.component.SegmentedTexts
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.data_number_invalid
import com.patrickzedler.grocy.core.resources.data_none
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** One item of a group; it gets its position for the segmented shape. */
internal typealias GroupItem = @Composable (index: Int, count: Int) -> Unit

/** Renders the [items] that are not null as one segmented group. */
@Composable
internal fun GroupItems(vararg items: GroupItem?) {
    val visible = items.filterNotNull()
    visible.forEachIndexed { index, item -> item(index, visible.size) }
}

/** The message of [error], with [requiredMessage] for a missing value. */
@Composable
internal fun fieldErrorText(error: FieldError?, requiredMessage: StringResource? = null): String? =
    when (error) {
        null -> null
        FieldError.Required -> requiredMessage?.let { stringResource(it) }
        is FieldError.InvalidNumber ->
            stringResource(Res.string.data_number_invalid, error.min, error.decimals.toString())
    }

@Composable
internal fun TextItem(
    state: TextFieldState,
    label: String,
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    hint: String? = null,
    error: String? = null,
    suffix: String? = null,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    multiLine: Boolean = false,
) {
    SegmentedContainer(index = index, count = count, modifier = modifier) {
        RoundedTextField(
            state = state,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            label = { Text(label) },
            suffix = suffix?.let { { Text(it) } },
            supportingText = error?.let { text ->
                {
                    Text(
                        text = text,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            },
            isError = error != null,
            keyboardOptions = KeyboardOptions(
                capitalization = if (keyboardType == KeyboardType.Text) {
                    KeyboardCapitalization.Sentences
                } else {
                    KeyboardCapitalization.None
                },
                keyboardType = keyboardType,
                imeAction = if (multiLine) ImeAction.Default else ImeAction.Next,
            ),
            // Multi-line fields start as high as the others and grow with their paragraphs
            lineLimits = if (multiLine) {
                TextFieldLineLimits.MultiLine(minHeightInLines = 1)
            } else {
                TextFieldLineLimits.SingleLine
            },
        )
        HintText(hint)
    }
}

/** Explanation below a field, styled like the supporting text of a list item. */
@Composable
private fun HintText(hint: String?) {
    if (hint == null) return
    Text(
        text = hint,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
internal fun DropdownItem(
    label: String,
    options: List<DropdownOption>,
    selectedId: Int?,
    onSelectedIdChange: (Int?) -> Unit,
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    hint: String? = null,
    error: String? = null,
    optional: Boolean = false,
    enabled: Boolean = true,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    SegmentedContainer(index = index, count = count, modifier = modifier) {
        SearchableDropdownField(
            label = label,
            options = options,
            selectedId = selectedId,
            onSelectedIdChange = onSelectedIdChange,
            enabled = enabled,
            isError = error != null,
            supportingText = error,
            noneLabel = if (optional) stringResource(Res.string.data_none) else null,
            trailingAction = trailingAction,
        )
        HintText(hint)
    }
}

/** A choice between a few values as connected buttons, with a headline and [hint] above. */
@Composable
internal fun <T> ChoiceItem(
    label: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    optionLabel: @Composable (T) -> String,
    index: Int,
    count: Int,
    hint: String? = null,
) {
    SegmentedContainer(index = index, count = count) {
        SegmentedTexts(headline = label, supporting = hint)
        Spacer(Modifier.height(12.dp))
        ConnectedButtonGroup(
            options = options,
            checked = selected,
            onCheckedChange = onSelect,
            modifier = Modifier.fillMaxWidth(),
            label = optionLabel,
        )
    }
}
