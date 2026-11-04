package com.patrickzedler.grocy.feature.masterdata.impl.userobjects

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.util.RefreshController
import com.patrickzedler.grocy.core.data.util.RefreshState
import com.patrickzedler.grocy.core.forms.userfieldText
import com.patrickzedler.grocy.core.forms.userfieldTextOrder
import com.patrickzedler.grocy.core.model.masterdata.Userentity
import com.patrickzedler.grocy.core.model.masterdata.Userfield
import com.patrickzedler.grocy.core.model.masterdata.UserfieldType
import com.patrickzedler.grocy.core.model.masterdata.Userobject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** The order of the list: by a field, Grocy's tables start with their first column. */
internal data class UserobjectSort(val field: Userfield?, val ascending: Boolean = true)

/** One object as an item: the first shown value names it, the others follow as lines. */
internal data class UserobjectRow(val id: Int, val title: String?, val lines: List<String>)

internal data class UserobjectsUiState(
    val userentity: Userentity?,
    val rows: List<UserobjectRow>,
    val fields: List<Userfield>,
    val sort: UserobjectSort,
    val hasObjects: Boolean,
)

/**
 * The fields a table shows: Grocy's columns. An entity without any would be rows of nothing in
 * Grocy; here its fields show instead, an item has to say what it is.
 */
internal fun shownUserfields(fields: List<Userfield>): List<Userfield> {
    val readable = fields.filter {
        it.type != UserfieldType.FILE && it.type != UserfieldType.IMAGE
    }
    return readable.filter { it.showAsColumnInTables }.ifEmpty { readable }
}

/** The items of the shown [fields], found by every shown value and sorted, empty values last. */
internal fun buildUserobjectRows(
    objects: List<Userobject>,
    fields: List<Userfield>,
    query: String,
    sort: UserobjectSort,
): List<UserobjectRow> {
    val shown = shownUserfields(fields)
    val filtered = objects.filter { userobject ->
        query.isBlank() || shown.any { field ->
            userfieldText(field, userobject.userfields[field.name])
                ?.contains(query.trim(), ignoreCase = true) == true
        }
    }
    val field = sort.field
    val sorted = if (field == null) {
        filtered
    } else {
        val order = userfieldTextOrder(field).let { if (sort.ascending) it else it.reversed() }
        filtered.sortedWith { a, b ->
            val first = userfieldText(field, a.userfields[field.name])
            val second = userfieldText(field, b.userfields[field.name])
            when {
                first == null && second == null -> a.id.compareTo(b.id)
                first == null -> 1
                second == null -> -1
                else -> order.compare(first, second).takeIf { it != 0 } ?: a.id.compareTo(b.id)
            }
        }
    }
    return sorted.map { userobject ->
        val texts = shown.mapNotNull { shownField ->
            userfieldText(shownField, userobject.userfields[shownField.name])
                ?.let { shownField to it }
        }
        UserobjectRow(
            id = userobject.id,
            title = texts.firstOrNull()?.second,
            lines = texts.drop(1).map { (shownField, text) ->
                // A checkbox's text is its caption already
                if (shownField.type == UserfieldType.CHECKBOX) {
                    text
                } else {
                    "${shownField.caption}: $text"
                }
            },
        )
    }
}

/**
 * Grocy's page of a userentity (`/userobjects/<name>`): its objects with the values of their
 * fields, stored for offline use and downloaded again when the page opens.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
internal class UserobjectsViewModel(
    private val entityName: String,
    private val repository: MasterDataRepository,
) : ViewModel() {

    val searchState = TextFieldState()

    var isSearchVisible by mutableStateOf(false)
        private set

    private val chosenSort = MutableStateFlow<UserobjectSort?>(null)

    private val refreshController = RefreshController(viewModelScope) {
        repository.refreshUserobjects(entityName)
    }
    val refresh: StateFlow<RefreshState> = refreshController.state

    private val query: Flow<String> = snapshotFlow { searchState.text.toString() }
        .distinctUntilChanged()
        .debounce { if (it.isEmpty()) 0L else SEARCH_DELAY_MILLIS }

    private val content: Flow<Content> = repository.userentities
        .map { list -> list.firstOrNull { it.name == entityName } }
        .distinctUntilChanged()
        .flatMapLatest { userentity ->
            if (userentity == null) {
                flowOf(Content(null, emptyList(), emptyList()))
            } else {
                combine(
                    repository.userobjects(userentity.id),
                    repository.userfields(MasterDataRepository.USERENTITY_PREFIX + entityName),
                ) { objects, fields -> Content(userentity, objects, fields) }
            }
        }

    val state: StateFlow<UserobjectsUiState?> =
        combine(content, chosenSort, query) { content, chosen, query ->
            val fields = shownUserfields(content.fields)
            val sort = chosen?.takeIf { it.field in fields }
                ?: UserobjectSort(fields.firstOrNull())
            UserobjectsUiState(
                userentity = content.userentity,
                rows = buildUserobjectRows(content.objects, content.fields, query, sort),
                fields = fields,
                sort = sort,
                hasObjects = content.objects.isNotEmpty(),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    init {
        refresh()
    }

    fun refresh() = refreshController.refresh()

    fun onErrorShown() = refreshController.onErrorShown()

    fun setSort(field: Userfield, ascending: Boolean) {
        chosenSort.value = UserobjectSort(field, ascending)
    }

    fun showSearch() {
        isSearchVisible = true
    }

    fun hideSearch() {
        isSearchVisible = false
        searchState.clearText()
    }

    private class Content(
        val userentity: Userentity?,
        val objects: List<Userobject>,
        val fields: List<Userfield>,
    )

    private companion object {
        const val SEARCH_DELAY_MILLIS = 250L
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
