package com.patrickzedler.grocy.feature.settings.impl.behavior

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.data.log.LogSource
import com.patrickzedler.grocy.core.design.GrocyTheme
import com.patrickzedler.grocy.core.design.component.LeadingTrailingContentWrapper
import com.patrickzedler.grocy.core.design.component.ScreenPane
import com.patrickzedler.grocy.core.design.component.SegmentedContainer
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.SegmentedSwitchItem
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.segmentedGroup
import com.patrickzedler.grocy.core.model.BehaviorSettings
import com.patrickzedler.grocy.core.model.DateInputOrder
import com.patrickzedler.grocy.core.model.ProductFactsSource
import com.patrickzedler.grocy.core.model.text.languageDateOrder
import com.patrickzedler.grocy.core.model.text.typedDatePattern
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.behavior_barcode_amount
import com.patrickzedler.grocy.core.resources.behavior_barcode_amount_hint
import com.patrickzedler.grocy.core.resources.behavior_date_keyboard
import com.patrickzedler.grocy.core.resources.behavior_date_keyboard_hint
import com.patrickzedler.grocy.core.resources.behavior_date_order
import com.patrickzedler.grocy.core.resources.behavior_date_order_automatic
import com.patrickzedler.grocy.core.resources.behavior_input
import com.patrickzedler.grocy.core.resources.behavior_product_databases
import com.patrickzedler.grocy.core.resources.behavior_product_databases_hint
import com.patrickzedler.grocy.core.resources.behavior_source_beauty
import com.patrickzedler.grocy.core.resources.behavior_source_food
import com.patrickzedler.grocy.core.resources.behavior_source_pet_food
import com.patrickzedler.grocy.core.resources.behavior_source_products
import com.patrickzedler.grocy.core.resources.settings_behavior
import com.patrickzedler.grocy.feature.settings.impl.SettingsMenuActions
import com.patrickzedler.grocy.feature.settings.impl.SettingsMenuViewModel
import com.patrickzedler.grocy.feature.settings.impl.SettingsScaffold
import com.patrickzedler.grocy.feature.settings.impl.SettingsScaffoldContent
import org.jetbrains.compose.resources.stringResource

/**
 * The open product databases first, off until the user turns them on because barcodes then leave
 * the device; below them how dates and barcodes are entered.
 */
@Composable
internal fun BehaviorSettingsScreen(
    viewModel: BehaviorSettingsViewModel,
    menuViewModel: SettingsMenuViewModel,
    menuActions: SettingsMenuActions,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    pane: ScreenPane? = null,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    SettingsScaffold(
        title = stringResource(Res.string.settings_behavior),
        menuViewModel = menuViewModel,
        menuActions = menuActions,
        onBackClick = onBackClick,
        modifier = modifier,
        pane = pane,
    ) {
        // Empty for the moment until the stored settings are read
        settings?.let { current ->
            behaviorSettings(
                settings = current,
                onSourceChange = viewModel::setSource,
                onDateKeyboardChange = viewModel::setDateKeyboard,
                onDateOrderChange = viewModel::setDateInputOrder,
                onBarcodeUsesAmountChange = viewModel::setBarcodeUsesAmount,
            )
        }
    }
}

private fun LazyListScope.behaviorSettings(
    settings: BehaviorSettings,
    onSourceChange: (ProductFactsSource, Boolean) -> Unit,
    onDateKeyboardChange: (Boolean) -> Unit,
    onDateOrderChange: (DateInputOrder) -> Unit,
    onBarcodeUsesAmountChange: (Boolean) -> Unit,
) {
    segmentedGroup(key = "databases", title = Res.string.behavior_product_databases) {
        val sources = ProductFactsSource.entries
        val count = sources.size + 1
        SegmentedContainer(index = 0, count = count) {
            Text(
                text = stringResource(Res.string.behavior_product_databases_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        sources.forEachIndexed { index, source ->
            SegmentedSwitchItem(
                headline = source.title,
                supporting = stringResource(source.description),
                checked = source in settings.productFactsSources,
                onCheckedChange = { onSourceChange(source, it) },
                index = index + 1,
                count = count,
            )
        }
    }
    segmentedGroup(key = "input", title = Res.string.behavior_input) {
        SegmentedSwitchItem(
            headline = stringResource(Res.string.behavior_date_keyboard),
            supporting = stringResource(Res.string.behavior_date_keyboard_hint),
            checked = settings.dateKeyboard,
            onCheckedChange = onDateKeyboardChange,
            index = 0,
            count = 2,
        )
        SegmentedSwitchItem(
            headline = stringResource(Res.string.behavior_barcode_amount),
            supporting = stringResource(Res.string.behavior_barcode_amount_hint),
            checked = settings.barcodeUsesAmount,
            onCheckedChange = onBarcodeUsesAmountChange,
            index = 1,
            count = 2,
        )
    }
    if (settings.dateKeyboard) {
        segmentedGroup(key = "dateOrder", title = Res.string.behavior_date_order) {
            DateOrderChoices(settings.dateInputOrder, onDateOrderChange)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DateOrderChoices(
    selected: DateInputOrder,
    onSelect: (DateInputOrder) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val languagePattern = remember { typedDatePattern(languageDateOrder(), withTime = false) }
    val orders = DateInputOrder.entries
    // Announced together as one choice
    Column(
        modifier = Modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        orders.forEachIndexed { index, order ->
            SegmentedListItem(
                selected = order == selected,
                onClick = {
                    haptic.confirm()
                    onSelect(order)
                },
                shapes = ListItemDefaults.segmentedShapes(index = index, count = orders.size),
                colors = SegmentedListDefaults.colors(),
                modifier = Modifier.fillMaxWidth().clickCursor(),
                verticalAlignment = Alignment.CenterVertically,
                leadingContent = {
                    LeadingTrailingContentWrapper {
                        // The item handles the click, the radio button only shows the choice
                        RadioButton(selected = order == selected, onClick = null)
                    }
                },
                content = {
                    Text(
                        order.fixed?.let { typedDatePattern(it, withTime = false) }
                            ?: stringResource(
                                Res.string.behavior_date_order_automatic,
                                languagePattern,
                            ),
                    )
                },
            )
        }
    }
}

private val ProductFactsSource.description
    get() = when (this) {
        ProductFactsSource.FOOD -> Res.string.behavior_source_food
        ProductFactsSource.BEAUTY -> Res.string.behavior_source_beauty
        ProductFactsSource.PET_FOOD -> Res.string.behavior_source_pet_food
        ProductFactsSource.PRODUCTS -> Res.string.behavior_source_products
    }

@Preview
@Composable
private fun BehaviorSettingsPreview() {
    GrocyTheme {
        SettingsScaffoldContent(
            title = stringResource(Res.string.settings_behavior),
            menuActions = SettingsMenuActions(
                onAboutClick = {},
                onLogClick = {},
                logSource = LogSource.LOGCAT,
            ),
            onBackClick = {},
            onHelpClick = {},
            onFeedbackClick = {},
        ) {
            behaviorSettings(
                settings = BehaviorSettings(
                    productFactsSources = setOf(ProductFactsSource.FOOD),
                    dateKeyboard = true,
                ),
                onSourceChange = { _, _ -> },
                onDateKeyboardChange = {},
                onDateOrderChange = {},
                onBarcodeUsesAmountChange = {},
            )
        }
    }
}
