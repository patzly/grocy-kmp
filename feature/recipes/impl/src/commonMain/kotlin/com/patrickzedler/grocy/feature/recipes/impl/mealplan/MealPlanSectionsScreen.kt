package com.patrickzedler.grocy.feature.recipes.impl.mealplan

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.FloatingToolbarIconButton
import com.patrickzedler.grocy.core.design.component.FloatingToolbarWithFab
import com.patrickzedler.grocy.core.design.component.ListItemOverflowMenu
import com.patrickzedler.grocy.core.design.component.NavigateBackButton
import com.patrickzedler.grocy.core.design.component.SegmentedListScreen
import com.patrickzedler.grocy.core.design.component.SupportingLines
import com.patrickzedler.grocy.core.design.component.insetItems
import com.patrickzedler.grocy.core.design.component.showNewestSnackbar
import com.patrickzedler.grocy.core.forms.DeleteDialog
import com.patrickzedler.grocy.core.forms.ListDetailItem
import com.patrickzedler.grocy.core.forms.actionMessage
import com.patrickzedler.grocy.core.forms.emptyListItem
import com.patrickzedler.grocy.core.forms.labelled
import com.patrickzedler.grocy.core.model.recipes.MealPlanSection
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_add
import com.patrickzedler.grocy.core.resources.action_delete
import com.patrickzedler.grocy.core.resources.action_scroll_to_top
import com.patrickzedler.grocy.core.resources.ic_rounded_add
import com.patrickzedler.grocy.core.resources.ic_rounded_vertical_align_top
import com.patrickzedler.grocy.core.resources.meal_plan_section_delete_question
import com.patrickzedler.grocy.core.resources.meal_plan_section_edit
import com.patrickzedler.grocy.core.resources.meal_plan_section_time
import com.patrickzedler.grocy.core.resources.meal_plan_sections_empty
import com.patrickzedler.grocy.core.resources.meal_plan_sections_title
import com.patrickzedler.grocy.core.resources.objects_delete_failed
import com.patrickzedler.grocy.core.resources.objects_sort_number_value
import com.patrickzedler.grocy.core.ui.RefreshErrorEffect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Grocy's "Meal plan sections" like its table: name, sort number and time per section in
 * their order; a tap edits, the menu deletes after Grocy's question, "Add" creates one.
 */
@Composable
internal fun MealPlanSectionsScreen(
    viewModel: MealPlanSectionsViewModel,
    menuItems: List<AppBarMenuItem>,
    onBackClick: () -> Unit,
) {
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val refresh by viewModel.refresh.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val current = sections

    RefreshErrorEffect(
        error = refresh.error,
        hasData = !current.isNullOrEmpty(),
        snackbarHostState = snackbarHostState,
        onErrorShown = viewModel::onErrorShown,
    )
    LaunchedEffect(viewModel) {
        viewModel.failures.collectLatest { error ->
            snackbarHostState.showNewestSnackbar(
                error.actionMessage(Res.string.objects_delete_failed),
            )
        }
    }
    when (val dialog = viewModel.dialog) {
        is MealPlanSectionsDialog.Edit -> MealPlanSectionDialog(
            state = dialog.state,
            onSave = viewModel::save,
            onDismissRequest = viewModel::dismissDialog,
        )
        is MealPlanSectionsDialog.Delete -> DeleteDialog(
            question = stringResource(
                Res.string.meal_plan_section_delete_question,
                dialog.section.name,
            ),
            hint = null,
            onDelete = { viewModel.delete(dialog.section) },
            onDismissRequest = viewModel::dismissDialog,
        )
        null -> Unit
    }

    SegmentedListScreen(
        title = stringResource(Res.string.meal_plan_sections_title),
        navigationIcon = { NavigateBackButton(onClick = onBackClick) },
        menuItems = menuItems,
        listState = listState,
        snackbarHostState = snackbarHostState,
        isRefreshing = refresh.isRefreshing && !current.isNullOrEmpty(),
        onRefresh = viewModel::refresh,
        itemSpacing = ListItemDefaults.SegmentedGap,
        floatingToolbar = { scrollBehavior, toolbarModifier ->
            FloatingToolbarWithFab(
                scrollBehavior = scrollBehavior,
                modifier = toolbarModifier,
                fabPainter = painterResource(Res.drawable.ic_rounded_add),
                fabDescription = stringResource(Res.string.action_add),
                onFabClick = viewModel::add,
            ) {
                FloatingToolbarIconButton(
                    painter = painterResource(Res.drawable.ic_rounded_vertical_align_top),
                    contentDescription = stringResource(Res.string.action_scroll_to_top),
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                    enabled = listState.canScrollBackward,
                )
            }
        },
    ) {
        if (current == null) return@SegmentedListScreen
        if (current.isEmpty()) {
            emptyListItem(
                hasData = false,
                storedByStart = true,
                refresh = refresh,
                emptyText = { stringResource(Res.string.meal_plan_sections_empty) },
                filteredEmptyText = { stringResource(Res.string.meal_plan_sections_empty) },
                onRetryClick = viewModel::refresh,
            )
            return@SegmentedListScreen
        }
        insetItems(
            items = current.withIndex().toList(),
            key = { it.value.id },
            contentType = { "section" },
            animate = true,
        ) { (index, section) ->
            SectionRow(
                section = section,
                index = index,
                count = current.size,
                onClick = { viewModel.edit(section) },
                onDeleteClick = { viewModel.requestDelete(section) },
            )
        }
    }
}

/** A row of Grocy's sections table: the name, then sort number and time. */
@Composable
private fun SectionRow(
    section: MealPlanSection,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val supporting = listOfNotNull(
        section.sortNumber?.let {
            stringResource(Res.string.objects_sort_number_value, it.toString())
        },
        section.timeInfo?.let { labelled(stringResource(Res.string.meal_plan_section_time), it) },
    )
    ListDetailItem(
        selected = null,
        onClick = onClick,
        index = index,
        count = count,
        modifier = Modifier.fillMaxWidth(),
        supportingContent = supporting.takeIf { it.isNotEmpty() }?.let {
            { SupportingLines(it) }
        },
        trailingContent = {
            ListItemOverflowMenu(
                groups = listOf(
                    listOf(
                        AppBarMenuItem(
                            stringResource(Res.string.meal_plan_section_edit),
                            onClick = onClick,
                        ),
                        AppBarMenuItem(
                            stringResource(Res.string.action_delete),
                            onClick = onDeleteClick,
                        ),
                    ),
                ),
            )
        },
    ) {
        Text(section.name)
    }
}
