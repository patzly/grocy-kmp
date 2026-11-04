package com.patrickzedler.grocy.core.forms

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.input.setTextAndSelectAll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TimePickerLayoutType
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.component.HapticTooltipBox
import com.patrickzedler.grocy.core.design.component.LocalFormFocus
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.model.text.DateFieldOrder
import com.patrickzedler.grocy.core.model.text.MAX_TYPED_DATE_DIGITS
import com.patrickzedler.grocy.core.model.text.MAX_TYPED_DATE_TIME_DIGITS
import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.core.model.text.grocyDateOfTyped
import com.patrickzedler.grocy.core.model.text.isoDateOf
import com.patrickzedler.grocy.core.model.text.timeOfDayOf
import com.patrickzedler.grocy.core.model.text.todayEpochDay
import com.patrickzedler.grocy.core.model.text.typedDateOf
import com.patrickzedler.grocy.core.model.text.typedDatePattern
import com.patrickzedler.grocy.core.model.text.typedDateSeparators
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_cancel
import com.patrickzedler.grocy.core.resources.action_clear
import com.patrickzedler.grocy.core.resources.action_ok
import com.patrickzedler.grocy.core.resources.date_typed_invalid
import com.patrickzedler.grocy.core.resources.ic_rounded_calendar_month
import com.patrickzedler.grocy.core.resources.ic_rounded_event
import com.patrickzedler.grocy.core.resources.ic_rounded_keyboard
import com.patrickzedler.grocy.core.resources.ic_rounded_schedule
import com.patrickzedler.grocy.core.resources.product_due_days_pick
import com.patrickzedler.grocy.core.resources.time_picker_dial
import com.patrickzedler.grocy.core.resources.time_picker_input
import com.patrickzedler.grocy.core.resources.time_picker_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val MILLIS_PER_DAY = 86_400_000L

/**
 * [text] with the date [epochDay] chosen in the date picker, in Grocy's format: "YYYY-MM-DD", for
 * [withTime] followed by the time [text] had (midnight without one).
 */
internal fun withPickedDate(text: String, epochDay: Long, withTime: Boolean): String {
    val date = isoDateOf(epochDay)
    if (!withTime) return date
    val (hour, minute) = timeOfDayOf(text) ?: (0 to 0)
    return "$date ${timeText(hour, minute)}"
}

/**
 * [text] with the time chosen in the time picker, "YYYY-MM-DD HH:MM:00"; the date stays, [today]
 * where [text] has none.
 */
internal fun withPickedTime(text: String, hour: Int, minute: Int, today: Long): String {
    val day = epochDayOf(text.trim()) ?: today
    return "${isoDateOf(day)} ${timeText(hour, minute)}"
}

/**
 * The days from [today] until the date [epochDay] picked for a due days field, so the first
 * purchase's due date can be picked instead of counted.
 */
internal fun dueDaysUntil(epochDay: Long, today: Long): String =
    (epochDay - today).coerceAtLeast(0).toString()

/**
 * The date the picker of a due days field starts on: [text] days after [today], today for no
 * or negative days (-1 means never overdue), at most the picker's last year.
 */
internal fun dueDateOf(text: String, today: Long): Long {
    val days = text.trim().toLongOrNull()?.coerceAtLeast(0) ?: 0
    return (today + days.coerceAtMost(LAST_PICKER_DAY - today)).coerceAtLeast(today)
}

// 2100-12-31, the end of Material's date picker
private const val LAST_PICKER_DAY = 47_846L

private fun timeText(hour: Int, minute: Int) =
    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}:00"

/** Material's date picker with a way to clear the date, for [withTime] then its time picker. */
@Composable
private fun DatePickerDialogs(
    state: TextFieldState,
    withTime: Boolean,
    showDate: Boolean,
    onShowDateChange: (Boolean) -> Unit,
) {
    var showTime by rememberSaveable { mutableStateOf(false) }
    if (showDate) {
        PickDateDialog(
            initialEpochDay = epochDayOf(state.text.toString().trim()) ?: todayEpochDay(),
            onPick = { epochDay ->
                onShowDateChange(false)
                val text = state.text.toString()
                state.setTextAndPlaceCursorAtEnd(withPickedDate(text, epochDay, withTime))
                showTime = withTime
            },
            onClear = if (state.text.isEmpty()) {
                null
            } else {
                {
                    onShowDateChange(false)
                    state.clearText()
                }
            },
            onDismissRequest = { onShowDateChange(false) },
        )
    }
    if (showTime) {
        PickTimeDialog(
            initial = timeOfDayOf(state.text.toString()),
            onPick = { hour, minute ->
                showTime = false
                val text = state.text.toString()
                state.setTextAndPlaceCursorAtEnd(
                    withPickedTime(text, hour, minute, todayEpochDay()),
                )
            },
            onDismissRequest = { showTime = false },
        )
    }
}

/** Hour and minute of a time of day "HH:MM" like a time input of Grocy; null for others. */
fun clockTimeOf(text: String): Pair<Int, Int>? {
    val match = CLOCK_TIME.matchEntire(text.trim()) ?: return null
    val hour = match.groupValues[1].toInt()
    val minute = match.groupValues[2].toInt()
    return (hour to minute).takeIf { hour in 0..23 && minute in 0..59 }
}

/** [hour] and [minute] as Grocy's time inputs send them, "HH:MM". */
fun clockTimeText(hour: Int, minute: Int): String =
    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

private val CLOCK_TIME = Regex("""(\d{1,2}):(\d{2})(:\d{2})?""")

/**
 * The clock button at the end of a time field like Grocy's time inputs: Material's time picker
 * writes the choice into [state] as "HH:MM", which can still be typed as well.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TimePickerButton(state: TextFieldState) {
    val haptic = LocalHapticFeedback.current
    var showTime by rememberSaveable { mutableStateOf(false) }
    val description = stringResource(Res.string.time_picker_title)

    HapticTooltipBox(text = description) {
        IconButton(
            onClick = {
                haptic.confirm()
                showTime = true
            },
            modifier = Modifier.clickCursor(),
            shapes = IconButtonDefaults.shapes(),
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_rounded_schedule),
                contentDescription = description,
            )
        }
    }

    if (showTime) {
        PickTimeDialog(
            initial = clockTimeOf(state.text.toString()),
            onPick = { hour, minute ->
                showTime = false
                state.setTextAndPlaceCursorAtEnd(clockTimeText(hour, minute))
            },
            onDismissRequest = { showTime = false },
        )
    }
}

/**
 * The calendar button at the end of a due days field: picking a date from today on writes the
 * days until it, e.g. the due date printed on the first purchase of a product.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DueDaysPickerButton(state: TextFieldState) {
    val haptic = LocalHapticFeedback.current
    var showDate by rememberSaveable { mutableStateOf(false) }
    val description = stringResource(Res.string.product_due_days_pick)

    HapticTooltipBox(text = description) {
        IconButton(
            onClick = {
                haptic.confirm()
                showDate = true
            },
            modifier = Modifier.clickCursor(),
            shapes = IconButtonDefaults.shapes(),
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_rounded_event),
                contentDescription = description,
            )
        }
    }

    if (showDate) {
        val today = todayEpochDay()
        PickDateDialog(
            initialEpochDay = dueDateOf(state.text.toString(), today),
            firstEpochDay = today,
            onPick = { epochDay ->
                showDate = false
                state.setTextAndPlaceCursorAtEnd(dueDaysUntil(epochDay, today))
            },
            onDismissRequest = { showDate = false },
        )
    }
}

/** Only [firstEpochDay] and the days after it can be picked. */
@OptIn(ExperimentalMaterial3Api::class)
private class FromDay(private val firstEpochDay: Long) : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        utcTimeMillis.floorDiv(MILLIS_PER_DAY) >= firstEpochDay

    override fun isSelectableYear(year: Int): Boolean =
        year >= isoDateOf(firstEpochDay).substringBefore('-').toInt()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PickDateDialog(
    initialEpochDay: Long,
    onPick: (epochDay: Long) -> Unit,
    onDismissRequest: () -> Unit,
    firstEpochDay: Long? = null,
    onClear: (() -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    // The picker counts in UTC milliseconds
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialEpochDay * MILLIS_PER_DAY,
        selectableDates = remember(firstEpochDay) {
            if (firstEpochDay == null) {
                DatePickerDefaults.AllDates
            } else {
                FromDay(firstEpochDay)
            }
        },
    )

    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    haptic.confirm()
                    pickerState.selectedDateMillis?.let { onPick(it.floorDiv(MILLIS_PER_DAY)) }
                },
                enabled = pickerState.selectedDateMillis != null,
                modifier = Modifier.clickCursor(),
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(stringResource(Res.string.action_ok))
            }
        },
        dismissButton = {
            Row {
                if (onClear != null) {
                    TextButton(
                        onClick = {
                            haptic.confirm()
                            onClear()
                        },
                        modifier = Modifier.clickCursor(),
                        shapes = ButtonDefaults.shapes(),
                    ) {
                        Text(stringResource(Res.string.action_clear))
                    }
                }
                TextButton(
                    onClick = {
                        haptic.confirm()
                        onDismissRequest()
                    },
                    modifier = Modifier.clickCursor(),
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(Res.string.action_cancel))
                }
            }
        },
    ) {
        DatePicker(state = pickerState)
    }
}

/** Material's time picker dialog, opening at [initial] (hour and minute). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PickTimeDialog(
    initial: Pair<Int, Int>?,
    onPick: (hour: Int, minute: Int) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val (hour, minute) = initial ?: (0 to 0)
    val pickerState = rememberTimePickerState(initialHour = hour, initialMinute = minute)
    // The dial or, e.g. for keyboards, two text fields
    var showInput by rememberSaveable { mutableStateOf(false) }
    val toggleDescription = stringResource(
        if (showInput) Res.string.time_picker_dial else Res.string.time_picker_input,
    )

    val layoutType = TimePickerDefaults.layoutType()

    TimePickerDialog(
        onDismissRequest = onDismissRequest,
        // Material's dialog fills the window's height beside its dial, e.g. in desktop browsers;
        // the specified landscape dialog is at most 384.dp high
        modifier = if (layoutType == TimePickerLayoutType.Horizontal) {
            Modifier.heightIn(max = LandscapeTimeDialogMaxHeight)
        } else {
            Modifier
        },
        title = {
            Text(
                text = stringResource(Res.string.time_picker_title),
                style = MaterialTheme.typography.labelMedium,
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    haptic.confirm()
                    onPick(pickerState.hour, pickerState.minute)
                },
                modifier = Modifier.clickCursor(),
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(stringResource(Res.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    haptic.confirm()
                    onDismissRequest()
                },
                modifier = Modifier.clickCursor(),
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
        modeToggleButton = {
            HapticTooltipBox(text = toggleDescription) {
                IconButton(
                    onClick = {
                        haptic.confirm()
                        showInput = !showInput
                    },
                    modifier = Modifier.clickCursor(),
                    shapes = IconButtonDefaults.shapes(),
                ) {
                    Icon(
                        painter = painterResource(
                            if (showInput) {
                                Res.drawable.ic_rounded_schedule
                            } else {
                                Res.drawable.ic_rounded_keyboard
                            },
                        ),
                        contentDescription = toggleDescription,
                    )
                }
            }
        },
    ) {
        if (showInput) {
            TimeInput(state = pickerState)
        } else {
            TimePicker(state = pickerState, layoutType = layoutType)
        }
    }
}

private val LandscapeTimeDialogMaxHeight = 384.dp

/**
 * A date as segmented item [index] of [count], in [state] always in Grocy's format as far as it
 * is complete. By default the field opens Material's date picker (for [withTime] followed by its
 * time picker) with a way to clear it; with the setting "Enter dates with keyboard" digits are
 * typed in the order of [LocalDateInput].
 */
@Composable
fun DateTextItem(
    state: TextFieldState,
    label: String,
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    withTime: Boolean = false,
    hint: String? = null,
    error: String? = null,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    autoFocus: Boolean = false,
    isLastField: Boolean = false,
    onFocusChange: (Boolean) -> Unit = {},
) {
    val input = LocalDateInput.current
    if (input.keyboard) {
        TypedDateItem(
            state = state,
            order = input.order,
            label = label,
            index = index,
            count = count,
            modifier = modifier,
            withTime = withTime,
            hint = hint,
            error = error,
            enabled = enabled,
            focusRequester = focusRequester,
            autoFocus = autoFocus,
            isLastField = isLastField,
            onFocusChange = onFocusChange,
        )
    } else {
        PickedDateItem(
            state = state,
            label = label,
            index = index,
            count = count,
            modifier = modifier,
            withTime = withTime,
            hint = hint,
            error = error,
            enabled = enabled,
        )
    }
}

@Composable
private fun PickedDateItem(
    state: TextFieldState,
    label: String,
    index: Int,
    count: Int,
    modifier: Modifier,
    withTime: Boolean,
    hint: String?,
    error: String?,
    enabled: Boolean,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    // A refused submit asks for this date, before the field's own effect looks for the focus
    val form = LocalFormFocus.current
    if (form != null && error != null && form.isSeekingError) {
        LaunchedEffect(Unit) {
            if (form.claimError()) showPicker = true
        }
    }
    TextItem(
        state = state,
        label = label,
        index = index,
        count = count,
        modifier = modifier,
        hint = hint,
        error = error,
        enabled = enabled,
        trailingAction = {
            Icon(
                painter = painterResource(Res.drawable.ic_rounded_calendar_month),
                contentDescription = null,
            )
        },
        onFieldClick = { showPicker = true },
    )
    DatePickerDialogs(
        state = state,
        withTime = withTime,
        showDate = showPicker,
        onShowDateChange = { showPicker = it },
    )
}

@Composable
private fun TypedDateItem(
    state: TextFieldState,
    order: DateFieldOrder,
    label: String,
    index: Int,
    count: Int,
    modifier: Modifier,
    withTime: Boolean,
    hint: String?,
    error: String?,
    enabled: Boolean,
    focusRequester: FocusRequester?,
    autoFocus: Boolean,
    isLastField: Boolean,
    onFocusChange: (Boolean) -> Unit,
) {
    val digits = rememberSaveable(saver = TextFieldState.Saver) {
        TextFieldState(typedDateOf(state.text.toString(), order, withTime))
    }
    // The form keeps Grocy's format, whichever side changed last
    LaunchedEffect(state, order, withTime) {
        launch {
            snapshotFlow { digits.text.toString() }.collect { typed ->
                val value = grocyDateOfTyped(typed, order, withTime) ?: typed
                if (state.text.toString() != value) state.setTextAndPlaceCursorAtEnd(value)
            }
        }
        snapshotFlow { state.text.toString() }.collect { value ->
            val typed = digits.text.toString()
            if ((grocyDateOfTyped(typed, order, withTime) ?: typed) != value) {
                digits.setTextAndSelectAll(typedDateOf(value, order, withTime))
            }
        }
    }
    val typed = digits.text.toString()
    TextItem(
        state = digits,
        label = label,
        index = index,
        count = count,
        modifier = modifier,
        hint = hint,
        // Grocy's format in the message would not be what is typed here
        error = if (error != null && typed.isNotEmpty() &&
            grocyDateOfTyped(typed, order, withTime) == null
        ) {
            stringResource(Res.string.date_typed_invalid, typedDatePattern(order, withTime))
        } else {
            error
        },
        enabled = enabled,
        keyboardType = KeyboardType.Number,
        focusRequester = focusRequester,
        autoFocus = autoFocus,
        isLastField = isLastField,
        autoCorrect = false,
        inputTransformation = remember(withTime) { TypedDateInput(withTime) },
        outputTransformation = remember(order, withTime) { TypedDateOutput(order, withTime) },
        onFocusChange = onFocusChange,
    )
}

/** Digits only, as many as the longest date of the field has. */
private class TypedDateInput(private val withTime: Boolean) : InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        val filtered = asCharSequence().filter { it.isDigit() }
            .take(if (withTime) MAX_TYPED_DATE_TIME_DIGITS else MAX_TYPED_DATE_DIGITS)
            .toString()
        if (filtered != asCharSequence().toString()) replace(0, length, filtered)
    }
}

/** The separators of what is typed, which is not part of the text. */
private class TypedDateOutput(
    private val order: DateFieldOrder,
    private val withTime: Boolean,
) : OutputTransformation {
    override fun TextFieldBuffer.transformOutput() {
        // From the end, so the positions stay right
        typedDateSeparators(length, order, withTime).asReversed().forEach { (position, separator) ->
            insert(position, separator)
        }
    }
}

/** How date fields take their input, from the behavior settings. */
@Immutable
data class DateInputSettings(
    /** Digits instead of the date picker. */
    val keyboard: Boolean = false,
    val order: DateFieldOrder = DateFieldOrder.DAY_MONTH_YEAR,
)

/** Provided by the app from the behavior settings. */
val LocalDateInput = compositionLocalOf { DateInputSettings() }
