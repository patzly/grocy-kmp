package com.patrickzedler.grocy.feature.calendar.impl

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.component.StatusColor
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.LocalColorRoleSpec
import com.patrickzedler.grocy.core.design.component.customStatusColorOf
import com.patrickzedler.grocy.core.model.calendar.CalendarColorSetting
import com.patrickzedler.grocy.core.model.calendar.CalendarEvent
import com.patrickzedler.grocy.core.model.calendar.CalendarEventKind
import com.patrickzedler.grocy.core.model.calendar.argbOfHexColor
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.calendar_assigned_to
import com.patrickzedler.grocy.core.resources.calendar_battery_due
import com.patrickzedler.grocy.core.resources.calendar_chore_due
import com.patrickzedler.grocy.core.resources.calendar_meal_plan_note
import com.patrickzedler.grocy.core.resources.calendar_meal_plan_product
import com.patrickzedler.grocy.core.resources.calendar_meal_plan_recipe
import com.patrickzedler.grocy.core.resources.calendar_product_due
import com.patrickzedler.grocy.core.resources.calendar_task_due
import com.patrickzedler.grocy.core.resources.ic_rounded_battery_charging_full
import com.patrickzedler.grocy.core.resources.ic_rounded_checklist
import com.patrickzedler.grocy.core.resources.ic_rounded_cleaning_services
import com.patrickzedler.grocy.core.resources.ic_rounded_edit_note
import com.patrickzedler.grocy.core.resources.ic_rounded_kitchen
import com.patrickzedler.grocy.core.resources.ic_rounded_menu_book
import com.patrickzedler.grocy.core.resources.ic_rounded_restaurant
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The colors of the events by their setting, harmonized with the theme; without a valid one they
 * use the theme's secondary color.
 */
internal class EventColors(state: CalendarUiState) {
    private val hexes = CalendarColorSetting.entries.associateWith { it.valueOf(state.settings) }

    @Composable
    fun of(kind: CalendarEventKind): StatusColor = colorRolesOf(hexes[kind.color].orEmpty())
}

/** The color of [hex] as the theme shows it, also what the color dialogs show of a choice. */
@Composable
internal fun colorRolesOf(hex: String): StatusColor {
    val scheme = MaterialTheme.colorScheme
    val spec = LocalColorRoleSpec.current
    return remember(hex, scheme.primary, scheme.secondary, spec) {
        argbOfHexColor(hex)?.let { customStatusColorOf(Color(it), scheme.primary, spec) }
            ?: StatusColor(scheme.secondary, scheme.onSecondary, scheme.secondary)
    }
}

/**
 * An event like a task in Google Calendar, which has no duration either: one line in its filled
 * color, with what it is about as icon (like a birthday's) where the chip is wide enough. Screen
 * readers get what Grocy's title says.
 */
@Composable
internal fun EventChip(
    event: CalendarEvent,
    text: String,
    colors: EventColors,
    showsIcon: Boolean,
    onClick: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val roles = colors.of(event.kind)
    val description = eventDescription(event)
    val click = {
        haptic.confirm()
        onClick(event)
    }
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .sharedCalendarBounds(event.key)
            .fillMaxWidth()
            .height(ChipHeight)
            .clip(MaterialTheme.shapes.small)
            .background(roles.container)
            .clickable(onClick = click)
            .clickCursor()
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                onClick {
                    click()
                    true
                }
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp),
        ) {
            if (showsIcon) {
                Icon(
                    painter = painterResource(event.kind.icon),
                    contentDescription = null,
                    tint = roles.content,
                    modifier = Modifier.size(ChipIconSize),
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = text,
                // Beside an icon there is room for what is easier to read
                style = if (showsIcon) {
                    MaterialTheme.typography.labelLarge
                } else {
                    MaterialTheme.typography.labelMedium
                },
                color = roles.content,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

/** The scopes of the change between two views, so what they share moves from one to the other. */
@OptIn(ExperimentalSharedTransitionApi::class)
internal class CalendarTransition(
    val shared: SharedTransitionScope,
    val visibility: AnimatedVisibilityScope,
)

internal val LocalCalendarTransition = staticCompositionLocalOf<CalendarTransition?> { null }

/**
 * Marks what means the same in every view under [key] (an event, a day's number), so when the
 * view changes it moves and grows from its place in one view into its place in the other on the
 * theme's spatial spring, like Google Calendar's views turn into each other.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun Modifier.sharedCalendarBounds(key: String): Modifier {
    val transition = LocalCalendarTransition.current ?: return this
    val spec = MaterialTheme.motionScheme.defaultSpatialSpec<Rect>()
    return with(transition.shared) {
        this@sharedCalendarBounds.sharedBounds(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = transition.visibility,
            boundsTransform = BoundsTransform { _, _ -> spec },
        )
    }
}

/** The key under which views share the number of the day [epochDay]. */
internal fun dayKey(epochDay: Long): String = "day-$epochDay"

/** The key under which views share the name of the weekday of [epochDay]. */
internal fun weekdayKey(epochDay: Long): String = "weekday-$epochDay"

/** What Grocy's title says of [event]: its kind, section and name, and to whom it is assigned. */
@Composable
internal fun eventDescription(event: CalendarEvent): String = listOfNotNull(
    event.time,
    stringResource(event.kind.label),
    event.section,
    event.name,
    event.assignedTo?.let { stringResource(Res.string.calendar_assigned_to, it) },
).joinToString(", ")

internal val CalendarEventKind.label: StringResource
    get() = when (this) {
        CalendarEventKind.PRODUCT -> Res.string.calendar_product_due
        CalendarEventKind.TASK -> Res.string.calendar_task_due
        CalendarEventKind.CHORE -> Res.string.calendar_chore_due
        CalendarEventKind.BATTERY -> Res.string.calendar_battery_due
        CalendarEventKind.MEAL_PLAN_RECIPE -> Res.string.calendar_meal_plan_recipe
        CalendarEventKind.MEAL_PLAN_NOTE -> Res.string.calendar_meal_plan_note
        CalendarEventKind.MEAL_PLAN_PRODUCT -> Res.string.calendar_meal_plan_product
    }

internal val CalendarEventKind.icon: DrawableResource
    get() = when (this) {
        CalendarEventKind.PRODUCT -> Res.drawable.ic_rounded_kitchen
        CalendarEventKind.TASK -> Res.drawable.ic_rounded_checklist
        CalendarEventKind.CHORE -> Res.drawable.ic_rounded_cleaning_services
        CalendarEventKind.BATTERY -> Res.drawable.ic_rounded_battery_charging_full
        CalendarEventKind.MEAL_PLAN_RECIPE -> Res.drawable.ic_rounded_menu_book
        CalendarEventKind.MEAL_PLAN_NOTE -> Res.drawable.ic_rounded_edit_note
        CalendarEventKind.MEAL_PLAN_PRODUCT -> Res.drawable.ic_rounded_restaurant
    }

// As high as Material's chips, and still dense enough for many events
internal val ChipHeight = 32.dp
internal val ChipGap = 2.dp
private val ChipIconSize = 18.dp
