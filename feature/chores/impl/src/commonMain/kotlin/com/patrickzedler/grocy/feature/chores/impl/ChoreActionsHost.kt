package com.patrickzedler.grocy.feature.chores.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import com.patrickzedler.grocy.core.design.component.DropdownOption
import com.patrickzedler.grocy.core.design.component.LeadingTrailingContentWrapper
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.showNewestSnackbar
import com.patrickzedler.grocy.core.forms.dateTimeText
import com.patrickzedler.grocy.core.forms.DateTextItem
import com.patrickzedler.grocy.core.forms.DropdownItem
import com.patrickzedler.grocy.core.forms.FormDialog
import com.patrickzedler.grocy.core.forms.TextItem
import com.patrickzedler.grocy.core.forms.actionMessage
import com.patrickzedler.grocy.core.forms.fieldErrorText
import com.patrickzedler.grocy.core.forms.saveMessage
import com.patrickzedler.grocy.core.model.User
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_ok
import com.patrickzedler.grocy.core.resources.action_undo
import com.patrickzedler.grocy.core.resources.chore_date_required
import com.patrickzedler.grocy.core.resources.chore_reset
import com.patrickzedler.grocy.core.resources.chores_assigned_to
import com.patrickzedler.grocy.core.resources.chores_next_tracking
import com.patrickzedler.grocy.core.resources.chores_reschedule
import com.patrickzedler.grocy.core.resources.chores_track_failed
import com.patrickzedler.grocy.core.resources.chores_tracked
import com.patrickzedler.grocy.core.resources.chores_undone
import com.patrickzedler.grocy.core.resources.grocycode_print_failed
import com.patrickzedler.grocy.core.resources.grocycode_printed
import com.patrickzedler.grocy.core.resources.ic_rounded_settings_backup_restore
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The messages of [actions] with Grocy's "Undo", in [snackbarHostState], and its reschedule
 * dialog; [users] are its choices, none without the assignments feature.
 */
@Composable
internal fun ChoreActionsHost(
    actions: ChoreActions,
    users: List<User>,
    snackbarHostState: SnackbarHostState,
) {
    LaunchedEffect(actions) {
        actions.events.collectLatest { event ->
            when (event) {
                is ChoreEvent.Tracked -> showTracked(event, snackbarHostState, actions::undo)
                ChoreEvent.Undone ->
                    snackbarHostState.showNewestSnackbar(getString(Res.string.chores_undone))
                ChoreEvent.LabelPrinted ->
                    snackbarHostState.showNewestSnackbar(getString(Res.string.grocycode_printed))
                is ChoreEvent.LabelFailed -> snackbarHostState.showNewestSnackbar(
                    event.error.actionMessage(Res.string.grocycode_print_failed),
                )
                is ChoreEvent.Failed -> snackbarHostState.showNewestSnackbar(
                    event.error.actionMessage(Res.string.chores_track_failed),
                )
            }
        }
    }
    actions.reschedule?.let { form ->
        RescheduleDialog(
            form = form,
            users = users,
            onSave = actions::saveReschedule,
            onReset = actions::resetReschedule,
            onDismissRequest = actions::dismissReschedule,
        )
    }
}

/** Grocy's "Tracked execution of chore %s on %s" with "Undo". */
internal suspend fun showTracked(
    event: ChoreEvent.Tracked,
    snackbarHostState: SnackbarHostState,
    onUndo: (ChoreEvent.Tracked) -> Unit,
) {
    val result = snackbarHostState.showNewestSnackbar(
        message = getString(
            Res.string.chores_tracked,
            event.choreName,
            dateTimeText(event.trackedTime, event.trackDateOnly),
        ),
        actionLabel = getString(Res.string.action_undo),
        duration = SnackbarDuration.Long,
    )
    if (result == SnackbarResult.ActionPerformed) onUndo(event)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RescheduleDialog(
    form: RescheduleForm,
    users: List<User>,
    onSave: () -> Unit,
    onReset: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val hasUsers = users.isNotEmpty()

    FormDialog(
        title = stringResource(Res.string.chores_reschedule),
        subtitle = form.chore.name,
        isSaving = form.isSaving,
        error = form.error?.saveMessage(),
        onSave = onSave,
        onDismissRequest = onDismissRequest,
        saveText = stringResource(Res.string.action_ok),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            val count = if (hasUsers) 2 else 1
            DateTextItem(
                withTime = !form.chore.trackDateOnly,
                state = form.date,
                label = stringResource(Res.string.chores_next_tracking),
                error = fieldErrorText(form.dateError, Res.string.chore_date_required),
                isLastField = !hasUsers,
                index = 0,
                count = count,
            )
            if (hasUsers) {
                DropdownItem(
                    label = stringResource(Res.string.chores_assigned_to),
                    options = users.map { DropdownOption(it.id, it.displayName) },
                    selectedId = form.assignedUserId,
                    onSelectedIdChange = { form.assignedUserId = it },
                    onUnmatchedChange = { form.isUserUnmatched = it },
                    optional = true,
                    isLastField = true,
                    index = 1,
                    count = count,
                )
            }
        }
        if (form.canReset) {
            SegmentedListItem(
                onClick = {
                    haptic.confirm()
                    onReset()
                },
                enabled = !form.isSaving,
                shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
                colors = SegmentedListDefaults.colors(),
                modifier = Modifier.fillMaxWidth().clickCursor(enabled = !form.isSaving),
                leadingContent = {
                    LeadingTrailingContentWrapper {
                        Icon(
                            painterResource(Res.drawable.ic_rounded_settings_backup_restore),
                            contentDescription = null,
                        )
                    }
                },
            ) {
                Text(stringResource(Res.string.chore_reset))
            }
        }
    }
}
