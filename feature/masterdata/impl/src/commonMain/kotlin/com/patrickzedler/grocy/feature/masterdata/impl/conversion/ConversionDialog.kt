package com.patrickzedler.grocy.feature.masterdata.impl.conversion

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.input.KeyboardType
import com.patrickzedler.grocy.core.design.component.DropdownOption
import com.patrickzedler.grocy.core.design.component.NumberStepper
import com.patrickzedler.grocy.core.design.component.SegmentedGroup
import com.patrickzedler.grocy.core.model.text.parseDecimal
import com.patrickzedler.grocy.core.model.text.platformDecimalSeparator
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.conversion_create
import com.patrickzedler.grocy.core.resources.conversion_default_for
import com.patrickzedler.grocy.core.resources.conversion_edit
import com.patrickzedler.grocy.core.resources.conversion_factor
import com.patrickzedler.grocy.core.resources.conversion_from
import com.patrickzedler.grocy.core.resources.conversion_means
import com.patrickzedler.grocy.core.resources.conversion_not_equal
import com.patrickzedler.grocy.core.resources.conversion_override_for
import com.patrickzedler.grocy.core.resources.conversion_qu_required
import com.patrickzedler.grocy.core.resources.conversion_to
import com.patrickzedler.grocy.core.resources.product_userfields
import com.patrickzedler.grocy.feature.masterdata.impl.product.DropdownItem
import com.patrickzedler.grocy.feature.masterdata.impl.product.TextItem
import com.patrickzedler.grocy.feature.masterdata.impl.product.UserfieldItem
import com.patrickzedler.grocy.feature.masterdata.impl.product.fieldErrorText
import com.patrickzedler.grocy.feature.masterdata.impl.related.FormDialog
import com.patrickzedler.grocy.feature.masterdata.impl.related.saveMessage
import org.jetbrains.compose.resources.stringResource

/** Grocy's form of a product's conversion override or a unit's default conversion. */
@Composable
internal fun ConversionDialog(
    state: ConversionDialogState,
    onSave: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val options = state.units.map { DropdownOption(it.id, it.name) }
    val from = state.units.firstOrNull { it.id == state.fromQuId }
    val to = state.units.firstOrNull { it.id == state.toQuId }
    val factor = parseDecimal(state.factor.text.toString())?.takeIf { it > 0 }

    FormDialog(
        title = stringResource(
            if (state.isNew) Res.string.conversion_create else Res.string.conversion_edit,
        ),
        subtitle = stringResource(
            if (state.isProductOverride) {
                Res.string.conversion_override_for
            } else {
                Res.string.conversion_default_for
            },
            state.ownerName,
        ),
        isSaving = state.isSaving,
        error = state.error?.saveMessage(),
        onSave = onSave,
        onDismissRequest = onDismissRequest,
    ) {
        SegmentedGroup {
            DropdownItem(
                label = stringResource(Res.string.conversion_from),
                options = options,
                selectedId = state.fromQuId,
                onSelectedIdChange = { state.fromQuId = it },
                onUnmatchedChange = { state.isFromUnmatched = it },
                error = state.errors[ConversionField.FROM]?.let { errorText(it, null) },
                enabled = !state.isFromFixed,
                index = 0,
                count = 3,
            )
            DropdownItem(
                label = stringResource(Res.string.conversion_to),
                options = options,
                selectedId = state.toQuId,
                onSelectedIdChange = { state.toQuId = it },
                onUnmatchedChange = { state.isToUnmatched = it },
                error = state.errors[ConversionField.TO]?.let { errorText(it, from?.name) },
                index = 1,
                count = 3,
            )
            TextItem(
                state = state.factor,
                label = stringResource(Res.string.conversion_factor),
                error = state.errors[ConversionField.FACTOR]?.let { errorText(it, null) },
                // Grocy explains the factor both ways once both units are chosen
                hint = if (from != null && to != null && factor != null && from.id != to.id) {
                    val separator = platformDecimalSeparator()
                    val language = LocalLocale.current.toLanguageTag()
                    listOf(false, true).map { inverse ->
                        val meaning = conversionMeaning(
                            from, to, factor, state.decimals, separator, language, inverse,
                        )
                        stringResource(
                            Res.string.conversion_means,
                            meaning.from,
                            meaning.amount,
                            meaning.to,
                        )
                    }.joinToString("\n")
                } else {
                    null
                },
                keyboardType = KeyboardType.Decimal,
                stepper = NumberStepper.Positive,
                index = 2,
                count = 3,
            )
        }
        if (state.userfields.isNotEmpty()) {
            SegmentedGroup(title = stringResource(Res.string.product_userfields)) {
                state.userfields.forEachIndexed { index, input ->
                    UserfieldItem(
                        input = input,
                        error = state.userfieldErrors[input.userfield.name],
                        index = index,
                        count = state.userfields.size,
                    )
                }
            }
        }
    }
}

@Composable
private fun errorText(error: ConversionError, fromName: String?): String? = when (error) {
    ConversionError.Required -> stringResource(Res.string.conversion_qu_required)
    ConversionError.SameUnit -> stringResource(Res.string.conversion_not_equal, fromName.orEmpty())
    is ConversionError.Number -> fieldErrorText(error.error)
}
