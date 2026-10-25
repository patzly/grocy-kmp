package com.patrickzedler.grocy.feature.masterdata.impl.objects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.design.component.DropdownOption
import com.patrickzedler.grocy.core.design.component.FloatingToolbarIconButton
import com.patrickzedler.grocy.core.design.component.NumberStepper
import com.patrickzedler.grocy.core.design.component.SegmentedSwitchItem
import com.patrickzedler.grocy.core.design.component.segmentedGroup
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.data_none
import com.patrickzedler.grocy.core.resources.ic_rounded_edit_attributes
import com.patrickzedler.grocy.core.resources.objects_configure_fields
import com.patrickzedler.grocy.core.resources.product_description
import com.patrickzedler.grocy.core.resources.product_name
import com.patrickzedler.grocy.core.resources.userentity_create
import com.patrickzedler.grocy.core.resources.userentity_edit
import com.patrickzedler.grocy.core.resources.userentity_icon
import com.patrickzedler.grocy.core.resources.userentity_icon_hint
import com.patrickzedler.grocy.core.resources.userentity_show_in_sidebar
import com.patrickzedler.grocy.core.resources.userfield_caption
import com.patrickzedler.grocy.core.resources.userfield_caption_hint
import com.patrickzedler.grocy.core.resources.userfield_caption_required
import com.patrickzedler.grocy.core.resources.userfield_config
import com.patrickzedler.grocy.core.resources.userfield_config_hint
import com.patrickzedler.grocy.core.resources.userfield_create
import com.patrickzedler.grocy.core.resources.userfield_default_now
import com.patrickzedler.grocy.core.resources.userfield_default_value
import com.patrickzedler.grocy.core.resources.userfield_edit
import com.patrickzedler.grocy.core.resources.userfield_entity
import com.patrickzedler.grocy.core.resources.userfield_entity_required
import com.patrickzedler.grocy.core.resources.userfield_mandatory
import com.patrickzedler.grocy.core.resources.userfield_mandatory_hint
import com.patrickzedler.grocy.core.resources.userfield_name_hint
import com.patrickzedler.grocy.core.resources.userfield_name_invalid
import com.patrickzedler.grocy.core.resources.userfield_show_as_column
import com.patrickzedler.grocy.core.resources.userfield_sort_number
import com.patrickzedler.grocy.core.resources.userfield_sort_number_hint
import com.patrickzedler.grocy.core.resources.userfield_type
import com.patrickzedler.grocy.core.resources.userfield_type_required
import com.patrickzedler.grocy.feature.masterdata.impl.product.ChoiceItem
import com.patrickzedler.grocy.feature.masterdata.impl.product.DropdownItem
import com.patrickzedler.grocy.feature.masterdata.impl.product.GroupItems
import com.patrickzedler.grocy.feature.masterdata.impl.product.TextItem
import com.patrickzedler.grocy.feature.masterdata.impl.product.fieldErrorText
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Grocy's userfield form: where the field appears, its names, type and options. */
@Composable
internal fun UserfieldScreen(
    viewModel: UserfieldViewModel,
    frame: FormFrame,
    modifier: Modifier = Modifier,
) {
    val entities by viewModel.entities.collectAsStateWithLifecycle()

    ObjectFormScreen(
        title = stringResource(
            if (viewModel.isNew) Res.string.userfield_create else Res.string.userfield_edit,
        ),
        viewModel = viewModel,
        frame = frame,
        modifier = modifier,
    ) { form ->
        val errors = viewModel.errors
        segmentedGroup(key = "general") {
            val type = form.type
            // An entity of a link stays choosable even when it is unknown here
            val entityOptions = (entities + listOfNotNull(form.entity)).distinct().sorted()
            GroupItems(
                { index, count ->
                    DropdownItem(
                        label = stringResource(Res.string.userfield_entity),
                        options = entityOptions.mapIndexed { i, it -> DropdownOption(i, it) },
                        selectedId = entityOptions.indexOf(form.entity).takeIf { it >= 0 },
                        onSelectedIdChange = { id -> form.entity = id?.let(entityOptions::get) },
                        onUnmatchedChange = { form.setUnmatched(UserfieldChoice.ENTITY, it) },
                        error = fieldErrorText(
                            errors[ObjectField.ENTITY],
                            Res.string.userfield_entity_required,
                        ),
                        index = index,
                        count = count,
                    )
                },
                { index, count ->
                    TextItem(
                        state = form.name,
                        label = stringResource(Res.string.product_name),
                        hint = stringResource(Res.string.userfield_name_hint),
                        error = fieldErrorText(
                            errors[ObjectField.NAME],
                            Res.string.userfield_name_invalid,
                        ),
                        keyboardType = KeyboardType.Ascii,
                        index = index,
                        count = count,
                    )
                },
                { index, count ->
                    TextItem(
                        state = form.caption,
                        label = stringResource(Res.string.userfield_caption),
                        hint = stringResource(Res.string.userfield_caption_hint),
                        error = fieldErrorText(
                            errors[ObjectField.CAPTION],
                            Res.string.userfield_caption_required,
                        ),
                        index = index,
                        count = count,
                    )
                },
                { index, count ->
                    TextItem(
                        state = form.sortNumber,
                        label = stringResource(Res.string.userfield_sort_number),
                        hint = stringResource(Res.string.userfield_sort_number_hint),
                        error = fieldErrorText(errors[ObjectField.SORT_NUMBER]),
                        keyboardType = KeyboardType.Number,
                        stepper = NumberStepper(min = 0.0),
                        index = index,
                        count = count,
                    )
                },
                { index, count ->
                    DropdownItem(
                        label = stringResource(Res.string.userfield_type),
                        options = GrocyUserfieldTypes.map {
                            DropdownOption(it.ordinal, stringResource(it.label))
                        },
                        selectedId = type?.ordinal,
                        onSelectedIdChange = { id ->
                            form.type = GrocyUserfieldTypes.firstOrNull { it.ordinal == id }
                        },
                        onUnmatchedChange = { form.setUnmatched(UserfieldChoice.TYPE, it) },
                        error = fieldErrorText(
                            errors[ObjectField.TYPE],
                            Res.string.userfield_type_required,
                        ),
                        index = index,
                        count = count,
                    )
                },
                if (type?.hasOptions == true) {
                    { index, count ->
                        TextItem(
                            state = form.config,
                            label = stringResource(Res.string.userfield_config),
                            hint = stringResource(Res.string.userfield_config_hint),
                            multiLine = true,
                            index = index,
                            count = count,
                        )
                    }
                } else {
                    null
                },
                if (type?.hasDefaultValue == true) {
                    { index, count ->
                        ChoiceItem(
                            label = stringResource(Res.string.userfield_default_value),
                            options = listOf(null, USERFIELD_DEFAULT_NOW),
                            selected = form.defaultValue,
                            onSelect = { form.defaultValue = it },
                            optionLabel = {
                                stringResource(
                                    if (it == null) {
                                        Res.string.data_none
                                    } else {
                                        Res.string.userfield_default_now
                                    },
                                )
                            },
                            index = index,
                            count = count,
                        )
                    }
                } else {
                    null
                },
                { index, count ->
                    SegmentedSwitchItem(
                        headline = stringResource(Res.string.userfield_show_as_column),
                        checked = form.showAsColumnInTables,
                        onCheckedChange = { form.showAsColumnInTables = it },
                        index = index,
                        count = count,
                    )
                },
                { index, count ->
                    SegmentedSwitchItem(
                        headline = stringResource(Res.string.userfield_mandatory),
                        supporting = stringResource(Res.string.userfield_mandatory_hint),
                        checked = form.inputRequired,
                        onCheckedChange = { form.inputRequired = it },
                        index = index,
                        count = count,
                    )
                },
            )
        }
    }
}

/** Grocy's userentity form; its fields are configured as userfields of the entity. */
@Composable
internal fun UserentityScreen(
    viewModel: UserentityViewModel,
    frame: FormFrame,
    onConfigureFieldsClick: (entity: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ObjectFormScreen(
        title = stringResource(
            if (viewModel.isNew) Res.string.userentity_create else Res.string.userentity_edit,
        ),
        viewModel = viewModel,
        frame = frame,
        modifier = modifier,
        toolbarActions = {
            // The fields belong to the saved name
            val savedName = viewModel.savedName
            FloatingToolbarIconButton(
                painter = painterResource(Res.drawable.ic_rounded_edit_attributes),
                contentDescription = stringResource(Res.string.objects_configure_fields),
                onClick = {
                    savedName?.let {
                        onConfigureFieldsClick(MasterDataRepository.USERENTITY_PREFIX + it)
                    }
                },
                enabled = savedName != null,
            )
        },
    ) { form ->
        val errors = viewModel.errors
        segmentedGroup(key = "general") {
            GroupItems(
                { index, count ->
                    TextItem(
                        state = form.name,
                        label = stringResource(Res.string.product_name),
                        error = fieldErrorText(
                            errors[ObjectField.NAME],
                            Res.string.userfield_name_invalid,
                        ),
                        keyboardType = KeyboardType.Ascii,
                        index = index,
                        count = count,
                    )
                },
                { index, count ->
                    TextItem(
                        state = form.caption,
                        label = stringResource(Res.string.userfield_caption),
                        error = fieldErrorText(
                            errors[ObjectField.CAPTION],
                            Res.string.userfield_caption_required,
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
                    SegmentedSwitchItem(
                        headline = stringResource(Res.string.userentity_show_in_sidebar),
                        checked = form.showInSidebarMenu,
                        onCheckedChange = { form.showInSidebarMenu = it },
                        index = index,
                        count = count,
                    )
                },
                { index, count ->
                    TextItem(
                        state = form.iconCssClass,
                        label = stringResource(Res.string.userentity_icon),
                        hint = stringResource(Res.string.userentity_icon_hint),
                        keyboardType = KeyboardType.Ascii,
                        index = index,
                        count = count,
                    )
                },
            )
        }
    }
}
