package com.patrickzedler.grocy.feature.batteries.impl

import androidx.compose.runtime.Stable
import com.patrickzedler.grocy.core.data.batteries.BatteriesRepository
import com.patrickzedler.grocy.core.forms.ChangeError
import com.patrickzedler.grocy.core.forms.changeErrorOf
import com.patrickzedler.grocy.core.model.text.grocyDateTimeOf
import com.patrickzedler.grocy.core.model.text.localNowMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** What an action on a battery did, for Grocy's messages. */
internal sealed interface BatteryEvent {
    /** [cycleId] undoes it. */
    data class Tracked(
        val batteryId: Int,
        val batteryName: String,
        val trackedTime: String,
        val cycleId: Int,
    ) : BatteryEvent

    data object Undone : BatteryEvent

    data object LabelPrinted : BatteryEvent

    data class LabelFailed(val error: ChangeError) : BatteryEvent

    data class Failed(val error: ChangeError) : BatteryEvent
}

/**
 * Grocy's actions on a battery of its overview and card: "Track charge cycle" now, no question,
 * with "Undo" in its message like its tracking page. All of them are online only.
 */
@Stable
internal class BatteryActions(
    private val scope: CoroutineScope,
    private val repository: BatteriesRepository,
    private val now: () -> String = { grocyDateTimeOf(localNowMillis()) },
) {
    private val eventChannel = Channel<BatteryEvent>(Channel.BUFFERED)
    val events: Flow<BatteryEvent> = eventChannel.receiveAsFlow()

    /** Batteries with a booking under way; a second tap on the same one waits for it. */
    private val tracking = mutableSetOf<Int>()

    fun track(batteryId: Int, batteryName: String) {
        if (!tracking.add(batteryId)) return
        val trackedTime = now()
        scope.launch {
            var event: BatteryEvent? = null
            val error = changeErrorOf {
                val id = repository.track(batteryId, trackedTime)
                event = BatteryEvent.Tracked(batteryId, batteryName, trackedTime, id)
            }
            tracking -= batteryId
            eventChannel.send(event ?: BatteryEvent.Failed(requireNotNull(error)))
        }
    }

    /** Grocy's "Undo" of a message. */
    fun undo(event: BatteryEvent.Tracked) {
        scope.launch {
            val error = changeErrorOf { repository.undo(event.cycleId, event.batteryId) }
            eventChannel.send(error?.let(BatteryEvent::Failed) ?: BatteryEvent.Undone)
        }
    }

    fun printLabel(batteryId: Int) {
        scope.launch {
            val error = changeErrorOf { repository.printLabel(batteryId) }
            eventChannel.send(error?.let(BatteryEvent::LabelFailed) ?: BatteryEvent.LabelPrinted)
        }
    }
}
