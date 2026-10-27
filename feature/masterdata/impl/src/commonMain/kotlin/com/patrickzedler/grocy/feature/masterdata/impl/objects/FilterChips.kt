package com.patrickzedler.grocy.feature.masterdata.impl.objects

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CheckableDropdownMenuItem
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import com.patrickzedler.grocy.core.design.component.EdgeToEdgeRow
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.verticalScrollbar
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.ic_rounded_arrow_downward
import com.patrickzedler.grocy.core.resources.ic_rounded_arrow_drop_down
import com.patrickzedler.grocy.core.resources.ic_rounded_arrow_upward
import com.patrickzedler.grocy.core.resources.ic_rounded_check
import com.patrickzedler.grocy.core.resources.ic_rounded_filter_list_off
import com.patrickzedler.grocy.core.resources.products_filter_clear
import com.patrickzedler.grocy.core.resources.products_filter_show_disabled
import com.patrickzedler.grocy.core.resources.products_sort_ascending
import com.patrickzedler.grocy.core.resources.products_sort_descending
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Grocy's filter row as Material 3 chips. Centered on wide screens, scrolls sideways where the
 * chips do not fit; [contentPadding] scrolls along. The row lies on the screen's background, so
 * the outlined chips get a container color of their own and no elevation, not even on hover.
 */
@Composable
internal fun FilterChipRow(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    content: @Composable RowScope.() -> Unit,
) {
    EdgeToEdgeRow(contentPadding = contentPadding, modifier = modifier, content = content)
}

/** Grocy's "Show disabled" checkbox as toggle chip. */
@Composable
internal fun ShowDisabledChip(selected: Boolean, onSelectedChange: (Boolean) -> Unit) {
    val haptic = LocalHapticFeedback.current
    FilterChip(
        selected = selected,
        modifier = Modifier.clickCursor(),
        onClick = {
            haptic.confirm()
            onSelectedChange(!selected)
        },
        label = { Text(stringResource(Res.string.products_filter_show_disabled)) },
        colors = filterChipColors(),
        elevation = filterChipElevation(),
        leadingIcon = if (selected) {
            { ChipIcon(Res.drawable.ic_rounded_check) }
        } else {
            null
        },
    )
}

/** "Clear filter": an action, not an input to remove, so an assist chip with the reset icon. */
@Composable
internal fun ClearFilterChip(onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    AssistChip(
        onClick = {
            haptic.confirm()
            onClick()
        },
        modifier = Modifier.clickCursor(),
        label = { Text(stringResource(Res.string.products_filter_clear)) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = AssistChipDefaults.assistChipElevation(
            elevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
            draggedElevation = 0.dp,
            disabledElevation = 0.dp,
        ),
        leadingIcon = {
            Icon(
                painter = painterResource(Res.drawable.ic_rounded_filter_list_off),
                contentDescription = null,
                modifier = Modifier.size(AssistChipDefaults.IconSize),
            )
        },
    )
}

/**
 * A filter chip that opens the choices of one filter. While a value other than the default is
 * chosen, the chip is selected and shows that value instead of the filter's name.
 */
@Composable
internal fun <T> DropdownFilterChip(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    isDefault: (T) -> Boolean = { it == null },
) {
    DropdownFilterChip(
        label = label,
        optionGroups = listOf(options),
        selected = selected,
        optionLabel = optionLabel,
        onSelect = onSelect,
        isDefault = isDefault,
    )
}

/**
 * [DropdownFilterChip] whose choices come in [optionGroups], one menu group each.
 * [selectedLabel] names a chosen value on the chip, by default like in the menu.
 */
@Composable
internal fun <T> DropdownFilterChip(
    label: String,
    optionGroups: List<List<T>>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    isDefault: (T) -> Boolean = { it == null },
    selectedLabel: @Composable (T) -> String = optionLabel,
) {
    val haptic = LocalHapticFeedback.current
    var expanded by remember { mutableStateOf(false) }
    val active = !isDefault(selected)
    val groups = optionGroups.filter { it.isNotEmpty() }

    Box {
        FilterChip(
            selected = active,
            onClick = {
                haptic.confirm()
                expanded = true
            },
            label = { Text(if (active) selectedLabel(selected) else label, maxLines = 1) },
            // It opens a menu, so it is announced as button with the filter's name
            modifier = Modifier
                .semantics {
                    role = Role.Button
                    if (active) stateDescription = label
                }
                .clickCursor(),
            trailingIcon = { ChipIcon(Res.drawable.ic_rounded_arrow_drop_down) },
            colors = filterChipColors(),
            elevation = filterChipElevation(),
        )
        DropdownMenuPopup(expanded = expanded, onDismissRequest = { expanded = false }) {
            ScrollingMenuGroups(groups.size) { groupIndex ->
                val options = groups[groupIndex]
                options.forEachIndexed { index, option ->
                    SelectableDropdownMenuItem(
                        selected = option == selected,
                        modifier = Modifier.clickCursor(),
                        onClick = {
                            haptic.confirm()
                            expanded = false
                            onSelect(option)
                        },
                        text = { Text(optionLabel(option)) },
                        shapes = MenuDefaults.itemShape(index = index, count = options.size),
                        selectedLeadingIcon = { MenuIcon(Res.drawable.ic_rounded_check) },
                    )
                }
            }
        }
    }
}

/**
 * A chip whose menu turns several [options] on and off, e.g. the shown fields of a list like
 * Grocy's "Table options"; the menu stays open for more changes. Never selected: what it shows
 * is a choice of the list's look, not a filter that hides something.
 */
@Composable
internal fun <T> MultiSelectChip(
    label: String,
    options: List<T>,
    checked: Set<T>,
    optionLabel: @Composable (T) -> String,
    onCheckedChange: (option: T, checked: Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var expanded by remember { mutableStateOf(false) }

    Box {
        FilterChip(
            selected = false,
            onClick = {
                haptic.confirm()
                expanded = true
            },
            label = { Text(label, maxLines = 1) },
            modifier = Modifier.semantics { role = Role.Button }.clickCursor(),
            trailingIcon = { ChipIcon(Res.drawable.ic_rounded_arrow_drop_down) },
            colors = filterChipColors(),
            elevation = filterChipElevation(),
        )
        DropdownMenuPopup(expanded = expanded, onDismissRequest = { expanded = false }) {
            ScrollingMenuGroups(groupCount = 1) {
                options.forEachIndexed { index, option ->
                    val isChecked = option in checked
                    CheckableDropdownMenuItem(
                        checked = isChecked,
                        onCheckedChange = { value ->
                            haptic.confirm()
                            onCheckedChange(option, value)
                        },
                        text = { Text(optionLabel(option)) },
                        shapes = MenuDefaults.itemShape(index = index, count = options.size),
                        modifier = Modifier.clickCursor(),
                        checkedLeadingIcon = { MenuIcon(Res.drawable.ic_rounded_check) },
                    )
                }
            }
        }
    }
}

/**
 * [groupCount] menu groups with Material's gap between them, scrolling together when they are
 * higher than [MenuMaxHeight].
 */
@Composable
private fun ScrollingMenuGroups(
    groupCount: Int,
    group: @Composable ColumnScope.(index: Int) -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(
        Modifier
            .heightIn(max = MenuMaxHeight)
            .verticalScrollbar(scrollState)
            .extendVertically(MenuShadowRoom)
            .verticalScroll(scrollState)
            .padding(vertical = MenuShadowRoom),
    ) {
        repeat(groupCount) { index ->
            if (index > 0) Spacer(Modifier.height(MenuDefaults.GroupSpacing))
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = index, count = groupCount)) {
                group(index)
            }
        }
    }
}

/**
 * Lets the content reach [room] above and below without taking more space: scrolling clips at
 * its bounds, which cut off the groups' shadows below the last one.
 */
private fun Modifier.extendVertically(room: Dp): Modifier = layout { measurable, constraints ->
    val extra = room.roundToPx()
    val placeable = measurable.measure(constraints.offset(vertical = 2 * extra))
    layout(placeable.width, (placeable.height - 2 * extra).coerceAtLeast(0)) {
        placeable.place(0, -extra)
    }
}

/**
 * The sort order like a click on one of Grocy's column headers: the chip names the sorted
 * field, its menu offers the [fields] and both directions. Selected while not the default.
 */
@Composable
internal fun <F> SortChip(
    fields: List<F>,
    field: F,
    ascending: Boolean,
    isDefault: Boolean,
    fieldLabel: @Composable (F) -> String,
    onSortChange: (field: F, ascending: Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var expanded by remember { mutableStateOf(false) }
    val sortDescription = stringResource(
        if (ascending) Res.string.products_sort_ascending else Res.string.products_sort_descending,
    )
    val directionIcon = if (ascending) {
        Res.drawable.ic_rounded_arrow_upward
    } else {
        Res.drawable.ic_rounded_arrow_downward
    }

    Box {
        FilterChip(
            selected = !isDefault,
            onClick = {
                haptic.confirm()
                expanded = true
            },
            label = { Text(fieldLabel(field), maxLines = 1) },
            modifier = Modifier.semantics {
                role = Role.Button
                stateDescription = sortDescription
            }.clickCursor(),
            leadingIcon = { ChipIcon(directionIcon) },
            trailingIcon = { ChipIcon(Res.drawable.ic_rounded_arrow_drop_down) },
            colors = filterChipColors(),
            elevation = filterChipElevation(),
        )
        DropdownMenuPopup(expanded = expanded, onDismissRequest = { expanded = false }) {
            ScrollingMenuGroups(groupCount = 2) { groupIndex ->
                if (groupIndex == 0) {
                    SortFieldItems(fields, field, ascending, fieldLabel, onSortChange) {
                        expanded = false
                    }
                } else {
                    SortDirectionItems(field, ascending, onSortChange) { expanded = false }
                }
            }
        }
    }
}

@Composable
private fun <F> SortFieldItems(
    fields: List<F>,
    field: F,
    ascending: Boolean,
    fieldLabel: @Composable (F) -> String,
    onSortChange: (field: F, ascending: Boolean) -> Unit,
    onDone: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    fields.forEachIndexed { index, option ->
        SelectableDropdownMenuItem(
            selected = option == field,
            modifier = Modifier.clickCursor(),
            onClick = {
                haptic.confirm()
                onSortChange(option, ascending)
                onDone()
            },
            text = { Text(fieldLabel(option)) },
            shapes = MenuDefaults.itemShape(index = index, count = fields.size),
            selectedLeadingIcon = { MenuIcon(Res.drawable.ic_rounded_check) },
        )
    }
}

@Composable
private fun <F> SortDirectionItems(
    field: F,
    ascending: Boolean,
    onSortChange: (field: F, ascending: Boolean) -> Unit,
    onDone: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    listOf(true, false).forEachIndexed { index, direction ->
        SelectableDropdownMenuItem(
            selected = ascending == direction,
            modifier = Modifier.clickCursor(),
            onClick = {
                haptic.confirm()
                onSortChange(field, direction)
                onDone()
            },
            text = {
                Text(
                    stringResource(
                        if (direction) {
                            Res.string.products_sort_ascending
                        } else {
                            Res.string.products_sort_descending
                        },
                    ),
                )
            },
            shapes = MenuDefaults.itemShape(index = index, count = 2),
            leadingIcon = {
                MenuIcon(
                    if (direction) {
                        Res.drawable.ic_rounded_arrow_upward
                    } else {
                        Res.drawable.ic_rounded_arrow_downward
                    },
                )
            },
        )
    }
}

@Composable
private fun filterChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
)

/**
 * No elevation in any state: chips are outlined with a transparent or flat container, and the
 * web draws the hover elevation's shadow of such a chip as a dark frame around it.
 */
@Composable
internal fun filterChipElevation() = FilterChipDefaults.filterChipElevation(
    elevation = 0.dp,
    pressedElevation = 0.dp,
    focusedElevation = 0.dp,
    hoveredElevation = 0.dp,
    draggedElevation = 0.dp,
    disabledElevation = 0.dp,
)

@Composable
internal fun ChipIcon(icon: DrawableResource) {
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        modifier = Modifier.size(FilterChipDefaults.IconSize),
    )
}

@Composable
private fun MenuIcon(icon: DrawableResource) {
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        modifier = Modifier.size(MenuDefaults.LeadingIconSize),
    )
}

// Room for the status menu of the stock overview, longer menus scroll
private val MenuMaxHeight = 480.dp

// Beyond the reach of the menu groups' shadows
private val MenuShadowRoom = 8.dp
