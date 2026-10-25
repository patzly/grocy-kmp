package com.patrickzedler.grocy.feature.masterdata.impl.objects

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.network.GrocyApiException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** What the overflow menus of lists and forms do with one object, in the menus' order. */
internal enum class ObjectAction { COPY, MERGE, DELETE }

/**
 * The actions Grocy offers on objects of [entity]: every object can be deleted, products
 * copied, products and chores merged.
 */
internal fun objectActionsOf(entity: String): List<ObjectAction> = when (entity) {
    MasterDataRepository.PRODUCTS -> ObjectAction.entries
    MasterDataRepository.CHORES -> listOf(ObjectAction.MERGE, ObjectAction.DELETE)
    else -> listOf(ObjectAction.DELETE)
}

/** Grocy's entity of the objects of the list, the name of their API path. */
internal val ObjectListKind.entity: String
    get() = when (this) {
        ObjectListKind.LOCATIONS -> MasterDataRepository.LOCATIONS
        ObjectListKind.STORES -> MasterDataRepository.STORES
        ObjectListKind.QUANTITY_UNITS -> MasterDataRepository.QUANTITY_UNITS
        ObjectListKind.PRODUCT_GROUPS -> MasterDataRepository.PRODUCT_GROUPS
        ObjectListKind.CHORES -> MasterDataRepository.CHORES
        ObjectListKind.BATTERIES -> MasterDataRepository.BATTERIES
        ObjectListKind.TASK_CATEGORIES -> MasterDataRepository.TASK_CATEGORIES
        ObjectListKind.USERFIELDS -> MasterDataRepository.USERFIELDS
        ObjectListKind.USERENTITIES -> MasterDataRepository.USERENTITIES
    }

/** An object of the merge pickers. */
internal data class MergeCandidate(val id: Int, val name: String)

internal enum class MergeError { REQUIRED, SAME }

/** Grocy needs both objects, and merging one into itself makes no sense. */
internal fun mergeErrorOf(keepId: Int?, removeId: Int?): MergeError? = when {
    keepId == null || removeId == null -> MergeError.REQUIRED
    keepId == removeId -> MergeError.SAME
    else -> null
}

internal sealed interface ObjectActionDialog {
    /** Asks before deleting, naming the object like Grocy. */
    data class Delete(val id: Int, val name: String) : ObjectActionDialog

    /** Grocy's merge dialog, the object it was opened for preset as the one to keep. */
    data class Merge(val keepId: Int) : ObjectActionDialog
}

internal sealed interface ObjectActionEvent {
    data class Deleted(val id: Int) : ObjectActionEvent

    /** The copy of a product, which opens like Grocy does. */
    data class Copied(val id: Int) : ObjectActionEvent

    data class Merged(val keepId: Int, val removeId: Int) : ObjectActionEvent

    data object Offline : ObjectActionEvent

    data class Failed(val action: ObjectAction, val serverMessage: String?) : ObjectActionEvent
}

/**
 * Deletes, copies and merges objects of [entity] (one of [MasterDataRepository]'s entity names)
 * for a list or a form, online only like every change. Deleting and merging ask first through
 * [dialog]; results arrive as [events].
 */
internal class ObjectActionsViewModel(
    val entity: String,
    private val repository: MasterDataRepository,
) : ViewModel() {

    val actions: List<ObjectAction> = objectActionsOf(entity)

    var dialog by mutableStateOf<ObjectActionDialog?>(null)
        private set

    var isRunning by mutableStateOf(false)
        private set

    private val eventChannel = Channel<ObjectActionEvent>(Channel.BUFFERED)
    val events: Flow<ObjectActionEvent> = eventChannel.receiveAsFlow()

    /** The objects the merge dialog offers, by name like Grocy's pickers. */
    val mergeCandidates: Flow<List<MergeCandidate>> = when (entity) {
        MasterDataRepository.PRODUCTS ->
            repository.products.map { list -> list.map { MergeCandidate(it.id, it.name) } }
        MasterDataRepository.CHORES ->
            repository.chores.map { list -> list.map { MergeCandidate(it.id, it.name) } }
        else -> flowOf(emptyList())
    }.map { list -> list.sortedBy { it.name.lowercase() } }

    fun request(action: ObjectAction, id: Int) {
        if (isRunning) return
        when (action) {
            ObjectAction.COPY -> run(action) {
                ObjectActionEvent.Copied(repository.copyProduct(id))
            }
            ObjectAction.MERGE -> dialog = ObjectActionDialog.Merge(keepId = id)
            ObjectAction.DELETE -> viewModelScope.launch {
                dialog = ObjectActionDialog.Delete(id, nameOf(id).orEmpty())
            }
        }
    }

    fun dismissDialog() {
        dialog = null
    }

    fun delete(id: Int) = run(ObjectAction.DELETE) {
        repository.deleteObject(entity, id)
        ObjectActionEvent.Deleted(id)
    }

    fun merge(keepId: Int, removeId: Int) {
        if (mergeErrorOf(keepId, removeId) != null) return
        run(ObjectAction.MERGE) {
            when (entity) {
                MasterDataRepository.PRODUCTS -> repository.mergeProducts(keepId, removeId)
                MasterDataRepository.CHORES -> repository.mergeChores(keepId, removeId)
                else -> error("Grocy merges no $entity")
            }
            ObjectActionEvent.Merged(keepId, removeId)
        }
    }

    private fun run(action: ObjectAction, block: suspend () -> ObjectActionEvent) {
        if (isRunning) return
        dialog = null
        isRunning = true
        viewModelScope.launch {
            val event = try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (_: GrocyApiException.Unreachable) {
                ObjectActionEvent.Offline
            } catch (e: GrocyApiException.HttpError) {
                ObjectActionEvent.Failed(action, e.serverMessage)
            } catch (_: GrocyApiException) {
                ObjectActionEvent.Failed(action, serverMessage = null)
            }
            isRunning = false
            eventChannel.send(event)
        }
    }

    /** The name Grocy's question shows; userfields and userentities by their technical name. */
    private suspend fun nameOf(id: Int): String? = when (entity) {
        MasterDataRepository.PRODUCTS -> repository.product(id).first()?.name
        MasterDataRepository.LOCATIONS -> repository.locations.first().find { it.id == id }?.name
        MasterDataRepository.STORES -> repository.stores.first().find { it.id == id }?.name
        MasterDataRepository.QUANTITY_UNITS ->
            repository.quantityUnits.first().find { it.id == id }?.name
        MasterDataRepository.PRODUCT_GROUPS ->
            repository.productGroups.first().find { it.id == id }?.name
        MasterDataRepository.CHORES -> repository.chores.first().find { it.id == id }?.name
        MasterDataRepository.BATTERIES -> repository.batteries.first().find { it.id == id }?.name
        MasterDataRepository.TASK_CATEGORIES ->
            repository.taskCategories.first().find { it.id == id }?.name
        MasterDataRepository.USERENTITIES ->
            repository.userentities.first().find { it.id == id }?.name
        MasterDataRepository.USERFIELDS ->
            repository.allUserfields.first().find { it.id == id }?.name
        // Grocy asks about barcodes and conversions without naming them
        else -> null
    }
}
