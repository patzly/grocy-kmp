package com.patrickzedler.grocy.feature.masterdata.impl.objects

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.forms.FieldError
import com.patrickzedler.grocy.core.forms.ObjectField
import com.patrickzedler.grocy.core.forms.ObjectFormState
import com.patrickzedler.grocy.core.forms.ObjectFormViewModel
import com.patrickzedler.grocy.core.model.masterdata.Userentity
import kotlinx.coroutines.flow.first

/** The userentity form's inputs as typed. */
internal data class UserentityValues(
    val name: String = "",
    val caption: String = "",
    val description: String = "",
    val showInSidebarMenu: Boolean = false,
    val iconCssClass: String = "",
)

/** Checks the values like Grocy's userentity form. */
internal fun UserentityValues.validate(): Map<ObjectField, FieldError> = buildMap {
    if (!isValidFieldName(name)) put(ObjectField.NAME, FieldError.Required)
    if (caption.isBlank()) put(ObjectField.CAPTION, FieldError.Required)
}

internal fun UserentityValues.toUserentity(id: Int) = Userentity(
    id = id,
    name = name,
    caption = caption.trim(),
    description = description,
    showInSidebarMenu = showInSidebarMenu,
    iconCssClass = iconCssClass.trim(),
)

internal fun Userentity.toValues() = UserentityValues(
    name = name,
    caption = caption,
    description = description.orEmpty(),
    showInSidebarMenu = showInSidebarMenu,
    iconCssClass = iconCssClass.orEmpty(),
)

@Stable
internal class UserentityFormState(initial: UserentityValues) : ObjectFormState<UserentityValues> {
    val name = TextFieldState(initial.name)
    val caption = TextFieldState(initial.caption)
    val description = TextFieldState(initial.description)
    var showInSidebarMenu by mutableStateOf(initial.showInSidebarMenu)
    val iconCssClass = TextFieldState(initial.iconCssClass)

    override fun values() = UserentityValues(
        name = name.text.toString(),
        caption = caption.text.toString(),
        description = description.text.toString(),
        showInSidebarMenu = showInSidebarMenu,
        iconCssClass = iconCssClass.text.toString(),
    )
}

/** Grocy's userentity form; its fields are userfields of "userentity-<name>". */
internal class UserentityViewModel(
    id: Int?,
    repository: MasterDataRepository,
) : ObjectFormViewModel<UserentityValues, UserentityFormState>(id, repository) {

    /** The name the server knows, null until the userentity is saved. */
    var savedName by mutableStateOf<String?>(null)
        private set

    init {
        start()
    }

    override suspend fun createForm(id: Int?): UserentityFormState? {
        if (id == null) return UserentityFormState(UserentityValues())
        val userentity = repository.userentities.first().find { it.id == id } ?: return null
        savedName = userentity.name
        return UserentityFormState(userentity.toValues())
    }

    override fun validate(form: UserentityFormState) = form.values().validate()

    override suspend fun save(
        id: Int,
        form: UserentityFormState,
        userfields: Map<String, String?>,
    ): Int {
        val userentity = form.values().toUserentity(id)
        return repository.saveUserentity(userentity).also { savedName = userentity.name }
    }
}
