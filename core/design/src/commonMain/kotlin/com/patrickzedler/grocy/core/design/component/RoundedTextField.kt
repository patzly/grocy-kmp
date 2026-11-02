package com.patrickzedler.grocy.core.design.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.selectAll
import androidx.compose.material3.SecureTextField
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldLabelScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

object RoundedTextFieldDefaults {

    /** 8.dp corners on top, square bottom where the indicator line sits. */
    val shape: Shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
}

/**
 * Filled text field with rounded top corners. A number's text is selected when it takes the focus,
 * so typing replaces it like in Grocy's forms; in a form ([LocalFormFocus]) it takes part in Enter
 * and in finding the first invalid field.
 *
 * @param autoFocus takes the focus once when the form opens.
 * @param onEnterKey lets the field use a hardware Enter itself, true when it did.
 */
@Composable
fun RoundedTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable (TextFieldLabelScope.() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    inputTransformation: InputTransformation? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.SingleLine,
    autoFocus: Boolean = false,
    onEnterKey: (() -> Boolean)? = null,
) {
    val form = LocalFormFocus.current
    val focusRequester = remember { FocusRequester() }
    AutoFocusEffect(autoFocus, focusRequester)
    InvalidFieldFocusEffect(form, isError, focusRequester)
    var isFocused by remember { mutableStateOf(false) }
    if (isFocused && selectsOnFocus(keyboardOptions.keyboardType)) {
        LaunchedEffect(Unit) {
            // After a tap's own cursor placement
            withFrameNanos {}
            state.edit { selectAll() }
        }
    }
    TextField(
        state = state,
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .submitOnEnter(form, lineLimits != TextFieldLineLimits.SingleLine, onEnterKey),
        enabled = enabled,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        suffix = suffix,
        supportingText = supportingText,
        isError = isError,
        inputTransformation = inputTransformation,
        keyboardOptions = keyboardOptions,
        onKeyboardAction = onKeyboardAction,
        lineLimits = lineLimits,
        shape = RoundedTextFieldDefaults.shape,
    )
}

/** Filled field for secrets such as API keys, with rounded top corners. */
@Composable
fun RoundedSecureTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable (TextFieldLabelScope.() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
) {
    val form = LocalFormFocus.current
    val focusRequester = remember { FocusRequester() }
    InvalidFieldFocusEffect(form, isError, focusRequester)
    SecureTextField(
        state = state,
        modifier = modifier
            .focusRequester(focusRequester)
            .submitOnEnter(form, isMultiLine = false, onEnter = null),
        enabled = enabled,
        label = label,
        supportingText = supportingText,
        isError = isError,
        keyboardOptions = keyboardOptions,
        onKeyboardAction = onKeyboardAction,
        shape = RoundedTextFieldDefaults.shape,
    )
}

/** Takes the focus when a refused submit looks for the first invalid field and this is it. */
@Composable
private fun InvalidFieldFocusEffect(
    form: FormFocus?,
    isError: Boolean,
    focusRequester: FocusRequester,
) {
    if (form != null && isError && form.isSeekingError) {
        LaunchedEffect(Unit) {
            if (form.claimError()) focusRequester.requestFocus()
        }
    }
}

internal fun selectsOnFocus(keyboardType: KeyboardType): Boolean =
    keyboardType == KeyboardType.Number || keyboardType == KeyboardType.Decimal
