package com.patrickzedler.grocy.core.design.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.model.text.FuzzySearch
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_show_choices
import com.patrickzedler.grocy.core.resources.ic_rounded_arrow_drop_down
import com.patrickzedler.grocy.core.resources.ic_rounded_check
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** A choice of a [SearchableDropdownField]; [searchTexts] are found besides the [label]. */
@Immutable
data class DropdownOption(
    val id: Int,
    val label: String,
    val supporting: String? = null,
    val searchTexts: List<String> = emptyList(),
)

/**
 * A text field that picks one of [options]: typing filters them and forgives small typos, the
 * dropdown arrow shows all of them. Tapping the field itself only focuses it for typing. Like
 * Grocy's pickers it accepts no free text; leaving the field shows the chosen option again.
 *
 * @param noneLabel makes the choice optional: shown as first option, and clearing the text
 *   chooses nothing.
 * @param trailingAction e.g. a scan button, placed before the dropdown arrow.
 */
@Composable
fun SearchableDropdownField(
    label: String,
    options: List<DropdownOption>,
    selectedId: Int?,
    onSelectedIdChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    noneLabel: String? = null,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    val selectedLabel = options.firstOrNull { it.id == selectedId }?.label.orEmpty()
    val textState = rememberTextFieldState(selectedLabel)
    var expanded by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    // True once the user typed, until a choice or leaving the field
    var isSearching by remember { mutableStateOf(false) }
    val currentSelectedLabel by rememberUpdatedState(selectedLabel)

    // Outside a search the field always shows the chosen option
    LaunchedEffect(selectedLabel, isFocused, isSearching) {
        if (!isSearching) textState.setTextAndPlaceCursorAtEnd(selectedLabel)
    }
    LaunchedEffect(textState) {
        snapshotFlow { textState.text.toString() }.collect { text ->
            if (isFocused && text != currentSelectedLabel) {
                isSearching = true
                expanded = true
            }
        }
    }

    val query = if (isSearching) textState.text.toString() else ""
    val matches = remember(options, query) {
        FuzzySearch.filter(options, query) { listOf(it.label) + it.searchTexts }
            .take(MAX_SHOWN_OPTIONS)
    }
    val showNone = noneLabel != null && !isSearching

    fun choose(id: Int?) {
        haptic.confirm()
        onSelectedIdChange(id)
        isSearching = false
        expanded = false
        textState.setTextAndPlaceCursorAtEnd(options.firstOrNull { it.id == id }?.label.orEmpty())
    }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        // Taps on the field (and its scan button) only close the menu; it opens through the
        // arrow or while typing, so the keyboard and the other field actions stay usable
        onExpandedChange = { if (!it) expanded = false },
        modifier = modifier,
    ) {
        RoundedTextField(
            state = textState,
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, enabled)
                .fillMaxWidth()
                .onFocusChanged { focus ->
                    if (isFocused && !focus.isFocused && isSearching) {
                        // Cleared optional fields choose nothing, other text is discarded
                        if (noneLabel != null && textState.text.isBlank()) onSelectedIdChange(null)
                        isSearching = false
                    }
                    isFocused = focus.isFocused
                },
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    trailingAction?.invoke()
                    // A button of its own, so the field keeps its label as accessible name
                    val description = stringResource(Res.string.action_show_choices)
                    IconButton(
                        onClick = {
                            haptic.confirm()
                            expanded = !expanded
                        },
                        enabled = enabled,
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_rounded_arrow_drop_down),
                            contentDescription = description,
                            modifier = Modifier.rotate(if (expanded) 180f else 0f),
                        )
                    }
                }
            },
            supportingText = supportingText?.let { { Text(it) } },
            isError = isError,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            onKeyboardAction = {
                // Enter takes the best match, like picking it from the list
                matches.firstOrNull()?.let { choose(it.id) }
            },
        )
        ExposedDropdownMenu(
            expanded = expanded && enabled && (matches.isNotEmpty() || showNone),
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = MenuMaxHeight),
            shape = MenuDefaults.standaloneGroupShape,
            containerColor = MenuDefaults.groupStandardContainerColor,
        ) {
            val count = matches.size + if (showNone) 1 else 0
            if (showNone && noneLabel != null) {
                OptionItem(
                    text = noneLabel,
                    supporting = null,
                    selected = selectedId == null,
                    index = 0,
                    count = count,
                    onClick = { choose(null) },
                )
            }
            val offset = if (showNone) 1 else 0
            matches.forEachIndexed { index, option ->
                OptionItem(
                    text = option.label,
                    supporting = option.supporting,
                    selected = option.id == selectedId,
                    index = index + offset,
                    count = count,
                    onClick = { choose(option.id) },
                )
            }
        }
    }
}

@Composable
private fun OptionItem(
    text: String,
    supporting: String?,
    selected: Boolean,
    index: Int,
    count: Int,
    onClick: () -> Unit,
) {
    SelectableDropdownMenuItem(
        selected = selected,
        onClick = onClick,
        text = { Text(text, style = MaterialTheme.typography.bodyLarge) },
        shapes = MenuDefaults.itemShape(index = index, count = count),
        supportingText = supporting?.let { { Text(it) } },
        selectedLeadingIcon = {
            Icon(
                painter = painterResource(Res.drawable.ic_rounded_check),
                contentDescription = null,
                modifier = Modifier.size(MenuDefaults.LeadingIconSize),
            )
        },
        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
    )
}

// A list of thousands of products only shows the best matches, typing narrows them down
private const val MAX_SHOWN_OPTIONS = 50

private val MenuMaxHeight = 320.dp
