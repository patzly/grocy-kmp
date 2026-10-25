package com.patrickzedler.grocy.feature.settings.impl

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.MenuAnchorPosition
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.data.log.LogSource
import com.patrickzedler.grocy.core.data.unlock.UnlockRepository
import com.patrickzedler.grocy.core.design.component.AppBarIconButton
import com.patrickzedler.grocy.core.design.component.NavigateBackButton
import com.patrickzedler.grocy.core.design.component.ScreenPane
import com.patrickzedler.grocy.core.design.component.SegmentedListScreen
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.model.UnlockState
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_more_options
import com.patrickzedler.grocy.core.resources.action_send_feedback
import com.patrickzedler.grocy.core.resources.action_show_log
import com.patrickzedler.grocy.core.resources.action_show_logcat
import com.patrickzedler.grocy.core.resources.ic_rounded_more_vert
import com.patrickzedler.grocy.core.resources.title_about
import com.patrickzedler.grocy.core.resources.title_help
import com.patrickzedler.grocy.core.ui.AppDialog
import com.patrickzedler.grocy.core.ui.AppDialogHost
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Unlock state for the feedback dialog of the settings menu. */
internal class SettingsMenuViewModel(unlockRepository: UnlockRepository) : ViewModel() {
    val unlockState: StateFlow<UnlockState?> = unlockRepository.unlockState
}

/** What the overflow menu of every settings screen leads to. */
internal class SettingsMenuActions(
    val onAboutClick: () -> Unit,
    val onLogClick: () -> Unit,
    /** Names the log entry: logcat on Android, the app's own log in browsers. */
    val logSource: LogSource,
)

/**
 * A settings screen: [SegmentedListScreen] with back button and the same overflow menu
 * everywhere (About, Help, Send feedback, Show logcat), including its dialogs. As [pane] of the
 * settings' list-detail layout a page beside the categories has neither: the categories' bar
 * has both. A page opened from such a page keeps its back button ([hasBackButton]).
 */
@Composable
internal fun SettingsScaffold(
    title: String,
    menuViewModel: SettingsMenuViewModel,
    menuActions: SettingsMenuActions,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    pane: ScreenPane? = null,
    hasBackButton: Boolean = pane != ScreenPane.Detail,
    snackbarHostState: SnackbarHostState? = null,
    content: LazyListScope.() -> Unit,
) {
    val unlockState by menuViewModel.unlockState.collectAsStateWithLifecycle()
    var activeDialog by rememberSaveable { mutableStateOf<AppDialog?>(null) }

    AppDialogHost(
        dialog = activeDialog,
        unlockState = unlockState,
        onDialogChange = { activeDialog = it },
    )

    SettingsScaffoldContent(
        title = title,
        menuActions = menuActions,
        onBackClick = onBackClick,
        onHelpClick = { activeDialog = AppDialog.HELP },
        onFeedbackClick = { activeDialog = AppDialog.FEEDBACK },
        modifier = modifier,
        pane = pane,
        hasBackButton = hasBackButton,
        snackbarHostState = snackbarHostState,
        content = content,
    )
}

/** Stateless part of [SettingsScaffold], for previews. */
@Composable
internal fun SettingsScaffoldContent(
    title: String,
    menuActions: SettingsMenuActions,
    onBackClick: () -> Unit,
    onHelpClick: () -> Unit,
    onFeedbackClick: () -> Unit,
    modifier: Modifier = Modifier,
    pane: ScreenPane? = null,
    hasBackButton: Boolean = pane != ScreenPane.Detail,
    snackbarHostState: SnackbarHostState? = null,
    content: LazyListScope.() -> Unit,
) {
    val isBesideCategories = pane == ScreenPane.Detail
    SegmentedListScreen(
        title = title,
        modifier = modifier,
        pane = pane,
        snackbarHostState = snackbarHostState,
        navigationIcon = {
            // Beside the categories their back button leaves both
            if (hasBackButton) NavigateBackButton(onClick = onBackClick)
        },
        actions = {
            if (!isBesideCategories) {
                SettingsMenu(
                    menuActions = menuActions,
                    onHelpClick = onHelpClick,
                    onFeedbackClick = onFeedbackClick,
                )
            }
        },
        content = content,
    )
}

@Composable
private fun SettingsMenu(
    menuActions: SettingsMenuActions,
    onHelpClick: () -> Unit,
    onFeedbackClick: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    AppBarIconButton(
        painter = painterResource(Res.drawable.ic_rounded_more_vert),
        contentDescription = stringResource(Res.string.action_more_options),
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
        val groupCount = 2
        // With a second group the ripples of the first one only look connected to it like this
        val firstGroupItemCount = 4

        DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = 0, count = groupCount)) {
            SettingsMenuItem(
                text = stringResource(Res.string.title_about),
                index = 0,
                count = firstGroupItemCount,
                onClick = {
                    showMenu = false
                    menuActions.onAboutClick()
                },
            )
            SettingsMenuItem(
                text = stringResource(Res.string.title_help),
                index = 1,
                count = firstGroupItemCount,
                onClick = {
                    showMenu = false
                    onHelpClick()
                },
            )
            SettingsMenuItem(
                text = stringResource(Res.string.action_send_feedback),
                index = 2,
                count = firstGroupItemCount,
                onClick = {
                    showMenu = false
                    onFeedbackClick()
                },
            )
        }

        Spacer(Modifier.height(MenuDefaults.GroupSpacing))

        DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = 1, count = groupCount)) {
            SettingsMenuItem(
                text = stringResource(
                    when (menuActions.logSource) {
                        LogSource.LOGCAT -> Res.string.action_show_logcat
                        LogSource.BROWSER -> Res.string.action_show_log
                    },
                ),
                // Index and count as the last item of the group, for the ripple shape
                index = 1,
                count = 2,
                onClick = {
                    showMenu = false
                    menuActions.onLogClick()
                },
            )
        }
    }
}

@Composable
private fun SettingsMenuItem(
    text: String,
    index: Int,
    count: Int,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    DropdownMenuItem(
        text = { Text(text) },
        modifier = Modifier.clickCursor(),
        onClick = {
            haptic.confirm()
            onClick()
        },
        shape = MenuDefaults.itemShape(index = index, count = count).shape,
    )
}
