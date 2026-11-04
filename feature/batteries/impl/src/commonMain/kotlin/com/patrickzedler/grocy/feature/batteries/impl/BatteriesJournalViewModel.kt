package com.patrickzedler.grocy.feature.batteries.impl

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.batteries.BatteriesRepository
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.settings.BatteriesJournalOptionsRepository
import com.patrickzedler.grocy.core.data.util.RefreshController
import com.patrickzedler.grocy.core.data.util.RefreshState
import com.patrickzedler.grocy.core.forms.ChangeError
import com.patrickzedler.grocy.core.forms.changeErrorOf
import com.patrickzedler.grocy.core.model.batteries.BatteriesJournalField
import com.patrickzedler.grocy.core.model.batteries.BatteriesJournalGrouping
import com.patrickzedler.grocy.core.model.batteries.BatteriesJournalOptions
import com.patrickzedler.grocy.core.model.batteries.BatteriesJournalSortField
import com.patrickzedler.grocy.core.model.batteries.BatteryChargeCycle
import com.patrickzedler.grocy.core.model.masterdata.Battery
import com.patrickzedler.grocy.core.model.text.todayEpochDay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the batteries journal shows. */
internal data class BatteriesJournalUiState(
    val groups: List<BatteriesJournalGroup>,
    /** The active batteries and the battery filtered by, the battery filter's choices. */
    val batteries: List<Battery>,
    val filter: BatteriesJournalFilter,
    val hasCycles: Boolean,
    val sortField: BatteriesJournalSortField,
    val ascending: Boolean,
    val isDefaultSort: Boolean,
    val grouping: BatteriesJournalGrouping,
    val fields: List<BatteriesJournalField>,
    val availableFields: List<BatteriesJournalField>,
)

internal sealed interface BatteriesJournalEvent {
    data object Undone : BatteriesJournalEvent

    data class Failed(val error: ChangeError) : BatteriesJournalEvent
}

/**
 * Grocy's "Batteries journal" (of [batteryId] if given) as stored, downloaded for the chosen date
 * range, with undoing charge cycles. Fields, grouping and sort are kept like Grocy's table options,
 * filters and search only here.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class BatteriesJournalViewModel(
    batteryId: Int?,
    masterData: MasterDataRepository,
    private val batteries: BatteriesRepository,
    private val optionsRepository: BatteriesJournalOptionsRepository,
    private val today: () -> Long = ::todayEpochDay,
) : ViewModel() {

    val searchState = TextFieldState()

    var isSearchVisible by mutableStateOf(false)
        private set

    private val filter = MutableStateFlow(BatteriesJournalFilter(batteryId = batteryId))

    private val since = filter.map { batteriesJournalSince(today(), it.months) }
        .distinctUntilChanged()

    private val refreshController = RefreshController(viewModelScope) {
        batteries.refreshChargeCycles(batteriesJournalSince(today(), filter.value.months))
    }
    val refresh: StateFlow<RefreshState> = refreshController.state

    private val query = snapshotFlow { searchState.text.toString() }.distinctUntilChanged()

    private val data = combine(
        masterData.batteries,
        masterData.userfields(CHARGE_CYCLES),
        ::Pair,
    )

    val state: StateFlow<BatteriesJournalUiState?> = combine(
        since.flatMapLatest { batteries.chargeCycles(it) },
        data,
        filter,
        query,
        optionsRepository.options.filterNotNull(),
    ) { cycles: List<BatteryChargeCycle>, (all, userfields), current, search, options ->
        val available = availableBatteriesJournalFields(
            hasUserfieldColumns = userfields.any { it.showAsColumnInTables },
        )
        val shown = available.filter { it in options.fields }
        val sort = effectiveBatteriesJournalSort(options, shown)
        BatteriesJournalUiState(
            groups = buildBatteriesJournal(
                cycles = cycles,
                batteries = all,
                userfields = userfields,
                filter = current,
                query = search,
                sort = sort,
                grouping = options.grouping,
            ),
            batteries = all.filter { it.active || it.id == current.batteryId }
                .sortedBy { it.name.lowercase() },
            filter = current,
            hasCycles = cycles.isNotEmpty(),
            sortField = sort.first,
            ascending = sort.second,
            isDefaultSort = sort == defaultBatteriesJournalSort(shown),
            grouping = options.grouping,
            fields = shown,
            availableFields = available,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    var isUndoing by mutableStateOf(false)
        private set

    private val eventChannel = Channel<BatteriesJournalEvent>(Channel.BUFFERED)
    val events: Flow<BatteriesJournalEvent> = eventChannel.receiveAsFlow()

    init {
        refresh()
    }

    fun refresh() = refreshController.refresh()

    fun onErrorShown() = refreshController.onErrorShown()

    /** A longer date range downloads the charge cycles it adds. */
    fun setFilter(update: (BatteriesJournalFilter) -> BatteriesJournalFilter) {
        val months = filter.value.months
        filter.update(update)
        if (filter.value.months != months) refresh()
    }

    fun clearFilter() = setFilter { BatteriesJournalFilter() }

    fun setSort(field: BatteriesJournalSortField, ascending: Boolean) {
        updateOptions { it.copy(sortField = field, ascending = ascending) }
    }

    fun setGrouping(grouping: BatteriesJournalGrouping) {
        updateOptions { it.copy(grouping = grouping) }
    }

    fun setFieldShown(field: BatteriesJournalField, shown: Boolean) {
        updateOptions { it.copy(fields = if (shown) it.fields + field else it.fields - field) }
    }

    fun showSearch() {
        isSearchVisible = true
    }

    fun hideSearch() {
        isSearchVisible = false
        searchState.clearText()
    }

    /** Grocy's "Undo charge cycle". */
    fun undo(cycle: BatteryChargeCycle) {
        if (isUndoing || cycle.undone) return
        isUndoing = true
        viewModelScope.launch {
            val error = changeErrorOf { batteries.undo(cycle.id, cycle.batteryId) }
            isUndoing = false
            eventChannel.send(
                if (error == null) {
                    BatteriesJournalEvent.Undone
                } else {
                    BatteriesJournalEvent.Failed(error)
                },
            )
        }
    }

    private fun updateOptions(transform: (BatteriesJournalOptions) -> BatteriesJournalOptions) {
        viewModelScope.launch { optionsRepository.update(transform) }
    }

    private companion object {
        const val CHARGE_CYCLES = "battery_charge_cycles"
    }
}
