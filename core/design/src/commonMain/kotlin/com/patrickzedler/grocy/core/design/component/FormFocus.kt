package com.patrickzedler.grocy.core.design.component

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalWindowInfo
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The form around its text fields, which find it in [LocalFormFocus]: "Done" and Enter submit it,
 * and a submit that finds invalid input takes the focus to the first invalid field.
 *
 * @param onSubmit saves the form; false when invalid input kept it from being sent.
 */
@Stable
class FormFocus(private val onSubmit: () -> Boolean) {

    /** True from a refused submit until a field with an error took the focus. */
    var isSeekingError by mutableStateOf(false)
        private set

    fun submit() {
        isSeekingError = !onSubmit()
    }

    /** Whether the asking field takes the focus; only the first one with an error gets it. */
    fun claimError(): Boolean {
        val claimed = isSeekingError
        isSeekingError = false
        return claimed
    }

    /** No field of the form has an error to focus. */
    fun stopSeekingError() {
        isSeekingError = false
    }
}

/** The form of the text fields below it, null outside of forms. */
val LocalFormFocus = staticCompositionLocalOf<FormFocus?> { null }

/**
 * A [FormFocus] for [onSubmit]. With [listState] it looks for an invalid field that lies outside
 * of the composed part of a long form: from the top, a page at a time.
 */
@Composable
fun rememberFormFocus(listState: LazyListState? = null, onSubmit: () -> Boolean): FormFocus {
    val currentOnSubmit by rememberUpdatedState(onSubmit)
    val form = remember { FormFocus { currentOnSubmit() } }
    LaunchedEffect(form, listState) {
        snapshotFlow { form.isSeekingError }.collect { isSeeking ->
            if (!isSeeking) return@collect
            withFrameNanos {}
            if (listState != null && form.isSeekingError) {
                listState.scrollToItem(0)
                withFrameNanos {}
                while (form.isSeekingError && listState.canScrollForward) {
                    val page = listState.layoutInfo.viewportSize.height * SEEK_PAGE
                    listState.animateScrollBy(page)
                    withFrameNanos {}
                }
            }
            form.stopSeekingError()
        }
    }
    return form
}

/**
 * Focuses [focusRequester] while [isRequested], once its window has the focus: the keyboard does
 * not open for a window that is still appearing, like a dialog. [onFocused] ends the request.
 */
@Composable
fun FocusRequestEffect(
    isRequested: Boolean,
    focusRequester: FocusRequester,
    onFocused: () -> Unit = {},
) {
    if (!isRequested) return
    val windowInfo = LocalWindowInfo.current
    LaunchedEffect(Unit) {
        withTimeoutOrNull(WindowFocusTimeout) {
            snapshotFlow { windowInfo.isWindowFocused }.first { it }
        }
        withFrameNanos {}
        focusRequester.requestFocus()
        onFocused()
    }
}

/** [FocusRequestEffect] for a field of a just opened form: once, not when it scrolls back. */
@Composable
fun AutoFocusEffect(enabled: Boolean, focusRequester: FocusRequester) {
    var isDone by rememberSaveable { mutableStateOf(false) }
    FocusRequestEffect(enabled && !isDone, focusRequester) { isDone = true }
}

/**
 * A key on a hardware keyboard or in the browser, like Grocy's forms: Enter submits, also where
 * the soft keyboard's action only goes on, unless [onEnter] uses it; a multi-line field keeps it
 * for its line breaks, Ctrl or Cmd with Enter submits from there too.
 */
internal fun Modifier.submitOnEnter(
    form: FormFocus?,
    isMultiLine: Boolean,
    onEnter: (() -> Boolean)?,
): Modifier {
    if (form == null) return this
    return onPreviewKeyEvent { event ->
        if (event.key != Key.Enter && event.key != Key.NumPadEnter) return@onPreviewKeyEvent false
        val withModifier = event.isCtrlPressed || event.isMetaPressed
        if (isMultiLine && !withModifier) return@onPreviewKeyEvent false
        if (event.type == KeyEventType.KeyDown && (withModifier || onEnter?.invoke() != true)) {
            form.submit()
        }
        true
    }
}

private const val SEEK_PAGE = 0.75f

private val WindowFocusTimeout = 500.milliseconds
