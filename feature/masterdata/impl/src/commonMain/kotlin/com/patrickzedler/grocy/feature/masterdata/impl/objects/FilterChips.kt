package com.patrickzedler.grocy.feature.masterdata.impl.objects

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
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
 * Grocy's filter row as Material 3 chips. Scrolls sideways on narrow screens; [contentPadding]
 * scrolls along. The row lies on the screen's background, so the outlined chips get a container
 * color of their own and no elevation, not even on hover.
 */
@Composable
internal fun FilterChipRow(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            // Inside the scrolling, so chips scroll up to the screen's edge instead of a margin
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
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
    val haptic = LocalHapticFeedback.current
    var expanded by remember { mutableStateOf(false) }
    val active = !isDefault(selected)

    Box {
        FilterChip(
            selected = active,
            onClick = {
                haptic.confirm()
                expanded = true
            },
            label = { Text(if (active) optionLabel(selected) else label, maxLines = 1) },
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
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(index = 0, count = 1),
                // Long lists of choices scroll inside the menu
                modifier = Modifier
                    .heightIn(max = MenuMaxHeight)
                    .verticalScroll(rememberScrollState()),
            ) {
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
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = 0, count = 2)) {
                fields.forEachIndexed { index, option ->
                    SelectableDropdownMenuItem(
                        selected = option == field,
                        modifier = Modifier.clickCursor(),
                        onClick = {
                            haptic.confirm()
                            onSortChange(option, ascending)
                            expanded = false
                        },
                        text = { Text(fieldLabel(option)) },
                        shapes = MenuDefaults.itemShape(index = index, count = fields.size),
                        selectedLeadingIcon = { MenuIcon(Res.drawable.ic_rounded_check) },
                    )
                }
            }
            Spacer(Modifier.height(MenuDefaults.GroupSpacing))
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = 1, count = 2)) {
                listOf(true, false).forEachIndexed { index, direction ->
                    SelectableDropdownMenuItem(
                        selected = ascending == direction,
                        modifier = Modifier.clickCursor(),
                        onClick = {
                            haptic.confirm()
                            onSortChange(field, direction)
                            expanded = false
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
        }
    }
}

@Composable
private fun filterChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
)

@Composable
private fun filterChipElevation() = FilterChipDefaults.filterChipElevation(
    elevation = 0.dp,
    pressedElevation = 0.dp,
    focusedElevation = 0.dp,
    hoveredElevation = 0.dp,
    draggedElevation = 0.dp,
    disabledElevation = 0.dp,
)

@Composable
private fun ChipIcon(icon: DrawableResource) {
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

private val MenuMaxHeight = 360.dp
