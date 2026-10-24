package com.patrickzedler.grocy.feature.masterdata.impl.objects

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.patrickzedler.grocy.core.design.component.FloatingToolbarIconButton
import com.patrickzedler.grocy.core.design.component.SegmentedSwitchItem
import com.patrickzedler.grocy.core.design.component.segmentedGroup
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.battery_charge_interval
import com.patrickzedler.grocy.core.resources.battery_charge_interval_hint
import com.patrickzedler.grocy.core.resources.battery_create
import com.patrickzedler.grocy.core.resources.battery_edit
import com.patrickzedler.grocy.core.resources.battery_used_in
import com.patrickzedler.grocy.core.resources.ic_rounded_conversion_path
import com.patrickzedler.grocy.core.resources.ic_rounded_edit_attributes
import com.patrickzedler.grocy.core.resources.location_create
import com.patrickzedler.grocy.core.resources.location_edit
import com.patrickzedler.grocy.core.resources.location_is_freezer
import com.patrickzedler.grocy.core.resources.location_is_freezer_hint
import com.patrickzedler.grocy.core.resources.objects_default_conversions
import com.patrickzedler.grocy.core.resources.product_active
import com.patrickzedler.grocy.core.resources.product_description
import com.patrickzedler.grocy.core.resources.product_group_create
import com.patrickzedler.grocy.core.resources.product_group_edit
import com.patrickzedler.grocy.core.resources.product_group_related
import com.patrickzedler.grocy.core.resources.product_name
import com.patrickzedler.grocy.core.resources.product_name_required
import com.patrickzedler.grocy.core.resources.products_configure_userfields
import com.patrickzedler.grocy.core.resources.quantity_unit_create
import com.patrickzedler.grocy.core.resources.quantity_unit_edit
import com.patrickzedler.grocy.core.resources.quantity_unit_name
import com.patrickzedler.grocy.core.resources.quantity_unit_name_plural
import com.patrickzedler.grocy.core.resources.quantity_unit_plural_forms
import com.patrickzedler.grocy.core.resources.quantity_unit_plural_forms_hint
import com.patrickzedler.grocy.core.resources.quantity_unit_save_hint
import com.patrickzedler.grocy.core.resources.store_create
import com.patrickzedler.grocy.core.resources.store_edit
import com.patrickzedler.grocy.core.resources.task_category_create
import com.patrickzedler.grocy.core.resources.task_category_edit
import com.patrickzedler.grocy.feature.masterdata.impl.product.GroupItems
import com.patrickzedler.grocy.feature.masterdata.impl.product.NavigationItem
import com.patrickzedler.grocy.feature.masterdata.impl.product.TextItem
import com.patrickzedler.grocy.feature.masterdata.impl.product.fieldErrorText
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Grocy's form of a location, store, quantity unit, product group, battery or task category. */
@Composable
internal fun SimpleObjectScreen(
    viewModel: SimpleObjectViewModel,
    frame: FormFrame,
    onConfigureUserfieldsClick: (entity: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val kind = viewModel.kind
    val (createTitle, editTitle) = kind.formTitles

    ObjectFormScreen(
        title = stringResource(if (viewModel.isNew) createTitle else editTitle),
        viewModel = viewModel,
        frame = frame,
        modifier = modifier,
        toolbarActions = {
            kind.userfieldEntity?.let { entity ->
                FloatingToolbarIconButton(
                    painter = painterResource(Res.drawable.ic_rounded_edit_attributes),
                    contentDescription = stringResource(Res.string.products_configure_userfields),
                    onClick = { onConfigureUserfieldsClick(entity) },
                )
            }
        },
    ) { form ->
        val errors = viewModel.errors
        segmentedGroup(key = "general") {
            GroupItems(
                { index, count ->
                    TextItem(
                        state = form.name,
                        label = stringResource(
                            if (kind == ObjectListKind.QUANTITY_UNITS) {
                                Res.string.quantity_unit_name
                            } else {
                                Res.string.product_name
                            },
                        ),
                        error = fieldErrorText(
                            errors[ObjectField.NAME],
                            Res.string.product_name_required,
                        ),
                        index = index,
                        count = count,
                    )
                },
                if (kind == ObjectListKind.QUANTITY_UNITS) {
                    { index, count ->
                        TextItem(
                            state = form.namePlural,
                            label = stringResource(Res.string.quantity_unit_name_plural),
                            index = index,
                            count = count,
                        )
                    }
                } else {
                    null
                },
                if (kind == ObjectListKind.QUANTITY_UNITS && viewModel.showPluralForms) {
                    { index, count ->
                        TextItem(
                            state = form.pluralForms,
                            label = stringResource(Res.string.quantity_unit_plural_forms),
                            hint = stringResource(
                                Res.string.quantity_unit_plural_forms_hint,
                                viewModel.pluralFormCount,
                            ),
                            multiLine = true,
                            index = index,
                            count = count,
                        )
                    }
                } else {
                    null
                },
                { index, count ->
                    SegmentedSwitchItem(
                        headline = stringResource(Res.string.product_active),
                        checked = form.active,
                        onCheckedChange = { form.active = it },
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
                if (kind == ObjectListKind.LOCATIONS && viewModel.showFreezer) {
                    { index, count ->
                        SegmentedSwitchItem(
                            headline = stringResource(Res.string.location_is_freezer),
                            supporting = stringResource(Res.string.location_is_freezer_hint),
                            checked = form.isFreezer,
                            onCheckedChange = { form.isFreezer = it },
                            index = index,
                            count = count,
                        )
                    }
                } else {
                    null
                },
                if (kind == ObjectListKind.BATTERIES) {
                    { index, count ->
                        TextItem(
                            state = form.usedIn,
                            label = stringResource(Res.string.battery_used_in),
                            index = index,
                            count = count,
                        )
                    }
                } else {
                    null
                },
                if (kind == ObjectListKind.BATTERIES) {
                    { index, count ->
                        TextItem(
                            state = form.chargeIntervalDays,
                            label = stringResource(Res.string.battery_charge_interval),
                            hint = stringResource(Res.string.battery_charge_interval_hint),
                            error = fieldErrorText(errors[ObjectField.CHARGE_INTERVAL]),
                            keyboardType = KeyboardType.Number,
                            index = index,
                            count = count,
                        )
                    }
                } else {
                    null
                },
            )
        }
        if (kind == ObjectListKind.QUANTITY_UNITS) {
            segmentedGroup(key = "related", title = Res.string.product_group_related) {
                // Its screen follows with the quantity unit conversions
                NavigationItem(
                    text = stringResource(Res.string.objects_default_conversions),
                    supporting = stringResource(Res.string.quantity_unit_save_hint)
                        .takeIf { viewModel.isNew },
                    iconPainter = painterResource(Res.drawable.ic_rounded_conversion_path),
                    index = 0,
                    count = 1,
                    enabled = !viewModel.isNew,
                    onClick = {},
                )
            }
        }
    }
}

/** Titles of the form when it creates and when it edits an object of this kind. */
private val ObjectListKind.formTitles: Pair<StringResource, StringResource>
    get() = when (this) {
        ObjectListKind.LOCATIONS -> Res.string.location_create to Res.string.location_edit
        ObjectListKind.STORES -> Res.string.store_create to Res.string.store_edit
        ObjectListKind.QUANTITY_UNITS ->
            Res.string.quantity_unit_create to Res.string.quantity_unit_edit
        ObjectListKind.PRODUCT_GROUPS ->
            Res.string.product_group_create to Res.string.product_group_edit
        ObjectListKind.BATTERIES -> Res.string.battery_create to Res.string.battery_edit
        ObjectListKind.TASK_CATEGORIES ->
            Res.string.task_category_create to Res.string.task_category_edit
        else -> error("$this has a form of its own")
    }
