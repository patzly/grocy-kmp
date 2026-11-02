@file:OptIn(ExperimentalLayoutApi::class)

package com.patrickzedler.grocy.feature.chores.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.StatusColors
import com.patrickzedler.grocy.core.design.component.SwipeAction
import com.patrickzedler.grocy.core.design.component.SwipeActionBox
import com.patrickzedler.grocy.core.design.component.rememberStatusColors
import com.patrickzedler.grocy.core.forms.FieldChip
import com.patrickzedler.grocy.core.forms.FieldChipView
import com.patrickzedler.grocy.core.forms.ItemButton
import com.patrickzedler.grocy.core.forms.ItemButtons
import com.patrickzedler.grocy.core.forms.ListDetailItem
import com.patrickzedler.grocy.core.forms.labelled
import com.patrickzedler.grocy.core.model.chores.ChoreDueStatus
import com.patrickzedler.grocy.core.model.chores.ChoresOverviewField
import com.patrickzedler.grocy.core.model.chores.CurrentChore
import com.patrickzedler.grocy.core.model.text.todayEpochDay
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.chore_edit
import com.patrickzedler.grocy.core.resources.chore_journal
import com.patrickzedler.grocy.core.resources.chore_overview
import com.patrickzedler.grocy.core.resources.chores_assigned_to
import com.patrickzedler.grocy.core.resources.chores_last_tracked
import com.patrickzedler.grocy.core.resources.chores_next_tracking
import com.patrickzedler.grocy.core.resources.chores_reassigned
import com.patrickzedler.grocy.core.resources.chores_reschedule
import com.patrickzedler.grocy.core.resources.chores_rescheduled
import com.patrickzedler.grocy.core.resources.chores_skip_next
import com.patrickzedler.grocy.core.resources.chores_track_next
import com.patrickzedler.grocy.core.resources.chores_track_now
import com.patrickzedler.grocy.core.resources.grocycode_chore
import com.patrickzedler.grocy.core.resources.grocycode_download_of
import com.patrickzedler.grocy.core.resources.grocycode_print_of
import com.patrickzedler.grocy.core.resources.ic_rounded_dataset
import com.patrickzedler.grocy.core.resources.ic_rounded_event
import com.patrickzedler.grocy.core.resources.ic_rounded_event_repeat
import com.patrickzedler.grocy.core.resources.ic_rounded_fast_forward
import com.patrickzedler.grocy.core.resources.ic_rounded_history
import com.patrickzedler.grocy.core.resources.ic_rounded_person
import com.patrickzedler.grocy.core.resources.ic_rounded_play_arrow
import com.patrickzedler.grocy.core.resources.ic_rounded_swap_horiz
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** What the rows of the chores overview do, Grocy's buttons and menu of a row. */
internal class ChoreRowActions(
    /** Opens the chore's overview, like a click on Grocy's chore name. */
    val onClick: (CurrentChore) -> Unit,
    val onTrack: (CurrentChore, atNextSchedule: Boolean) -> Unit,
    val onSkip: (CurrentChore) -> Unit,
    val onReschedule: (CurrentChore) -> Unit,
    val onJournal: (CurrentChore) -> Unit,
    val onEdit: (CurrentChore) -> Unit,
    val onDownloadGrocycode: (CurrentChore, twoDimensional: Boolean) -> Unit,
    val onPrintLabel: (CurrentChore) -> Unit,
)

/** How a row looks: the shown fields and the server's settings. */
@Immutable
internal class ChoreRowStyle(
    val fields: List<ChoresOverviewField>,
    val swapsTrackingButtons: Boolean,
    val grocycode2D: Boolean,
    val printsLabels: Boolean,
)

/**
 * A row of the chores overview: the name as title, its shown fields as chips below, colored like
 * Grocy's rows (red overdue, blue due today, yellow due soon). Grocy's main button stays on the
 * row; swiping towards the end does the same, towards the start skips, both also in the menu and
 * accessibility actions. [selected] marks the chore shown beside the list, null without details.
 */
@Composable
internal fun ChoreListItem(
    row: ChoreRow,
    style: ChoreRowStyle,
    index: Int,
    count: Int,
    actions: ChoreRowActions,
    selected: Boolean?,
    modifier: Modifier = Modifier,
) {
    val chore = row.chore
    val atNextSchedule = !style.swapsTrackingButtons
    val trackText = trackText(atNextSchedule)
    val skipText = stringResource(Res.string.chores_skip_next)

    SwipeActionBox(
        startAction = SwipeAction(
            icon = painterResource(Res.drawable.ic_rounded_play_arrow),
        ) { actions.onTrack(chore, atNextSchedule) },
        endAction = SwipeAction(
            icon = painterResource(Res.drawable.ic_rounded_fast_forward),
            enabled = row.canSkip,
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary,
        ) { actions.onSkip(chore) },
        modifier = modifier,
    ) {
        ListDetailItem(
            selected = selected,
            onClick = { actions.onClick(chore) },
            index = index,
            count = count,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    customActions = listOfNotNull(
                        CustomAccessibilityAction(trackText) {
                            actions.onTrack(chore, atNextSchedule)
                            true
                        },
                        CustomAccessibilityAction(skipText) {
                            actions.onSkip(chore)
                            true
                        }.takeIf { row.canSkip },
                    )
                },
            supportingContent = { ChoreChips(row, style) },
            trailingContent = {
                ItemButtons(
                    buttons = listOf(
                        ItemButton(
                            painter = painterResource(Res.drawable.ic_rounded_play_arrow),
                            description = trackText,
                        ) { actions.onTrack(chore, atNextSchedule) },
                    ),
                    menuGroups = menuGroups(row, style, actions, atNextSchedule, skipText),
                )
            },
        ) {
            Text(chore.name)
        }
    }
}

@Composable
private fun trackText(atNextSchedule: Boolean): String = stringResource(
    if (atNextSchedule) Res.string.chores_track_next else Res.string.chores_track_now,
)

/** Grocy's menu of a row, in its groups. */
@Composable
private fun menuGroups(
    row: ChoreRow,
    style: ChoreRowStyle,
    actions: ChoreRowActions,
    atNextSchedule: Boolean,
    skipText: String,
): List<List<AppBarMenuItem>> {
    val chore = row.chore
    val choreName = stringResource(Res.string.grocycode_chore)
    return listOf(
        listOf(
            AppBarMenuItem(trackText(!atNextSchedule)) { actions.onTrack(chore, !atNextSchedule) },
            AppBarMenuItem(skipText, enabled = row.canSkip) { actions.onSkip(chore) },
            AppBarMenuItem(stringResource(Res.string.chores_reschedule)) {
                actions.onReschedule(chore)
            },
        ),
        listOf(
            AppBarMenuItem(stringResource(Res.string.chore_overview)) { actions.onClick(chore) },
            AppBarMenuItem(stringResource(Res.string.chore_journal)) { actions.onJournal(chore) },
            AppBarMenuItem(stringResource(Res.string.chore_edit)) { actions.onEdit(chore) },
        ),
        listOfNotNull(
            AppBarMenuItem(stringResource(Res.string.grocycode_download_of, choreName)) {
                actions.onDownloadGrocycode(chore, style.grocycode2D)
            },
            AppBarMenuItem(stringResource(Res.string.grocycode_print_of, choreName)) {
                actions.onPrintLabel(chore)
            }.takeIf { style.printsLabels },
        ),
    )
}

@Composable
private fun ChoreChips(row: ChoreRow, style: ChoreRowStyle) {
    val chips = choreChips(row, style)
    key(chips.map { it.text }) {
        if (chips.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                chips.forEach { FieldChipView(it) }
            }
        }
    }
}

@Composable
private fun choreChips(row: ChoreRow, style: ChoreRowStyle): List<FieldChip> {
    val chore = row.chore
    val colors = rememberStatusColors()
    val today = remember { todayEpochDay() }
    val dateOnly = chore.trackDateOnly
    val chips = mutableListOf<FieldChip>()
    for (field in style.fields) {
        when (field) {
            ChoresOverviewField.NEXT_ESTIMATED_TRACKING ->
                chore.nextEstimatedExecutionTime?.let { next ->
                    val rescheduled = stringResource(Res.string.chores_rescheduled)
                        .takeIf { chore.isRescheduled }
                    chips += FieldChip(
                        text = choreRelativeLabel(next, dateOnly, today),
                        description = labelled(
                            stringResource(Res.string.chores_next_tracking),
                            listOfNotNull(
                                choreDateTimeWithRelative(next, dateOnly, today),
                                rescheduled,
                            ).joinToString(", "),
                        ),
                        icon = if (chore.isRescheduled) {
                            Res.drawable.ic_rounded_event_repeat
                        } else {
                            Res.drawable.ic_rounded_event
                        },
                        status = row.dueStatus?.let { statusColor(it, colors) },
                    )
                }
            ChoresOverviewField.LAST_TRACKED -> chore.lastTrackedTime?.let { last ->
                chips += FieldChip(
                    text = choreRelativeLabel(last, dateOnly, today),
                    description = labelled(
                        stringResource(Res.string.chores_last_tracked),
                        choreDateTimeWithRelative(last, dateOnly, today),
                    ),
                    icon = Res.drawable.ic_rounded_history,
                )
            }
            ChoresOverviewField.ASSIGNED_TO -> row.assignedUserName?.let { name ->
                val reassigned = stringResource(Res.string.chores_reassigned)
                    .takeIf { chore.isReassigned }
                chips += FieldChip(
                    text = name,
                    description = labelled(
                        stringResource(Res.string.chores_assigned_to),
                        listOfNotNull(name, reassigned).joinToString(", "),
                    ),
                    icon = if (chore.isReassigned) {
                        Res.drawable.ic_rounded_swap_horiz
                    } else {
                        Res.drawable.ic_rounded_person
                    },
                )
            }
            ChoresOverviewField.USERFIELDS -> row.userfields.forEach { (caption, value) ->
                chips += FieldChip(
                    text = value,
                    description = labelled(caption, value),
                    icon = Res.drawable.ic_rounded_dataset,
                )
            }
        }
    }
    return chips
}

private fun statusColor(status: ChoreDueStatus, colors: StatusColors) = when (status) {
    ChoreDueStatus.OVERDUE -> colors.critical
    ChoreDueStatus.DUE_TODAY -> colors.info
    ChoreDueStatus.DUE_SOON -> colors.warning
}
