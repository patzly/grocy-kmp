@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.patrickzedler.grocy.feature.batteries.impl

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
import com.patrickzedler.grocy.core.productcard.rememberGrocycodeDownload
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_close
import com.patrickzedler.grocy.core.resources.action_retry
import com.patrickzedler.grocy.core.resources.batteries_track
import com.patrickzedler.grocy.core.resources.battery_edit
import com.patrickzedler.grocy.core.resources.battery_journal
import com.patrickzedler.grocy.core.resources.battery_overview
import com.patrickzedler.grocy.core.resources.data_load_failed
import com.patrickzedler.grocy.core.resources.data_offline
import com.patrickzedler.grocy.core.resources.grocycode_battery
import com.patrickzedler.grocy.core.resources.grocycode_download_of
import com.patrickzedler.grocy.core.resources.grocycode_print_of
import com.patrickzedler.grocy.core.resources.ic_rounded_edit
import com.patrickzedler.grocy.core.resources.ic_rounded_receipt_long
import com.patrickzedler.grocy.core.scanner.writer.Grocycode
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Where Grocy's "Battery overview" leads: the battery's journal and its form. */
internal class BatteryOverviewLinks(
    val onJournalClick: (batteryId: Int) -> Unit,
    val onEditClick: (batteryId: Int) -> Unit,
)

/**
 * Grocy's "Battery overview" of the battery [overview] shows, with its actions and their messages:
 * the pages open instead of it, tracking closes it for their message with "Undo".
 */
@Composable
internal fun BatteryOverviewHost(
    overview: BatteryOverviewViewModel,
    links: BatteryOverviewLinks,
    snackbarHostState: SnackbarHostState,
) {
    val shownBattery by overview.shownBattery.collectAsStateWithLifecycle()
    val state by overview.state.collectAsStateWithLifecycle()
    val refresh by overview.refresh.collectAsStateWithLifecycle()
    val downloadGrocycode = rememberGrocycodeDownload(snackbarHostState)
    val actions = overview.actions

    BatteryActionsHost(actions, snackbarHostState)

    shownBattery?.let { battery ->
        // Right after opening it may still be the previously opened battery's
        val current = state?.takeIf { it.batteryId == battery.id }
        BatteryOverviewDialog(
            name = battery.name,
            state = current,
            refresh = refresh,
            onRetryClick = overview::refresh,
            onCloseClick = overview::hide,
            menuGroups = batteryOverviewMenuGroups(
                state = current,
                onTrack = {
                    overview.hide()
                    actions.track(battery.id, battery.name)
                },
                onGrocycodeDownload = { twoDimensional ->
                    downloadGrocycode(Grocycode.battery(battery.id), twoDimensional)
                },
                onGrocycodePrint = { actions.printLabel(battery.id) },
            ),
            onJournalClick = {
                overview.hide()
                links.onJournalClick(battery.id)
            },
            onEditClick = {
                overview.hide()
                links.onEditClick(battery.id)
            },
        )
    }
}

@Composable
private fun BatteryOverviewDialog(
    name: String,
    state: BatteryOverviewUiState?,
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
        paneTitle = stringResource(Res.string.battery_overview),
    ) {
        ScrollableAlertDialogContent(
            title = { Text(stringResource(Res.string.battery_overview)) },
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
                BatteryOverviewActions(menuGroups, onJournalClick, onEditClick)
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
                state?.card?.description?.let { BatteryDescriptionItem(it) }
                // Downloads with stored values show here, the values stay readable meanwhile
                val card = state?.card?.takeIf { state.hasDetails || !refresh.isRefreshing }
                BatteryFactsGroup(name = state?.card?.name ?: name, card = card)
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
private fun BatteryOverviewActions(
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
            BatteryActionButton(
                icon = Res.drawable.ic_rounded_receipt_long,
                label = stringResource(Res.string.battery_journal),
                onClick = onJournalClick,
                modifier = itemModifier,
                interactionSource = interactionSource,
            )
        }
        animatedItem { itemModifier, interactionSource ->
            BatteryActionButton(
                icon = Res.drawable.ic_rounded_edit,
                label = stringResource(Res.string.battery_edit),
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
private fun batteryOverviewMenuGroups(
    state: BatteryOverviewUiState?,
    onTrack: () -> Unit,
    onGrocycodeDownload: (twoDimensional: Boolean) -> Unit,
    onGrocycodePrint: () -> Unit,
): List<List<AppBarMenuItem>> {
    val config = state?.config
    val battery = stringResource(Res.string.grocycode_battery)
    return listOf(
        listOf(AppBarMenuItem(stringResource(Res.string.batteries_track), onClick = onTrack)),
        listOfNotNull(
            AppBarMenuItem(stringResource(Res.string.grocycode_download_of, battery)) {
                onGrocycodeDownload(config?.grocycode2D == true)
            },
            AppBarMenuItem(stringResource(Res.string.grocycode_print_of, battery)) {
                onGrocycodePrint()
            }.takeIf { config?.labelPrinter == true && config.labelPrinterRunsOnServer },
        ),
    )
}

@Composable
private fun BatteryActionButton(
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
