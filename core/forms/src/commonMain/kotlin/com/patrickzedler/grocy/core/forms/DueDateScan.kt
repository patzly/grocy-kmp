package com.patrickzedler.grocy.core.forms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.component.HapticTooltipBox
import com.patrickzedler.grocy.core.design.component.TextDialog
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.fieldActionSize
import com.patrickzedler.grocy.core.model.text.PrintedDueDate
import com.patrickzedler.grocy.core.model.text.PrintedDueDateVotes
import com.patrickzedler.grocy.core.model.text.formatDate
import com.patrickzedler.grocy.core.model.text.printedDueDateOf
import com.patrickzedler.grocy.core.model.text.todayEpochDay
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_close_scanner
import com.patrickzedler.grocy.core.resources.action_scan_due_date
import com.patrickzedler.grocy.core.resources.due_date_scan_choose
import com.patrickzedler.grocy.core.resources.due_date_scan_hint
import com.patrickzedler.grocy.core.resources.ic_rounded_close
import com.patrickzedler.grocy.core.resources.ic_rounded_document_scanner
import com.patrickzedler.grocy.core.resources.product_due_days_scan
import com.patrickzedler.grocy.core.resources.product_due_days_scan_invalid
import com.patrickzedler.grocy.core.resources.product_due_days_scan_invalid_title
import com.patrickzedler.grocy.core.scanner.LocalTextRecognizerFactory
import com.patrickzedler.grocy.core.scanner.TextScanner
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Whether due date fields offer their scanner: the app provides a text engine and the user did
 * not turn scanning due dates off.
 */
@Composable
fun canScanDueDates(): Boolean = LocalTextRecognizerFactory.current != null

/**
 * The days a due days field gets for the scanned date [epochDay]: the days from [today] until it,
 * null for a date that is not after today, from which no days can be counted.
 */
internal fun dueDaysOfScanned(epochDay: Long, today: Long): String? =
    (epochDay - today).takeIf { it > 0 }?.toString()

/** The trailing button of a field with a date scanner: opens and closes it. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DateScanButton(
    isScannerOpen: Boolean,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val shownDescription = if (isScannerOpen) {
        stringResource(Res.string.action_close_scanner)
    } else {
        description
    }
    HapticTooltipBox(text = shownDescription) {
        IconButton(
            onClick = {
                haptic.confirm()
                // The camera takes the keyboard's place
                if (!isScannerOpen) focusManager.clearFocus()
                onClick()
            },
            modifier = fieldActionSize().clickCursor(enabled = enabled),
            enabled = enabled,
            shapes = IconButtonDefaults.shapes(),
        ) {
            Icon(
                painter = painterResource(
                    if (isScannerOpen) {
                        Res.drawable.ic_rounded_close
                    } else {
                        Res.drawable.ic_rounded_document_scanner
                    },
                ),
                contentDescription = shownDescription,
            )
        }
    }
}

/**
 * The camera below a due date field, reading the date printed on a package. A date counts once
 * frames agree on it ([PrintedDueDateVotes]), so a misread digit never fills the field; a date
 * whose day and month could be swapped asks which one is meant. [onDate] gets the date.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DueDateScanner(onDate: (epochDay: Long) -> Unit) {
    val order = LocalDateInput.current.order
    val haptic = LocalHapticFeedback.current
    val votes = remember { PrintedDueDateVotes() }
    var choice by rememberSaveable(stateSaver = PrintedDueDateSaver) {
        mutableStateOf<PrintedDueDate?>(null)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextScanner(
            onLines = { lines ->
                val reading = printedDueDateOf(lines, todayEpochDay(), order)
                votes.accept(reading)?.let { date ->
                    haptic.confirm()
                    if (date.alternative == null) onDate(date.epochDay) else choice = date
                }
            },
            shape = MaterialTheme.shapes.small,
            paused = choice != null,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
        val current = choice
        Text(
            text = stringResource(
                if (current == null) {
                    Res.string.due_date_scan_hint
                } else {
                    Res.string.due_date_scan_choose
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        val alternative = current?.alternative
        if (current != null && alternative != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(current.epochDay, alternative).forEach { epochDay ->
                    OutlinedButton(
                        onClick = {
                            haptic.confirm()
                            choice = null
                            votes.reset()
                            onDate(epochDay)
                        },
                        modifier = Modifier.weight(1f).clickCursor(),
                        shapes = ButtonDefaults.shapes(),
                    ) {
                        Text(formatDate(epochDay))
                    }
                }
            }
        }
    }
}

private val PrintedDueDateSaver = listSaver<PrintedDueDate?, Long>(
    save = { date -> listOfNotNull(date?.epochDay, date?.alternative) },
    restore = { values -> values.firstOrNull()?.let { PrintedDueDate(it, values.getOrNull(1)) } },
)

/**
 * The date scanner of a due days field: a scanned date gives the days from today until it, a
 * date not after today explains in a dialog why it gives none. Created by [rememberDueDaysScan].
 */
@Stable
class DueDaysScan internal constructor(private val state: TextFieldState) {

    var isOpen by mutableStateOf(false)
        internal set

    /** The scanned date that is not after today, while its dialog is shown. */
    internal var invalidDate by mutableStateOf<Long?>(null)

    internal fun onDate(epochDay: Long, today: Long) {
        isOpen = false
        val days = dueDaysOfScanned(epochDay, today)
        if (days == null) {
            invalidDate = epochDay
        } else {
            state.setTextAndPlaceCursorAtEnd(days)
        }
    }

    /** The camera below the field while it is open, for [TextItem]'s `below`. */
    val below: (@Composable () -> Unit)?
        get() = if (isOpen) {
            { DueDateScanner(onDate = { onDate(it, todayEpochDay()) }) }
        } else {
            null
        }
}

/** The date scanner of the due days field of [state], null where dates cannot be scanned. */
@Composable
fun rememberDueDaysScan(state: TextFieldState): DueDaysScan? {
    val scan = rememberSaveable(
        state,
        saver = listSaver<DueDaysScan, Any>(
            save = { listOfNotNull(it.isOpen, it.invalidDate) },
            restore = { values ->
                DueDaysScan(state).apply {
                    isOpen = values[0] as Boolean
                    invalidDate = values.getOrNull(1) as Long?
                }
            },
        ),
    ) { DueDaysScan(state) }
    return scan.takeIf { canScanDueDates() }
}

/**
 * The trailing button of a due days field for [scan], next to [DueDaysPickerButton], with the
 * dialog of a date that gives no days.
 */
@Composable
fun DueDaysScanButton(scan: DueDaysScan) {
    DateScanButton(
        isScannerOpen = scan.isOpen,
        description = stringResource(Res.string.product_due_days_scan),
        onClick = { scan.isOpen = !scan.isOpen },
    )
    scan.invalidDate?.let { epochDay ->
        TextDialog(
            title = stringResource(Res.string.product_due_days_scan_invalid_title),
            text = stringResource(Res.string.product_due_days_scan_invalid, formatDate(epochDay)),
            onDismissRequest = { scan.invalidDate = null },
        )
    }
}

/** The date scanner's button of a due date field. */
@Composable
internal fun DueDateScanButton(isScannerOpen: Boolean, onClick: () -> Unit, enabled: Boolean) {
    DateScanButton(
        isScannerOpen = isScannerOpen,
        description = stringResource(Res.string.action_scan_due_date),
        onClick = onClick,
        enabled = enabled,
    )
}
