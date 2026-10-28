package com.patrickzedler.grocy.feature.settings.impl.grocy

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.design.component.DropdownOption
import com.patrickzedler.grocy.core.design.component.NumberStepper
import com.patrickzedler.grocy.core.design.component.ScreenPane
import com.patrickzedler.grocy.core.design.component.SearchableDropdownField
import com.patrickzedler.grocy.core.design.component.SegmentedContainer
import com.patrickzedler.grocy.core.design.component.SegmentedSwitchItem
import com.patrickzedler.grocy.core.design.component.segmentedGroup
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.settings_due_soon_days_hint
import com.patrickzedler.grocy.core.resources.shopping_list_list
import com.patrickzedler.grocy.core.resources.stock_settings_due_soon_days
import com.patrickzedler.grocy.feature.settings.impl.SettingNumberItem
import com.patrickzedler.grocy.feature.settings.impl.SettingsMenuActions
import com.patrickzedler.grocy.feature.settings.impl.SettingsMenuViewModel
import com.patrickzedler.grocy.feature.settings.impl.SettingsScaffold
import com.patrickzedler.grocy.feature.settings.impl.UserSettingEventEffect
import org.jetbrains.compose.resources.stringResource

/**
 * One of Grocy's settings pages of the user (shopping list, recipes, chores, tasks, batteries)
 * with every setting of its page in its groups; each change is saved at once.
 */
@Composable
internal fun GrocySettingsPageScreen(
    viewModel: GrocySettingsPageViewModel,
    menuViewModel: SettingsMenuViewModel,
    menuActions: SettingsMenuActions,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    pane: ScreenPane? = null,
) {
    val options by viewModel.options.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    UserSettingEventEffect(viewModel.events, snackbarHostState)

    SettingsScaffold(
        title = stringResource(viewModel.page.title),
        menuViewModel = menuViewModel,
        menuActions = menuActions,
        onBackClick = onBackClick,
        modifier = modifier,
        pane = pane,
        // Opened from Grocy's settings, it returns to them beside the categories too
        hasBackButton = true,
        snackbarHostState = snackbarHostState,
    ) {
        val form = viewModel.form
        val currentOptions = options
        // Empty for the moment until the stored settings are read
        if (form != null && currentOptions != null) {
            settingsPage(viewModel, form, currentOptions)
        }
    }
}

private fun LazyListScope.settingsPage(
    viewModel: GrocySettingsPageViewModel,
    form: GrocySettingsForm,
    options: GrocySettingsPageOptions,
) {
    viewModel.page.groups(options.config).forEachIndexed { groupIndex, group ->
        segmentedGroup(key = groupIndex, title = group.title) {
            group.settings.forEachIndexed { index, setting ->
                SettingItem(
                    setting = setting,
                    viewModel = viewModel,
                    form = form,
                    options = options,
                    index = index,
                    count = group.settings.size,
                )
            }
        }
    }
}

@Composable
private fun SettingItem(
    setting: GrocySetting,
    viewModel: GrocySettingsPageViewModel,
    form: GrocySettingsForm,
    options: GrocySettingsPageOptions,
    index: Int,
    count: Int,
) {
    when (setting) {
        is GrocySetting.Switch -> SegmentedSwitchItem(
            headline = stringResource(setting.setting.label),
            supporting = setting.setting.hint?.let { stringResource(it) },
            checked = form.switches[setting.setting] == true,
            onCheckedChange = { viewModel.setSwitch(setting.setting, it) },
            index = index,
            count = count,
        )

        is GrocySetting.DueSoonDays -> SettingNumberItem(
            label = stringResource(Res.string.stock_settings_due_soon_days),
            hint = stringResource(Res.string.settings_due_soon_days_hint),
            state = form.dueSoonDays.getValue(setting.setting),
            stepper = NumberStepper(min = 0.0),
            error = form.errors[setting.setting],
            index = index,
            count = count,
        )

        GrocySetting.AutoAddList -> SegmentedContainer(index = index, count = count) {
            SearchableDropdownField(
                label = stringResource(Res.string.shopping_list_list),
                options = options.shoppingLists.map { DropdownOption(it.id, it.name) },
                selectedId = form.autoAddListId,
                onSelectedIdChange = viewModel::setAutoAddList,
                // Like Grocy's choice, which is disabled while products are not added
                enabled = form.switches[GrocySwitchSetting.AUTO_ADD_BELOW_MIN_STOCK] == true,
            )
        }
    }
}
