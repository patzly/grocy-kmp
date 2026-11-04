package com.patrickzedler.grocy.feature.equipment.impl

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.equipment.EquipmentRepository
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.settings.EquipmentOptionsRepository
import com.patrickzedler.grocy.core.data.util.RefreshController
import com.patrickzedler.grocy.core.data.util.RefreshState
import com.patrickzedler.grocy.core.forms.sortableUserfieldColumns
import com.patrickzedler.grocy.core.model.equipment.EquipmentField
import com.patrickzedler.grocy.core.model.equipment.EquipmentGrouping
import com.patrickzedler.grocy.core.model.equipment.EquipmentSortField
import com.patrickzedler.grocy.core.model.masterdata.TableColumn
import com.patrickzedler.grocy.core.model.masterdata.Userfield
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the equipment list shows; built from the database, never waits for the server. */
internal data class EquipmentUiState(
    val groups: List<EquipmentGroup>,
    val count: Int,
    val sortField: TableColumn<EquipmentSortField>,
    val ascending: Boolean,
    val isDefaultSort: Boolean,
    val grouping: TableColumn<EquipmentGrouping>,
    val fields: List<EquipmentField>,
    /** The userfield columns the list can be sorted and grouped by. */
    val userfieldColumns: List<Userfield>,
) {
    val hasEquipment: Boolean
        get() = count > 0
}

/**
 * Grocy's equipment table: shows the stored equipment at once and downloads it in the background.
 * Fields, grouping and sort are kept like Grocy's table options, the search only here.
 */
internal class EquipmentViewModel(
    masterData: MasterDataRepository,
    private val equipment: EquipmentRepository,
    private val optionsRepository: EquipmentOptionsRepository,
) : ViewModel() {

    val searchState = TextFieldState()

    var isSearchVisible by mutableStateOf(false)
        private set

    private val refreshController = RefreshController(viewModelScope) { equipment.refresh() }
    val refresh: StateFlow<RefreshState> = refreshController.state

    private val query = snapshotFlow { searchState.text.toString() }.distinctUntilChanged()

    val state: StateFlow<EquipmentUiState?> = combine(
        equipment.equipment,
        masterData.userfields(MasterDataRepository.EQUIPMENT),
        optionsRepository.options.filterNotNull(),
        query,
    ) { all, userfields, options, search ->
        val columns = sortableUserfieldColumns(userfields)
        val shown = EquipmentField.entries
            .filter { it in options.fields && columns.isNotEmpty() }
        val sort = effectiveEquipmentSort(options, shown, columns)
        val grouping = effectiveEquipmentGrouping(options, availableEquipmentGroupings(columns))
        EquipmentUiState(
            groups = buildEquipmentList(all, userfields, sort, grouping, search),
            count = all.size,
            sortField = sort.first,
            ascending = sort.second,
            isDefaultSort = sort == (TableColumn.Own(EquipmentSortField.NAME) to true),
            grouping = grouping,
            fields = shown,
            userfieldColumns = columns,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    init {
        refresh()
    }

    fun refresh() = refreshController.refresh()

    fun onErrorShown() = refreshController.onErrorShown()

    fun setSort(column: TableColumn<EquipmentSortField>, ascending: Boolean) {
        viewModelScope.launch { optionsRepository.update { it.withSort(column, ascending) } }
    }

    fun setGrouping(column: TableColumn<EquipmentGrouping>) {
        viewModelScope.launch { optionsRepository.update { it.withGrouping(column) } }
    }

    fun setFieldShown(field: EquipmentField, shown: Boolean) {
        viewModelScope.launch {
            optionsRepository.update {
                it.copy(fields = if (shown) it.fields + field else it.fields - field)
            }
        }
    }

    fun showSearch() {
        isSearchVisible = true
    }

    fun hideSearch() {
        searchState.clearText()
        isSearchVisible = false
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
