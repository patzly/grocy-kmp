package com.patrickzedler.grocy.feature.tasks.api

import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.core.navigation.DetailRoute
import com.patrickzedler.grocy.core.navigation.Route
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/** Grocy's "Tasks", the list pane of a list-detail scene of its own. */
@Serializable
@SerialName("tasks")
data object TasksRoute : Route

/**
 * Grocy's form of task [id], of a new task without one. [inList]: beside the tasks where the
 * window is wide enough; without, a screen of its own, e.g. what the start screen opens in narrow
 * windows.
 */
@Serializable
@SerialName("task")
data class TaskRoute(
    val id: Int? = null,
    val inList: Boolean = true,
) : DetailRoute

val tasksRouteSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(TasksRoute::class, TasksRoute.serializer())
        subclass(TaskRoute::class, TaskRoute.serializer())
    }
}
