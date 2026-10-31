package com.patrickzedler.grocy.feature.settings.impl.general

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.GrocyTheme
import com.patrickzedler.grocy.core.design.component.LeadingTrailingContentWrapper
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialog
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialogContent
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.dialogListScrollbar
import com.patrickzedler.grocy.core.design.component.tryOpenUri
import com.patrickzedler.grocy.core.model.AppLanguages
import com.patrickzedler.grocy.core.model.resolveAppLanguage
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_close
import com.patrickzedler.grocy.core.resources.action_learn_more
import com.patrickzedler.grocy.core.resources.app_translate
import com.patrickzedler.grocy.core.resources.grocy_language_default
import com.patrickzedler.grocy.core.resources.grocy_language_default_description
import com.patrickzedler.grocy.core.resources.grocy_language_info
import com.patrickzedler.grocy.core.resources.settings_language
import com.patrickzedler.grocy.core.resources.settings_language_info
import com.patrickzedler.grocy.core.resources.settings_language_system
import com.patrickzedler.grocy.core.resources.settings_language_system_description
import org.jetbrains.compose.resources.stringResource

/** Single choice between the system language and every translation of the app. */
@Composable
internal fun LanguageDialog(
    selectedLanguage: String?,
    onLanguageSelected: (String?) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val uriHandler = LocalUriHandler.current
    val appTranslate = stringResource(Res.string.app_translate)

    ScrollableAlertDialog(onDismissRequest = onDismissRequest) {
        LanguageDialogContent(
            languages = AppLanguages,
            // The system language may be regional ("de-CH"), its translation is the selected one
            selectedLanguage = resolveAppLanguage(selectedLanguage) ?: selectedLanguage,
            subtitle = stringResource(Res.string.settings_language_info),
            defaultName = stringResource(Res.string.settings_language_system),
            defaultDescription = stringResource(Res.string.settings_language_system_description),
            onLanguageClick = { tag ->
                haptic.confirm()
                onDismissRequest()
                if (tag != selectedLanguage) onLanguageSelected(tag)
            },
            onLearnMoreClick = {
                haptic.confirm()
                uriHandler.tryOpenUri(appTranslate)
            },
            onCloseClick = {
                haptic.confirm()
                onDismissRequest()
            },
        )
    }
}

/**
 * Single choice between Grocy's default and the [languages] Grocy has, for Grocy's own
 * language of the user: Grocy's codes ("pt_BR"), named in their language like the app's.
 */
@Composable
internal fun GrocyLanguageDialog(
    languages: List<String>,
    selectedLanguage: String?,
    onLanguageSelected: (String?) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    ScrollableAlertDialog(onDismissRequest = onDismissRequest) {
        LanguageDialogContent(
            languages = languages,
            selectedLanguage = selectedLanguage,
            subtitle = stringResource(Res.string.grocy_language_info),
            defaultName = stringResource(Res.string.grocy_language_default),
            defaultDescription = stringResource(Res.string.grocy_language_default_description),
            onLanguageClick = { code ->
                haptic.confirm()
                onDismissRequest()
                if (code != selectedLanguage) onLanguageSelected(code)
            },
            onLearnMoreClick = null,
            onCloseClick = {
                haptic.confirm()
                onDismissRequest()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LanguageDialogContent(
    languages: List<String>,
    selectedLanguage: String?,
    subtitle: String,
    defaultName: String,
    defaultDescription: String,
    modifier: Modifier = Modifier,
    onLanguageClick: (String?) -> Unit = {},
    onLearnMoreClick: (() -> Unit)? = {},
    onCloseClick: () -> Unit = {},
) {
    ScrollableAlertDialogContent(
        modifier = modifier,
        title = { Text(stringResource(Res.string.settings_language)) },
        subtitle = { Text(subtitle) },
        confirmButton = {
            TextButton(
                onClick = onCloseClick,
                modifier = Modifier.clickCursor(),
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(stringResource(Res.string.action_close))
            }
        },
        extraButton = onLearnMoreClick?.let { onClick ->
            @Composable {
                TextButton(
                    onClick = onClick,
                    modifier = Modifier.clickCursor(),
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(Res.string.action_learn_more))
                }
            }
        },
        isScrollableControlledByContent = true,
    ) {
        val colors = SegmentedListDefaults.colors()
        val count = languages.size + 1
        val namedLanguages = remember(languages) {
            languages
                // Grocy's codes separate the region with "_"
                .map { it to languageName(it.replace('_', '-')) }
                .sortedWith(compareBy(languageNameComparator()) { (_, name) -> name })
        }

        // The current language is in view right away, also far down the list
        val listState = rememberLazyListState(
            initialFirstVisibleItemIndex = if (selectedLanguage == null) {
                0
            } else {
                // Behind the system language, the first item
                namedLanguages.indexOfFirst { (tag, _) -> tag == selectedLanguage } + 1
            },
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .dialogListScrollbar(listState),
            state = listState,
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            item(key = "system") {
                LanguageItem(
                    name = defaultName,
                    description = defaultDescription,
                    selected = selectedLanguage == null,
                    index = 0,
                    count = count,
                    colors = colors,
                    onClick = { onLanguageClick(null) },
                )
            }
            itemsIndexed(
                items = namedLanguages,
                key = { _, (tag, _) -> tag },
            ) { index, (tag, name) ->
                LanguageItem(
                    name = name,
                    description = null,
                    selected = selectedLanguage == tag,
                    index = index + 1,
                    count = count,
                    colors = colors,
                    onClick = { onLanguageClick(tag) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LanguageItem(
    name: String,
    description: String?,
    selected: Boolean,
    index: Int,
    count: Int,
    colors: ListItemColors,
    onClick: () -> Unit,
) {
    SegmentedListItem(
        selected = selected,
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = colors,
        modifier = Modifier.fillMaxWidth().clickCursor(),
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = {
            LeadingTrailingContentWrapper {
                // The item handles the click, the radio button only shows the choice
                RadioButton(selected = selected, modifier = Modifier.clickCursor(), onClick = null)
            }
        },
        supportingContent = description?.let { { Text(it) } },
        content = { Text(name) },
    )
}

@Preview
@Composable
private fun LanguageDialogPreview() {
    GrocyTheme {
        LanguageDialogContent(
            languages = listOf("de", "en"),
            selectedLanguage = "en",
            subtitle = stringResource(Res.string.settings_language_info),
            defaultName = stringResource(Res.string.settings_language_system),
            defaultDescription = stringResource(Res.string.settings_language_system_description),
        )
    }
}
