package com.patrickzedler.grocy.feature.masterdata.impl.product

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.component.HapticTooltipBox
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.core.model.text.isoDateOf
import com.patrickzedler.grocy.core.model.text.timeOfDayOf
import com.patrickzedler.grocy.core.model.text.todayEpochDay
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_cancel
import com.patrickzedler.grocy.core.resources.action_ok
import com.patrickzedler.grocy.core.resources.date_picker_open
import com.patrickzedler.grocy.core.resources.product_due_days_pick
import com.patrickzedler.grocy.core.resources.ic_rounded_calendar_month
import com.patrickzedler.grocy.core.resources.ic_rounded_event
import com.patrickzedler.grocy.core.resources.ic_rounded_keyboard
import com.patrickzedler.grocy.core.resources.ic_rounded_schedule
import com.patrickzedler.grocy.core.resources.time_picker_dial
import com.patrickzedler.grocy.core.resources.time_picker_input
import com.patrickzedler.grocy.core.resources.time_picker_title
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

/**
 * The calendar button at the end of a date field: Material's date picker, for [withTime]
 * followed by its time picker. The choice is written into [state] in Grocy's format, which can
 * still be typed as well.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DatePickerButton(state: TextFieldState, withTime: Boolean) {
    val haptic = LocalHapticFeedback.current
    // Saveable, so an open picker survives rotating the device
    var showDate by rememberSaveable { mutableStateOf(false) }
    var showTime by rememberSaveable { mutableStateOf(false) }
    val description = stringResource(Res.string.date_picker_open)

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
                painter = painterResource(Res.drawable.ic_rounded_calendar_month),
                contentDescription = description,
            )
        }
    }

    if (showDate) {
        PickDateDialog(
            initialEpochDay = epochDayOf(state.text.toString().trim()) ?: todayEpochDay(),
            onPick = { epochDay ->
                showDate = false
                val text = state.text.toString()
                state.setTextAndPlaceCursorAtEnd(withPickedDate(text, epochDay, withTime))
                showTime = withTime
            },
            onDismissRequest = { showDate = false },
        )
    }
    if (showTime) {
        PickTimeDialog(
            text = state.text.toString(),
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

/**
 * The calendar button at the end of a due days field: picking a date from today on writes the
 * days until it, e.g. the due date printed on the first purchase of a product.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DueDaysPickerButton(state: TextFieldState) {
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
    ) {
        DatePicker(state = pickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PickTimeDialog(
    text: String,
    onPick: (hour: Int, minute: Int) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val (hour, minute) = timeOfDayOf(text) ?: (0 to 0)
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
