package com.patrickzedler.grocy.feature.recipes.impl.mealplan

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.recipes.MealPlanRepository
import com.patrickzedler.grocy.core.data.util.RefreshController
import com.patrickzedler.grocy.core.data.util.RefreshState
import com.patrickzedler.grocy.core.forms.ChangeError
import com.patrickzedler.grocy.core.forms.changeErrorOf
import com.patrickzedler.grocy.core.model.recipes.MealPlanSection
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal sealed interface MealPlanSectionsDialog {
    class Edit(val state: MealPlanSectionDialogState) : MealPlanSectionsDialog

    /** Grocy's question before deleting, naming the section. */
    data class Delete(val section: MealPlanSection) : MealPlanSectionsDialog
}

/**
 * Grocy's "Meal plan sections": the sections in their order without Grocy's internal default
 * one, created and edited in its form as dialog, deleted after its question. Online only.
 */
internal class MealPlanSectionsViewModel(private val mealPlan: MealPlanRepository) : ViewModel() {

    val sections: StateFlow<List<MealPlanSection>?> = mealPlan.sections
        .map { list -> list.filter { it.id != MealPlanSection.DEFAULT_ID } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    private val refreshController = RefreshController(viewModelScope) { mealPlan.refresh() }
    val refresh: StateFlow<RefreshState> = refreshController.state

    var dialog by mutableStateOf<MealPlanSectionsDialog?>(null)
        private set

    private val failureChannel = Channel<ChangeError>(Channel.BUFFERED)

    /** Deletions the server refused. */
    val failures: Flow<ChangeError> = failureChannel.receiveAsFlow()

    init {
        refresh()
    }

    fun refresh() = refreshController.refresh()

    fun onErrorShown() = refreshController.onErrorShown()

    fun add() {
        dialog = MealPlanSectionsDialog.Edit(MealPlanSectionDialogState(original = null))
    }

    fun edit(section: MealPlanSection) {
        dialog = MealPlanSectionsDialog.Edit(MealPlanSectionDialogState(section))
    }

    /** Saves the open form; it closes once the server took the section. */
    fun save() {
        val state = (dialog as? MealPlanSectionsDialog.Edit)?.state ?: return
        if (state.isSaving) return
        val section = state.validated() ?: return
        state.isSaving = true
        state.error = null
        viewModelScope.launch {
            val error = changeErrorOf { mealPlan.saveSection(section) }
            state.isSaving = false
            if (error == null) {
                if ((dialog as? MealPlanSectionsDialog.Edit)?.state === state) dialog = null
            } else {
                state.error = error
            }
        }
    }

    fun requestDelete(section: MealPlanSection) {
        dialog = MealPlanSectionsDialog.Delete(section)
    }

    fun delete(section: MealPlanSection) {
        dialog = null
        viewModelScope.launch {
            changeErrorOf { mealPlan.deleteSection(section.id) }?.let { failureChannel.send(it) }
        }
    }

    fun dismissDialog() {
        val editing = (dialog as? MealPlanSectionsDialog.Edit)?.state
        if (editing?.isSaving != true) dialog = null
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
