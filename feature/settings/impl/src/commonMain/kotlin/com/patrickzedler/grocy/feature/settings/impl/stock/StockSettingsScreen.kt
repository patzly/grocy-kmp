package com.patrickzedler.grocy.feature.settings.impl.stock

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.design.component.ConnectedButtonGroup
import com.patrickzedler.grocy.core.design.component.DropdownOption
import com.patrickzedler.grocy.core.design.component.NumberStepper
import com.patrickzedler.grocy.core.design.component.NumberStepperField
import com.patrickzedler.grocy.core.design.component.RoundedTextField
import com.patrickzedler.grocy.core.design.component.ScreenPane
import com.patrickzedler.grocy.core.design.component.SearchableDropdownField
import com.patrickzedler.grocy.core.design.component.SegmentedContainer
import com.patrickzedler.grocy.core.design.component.SegmentedSwitchItem
import com.patrickzedler.grocy.core.design.component.SegmentedTexts
import com.patrickzedler.grocy.core.design.component.segmentedGroup
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.data_location
import com.patrickzedler.grocy.core.resources.data_none
import com.patrickzedler.grocy.core.resources.data_number_invalid
import com.patrickzedler.grocy.core.resources.data_number_range_invalid
import com.patrickzedler.grocy.core.resources.data_product_group
import com.patrickzedler.grocy.core.resources.data_quantity_unit
import com.patrickzedler.grocy.core.resources.data_save_failed
import com.patrickzedler.grocy.core.resources.data_save_failed_details
import com.patrickzedler.grocy.core.resources.data_save_offline
import com.patrickzedler.grocy.core.resources.product_stock_label
import com.patrickzedler.grocy.core.resources.product_stock_label_none
import com.patrickzedler.grocy.core.resources.product_stock_label_per_unit
import com.patrickzedler.grocy.core.resources.product_stock_label_single
import com.patrickzedler.grocy.core.resources.product_treat_opened_as_out_of_stock
import com.patrickzedler.grocy.core.resources.products_presets
import com.patrickzedler.grocy.core.resources.settings_stock
import com.patrickzedler.grocy.core.resources.stock_settings_auto_decimal_separator
import com.patrickzedler.grocy.core.resources.stock_settings_auto_decimal_separator_hint
import com.patrickzedler.grocy.core.resources.stock_settings_common
import com.patrickzedler.grocy.core.resources.stock_settings_consume
import com.patrickzedler.grocy.core.resources.stock_settings_consume_amount
import com.patrickzedler.grocy.core.resources.stock_settings_decimals_amounts
import com.patrickzedler.grocy.core.resources.stock_settings_decimals_prices_display
import com.patrickzedler.grocy.core.resources.stock_settings_decimals_prices_input
import com.patrickzedler.grocy.core.resources.stock_settings_default_due_days
import com.patrickzedler.grocy.core.resources.stock_settings_due_date_warning
import com.patrickzedler.grocy.core.resources.stock_settings_due_soon_days
import com.patrickzedler.grocy.core.resources.stock_settings_overview
import com.patrickzedler.grocy.core.resources.stock_settings_purchase
import com.patrickzedler.grocy.core.resources.stock_settings_purchase_amount
import com.patrickzedler.grocy.core.resources.stock_settings_show_all_out_of_stock
import com.patrickzedler.grocy.core.resources.stock_settings_show_all_out_of_stock_hint
import com.patrickzedler.grocy.core.resources.stock_settings_show_icon
import com.patrickzedler.grocy.core.resources.stock_settings_show_purchased_date
import com.patrickzedler.grocy.core.resources.stock_settings_show_purchased_date_hint
import com.patrickzedler.grocy.core.resources.stock_settings_use_quick_consume
import com.patrickzedler.grocy.feature.settings.impl.SettingsMenuActions
import com.patrickzedler.grocy.feature.settings.impl.SettingsMenuViewModel
import com.patrickzedler.grocy.feature.settings.impl.SettingsScaffold
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * Grocy's stock settings of the user in its groups: presets for new products, stock overview,
 * purchase, consume and the decimal places. Settings of turned off features are hidden.
 */
@Composable
internal fun StockSettingsScreen(
    viewModel: StockSettingsViewModel,
    menuViewModel: SettingsMenuViewModel,
    menuActions: SettingsMenuActions,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    pane: ScreenPane? = null,
) {
    val options by viewModel.options.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            snackbarHostState.showSnackbar(
                when (event) {
                    StockSettingsEvent.Offline -> getString(Res.string.data_save_offline)
                    is StockSettingsEvent.Failed -> listOfNotNull(
                        getString(Res.string.data_save_failed),
                        event.serverMessage?.let {
                            getString(Res.string.data_save_failed_details, it)
                        },
                    ).joinToString("\n")
                },
            )
        }
    }

    SettingsScaffold(
        title = stringResource(Res.string.settings_stock),
        menuViewModel = menuViewModel,
        menuActions = menuActions,
        onBackClick = onBackClick,
        modifier = modifier,
        pane = pane,
        // Opened from the user settings, it returns to them beside the categories too
        hasBackButton = true,
        snackbarHostState = snackbarHostState,
    ) {
        val form = viewModel.form
        val currentOptions = options
        // Empty for the moment until the stored settings are read
        if (form != null && currentOptions != null) {
            stockSettings(form, currentOptions, viewModel)
        }
    }
}

private fun LazyListScope.stockSettings(
    form: StockSettingsForm,
    options: StockSettingsOptions,
    viewModel: StockSettingsViewModel,
) {
    val config = options.config
    segmentedGroup(key = "presets", title = Res.string.products_presets) {
        val items = buildList<@Composable (Int, Int) -> Unit> {
            if (config.stockLocationTracking) {
                add { index, count ->
                    PresetItem(
                        label = Res.string.data_location,
                        options = options.locations.map { DropdownOption(it.id, it.name) },
                        setting = StockPresetSetting.LOCATION,
                        form = form,
                        viewModel = viewModel,
                        index = index,
                        count = count,
                    )
                }
            }
            add { index, count ->
                PresetItem(
                    label = Res.string.data_product_group,
                    options = options.productGroups.map { DropdownOption(it.id, it.name) },
                    setting = StockPresetSetting.PRODUCT_GROUP,
                    form = form,
                    viewModel = viewModel,
                    index = index,
                    count = count,
                )
            }
            add { index, count ->
                PresetItem(
                    label = Res.string.data_quantity_unit,
                    options = options.quantityUnits.map { DropdownOption(it.id, it.name) },
                    setting = StockPresetSetting.QUANTITY_UNIT,
                    form = form,
                    viewModel = viewModel,
                    index = index,
                    count = count,
                )
            }
            add { index, count ->
                NumberItem(
                    label = Res.string.stock_settings_default_due_days,
                    setting = StockNumberSetting.PRESET_DUE_DAYS,
                    form = form,
                    index = index,
                    count = count,
                )
            }
            if (config.stockProductOpenedTracking) {
                add { index, count ->
                    SwitchItem(
                        label = Res.string.product_treat_opened_as_out_of_stock,
                        setting = StockSwitchSetting.TREAT_OPENED_AS_OUT_OF_STOCK,
                        form = form,
                        viewModel = viewModel,
                        index = index,
                        count = count,
                    )
                }
            }
            if (config.labelPrinter) {
                add { index, count -> LabelTypeItem(form, viewModel, index, count) }
            }
        }
        items.forEachIndexed { index, item -> item(index, items.size) }
    }

    segmentedGroup(key = "overview", title = Res.string.stock_settings_overview) {
        NumberItem(
            label = Res.string.stock_settings_due_soon_days,
            setting = StockNumberSetting.DUE_SOON_DAYS,
            form = form,
            index = 0,
            count = 3,
        )
        SwitchItem(
            label = Res.string.stock_settings_show_icon,
            setting = StockSwitchSetting.SHOW_ICON_ON_SHOPPING_LIST,
            form = form,
            viewModel = viewModel,
            index = 1,
            count = 3,
        )
        SwitchItem(
            label = Res.string.stock_settings_show_all_out_of_stock,
            hint = Res.string.stock_settings_show_all_out_of_stock_hint,
            setting = StockSwitchSetting.SHOW_ALL_OUT_OF_STOCK,
            form = form,
            viewModel = viewModel,
            index = 2,
            count = 3,
        )
    }

    segmentedGroup(key = "purchase", title = Res.string.stock_settings_purchase) {
        NumberItem(
            label = Res.string.stock_settings_purchase_amount,
            setting = StockNumberSetting.PURCHASE_AMOUNT,
            form = form,
            index = 0,
            count = 3,
        )
        SwitchItem(
            label = Res.string.stock_settings_show_purchased_date,
            hint = Res.string.stock_settings_show_purchased_date_hint,
            setting = StockSwitchSetting.SHOW_PURCHASED_DATE,
            form = form,
            viewModel = viewModel,
            index = 1,
            count = 3,
        )
        SwitchItem(
            label = Res.string.stock_settings_due_date_warning,
            setting = StockSwitchSetting.SHOW_DUE_DATE_WARNING,
            form = form,
            viewModel = viewModel,
            index = 2,
            count = 3,
        )
    }

    segmentedGroup(key = "consume", title = Res.string.stock_settings_consume) {
        NumberItem(
            label = Res.string.stock_settings_consume_amount,
            setting = StockNumberSetting.CONSUME_AMOUNT,
            form = form,
            index = 0,
            count = 2,
            // The product's quick consume amount takes its place
            enabled = form.switches[StockSwitchSetting.USE_QUICK_CONSUME_AMOUNT] != true,
        )
        SwitchItem(
            label = Res.string.stock_settings_use_quick_consume,
            setting = StockSwitchSetting.USE_QUICK_CONSUME_AMOUNT,
            form = form,
            viewModel = viewModel,
            index = 1,
            count = 2,
        )
    }

    segmentedGroup(key = "common", title = Res.string.stock_settings_common) {
        val count = if (config.stockPriceTracking) 4 else 1
        NumberItem(
            label = Res.string.stock_settings_decimals_amounts,
            setting = StockNumberSetting.DECIMALS_AMOUNTS,
            form = form,
            index = 0,
            count = count,
        )
        if (config.stockPriceTracking) {
            NumberItem(
                label = Res.string.stock_settings_decimals_prices_input,
                setting = StockNumberSetting.DECIMALS_PRICES_INPUT,
                form = form,
                index = 1,
                count = count,
            )
            NumberItem(
                label = Res.string.stock_settings_decimals_prices_display,
                setting = StockNumberSetting.DECIMALS_PRICES_DISPLAY,
                form = form,
                index = 2,
                count = count,
            )
            SwitchItem(
                label = Res.string.stock_settings_auto_decimal_separator,
                hint = Res.string.stock_settings_auto_decimal_separator_hint,
                setting = StockSwitchSetting.AUTO_DECIMAL_SEPARATOR,
                form = form,
                viewModel = viewModel,
                index = 3,
                count = count,
            )
        }
    }
}

@Composable
private fun PresetItem(
    label: StringResource,
    options: List<DropdownOption>,
    setting: StockPresetSetting,
    form: StockSettingsForm,
    viewModel: StockSettingsViewModel,
    index: Int,
    count: Int,
) {
    SegmentedContainer(index = index, count = count) {
        SearchableDropdownField(
            label = stringResource(label),
            options = options,
            selectedId = form.presets[setting],
            onSelectedIdChange = { viewModel.setPreset(setting, it) },
            noneLabel = stringResource(Res.string.data_none),
        )
    }
}

@Composable
private fun NumberItem(
    label: StringResource,
    setting: StockNumberSetting,
    form: StockSettingsForm,
    index: Int,
    count: Int,
    enabled: Boolean = true,
) {
    val error = form.errors[setting]?.let { errorText(it) }
    val state = form.numbers.getValue(setting)
    SegmentedContainer(index = index, count = count) {
        NumberStepperField(
            state = state,
            stepper = NumberStepper(min = setting.min, max = setting.max),
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            error = error,
        ) { fieldModifier ->
            NumberField(state, label, isError = error != null, enabled, modifier = fieldModifier)
        }
    }
}

@Composable
private fun NumberField(
    state: TextFieldState,
    label: StringResource,
    isError: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    // The stepper shows the error below its buttons, so they stay centered on the field
    RoundedTextField(
        state = state,
        modifier = modifier,
        enabled = enabled,
        label = { Text(stringResource(label)) },
        isError = isError,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Next,
        ),
    )
}

@Composable
private fun errorText(error: SettingError): String = when (error) {
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

@Composable
private fun SwitchItem(
    label: StringResource,
    setting: StockSwitchSetting,
    form: StockSettingsForm,
    viewModel: StockSettingsViewModel,
    index: Int,
    count: Int,
    hint: StringResource? = null,
) {
    SegmentedSwitchItem(
        headline = stringResource(label),
        supporting = hint?.let { stringResource(it) },
        checked = form.switches[setting] == true,
        onCheckedChange = { viewModel.setSwitch(setting, it) },
        index = index,
        count = count,
    )
}

/** Grocy's "Default stock entry label": no label, a single label or one per unit. */
@Composable
private fun LabelTypeItem(
    form: StockSettingsForm,
    viewModel: StockSettingsViewModel,
    index: Int,
    count: Int,
) {
    SegmentedContainer(index = index, count = count) {
        SegmentedTexts(headline = stringResource(Res.string.product_stock_label))
        Spacer(Modifier.height(12.dp))
        ConnectedButtonGroup(
            options = LabelTypes,
            checked = form.labelType,
            onCheckedChange = viewModel::setLabelType,
            modifier = Modifier.fillMaxWidth(),
            label = { type ->
                stringResource(
                    when (type) {
                        1 -> Res.string.product_stock_label_single
                        2 -> Res.string.product_stock_label_per_unit
                        else -> Res.string.product_stock_label_none
                    },
                )
            },
        )
    }
}

// Grocy's label types: none, single, per unit
private val LabelTypes = listOf(0, 1, 2)
