@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.patrickzedler.grocy.feature.chores.impl

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.data.util.RefreshError
import com.patrickzedler.grocy.core.data.util.RefreshState
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.FilledOverflowMenu
import com.patrickzedler.grocy.core.design.component.HapticTooltipBox
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialog
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialogContent
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.animatedItem
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.smallIconButtonSize
import com.patrickzedler.grocy.core.model.chores.CurrentChore
import com.patrickzedler.grocy.core.productcard.rememberGrocycodeDownload
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_close
import com.patrickzedler.grocy.core.resources.action_retry
import com.patrickzedler.grocy.core.resources.chore_edit
import com.patrickzedler.grocy.core.resources.chore_journal
import com.patrickzedler.grocy.core.resources.chore_overview
import com.patrickzedler.grocy.core.resources.chores_reschedule
import com.patrickzedler.grocy.core.resources.chores_skip_next
import com.patrickzedler.grocy.core.resources.chores_track_next
import com.patrickzedler.grocy.core.resources.chores_track_now
import com.patrickzedler.grocy.core.resources.data_load_failed
import com.patrickzedler.grocy.core.resources.data_offline
import com.patrickzedler.grocy.core.resources.grocycode_chore
import com.patrickzedler.grocy.core.resources.grocycode_download_of
import com.patrickzedler.grocy.core.resources.grocycode_print_of
import com.patrickzedler.grocy.core.resources.ic_rounded_edit
import com.patrickzedler.grocy.core.resources.ic_rounded_receipt_long
import com.patrickzedler.grocy.core.scanner.writer.Grocycode
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Where Grocy's "Chore overview" leads: the chore's journal and its form. */
internal class ChoreOverviewLinks(
    val onJournalClick: (choreId: Int) -> Unit,
    val onEditClick: (choreId: Int) -> Unit,
)

/**
 * Grocy's "Chore overview" of the chore [overview] shows, with its actions and their messages:
 * the pages open instead of it, tracking and skipping close it for their message with "Undo".
 */
@Composable
internal fun ChoreOverviewHost(
    overview: ChoreOverviewViewModel,
    links: ChoreOverviewLinks,
    snackbarHostState: SnackbarHostState,
) {
    val shownChore by overview.shownChore.collectAsStateWithLifecycle()
    val state by overview.state.collectAsStateWithLifecycle()
    val refresh by overview.refresh.collectAsStateWithLifecycle()
    val downloadGrocycode = rememberGrocycodeDownload(snackbarHostState)
    val actions = overview.actions

    ChoreActionsHost(actions, state?.users.orEmpty(), snackbarHostState)

    shownChore?.let { chore ->
        // Right after opening it may still be the previously opened chore's
        val current = state?.takeIf { it.choreId == chore.id }
        ChoreOverviewDialog(
            name = chore.name,
            state = current,
            refresh = refresh,
            onRetryClick = overview::refresh,
            onCloseClick = overview::hide,
            menuGroups = choreOverviewMenuGroups(
                state = current,
                onTrack = { row, atNextSchedule ->
                    overview.hide()
                    actions.track(row, atNextSchedule)
                },
                onSkip = { row ->
                    overview.hide()
                    actions.skip(row)
                },
                onReschedule = actions::openReschedule,
                onGrocycodeDownload = { twoDimensional ->
                    downloadGrocycode(Grocycode.chore(chore.id), twoDimensional)
                },
                onGrocycodePrint = { actions.printLabel(chore.id) },
            ),
            onJournalClick = {
                overview.hide()
                links.onJournalClick(chore.id)
            },
            onEditClick = {
                overview.hide()
                links.onEditClick(chore.id)
            },
        )
    }
}

@Composable
private fun ChoreOverviewDialog(
    name: String,
    state: ChoreOverviewUiState?,
    refresh: RefreshState,
    onRetryClick: () -> Unit,
    onCloseClick: () -> Unit,
    menuGroups: List<List<AppBarMenuItem>>,
    onJournalClick: () -> Unit,
    onEditClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    ScrollableAlertDialog(
        onDismissRequest = onCloseClick,
        paneTitle = stringResource(Res.string.chore_overview),
    ) {
        ScrollableAlertDialogContent(
            title = { Text(stringResource(Res.string.chore_overview)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptic.confirm()
                        onCloseClick()
                    },
                    modifier = Modifier.clickCursor(),
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(Res.string.action_close))
                }
            },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(SegmentedListDefaults.GroupSpacing),
            ) {
                ChoreOverviewActions(menuGroups, onJournalClick, onEditClick)
                refresh.error?.let { error ->
                    Text(
                        text = stringResource(
                            if (error == RefreshError.OFFLINE) {
                                Res.string.data_offline
                            } else {
                                Res.string.data_load_failed
                            },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    if (state?.hasDetails != true) {
                        TextButton(
                            onClick = {
                                haptic.confirm()
                                onRetryClick()
                            },
                            modifier = Modifier.clickCursor(),
                            shapes = ButtonDefaults.shapes(),
                        ) {
                            Text(stringResource(Res.string.action_retry))
                        }
                    }
                }
                state?.card?.description?.let { ChoreDescriptionItem(it) }
                // Downloads with stored values show here, the values stay readable meanwhile
                val card = state?.card?.takeIf { state.hasDetails || !refresh.isRefreshing }
                ChoreFactsGroup(name = state?.card?.name ?: name, card = card)
                if (card == null && refresh.isRefreshing) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        LoadingIndicator()
                    }
                }
            }
        }
    }
}

/**
 * The buttons of Grocy's card header as button group, the pressed one widens: the journal and the
 * form as wide buttons, what Grocy's overview offers per row and the grocycode in the menu of a
 * narrow tonal button after them.
 */
@Composable
private fun ChoreOverviewActions(
    menuGroups: List<List<AppBarMenuItem>>,
    onJournalClick: () -> Unit,
    onEditClick: () -> Unit,
) {
    // Always fits, a dialog is at least 280.dp wide
    ButtonGroup(
        overflowIndicator = {},
        // The guidelines' 12.dp between buttons of a small standard group
        horizontalArrangement = ButtonGroupDefaults.HorizontalArrangement,
    ) {
        animatedItem { itemModifier, interactionSource ->
            ChoreActionButton(
                icon = Res.drawable.ic_rounded_receipt_long,
                label = stringResource(Res.string.chore_journal),
                onClick = onJournalClick,
                modifier = itemModifier,
                interactionSource = interactionSource,
            )
        }
        animatedItem { itemModifier, interactionSource ->
            ChoreActionButton(
                icon = Res.drawable.ic_rounded_edit,
                label = stringResource(Res.string.chore_edit),
                onClick = onEditClick,
                modifier = itemModifier,
                interactionSource = interactionSource,
            )
        }
        animatedItem { itemModifier, interactionSource ->
            FilledOverflowMenu(
                groups = menuGroups,
                modifier = itemModifier,
                interactionSource = interactionSource,
            )
        }
    }
}

@Composable
private fun choreOverviewMenuGroups(
    state: ChoreOverviewUiState?,
    onTrack: (CurrentChore, Boolean) -> Unit,
    onSkip: (CurrentChore) -> Unit,
    onReschedule: (CurrentChore) -> Unit,
    onGrocycodeDownload: (twoDimensional: Boolean) -> Unit,
    onGrocycodePrint: () -> Unit,
): List<List<AppBarMenuItem>> {
    val swaps = state?.swapsTrackingButtons == true
    val row = state?.current
    val config = state?.config
    val chore = stringResource(Res.string.grocycode_chore)
    return listOf(
        if (row == null) {
            emptyList()
        } else {
            listOfNotNull(
                AppBarMenuItem(
                    stringResource(
                        if (swaps) Res.string.chores_track_now else Res.string.chores_track_next,
                    ),
                ) { onTrack(row, !swaps) },
                AppBarMenuItem(
                    stringResource(
                        if (swaps) Res.string.chores_track_next else Res.string.chores_track_now,
                    ),
                ) { onTrack(row, swaps) },
                AppBarMenuItem(stringResource(Res.string.chores_skip_next)) { onSkip(row) }
                    .takeIf { state.canSkip },
                AppBarMenuItem(stringResource(Res.string.chores_reschedule)) { onReschedule(row) },
            )
        },
        listOfNotNull(
            AppBarMenuItem(stringResource(Res.string.grocycode_download_of, chore)) {
                onGrocycodeDownload(config?.grocycode2D == true)
            },
            AppBarMenuItem(stringResource(Res.string.grocycode_print_of, chore)) {
                onGrocycodePrint()
            }.takeIf { config?.labelPrinter == true && config.labelPrinterRunsOnServer },
        ),
    ).filter { it.isNotEmpty() }
}

@Composable
private fun ChoreActionButton(
    icon: DrawableResource,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier,
    interactionSource: MutableInteractionSource,
) {
    val haptic = LocalHapticFeedback.current

    HapticTooltipBox(text = label, modifier = modifier) {
        FilledIconButton(
            onClick = {
                haptic.confirm()
                onClick()
            },
            modifier = smallIconButtonSize(IconButtonDefaults.IconButtonWidthOption.Wide)
                .clickCursor(),
            shapes = IconButtonDefaults.shapes(),
            interactionSource = interactionSource,
        ) {
            Icon(painter = painterResource(icon), contentDescription = label)
        }
    }
}
