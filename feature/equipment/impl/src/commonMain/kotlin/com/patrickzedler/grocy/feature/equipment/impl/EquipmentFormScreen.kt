@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.patrickzedler.grocy.feature.equipment.impl

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import com.patrickzedler.grocy.core.data.masterdata.PictureChange
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.FloatingToolbarIconButton
import com.patrickzedler.grocy.core.design.component.LeadingTrailingContentWrapper
import com.patrickzedler.grocy.core.design.component.ListItemOverflowMenu
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.segmentedGroup
import com.patrickzedler.grocy.core.forms.FormFrame
import com.patrickzedler.grocy.core.forms.GroupItems
import com.patrickzedler.grocy.core.forms.ObjectField
import com.patrickzedler.grocy.core.forms.ObjectFormScreen
import com.patrickzedler.grocy.core.forms.TextItem
import com.patrickzedler.grocy.core.forms.fieldErrorText
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_delete
import com.patrickzedler.grocy.core.resources.equipment_choose_file
import com.patrickzedler.grocy.core.resources.equipment_create
import com.patrickzedler.grocy.core.resources.equipment_edit
import com.patrickzedler.grocy.core.resources.equipment_manual
import com.patrickzedler.grocy.core.resources.equipment_manual_none
import com.patrickzedler.grocy.core.resources.equipment_manual_will_delete
import com.patrickzedler.grocy.core.resources.equipment_notes
import com.patrickzedler.grocy.core.resources.ic_rounded_description
import com.patrickzedler.grocy.core.resources.ic_rounded_edit_attributes
import com.patrickzedler.grocy.core.resources.product_name
import com.patrickzedler.grocy.core.resources.product_name_required
import com.patrickzedler.grocy.core.resources.products_configure_userfields
import com.patrickzedler.grocy.core.ui.rememberFilePicker
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Grocy's equipment form: name and notes, its userfields below and the instruction manual: a tap
 * chooses a PDF, the item's menu deletes the current one, both when the form is saved like Grocy.
 */
@Composable
internal fun EquipmentFormScreen(
    viewModel: EquipmentFormViewModel,
    frame: FormFrame,
    onConfigureUserfieldsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    ObjectFormScreen(
        title = stringResource(
            if (viewModel.isNew) Res.string.equipment_create else Res.string.equipment_edit,
        ),
        viewModel = viewModel,
        frame = frame,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
        toolbarActions = {
            FloatingToolbarIconButton(
                painter = painterResource(Res.drawable.ic_rounded_edit_attributes),
                contentDescription = stringResource(Res.string.products_configure_userfields),
                onClick = onConfigureUserfieldsClick,
            )
        },
    ) { form ->
        val errors = viewModel.errors
        segmentedGroup(key = "general") {
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
                        state = form.notes,
                        label = stringResource(Res.string.equipment_notes),
                        multiLine = true,
                        index = index,
                        count = count,
                    )
                },
            )
        }
        segmentedGroup(key = "manual", title = Res.string.equipment_manual) {
            ManualItem(form, viewModel.savedManual)
        }
    }
}

@Composable
private fun ManualItem(form: EquipmentFormState, savedManual: String?) {
    val haptic = LocalHapticFeedback.current
    val pick = rememberFilePicker(mimeType = "application/pdf") { file ->
        form.manual = PictureChange.Replace(file.bytes, file.fileName)
    }
    val change = form.manual
    val text = when {
        change is PictureChange.Replace -> change.fileName
        change == PictureChange.Delete -> stringResource(Res.string.equipment_manual_will_delete)
        else -> savedManual ?: stringResource(Res.string.equipment_manual_none)
    }
    val canDelete = change is PictureChange.Replace ||
        (change == PictureChange.Keep && savedManual != null)

    SegmentedListItem(
        onClick = {
            haptic.confirm()
            pick()
        },
        modifier = Modifier.fillMaxWidth().clickCursor(),
        shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
        colors = SegmentedListDefaults.colors(),
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = {
            LeadingTrailingContentWrapper {
                Icon(
                    painterResource(Res.drawable.ic_rounded_description),
                    contentDescription = null,
                )
            }
        },
        supportingContent = { Text(text) },
        trailingContent = if (canDelete) {
            {
                val deleteText = stringResource(Res.string.action_delete)
                ListItemOverflowMenu(
                    groups = listOf(
                        listOf(
                            AppBarMenuItem(deleteText) {
                                // A file only chosen yet leaves what is stored as it was
                                form.manual = if (change is PictureChange.Replace) {
                                    PictureChange.Keep
                                } else {
                                    PictureChange.Delete
                                }
                            },
                        ),
                    ),
                )
            }
        } else {
            null
        },
    ) {
        Text(stringResource(Res.string.equipment_choose_file))
    }
}
