package com.patrickzedler.grocy.feature.tasks.impl

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import com.patrickzedler.grocy.core.data.tasks.TasksRepository
import com.patrickzedler.grocy.core.forms.ChangeError
import com.patrickzedler.grocy.core.forms.actionMessage
import com.patrickzedler.grocy.core.forms.changeErrorOf
import com.patrickzedler.grocy.core.model.tasks.Task
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_undo
import com.patrickzedler.grocy.core.resources.task_change_failed
import com.patrickzedler.grocy.core.resources.task_completed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/** What an action on a task did, for Grocy's messages. */
internal sealed interface TaskEvent {
    data class Completed(val taskId: Int, val taskName: String, val doneTime: String) : TaskEvent

    data class Failed(val error: ChangeError) : TaskEvent
}

/**
 * Grocy's buttons of a task's row: "Mark task as completed" with the current time and "Undo task".
 * Both are online only.
 */
@Stable
internal class TaskActions(
    private val scope: CoroutineScope,
    private val repository: TasksRepository,
    private val now: () -> String,
) {
    private val eventChannel = Channel<TaskEvent>(Channel.BUFFERED)
    val events: Flow<TaskEvent> = eventChannel.receiveAsFlow()

    /** Tasks with a change under way; a second tap on the same task waits for it. */
    private val changing = mutableSetOf<Int>()

    fun complete(task: Task) {
        if (!changing.add(task.id)) return
        val doneTime = now()
        scope.launch {
            val error = changeErrorOf { repository.complete(task.id, doneTime) }
            changing -= task.id
            eventChannel.send(
                error?.let(TaskEvent::Failed)
                    ?: TaskEvent.Completed(task.id, task.name, doneTime),
            )
        }
    }

    fun undo(taskId: Int) {
        if (!changing.add(taskId)) return
        scope.launch {
            val error = changeErrorOf { repository.undo(taskId) }
            changing -= taskId
            error?.let { eventChannel.send(TaskEvent.Failed(it)) }
        }
    }
}

/**
 * The messages of [actions] in [snackbarHostState]; Grocy's "Marked task %s as completed on %s"
 * with "Undo", which Grocy has not: a swipe is easily done by mistake.
 */
@Composable
internal fun TaskActionsHost(actions: TaskActions, snackbarHostState: SnackbarHostState) {
    LaunchedEffect(actions) {
        actions.events.collect { event ->
            when (event) {
                is TaskEvent.Completed -> {
                    val result = snackbarHostState.showSnackbar(
                        message = getString(
                            Res.string.task_completed,
                            event.taskName,
                            taskDateTimeText(event.doneTime),
                        ),
                        actionLabel = getString(Res.string.action_undo),
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) actions.undo(event.taskId)
                }
                is TaskEvent.Failed -> snackbarHostState.showSnackbar(
                    event.error.actionMessage(Res.string.task_change_failed),
                )
            }
        }
    }
}
