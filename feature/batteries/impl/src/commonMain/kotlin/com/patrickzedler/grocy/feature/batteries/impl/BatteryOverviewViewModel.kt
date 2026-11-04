package com.patrickzedler.grocy.feature.batteries.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.batteries.BatteriesRepository
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.util.RefreshController
import com.patrickzedler.grocy.core.data.util.RefreshState
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.text.grocyDateTimeOf
import com.patrickzedler.grocy.core.model.text.localNowMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

/** The battery whose overview is open, with the name its opener knows for the first frame. */
internal data class ShownBattery(val id: Int, val name: String)

/** What Grocy's "Battery overview" shows of a battery. */
internal data class BatteryOverviewUiState(
    val batteryId: Int,
    val card: BatteryCardData?,
    val config: ServerConfig,
    val hasDetails: Boolean,
)

/**
 * Grocy's "Battery overview" of the battery a screen [show]s: the stored values at once (also
 * offline), the server's current ones downloaded every time it opens.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class BatteryOverviewViewModel(
    private val masterData: MasterDataRepository,
    private val batteries: BatteriesRepository,
    now: () -> String = { grocyDateTimeOf(localNowMillis()) },
) : ViewModel() {

    val actions = BatteryActions(viewModelScope, batteries, now)

    private val shownBatteryState = MutableStateFlow<ShownBattery?>(null)

    /** The battery whose overview is open, null while closed; kept across rotations. */
    val shownBattery: StateFlow<ShownBattery?> = shownBatteryState.asStateFlow()

    private val shownBatteryId = MutableStateFlow<Int?>(null)

    /**
     * Null while closed and while the database is read; briefly the previous battery's after
     * [show], so compare [BatteryOverviewUiState.batteryId].
     */
    val state: StateFlow<BatteryOverviewUiState?> = shownBatteryId
        .flatMapLatest { id -> if (id == null) flowOf(null) else overview(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    // One controller per opening: closing cancels its download, the next opening starts fresh
    private val controller = MutableStateFlow<RefreshController?>(null)
    private var controllerScope: CoroutineScope? = null

    /** Errors stay until the next download: the dialog shows them as long as it is open. */
    val refresh: StateFlow<RefreshState> = controller
        .flatMapLatest { it?.state ?: flowOf(RefreshState()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, RefreshState())

    /** Opens the overview of battery [id] called [name] and downloads it again. */
    fun show(id: Int, name: String) {
        hide()
        shownBatteryState.value = ShownBattery(id, name)
        shownBatteryId.value = id
        val scope = viewModelScope + Job(viewModelScope.coroutineContext[Job])
        controllerScope = scope
        controller.value = RefreshController(scope) {
            coroutineScope {
                launch { batteries.refresh() }
                launch { batteries.refreshDetails(id) }
            }
        }.also { it.refresh() }
    }

    fun hide() {
        controllerScope?.cancel()
        controllerScope = null
        controller.value = null
        shownBatteryState.value = null
        shownBatteryId.value = null
    }

    fun refresh() {
        controller.value?.refresh()
    }

    private fun overview(id: Int) = combine(
        masterData.batteries,
        batteries.current,
        batteries.details(id),
        masterData.serverConfig,
    ) { all, current, details, config ->
        BatteryOverviewUiState(
            batteryId = id,
            card = batteryCardOf(
                battery = all.firstOrNull { it.id == id },
                current = current.firstOrNull { it.batteryId == id },
                details = details,
            ),
            config = config,
            hasDetails = details != null,
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
