package com.patrickzedler.grocy.core.forms

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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.component.ConnectedButtonGroup
import com.patrickzedler.grocy.core.design.component.DropdownOption
import com.patrickzedler.grocy.core.design.component.NumberStepper
import com.patrickzedler.grocy.core.design.component.NumberStepperField
import com.patrickzedler.grocy.core.design.component.RoundedTextField
import com.patrickzedler.grocy.core.design.component.SearchableDropdownField
import com.patrickzedler.grocy.core.design.component.SegmentedContainer
import com.patrickzedler.grocy.core.design.component.SegmentedTexts
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.data_date_invalid
import com.patrickzedler.grocy.core.resources.data_datetime_invalid
import com.patrickzedler.grocy.core.resources.data_none
import com.patrickzedler.grocy.core.resources.data_number_invalid
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** One item of a group; it gets its position for the segmented shape. */
typealias GroupItem = @Composable (index: Int, count: Int) -> Unit

/** Renders the [items] that are not null as one segmented group. */
@Composable
fun GroupItems(vararg items: GroupItem?) {
    val visible = items.filterNotNull()
    visible.forEachIndexed { index, item -> item(index, visible.size) }
}

/** The message of [error], with [requiredMessage] for a missing value. */
@Composable
fun fieldErrorText(error: FieldError?, requiredMessage: StringResource? = null): String? =
    when (error) {
        null -> null
        FieldError.Required -> requiredMessage?.let { stringResource(it) }
        is FieldError.InvalidNumber ->
            stringResource(Res.string.data_number_invalid, error.min, error.decimals.toString())
        FieldError.InvalidDateTime -> stringResource(Res.string.data_datetime_invalid)
        FieldError.InvalidDate -> stringResource(Res.string.data_date_invalid)
    }

/**
 * A text field as segmented item [index] of [count]: [error] below it, then [hint], and [below]
 * for what belongs to the field (e.g. the scanner of [trailingAction]). With [stepper] it gets
 * buttons to count up and down.
 */
@Composable
fun TextItem(
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
    trailingAction: (@Composable () -> Unit)? = null,
    below: (@Composable () -> Unit)? = null,
    stepper: NumberStepper? = null,
) {
    SegmentedContainer(index = index, count = count, modifier = modifier) {
        val field: @Composable (Modifier) -> Unit = { fieldModifier ->
            RoundedTextField(
                state = state,
                modifier = fieldModifier,
                enabled = enabled,
                label = { Text(label) },
                trailingIcon = trailingAction,
                suffix = suffix?.let { { Text(it) } },
                // The stepper shows it below its buttons too, so they stay centered on the field
                supportingText = error?.takeIf { stepper == null }?.let { text ->
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
        }
        if (stepper == null) {
            field(Modifier.fillMaxWidth())
        } else {
            NumberStepperField(
                state = state,
                stepper = stepper,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                error = error,
                field = field,
            )
        }
        HintText(hint)
        // Belongs to the field, e.g. the scanner of its scan action
        if (below != null) {
            Spacer(Modifier.height(16.dp))
            below()
        }
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

/**
 * A choice of objects as segmented item [index] of [count], Grocy's combobox picker: the text
 * names the option; [optional] ones offer "None". [onUnmatchedChange] reports text naming no
 * option, so the form can refuse to save; [focusRequester] focuses the field for typing.
 */
@Composable
fun DropdownItem(
    label: String,
    options: List<DropdownOption>,
    selectedId: Int?,
    onSelectedIdChange: (Int?) -> Unit,
    onUnmatchedChange: (Boolean) -> Unit,
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    hint: String? = null,
    error: String? = null,
    optional: Boolean = false,
    enabled: Boolean = true,
    trailingAction: (@Composable () -> Unit)? = null,
    below: (@Composable () -> Unit)? = null,
    focusRequester: FocusRequester? = null,
) {
    SegmentedContainer(index = index, count = count, modifier = modifier) {
        SearchableDropdownField(
            label = label,
            options = options,
            selectedId = selectedId,
            onSelectedIdChange = onSelectedIdChange,
            onUnmatchedChange = onUnmatchedChange,
            enabled = enabled,
            isError = error != null,
            supportingText = error,
            noneLabel = if (optional) stringResource(Res.string.data_none) else null,
            trailingAction = trailingAction,
            focusRequester = focusRequester,
        )
        HintText(hint)
        // Belongs to the field, e.g. the scanner of its scan action
        if (below != null) {
            Spacer(Modifier.height(16.dp))
            below()
        }
    }
}

/** A choice between a few values as connected buttons, with a headline and [hint] above. */
@Composable
fun <T> ChoiceItem(
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
