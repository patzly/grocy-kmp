package com.patrickzedler.grocy.feature.masterdata.impl.objects

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
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.masterdata_products_count
import com.patrickzedler.grocy.core.resources.objects_charge_interval_value
import com.patrickzedler.grocy.core.resources.objects_sort_number_value
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString

/** Texts of the app language for list values, replaced in tests. */
internal interface ListTexts {
    suspend fun string(resource: StringResource, vararg args: Any): String

    suspend fun plural(resource: PluralStringResource, quantity: Int): String
}

internal object ResourceListTexts : ListTexts {
    override suspend fun string(resource: StringResource, vararg args: Any): String =
        getString(resource, *args)

    override suspend fun plural(resource: PluralStringResource, quantity: Int): String =
        getPluralString(resource, quantity, quantity)
}

/** What a master data list shows; built from the database, never waits for the server. */
internal data class ObjectListUiState(
    val rows: List<ObjectRow>,
    val filter: ObjectFilter,
    val sort: ObjectSort,
    /** False until the first download stored any object. */
    val hasObjects: Boolean,
    /** Choices of the entity filter of the userfields. */
    val entities: List<String>,
)

/**
 * One of Grocy's master data lists of [kind]: shows the objects of the database at once and
 * downloads the server's state into it in the background.
 *
 * @param entity the userfields' entity filter to start with.
 */
@OptIn(FlowPreview::class)
internal class ObjectListViewModel(
    val kind: ObjectListKind,
    private val repository: MasterDataRepository,
    entity: String? = null,
    private val texts: ListTexts = ResourceListTexts,
    computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    val searchState = TextFieldState()

    var isSearchVisible by mutableStateOf(false)
        private set

    private val filter = MutableStateFlow(ObjectFilter(entity = entity))
    private val sort = MutableStateFlow(defaultSort)

    private val refreshController = RefreshController(viewModelScope) { repository.refresh() }
    val refresh: StateFlow<RefreshState> = refreshController.state

    // Typing stays smooth with many objects: the list follows after a short pause
    private val query = snapshotFlow { searchState.text.toString() }
        .distinctUntilChanged()
        .debounce { if (it.isEmpty()) 0L else SEARCH_DELAY_MILLIS }

    private val entities: Flow<List<String>> = if (kind == ObjectListKind.USERFIELDS) {
        repository.userentities.map { list -> userfieldEntities(list.map { it.name }) }
    } else {
        flowOf(emptyList())
    }

    val state: StateFlow<ObjectListUiState?> =
        combine(rows(), entities, filter, sort, query) { rows, entities, filter, sort, query ->
            ObjectListUiState(
                rows = buildObjectList(rows, filter, sort, query, nameColumn = kind.headline),
                filter = filter,
                sort = sort,
                hasObjects = rows.isNotEmpty(),
                entities = entities,
            )
        }
            .flowOn(computeDispatcher)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    init {
        refresh()
    }

    fun refresh() = refreshController.refresh()

    fun onErrorShown() = refreshController.onErrorShown()

    fun setFilter(update: (ObjectFilter) -> ObjectFilter) {
        filter.update(update)
    }

    fun setSort(value: ObjectSort) {
        sort.value = value
    }

    val defaultSort: ObjectSort
        get() = ObjectSort(kind.defaultColumn)

    /** Like Grocy's "Clear filter": resets every filter and the search. */
    fun clearFilter() {
        filter.value = ObjectFilter()
        searchState.clearText()
        isSearchVisible = false
    }

    fun showSearch() {
        isSearchVisible = true
    }

    fun hideSearch() {
        searchState.clearText()
        isSearchVisible = false
    }

    private fun rows(): Flow<List<ObjectRow>> = when (kind) {
        ObjectListKind.LOCATIONS -> repository.locations.map { list ->
            list.map { simpleRow(it.id, it.active, it.name, it.description) }
        }

        ObjectListKind.STORES -> repository.stores.map { list ->
            list.map { simpleRow(it.id, it.active, it.name, it.description) }
        }

        ObjectListKind.QUANTITY_UNITS -> repository.quantityUnits.map { list ->
            list.map { simpleRow(it.id, it.active, it.name, it.description) }
        }

        ObjectListKind.PRODUCT_GROUPS ->
            combine(repository.productGroups, repository.products) { groups, products ->
                // Like Grocy, every product of the group counts, disabled ones too
                val counts = products.groupingBy { it.productGroupId }.eachCount()
                groups.map { group ->
                    val count = counts[group.id] ?: 0
                    val row = simpleRow(group.id, group.active, group.name, group.description)
                    row.copy(
                        texts = row.texts + (ObjectColumn.PRODUCT_COUNT to
                            texts.plural(Res.plurals.masterdata_products_count, count)),
                        numbers = mapOf(ObjectColumn.PRODUCT_COUNT to count.toDouble()),
                    )
                }
            }

        ObjectListKind.CHORES -> repository.chores.map { list ->
            list.map { chore ->
                val row = simpleRow(chore.id, chore.active, chore.name, chore.description)
                row.copy(
                    texts = row.texts +
                        (ObjectColumn.PERIOD_TYPE to texts.string(chore.periodType.label)),
                )
            }
        }

        ObjectListKind.BATTERIES -> repository.batteries.map { list ->
            list.map { battery ->
                val row = simpleRow(battery.id, battery.active, battery.name, battery.description)
                val interval = battery.chargeIntervalDays
                row.copy(
                    texts = row.texts + textsOf(
                        ObjectColumn.USED_IN to battery.usedIn,
                        // 0 turns the charge cycles off, Grocy's table shows it as 0
                        ObjectColumn.CHARGE_INTERVAL to texts
                            .plural(Res.plurals.objects_charge_interval_value, interval)
                            .takeIf { interval > 0 },
                    ),
                    numbers = mapOf(ObjectColumn.CHARGE_INTERVAL to interval.toDouble()),
                )
            }
        }

        ObjectListKind.TASK_CATEGORIES -> repository.taskCategories.map { list ->
            list.map { simpleRow(it.id, it.active, it.name, it.description) }
        }

        ObjectListKind.USERFIELDS -> repository.allUserfields.map { list ->
            list.map { field ->
                ObjectRow(
                    id = field.id,
                    texts = textsOf(
                        ObjectColumn.ENTITY to field.entity,
                        ObjectColumn.NAME to field.name,
                        ObjectColumn.CAPTION to field.caption,
                        ObjectColumn.TYPE to texts.string(field.type.label),
                        ObjectColumn.SORT_NUMBER to field.sortNumber?.let {
                            texts.string(Res.string.objects_sort_number_value, it.toString())
                        },
                    ),
                    numbers = field.sortNumber
                        ?.let { mapOf(ObjectColumn.SORT_NUMBER to it.toDouble()) }
                        .orEmpty(),
                    entity = field.entity,
                )
            }
        }

        ObjectListKind.USERENTITIES -> repository.userentities.map { list ->
            list.map { entity ->
                ObjectRow(
                    id = entity.id,
                    texts = textsOf(
                        ObjectColumn.NAME to entity.name,
                        ObjectColumn.CAPTION to entity.caption,
                    ),
                    entity = MasterDataRepository.USERENTITY_PREFIX + entity.name,
                )
            }
        }
    }

    private companion object {
        const val SEARCH_DELAY_MILLIS = 150L
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

private fun simpleRow(id: Int, active: Boolean, name: String, description: String?) = ObjectRow(
    id = id,
    active = active,
    texts = textsOf(ObjectColumn.NAME to name, ObjectColumn.DESCRIPTION to description),
)

/** The values that are not empty. */
private fun textsOf(vararg values: Pair<ObjectColumn, String?>): Map<ObjectColumn, String> =
    values.mapNotNull { (column, text) ->
        text?.trim()?.takeIf { it.isNotEmpty() }?.let { column to it }
    }.toMap()
