package com.patrickzedler.grocy.feature.chores.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.chores.ChoresRepository
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.util.RefreshController
import com.patrickzedler.grocy.core.data.util.RefreshState
import com.patrickzedler.grocy.core.model.User
import com.patrickzedler.grocy.core.model.chores.CurrentChore
import com.patrickzedler.grocy.core.model.masterdata.ChorePeriodType
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

/** The chore whose overview is open, with the name its opener knows for the first frame. */
internal data class ShownChore(val id: Int, val name: String)

/** What Grocy's "Chore overview" shows of a chore and what can be done with it. */
internal data class ChoreOverviewUiState(
    val choreId: Int,
    val card: ChoreCardData?,
    /** Null while the chore has no row of the overview, e.g. disabled. */
    val current: CurrentChore?,
    val canSkip: Boolean,
    val swapsTrackingButtons: Boolean,
    val users: List<User>,
    val config: ServerConfig,
    val hasDetails: Boolean,
)

/**
 * Grocy's "Chore overview" of the chore a screen [show]s: the stored values at once (also offline),
 * the server's current ones downloaded every time it opens.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class ChoreOverviewViewModel(
    private val masterData: MasterDataRepository,
    private val chores: ChoresRepository,
    now: () -> String = { grocyDateTimeOf(localNowMillis()) },
) : ViewModel() {

    val actions = ChoreActions(viewModelScope, chores, now)

    private val shownChoreState = MutableStateFlow<ShownChore?>(null)

    /** The chore whose overview is open, null while closed; kept across rotations. */
    val shownChore: StateFlow<ShownChore?> = shownChoreState.asStateFlow()

    private val shownChoreId = MutableStateFlow<Int?>(null)

    /**
     * Null while closed and while the database is read; briefly the previous chore's after [show],
     * so compare [ChoreOverviewUiState.choreId].
     */
    val state: StateFlow<ChoreOverviewUiState?> = shownChoreId
        .flatMapLatest { id -> if (id == null) flowOf(null) else overview(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    // One controller per opening: closing cancels its download, the next opening starts fresh
    private val controller = MutableStateFlow<RefreshController?>(null)
    private var controllerScope: CoroutineScope? = null

    /** Errors stay until the next download: the dialog shows them as long as it is open. */
    val refresh: StateFlow<RefreshState> = controller
        .flatMapLatest { it?.state ?: flowOf(RefreshState()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, RefreshState())

    /** Opens the overview of chore [id] called [name] and downloads it again. */
    fun show(id: Int, name: String) {
        hide()
        shownChoreState.value = ShownChore(id, name)
        shownChoreId.value = id
        val scope = viewModelScope + Job(viewModelScope.coroutineContext[Job])
        controllerScope = scope
        controller.value = RefreshController(scope) {
            coroutineScope {
                launch { chores.refresh() }
                launch { chores.refreshDetails(id) }
            }
        }.also { it.refresh() }
    }

    fun hide() {
        controllerScope?.cancel()
        controllerScope = null
        controller.value = null
        shownChoreState.value = null
        shownChoreId.value = null
    }

    fun refresh() {
        controller.value?.refresh()
    }

    private fun overview(id: Int) = combine(
        masterData.chores,
        chores.current,
        chores.details(id),
        settings,
    ) { all, current, details, (config, swaps, users) ->
        val chore = all.firstOrNull { it.id == id }
        val row = current.firstOrNull { it.choreId == id }
        ChoreOverviewUiState(
            choreId = id,
            card = choreCardOf(chore, row, details, users, config.choresAssignments),
            current = row,
            canSkip = chore?.periodType != ChorePeriodType.MANUALLY,
            swapsTrackingButtons = swaps,
            users = users.takeIf { config.choresAssignments }.orEmpty(),
            config = config,
            hasDetails = details != null,
        )
    }

    private val settings
        get() = combine(
            masterData.serverConfig,
            masterData.userSettings,
            masterData.users,
        ) { config, settings, users ->
            Triple(config, settings.choresOverviewSwapTrackingButtons, users)
        }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
