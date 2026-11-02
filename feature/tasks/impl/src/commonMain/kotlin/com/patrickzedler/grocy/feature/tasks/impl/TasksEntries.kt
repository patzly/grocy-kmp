package com.patrickzedler.grocy.feature.tasks.impl

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.design.component.DetailPlaceholder
import com.patrickzedler.grocy.core.forms.ObjectActionsViewModel
import com.patrickzedler.grocy.core.forms.formFrame
import com.patrickzedler.grocy.core.navigation.Navigator
import com.patrickzedler.grocy.core.navigation.detailPaneMetadata
import com.patrickzedler.grocy.core.navigation.listPaneMetadata
import com.patrickzedler.grocy.core.navigation.showsDetailBesideList
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.ic_rounded_checklist
import com.patrickzedler.grocy.core.resources.tasks_detail_hint
import com.patrickzedler.grocy.core.ui.appMenuItems
import com.patrickzedler.grocy.feature.masterdata.api.TaskCategoriesRoute
import com.patrickzedler.grocy.feature.masterdata.api.UserfieldsRoute
import com.patrickzedler.grocy.feature.settings.api.GrocySettingsPage
import com.patrickzedler.grocy.feature.settings.api.GrocySettingsPageRoute
import com.patrickzedler.grocy.feature.tasks.api.TaskRoute
import com.patrickzedler.grocy.feature.tasks.api.TasksRoute
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

// The list and the form share one list-detail scene
private const val TASKS_SCENE = "tasks"

/** Grocy's tasks: the list with a task's form beside it where the window is wide enough. */
fun EntryProviderScope<NavKey>.tasksEntries(
    dependencies: TasksDependencies,
    navigator: Navigator,
) {
    val masterData = dependencies.masterDataRepository
    val tasks = dependencies.tasksRepository

    entry<TasksRoute>(
        metadata = listPaneMetadata(TASKS_SCENE) {
            DetailPlaceholder(
                icon = painterResource(Res.drawable.ic_rounded_checklist),
                text = stringResource(Res.string.tasks_detail_hint),
            )
        },
    ) { route ->
        val currentRoute = navigator.currentRoute
        TasksScreen(
            viewModel = viewModel {
                TasksViewModel(masterData, tasks, dependencies.tasksOverviewOptions)
            },
            objectActions = viewModel {
                ObjectActionsViewModel(MasterDataRepository.TASKS, masterData)
            },
            links = TasksLinks(
                onCreateClick = { navigator.openDetail(TaskRoute()) },
                onTaskClick = { navigator.openDetail(TaskRoute(it)) },
                // Categories and settings live in scenes of their own
                onCategoriesClick = { navigator.navigate(TaskCategoriesRoute) },
                onSettingsClick = {
                    navigator.navigate(GrocySettingsPageRoute(GrocySettingsPage.TASKS))
                },
                onTaskDeleted = { id ->
                    if ((navigator.currentRoute as? TaskRoute)?.id == id) navigator.goBack()
                },
            ),
            menuItems = appMenuItems(),
            selectedTaskId = selectedTaskOf(currentRoute),
            isBesideDetail = showsDetailBesideList(),
            isShown = isInTasksScene(currentRoute),
            onBackClick = { navigator.goBack(from = route) },
        )
    }

    entry<TaskRoute>(
        metadata = { route ->
            if (route.inList) detailPaneMetadata(TASKS_SCENE) else emptyMap()
        },
    ) { route ->
        TaskScreen(
            viewModel = viewModel { TaskFormViewModel(route.id, masterData, tasks) },
            frame = formFrame(
                navigator = navigator,
                route = route,
                entity = MasterDataRepository.TASKS,
                repository = masterData,
                inList = route.inList,
            ) { TaskRoute(it) },
            onConfigureUserfieldsClick = {
                navigator.navigate(UserfieldsRoute(MasterDataRepository.TASKS))
            },
        )
    }
}

/** The task the list marks while [route] shows it beside the list. */
internal fun selectedTaskOf(route: NavKey): Int? = (route as? TaskRoute)?.id

/** Whether [route] shows the list, also beside it, not a page on top of it. */
internal fun isInTasksScene(route: NavKey): Boolean =
    route == TasksRoute || (route is TaskRoute && route.inList)
