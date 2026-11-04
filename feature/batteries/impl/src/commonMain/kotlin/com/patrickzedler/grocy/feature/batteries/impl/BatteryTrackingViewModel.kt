package com.patrickzedler.grocy.feature.batteries.impl

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.batteries.BatteriesRepository
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
import com.patrickzedler.grocy.core.model.masterdata.Battery
import com.patrickzedler.grocy.core.model.text.grocyDateTimeOf
import com.patrickzedler.grocy.core.model.text.localNowMillis
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The inputs of Grocy's battery tracking that can be invalid. */
internal enum class BatteryTrackingField { BATTERY, TRACKED_TIME }

/** What Grocy's battery tracking tells besides its messages for a tracked battery. */
internal sealed interface BatteryTrackingEvent {
    data class Result(val event: BatteryEvent) : BatteryTrackingEvent

    /** A scanned code names no active battery. */
    data object UnknownCode : BatteryTrackingEvent
}

/**
 * Grocy's "Battery tracking": the battery (typed, chosen or scanned as grocycode), the tracked time
 * (not after now) and the charge cycle's userfields. Like Grocy's page the form starts over once
 * tracked and the battery's overview stays available. Tracking is online only.
 *
 * @param scanner the scanner of the battery's field and where the focus goes around it; [hasValue]
 *   a preset battery needs neither.
 */
internal class BatteryTrackingViewModel(
    presetBatteryId: Int?,
    private val masterData: MasterDataRepository,
    private val batteries: BatteriesRepository,
    scanner: (hasValue: Boolean) -> ScannerFieldState = { ScannerFieldState(hasValue = it) },
    private val now: () -> String = { grocyDateTimeOf(localNowMillis()) },
) : ViewModel() {

    val scanner: ScannerFieldState = scanner(presetBatteryId != null)

    var batteryId by mutableStateOf<Int?>(null)
        private set

    var isBatteryUnmatched by mutableStateOf(false)

    val trackedTime = TextFieldState(now())

    var userfields by mutableStateOf<List<UserfieldInput>>(emptyList())
        private set

    var errors by mutableStateOf<Map<BatteryTrackingField, FieldError>>(emptyMap())
        private set

    var userfieldErrors by mutableStateOf<Map<String, UserfieldError>>(emptyMap())
        private set

    var isSaving by mutableStateOf(false)
        private set

    /** Grocy's choices: the active batteries by name. */
    val options: StateFlow<List<Battery>?> = masterData.batteries
        .map { all -> all.filter { it.active }.sortedBy { it.name.lowercase() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Stays when the form starts over, for the overview of the battery just tracked
    private val shownBatteryId = MutableStateFlow<Int?>(null)

    /** The battery chosen or last tracked, what Grocy's "Battery overview" opens for. */
    val shownBattery: StateFlow<ShownBattery?> = combine(shownBatteryId, options) { id, current ->
        id?.let { shown ->
            current?.firstOrNull { it.id == shown }?.let { ShownBattery(shown, it.name) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    private val eventChannel = Channel<BatteryTrackingEvent>(Channel.BUFFERED)
    val events: Flow<BatteryTrackingEvent> = eventChannel.receiveAsFlow()

    val selectedBattery: Battery?
        get() = options.value?.firstOrNull { it.id == batteryId }

    init {
        viewModelScope.launch {
            options.filterNotNull().first()
            presetBatteryId?.let { select(it, movesOn = false) }
            userfields = freshUserfields()
        }
        viewModelScope.launch { ignoringServerErrors { batteries.refresh() } }
    }

    /** Chooses battery [id] with the current time like Grocy's page. */
    fun selectBattery(id: Int?) {
        if (id == batteryId) return
        if (id == null) {
            batteryId = null
            return
        }
        select(id, movesOn = true)
    }

    private fun select(id: Int, movesOn: Boolean) {
        batteryId = id
        errors = errors - BatteryTrackingField.BATTERY
        setNow()
        shownBatteryId.value = id
        if (movesOn) scanner.onValueChosen()
    }

    /** A scanned code: its battery is chosen, one tap tracks it; false if it names none. */
    fun onScanned(code: String): Boolean {
        val id = batteryOfCode(code)
        if (id == null) {
            viewModelScope.launch { eventChannel.send(BatteryTrackingEvent.UnknownCode) }
            return false
        }
        select(id, movesOn = false)
        scanner.onScanned()
        return true
    }

    /** Text submitted in the battery's field, which a barcode reader types like a keyboard. */
    fun onTextSubmitted(text: String): Boolean {
        val id = batteryOfCode(text) ?: return false
        select(id, movesOn = false)
        scanner.requestFocus(FormFocusTarget.NEXT_FIELD)
        return true
    }

    /** The keyboard's action of the battery's field; without a battery typing goes on there. */
    fun goToTrackedTime() {
        if (batteryId != null) scanner.requestFocus(FormFocusTarget.NEXT_FIELD)
    }

    private fun batteryOfCode(code: String): Int? =
        Grocycode.batteryIdOf(code)?.takeIf { id -> options.value?.any { it.id == id } == true }

    /** Grocy's "OK"; false when nothing is sent because the input is invalid. */
    fun track(): Boolean {
        if (isSaving) return true
        val time = validated()
        val inputs = userfields
        userfieldErrors = inputs.validationErrors()
        if (time == null || errors.isNotEmpty() || userfieldErrors.isNotEmpty() ||
            isBatteryUnmatched || inputs.any { it.isUnmatched }
        ) {
            return false
        }
        val battery = selectedBattery ?: return false
        isSaving = true
        viewModelScope.launch {
            var event: BatteryEvent? = null
            val error = changeErrorOf {
                val id = batteries.track(battery.id, time, inputs.changedValues())
                event = BatteryEvent.Tracked(battery.id, battery.name, time, id)
            }
            isSaving = false
            if (event != null) startOver()
            eventChannel.send(
                BatteryTrackingEvent.Result(event ?: BatteryEvent.Failed(requireNotNull(error))),
            )
        }
        return true
    }

    /** Grocy's "Undo" of the message. */
    fun undo(event: BatteryEvent.Tracked) {
        viewModelScope.launch {
            val error = changeErrorOf { batteries.undo(event.cycleId, event.batteryId) }
            eventChannel.send(
                BatteryTrackingEvent.Result(error?.let(BatteryEvent::Failed) ?: BatteryEvent.Undone),
            )
        }
    }

    /** The tracked time as Grocy takes it, null when it or the battery is invalid. */
    private fun validated(): String? {
        val text = trackedTime.text.toString().trim()
        val valid = normalizedDateTime(text)
        errors = buildMap {
            if (batteryId == null) put(BatteryTrackingField.BATTERY, FieldError.Required)
            when {
                text.isEmpty() -> put(BatteryTrackingField.TRACKED_TIME, FieldError.Required)
                valid == null -> put(BatteryTrackingField.TRACKED_TIME, FieldError.InvalidDateTime)
                valid > now() -> put(BatteryTrackingField.TRACKED_TIME, FieldError.NotBeforeNow)
            }
        }
        return valid
    }

    private suspend fun startOver() {
        batteryId = null
        setNow()
        errors = emptyMap()
        userfields = freshUserfields()
        userfieldErrors = emptyMap()
        scanner.onFormReset()
    }

    private fun setNow() {
        trackedTime.setTextAndPlaceCursorAtEnd(now())
    }

    private suspend fun freshUserfields(): List<UserfieldInput> =
        userfieldInputs(masterData.userfields(CHARGE_CYCLES).first(), null)

    private companion object {
        const val CHARGE_CYCLES = "battery_charge_cycles"
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
