package com.patrickzedler.grocy.feature.recipes.impl.actions

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.recipes.RecipesRepository
import com.patrickzedler.grocy.core.forms.ChangeError
import com.patrickzedler.grocy.core.forms.FieldError
import com.patrickzedler.grocy.core.forms.changeErrorOf
import com.patrickzedler.grocy.core.forms.minimumAmount
import com.patrickzedler.grocy.core.forms.numberError
import com.patrickzedler.grocy.core.model.recipes.MealPlanSection
import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.core.model.text.isoDateOf
import com.patrickzedler.grocy.core.model.text.parseDecimal
import com.patrickzedler.grocy.core.model.text.todayEpochDay
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** The actions of Grocy's recipe menus, for their messages. */
internal enum class RecipeAction { COPY, DELETE, MEAL_PLAN, PRINT }

/** What became of an action of [RecipeActionsViewModel], shown or acted on once. */
internal sealed interface RecipeActionEvent {
    /** The copy, which opens like Grocy does. */
    data class Copied(val id: Int) : RecipeActionEvent

    data class Deleted(val id: Int) : RecipeActionEvent

    data object AddedToMealPlan : RecipeActionEvent

    data object Printed : RecipeActionEvent

    data class Failed(val action: RecipeAction, val error: ChangeError) : RecipeActionEvent
}

/** The inputs of Grocy's meal plan dialog that can be invalid. */
internal enum class MealPlanField { DAY, SERVINGS }

/** Grocy's "Add meal plan entry" of recipe [recipeId]; the day starts as today. */
@Stable
internal class MealPlanDialogState(
    val recipeId: Int,
    val recipeName: String,
    today: Long,
    val decimals: Int,
) {
    val day = TextFieldState(isoDateOf(today))
    val servings = TextFieldState("1")

    /** The sections once downloaded; the default one without name always exists. */
    var sections by mutableStateOf(listOf(MealPlanSection(DEFAULT_SECTION_ID, "", null)))
    var sectionId by mutableStateOf(DEFAULT_SECTION_ID)
    var isSectionUnmatched by mutableStateOf(false)
    var errors by mutableStateOf<Map<MealPlanField, FieldError>>(emptyMap())
    var isSaving by mutableStateOf(false)
    var error by mutableStateOf<ChangeError?>(null)

    companion object {
        /** Grocy's internal section of entries without one. */
        const val DEFAULT_SECTION_ID = -1
    }
}

internal sealed interface RecipeActionDialog {
    /** Grocy's question before deleting, naming the recipe. */
    data class Delete(val id: Int, val name: String) : RecipeActionDialog

    class MealPlan(val state: MealPlanDialogState) : RecipeActionDialog
}

/** Grocy's checks of its meal plan dialog: a day as "YYYY-MM-DD", servings above zero. */
internal fun validateMealPlan(day: String, servings: String, decimals: Int) = buildMap {
    when {
        day.isBlank() -> put(MealPlanField.DAY, FieldError.Required)
        epochDayOf(day.trim()) == null -> put(MealPlanField.DAY, FieldError.InvalidDate)
    }
    numberError(servings, minimumAmount(decimals), decimals, required = true)?.let {
        put(MealPlanField.SERVINGS, it)
    }
}

/**
 * The actions of Grocy's recipe menus on the list and the recipe page: copy, delete (asks
 * first), add to the meal plan (its dialog in [dialog]) and print the grocycode, online only.
 * Results arrive as [events].
 */
internal class RecipeActionsViewModel(
    private val recipes: RecipesRepository,
    private val masterData: MasterDataRepository,
    private val today: () -> Long = ::todayEpochDay,
) : ViewModel() {

    var dialog by mutableStateOf<RecipeActionDialog?>(null)
        private set

    var isRunning by mutableStateOf(false)
        private set

    private val eventChannel = Channel<RecipeActionEvent>(Channel.BUFFERED)
    val events: Flow<RecipeActionEvent> = eventChannel.receiveAsFlow()

    fun copy(id: Int) = run(RecipeAction.COPY) { RecipeActionEvent.Copied(recipes.copyRecipe(id)) }

    fun requestDelete(id: Int) {
        if (isRunning) return
        viewModelScope.launch {
            val name = recipes.recipe(id).first()?.name ?: return@launch
            dialog = RecipeActionDialog.Delete(id, name)
        }
    }

    fun delete(id: Int) = run(RecipeAction.DELETE) {
        recipes.deleteRecipe(id)
        RecipeActionEvent.Deleted(id)
    }

    fun print(id: Int) = run(RecipeAction.PRINT) {
        recipes.printLabel(id)
        RecipeActionEvent.Printed
    }

    /** Opens Grocy's meal plan dialog for recipe [id] and downloads the sections for it. */
    fun openMealPlan(id: Int) {
        if (isRunning) return
        viewModelScope.launch {
            val name = recipes.recipe(id).first()?.name ?: return@launch
            val decimals = masterData.userSettings.first().stockDecimalPlacesAmounts
            val state = MealPlanDialogState(id, name, today(), decimals)
            dialog = RecipeActionDialog.MealPlan(state)
            val error = changeErrorOf {
                val sections = recipes.mealPlanSections()
                if (sections.isNotEmpty()) state.sections = sections
            }
            // Saving needs the server too, the dialog says so right away
            state.error = error
        }
    }

    /** Saves the open meal plan dialog; it closes once the server took the entry. */
    fun saveMealPlan() {
        val state = (dialog as? RecipeActionDialog.MealPlan)?.state ?: return
        if (state.isSaving) return
        val day = state.day.text.toString().trim()
        val servingsText = state.servings.text.toString()
        state.errors = validateMealPlan(day, servingsText, state.decimals)
        val servings = parseDecimal(servingsText)
        if (state.errors.isNotEmpty() || state.isSectionUnmatched || servings == null) return
        state.isSaving = true
        state.error = null
        viewModelScope.launch {
            val error = changeErrorOf {
                recipes.addToMealPlan(state.recipeId, day, servings, state.sectionId)
            }
            state.isSaving = false
            if (error == null) {
                if ((dialog as? RecipeActionDialog.MealPlan)?.state === state) dialog = null
                eventChannel.send(RecipeActionEvent.AddedToMealPlan)
            } else {
                state.error = error
            }
        }
    }

    fun dismissDialog() {
        val mealPlan = (dialog as? RecipeActionDialog.MealPlan)?.state
        if (mealPlan?.isSaving != true) dialog = null
    }

    private fun run(action: RecipeAction, block: suspend () -> RecipeActionEvent) {
        if (isRunning) return
        dialog = null
        isRunning = true
        viewModelScope.launch {
            var event: RecipeActionEvent? = null
            val error = changeErrorOf { event = block() }
            isRunning = false
            eventChannel.send(event ?: RecipeActionEvent.Failed(action, requireNotNull(error)))
        }
    }
}
