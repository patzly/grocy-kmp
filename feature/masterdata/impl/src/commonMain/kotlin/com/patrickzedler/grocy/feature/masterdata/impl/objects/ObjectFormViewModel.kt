package com.patrickzedler.grocy.feature.masterdata.impl.objects

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.network.GrocyApiException
import com.patrickzedler.grocy.feature.masterdata.impl.product.FieldError
import com.patrickzedler.grocy.feature.masterdata.impl.product.UserfieldError
import com.patrickzedler.grocy.feature.masterdata.impl.product.UserfieldInput
import com.patrickzedler.grocy.feature.masterdata.impl.product.defaultUserfieldValue
import com.patrickzedler.grocy.feature.masterdata.impl.product.validateUserfield
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** Inputs of the master data forms that can be invalid. */
internal enum class ObjectField {
    NAME,
    CAPTION,
    ENTITY,
    TYPE,
    CHARGE_INTERVAL,
    SORT_NUMBER,
    PERIOD_DAYS,
    PERIOD_INTERVAL,
    START_DATE,
    ASSIGNED_USERS,
    PRODUCT_AMOUNT,
}

/** The editable state of a form, which turns into its typed [values]. */
internal interface ObjectFormState<V> {
    fun values(): V

    /** True while a picker's text names none of its options; the form is not saved then. */
    val hasUnmatchedChoices: Boolean
        get() = false
}

internal sealed interface ObjectFormLoad<out F> {
    data object Loading : ObjectFormLoad<Nothing>

    /** The object does not exist (anymore), e.g. deleted on the server. */
    data object NotFound : ObjectFormLoad<Nothing>

    class Ready<F>(val form: F, val userfields: List<UserfieldInput>) : ObjectFormLoad<F>
}

internal sealed interface ObjectFormEvent {
    /** An existing object was saved, the form closes like Grocy's "Save & return". */
    data object Saved : ObjectFormEvent

    /** A new object was created; the form now edits it. */
    data object Created : ObjectFormEvent

    data object Offline : ObjectFormEvent

    data class Failed(val serverMessage: String?) : ObjectFormEvent
}

/**
 * Grocy's form of one master data object: edits the object with [id] or creates one. Reads the
 * database, saving is online only and stores what the server saved. Subclasses call [start] at
 * the end of their initialization.
 */
internal abstract class ObjectFormViewModel<V : Any, F : ObjectFormState<V>>(
    id: Int?,
    protected val repository: MasterDataRepository,
) : ViewModel() {

    /** The edited object's id, [MasterDataRepository.NEW_OBJECT_ID] until it is created. */
    var objectId by mutableStateOf(id ?: MasterDataRepository.NEW_OBJECT_ID)
        private set

    var load by mutableStateOf<ObjectFormLoad<F>>(ObjectFormLoad.Loading)
        private set

    var errors by mutableStateOf<Map<ObjectField, FieldError>>(emptyMap())
        private set

    var userfieldErrors by mutableStateOf<Map<String, UserfieldError>>(emptyMap())
        private set

    var isSaving by mutableStateOf(false)
        private set

    // What the form showed when it was loaded or last saved, to tell unsaved changes
    private var savedValues by mutableStateOf<V?>(null)
    private var savedUserfields by mutableStateOf<Map<String, String?>>(emptyMap())

    private val eventChannel = Channel<ObjectFormEvent>(Channel.BUFFERED)
    val events: Flow<ObjectFormEvent> = eventChannel.receiveAsFlow()

    val isNew: Boolean
        get() = objectId == MasterDataRepository.NEW_OBJECT_ID

    /** True when the form differs from the saved object (or the defaults of a new one). */
    val hasUnsavedChanges: Boolean
        get() {
            val ready = load as? ObjectFormLoad.Ready ?: return false
            return ready.form.values() != savedValues ||
                ready.form.hasUnmatchedChoices ||
                ready.userfields.any {
                    it.isUnmatched || savedUserfields[it.userfield.name] != it.value
                }
        }

    /** The form of the object [id], or of a new object without one; null if it is unknown. */
    protected abstract suspend fun createForm(id: Int?): F?

    /** Userfield values of the object [id] by name; never called for new objects. */
    protected open suspend fun userfieldValues(id: Int): Map<String, String?> = emptyMap()

    /** Grocy's entity of the object's userfields, null for objects without. */
    protected open val userfieldEntity: String? = null

    protected abstract fun validate(form: F): Map<ObjectField, FieldError>

    /**
     * Saves [form] as object [id] (new for [MasterDataRepository.NEW_OBJECT_ID]) with the
     * changed [userfields]; returns its id.
     *
     * @throws GrocyApiException when the server cannot be reached or rejects it.
     */
    protected abstract suspend fun save(id: Int, form: F, userfields: Map<String, String?>): Int

    protected fun start() {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val id = objectId.takeUnless { isNew }
        var form = createForm(id)
        if (id != null && form == null) {
            // Opened from a link before the objects were downloaded
            try {
                repository.refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (_: GrocyApiException) {
                // Not found either way
            }
            form = createForm(id)
        }
        if (form == null) {
            load = ObjectFormLoad.NotFound
            return
        }
        val values = id?.let { userfieldValues(it) }
        val userfields = userfieldEntity?.let { repository.userfields(it).first() }.orEmpty()
            .map { field ->
                UserfieldInput(field, if (values != null) values[field.name] else {
                    defaultUserfieldValue(field)
                })
            }
        savedValues = form.values()
        savedUserfields = userfields.associate { it.userfield.name to it.value }
        load = ObjectFormLoad.Ready(form, userfields)
    }

    /** Saves the form; false when nothing is sent because the input is invalid. */
    fun save(): Boolean {
        val ready = load as? ObjectFormLoad.Ready ?: return false
        if (isSaving) return true
        errors = validate(ready.form)
        userfieldErrors = ready.userfields.mapNotNull { input ->
            validateUserfield(input.userfield, input.value)?.let { input.userfield.name to it }
        }.toMap()
        // Pickers with text that names no option show their own error
        val hasUnmatchedChoices = ready.form.hasUnmatchedChoices ||
            ready.userfields.any { it.isUnmatched }
        if (errors.isNotEmpty() || userfieldErrors.isNotEmpty() || hasUnmatchedChoices) {
            return false
        }

        val changedUserfields = ready.userfields.filter { it.isChanged }
            .associate { it.userfield.name to it.value }
        isSaving = true
        viewModelScope.launch {
            val event = try {
                val wasNew = isNew
                val id = save(objectId, ready.form, changedUserfields)
                objectId = id
                savedValues = ready.form.values()
                savedUserfields = ready.userfields.associate { it.userfield.name to it.value }
                if (wasNew) ObjectFormEvent.Created else ObjectFormEvent.Saved
            } catch (e: CancellationException) {
                throw e
            } catch (_: GrocyApiException.Unreachable) {
                ObjectFormEvent.Offline
            } catch (e: GrocyApiException.HttpError) {
                ObjectFormEvent.Failed(e.serverMessage)
            } catch (_: GrocyApiException) {
                ObjectFormEvent.Failed(null)
            }
            isSaving = false
            eventChannel.send(event)
        }
        return true
    }
}
