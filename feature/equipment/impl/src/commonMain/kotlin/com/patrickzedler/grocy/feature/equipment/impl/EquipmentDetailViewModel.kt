package com.patrickzedler.grocy.feature.equipment.impl

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.equipment.EquipmentRepository
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.util.RefreshController
import com.patrickzedler.grocy.core.data.util.RefreshState
import com.patrickzedler.grocy.core.forms.htmlToPlainText
import com.patrickzedler.grocy.core.model.equipment.Equipment
import com.patrickzedler.grocy.core.model.equipment.isPdf
import com.patrickzedler.grocy.core.model.equipment.userfieldFileNameOf
import com.patrickzedler.grocy.core.model.masterdata.Userfield
import com.patrickzedler.grocy.core.model.masterdata.UserfieldType
import com.patrickzedler.grocy.core.network.GrocyApiException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A tab of Grocy's equipment page, in its order. */
internal sealed interface EquipmentTab {
    data object Manual : EquipmentTab

    data object Notes : EquipmentTab

    /** A userfield of the type file, which Grocy shows as a tab of its own. */
    data class File(val name: String, val caption: String) : EquipmentTab
}

/** The tabs of a piece of equipment with the server's [userfields]. */
internal fun equipmentTabsOf(userfields: List<Userfield>): List<EquipmentTab> =
    listOf(EquipmentTab.Manual, EquipmentTab.Notes) +
        userfields.filter { it.type == UserfieldType.FILE }
            .map { EquipmentTab.File(it.name, it.caption) }

/** What Grocy opens first: the manual, the notes for a piece without one. */
internal fun firstTabOf(equipment: Equipment): EquipmentTab =
    if (equipment.instructionManualFileName != null) EquipmentTab.Manual else EquipmentTab.Notes

/** What a tab with a file shows. */
internal sealed interface FileState {
    /** The piece has no such file. */
    data object None : FileState

    data object Loading : FileState

    /** It could not be downloaded; [isOffline] when the server could not be reached. */
    data class Failed(val isOffline: Boolean) : FileState

    /** Downloaded; only [isPdf] files are shown, every file can be saved. */
    class Loaded(val fileName: String, val bytes: ByteArray) : FileState {
        val isPdf: Boolean = isPdf(bytes)
    }
}

/** What Grocy's equipment page shows of a piece. */
internal data class EquipmentDetailUiState(
    val equipment: Equipment,
    val tabs: List<EquipmentTab>,
    /** Grocy's notes as text, null without any. */
    val notes: String?,
)

/**
 * What Grocy's equipment page shows of piece [id]: the stored values at once, files are only
 * downloaded for the tab that shows them and kept while the screen lives.
 */
internal class EquipmentDetailViewModel(
    private val id: Int,
    masterData: MasterDataRepository,
    private val repository: EquipmentRepository,
) : ViewModel() {

    private val refreshController = RefreshController(viewModelScope) { repository.refresh() }
    val refresh: StateFlow<RefreshState> = refreshController.state

    /** Null until read or while the piece does not exist (anymore). */
    val state: StateFlow<EquipmentDetailUiState?> = combine(
        repository.equipment,
        masterData.userfields(MasterDataRepository.EQUIPMENT),
    ) { all, userfields ->
        all.firstOrNull { it.id == id }?.let { equipment ->
            EquipmentDetailUiState(
                equipment = equipment,
                tabs = equipmentTabsOf(userfields),
                notes = equipment.description?.let(::htmlToPlainText)?.takeIf { it.isNotBlank() },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** The tab the user chose, null for Grocy's first one. */
    var chosenTab by mutableStateOf<EquipmentTab?>(null)
        private set

    private val files = mutableMapOf<String, FileState>()
    private var loadedFor: String? = null
    private var loadJob: Job? = null

    /** What the [tab] of the file shows. */
    var fileState by mutableStateOf<FileState>(FileState.None)
        private set

    init {
        refresh()
    }

    fun refresh() = refreshController.refresh()

    fun onErrorShown() = refreshController.onErrorShown()

    fun tabOf(state: EquipmentDetailUiState): EquipmentTab =
        chosenTab?.takeIf { it in state.tabs } ?: firstTabOf(state.equipment)

    fun chooseTab(tab: EquipmentTab) {
        chosenTab = tab
    }

    /** Called for the tab shown: downloads its file once. */
    fun load(tab: EquipmentTab, equipment: Equipment) {
        when (tab) {
            EquipmentTab.Notes -> show(key = null, fileName = { it }, download = repository::manual)
            EquipmentTab.Manual -> show(
                key = equipment.instructionManualFileName,
                fileName = { it },
                download = repository::manual,
            )
            is EquipmentTab.File -> show(
                key = equipment.userfields[tab.name]?.ifBlank { null },
                fileName = { userfieldFileNameOf(it) ?: tab.caption },
                download = repository::userfieldFile,
            )
        }
    }

    /** Downloads the file of the shown tab again after a failure. */
    fun retry() {
        val key = loadedFor ?: return
        files.remove(key)
        loadedFor = null
        val current = state.value ?: return
        load(tabOf(current), current.equipment)
    }

    private fun show(
        key: String?,
        fileName: (String) -> String,
        download: suspend (String) -> ByteArray,
    ) {
        if (key == null) {
            loadJob?.cancel()
            loadedFor = null
            fileState = FileState.None
            return
        }
        if (key == loadedFor) return
        loadJob?.cancel()
        loadedFor = key
        files[key]?.let {
            fileState = it
            return
        }
        fileState = FileState.Loading
        loadJob = viewModelScope.launch {
            val result = try {
                FileState.Loaded(fileName(key), download(key))
            } catch (e: CancellationException) {
                throw e
            } catch (e: GrocyApiException) {
                FileState.Failed(isOffline = e is GrocyApiException.Unreachable)
            }
            if (result is FileState.Loaded) files[key] = result
            fileState = result
        }
    }
}
