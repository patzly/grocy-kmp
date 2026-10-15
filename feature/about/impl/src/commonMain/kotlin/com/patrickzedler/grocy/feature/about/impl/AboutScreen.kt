package com.patrickzedler.grocy.feature.about.impl

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MenuAnchorPosition
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.design.GrocyTheme
import com.patrickzedler.grocy.core.design.component.AppBarIconButton
import com.patrickzedler.grocy.core.design.component.LeadingContentWrapper
import com.patrickzedler.grocy.core.design.component.NavigateBackButton
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.SegmentedListScreen
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.segmentedGroup
import com.patrickzedler.grocy.core.design.component.tryOpenUri
import com.patrickzedler.grocy.core.model.UnlockState
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.about_changelog
import com.patrickzedler.grocy.core.resources.about_changelog_description
import com.patrickzedler.grocy.core.resources.about_developer
import com.patrickzedler.grocy.core.resources.about_github
import com.patrickzedler.grocy.core.resources.about_github_description
import com.patrickzedler.grocy.core.resources.about_key
import com.patrickzedler.grocy.core.resources.about_key_description_ignored
import com.patrickzedler.grocy.core.resources.about_key_description_installed
import com.patrickzedler.grocy.core.resources.about_key_description_not_installed
import com.patrickzedler.grocy.core.resources.about_privacy
import com.patrickzedler.grocy.core.resources.about_privacy_description
import com.patrickzedler.grocy.core.resources.about_translation
import com.patrickzedler.grocy.core.resources.about_translation_description
import com.patrickzedler.grocy.core.resources.about_vending
import com.patrickzedler.grocy.core.resources.about_vending_description
import com.patrickzedler.grocy.core.resources.about_version
import com.patrickzedler.grocy.core.resources.action_more_options
import com.patrickzedler.grocy.core.resources.action_recommend
import com.patrickzedler.grocy.core.resources.action_send_feedback
import com.patrickzedler.grocy.core.resources.app_developer
import com.patrickzedler.grocy.core.resources.app_github
import com.patrickzedler.grocy.core.resources.app_privacy
import com.patrickzedler.grocy.core.resources.app_translate
import com.patrickzedler.grocy.core.resources.app_vending_dev
import com.patrickzedler.grocy.core.resources.app_vending_key
import com.patrickzedler.grocy.core.resources.app_website
import com.patrickzedler.grocy.core.resources.ic_rounded_code
import com.patrickzedler.grocy.core.resources.ic_rounded_copyright
import com.patrickzedler.grocy.core.resources.ic_rounded_history
import com.patrickzedler.grocy.core.resources.ic_rounded_info
import com.patrickzedler.grocy.core.resources.ic_rounded_key
import com.patrickzedler.grocy.core.resources.ic_rounded_more_vert
import com.patrickzedler.grocy.core.resources.ic_rounded_person
import com.patrickzedler.grocy.core.resources.ic_rounded_policy
import com.patrickzedler.grocy.core.resources.ic_rounded_shop
import com.patrickzedler.grocy.core.resources.ic_rounded_translate
import com.patrickzedler.grocy.core.resources.license_author_google
import com.patrickzedler.grocy.core.resources.license_google_sans_flex
import com.patrickzedler.grocy.core.resources.license_material_icons
import com.patrickzedler.grocy.core.resources.title_about
import com.patrickzedler.grocy.core.resources.title_help
import com.patrickzedler.grocy.core.resources.title_licenses
import com.patrickzedler.grocy.core.ui.AppDialog
import com.patrickzedler.grocy.core.ui.AppDialogHost
import com.patrickzedler.grocy.core.ui.rememberRecommendAction
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AboutScreen(
    viewModel: AboutViewModel,
    versionName: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val unlockState by viewModel.unlockState.collectAsStateWithLifecycle()
    var activeDialog by rememberSaveable { mutableStateOf<AppDialog?>(null) }
    val recommend = rememberRecommendAction(isPlayVariant = unlockState != null)
    val appVendingKey = stringResource(Res.string.app_vending_key)

    LifecycleResumeEffect(viewModel) {
        viewModel.refreshUnlockState()
        onPauseOrDispose {}
    }

    AppDialogHost(
        dialog = activeDialog,
        unlockState = unlockState,
        onDialogChange = { activeDialog = it },
    )

    AboutContent(
        versionName = versionName,
        unlockState = unlockState,
        modifier = modifier,
        onBackClick = onBackClick,
        onHelpClick = { activeDialog = AppDialog.HELP },
        onRecommendClick = recommend,
        onFeedbackClick = { activeDialog = AppDialog.FEEDBACK },
        onChangelogClick = { activeDialog = AppDialog.CHANGELOG },
        onKeyClick = {
            if (unlockState?.isKeyInstalled == true) {
                uriHandler.tryOpenUri(appVendingKey)
            } else {
                activeDialog = AppDialog.UNLOCK
            }
        },
        onKeyLongClick = viewModel::onKeyLongClick,
        onLicenseFontClick = { activeDialog = AppDialog.LICENSE_FONT },
        onLicenseIconsClick = { activeDialog = AppDialog.LICENSE_ICONS },
    )
}

@Composable
private fun AboutContent(
    versionName: String,
    unlockState: UnlockState?,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onHelpClick: () -> Unit = {},
    onRecommendClick: () -> Unit = {},
    onFeedbackClick: () -> Unit = {},
    onChangelogClick: () -> Unit = {},
    onKeyClick: () -> Unit = {},
    onKeyLongClick: () -> Unit = {},
    onLicenseFontClick: () -> Unit = {},
    onLicenseIconsClick: () -> Unit = {},
) {
    val uriHandler = LocalUriHandler.current
    val appWebsite = stringResource(Res.string.app_website)
    val appVendingDev = stringResource(Res.string.app_vending_dev)
    val appGithub = stringResource(Res.string.app_github)
    val appTranslate = stringResource(Res.string.app_translate)
    val appPrivacy = stringResource(Res.string.app_privacy)

    SegmentedListScreen(
        title = stringResource(Res.string.title_about),
        modifier = modifier,
        navigationIcon = { NavigateBackButton(onClick = onBackClick) },
        actions = {
            AboutMenu(
                onHelpClick = onHelpClick,
                onRecommendClick = onRecommendClick,
                onFeedbackClick = onFeedbackClick,
            )
        },
    ) {
        segmentedGroup {
            val colors = SegmentedListDefaults.colors()
            VersionItem(versionName = versionName, colors = colors)
            AboutItem(
                title = stringResource(Res.string.about_changelog),
                icon = Res.drawable.ic_rounded_history,
                index = 1,
                count = 4,
                colors = colors,
                onClick = onChangelogClick,
                supporting = stringResource(Res.string.about_changelog_description),
            )
            AboutItem(
                title = stringResource(Res.string.app_developer),
                icon = Res.drawable.ic_rounded_person,
                index = 2,
                count = 4,
                colors = colors,
                onClick = { uriHandler.tryOpenUri(appWebsite) },
                overline = stringResource(Res.string.about_developer),
            )
            AboutItem(
                title = stringResource(Res.string.about_vending),
                icon = Res.drawable.ic_rounded_shop,
                index = 3,
                count = 4,
                colors = colors,
                onClick = { uriHandler.tryOpenUri(appVendingDev) },
                supporting = stringResource(Res.string.about_vending_description),
            )
        }

        if (unlockState?.isPlayStoreInstalled == true) {
            segmentedGroup {
                AboutItem(
                    title = stringResource(Res.string.about_key),
                    icon = Res.drawable.ic_rounded_key,
                    index = 0,
                    count = 1,
                    colors = SegmentedListDefaults.colors(),
                    onClick = onKeyClick,
                    supporting = stringResource(
                        when {
                            unlockState.isKeyInstalled ->
                                Res.string.about_key_description_installed
                            !unlockState.checkUnlockKey ->
                                Res.string.about_key_description_ignored
                            else -> Res.string.about_key_description_not_installed
                        },
                    ),
                    onLongClick = onKeyLongClick,
                )
            }
        }

        segmentedGroup {
            val colors = SegmentedListDefaults.colors()
            AboutItem(
                title = stringResource(Res.string.about_github),
                icon = Res.drawable.ic_rounded_code,
                index = 0,
                count = 3,
                colors = colors,
                onClick = { uriHandler.tryOpenUri(appGithub) },
                supporting = stringResource(Res.string.about_github_description),
            )
            AboutItem(
                title = stringResource(Res.string.about_translation),
                icon = Res.drawable.ic_rounded_translate,
                index = 1,
                count = 3,
                colors = colors,
                onClick = { uriHandler.tryOpenUri(appTranslate) },
                supporting = stringResource(Res.string.about_translation_description),
            )
            AboutItem(
                title = stringResource(Res.string.about_privacy),
                icon = Res.drawable.ic_rounded_policy,
                index = 2,
                count = 3,
                colors = colors,
                onClick = { uriHandler.tryOpenUri(appPrivacy) },
                supporting = stringResource(Res.string.about_privacy_description),
            )
        }

        segmentedGroup(title = Res.string.title_licenses) {
            val colors = SegmentedListDefaults.colors()
            AboutItem(
                title = stringResource(Res.string.license_google_sans_flex),
                icon = Res.drawable.ic_rounded_copyright,
                index = 0,
                count = 2,
                colors = colors,
                onClick = onLicenseFontClick,
                supporting = stringResource(Res.string.license_author_google),
            )
            AboutItem(
                title = stringResource(Res.string.license_material_icons),
                icon = Res.drawable.ic_rounded_copyright,
                index = 1,
                count = 2,
                colors = colors,
                onClick = onLicenseIconsClick,
                supporting = stringResource(Res.string.license_author_google),
            )
        }
    }
}

@Composable
private fun AboutMenu(
    onHelpClick: () -> Unit,
    onRecommendClick: () -> Unit,
    onFeedbackClick: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    val moreText = stringResource(Res.string.action_more_options)

    AppBarIconButton(
        painter = painterResource(Res.drawable.ic_rounded_more_vert),
        contentDescription = moreText,
        onClick = { showMenu = true },
        narrow = true,
    )

    DropdownMenuPopup(
        expanded = showMenu,
        onDismissRequest = { showMenu = false },
        popupPositionProvider = MenuDefaults.rememberDropdownMenuPopupPositionProvider(
            dropdownMenuAnchorPosition = MenuAnchorPosition.Below,
            offset = DpOffset(x = (-8).dp, y = 0.dp),
        ),
    ) {
        DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = 0, count = 1)) {
            AboutMenuItem(
                text = stringResource(Res.string.title_help),
                index = 0,
                onClick = {
                    showMenu = false
                    onHelpClick()
                },
            )
            AboutMenuItem(
                text = stringResource(Res.string.action_recommend),
                index = 1,
                onClick = {
                    showMenu = false
                    onRecommendClick()
                },
            )
            AboutMenuItem(
                text = stringResource(Res.string.action_send_feedback),
                index = 2,
                onClick = {
                    showMenu = false
                    onFeedbackClick()
                },
            )
        }
    }
}

private const val ABOUT_MENU_ITEM_COUNT = 3

@Composable
private fun AboutMenuItem(
    text: String,
    index: Int,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    DropdownMenuItem(
        text = { Text(text) },
        onClick = {
            haptic.confirm()
            onClick()
        },
        shape = MenuDefaults.itemShape(index = index, count = ABOUT_MENU_ITEM_COUNT).shape,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VersionItem(
    versionName: String,
    colors: ListItemColors,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier.fillMaxWidth(),
        overlineContent = { Text(stringResource(Res.string.about_version)) },
        leadingContent = { AboutItemIcon(Res.drawable.ic_rounded_info) },
        shapes = ListItemDefaults.segmentedShapes(index = 0, count = 4),
        colors = colors,
        content = { Text(versionName) },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AboutItem(
    title: String,
    icon: DrawableResource,
    index: Int,
    count: Int,
    colors: ListItemColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    overline: String? = null,
    supporting: String? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    SegmentedListItem(
        onClick = {
            haptic.confirm()
            onClick()
        },
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = colors,
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        overlineContent = overline?.let { { Text(it) } },
        supportingContent = supporting?.let { { Text(it) } },
        leadingContent = { AboutItemIcon(icon) },
        onLongClick = onLongClick,
        content = { Text(title) },
    )
}

@Composable
private fun AboutItemIcon(icon: DrawableResource) {
    LeadingContentWrapper {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
        )
    }
}

@Preview
@Composable
private fun AboutKeyNotInstalledPreview() {
    GrocyTheme {
        AboutContent(
            versionName = "1.0.0",
            unlockState = UnlockState(
                isKeyInstalled = false,
                isPlayStoreInstalled = true,
                checkUnlockKey = true,
            ),
        )
    }
}

@Preview
@Composable
private fun AboutKeyInstalledPreview() {
    GrocyTheme {
        AboutContent(
            versionName = "1.0.0",
            unlockState = UnlockState(
                isKeyInstalled = true,
                isPlayStoreInstalled = true,
                checkUnlockKey = true,
            ),
        )
    }
}

@Preview
@Composable
private fun AboutFossPreview() {
    GrocyTheme {
        AboutContent(
            versionName = "1.0.0",
            unlockState = null,
        )
    }
}
