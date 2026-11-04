package com.patrickzedler.grocy.feature.recipes.impl.mealplan

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.KeyboardType
import com.patrickzedler.grocy.core.design.component.DropdownOption
import com.patrickzedler.grocy.core.design.component.NumberStepper
import com.patrickzedler.grocy.core.design.component.SegmentedGroup
import com.patrickzedler.grocy.core.forms.DateTextItem
import com.patrickzedler.grocy.core.forms.DeleteDialog
import com.patrickzedler.grocy.core.forms.DropdownItem
import com.patrickzedler.grocy.core.forms.FormDialog
import com.patrickzedler.grocy.core.forms.GroupItem
import com.patrickzedler.grocy.core.forms.GroupItems
import com.patrickzedler.grocy.core.forms.TextItem
import com.patrickzedler.grocy.core.forms.TimePickerButton
import com.patrickzedler.grocy.core.forms.fieldErrorText
import com.patrickzedler.grocy.core.forms.saveMessage
import com.patrickzedler.grocy.core.model.recipes.MealPlanEntryType
import com.patrickzedler.grocy.core.model.recipes.MealPlanSection
import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.core.model.text.formatDate
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_copy
import com.patrickzedler.grocy.core.resources.booking_product_required
import com.patrickzedler.grocy.core.resources.booking_unit_required
import com.patrickzedler.grocy.core.resources.data_amount
import com.patrickzedler.grocy.core.resources.data_none
import com.patrickzedler.grocy.core.resources.data_note
import com.patrickzedler.grocy.core.resources.data_product
import com.patrickzedler.grocy.core.resources.data_quantity_unit
import com.patrickzedler.grocy.core.resources.meal_plan_add_entry
import com.patrickzedler.grocy.core.resources.meal_plan_consume_week_question
import com.patrickzedler.grocy.core.resources.meal_plan_copy_day_title
import com.patrickzedler.grocy.core.resources.meal_plan_day
import com.patrickzedler.grocy.core.resources.meal_plan_day_required
import com.patrickzedler.grocy.core.resources.meal_plan_edit_entry
import com.patrickzedler.grocy.core.resources.meal_plan_section
import com.patrickzedler.grocy.core.resources.meal_plan_section_create
import com.patrickzedler.grocy.core.resources.meal_plan_section_edit
import com.patrickzedler.grocy.core.resources.meal_plan_section_sort_hint
import com.patrickzedler.grocy.core.resources.meal_plan_section_time
import com.patrickzedler.grocy.core.resources.meal_plan_servings
import com.patrickzedler.grocy.core.resources.objects_column_sort_number
import com.patrickzedler.grocy.core.resources.product_name
import com.patrickzedler.grocy.core.resources.product_name_required
import com.patrickzedler.grocy.core.resources.recipe_consume_confirm
import com.patrickzedler.grocy.core.resources.recipe_consume_hint
import com.patrickzedler.grocy.core.resources.recipe_nesting_recipe
import com.patrickzedler.grocy.core.resources.recipe_nesting_recipe_required
import com.patrickzedler.grocy.feature.recipes.impl.recipe.ConsumeDialog
import org.jetbrains.compose.resources.stringResource

/** The open dialog of the meal plan and what its buttons do. */
@Composable
internal fun MealPlanDialogHost(viewModel: MealPlanViewModel) {
    when (val dialog = viewModel.dialog) {
        is MealPlanDialog.Entry -> MealPlanEntryDialog(
            state = dialog.state,
            onSave = viewModel::saveEntry,
            onDismissRequest = viewModel::dismissDialog,
        )
        is MealPlanDialog.CopyDay -> CopyDayDialog(
            state = dialog.state,
            onCopy = viewModel::copyDay,
            onDismissRequest = viewModel::dismissDialog,
        )
        is MealPlanDialog.ConsumeRecipe -> ConsumeDialog(
            recipeName = dialog.item.name,
            onConsume = { viewModel.consumeRecipe(dialog.item) },
            onDismissRequest = viewModel::dismissDialog,
        )
        is MealPlanDialog.ConsumeWeek -> DeleteDialog(
            question = stringResource(Res.string.meal_plan_consume_week_question),
            hint = stringResource(Res.string.recipe_consume_hint),
            onDelete = { viewModel.consumeWeek(dialog.weekRecipeId) },
            onDismissRequest = viewModel::dismissDialog,
            confirmText = stringResource(Res.string.recipe_consume_confirm),
        )
        null -> Unit
    }
}

/**
 * Grocy's dialogs of a meal plan entry, the type ("Recipe", "Product", "Note") below the title
 * like Grocy names it: the day, then a recipe and its servings, a product and its amount in
 * one of its units or a note, then the section.
 */
@Composable
private fun MealPlanEntryDialog(
    state: MealPlanEntryDialogState,
    onSave: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val errors = state.errors
    FormDialog(
        title = stringResource(
            if (state.isNew) Res.string.meal_plan_add_entry else Res.string.meal_plan_edit_entry,
        ),
        subtitle = stringResource(
            when (state.type) {
                MealPlanEntryType.RECIPE -> Res.string.recipe_nesting_recipe
                MealPlanEntryType.PRODUCT -> Res.string.data_product
                MealPlanEntryType.NOTE -> Res.string.data_note
            },
        ),
        isSaving = state.isSaving,
        error = state.error?.saveMessage(),
        onSave = onSave,
        onDismissRequest = onDismissRequest,
    ) {
        // One group whatever the type, a note alone would leave groups of single items
        SegmentedGroup {
            val day: GroupItem = { index, count ->
                DateTextItem(
                    state = state.day,
                    label = stringResource(Res.string.meal_plan_day),
                    error = fieldErrorText(
                        errors[MealPlanEntryField.DAY],
                        Res.string.meal_plan_day_required,
                    ),
                    index = index,
                    count = count,
                )
            }
            val section: GroupItem = { index, count ->
                SectionItem(
                    sections = state.choices.sections,
                    selectedId = state.sectionId,
                    onSelectedIdChange = { state.sectionId = it ?: MealPlanSection.DEFAULT_ID },
                    onUnmatchedChange = { state.isSectionUnmatched = it },
                    index = index,
                    count = count,
                )
            }
            val typeItems = when (state.type) {
                MealPlanEntryType.RECIPE -> recipeItems(state)
                MealPlanEntryType.PRODUCT -> productItems(state)
                MealPlanEntryType.NOTE -> listOf(noteItem(state))
            }
            GroupItems(day, *typeItems.toTypedArray(), section)
        }
    }
}

/** The recipe, which brings its servings like Grocy's picker, and the servings. */
private fun recipeItems(state: MealPlanEntryDialogState): List<GroupItem> = listOf(
    { index, count ->
        DropdownItem(
            label = stringResource(Res.string.recipe_nesting_recipe),
            autoFocus = state.isNew,
            options = state.choices.recipes.map { DropdownOption(it.id, it.name) },
            selectedId = state.recipeId,
            onSelectedIdChange = state::selectRecipe,
            onUnmatchedChange = { state.isRecipeUnmatched = it },
            error = fieldErrorText(
                state.errors[MealPlanEntryField.RECIPE],
                Res.string.recipe_nesting_recipe_required,
            ),
            index = index,
            count = count,
        )
    },
    { index, count ->
        TextItem(
            state = state.servings,
            autoFocus = !state.isNew,
            label = stringResource(Res.string.meal_plan_servings),
            error = fieldErrorText(state.errors[MealPlanEntryField.SERVINGS]),
            keyboardType = KeyboardType.Decimal,
            stepper = NumberStepper.Positive,
            index = index,
            count = count,
        )
    },
)

/** The product, its amount and the unit of the amount, the stock unit at first. */
private fun productItems(state: MealPlanEntryDialogState): List<GroupItem> = listOf(
    { index, count ->
        DropdownItem(
            label = stringResource(Res.string.data_product),
            autoFocus = state.isNew,
            options = state.choices.products.map { DropdownOption(it.id, it.name) },
            selectedId = state.productId,
            onSelectedIdChange = state::selectProduct,
            onUnmatchedChange = { state.isProductUnmatched = it },
            error = fieldErrorText(
                state.errors[MealPlanEntryField.PRODUCT],
                Res.string.booking_product_required,
            ),
            index = index,
            count = count,
        )
    },
    { index, count ->
        TextItem(
            state = state.amount,
            autoFocus = !state.isNew,
            label = stringResource(Res.string.data_amount),
            error = fieldErrorText(state.errors[MealPlanEntryField.AMOUNT]),
            keyboardType = KeyboardType.Decimal,
            stepper = NumberStepper.Positive,
            index = index,
            count = count,
        )
    },
    { index, count ->
        val units = state.units
        DropdownItem(
            label = stringResource(Res.string.data_quantity_unit),
            options = units.map { DropdownOption(it.unit.id, it.unit.name) },
            selectedId = state.quId,
            onSelectedIdChange = { state.quId = it },
            onUnmatchedChange = { state.isUnitUnmatched = it },
            error = fieldErrorText(
                state.errors[MealPlanEntryField.UNIT],
                Res.string.booking_unit_required,
            ),
            // Grocy's picker is fixed while the product has only one unit
            enabled = units.size > 1,
            index = index,
            count = count,
        )
    },
)

private fun noteItem(state: MealPlanEntryDialogState): GroupItem = { index, count ->
    TextItem(
        state = state.note,
        autoFocus = true,
        label = stringResource(Res.string.data_note),
        multiLine = true,
        index = index,
        count = count,
    )
}

/** Grocy's section choice; its default section has no name, "None" here. */
@Composable
private fun SectionItem(
    sections: List<MealPlanSection>,
    selectedId: Int,
    onSelectedIdChange: (Int?) -> Unit,
    onUnmatchedChange: (Boolean) -> Unit,
    index: Int,
    count: Int,
) {
    val none = stringResource(Res.string.data_none)
    DropdownItem(
        label = stringResource(Res.string.meal_plan_section),
        isLastField = true,
        options = sections.map { section ->
            DropdownOption(section.id, sectionLabel(section) ?: none)
        },
        selectedId = selectedId,
        onSelectedIdChange = onSelectedIdChange,
        onUnmatchedChange = onUnmatchedChange,
        index = index,
        count = count,
    )
}

/** Grocy's "Copy this day": the day to copy every entry of the day to. */
@Composable
private fun CopyDayDialog(
    state: CopyDayDialogState,
    onCopy: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val from = epochDayOf(state.fromDay)?.let { formatDate(it) } ?: state.fromDay
    FormDialog(
        title = stringResource(Res.string.meal_plan_copy_day_title, from),
        isSaving = state.isSaving,
        error = state.saveError?.saveMessage(),
        onSave = onCopy,
        onDismissRequest = onDismissRequest,
        saveText = stringResource(Res.string.action_copy),
    ) {
        SegmentedGroup {
            GroupItems(
                { index, count ->
                    DateTextItem(
                        state = state.toDay,
                        autoFocus = true,
                        isLastField = true,
                        label = stringResource(Res.string.meal_plan_day),
                        error = fieldErrorText(state.error, Res.string.meal_plan_day_required),
                        index = index,
                        count = count,
                    )
                },
            )
        }
    }
}

/** Grocy's form of a meal plan section as dialog: name, sort number (with its hint) and time. */
@Composable
internal fun MealPlanSectionDialog(
    state: MealPlanSectionDialogState,
    onSave: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    FormDialog(
        title = stringResource(
            if (state.isNew) {
                Res.string.meal_plan_section_create
            } else {
                Res.string.meal_plan_section_edit
            },
        ),
        isSaving = state.isSaving,
        error = state.error?.saveMessage(),
        onSave = onSave,
        onDismissRequest = onDismissRequest,
    ) {
        SegmentedGroup {
            GroupItems(
                { index, count ->
                    TextItem(
                        state = state.name,
                        autoFocus = state.isNew,
                        label = stringResource(Res.string.product_name),
                        error = fieldErrorText(state.nameError, Res.string.product_name_required),
                        index = index,
                        count = count,
                    )
                },
                { index, count ->
                    TextItem(
                        state = state.sortNumber,
                        label = stringResource(Res.string.objects_column_sort_number),
                        hint = stringResource(Res.string.meal_plan_section_sort_hint),
                        error = fieldErrorText(state.sortNumberError),
                        keyboardType = KeyboardType.Number,
                        index = index,
                        count = count,
                    )
                },
                { index, count ->
                    TextItem(
                        state = state.timeInfo,
                        isLastField = true,
                        label = stringResource(Res.string.meal_plan_section_time),
                        trailingAction = { TimePickerButton(state.timeInfo) },
                        index = index,
                        count = count,
                    )
                },
            )
        }
    }
}
