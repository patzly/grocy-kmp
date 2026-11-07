@file:OptIn(ExperimentalLayoutApi::class)

package com.patrickzedler.grocy.feature.batteries.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.patrickzedler.grocy.core.forms.dateTimeWithRelative
import com.patrickzedler.grocy.core.forms.labelled
import com.patrickzedler.grocy.core.forms.relativeDateTimeLabel
import com.patrickzedler.grocy.core.model.batteries.BatteriesOverviewField
import com.patrickzedler.grocy.core.model.batteries.BatteryDueStatus
import com.patrickzedler.grocy.core.model.masterdata.Battery
import com.patrickzedler.grocy.core.model.text.todayEpochDay
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.batteries_last_charged
import com.patrickzedler.grocy.core.resources.batteries_next_charge
import com.patrickzedler.grocy.core.resources.batteries_track
import com.patrickzedler.grocy.core.resources.battery_edit
import com.patrickzedler.grocy.core.resources.battery_journal
import com.patrickzedler.grocy.core.resources.battery_overview
import com.patrickzedler.grocy.core.resources.battery_used_in
import com.patrickzedler.grocy.core.resources.grocycode_battery
import com.patrickzedler.grocy.core.resources.grocycode_download_of
import com.patrickzedler.grocy.core.resources.grocycode_print_of
import com.patrickzedler.grocy.core.resources.ic_rounded_battery_charging_90
import com.patrickzedler.grocy.core.resources.ic_rounded_build
import com.patrickzedler.grocy.core.resources.ic_rounded_dataset
import com.patrickzedler.grocy.core.resources.ic_rounded_event
import com.patrickzedler.grocy.core.resources.ic_rounded_history
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** What the rows of the batteries overview do, Grocy's button and menu of a row. */
internal class BatteryRowActions(
    /** Opens the battery's overview, like a click on Grocy's battery name. */
    val onClick: (Battery) -> Unit,
    val onTrack: (Battery) -> Unit,
    val onJournal: (Battery) -> Unit,
    val onEdit: (Battery) -> Unit,
    val onDownloadGrocycode: (Battery, twoDimensional: Boolean) -> Unit,
    val onPrintLabel: (Battery) -> Unit,
)

/** How a row looks: the shown fields and the server's settings. */
@Immutable
internal class BatteryRowStyle(
    val fields: List<BatteriesOverviewField>,
    val grocycode2D: Boolean,
    val printsLabels: Boolean,
)

/**
 * A row of the batteries overview: the name as title, its shown fields as chips below, colored
 * like Grocy's rows (red overdue, blue due today, yellow due soon). Grocy's "Track charge cycle"
 * stays on the row; swiping towards the end does the same, also in the accessibility actions.
 * [selected] marks the battery shown beside the list, null without details.
 */
@Composable
internal fun BatteryListItem(
    row: BatteryRow,
    style: BatteryRowStyle,
    index: Int,
    count: Int,
    actions: BatteryRowActions,
    selected: Boolean?,
    modifier: Modifier = Modifier,
) {
    val battery = row.battery
    val trackText = stringResource(Res.string.batteries_track)
    val trackIcon = painterResource(Res.drawable.ic_rounded_battery_charging_90)

    SwipeActionBox(
        startAction = SwipeAction(icon = trackIcon) { actions.onTrack(battery) },
        endAction = null,
        modifier = modifier,
    ) {
        ListDetailItem(
            selected = selected,
            onClick = { actions.onClick(battery) },
            index = index,
            count = count,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    customActions = listOf(
                        CustomAccessibilityAction(trackText) {
                            actions.onTrack(battery)
                            true
                        },
                    )
                },
            supportingContent = { BatteryChips(row, style) },
            trailingContent = {
                ItemButtons(
                    buttons = listOf(
                        ItemButton(painter = trackIcon, description = trackText) {
                            actions.onTrack(battery)
                        },
                    ),
                    menuGroups = menuGroups(battery, style, actions),
                )
            },
        ) {
            Text(battery.name)
        }
    }
}

/** Grocy's menu of a row, in its groups. */
@Composable
private fun menuGroups(
    battery: Battery,
    style: BatteryRowStyle,
    actions: BatteryRowActions,
): List<List<AppBarMenuItem>> {
    val name = stringResource(Res.string.grocycode_battery)
    return listOf(
        listOf(
            AppBarMenuItem(stringResource(Res.string.battery_overview)) {
                actions.onClick(battery)
            },
            AppBarMenuItem(stringResource(Res.string.battery_journal)) {
                actions.onJournal(battery)
            },
            AppBarMenuItem(stringResource(Res.string.battery_edit)) { actions.onEdit(battery) },
        ),
        listOfNotNull(
            AppBarMenuItem(stringResource(Res.string.grocycode_download_of, name)) {
                actions.onDownloadGrocycode(battery, style.grocycode2D)
            },
            AppBarMenuItem(stringResource(Res.string.grocycode_print_of, name)) {
                actions.onPrintLabel(battery)
            }.takeIf { style.printsLabels },
        ),
    )
}

@Composable
private fun BatteryChips(row: BatteryRow, style: BatteryRowStyle) {
    val chips = batteryChips(row, style)
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
private fun batteryChips(row: BatteryRow, style: BatteryRowStyle): List<FieldChip> {
    val colors = rememberStatusColors()
    val today = remember { todayEpochDay() }
    val chips = mutableListOf<FieldChip>()
    for (field in style.fields) {
        when (field) {
            BatteriesOverviewField.USED_IN -> row.battery.usedIn?.ifBlank { null }?.let { usedIn ->
                chips += FieldChip(
                    text = usedIn,
                    description = labelled(stringResource(Res.string.battery_used_in), usedIn),
                    icon = Res.drawable.ic_rounded_build,
                )
            }
            BatteriesOverviewField.LAST_CHARGED -> row.current.lastTrackedTime?.let { last ->
                chips += FieldChip(
                    text = relativeDateTimeLabel(last, today),
                    description = labelled(
                        stringResource(Res.string.batteries_last_charged),
                        dateTimeWithRelative(last, today),
                    ),
                    icon = Res.drawable.ic_rounded_history,
                )
            }
            BatteriesOverviewField.NEXT_CHARGE ->
                row.current.nextEstimatedChargeTime?.let { next ->
                    chips += FieldChip(
                        text = relativeDateTimeLabel(next, today),
                        description = labelled(
                            stringResource(Res.string.batteries_next_charge),
                            dateTimeWithRelative(next, today),
                        ),
                        icon = Res.drawable.ic_rounded_event,
                        status = row.dueStatus?.let { statusColor(it, colors) },
                    )
                }
            BatteriesOverviewField.USERFIELDS -> row.userfields.forEach { (caption, value) ->
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

private fun statusColor(status: BatteryDueStatus, colors: StatusColors) = when (status) {
    BatteryDueStatus.OVERDUE -> colors.critical
    BatteryDueStatus.DUE_TODAY -> colors.info
    BatteryDueStatus.DUE_SOON -> colors.warning
}
