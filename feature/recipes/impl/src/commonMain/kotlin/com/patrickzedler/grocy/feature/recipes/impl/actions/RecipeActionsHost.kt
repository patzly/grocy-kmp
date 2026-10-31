package com.patrickzedler.grocy.feature.recipes.impl.actions

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.text.input.KeyboardType
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.DropdownOption
import com.patrickzedler.grocy.core.design.component.NumberStepper
import com.patrickzedler.grocy.core.design.component.SegmentedGroup
import com.patrickzedler.grocy.core.forms.DatePickerButton
import com.patrickzedler.grocy.core.forms.DeleteDialog
import com.patrickzedler.grocy.core.forms.DropdownItem
import com.patrickzedler.grocy.core.forms.FormDialog
import com.patrickzedler.grocy.core.forms.GroupItems
import com.patrickzedler.grocy.core.forms.TextItem
import com.patrickzedler.grocy.core.forms.actionMessage
import com.patrickzedler.grocy.core.forms.fieldErrorText
import com.patrickzedler.grocy.core.forms.saveMessage
import com.patrickzedler.grocy.core.model.recipes.recipeGrocycode
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.productcard.rememberGrocycodeDownload
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_copy
import com.patrickzedler.grocy.core.resources.action_delete
import com.patrickzedler.grocy.core.resources.data_none
import com.patrickzedler.grocy.core.resources.data_save_failed
import com.patrickzedler.grocy.core.resources.grocycode_download_of
import com.patrickzedler.grocy.core.resources.grocycode_print_failed
import com.patrickzedler.grocy.core.resources.grocycode_print_of
import com.patrickzedler.grocy.core.resources.grocycode_printed
import com.patrickzedler.grocy.core.resources.grocycode_recipe
import com.patrickzedler.grocy.core.resources.meal_plan_add_entry
import com.patrickzedler.grocy.core.resources.meal_plan_added
import com.patrickzedler.grocy.core.resources.meal_plan_day
import com.patrickzedler.grocy.core.resources.meal_plan_day_required
import com.patrickzedler.grocy.core.resources.meal_plan_section
import com.patrickzedler.grocy.core.resources.meal_plan_servings
import com.patrickzedler.grocy.core.resources.objects_delete_failed
import com.patrickzedler.grocy.core.resources.recipe_edit
import com.patrickzedler.grocy.core.resources.recipe_ingredient_of
import com.patrickzedler.grocy.core.resources.recipes_add_to_meal_plan
import com.patrickzedler.grocy.core.resources.recipes_delete_question
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * The menu of a recipe like Grocy's: edit; add to the meal plan, copy and delete; the grocycode
 * as download and, where the server prints labels, on the label printer.
 */
@Composable
internal fun recipeMenuGroups(
    recipeId: Int,
    config: ServerConfig,
    actions: RecipeActionsViewModel,
    onEditClick: (recipeId: Int) -> Unit,
    onGrocycodeDownload: (recipeId: Int) -> Unit,
): List<List<AppBarMenuItem>> {
    val recipe = stringResource(Res.string.grocycode_recipe)
    return listOf(
        listOf(AppBarMenuItem(stringResource(Res.string.recipe_edit)) { onEditClick(recipeId) }),
        listOfNotNull(
            AppBarMenuItem(stringResource(Res.string.recipes_add_to_meal_plan)) {
                actions.openMealPlan(recipeId)
            }.takeIf { config.recipesMealPlan },
            AppBarMenuItem(stringResource(Res.string.action_copy)) { actions.copy(recipeId) },
            AppBarMenuItem(stringResource(Res.string.action_delete)) {
                actions.requestDelete(recipeId)
            },
        ),
        listOfNotNull(
            AppBarMenuItem(stringResource(Res.string.grocycode_download_of, recipe)) {
                onGrocycodeDownload(recipeId)
            },
            AppBarMenuItem(stringResource(Res.string.grocycode_print_of, recipe)) {
                actions.print(recipeId)
            }.takeIf { config.labelPrinter && config.labelPrinterRunsOnServer },
        ),
    )
}

/** Saves recipe grocycodes as PNG like Grocy's download; failures show in [snackbarHostState]. */
@Composable
internal fun rememberRecipeGrocycodeDownload(
    snackbarHostState: SnackbarHostState,
    twoDimensional: () -> Boolean,
): (recipeId: Int) -> Unit {
    val download = rememberGrocycodeDownload(snackbarHostState)
    val currentTwoDimensional by rememberUpdatedState(twoDimensional)
    return remember(download) {
        { id -> download(recipeGrocycode(id), currentTwoDimensional()) }
    }
}

/**
 * The questions and dialogs of [actions] and their messages in [snackbarHostState]; [onDone]
 * gets the copies and deletions, e.g. to open the copy like Grocy does.
 */
@Composable
internal fun RecipeActionsHost(
    actions: RecipeActionsViewModel,
    snackbarHostState: SnackbarHostState,
    onDone: (RecipeActionEvent) -> Unit,
) {
    val currentOnDone by rememberUpdatedState(onDone)

    LaunchedEffect(actions) {
        actions.events.collect { event ->
            when (event) {
                RecipeActionEvent.AddedToMealPlan ->
                    snackbarHostState.showSnackbar(getString(Res.string.meal_plan_added))
                RecipeActionEvent.Printed ->
                    snackbarHostState.showSnackbar(getString(Res.string.grocycode_printed))
                is RecipeActionEvent.Failed ->
                    snackbarHostState.showSnackbar(event.error.actionMessage(event.action.failure))
                is RecipeActionEvent.Copied, is RecipeActionEvent.Deleted -> currentOnDone(event)
            }
        }
    }

    when (val dialog = actions.dialog) {
        is RecipeActionDialog.Delete -> DeleteDialog(
            question = stringResource(Res.string.recipes_delete_question, dialog.name),
            hint = null,
            onDelete = { actions.delete(dialog.id) },
            onDismissRequest = actions::dismissDialog,
        )
        is RecipeActionDialog.MealPlan -> MealPlanDialog(
            state = dialog.state,
            onSave = actions::saveMealPlan,
            onDismissRequest = actions::dismissDialog,
        )
        null -> Unit
    }
}

// Grocy explains failed copies with its saving error, a name that exists already
private val RecipeAction.failure: StringResource
    get() = when (this) {
        RecipeAction.COPY, RecipeAction.MEAL_PLAN -> Res.string.data_save_failed
        RecipeAction.DELETE -> Res.string.objects_delete_failed
        RecipeAction.PRINT -> Res.string.grocycode_print_failed
    }

/** Grocy's "Add meal plan entry" of a recipe: day, servings and section. */
@Composable
private fun MealPlanDialog(
    state: MealPlanDialogState,
    onSave: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val errors = state.errors
    val none = stringResource(Res.string.data_none)

    FormDialog(
        title = stringResource(Res.string.meal_plan_add_entry),
        subtitle = stringResource(Res.string.recipe_ingredient_of, state.recipeName),
        isSaving = state.isSaving,
        error = state.error?.saveMessage(),
        onSave = onSave,
        onDismissRequest = onDismissRequest,
    ) {
        SegmentedGroup {
            GroupItems(
                { index, count ->
                    TextItem(
                        state = state.day,
                        label = stringResource(Res.string.meal_plan_day),
                        error = fieldErrorText(
                            errors[MealPlanField.DAY],
                            Res.string.meal_plan_day_required,
                        ),
                        trailingAction = { DatePickerButton(state.day, withTime = false) },
                        index = index,
                        count = count,
                    )
                },
                { index, count ->
                    TextItem(
                        state = state.servings,
                        label = stringResource(Res.string.meal_plan_servings),
                        error = fieldErrorText(errors[MealPlanField.SERVINGS]),
                        keyboardType = KeyboardType.Decimal,
                        stepper = NumberStepper.Positive,
                        index = index,
                        count = count,
                    )
                },
                { index, count ->
                    DropdownItem(
                        label = stringResource(Res.string.meal_plan_section),
                        // Grocy's default section has no name
                        options = state.sections.map {
                            DropdownOption(it.id, it.name.ifBlank { none })
                        },
                        selectedId = state.sectionId,
                        onSelectedIdChange = {
                            state.sectionId = it ?: MealPlanDialogState.DEFAULT_SECTION_ID
                        },
                        onUnmatchedChange = { state.isSectionUnmatched = it },
                        index = index,
                        count = count,
                    )
                },
            )
        }
    }
}
