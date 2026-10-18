package com.patrickzedler.grocy.core.design.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorPosition
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
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.model.text.FuzzySearch
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_show_choices
import com.patrickzedler.grocy.core.resources.ic_rounded_arrow_drop_down
import com.patrickzedler.grocy.core.resources.ic_rounded_check
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

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
    var lastClose by remember { mutableStateOf<TimeMark?>(null) }
    fun close() {
        if (expanded) lastClose = TimeSource.Monotonic.markNow()
        expanded = false
    }
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
        onExpandedChange = { if (!it) close() },
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
                            // A tap on the arrow of an open menu closes it on touch down already
                            // (outside the menu); that tap must not open it again
                            val justClosed = lastClose?.elapsedNow()?.let { it < ReopenDelay }
                            expanded = !expanded && justClosed != true
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
        // An expressive menu group instead of ExposedDropdownMenu: that one wraps its items in
        // the padding of the old menus outside the scrolling, which cuts them off and breaks
        // the group's concentric corners. Not focusable, so the keyboard stays for typing.
        DropdownMenuPopup(
            expanded = expanded && enabled && (matches.isNotEmpty() || showNone),
            onDismissRequest = { close() },
            modifier = Modifier.exposedDropdownSize(),
            popupPositionProvider = MenuDefaults.rememberDropdownMenuPopupPositionProvider(
                MenuAnchorPosition.Below,
            ),
            properties = PopupProperties(focusable = false),
        ) {
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(index = 0, count = 1),
                // Scrolls inside the group, so the group's padding scrolls with the items
                modifier = Modifier
                    .heightIn(max = MenuMaxHeight)
                    .verticalScroll(rememberScrollState()),
            ) {
                MenuOptions(
                    matches = matches,
                    selectedId = selectedId,
                    noneLabel = noneLabel.takeIf { showNone },
                    onChoose = ::choose,
                )
            }
        }
    }
}

@Composable
private fun MenuOptions(
    matches: List<DropdownOption>,
    selectedId: Int?,
    noneLabel: String?,
    onChoose: (Int?) -> Unit,
) {
    val showNone = noneLabel != null
    val count = matches.size + if (showNone) 1 else 0
    if (noneLabel != null) {
        OptionItem(
            text = noneLabel,
            supporting = null,
            selected = selectedId == null,
            index = 0,
            count = count,
            onClick = { onChoose(null) },
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
            onClick = { onChoose(option.id) },
        )
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
        text = { Text(text) },
        shapes = MenuDefaults.itemShape(index = index, count = count),
        supportingText = supporting?.let { { Text(it) } },
        selectedLeadingIcon = {
            Icon(
                painter = painterResource(Res.drawable.ic_rounded_check),
                contentDescription = null,
                modifier = Modifier.size(MenuDefaults.LeadingIconSize),
            )
        },
    )
}

// A list of thousands of products only shows the best matches, typing narrows them down
private const val MAX_SHOWN_OPTIONS = 50

private val MenuMaxHeight = 320.dp

// Longer than a tap, shorter than a deliberate second tap
private val ReopenDelay = 400.milliseconds
