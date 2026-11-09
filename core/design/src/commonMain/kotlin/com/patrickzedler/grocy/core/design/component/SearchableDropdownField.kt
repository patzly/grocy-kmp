package com.patrickzedler.grocy.core.design.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorPosition
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.patrickzedler.grocy.core.model.text.FuzzySearch
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_show_choices
import com.patrickzedler.grocy.core.resources.ic_rounded_arrow_drop_down
import com.patrickzedler.grocy.core.resources.msg_choose_from_list
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource
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
 * A combobox like Grocy's pickers: the text in the field is the value. Text that names an
 * option (ignoring case) chooses it, an empty field chooses nothing; other text chooses nothing
 * either, keeps standing and shows an error once no suggestion is open (unless
 * [submitsTextOnLeave]), and [onUnmatchedChange] reports it, so a form can refuse to save. The menu only helps typing: it suggests the options
 * that fit the text (forgiving small typos), the arrow shows all of them, and choosing one or
 * typing one completely closes it. Tapping the field itself only focuses it for typing; the
 * arrow opens the menu without focusing it, so choosing needs no keyboard. The keyboard's action
 * takes the best suggestion for typed text and goes on to the next field, the last field of a
 * form submits it instead.
 *
 * @param noneLabel makes the choice optional: offered as first suggestion, it clears the field.
 * @param trailingAction e.g. a scan button, placed before the dropdown arrow.
 * @param focusRequester focuses the field for typing, e.g. the first field of a new object.
 * @param autoFocus takes the focus once when the form opens.
 * @param isLastField the keyboard's action submits the form of [LocalFormFocus].
 * @param onSubmitText can use text submitted by the keyboard itself, e.g. as a code, with whether
 *   a suggestion containing that text would take it otherwise; true when it did.
 * @param submitsTextOnLeave text naming no option also goes to [onSubmitText] when the field loses
 *   the focus, like Grocy's product picker; it is no error then, the form asks what it means.
 * @param textResetKey a new value replaces typed text with the current choice's label.
 * @param onKeyboardNext replaces going on to the next field, e.g. for a field whose next field
 *   only takes the focus after a choice.
 * @param onFocusChange whether the field has the focus.
 */
@Composable
fun SearchableDropdownField(
    label: String,
    options: List<DropdownOption>,
    selectedId: Int?,
    onSelectedIdChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    onUnmatchedChange: (Boolean) -> Unit = {},
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    noneLabel: String? = null,
    trailingAction: (@Composable () -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    autoFocus: Boolean = false,
    isLastField: Boolean = false,
    onSubmitText: ((text: String, hasSuggestion: Boolean) -> Boolean)? = null,
    submitsTextOnLeave: Boolean = false,
    textResetKey: Any? = null,
    onKeyboardNext: (() -> Unit)? = null,
    onFocusChange: (Boolean) -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val inputModeManager = LocalInputModeManager.current
    val form = LocalFormFocus.current
    val selectedLabel = options.firstOrNull { it.id == selectedId }?.label.orEmpty()
    val textState = rememberTextFieldState(selectedLabel)
    val currentOptions by rememberUpdatedState(options)
    val currentOnSelectedIdChange by rememberUpdatedState(onSelectedIdChange)
    val currentOnUnmatchedChange by rememberUpdatedState(onUnmatchedChange)
    var expanded by remember { mutableStateOf(false) }
    var lastClose by remember { mutableStateOf<TimeMark?>(null) }
    fun close() {
        if (expanded) lastClose = TimeSource.Monotonic.markNow()
        expanded = false
    }
    var isFocused by remember { mutableStateOf(false) }
    // Text the field wrote itself, so it is not taken as typing
    var writtenText by remember { mutableStateOf(selectedLabel) }
    // True from typing until the text is replaced by a choice
    var isTyped by remember { mutableStateOf(false) }
    // The choice this field reported last; other changes come from outside (e.g. a scan)
    var reportedId by remember { mutableStateOf(selectedId) }
    val isUnmatched = isTyped && dropdownTextMatch(options, textState.text) == NoMatch
    // Text already handed to onSubmitText, so losing the focus to its answer does not ask again
    var submittedText by remember { mutableStateOf<String?>(null) }
    var resetKey by remember { mutableStateOf(textResetKey) }

    fun write(text: String) {
        writtenText = text
        isTyped = false
        textState.setTextAndPlaceCursorAtEnd(text)
    }

    // A choice from outside, or the options arriving, replace text the user did not type
    LaunchedEffect(selectedId, selectedLabel) {
        if (selectedId != reportedId || !isTyped) {
            reportedId = selectedId
            write(selectedLabel)
        }
    }
    LaunchedEffect(textResetKey) {
        if (textResetKey != resetKey) {
            resetKey = textResetKey
            write(selectedLabel)
        }
    }
    LaunchedEffect(isUnmatched) { currentOnUnmatchedChange(isUnmatched) }
    LaunchedEffect(textState) {
        snapshotFlow { textState.text.toString() }.collect { text ->
            if (text == writtenText) return@collect
            writtenText = text
            isTyped = true
            submittedText = null
            val match = dropdownTextMatch(currentOptions, text)
            // A fully typed option needs no suggestions anymore, typing on shows them again
            if (isFocused) expanded = match == null || match == NoMatch
            val id = if (match == NoMatch) null else match
            if (id != reportedId) {
                reportedId = id
                currentOnSelectedIdChange(id)
            }
        }
    }

    val query = if (isTyped) textState.text.toString() else ""
    val matches = remember(options, query) {
        FuzzySearch.filter(options, query) { listOf(it.label) + it.searchTexts }
            .take(MAX_SHOWN_OPTIONS)
    }
    val showNone = noneLabel != null && query.isBlank()

    fun choose(id: Int?) {
        haptic.confirm()
        close()
        reportedId = id
        onSelectedIdChange(id)
        write(options.firstOrNull { it.id == id }?.label.orEmpty())
    }

    fun submitText(text: String): Boolean {
        val used = onSubmitText?.invoke(text, hasLiteralSuggestion(matches, text)) == true
        if (used) submittedText = text
        return used
    }

    // What the keyboard's action makes of typed text; true when that gave the field its value
    fun useTypedText(): Boolean {
        if (!isTyped) return false
        val text = textState.text.toString().trim()
        val named = dropdownTextMatch(options, text)
        if (named != NoMatch && named != null) {
            close()
            write(selectedLabel)
            return true
        }
        if (text.isNotEmpty() && submitText(text)) {
            close()
            return true
        }
        val suggested = typedChoice(isTyped, text, matches) ?: return false
        choose(suggested)
        return true
    }

    fun goOn() {
        when {
            onKeyboardNext != null -> onKeyboardNext()
            isLastField && form != null -> form.submit()
            else -> focusManager.moveFocus(FocusDirection.Next)
        }
    }

    // A suggestion tapped while typing goes on to the next field, the keyboard stays open for it
    fun chooseFromMenu(id: Int?) {
        choose(id)
        if (!isFocused || inputModeManager.inputMode != InputMode.Touch) return
        if (isLastField) focusManager.clearFocus() else goOn()
    }

    val unmatchedText = stringResource(Res.string.msg_choose_from_list)
    val isMenuShown = expanded && enabled && (matches.isNotEmpty() || showNone)
    val showUnmatched = showsUnmatchedError(isUnmatched, isMenuShown, submitsTextOnLeave)
    // Material's ExposedDropdownMenuBox focuses the field whenever its menu opens, which opened
    // the keyboard for a tap on the arrow, and the menu jumped up once it was there
    var fieldWidth by remember { mutableIntStateOf(0) }
    Box(modifier = modifier) {
        RoundedTextField(
            state = textState,
            modifier = Modifier
                .fillMaxWidth()
                .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
                .onSizeChanged { fieldWidth = it.width }
                .semantics {
                    if (enabled) {
                        if (isMenuShown) {
                            collapse {
                                close()
                                true
                            }
                        } else {
                            expand {
                                expanded = true
                                true
                            }
                        }
                    }
                }
                .onFocusChanged { focus ->
                    if (isFocused && !focus.isFocused) {
                        close()
                        // A matching text shows the option's own spelling
                        if (isTyped && !isUnmatched) write(selectedLabel)
                        val text = textState.text.toString().trim()
                        if (submitsTextOnLeave && isUnmatched && text != submittedText) {
                            submitText(text)
                        }
                    } else if (!isFocused && focus.isFocused && submittedText != null) {
                        // Back from the question about it, typing replaces it like in Grocy
                        textState.edit { selection = TextRange(0, length) }
                    }
                    if (isFocused != focus.isFocused) onFocusChange(focus.isFocused)
                    isFocused = focus.isFocused
                },
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = {
                val arrow = @Composable {
                    DropdownArrow(
                        expanded = isMenuShown,
                        enabled = enabled,
                        onClick = {
                            haptic.confirm()
                            // A tap on the arrow of an open menu closes it on touch down already
                            // (outside the menu); that tap must not open it again
                            val justClosed = lastClose?.elapsedNow()?.let { it < ReopenDelay }
                            expanded = !expanded && justClosed != true
                        },
                    )
                }
                if (trailingAction != null) {
                    TextFieldActions {
                        trailingAction()
                        arrow()
                    }
                } else {
                    arrow()
                }
            },
            supportingText = (if (showUnmatched) unmatchedText else supportingText)
                ?.let { { Text(it) } },
            isError = isError || showUnmatched,
            // Names are chosen from the suggestions, autocorrect would only change what they find
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = false,
                imeAction = if (isLastField && form != null) ImeAction.Done else ImeAction.Next,
            ),
            onKeyboardAction = {
                // Enter takes the best suggestion, like picking it from the list
                useTypedText()
                goOn()
            },
            autoFocus = autoFocus,
            onEnterKey = {
                // Like Grocy's picker: a suggestion first, Enter again submits the form
                useTypedText() || onKeyboardNext?.let { next ->
                    next()
                    true
                } == true
            },
        )
        // An expressive menu group instead of ExposedDropdownMenu: that one wraps its items in
        // the padding of the old menus outside the scrolling, which cuts them off and breaks
        // the group's concentric corners. Not focusable, so the keyboard stays for typing; a tap
        // beside it (also on the field) closes it.
        DropdownMenuPopup(
            expanded = isMenuShown,
            onDismissRequest = { close() },
            modifier = Modifier.width(with(LocalDensity.current) { fieldWidth.toDp() }),
            popupPositionProvider = MenuDefaults.rememberDropdownMenuPopupPositionProvider(
                MenuAnchorPosition.Below,
            ),
            properties = PopupProperties(focusable = false),
        ) {
            val menuScrollState = rememberScrollState()
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(index = 0, count = 1),
                // Scrolls inside the group, so the group's padding scrolls with the items
                modifier = Modifier
                    .heightIn(max = MenuMaxHeight)
                    .verticalScrollbar(menuScrollState)
                    .verticalScroll(menuScrollState),
            ) {
                val count = matches.size + if (showNone) 1 else 0
                if (showNone) {
                    OptionItem(
                        text = noneLabel,
                        supporting = null,
                        index = 0,
                        count = count,
                        onClick = { chooseFromMenu(null) },
                    )
                }
                val offset = if (showNone) 1 else 0
                matches.forEachIndexed { index, option ->
                    OptionItem(
                        text = option.label,
                        supporting = option.supporting,
                        index = index + offset,
                        count = count,
                        onClick = { chooseFromMenu(option.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DropdownArrow(
    expanded: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    // A button of its own, so the field keeps its label as accessible name
    IconButton(
        onClick = onClick,
        modifier = fieldActionSize().clickCursor(enabled = enabled),
        enabled = enabled,
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_rounded_arrow_drop_down),
            contentDescription = stringResource(Res.string.action_show_choices),
            modifier = Modifier.rotate(if (expanded) 180f else 0f),
        )
    }
}

@Composable
private fun OptionItem(
    text: String,
    supporting: String?,
    index: Int,
    count: Int,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        onClick = onClick,
        modifier = Modifier.clickCursor(),
        text = { Text(text) },
        shape = menuItemShape(index = index, count = count),
        supportingText = supporting?.let { { Text(it) } },
    )
}

/**
 * What [text] chooses among [options]: the id of the option it names (ignoring case and
 * surrounding spaces), null for an empty text, or [NoMatch].
 */
internal fun dropdownTextMatch(options: List<DropdownOption>, text: CharSequence): Int? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return null
    return options.firstOrNull { it.label.trim().equals(trimmed.toString(), ignoreCase = true) }
        ?.id ?: NoMatch
}

/**
 * The option the keyboard's action chooses: the best of [matches] for [text] the user typed. Text
 * that is only shown, or nothing at all, chooses nothing, so going on through a form changes
 * nothing.
 */
internal fun typedChoice(
    isTyped: Boolean,
    text: CharSequence,
    matches: List<DropdownOption>,
): Int? = if (isTyped && text.isNotBlank()) matches.firstOrNull()?.id else null

/** Whether one of the [matches] contains [text], unlike those only found by forgiving a typo. */
internal fun hasLiteralSuggestion(matches: List<DropdownOption>, text: String): Boolean =
    matches.any { option ->
        (listOf(option.label) + option.searchTexts).any { it.contains(text, ignoreCase = true) }
    }

/**
 * Whether text naming no option is an error: as soon as no suggestion is left, because a tap beside
 * the field does not take its focus away. Not where [asksAboutText], there it is a valid input the
 * form asks about.
 */
internal fun showsUnmatchedError(
    isUnmatched: Boolean,
    isMenuShown: Boolean,
    asksAboutText: Boolean,
): Boolean = isUnmatched && !isMenuShown && !asksAboutText

/** Result of [dropdownTextMatch] for a text that names no option. */
internal const val NoMatch = Int.MIN_VALUE

// A list of thousands of products only shows the best matches, typing narrows them down
private const val MAX_SHOWN_OPTIONS = 50

private val MenuMaxHeight = 320.dp

// Longer than a tap, shorter than a deliberate second tap
private val ReopenDelay = 400.milliseconds
