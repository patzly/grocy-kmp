package com.patrickzedler.grocy.feature.equipment.impl

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.patrickzedler.grocy.core.data.equipment.EquipmentRepository
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.masterdata.PictureChange
import com.patrickzedler.grocy.core.forms.FieldError
import com.patrickzedler.grocy.core.forms.ObjectField
import com.patrickzedler.grocy.core.forms.ObjectFormState
import com.patrickzedler.grocy.core.forms.ObjectFormViewModel
import com.patrickzedler.grocy.core.forms.htmlToPlainText
import com.patrickzedler.grocy.core.forms.plainTextToHtml
import com.patrickzedler.grocy.core.model.equipment.Equipment
import kotlinx.coroutines.flow.first

/** The form's inputs as typed. */
internal data class EquipmentValues(
    val name: String = "",
    val notes: String = "",
    val manual: PictureChange = PictureChange.Keep,
)

/** Checks the values like Grocy's form: a name. */
internal fun EquipmentValues.validate(): Map<ObjectField, FieldError> = buildMap {
    if (name.isBlank()) put(ObjectField.NAME, FieldError.Required)
}

internal fun Equipment.toValues() =
    EquipmentValues(name = name, notes = htmlToPlainText(description.orEmpty()))

/**
 * The piece the form saves as [id]; unchanged notes keep their HTML (the form edits plain text),
 * the manual stays until the repository was told otherwise.
 */
internal fun EquipmentValues.toEquipment(id: Int, original: Equipment?): Equipment {
    val keepsNotes =
        original != null && htmlToPlainText(original.description.orEmpty()) == notes.trim()
    return Equipment(
        id = id,
        name = name.trim(),
        description = if (keepsNotes) original.description else plainTextToHtml(notes),
        instructionManualFileName = original?.instructionManualFileName,
    )
}

@Stable
internal class EquipmentFormState(initial: EquipmentValues) : ObjectFormState<EquipmentValues> {
    val name = TextFieldState(initial.name)
    val notes = TextFieldState(initial.notes)
    var manual by mutableStateOf(initial.manual)

    override fun values() = EquipmentValues(
        name = name.text.toString(),
        notes = notes.text.toString(),
        manual = manual,
    )
}

/**
 * Grocy's equipment form: edits piece [id] or creates one, with its instruction manual, which
 * changes when the form is saved. A created piece closes the form like Grocy's.
 */
internal class EquipmentFormViewModel(
    id: Int?,
    repository: MasterDataRepository,
    private val equipment: EquipmentRepository,
) : ObjectFormViewModel<EquipmentValues, EquipmentFormState>(id, repository) {

    /** The stored file of the piece, what the form says about its manual. */
    var savedManual by mutableStateOf<String?>(null)
        private set

    private var original: Equipment? = null

    override val userfieldEntity: String = MasterDataRepository.EQUIPMENT

    override val closesWhenCreated: Boolean = true

    init {
        start()
    }

    override suspend fun createForm(id: Int?): EquipmentFormState? {
        if (id == null) return EquipmentFormState(EquipmentValues())
        val piece = equipment.equipment.first().find { it.id == id } ?: return null
        original = piece
        savedManual = piece.instructionManualFileName
        return EquipmentFormState(piece.toValues())
    }

    override suspend fun refreshForUnknownObject() {
        equipment.refresh()
    }

    override suspend fun userfieldValues(id: Int): Map<String, String?> =
        equipment.equipment.first().find { it.id == id }?.userfields.orEmpty()

    override fun validate(form: EquipmentFormState) = form.values().validate()

    override suspend fun save(
        id: Int,
        form: EquipmentFormState,
        userfields: Map<String, String?>,
    ): Int {
        val values = form.values()
        return equipment.save(values.toEquipment(id, original), userfields, values.manual)
    }
}
