package com.patrickzedler.grocy.core.forms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.component.ConnectedButtonGroup
import com.patrickzedler.grocy.core.design.component.DropdownOption
import com.patrickzedler.grocy.core.design.component.LocalFormFocus
import com.patrickzedler.grocy.core.design.component.NumberStepper
import com.patrickzedler.grocy.core.design.component.NumberStepperField
import com.patrickzedler.grocy.core.design.component.RoundedTextField
import com.patrickzedler.grocy.core.design.component.SearchableDropdownField
import com.patrickzedler.grocy.core.design.component.SegmentedContainer
import com.patrickzedler.grocy.core.design.component.TextFieldActions
import com.patrickzedler.grocy.core.design.component.SegmentedTexts
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.data_date_invalid
import com.patrickzedler.grocy.core.resources.data_not_before_now
import com.patrickzedler.grocy.core.resources.data_datetime_invalid
import com.patrickzedler.grocy.core.resources.data_none
import com.patrickzedler.grocy.core.resources.data_number_invalid
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

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
        FieldError.NotBeforeNow -> stringResource(Res.string.data_not_before_now)
    }

/**
 * A text field as segmented item [index] of [count]: [error] below it, then [hint], and [below]
 * for what belongs to the field (e.g. the scanner of [trailingAction]). With [stepper] it gets
 * buttons to count up and down.
 *
 * @param autoFocus takes the focus once when the form opens; for fields the user types into
 *   right away, see `docs/focus.md`.
 * @param isLastField the keyboard's action submits the form instead of going on, outside of a
 *   form it closes the keyboard.
 * @param autoCorrect off for what is no language, like codes.
 * @param onFieldClick makes the field a button instead of an input, e.g. for a date picker: it
 *   shows its value, tapping it calls this, and the keyboard's way through the form passes it.
 * @param trailingButton a button after [trailingAction] that keeps its own click when
 *   [onFieldClick] makes the rest of the field a button, e.g. a date field's scanner.
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
    focusRequester: FocusRequester? = null,
    autoFocus: Boolean = false,
    isLastField: Boolean = false,
    autoCorrect: Boolean = true,
    inputTransformation: InputTransformation? = null,
    outputTransformation: OutputTransformation? = null,
    onFieldClick: (() -> Unit)? = null,
    onFocusChange: (Boolean) -> Unit = {},
    trailingButton: (@Composable () -> Unit)? = null,
) {
    val form = LocalFormFocus.current
    val isDone = isLastField && !multiLine
    // Where the trailing button starts, so the field's button leaves its click to it
    val buttonArea = remember { TrailingButtonArea() }
    val trailing: (@Composable () -> Unit)? = if (trailingButton == null) {
        trailingAction
    } else {
        {
            val button = @Composable {
                Box(Modifier.onGloballyPositioned(buttonArea::onButtonPlaced)) {
                    trailingButton()
                }
            }
            if (trailingAction != null) {
                TextFieldActions {
                    trailingAction()
                    button()
                }
            } else {
                button()
            }
        }
    }
    SegmentedContainer(index = index, count = count, modifier = modifier) {
        val field: @Composable (Modifier) -> Unit = { fieldModifier ->
            RoundedTextField(
                state = state,
                modifier = fieldModifier
                    .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
                    .then(
                        if (onFieldClick != null) {
                            // The button above it takes focus and semantics
                            Modifier
                                .focusProperties { canFocus = false }
                                .clearAndSetSemantics {}
                        } else {
                            Modifier
                        },
                    )
                    .onFocusChanged { onFocusChange(it.isFocused) },
                enabled = enabled,
                readOnly = onFieldClick != null,
                inputTransformation = inputTransformation,
                outputTransformation = outputTransformation,
                label = { Text(label) },
                trailingIcon = trailing,
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
                    capitalization = if (keyboardType == KeyboardType.Text && autoCorrect) {
                        KeyboardCapitalization.Sentences
                    } else {
                        KeyboardCapitalization.None
                    },
                    autoCorrectEnabled = autoCorrect,
                    keyboardType = keyboardType,
                    imeAction = when {
                        multiLine -> ImeAction.Default
                        isDone -> ImeAction.Done
                        else -> ImeAction.Next
                    },
                ),
                // Outside of a form Done only closes the keyboard, its default
                onKeyboardAction = if (isDone && form != null) {
                    { form.submit() }
                } else {
                    null
                },
                autoFocus = autoFocus,
                // Multi-line fields start as high as the others and grow with their paragraphs
                lineLimits = if (multiLine) {
                    TextFieldLineLimits.MultiLine(minHeightInLines = 1)
                } else {
                    TextFieldLineLimits.SingleLine
                },
            )
        }
        if (onFieldClick != null) {
            val description = listOfNotNull(label, state.text.toString().ifEmpty { null }, error)
                .joinToString(", ")
            val haptic = LocalHapticFeedback.current
            Box(Modifier.fillMaxWidth().onGloballyPositioned(buttonArea::onFieldPlaced)) {
                field(Modifier.fillMaxWidth())
                val excludedEnd = with(LocalDensity.current) { buttonArea.endWidth.toDp() }
                Box(
                    Modifier
                        .matchParentSize()
                        .padding(end = excludedEnd)
                        .clickCursor(enabled)
                        .semantics { contentDescription = description }
                        .clickable(
                            enabled = enabled,
                            role = Role.Button,
                            interactionSource = null,
                            indication = null,
                        ) {
                            haptic.confirm()
                            onFieldClick()
                        },
                )
            }
        } else if (stepper == null) {
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

/** The width at the end of a field that its trailing button takes, up to the field's end. */
private class TrailingButtonArea {
    private var field: LayoutCoordinates? = null
    private var button: LayoutCoordinates? = null

    var endWidth by mutableIntStateOf(0)
        private set

    fun onFieldPlaced(coordinates: LayoutCoordinates) {
        field = coordinates
        update()
    }

    fun onButtonPlaced(coordinates: LayoutCoordinates) {
        button = coordinates
        update()
    }

    private fun update() {
        val field = field?.takeIf { it.isAttached } ?: return
        val button = button?.takeIf { it.isAttached } ?: return
        val start = field.localBoundingBoxOf(button, clipBounds = false).left
        endWidth = (field.size.width - start).roundToInt().coerceAtLeast(0)
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
 * option, so the form can refuse to save; [focusRequester] focuses the field for typing. The
 * other focus parameters are [SearchableDropdownField]'s.
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
    autoFocus: Boolean = false,
    isLastField: Boolean = false,
    onSubmitText: ((text: String, hasSuggestion: Boolean) -> Boolean)? = null,
    submitsTextOnLeave: Boolean = false,
    textResetKey: Any? = null,
    onKeyboardNext: (() -> Unit)? = null,
    onFocusChange: (Boolean) -> Unit = {},
    onUnmatchedTextChange: (String) -> Unit = {},
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
            autoFocus = autoFocus,
            isLastField = isLastField,
            onSubmitText = onSubmitText,
            submitsTextOnLeave = submitsTextOnLeave,
            textResetKey = textResetKey,
            onKeyboardNext = onKeyboardNext,
            onFocusChange = onFocusChange,
            onUnmatchedTextChange = onUnmatchedTextChange,
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
