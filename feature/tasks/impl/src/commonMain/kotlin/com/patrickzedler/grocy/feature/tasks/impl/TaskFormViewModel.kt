package com.patrickzedler.grocy.feature.tasks.impl

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.tasks.TasksRepository
import com.patrickzedler.grocy.core.forms.FieldError
import com.patrickzedler.grocy.core.forms.ObjectField
import com.patrickzedler.grocy.core.forms.ObjectFormState
import com.patrickzedler.grocy.core.forms.ObjectFormViewModel
import com.patrickzedler.grocy.core.model.User
import com.patrickzedler.grocy.core.model.masterdata.TaskCategory
import com.patrickzedler.grocy.core.model.tasks.Task
import com.patrickzedler.grocy.core.model.text.normalizedDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn

/** The task form's inputs as typed. */
internal data class TaskValues(
    val name: String = "",
    val description: String = "",
    val dueDate: String = "",
    val categoryId: Int? = null,
    val assignedUserId: Int? = null,
)

/** Checks the values like Grocy's form: a name; the due date is optional, but must be one. */
internal fun TaskValues.validate(): Map<ObjectField, FieldError> = buildMap {
    if (name.isBlank()) put(ObjectField.NAME, FieldError.Required)
    val date = dueDate.trim()
    if (date.isNotEmpty() && normalizedDate(date) == null) {
        put(ObjectField.DUE_DATE, FieldError.InvalidDate)
    }
}

internal fun Task.toValues() = TaskValues(
    name = name,
    description = description.orEmpty(),
    // Grocy's form shows only the date of what older versions stored with a time
    dueDate = dueDate?.take(DATE_LENGTH).orEmpty(),
    categoryId = categoryId,
    assignedUserId = assignedToUserId,
)

/** The task as saved; whether it is done is not part of the form. */
internal fun TaskValues.toTask(id: Int) = Task(
    id = id,
    name = name.trim(),
    description = description,
    dueDate = normalizedDate(dueDate.trim()),
    categoryId = categoryId,
    assignedToUserId = assignedUserId,
)

/** What the form's pickers choose from: Grocy's active categories, and the [current] one. */
internal fun taskCategoryChoices(all: List<TaskCategory>, current: Int?): List<TaskCategory> =
    all.filter { it.active || it.id == current }

@Stable
internal class TaskFormState(initial: TaskValues) : ObjectFormState<TaskValues> {
    val name = TextFieldState(initial.name)
    val description = TextFieldState(initial.description)
    val dueDate = TextFieldState(initial.dueDate)
    var categoryId by mutableStateOf(initial.categoryId)
    var assignedUserId by mutableStateOf(initial.assignedUserId)
    var isCategoryUnmatched by mutableStateOf(false)
    var isUserUnmatched by mutableStateOf(false)

    /** The category the task had, which stays a choice even when it was disabled since. */
    val initialCategoryId = initial.categoryId

    override val hasUnmatchedChoices: Boolean
        get() = isCategoryUnmatched || isUserUnmatched

    override fun values() = TaskValues(
        name = name.text.toString(),
        description = description.text.toString(),
        dueDate = dueDate.text.toString(),
        categoryId = categoryId,
        assignedUserId = assignedUserId,
    )
}

/** What the form's pickers offer. */
internal data class TaskFormOptions(
    val categories: List<TaskCategory> = emptyList(),
    val users: List<User> = emptyList(),
)

/**
 * Grocy's task form: edits task [id] or creates one, assigned to the logged in user. A created
 * task closes the form like Grocy's "Save & close", [saveAndAddAnother] starts over instead.
 */
internal class TaskFormViewModel(
    id: Int?,
    repository: MasterDataRepository,
    private val tasks: TasksRepository,
) : ObjectFormViewModel<TaskValues, TaskFormState>(id, repository) {

    val options: StateFlow<TaskFormOptions> = combine(
        repository.taskCategories,
        repository.users,
        ::TaskFormOptions,
    ).stateIn(viewModelScope, SharingStarted.Eagerly, TaskFormOptions())

    /** Changes with every form started over, whose fields then start over as well. */
    var generation by mutableIntStateOf(0)
        private set

    private var addsAnother = false

    override val userfieldEntity: String = MasterDataRepository.TASKS

    override val closesWhenCreated: Boolean
        get() = !addsAnother

    init {
        start()
    }

    override suspend fun createForm(id: Int?): TaskFormState? {
        if (id == null) {
            return TaskFormState(TaskValues(assignedUserId = tasks.currentUserId.first()))
        }
        return tasks.tasks.first().find { it.id == id }?.let { TaskFormState(it.toValues()) }
    }

    override suspend fun refreshForUnknownObject() {
        tasks.refresh()
    }

    override suspend fun userfieldValues(id: Int): Map<String, String?> =
        tasks.tasks.first().find { it.id == id }?.userfields.orEmpty()

    override fun validate(form: TaskFormState) = form.values().validate()

    override suspend fun save(id: Int, form: TaskFormState, userfields: Map<String, String?>): Int =
        try {
            tasks.save(form.values().toTask(id), userfields)
        } catch (e: Exception) {
            // The next save is a plain one again, whatever this one was
            addsAnother = false
            throw e
        }

    override fun onCreated() {
        addsAnother = false
        generation++
        startNew()
    }

    /**
     * Grocy's "Save & add another task": [submit] saves through the form's focus, which leads to
     * the first invalid field when it refuses.
     */
    fun saveAndAddAnother(submit: () -> Unit) {
        addsAnother = true
        submit()
        if (!isSaving) addsAnother = false
    }
}

private const val DATE_LENGTH = 10
