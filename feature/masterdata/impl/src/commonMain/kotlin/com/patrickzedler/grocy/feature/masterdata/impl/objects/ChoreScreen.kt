package com.patrickzedler.grocy.feature.masterdata.impl.objects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.design.component.DropdownOption
import com.patrickzedler.grocy.core.design.component.FloatingToolbarIconButton
import com.patrickzedler.grocy.core.design.component.NumberStepper
import com.patrickzedler.grocy.core.design.component.SegmentedContainer
import com.patrickzedler.grocy.core.design.component.SegmentedSwitchItem
import com.patrickzedler.grocy.core.design.component.SegmentedTexts
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.segmentedGroup
import com.patrickzedler.grocy.core.model.masterdata.ChoreAssignmentType
import com.patrickzedler.grocy.core.model.masterdata.ChorePeriodType
import com.patrickzedler.grocy.core.model.masterdata.Weekday
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.chore_assign_to
import com.patrickzedler.grocy.core.resources.chore_assign_to_required
import com.patrickzedler.grocy.core.resources.chore_assignment_info_in_alphabetical_order
import com.patrickzedler.grocy.core.resources.chore_assignment_info_none
import com.patrickzedler.grocy.core.resources.chore_assignment_info_random
import com.patrickzedler.grocy.core.resources.chore_assignment_info_who_least_did_first
import com.patrickzedler.grocy.core.resources.chore_assignment_type
import com.patrickzedler.grocy.core.resources.chore_consume_product
import com.patrickzedler.grocy.core.resources.chore_create
import com.patrickzedler.grocy.core.resources.chore_day_of_month
import com.patrickzedler.grocy.core.resources.chore_edit
import com.patrickzedler.grocy.core.resources.chore_friday
import com.patrickzedler.grocy.core.resources.chore_group_assignment
import com.patrickzedler.grocy.core.resources.chore_group_product
import com.patrickzedler.grocy.core.resources.chore_group_schedule
import com.patrickzedler.grocy.core.resources.chore_monday
import com.patrickzedler.grocy.core.resources.chore_period_interval
import com.patrickzedler.grocy.core.resources.chore_period_type
import com.patrickzedler.grocy.core.resources.chore_product
import com.patrickzedler.grocy.core.resources.chore_product_amount
import com.patrickzedler.grocy.core.resources.chore_rollover
import com.patrickzedler.grocy.core.resources.chore_rollover_hint
import com.patrickzedler.grocy.core.resources.chore_saturday
import com.patrickzedler.grocy.core.resources.chore_schedule_adaptive
import com.patrickzedler.grocy.core.resources.chore_schedule_daily
import com.patrickzedler.grocy.core.resources.chore_schedule_hourly
import com.patrickzedler.grocy.core.resources.chore_schedule_manually
import com.patrickzedler.grocy.core.resources.chore_schedule_monthly
import com.patrickzedler.grocy.core.resources.chore_schedule_weekly
import com.patrickzedler.grocy.core.resources.chore_schedule_yearly
import com.patrickzedler.grocy.core.resources.chore_start_date
import com.patrickzedler.grocy.core.resources.chore_start_date_hint
import com.patrickzedler.grocy.core.resources.chore_start_date_required
import com.patrickzedler.grocy.core.resources.chore_sunday
import com.patrickzedler.grocy.core.resources.chore_thursday
import com.patrickzedler.grocy.core.resources.chore_track_date_only
import com.patrickzedler.grocy.core.resources.chore_track_date_only_hint
import com.patrickzedler.grocy.core.resources.chore_tuesday
import com.patrickzedler.grocy.core.resources.chore_users_unavailable
import com.patrickzedler.grocy.core.resources.chore_wednesday
import com.patrickzedler.grocy.core.resources.chore_weekdays
import com.patrickzedler.grocy.core.resources.ic_rounded_check
import com.patrickzedler.grocy.core.resources.ic_rounded_edit_attributes
import com.patrickzedler.grocy.core.resources.product_active
import com.patrickzedler.grocy.core.resources.product_description
import com.patrickzedler.grocy.core.resources.product_name
import com.patrickzedler.grocy.core.resources.product_name_required
import com.patrickzedler.grocy.core.resources.products_configure_userfields
import com.patrickzedler.grocy.feature.masterdata.impl.product.DatePickerButton
import com.patrickzedler.grocy.feature.masterdata.impl.product.DropdownItem
import com.patrickzedler.grocy.feature.masterdata.impl.product.GroupItems
import com.patrickzedler.grocy.feature.masterdata.impl.product.TextItem
import com.patrickzedler.grocy.feature.masterdata.impl.product.fieldErrorText
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Grocy's chore form: schedule, assignment and the product an execution consumes. */
@Composable
internal fun ChoreScreen(
    viewModel: ChoreViewModel,
    frame: FormFrame,
    onConfigureUserfieldsClick: (entity: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options by viewModel.options.collectAsStateWithLifecycle()

    ObjectFormScreen(
        title = stringResource(
            if (viewModel.isNew) Res.string.chore_create else Res.string.chore_edit,
        ),
        viewModel = viewModel,
        frame = frame,
        modifier = modifier,
        toolbarActions = {
            FloatingToolbarIconButton(
                painter = painterResource(Res.drawable.ic_rounded_edit_attributes),
                contentDescription = stringResource(Res.string.products_configure_userfields),
                onClick = { onConfigureUserfieldsClick(MasterDataRepository.CHORES) },
            )
        },
    ) { form ->
        val errors = viewModel.errors
        val features = viewModel.features

        segmentedGroup(key = "general") {
            GroupItems(
                { index, count ->
                    TextItem(
                        state = form.name,
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
            )
        }

        segmentedGroup(key = "schedule", title = Res.string.chore_group_schedule) {
            val periodType = form.periodType
            GroupItems(
                { index, count ->
                    DropdownItem(
                        label = stringResource(Res.string.chore_period_type),
                        options = ChorePeriodType.entries.map {
                            DropdownOption(it.ordinal, stringResource(it.label))
                        },
                        selectedId = periodType.ordinal,
                        onSelectedIdChange = { id ->
                            id?.let { form.periodType = ChorePeriodType.entries[it] }
                        },
                        onUnmatchedChange = { form.setUnmatched(ChoreChoice.PERIOD_TYPE, it) },
                        hint = scheduleInfo(periodType, form.periodInterval.text.toString()),
                        index = index,
                        count = count,
                    )
                },
                if (periodType == ChorePeriodType.MONTHLY) {
                    { index, count ->
                        TextItem(
                            state = form.periodDays,
                            label = stringResource(Res.string.chore_day_of_month),
                            error = fieldErrorText(errors[ObjectField.PERIOD_DAYS]),
                            keyboardType = KeyboardType.Number,
                            stepper = NumberStepper(min = 1.0, max = 31.0),
                            index = index,
                            count = count,
                        )
                    }
                } else {
                    null
                },
                if (periodType == ChorePeriodType.WEEKLY) {
                    { index, count ->
                        ChipsItem(
                            label = stringResource(Res.string.chore_weekdays),
                            options = Weekday.entries,
                            selected = form.weekdays,
                            optionLabel = { stringResource(it.label) },
                            onSelectedChange = { form.weekdays = it },
                            index = index,
                            count = count,
                        )
                    }
                } else {
                    null
                },
                if (periodType.hasInterval) {
                    { index, count ->
                        TextItem(
                            state = form.periodInterval,
                            label = stringResource(Res.string.chore_period_interval),
                            error = fieldErrorText(errors[ObjectField.PERIOD_INTERVAL]),
                            keyboardType = KeyboardType.Number,
                            stepper = NumberStepper(min = 1.0),
                            index = index,
                            count = count,
                        )
                    }
                } else {
                    null
                },
                { index, count ->
                    TextItem(
                        state = form.startDate,
                        label = stringResource(Res.string.chore_start_date),
                        hint = "YYYY-MM-DD HH:MM:SS\n" +
                            stringResource(Res.string.chore_start_date_hint),
                        error = fieldErrorText(
                            errors[ObjectField.START_DATE],
                            Res.string.chore_start_date_required,
                        ),
                        index = index,
                        count = count,
                        trailingAction = {
                            DatePickerButton(form.startDate, withTime = !form.trackDateOnly)
                        },
                    )
                },
                { index, count ->
                    SegmentedSwitchItem(
                        headline = stringResource(Res.string.chore_track_date_only),
                        supporting = stringResource(Res.string.chore_track_date_only_hint),
                        checked = form.trackDateOnly,
                        onCheckedChange = { form.trackDateOnly = it },
                        index = index,
                        count = count,
                    )
                },
                { index, count ->
                    SegmentedSwitchItem(
                        headline = stringResource(Res.string.chore_rollover),
                        supporting = stringResource(Res.string.chore_rollover_hint),
                        checked = form.rollover,
                        onCheckedChange = { form.rollover = it },
                        index = index,
                        count = count,
                    )
                },
            )
        }

        if (features.assignments) {
            segmentedGroup(key = "assignment", title = Res.string.chore_group_assignment) {
                val assignmentType = form.assignmentType
                val users = options.users
                GroupItems(
                    { index, count ->
                        DropdownItem(
                            label = stringResource(Res.string.chore_assignment_type),
                            options = ChoreAssignmentType.entries.map {
                                DropdownOption(it.ordinal, stringResource(it.label))
                            },
                            selectedId = assignmentType.ordinal,
                            onSelectedIdChange = { id ->
                                id?.let { form.assignmentType = ChoreAssignmentType.entries[it] }
                            },
                            onUnmatchedChange = {
                                form.setUnmatched(ChoreChoice.ASSIGNMENT_TYPE, it)
                            },
                            hint = stringResource(assignmentType.info),
                            index = index,
                            count = count,
                        )
                    },
                    if (assignmentType != ChoreAssignmentType.NO_ASSIGNMENT) {
                        { index, count ->
                            ChipsItem(
                                label = stringResource(Res.string.chore_assign_to),
                                options = users.map { it.id },
                                selected = form.assignedUserIds,
                                optionLabel = { id ->
                                    users.firstOrNull { it.id == id }?.displayName.orEmpty()
                                },
                                onSelectedChange = { form.assignedUserIds = it },
                                hint = stringResource(Res.string.chore_users_unavailable)
                                    .takeIf { users.isEmpty() },
                                error = errors[ObjectField.ASSIGNED_USERS]?.let {
                                    stringResource(Res.string.chore_assign_to_required)
                                },
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

        if (features.stock) {
            segmentedGroup(key = "product", title = Res.string.chore_group_product) {
                val consume = form.consumeProduct
                val product = options.products.firstOrNull { it.id == form.productId }
                val stockUnit = options.quantityUnits.firstOrNull { it.id == product?.quIdStock }
                GroupItems(
                    { index, count ->
                        SegmentedSwitchItem(
                            headline = stringResource(Res.string.chore_consume_product),
                            checked = consume,
                            onCheckedChange = { form.consumeProduct = it },
                            index = index,
                            count = count,
                        )
                    },
                    { index, count ->
                        DropdownItem(
                            label = stringResource(Res.string.chore_product),
                            // Active products; a disabled one stays visible while chosen
                            options = options.products
                                .filter { it.active || it.id == form.productId }
                                .map { DropdownOption(it.id, it.name) },
                            selectedId = form.productId,
                            onSelectedIdChange = { form.productId = it },
                            onUnmatchedChange = { form.setUnmatched(ChoreChoice.PRODUCT, it) },
                            optional = true,
                            enabled = consume,
                            index = index,
                            count = count,
                        )
                    },
                    { index, count ->
                        TextItem(
                            state = form.productAmount,
                            label = stringResource(Res.string.chore_product_amount),
                            error = fieldErrorText(errors[ObjectField.PRODUCT_AMOUNT]),
                            suffix = stockUnit?.let { it.namePlural?.ifBlank { null } ?: it.name },
                            enabled = consume,
                            keyboardType = KeyboardType.Decimal,
                            stepper = NumberStepper.Positive,
                            index = index,
                            count = count,
                        )
                    },
                )
            }
        }
    }
}

/** Grocy's explanation of when the next execution of a chore of [type] is scheduled. */
@Composable
private fun scheduleInfo(type: ChorePeriodType, intervalText: String): String {
    val interval = wholeNumber(intervalText, min = 1) ?: 1
    return when (type) {
        ChorePeriodType.MANUALLY -> stringResource(Res.string.chore_schedule_manually)
        ChorePeriodType.HOURLY ->
            pluralStringResource(Res.plurals.chore_schedule_hourly, interval, interval)
        ChorePeriodType.DAILY ->
            pluralStringResource(Res.plurals.chore_schedule_daily, interval, interval)
        ChorePeriodType.WEEKLY ->
            pluralStringResource(Res.plurals.chore_schedule_weekly, interval, interval)
        ChorePeriodType.MONTHLY ->
            pluralStringResource(Res.plurals.chore_schedule_monthly, interval, interval)
        ChorePeriodType.YEARLY ->
            pluralStringResource(Res.plurals.chore_schedule_yearly, interval, interval)
        ChorePeriodType.ADAPTIVE -> stringResource(Res.string.chore_schedule_adaptive)
    }
}

/** Several choices at once as filter chips below a headline, like Grocy's checkboxes. */
@Composable
private fun <T> ChipsItem(
    label: String,
    options: List<T>,
    selected: Set<T>,
    optionLabel: @Composable (T) -> String,
    onSelectedChange: (Set<T>) -> Unit,
    index: Int,
    count: Int,
    hint: String? = null,
    error: String? = null,
) {
    val haptic = LocalHapticFeedback.current

    SegmentedContainer(index = index, count = count) {
        SegmentedTexts(headline = label, supporting = hint)
        if (options.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { option ->
                    val isSelected = option in selected
                    FilterChip(
                        selected = isSelected,
                        modifier = Modifier.clickCursor(),
                        onClick = {
                            haptic.confirm()
                            onSelectedChange(
                                if (isSelected) selected - option else selected + option,
                            )
                        },
                        label = { Text(optionLabel(option)) },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_rounded_check),
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        }
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

private val Weekday.label: StringResource
    get() = when (this) {
        Weekday.MONDAY -> Res.string.chore_monday
        Weekday.TUESDAY -> Res.string.chore_tuesday
        Weekday.WEDNESDAY -> Res.string.chore_wednesday
        Weekday.THURSDAY -> Res.string.chore_thursday
        Weekday.FRIDAY -> Res.string.chore_friday
        Weekday.SATURDAY -> Res.string.chore_saturday
        Weekday.SUNDAY -> Res.string.chore_sunday
    }

private val ChoreAssignmentType.info: StringResource
    get() = when (this) {
        ChoreAssignmentType.IN_ALPHABETICAL_ORDER ->
            Res.string.chore_assignment_info_in_alphabetical_order
        ChoreAssignmentType.NO_ASSIGNMENT -> Res.string.chore_assignment_info_none
        ChoreAssignmentType.RANDOM -> Res.string.chore_assignment_info_random
        ChoreAssignmentType.WHO_LEAST_DID_FIRST ->
            Res.string.chore_assignment_info_who_least_did_first
    }
