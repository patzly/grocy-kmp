@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.patrickzedler.grocy.feature.chores.impl

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
import com.patrickzedler.grocy.core.model.User
import com.patrickzedler.grocy.core.model.chores.ChoreDetails
import com.patrickzedler.grocy.core.model.chores.CurrentChore
import com.patrickzedler.grocy.core.model.masterdata.Chore
import com.patrickzedler.grocy.core.model.text.durationOf
import com.patrickzedler.grocy.core.model.text.formatDuration
import com.patrickzedler.grocy.core.model.text.todayEpochDay
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.chore_average_frequency
import com.patrickzedler.grocy.core.resources.chore_hours
import com.patrickzedler.grocy.core.resources.chore_last_done_by
import com.patrickzedler.grocy.core.resources.chore_name
import com.patrickzedler.grocy.core.resources.chore_never
import com.patrickzedler.grocy.core.resources.chore_tracked_count
import com.patrickzedler.grocy.core.resources.chore_unknown
import com.patrickzedler.grocy.core.resources.chores_assigned_to
import com.patrickzedler.grocy.core.resources.chores_last_tracked
import com.patrickzedler.grocy.core.resources.chores_next_tracking
import com.patrickzedler.grocy.core.resources.chores_reassigned
import com.patrickzedler.grocy.core.resources.chores_rescheduled
import com.patrickzedler.grocy.core.resources.ic_rounded_calculate
import com.patrickzedler.grocy.core.resources.ic_rounded_cleaning_services
import com.patrickzedler.grocy.core.resources.ic_rounded_event
import com.patrickzedler.grocy.core.resources.ic_rounded_event_repeat
import com.patrickzedler.grocy.core.resources.ic_rounded_fact_check
import com.patrickzedler.grocy.core.resources.ic_rounded_history
import com.patrickzedler.grocy.core.resources.ic_rounded_person
import com.patrickzedler.grocy.core.resources.ic_rounded_swap_horiz
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** What Grocy's "Chore overview" card shows of a chore, with what its overview tells on top. */
internal data class ChoreCardData(
    val name: String,
    val description: String?,
    val trackedCount: Int?,
    val averageFrequencyHours: Double?,
    val lastTracked: String?,
    val trackDateOnly: Boolean,
    val lastDoneByName: String?,
    val next: String?,
    val isRescheduled: Boolean,
    val assignedUserName: String?,
    val isReassigned: Boolean,
    val showsAssignments: Boolean,
)

/** The card of [chore], null without anything to show; [details] are null until downloaded. */
internal fun choreCardOf(
    chore: Chore?,
    current: CurrentChore?,
    details: ChoreDetails?,
    users: List<User>,
    showsAssignments: Boolean,
): ChoreCardData? {
    val name = chore?.name ?: current?.name ?: return null
    return ChoreCardData(
        name = name,
        description = chore?.description?.takeIf { it.isNotBlank() },
        trackedCount = details?.trackedCount,
        averageFrequencyHours = details?.averageFrequencyHours,
        lastTracked = details?.lastTracked ?: current?.lastTrackedTime,
        trackDateOnly = chore?.trackDateOnly ?: current?.trackDateOnly ?: false,
        lastDoneByName = details?.lastDoneByName,
        next = current?.nextEstimatedExecutionTime,
        isRescheduled = current?.isRescheduled == true,
        assignedUserName = current?.assignedUserId
            ?.let { id -> users.firstOrNull { it.id == id }?.displayName },
        isReassigned = current?.isReassigned == true,
        showsAssignments = showsAssignments,
    )
}

/** The description of Grocy's "Chore overview", above its facts like the product overview's. */
@Composable
internal fun ChoreDescriptionItem(description: String) {
    SegmentedContainer(index = 0, count = 1) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A value of Grocy's "Chore overview" with what names it. */
private class ChoreFact(val label: String, val value: String, val icon: DrawableResource)

/**
 * The name and the facts of Grocy's "Chore overview"; "never" and "Unknown" like Grocy for what is
 * not known. Without a [card], while it loads, only the [name].
 */
@Composable
internal fun ChoreFactsGroup(name: String, card: ChoreCardData?) {
    SegmentedGroup {
        val nameFact = ChoreFact(
            label = stringResource(Res.string.chore_name),
            value = name,
            icon = Res.drawable.ic_rounded_cleaning_services,
        )
        val facts = listOf(nameFact) + card?.let { choreFacts(it) }.orEmpty()
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
private fun choreFacts(card: ChoreCardData): List<ChoreFact> {
    val today = remember { todayEpochDay() }
    val unknown = stringResource(Res.string.chore_unknown)
    val next = card.next?.let { dateTimeWithRelative(it, today, card.trackDateOnly) }
        ?.let { text ->
            if (card.isRescheduled) {
                "$text · ${stringResource(Res.string.chores_rescheduled)}"
            } else {
                text
            }
        }
    val assigned = card.assignedUserName?.let { name ->
        if (card.isReassigned) "$name · ${stringResource(Res.string.chores_reassigned)}" else name
    }
    return listOfNotNull(
        next?.let {
            ChoreFact(
                label = stringResource(Res.string.chores_next_tracking),
                value = it,
                // The same icons as the chips of the chore's row
                icon = if (card.isRescheduled) {
                    Res.drawable.ic_rounded_event_repeat
                } else {
                    Res.drawable.ic_rounded_event
                },
            )
        },
        assigned?.takeIf { card.showsAssignments }?.let {
            ChoreFact(
                label = stringResource(Res.string.chores_assigned_to),
                value = it,
                icon = if (card.isReassigned) {
                    Res.drawable.ic_rounded_swap_horiz
                } else {
                    Res.drawable.ic_rounded_person
                },
            )
        },
        ChoreFact(
            label = stringResource(Res.string.chore_tracked_count),
            value = card.trackedCount?.toString() ?: unknown,
            icon = Res.drawable.ic_rounded_fact_check,
        ),
        ChoreFact(
            label = stringResource(Res.string.chore_average_frequency),
            value = card.averageFrequencyHours?.let { frequencyText(it) } ?: unknown,
            icon = Res.drawable.ic_rounded_calculate,
        ),
        ChoreFact(
            label = stringResource(Res.string.chores_last_tracked),
            value = card.lastTracked
                ?.let { dateTimeWithRelative(it, today, card.trackDateOnly) }
                ?: stringResource(Res.string.chore_never),
            icon = Res.drawable.ic_rounded_history,
        ),
        ChoreFact(
            label = stringResource(Res.string.chore_last_done_by),
            value = card.lastDoneByName ?: unknown,
            icon = Res.drawable.ic_rounded_person,
        ).takeIf { card.showsAssignments },
    )
}

/** Grocy's humanized average, e.g. "3 days"; hours for less than a day like moment.js. */
@Composable
private fun frequencyText(hours: Double): String = if (hours < HOURS_UP_TO_DAYS) {
    val whole = hours.roundToInt().coerceAtLeast(1)
    pluralStringResource(Res.plurals.chore_hours, whole, whole)
} else {
    formatDuration(durationOf(hours / HOURS_PER_DAY))
}

private const val HOURS_PER_DAY = 24
