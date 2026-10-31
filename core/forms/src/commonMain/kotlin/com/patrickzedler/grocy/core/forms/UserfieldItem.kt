package com.patrickzedler.grocy.core.forms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.component.DropdownOption
import com.patrickzedler.grocy.core.design.component.NumberStepper
import com.patrickzedler.grocy.core.design.component.RoundedTextField
import com.patrickzedler.grocy.core.design.component.SegmentedContainer
import com.patrickzedler.grocy.core.design.component.SegmentedSwitchItem
import com.patrickzedler.grocy.core.design.component.SegmentedTexts
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.model.masterdata.UserfieldType
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.data_date_invalid
import com.patrickzedler.grocy.core.resources.data_datetime_invalid
import com.patrickzedler.grocy.core.resources.data_number_invalid
import com.patrickzedler.grocy.core.resources.ic_rounded_check
import com.patrickzedler.grocy.core.resources.product_userfield_file
import com.patrickzedler.grocy.core.resources.product_userfield_link
import com.patrickzedler.grocy.core.resources.product_userfield_required
import com.patrickzedler.grocy.core.resources.product_userfield_title
import org.jetbrains.compose.resources.stringResource

/** The input of one userfield by its type, used by the forms of every object with userfields. */
@Composable
fun UserfieldItem(
    input: UserfieldInput,
    error: UserfieldError?,
    index: Int,
    count: Int,
) {
    val haptic = LocalHapticFeedback.current
    val field = input.userfield
    val errorText = when (error) {
        null -> null
        UserfieldError.REQUIRED -> stringResource(Res.string.product_userfield_required)
        UserfieldError.INVALID_NUMBER -> stringResource(Res.string.data_number_invalid, "0", "4")
        UserfieldError.INVALID_DATE -> stringResource(Res.string.data_date_invalid)
        UserfieldError.INVALID_DATETIME -> stringResource(Res.string.data_datetime_invalid)
    }
    when (field.type) {
        UserfieldType.CHECKBOX -> SegmentedSwitchItem(
            headline = field.caption,
            supporting = errorText,
            checked = input.checked,
            onCheckedChange = { input.checked = it },
            index = index,
            count = count,
        )

        UserfieldType.PRESET_LIST -> DropdownItem(
            label = field.caption,
            options = field.options.mapIndexed { optionIndex, option ->
                DropdownOption(optionIndex, option)
            },
            selectedId = field.options.indexOf(input.selectedOption).takeIf { it >= 0 },
            onSelectedIdChange = { id -> input.selectedOption = id?.let { field.options[it] } },
            onUnmatchedChange = { input.isUnmatched = it },
            error = errorText,
            optional = true,
            index = index,
            count = count,
        )

        UserfieldType.PRESET_CHECKLIST -> SegmentedContainer(index = index, count = count) {
            SegmentedTexts(headline = field.caption, supporting = errorText)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                field.options.forEach { option ->
                    val selected = option in input.checklist
                    FilterChip(
                        selected = selected,
                        modifier = Modifier.clickCursor(),
                        onClick = {
                            haptic.confirm()
                            input.checklist = if (selected) {
                                input.checklist - option
                            } else {
                                input.checklist + option
                            }
                        },
                        label = { Text(option) },
                        elevation = filterChipElevation(),
                        leadingIcon = if (selected) {
                            { ChipIcon(Res.drawable.ic_rounded_check) }
                        } else {
                            null
                        },
                    )
                }
            }
        }

        UserfieldType.LINK_WITH_TITLE -> SegmentedContainer(index = index, count = count) {
            SegmentedTexts(headline = field.caption, supporting = errorText)
            Spacer(Modifier.height(8.dp))
            RoundedTextField(
                state = input.title,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.product_userfield_title)) },
            )
            Spacer(Modifier.height(8.dp))
            RoundedTextField(
                state = input.link,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.product_userfield_link)) },
            )
        }

        UserfieldType.FILE, UserfieldType.IMAGE -> SegmentedContainer(index, count) {
            SegmentedTexts(
                headline = field.caption,
                supporting = stringResource(Res.string.product_userfield_file),
            )
        }

        else -> TextItem(
            state = input.text,
            label = field.caption,
            error = errorText,
            hint = when (field.type) {
                UserfieldType.DATE -> "YYYY-MM-DD"
                UserfieldType.DATETIME -> "YYYY-MM-DD HH:MM:SS"
                else -> null
            },
            keyboardType = when (field.type) {
                UserfieldType.NUMBER_INTEGRAL -> KeyboardType.Number
                UserfieldType.NUMBER_DECIMAL, UserfieldType.NUMBER_CURRENCY -> KeyboardType.Decimal
                UserfieldType.LINK -> KeyboardType.Uri
                else -> KeyboardType.Text
            },
            multiLine = field.type == UserfieldType.TEXT_MULTI_LINE,
            stepper = if (field.type in NumberTypes) NumberStepper() else null,
            index = index,
            count = count,
            trailingAction = if (field.type == UserfieldType.DATE ||
                field.type == UserfieldType.DATETIME
            ) {
                { DatePickerButton(input.text, withTime = field.type == UserfieldType.DATETIME) }
            } else {
                null
            },
        )
    }
}

// Grocy's number pickers, with their minus and plus buttons
private val NumberTypes = setOf(
    UserfieldType.NUMBER_INTEGRAL,
    UserfieldType.NUMBER_DECIMAL,
    UserfieldType.NUMBER_CURRENCY,
)
