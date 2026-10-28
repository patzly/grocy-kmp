package com.patrickzedler.grocy.feature.settings.impl

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.patrickzedler.grocy.core.design.component.NumberStepper
import com.patrickzedler.grocy.core.design.component.NumberStepperField
import com.patrickzedler.grocy.core.design.component.RoundedTextField
import com.patrickzedler.grocy.core.design.component.SegmentedContainer
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.data_number_invalid
import com.patrickzedler.grocy.core.resources.data_number_range_invalid
import org.jetbrains.compose.resources.stringResource

/**
 * A number of Grocy's settings pages as its number picker: the field with minus and plus, the
 * [error] of the typed number below them; [hint] is Grocy's help text of the setting.
 */
@Composable
internal fun SettingNumberItem(
    label: String,
    state: TextFieldState,
    stepper: NumberStepper,
    error: SettingError?,
    index: Int,
    count: Int,
    enabled: Boolean = true,
    hint: String? = null,
) {
    val errorText = error?.let { settingErrorText(it) }
    SegmentedContainer(index = index, count = count) {
        NumberStepperField(
            state = state,
            stepper = stepper,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            error = errorText,
            supportingText = hint,
        ) { fieldModifier ->
            // The stepper shows the error below its buttons, so they stay centered on the field
            RoundedTextField(
                state = state,
                modifier = fieldModifier,
                enabled = enabled,
                label = { Text(label) },
                isError = errorText != null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                ),
            )
        }
    }
}

@Composable
private fun settingErrorText(error: SettingError): String = when (error) {
    is SettingError.InvalidNumber -> if (error.max == null) {
        stringResource(Res.string.data_number_invalid, error.min, error.decimals.toString())
    } else {
        stringResource(
            Res.string.data_number_range_invalid,
            error.min,
            error.max,
            error.decimals.toString(),
        )
    }
}
