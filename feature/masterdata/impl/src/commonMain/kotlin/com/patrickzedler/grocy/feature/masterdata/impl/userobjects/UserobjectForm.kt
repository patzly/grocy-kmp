package com.patrickzedler.grocy.feature.masterdata.impl.userobjects

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.design.component.FloatingToolbarIconButton
import com.patrickzedler.grocy.core.design.component.SegmentedContainer
import com.patrickzedler.grocy.core.design.component.insetItem
import com.patrickzedler.grocy.core.forms.FieldError
import com.patrickzedler.grocy.core.forms.FormFrame
import com.patrickzedler.grocy.core.forms.ObjectField
import com.patrickzedler.grocy.core.forms.ObjectFormLoad
import com.patrickzedler.grocy.core.forms.ObjectFormScreen
import com.patrickzedler.grocy.core.forms.ObjectFormState
import com.patrickzedler.grocy.core.forms.ObjectFormViewModel
import com.patrickzedler.grocy.core.model.masterdata.Userentity
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.ic_rounded_edit_attributes
import com.patrickzedler.grocy.core.resources.objects_configure_fields
import com.patrickzedler.grocy.core.resources.userobject_create
import com.patrickzedler.grocy.core.resources.userobject_edit
import com.patrickzedler.grocy.core.resources.userobject_no_fields
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** An object has nothing but its userfields. */
internal object UserobjectFormState : ObjectFormState<Unit> {
    override fun values() = Unit
}

/**
 * Grocy's form of an object of a userentity: only the entity's fields. Like Grocy it goes back to
 * the list once saved, also when the object is new.
 */
internal class UserobjectViewModel(
    private val entityName: String,
    id: Int?,
    repository: MasterDataRepository,
) : ObjectFormViewModel<Unit, UserobjectFormState>(id, repository) {

    /** The entity's caption for the title, known once the form is loaded. */
    var userentity by mutableStateOf<Userentity?>(null)
        private set

    override val userfieldEntity: String = MasterDataRepository.USERENTITY_PREFIX + entityName

    override val closesWhenCreated: Boolean = true

    init {
        start()
    }

    override suspend fun createForm(id: Int?): UserobjectFormState? {
        userentity = repository.userentities.first().firstOrNull { it.name == entityName }
        val entity = userentity ?: return null
        if (id == null) return UserobjectFormState
        return UserobjectFormState.takeIf {
            repository.userobjects(entity.id).first().any { it.id == id }
        }
    }

    override suspend fun userfieldValues(id: Int): Map<String, String?> {
        val entity = userentity ?: return emptyMap()
        return repository.userobjects(entity.id).first().firstOrNull { it.id == id }
            ?.userfields.orEmpty()
    }

    override suspend fun refreshForUnknownObject() {
        repository.refreshUserobjects(entityName)
    }

    override fun validate(form: UserobjectFormState) = emptyMap<ObjectField, FieldError>()

    override suspend fun save(
        id: Int,
        form: UserobjectFormState,
        userfields: Map<String, String?>,
    ): Int = repository.saveUserobject(requireNotNull(userentity), id, userfields)
}

@Composable
internal fun UserobjectScreen(
    viewModel: UserobjectViewModel,
    frame: FormFrame,
    onConfigureFieldsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val caption = viewModel.userentity?.caption.orEmpty()
    ObjectFormScreen(
        title = stringResource(
            if (viewModel.isNew) Res.string.userobject_create else Res.string.userobject_edit,
            caption,
        ),
        viewModel = viewModel,
        frame = frame,
        modifier = modifier,
        // The fields are the whole form, it needs no group title and starts in the first one
        userfieldsTitle = null,
        focusFirstUserfield = viewModel.isNew,
        toolbarActions = {
            FloatingToolbarIconButton(
                painter = painterResource(Res.drawable.ic_rounded_edit_attributes),
                contentDescription = stringResource(Res.string.objects_configure_fields),
                onClick = onConfigureFieldsClick,
            )
        },
    ) {
        if ((viewModel.load as? ObjectFormLoad.Ready)?.userfields?.isEmpty() == true) {
            insetItem(key = "noFields") {
                SegmentedContainer(index = 0, count = 1) {
                    Text(stringResource(Res.string.userobject_no_fields))
                }
            }
        }
    }
}
