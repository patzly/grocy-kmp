package com.patrickzedler.grocy.feature.masterdata.impl.objects

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.model.masterdata.Userentity
import com.patrickzedler.grocy.core.model.masterdata.Userfield
import com.patrickzedler.grocy.core.model.masterdata.UserfieldType
import com.patrickzedler.grocy.feature.masterdata.impl.product.FieldError
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Grocy's "Now / today", the default value of date userfields. */
internal const val USERFIELD_DEFAULT_NOW = "now"

/** The userfield form's inputs as typed; entity and type start empty like Grocy's selects. */
internal data class UserfieldValues(
    val entity: String? = null,
    val name: String = "",
    val caption: String = "",
    val sortNumber: String = "",
    val type: UserfieldType? = null,
    /** Options of the preset types, one per line. */
    val config: String = "",
    val defaultValue: String? = null,
    val showAsColumnInTables: Boolean = false,
    val inputRequired: Boolean = false,
)

/** Types with predefined options, whose configuration lists them. */
internal val UserfieldType.hasOptions: Boolean
    get() = this == UserfieldType.PRESET_LIST || this == UserfieldType.PRESET_CHECKLIST

/** Types that can default to the current date. */
internal val UserfieldType.hasDefaultValue: Boolean
    get() = this == UserfieldType.DATE || this == UserfieldType.DATETIME

/** Checks the values like Grocy's userfield form. */
internal fun UserfieldValues.validate(): Map<ObjectField, FieldError> = buildMap {
    if (entity.isNullOrBlank()) put(ObjectField.ENTITY, FieldError.Required)
    if (!isValidFieldName(name)) put(ObjectField.NAME, FieldError.Required)
    if (caption.isBlank()) put(ObjectField.CAPTION, FieldError.Required)
    if (sortNumber.isNotBlank() && wholeNumber(sortNumber, min = 0) == null) {
        put(ObjectField.SORT_NUMBER, FieldError.InvalidNumber("0", 0))
    }
    if (type == null) put(ObjectField.TYPE, FieldError.Required)
}

/** Grocy's pattern of userfield and userentity names, which the API uses. */
internal fun isValidFieldName(name: String): Boolean = FIELD_NAME.matches(name)

/** The userfield to save from valid values; hidden inputs are sent empty like Grocy's form. */
internal fun UserfieldValues.toUserfield(id: Int): Userfield {
    val type = requireNotNull(type)
    return Userfield(
        id = id,
        entity = requireNotNull(entity),
        name = name,
        caption = caption.trim(),
        type = type,
        showAsColumnInTables = showAsColumnInTables,
        inputRequired = inputRequired,
        options = if (type.hasOptions) {
            config.lines().map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            emptyList()
        },
        defaultValue = defaultValue.takeIf { type.hasDefaultValue },
        sortNumber = wholeNumber(sortNumber, min = 0),
    )
}

internal fun Userfield.toValues() = UserfieldValues(
    entity = entity,
    name = name,
    caption = caption,
    sortNumber = sortNumber?.toString().orEmpty(),
    type = type,
    config = options.joinToString("\n"),
    defaultValue = defaultValue?.takeIf { it.isNotBlank() },
    showAsColumnInTables = showAsColumnInTables,
    inputRequired = inputRequired,
)

/** The pickers of the userfield form. */
internal enum class UserfieldChoice { ENTITY, TYPE }

@Stable
internal class UserfieldFormState(initial: UserfieldValues) : ObjectFormState<UserfieldValues> {
    var entity by mutableStateOf(initial.entity)
    val name = TextFieldState(initial.name)
    val caption = TextFieldState(initial.caption)
    val sortNumber = TextFieldState(initial.sortNumber)
    var type by mutableStateOf(initial.type)
    val config = TextFieldState(initial.config)
    var defaultValue by mutableStateOf(initial.defaultValue)
    var showAsColumnInTables by mutableStateOf(initial.showAsColumnInTables)
    var inputRequired by mutableStateOf(initial.inputRequired)

    var unmatchedChoices by mutableStateOf(emptySet<UserfieldChoice>())
        private set

    override val hasUnmatchedChoices: Boolean
        get() = unmatchedChoices.isNotEmpty()

    fun setUnmatched(choice: UserfieldChoice, unmatched: Boolean) {
        unmatchedChoices = if (unmatched) unmatchedChoices + choice else unmatchedChoices - choice
    }

    override fun values() = UserfieldValues(
        entity = entity,
        name = name.text.toString(),
        caption = caption.text.toString(),
        sortNumber = sortNumber.text.toString(),
        type = type,
        config = config.text.toString(),
        defaultValue = defaultValue,
        showAsColumnInTables = showAsColumnInTables,
        inputRequired = inputRequired,
    )
}

/**
 * Grocy's userfield form: edits the userfield [id] or creates one, for [entity] when it comes
 * from the userfields of an object.
 */
internal class UserfieldViewModel(
    id: Int?,
    private val entity: String?,
    repository: MasterDataRepository,
) : ObjectFormViewModel<UserfieldValues, UserfieldFormState>(id, repository) {

    /** The entities Grocy offers, those of the userentities included. */
    val entities: StateFlow<List<String>> = repository.userentities
        .map { list -> userfieldEntities(list.map(Userentity::name)) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, userfieldEntities(emptyList()))

    init {
        start()
    }

    override suspend fun createForm(id: Int?): UserfieldFormState? {
        if (id == null) return UserfieldFormState(UserfieldValues(entity = entity))
        val userfield = repository.allUserfields.first().find { it.id == id } ?: return null
        return UserfieldFormState(userfield.toValues())
    }

    override fun validate(form: UserfieldFormState) = form.values().validate()

    override suspend fun save(
        id: Int,
        form: UserfieldFormState,
        userfields: Map<String, String?>,
    ): Int = repository.saveUserfield(form.values().toUserfield(id))
}

private val FIELD_NAME = Regex("^[a-zA-Z0-9_]+$")
