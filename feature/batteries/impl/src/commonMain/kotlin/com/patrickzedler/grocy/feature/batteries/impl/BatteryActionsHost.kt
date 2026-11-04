package com.patrickzedler.grocy.feature.batteries.impl

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.patrickzedler.grocy.core.forms.actionMessage
import com.patrickzedler.grocy.core.forms.dateTimeText
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_undo
import com.patrickzedler.grocy.core.resources.batteries_track_failed
import com.patrickzedler.grocy.core.resources.batteries_tracked
import com.patrickzedler.grocy.core.resources.batteries_undone
import com.patrickzedler.grocy.core.resources.grocycode_print_failed
import com.patrickzedler.grocy.core.resources.grocycode_printed
import org.jetbrains.compose.resources.getString

/** The messages of [actions] with Grocy's "Undo", in [snackbarHostState]. */
@Composable
internal fun BatteryActionsHost(actions: BatteryActions, snackbarHostState: SnackbarHostState) {
    LaunchedEffect(actions) {
        actions.events.collect { event ->
            when (event) {
                is BatteryEvent.Tracked -> showTracked(event, snackbarHostState, actions::undo)
                BatteryEvent.Undone ->
                    snackbarHostState.showSnackbar(getString(Res.string.batteries_undone))
                BatteryEvent.LabelPrinted ->
                    snackbarHostState.showSnackbar(getString(Res.string.grocycode_printed))
                is BatteryEvent.LabelFailed -> snackbarHostState.showSnackbar(
                    event.error.actionMessage(Res.string.grocycode_print_failed),
                )
                is BatteryEvent.Failed -> snackbarHostState.showSnackbar(
                    event.error.actionMessage(Res.string.batteries_track_failed),
                )
            }
        }
    }
}

/** Grocy's "Tracked charge cycle of battery %s on %s" with "Undo". */
internal suspend fun showTracked(
    event: BatteryEvent.Tracked,
    snackbarHostState: SnackbarHostState,
    onUndo: (BatteryEvent.Tracked) -> Unit,
) {
    val result = snackbarHostState.showSnackbar(
        message = getString(
            Res.string.batteries_tracked,
            event.batteryName,
            dateTimeText(event.trackedTime),
        ),
        actionLabel = getString(Res.string.action_undo),
        duration = SnackbarDuration.Long,
    )
    if (result == SnackbarResult.ActionPerformed) onUndo(event)
}
