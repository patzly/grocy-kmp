@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.patrickzedler.grocy.feature.batteries.impl

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.patrickzedler.grocy.core.design.component.LeadingTrailingContentWrapper
import com.patrickzedler.grocy.core.design.component.SegmentedContainer
import com.patrickzedler.grocy.core.design.component.SegmentedGroup
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.forms.dateTimeWithRelative
import com.patrickzedler.grocy.core.model.batteries.BatteryDetails
import com.patrickzedler.grocy.core.model.batteries.CurrentBattery
import com.patrickzedler.grocy.core.model.masterdata.Battery
import com.patrickzedler.grocy.core.model.text.todayEpochDay
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.batteries_last_charged
import com.patrickzedler.grocy.core.resources.batteries_next_charge
import com.patrickzedler.grocy.core.resources.battery_charge_cycles_count
import com.patrickzedler.grocy.core.resources.battery_field
import com.patrickzedler.grocy.core.resources.battery_used_in
import com.patrickzedler.grocy.core.resources.chore_never
import com.patrickzedler.grocy.core.resources.chore_unknown
import com.patrickzedler.grocy.core.resources.ic_rounded_battery_charging_full
import com.patrickzedler.grocy.core.resources.ic_rounded_build
import com.patrickzedler.grocy.core.resources.ic_rounded_event
import com.patrickzedler.grocy.core.resources.ic_rounded_fact_check
import com.patrickzedler.grocy.core.resources.ic_rounded_history
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** What Grocy's "Battery overview" card shows of a battery, with what its overview tells on top. */
internal data class BatteryCardData(
    val name: String,
    val description: String?,
    val usedIn: String?,
    val chargeCyclesCount: Int?,
    val lastCharged: String?,
    val next: String?,
)

/** The card of [battery], null without it; [details] are null until downloaded. */
internal fun batteryCardOf(
    battery: Battery?,
    current: CurrentBattery?,
    details: BatteryDetails?,
): BatteryCardData? = battery?.let {
    BatteryCardData(
        name = it.name,
        description = it.description?.takeIf { text -> text.isNotBlank() },
        usedIn = it.usedIn?.takeIf { text -> text.isNotBlank() },
        chargeCyclesCount = details?.chargeCyclesCount,
        lastCharged = details?.lastCharged ?: current?.lastTrackedTime,
        next = current?.nextEstimatedChargeTime,
    )
}

/** The description of a battery, above the facts of its overview like the product overview's. */
@Composable
internal fun BatteryDescriptionItem(description: String) {
    SegmentedContainer(index = 0, count = 1) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A value of Grocy's "Battery overview" with what names it. */
private class BatteryFact(val label: String, val value: String, val icon: DrawableResource)

/**
 * The name and the facts of Grocy's "Battery overview"; "never" like Grocy for a battery not
 * charged yet. Without a [card], while it loads, only the [name].
 */
@Composable
internal fun BatteryFactsGroup(name: String, card: BatteryCardData?) {
    SegmentedGroup {
        val nameFact = BatteryFact(
            label = stringResource(Res.string.battery_field),
            value = name,
            icon = Res.drawable.ic_rounded_battery_charging_full,
        )
        val facts = listOf(nameFact) + card?.let { batteryFacts(it) }.orEmpty()
        facts.forEachIndexed { index, fact ->
            ListItem(
                modifier = Modifier.fillMaxWidth(),
                leadingContent = {
                    LeadingTrailingContentWrapper {
                        Icon(painter = painterResource(fact.icon), contentDescription = null)
                    }
                },
                shapes = ListItemDefaults.segmentedShapes(index = index, count = facts.size),
                colors = SegmentedListDefaults.colors(),
                overlineContent = { Text(fact.label) },
                content = { Text(fact.value) },
            )
        }
    }
}

@Composable
private fun batteryFacts(card: BatteryCardData): List<BatteryFact> {
    val today = remember { todayEpochDay() }
    return listOfNotNull(
        card.usedIn?.let {
            BatteryFact(
                label = stringResource(Res.string.battery_used_in),
                value = it,
                icon = Res.drawable.ic_rounded_build,
            )
        },
        BatteryFact(
            label = stringResource(Res.string.battery_charge_cycles_count),
            value = card.chargeCyclesCount?.toString() ?: stringResource(Res.string.chore_unknown),
            icon = Res.drawable.ic_rounded_fact_check,
        ),
        BatteryFact(
            label = stringResource(Res.string.batteries_last_charged),
            value = card.lastCharged?.let { dateTimeWithRelative(it, today) }
                ?: stringResource(Res.string.chore_never),
            icon = Res.drawable.ic_rounded_history,
        ),
        card.next?.let {
            BatteryFact(
                label = stringResource(Res.string.batteries_next_charge),
                value = dateTimeWithRelative(it, today),
                icon = Res.drawable.ic_rounded_event,
            )
        },
    )
}
