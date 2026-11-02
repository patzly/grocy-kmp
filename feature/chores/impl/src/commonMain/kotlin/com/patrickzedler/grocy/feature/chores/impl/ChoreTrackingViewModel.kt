package com.patrickzedler.grocy.feature.chores.impl

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.chores.ChoresRepository
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.forms.FieldError
import com.patrickzedler.grocy.core.forms.FormFocusTarget
import com.patrickzedler.grocy.core.forms.ScannerFieldState
import com.patrickzedler.grocy.core.forms.UserfieldError
import com.patrickzedler.grocy.core.forms.UserfieldInput
import com.patrickzedler.grocy.core.forms.changeErrorOf
import com.patrickzedler.grocy.core.forms.changedValues
import com.patrickzedler.grocy.core.forms.ignoringServerErrors
import com.patrickzedler.grocy.core.forms.userfieldInputs
import com.patrickzedler.grocy.core.forms.validationErrors
import com.patrickzedler.grocy.core.model.User
import com.patrickzedler.grocy.core.model.masterdata.Chore
import com.patrickzedler.grocy.core.model.masterdata.ChorePeriodType
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.text.grocyDateTimeOf
import com.patrickzedler.grocy.core.model.text.localNowMillis
import com.patrickzedler.grocy.core.model.text.normalizedDate
import com.patrickzedler.grocy.core.model.text.normalizedDateTime
import com.patrickzedler.grocy.core.scanner.writer.Grocycode
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The inputs of Grocy's chore tracking that can be invalid. */
internal enum class ChoreTrackingField { CHORE, TRACKED_TIME }

/** The choices of Grocy's chore tracking. */
internal data class ChoreTrackingOptions(
    /** The active chores by name. */
    val chores: List<Chore>,
    /** "Done by", empty without the assignments feature or the permission to read users. */
    val users: List<User>,
    val config: ServerConfig,
)

/** What Grocy's chore tracking tells besides its messages for a tracked chore. */
internal sealed interface ChoreTrackingEvent {
    data class Result(val event: ChoreEvent) : ChoreTrackingEvent

    /** A scanned code names no active chore. */
    data object UnknownCode : ChoreTrackingEvent
}

/**
 * Grocy's "Chore tracking": the chore (typed, chosen or scanned as grocycode), the tracked time,
 * who did it and the execution's userfields, tracked or skipped. Like Grocy's page the form starts
 * over once tracked and the chore's overview stays available. Tracking is online only.
 *
 * @param scanner the scanner of the chore's field and where the focus goes around it; [hasValue]
 *   a preset chore needs neither.
 */
internal class ChoreTrackingViewModel(
    presetChoreId: Int?,
    private val masterData: MasterDataRepository,
    private val chores: ChoresRepository,
    scanner: (hasValue: Boolean) -> ScannerFieldState = { ScannerFieldState(hasValue = it) },
    private val now: () -> String = { grocyDateTimeOf(localNowMillis()) },
) : ViewModel() {

    val scanner: ScannerFieldState = scanner(presetChoreId != null)

    var choreId by mutableStateOf<Int?>(null)
        private set

    var isChoreUnmatched by mutableStateOf(false)

    val trackedTime = TextFieldState(now())

    var doneByUserId by mutableStateOf<Int?>(null)

    var isDoneByUnmatched by mutableStateOf(false)

    var userfields by mutableStateOf<List<UserfieldInput>>(emptyList())
        private set

    var errors by mutableStateOf<Map<ChoreTrackingField, FieldError>>(emptyMap())
        private set

    var userfieldErrors by mutableStateOf<Map<String, UserfieldError>>(emptyMap())
        private set

    var isSaving by mutableStateOf(false)
        private set

    val options: StateFlow<ChoreTrackingOptions?> = combine(
        masterData.chores,
        masterData.users,
        masterData.serverConfig,
    ) { all, users, config ->
        ChoreTrackingOptions(
            chores = all.filter { it.active }.sortedBy { it.name.lowercase() },
            users = users.takeIf { config.choresAssignments }.orEmpty(),
            config = config,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Stays when the form starts over, for the overview of the chore just tracked
    private val shownChoreId = MutableStateFlow<Int?>(null)

    /** The chore chosen or last tracked, what Grocy's "Chore overview" opens for. */
    val shownChore: StateFlow<ShownChore?> = combine(shownChoreId, options) { id, current ->
        id?.let { shown ->
            current?.chores?.firstOrNull { it.id == shown }?.let { ShownChore(shown, it.name) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    private val eventChannel = Channel<ChoreTrackingEvent>(Channel.BUFFERED)
    val events: Flow<ChoreTrackingEvent> = eventChannel.receiveAsFlow()

    val selectedChore: Chore?
        get() = options.value?.chores?.firstOrNull { it.id == choreId }

    val trackDateOnly: Boolean
        get() = selectedChore?.trackDateOnly == true

    /** Grocy's "Skip" has no next schedule to skip for chores without one. */
    val canSkip: Boolean
        get() = selectedChore?.periodType != ChorePeriodType.MANUALLY

    init {
        viewModelScope.launch {
            val userId = chores.currentUserId.first()
            if (userId != null) doneByUserId = userId
            options.filterNotNull().first()
            presetChoreId?.let { select(it, movesOn = false) }
            userfields = freshUserfields()
        }
        viewModelScope.launch {
            ignoringServerErrors { chores.refresh() }
            // The first download also tells who the logged in user is
            if (doneByUserId == null) doneByUserId = chores.currentUserId.first()
        }
    }

    /** Chooses chore [id] with the time as it tracks it. */
    fun selectChore(id: Int?) {
        if (id == choreId) return
        if (id == null) {
            choreId = null
            return
        }
        select(id, movesOn = true)
    }

    private fun select(id: Int, movesOn: Boolean) {
        choreId = id
        errors = errors - ChoreTrackingField.CHORE
        setNow()
        shownChoreId.value = id
        if (movesOn) scanner.onValueChosen()
    }

    /** A scanned code: its chore is chosen, one tap tracks it; false if it names none. */
    fun onScanned(code: String): Boolean {
        val id = choreOfCode(code)
        if (id == null) {
            viewModelScope.launch { eventChannel.send(ChoreTrackingEvent.UnknownCode) }
            return false
        }
        select(id, movesOn = false)
        scanner.onScanned()
        return true
    }

    /** Text submitted in the chore's field, which a barcode reader types like a keyboard. */
    fun onTextSubmitted(text: String): Boolean {
        val id = choreOfCode(text) ?: return false
        select(id, movesOn = false)
        scanner.requestFocus(FormFocusTarget.NEXT_FIELD)
        return true
    }

    /** The keyboard's action of the chore's field; without a chore typing goes on there. */
    fun goToTrackedTime() {
        if (choreId != null) scanner.requestFocus(FormFocusTarget.NEXT_FIELD)
    }

    private fun choreOfCode(code: String): Int? =
        Grocycode.choreIdOf(code)
            ?.takeIf { id -> options.value?.chores?.any { it.id == id } == true }

    /**
     * Grocy's "OK" or, with [skipped], its "Skip"; false when nothing is sent because the input
     * is invalid.
     */
    fun track(skipped: Boolean = false): Boolean {
        if (isSaving) return true
        val time = validated()
        val inputs = userfields
        userfieldErrors = inputs.validationErrors()
        if (time == null || errors.isNotEmpty() || userfieldErrors.isNotEmpty() ||
            isChoreUnmatched || isDoneByUnmatched || inputs.any { it.isUnmatched }
        ) {
            return false
        }
        val chore = selectedChore ?: return false
        if (skipped && !canSkip) return true
        isSaving = true
        viewModelScope.launch {
            var event: ChoreEvent? = null
            val error = changeErrorOf {
                val id = chores.track(
                    choreId = chore.id,
                    trackedTime = time,
                    doneByUserId = doneByUserId,
                    skipped = skipped,
                    userfields = inputs.changedValues(),
                )
                event = ChoreEvent.Tracked(chore.id, chore.name, time, chore.trackDateOnly, id)
            }
            isSaving = false
            if (event != null) startOver()
            eventChannel.send(
                ChoreTrackingEvent.Result(event ?: ChoreEvent.Failed(requireNotNull(error))),
            )
        }
        return true
    }

    /** Grocy's "Undo" of the message. */
    fun undo(event: ChoreEvent.Tracked) {
        viewModelScope.launch {
            val error = changeErrorOf { chores.undo(event.executionId, event.choreId) }
            eventChannel.send(
                ChoreTrackingEvent.Result(error?.let(ChoreEvent::Failed) ?: ChoreEvent.Undone),
            )
        }
    }

    /** The tracked time as Grocy takes it, null when it or the chore is invalid. */
    private fun validated(): String? {
        val text = trackedTime.text.toString().trim()
        val valid = if (trackDateOnly) normalizedDate(text) else normalizedDateTime(text)
        errors = buildMap {
            if (choreId == null) put(ChoreTrackingField.CHORE, FieldError.Required)
            when {
                text.isEmpty() -> put(ChoreTrackingField.TRACKED_TIME, FieldError.Required)
                valid == null -> put(
                    ChoreTrackingField.TRACKED_TIME,
                    if (trackDateOnly) FieldError.InvalidDate else FieldError.InvalidDateTime,
                )
            }
        }
        return valid
    }

    private suspend fun startOver() {
        choreId = null
        setNow()
        errors = emptyMap()
        userfields = freshUserfields()
        userfieldErrors = emptyMap()
        scanner.onFormReset()
    }

    private fun setNow() {
        val time = now()
        trackedTime.setTextAndPlaceCursorAtEnd(if (trackDateOnly) time.take(DATE_LENGTH) else time)
    }

    private suspend fun freshUserfields(): List<UserfieldInput> =
        userfieldInputs(masterData.userfields(CHORES_LOG).first(), null)

    private companion object {
        const val CHORES_LOG = "chores_log"
        const val DATE_LENGTH = 10
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
