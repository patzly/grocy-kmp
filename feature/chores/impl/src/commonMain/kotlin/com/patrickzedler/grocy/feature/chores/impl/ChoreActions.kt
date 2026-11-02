package com.patrickzedler.grocy.feature.chores.impl

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.patrickzedler.grocy.core.data.chores.ChoresRepository
import com.patrickzedler.grocy.core.forms.ChangeError
import com.patrickzedler.grocy.core.forms.FieldError
import com.patrickzedler.grocy.core.forms.changeErrorOf
import com.patrickzedler.grocy.core.model.chores.CurrentChore
import com.patrickzedler.grocy.core.model.chores.choreTrackedTime
import com.patrickzedler.grocy.core.model.text.grocyDateTimeOf
import com.patrickzedler.grocy.core.model.text.localNowMillis
import com.patrickzedler.grocy.core.model.text.normalizedDate
import com.patrickzedler.grocy.core.model.text.normalizedDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** What an action on a chore did, for Grocy's messages. */
internal sealed interface ChoreEvent {
    /** [executionId] undoes it. */
    data class Tracked(
        val choreId: Int,
        val choreName: String,
        val trackedTime: String,
        val trackDateOnly: Boolean,
        val executionId: Int,
    ) : ChoreEvent

    data object Undone : ChoreEvent

    data object LabelPrinted : ChoreEvent

    data class LabelFailed(val error: ChangeError) : ChoreEvent

    data class Failed(val error: ChangeError) : ChoreEvent
}

/** Grocy's dialog "Reschedule next execution" of [chore]. */
@Stable
internal class RescheduleForm(val chore: CurrentChore, now: String) {
    private val prefill = chore.nextEstimatedExecutionTime ?: now

    val date = TextFieldState(if (chore.trackDateOnly) prefill.take(DATE_LENGTH) else prefill)
    var assignedUserId by mutableStateOf(chore.assignedUserId)
    var isUserUnmatched by mutableStateOf(false)
    var dateError by mutableStateOf<FieldError?>(null)
    var isSaving by mutableStateOf(false)
    var error by mutableStateOf<ChangeError?>(null)

    /** "Reset" has something to take back only while a date or user was set here before. */
    val canReset: Boolean
        get() = chore.isRescheduled || chore.isReassigned

    /** The date as Grocy stores it, null with [dateError] set when it is none. */
    fun validDate(): String? {
        val text = date.text.toString().trim()
        val valid = if (chore.trackDateOnly) normalizedDate(text) else normalizedDateTime(text)
        dateError = when {
            text.isEmpty() -> FieldError.Required
            valid != null -> null
            chore.trackDateOnly -> FieldError.InvalidDate
            else -> FieldError.InvalidDateTime
        }
        return valid
    }

    private companion object {
        const val DATE_LENGTH = 10
    }
}

/**
 * Grocy's actions on a chore of its overview and card: its buttons track the next schedule or
 * now and skip like Grocy (the logged in user, no question), each with "Undo" in its message;
 * rescheduling goes through its dialog ([reschedule]). All of them are online only.
 */
@Stable
internal class ChoreActions(
    private val scope: CoroutineScope,
    private val repository: ChoresRepository,
    private val now: () -> String = { grocyDateTimeOf(localNowMillis()) },
) {
    private val eventChannel = Channel<ChoreEvent>(Channel.BUFFERED)
    val events: Flow<ChoreEvent> = eventChannel.receiveAsFlow()

    /** Chores with a booking under way; a second tap on the same chore waits for it. */
    private val tracking = mutableSetOf<Int>()

    var reschedule by mutableStateOf<RescheduleForm?>(null)
        private set

    /** Grocy's "Track next chore schedule" ([atNextSchedule]) or "Track chore execution now". */
    fun track(chore: CurrentChore, atNextSchedule: Boolean) =
        book(chore, atNextSchedule = atNextSchedule, skipped = false)

    /** Grocy's "Skip next chore schedule". */
    fun skip(chore: CurrentChore) = book(chore, atNextSchedule = true, skipped = true)

    private fun book(chore: CurrentChore, atNextSchedule: Boolean, skipped: Boolean) {
        if (!tracking.add(chore.choreId)) return
        val trackedTime = choreTrackedTime(
            next = chore.nextEstimatedExecutionTime,
            trackDateOnly = chore.trackDateOnly,
            now = now(),
            atNextSchedule = atNextSchedule,
        )
        scope.launch {
            var event: ChoreEvent? = null
            val error = changeErrorOf {
                val id = repository.track(chore.choreId, trackedTime, skipped = skipped)
                event = ChoreEvent.Tracked(
                    chore.choreId,
                    chore.name,
                    trackedTime,
                    chore.trackDateOnly,
                    id,
                )
            }
            tracking -= chore.choreId
            eventChannel.send(event ?: ChoreEvent.Failed(requireNotNull(error)))
        }
    }

    /** Grocy's "Undo" of a message. */
    fun undo(event: ChoreEvent.Tracked) {
        scope.launch {
            val error = changeErrorOf { repository.undo(event.executionId, event.choreId) }
            eventChannel.send(error?.let(ChoreEvent::Failed) ?: ChoreEvent.Undone)
        }
    }

    fun printLabel(choreId: Int) {
        scope.launch {
            val error = changeErrorOf { repository.printLabel(choreId) }
            eventChannel.send(error?.let(ChoreEvent::LabelFailed) ?: ChoreEvent.LabelPrinted)
        }
    }

    fun openReschedule(chore: CurrentChore) {
        reschedule = RescheduleForm(chore, now())
    }

    fun dismissReschedule() {
        if (reschedule?.isSaving != true) reschedule = null
    }

    /** Grocy's "OK": the date and user of the next execution. */
    fun saveReschedule() {
        val form = reschedule ?: return
        val date = form.validDate() ?: return
        if (form.isUserUnmatched) return
        saveReschedule(form, date, form.assignedUserId)
    }

    /** Grocy's "Reset": the next execution as the chore's schedule has it again. */
    fun resetReschedule() {
        saveReschedule(reschedule ?: return, null, null)
    }

    private fun saveReschedule(form: RescheduleForm, date: String?, userId: Int?) {
        if (form.isSaving) return
        form.isSaving = true
        form.error = null
        scope.launch {
            val error = changeErrorOf { repository.reschedule(form.chore.choreId, date, userId) }
            form.isSaving = false
            form.error = error
            if (error == null) reschedule = null
        }
    }
}
