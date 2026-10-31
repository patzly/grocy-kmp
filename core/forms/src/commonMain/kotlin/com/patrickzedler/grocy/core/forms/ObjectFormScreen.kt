@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.patrickzedler.grocy.core.forms

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.FloatingToolbarIconButton
import com.patrickzedler.grocy.core.design.component.FloatingToolbarWithFab
import com.patrickzedler.grocy.core.design.component.NavigateBackButton
import com.patrickzedler.grocy.core.design.component.ScreenPane
import com.patrickzedler.grocy.core.design.component.SegmentedListScreen
import com.patrickzedler.grocy.core.design.component.insetItem
import com.patrickzedler.grocy.core.design.component.segmentedGroup
import com.patrickzedler.grocy.core.navigation.LeaveGuard
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_save
import com.patrickzedler.grocy.core.resources.action_scroll_to_top
import com.patrickzedler.grocy.core.resources.data_save_failed
import com.patrickzedler.grocy.core.resources.data_save_failed_details
import com.patrickzedler.grocy.core.resources.data_save_offline
import com.patrickzedler.grocy.core.resources.data_saved
import com.patrickzedler.grocy.core.resources.ic_rounded_save
import com.patrickzedler.grocy.core.resources.ic_rounded_vertical_align_top
import com.patrickzedler.grocy.core.resources.product_userfields
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * What a form needs from where it is shown.
 *
 * @param menuItems the app's menu items, after the actions on the object in the overflow menu.
 * @param leaveGuard reports that the list beside the form leaves or shows another object.
 * @param isBesideList the form is the detail pane beside its list: no back button of its own,
 *   the list's small app bar.
 * @param objectActions copying, merging and deleting the edited object; [onActionDone] gets
 *   what succeeded.
 */
class FormFrame(
    val menuItems: List<AppBarMenuItem>,
    val leaveGuard: LeaveGuard,
    val isBesideList: Boolean,
    val objectActions: ObjectActionsViewModel,
    val onBackClick: () -> Unit,
    val onActionDone: (ObjectActionEvent) -> Unit,
)

/**
 * The screen of a master data form like the product form: [content] adds the groups of the
 * form, its userfields follow in a group of their own. Saving is the toolbar's FAB; leaving with
 * unsaved changes asks first, also when the list beside it leaves. The overflow menu acts on the
 * saved object; a deleted object, or one merged into another, closes the form.
 */
@Composable
fun <F : ObjectFormState<*>> ObjectFormScreen(
    title: String,
    viewModel: ObjectFormViewModel<*, F>,
    frame: FormFrame,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    toolbarActions: @Composable RowScope.() -> Unit = {},
    content: LazyListScope.(form: F) -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // Saveable, so the question and the pending leave survive rotating the device
    var showUnsavedDialog by rememberSaveable { mutableStateOf(false) }
    var leaveAfterSave by rememberSaveable { mutableStateOf(false) }
    // Choosing another item of the list beside the form leaves differently than back
    var pendingLeave by remember { mutableStateOf<(() -> Unit)?>(null) }
    val onBackClick = frame.onBackClick
    val leaveGuard = frame.leaveGuard
    val currentOnBackClick by rememberUpdatedState(onBackClick)
    val leave = { (pendingLeave ?: currentOnBackClick)() }
    val requestBack = {
        pendingLeave = null
        if (viewModel.hasUnsavedChanges) showUnsavedDialog = true else onBackClick()
    }

    DisposableEffect(leaveGuard, viewModel) {
        leaveGuard.onLeaveRequest = { action ->
            if (viewModel.hasUnsavedChanges) {
                pendingLeave = action
                showUnsavedDialog = true
            } else {
                action()
            }
        }
        onDispose { leaveGuard.onLeaveRequest = null }
    }
    // The back gesture and the browser's back ask too
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = viewModel.hasUnsavedChanges && !viewModel.isSaving,
        onBackCompleted = {
            pendingLeave = null
            showUnsavedDialog = true
        },
    )
    if (showUnsavedDialog) {
        UnsavedChangesDialog(
            onSave = { leaveAfterSave = viewModel.save() },
            onDiscard = leave,
            onDismissRequest = { showUnsavedDialog = false },
        )
    }

    ObjectActionsHost(frame.objectActions, snackbarHostState) { event ->
        val id = viewModel.objectId
        val isGone = event is ObjectActionEvent.Deleted && event.id == id ||
            event is ObjectActionEvent.Merged && event.removeId == id
        // Its changes went with it, nothing to ask
        if (isGone) onBackClick()
        frame.onActionDone(event)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val leaving = leaveAfterSave
            leaveAfterSave = false
            when (event) {
                ObjectFormEvent.Saved -> leave()
                ObjectFormEvent.Created -> if (leaving) {
                    leave()
                } else {
                    snackbarHostState.showSnackbar(getString(Res.string.data_saved))
                }
                ObjectFormEvent.Offline ->
                    snackbarHostState.showSnackbar(getString(Res.string.data_save_offline))
                is ObjectFormEvent.Failed -> snackbarHostState.showSnackbar(
                    listOfNotNull(
                        getString(Res.string.data_save_failed),
                        event.serverMessage?.let {
                            getString(Res.string.data_save_failed_details, it)
                        },
                    ).joinToString("\n"),
                )
            }
        }
    }

    SegmentedListScreen(
        title = title,
        modifier = modifier,
        navigationIcon = {
            // Beside the list its own back button leaves both
            if (!frame.isBesideList) NavigateBackButton(onClick = requestBack)
        },
        menuItems = frame.menuItems,
        actionMenuItems = if (viewModel.isNew || viewModel.load !is ObjectFormLoad.Ready) {
            emptyList()
        } else {
            objectActionItems(frame.objectActions, viewModel.objectId)
        },
        pane = ScreenPane.Detail.takeIf { frame.isBesideList },
        listState = listState,
        snackbarHostState = snackbarHostState,
        floatingToolbar = { scrollBehavior, toolbarModifier ->
            FloatingToolbarWithFab(
                scrollBehavior = scrollBehavior,
                modifier = toolbarModifier,
                fabPainter = painterResource(Res.drawable.ic_rounded_save),
                fabDescription = stringResource(Res.string.action_save),
                onFabClick = { viewModel.save() },
            ) {
                toolbarActions()
                FloatingToolbarIconButton(
                    painter = painterResource(Res.drawable.ic_rounded_vertical_align_top),
                    contentDescription = stringResource(Res.string.action_scroll_to_top),
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                    enabled = listState.canScrollBackward,
                )
            }
        },
    ) {
        when (val load = viewModel.load) {
            is ObjectFormLoad.Ready -> {
                content(load.form)
                if (load.userfields.isNotEmpty()) {
                    segmentedGroup(key = "userfields", title = Res.string.product_userfields) {
                        val errors = viewModel.userfieldErrors
                        load.userfields.forEachIndexed { index, input ->
                            UserfieldItem(
                                input = input,
                                error = errors[input.userfield.name],
                                index = index,
                                count = load.userfields.size,
                            )
                        }
                    }
                }
            }

            ObjectFormLoad.NotFound -> Unit

            ObjectFormLoad.Loading -> insetItem(key = "loading") {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            }
        }
    }
}
