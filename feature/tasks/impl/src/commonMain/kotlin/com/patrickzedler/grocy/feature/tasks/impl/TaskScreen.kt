package com.patrickzedler.grocy.feature.tasks.impl

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.design.component.DropdownOption
import com.patrickzedler.grocy.core.design.component.FloatingToolbarIconButton
import com.patrickzedler.grocy.core.design.component.LocalFormFocus
import com.patrickzedler.grocy.core.design.component.segmentedGroup
import com.patrickzedler.grocy.core.forms.DateTextItem
import com.patrickzedler.grocy.core.forms.DropdownItem
import com.patrickzedler.grocy.core.forms.FormFrame
import com.patrickzedler.grocy.core.forms.GroupItems
import com.patrickzedler.grocy.core.forms.ObjectField
import com.patrickzedler.grocy.core.forms.ObjectFormScreen
import com.patrickzedler.grocy.core.forms.TextItem
import com.patrickzedler.grocy.core.forms.fieldErrorText
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.chores_assigned_to
import com.patrickzedler.grocy.core.resources.ic_rounded_add
import com.patrickzedler.grocy.core.resources.ic_rounded_edit_attributes
import com.patrickzedler.grocy.core.resources.product_description
import com.patrickzedler.grocy.core.resources.product_name
import com.patrickzedler.grocy.core.resources.product_name_required
import com.patrickzedler.grocy.core.resources.products_configure_userfields
import com.patrickzedler.grocy.core.resources.task_category
import com.patrickzedler.grocy.core.resources.task_create
import com.patrickzedler.grocy.core.resources.task_due
import com.patrickzedler.grocy.core.resources.task_edit
import com.patrickzedler.grocy.core.resources.task_save_add_another
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Grocy's task form: name, description, due date, category and who it is assigned to, its
 * userfields below. Saving is the toolbar's FAB, "Save & add another task" beside it for new tasks.
 */
@Composable
internal fun TaskScreen(
    viewModel: TaskFormViewModel,
    frame: FormFrame,
    onConfigureUserfieldsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val options by viewModel.options.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    ObjectFormScreen(
        title = stringResource(
            if (viewModel.isNew) Res.string.task_create else Res.string.task_edit,
        ),
        viewModel = viewModel,
        frame = frame,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
        toolbarActions = {
            if (viewModel.isNew) {
                val formFocus = LocalFormFocus.current
                FloatingToolbarIconButton(
                    painter = painterResource(Res.drawable.ic_rounded_add),
                    contentDescription = stringResource(Res.string.task_save_add_another),
                    onClick = { viewModel.saveAndAddAnother { formFocus?.submit() } },
                )
            }
            FloatingToolbarIconButton(
                painter = painterResource(Res.drawable.ic_rounded_edit_attributes),
                contentDescription = stringResource(Res.string.products_configure_userfields),
                onClick = onConfigureUserfieldsClick,
            )
        },
    ) { form ->
        val errors = viewModel.errors
        // A form started over is a new one, with its first field focused again
        segmentedGroup(key = "general-${viewModel.generation}") {
            val categories = taskCategoryChoices(options.categories, form.initialCategoryId)
            GroupItems(
                { index, count ->
                    TextItem(
                        state = form.name,
                        autoFocus = viewModel.isNew,
                        label = stringResource(Res.string.product_name),
                        error = fieldErrorText(
                            errors[ObjectField.NAME],
                            Res.string.product_name_required,
                        ),
                        index = index,
                        count = count,
                    )
                },
                { index, count ->
                    TextItem(
                        state = form.description,
                        label = stringResource(Res.string.product_description),
                        multiLine = true,
                        index = index,
                        count = count,
                    )
                },
                { index, count ->
                    DateTextItem(
                        state = form.dueDate,
                        label = stringResource(Res.string.task_due),
                        error = fieldErrorText(errors[ObjectField.DUE_DATE]),
                        index = index,
                        count = count,
                    )
                },
                { index, count ->
                    DropdownItem(
                        label = stringResource(Res.string.task_category),
                        options = categories.map { DropdownOption(it.id, it.name) },
                        selectedId = form.categoryId,
                        onSelectedIdChange = { form.categoryId = it },
                        onUnmatchedChange = { form.isCategoryUnmatched = it },
                        optional = true,
                        isLastField = !viewModel.hasUserfields && options.users.isEmpty(),
                        index = index,
                        count = count,
                    )
                },
                if (options.users.isNotEmpty()) {
                    { index, count ->
                        DropdownItem(
                            label = stringResource(Res.string.chores_assigned_to),
                            options = options.users.map { DropdownOption(it.id, it.displayName) },
                            selectedId = form.assignedUserId,
                            onSelectedIdChange = { form.assignedUserId = it },
                            onUnmatchedChange = { form.isUserUnmatched = it },
                            optional = true,
                            isLastField = !viewModel.hasUserfields,
                            index = index,
                            count = count,
                        )
                    }
                } else {
                    null
                },
            )
        }
    }
}
