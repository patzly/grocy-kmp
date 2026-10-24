package com.patrickzedler.grocy.feature.settings.impl.general

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.data.log.LogSource
import com.patrickzedler.grocy.core.design.GrocyTheme
import com.patrickzedler.grocy.core.design.component.ConnectedButtonGroup
import com.patrickzedler.grocy.core.design.component.HueSlider
import com.patrickzedler.grocy.core.design.component.LeadingTrailingContentWrapper
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.SegmentedTexts
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.segmentedGroup
import com.patrickzedler.grocy.core.design.dynamicColorFollowsSystemContrast
import com.patrickzedler.grocy.core.design.isDynamicColorAvailable
import com.patrickzedler.grocy.core.model.AppColor
import com.patrickzedler.grocy.core.model.AppContrast
import com.patrickzedler.grocy.core.model.AppPaletteStyle
import com.patrickzedler.grocy.core.model.AppTheme
import com.patrickzedler.grocy.core.model.AppearanceSettings
import com.patrickzedler.grocy.core.model.resolveAppLanguage
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.ic_rounded_contrast
import com.patrickzedler.grocy.core.resources.ic_rounded_language
import com.patrickzedler.grocy.core.resources.ic_rounded_palette
import com.patrickzedler.grocy.core.resources.settings_contrast
import com.patrickzedler.grocy.core.resources.settings_contrast_description
import com.patrickzedler.grocy.core.resources.settings_contrast_dynamic
import com.patrickzedler.grocy.core.resources.settings_contrast_dynamic_unsupported
import com.patrickzedler.grocy.core.resources.settings_contrast_high
import com.patrickzedler.grocy.core.resources.settings_contrast_medium
import com.patrickzedler.grocy.core.resources.settings_contrast_standard
import com.patrickzedler.grocy.core.resources.settings_general
import com.patrickzedler.grocy.core.resources.settings_language
import com.patrickzedler.grocy.core.resources.settings_language_system
import com.patrickzedler.grocy.core.resources.settings_style_monochrome
import com.patrickzedler.grocy.core.resources.settings_style_neutral
import com.patrickzedler.grocy.core.resources.settings_style_rainbow
import com.patrickzedler.grocy.core.resources.settings_style_tonal_spot
import com.patrickzedler.grocy.core.resources.settings_style_vibrant
import com.patrickzedler.grocy.core.resources.settings_theme
import com.patrickzedler.grocy.core.resources.settings_theme_auto
import com.patrickzedler.grocy.core.resources.settings_theme_dark
import com.patrickzedler.grocy.core.resources.settings_theme_description
import com.patrickzedler.grocy.core.resources.settings_theme_dynamic
import com.patrickzedler.grocy.core.resources.settings_theme_hue
import com.patrickzedler.grocy.core.resources.settings_theme_light
import com.patrickzedler.grocy.core.resources.settings_theme_static
import com.patrickzedler.grocy.feature.settings.impl.SettingsMenuActions
import com.patrickzedler.grocy.feature.settings.impl.SettingsMenuViewModel
import com.patrickzedler.grocy.feature.settings.impl.SettingsScaffold
import com.patrickzedler.grocy.feature.settings.impl.SettingsScaffoldContent
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun GeneralSettingsScreen(
    viewModel: GeneralSettingsViewModel,
    menuViewModel: SettingsMenuViewModel,
    menuActions: SettingsMenuActions,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }

    if (showLanguageDialog) {
        LanguageDialog(
            selectedLanguage = language,
            onLanguageSelected = viewModel::selectLanguage,
            onDismissRequest = { showLanguageDialog = false },
        )
    }

    SettingsScaffold(
        title = stringResource(Res.string.settings_general),
        menuViewModel = menuViewModel,
        menuActions = menuActions,
        onBackClick = onBackClick,
        modifier = modifier,
    ) {
        // Empty for the moment until the stored settings are read
        appearance?.let {
            generalSettings(
                appearance = it,
                language = language,
                onLanguageClick = { showLanguageDialog = true },
                onAppearanceChange = viewModel::updateAppearance,
            )
        }
    }
}

private fun LazyListScope.generalSettings(
    appearance: AppearanceSettings,
    language: String?,
    onLanguageClick: () -> Unit,
    onAppearanceChange: ((AppearanceSettings) -> AppearanceSettings) -> Unit,
) {
    segmentedGroup(key = "language") {
        LanguageItem(language = language, onClick = onLanguageClick)
    }
    segmentedGroup(key = "appearance") {
        ThemeItem(
            appearance = appearance,
            onAppearanceChange = onAppearanceChange,
            index = 0,
            count = 2,
        )
        ContrastItem(
            appearance = appearance,
            onContrastChange = { contrast -> onAppearanceChange { it.copy(contrast = contrast) } },
            index = 1,
            count = 2,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LanguageItem(
    language: String?,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    // A regional system language shows the translation the app uses for it
    val languageName = if (language == null) {
        stringResource(Res.string.settings_language_system)
    } else {
        languageName(resolveAppLanguage(language) ?: language)
    }

    SegmentedListItem(
        onClick = {
            haptic.confirm()
            onClick()
        },
        shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
        colors = SegmentedListDefaults.colors(),
        modifier = Modifier.fillMaxWidth().clickCursor(),
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = { SettingIcon(Res.drawable.ic_rounded_language) },
        supportingContent = { Text(languageName) },
        content = { Text(stringResource(Res.string.settings_language)) },
    )
}

@Composable
private fun ThemeItem(
    appearance: AppearanceSettings,
    onAppearanceChange: ((AppearanceSettings) -> AppearanceSettings) -> Unit,
    index: Int,
    count: Int,
) {
    val usesDynamicColor = isDynamicColorAvailable && appearance.color == AppColor.DYNAMIC

    ControlItem(icon = Res.drawable.ic_rounded_palette, index = index, count = count) {
        SegmentedTexts(
            headline = stringResource(Res.string.settings_theme),
            supporting = stringResource(Res.string.settings_theme_description),
        )
        // Wallpaper colors exist from Android 12 on
        if (isDynamicColorAvailable) {
            ConnectedButtonGroup(
                options = AppColor.entries,
                checked = appearance.color,
                onCheckedChange = { color -> onAppearanceChange { it.copy(color = color) } },
                label = {
                    stringResource(
                        when (it) {
                            AppColor.DYNAMIC -> Res.string.settings_theme_dynamic
                            AppColor.STATIC -> Res.string.settings_theme_static
                        },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        HueSlider(
            hue = appearance.hue,
            onHueChange = { hue -> onAppearanceChange { it.copy(hue = hue) } },
            contentDescription = stringResource(Res.string.settings_theme_hue),
            enabled = !usesDynamicColor,
            modifier = Modifier.fillMaxWidth(),
        )
        // How the scheme grows from the hue; the overflow menu holds the variants that do not fit
        ConnectedButtonGroup(
            options = AppPaletteStyle.entries,
            checked = appearance.style,
            onCheckedChange = { style -> onAppearanceChange { it.copy(style = style) } },
            enabled = !usesDynamicColor,
            label = { stringResource(it.label) },
            modifier = Modifier.fillMaxWidth(),
        )
        ConnectedButtonGroup(
            options = AppTheme.entries,
            checked = appearance.theme,
            onCheckedChange = { theme -> onAppearanceChange { it.copy(theme = theme) } },
            label = {
                stringResource(
                    when (it) {
                        AppTheme.SYSTEM -> Res.string.settings_theme_auto
                        AppTheme.LIGHT -> Res.string.settings_theme_light
                        AppTheme.DARK -> Res.string.settings_theme_dark
                    },
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ContrastItem(
    appearance: AppearanceSettings,
    onContrastChange: (AppContrast) -> Unit,
    index: Int,
    count: Int,
) {
    val usesDynamicColor = isDynamicColorAvailable && appearance.color == AppColor.DYNAMIC

    ControlItem(icon = Res.drawable.ic_rounded_contrast, index = index, count = count) {
        SegmentedTexts(
            headline = stringResource(Res.string.settings_contrast),
            supporting = stringResource(Res.string.settings_contrast_description),
        )
        if (usesDynamicColor) {
            Text(
                text = stringResource(
                    if (dynamicColorFollowsSystemContrast) {
                        Res.string.settings_contrast_dynamic
                    } else {
                        Res.string.settings_contrast_dynamic_unsupported
                    },
                ),
                style = MaterialTheme.typography.bodyMediumEmphasized,
                color = MaterialTheme.colorScheme.error,
            )
        }
        ConnectedButtonGroup(
            options = AppContrast.entries,
            checked = appearance.contrast,
            onCheckedChange = onContrastChange,
            // The system decides the contrast of dynamic colors
            enabled = !usesDynamicColor,
            label = {
                stringResource(
                    when (it) {
                        AppContrast.STANDARD -> Res.string.settings_contrast_standard
                        AppContrast.MEDIUM -> Res.string.settings_contrast_medium
                        AppContrast.HIGH -> Res.string.settings_contrast_high
                    },
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** A non-clickable item with an icon and controls below its texts. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ControlItem(
    icon: DrawableResource,
    index: Int,
    count: Int,
    content: @Composable () -> Unit,
) {
    ListItem(
        modifier = Modifier.fillMaxWidth(),
        leadingContent = { SettingIcon(icon) },
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = SegmentedListDefaults.colors(),
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                content()
            }
        },
    )
}

@Composable
private fun SettingIcon(icon: DrawableResource) {
    LeadingTrailingContentWrapper {
        Icon(painter = painterResource(icon), contentDescription = null)
    }
}

@Preview
@Composable
private fun GeneralSettingsPreview() {
    GrocyTheme {
        SettingsScaffoldContent(
            title = stringResource(Res.string.settings_general),
            menuActions = SettingsMenuActions(
                onAboutClick = {},
                onLogClick = {},
                logSource = LogSource.LOGCAT,
            ),
            onBackClick = {},
            onHelpClick = {},
            onFeedbackClick = {},
        ) {
            generalSettings(
                appearance = AppearanceSettings(),
                language = null,
                onLanguageClick = {},
                onAppearanceChange = {},
            )
        }
    }
}

private val AppPaletteStyle.label: StringResource
    get() = when (this) {
        AppPaletteStyle.TONAL_SPOT -> Res.string.settings_style_tonal_spot
        AppPaletteStyle.NEUTRAL -> Res.string.settings_style_neutral
        AppPaletteStyle.VIBRANT -> Res.string.settings_style_vibrant
        AppPaletteStyle.RAINBOW -> Res.string.settings_style_rainbow
        AppPaletteStyle.MONOCHROME -> Res.string.settings_style_monochrome
    }
