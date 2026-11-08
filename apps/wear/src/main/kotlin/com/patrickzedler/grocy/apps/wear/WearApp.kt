package com.patrickzedler.grocy.apps.wear

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CheckboxButton
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListSubHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListItemSync
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.ic_rounded_sync
import com.patrickzedler.grocy.core.resources.ic_rounded_sync_problem
import com.patrickzedler.grocy.core.resources.ic_rounded_tune
import com.patrickzedler.grocy.core.resources.shopping_list_group_done
import com.patrickzedler.grocy.core.resources.shopping_list_item_not_sent
import com.patrickzedler.grocy.core.resources.shopping_list_item_sending
import com.patrickzedler.grocy.core.resources.shopping_list_ungrouped
import com.patrickzedler.grocy.core.resources.shopping_mode_settings
import com.patrickzedler.grocy.core.resources.stock_overview_group_not_set
import com.patrickzedler.grocy.core.resources.wear_list_empty
import com.patrickzedler.grocy.core.wear.WearShoppingGroupTitle
import com.patrickzedler.grocy.core.wear.WearShoppingItem
import com.patrickzedler.grocy.core.wear.WearShoppingState
import com.patrickzedler.grocy.core.wear.WearState
import com.patrickzedler.grocy.core.wear.WearViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val ROUTE_HOME = "home"
private const val ROUTE_LISTS = "lists"
private const val ROUTE_OPTIONS = "options"
private const val ROUTE_MANUAL_LOGIN = "login"

/** The watch app: the login until there is one, then the shopping list to check off. */
@Composable
fun WearApp(graph: WearGraph) {
    val viewModel = viewModel {
        WearViewModel(graph.auth, graph.shoppingList, graph.sync, graph.phone)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.onStart()
        onPauseOrDispose {}
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.syncWhileShown() }
    }
    graph.localNetworkGate?.let { WearLocalNetworkPrompt(it) }

    GrocyWearTheme {
        AppScaffold {
            val navController = rememberSwipeDismissableNavController()
            SwipeDismissableNavHost(navController, startDestination = ROUTE_HOME) {
                composable(ROUTE_HOME) {
                    when (val current = state) {
                        WearState.Loading -> Loading()
                        WearState.LoggedOut -> WaitingForPhoneScreen(
                            viewModel = viewModel,
                            onManualClick = { navController.navigate(ROUTE_MANUAL_LOGIN) },
                        )
                        is WearState.LoggedIn -> ShoppingListScreen(
                            state = current.shopping,
                            onListClick = { navController.navigate(ROUTE_LISTS) },
                            onDoneChange = viewModel::setDone,
                            onOptionsClick = { navController.navigate(ROUTE_OPTIONS) },
                        )
                    }
                }
                composable(ROUTE_LISTS) {
                    ListChoiceScreen(
                        state = (state as? WearState.LoggedIn)?.shopping ?: WearShoppingState(),
                        onListClick = { id ->
                            viewModel.selectList(id)
                            navController.popBackStack()
                        },
                    )
                }
                composable(ROUTE_OPTIONS) {
                    WearOptionsScreen(
                        state = (state as? WearState.LoggedIn)?.shopping ?: WearShoppingState(),
                        isRefreshing = isRefreshing,
                        onReloadClick = viewModel::refreshNow,
                        onAutoSyncChange = viewModel::setAutoSync,
                        onDoneItemsLastChange = viewModel::setDoneItemsLast,
                        onGroupingChange = viewModel::setGrouping,
                        onStatusChange = viewModel::setStatus,
                        onSyncIntervalChange = viewModel::setSyncInterval,
                    )
                }
                composable(ROUTE_MANUAL_LOGIN) {
                    ManualLoginScreen(
                        viewModel = viewModel,
                        isLoggedIn = state is WearState.LoggedIn,
                        onDone = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}

@Composable
private fun Loading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ShoppingListScreen(
    state: WearShoppingState,
    onListClick: () -> Unit,
    onDoneChange: (Int, Boolean) -> Unit,
    onOptionsClick: () -> Unit,
) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    val haptic = LocalHapticFeedback.current
    val listName = state.lists.firstOrNull { it.id == state.listId }?.name.orEmpty()

    ScreenScaffold(
        scrollState = listState,
        edgeButton = {
            // The list syncs by itself, so the end of it belongs to its options, as on the phone
            EdgeButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    onOptionsClick()
                },
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_rounded_tune),
                    contentDescription = stringResource(Res.string.shopping_mode_settings),
                )
            }
        },
    ) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item(key = "header") {
                if (state.lists.size > 1) {
                    // Several lists: the name is the way to the other ones
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            onListClick()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, spec),
                        transformation = SurfaceTransformation(spec),
                        colors = ButtonDefaults.filledTonalButtonColors(),
                        label = { Text(listName) },
                    )
                } else {
                    ListHeader(Modifier.transformedHeight(this, spec)) { Text(listName) }
                }
            }
            state.groups.forEach { group ->
                group.title?.let { title ->
                    item(key = "group-$title") {
                        ListSubHeader(
                            modifier = Modifier
                                .animateItem()
                                .transformedHeight(this, spec),
                        ) {
                            Text(groupTitle(title))
                        }
                    }
                }
                items(group.items, key = { it.id }) { item ->
                    ShoppingItem(
                        item = item,
                        onDoneChange = { onDoneChange(item.id, it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                            .transformedHeight(this, spec),
                        transformation = SurfaceTransformation(spec),
                    )
                }
            }
            if (state.groups.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(Res.string.wear_list_empty),
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, spec),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun groupTitle(title: WearShoppingGroupTitle): String = when (title) {
    is WearShoppingGroupTitle.Text -> title.text
    WearShoppingGroupTitle.Ungrouped -> stringResource(Res.string.shopping_list_ungrouped)
    WearShoppingGroupTitle.NotSet -> stringResource(Res.string.stock_overview_group_not_set)
    WearShoppingGroupTitle.Done -> stringResource(Res.string.shopping_list_group_done)
}

@Composable
private fun ShoppingItem(
    item: WearShoppingItem,
    onDoneChange: (Boolean) -> Unit,
    modifier: Modifier,
    transformation: SurfaceTransformation,
) {
    val haptic = LocalHapticFeedback.current
    CheckboxButton(
        checked = item.done,
        onCheckedChange = {
            haptic.performHapticFeedback(
                if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff,
            )
            onDoneChange(it)
        },
        modifier = modifier,
        transformation = transformation,
        icon = item.sync?.let { sync -> { SyncIcon(sync) } },
        secondaryLabel = item.details?.let { { Text(it, maxLines = 2) } },
        label = {
            Text(
                text = item.title,
                maxLines = 3,
                textDecoration = TextDecoration.LineThrough.takeIf { item.done },
            )
        },
    )
}

/** That a check off is not on the server yet: the watch often has no network of its own. */
@Composable
private fun SyncIcon(sync: ShoppingListItemSync) {
    val (icon, description) = when (sync) {
        ShoppingListItemSync.SENDING ->
            Res.drawable.ic_rounded_sync to Res.string.shopping_list_item_sending
        ShoppingListItemSync.FAILED ->
            Res.drawable.ic_rounded_sync_problem to Res.string.shopping_list_item_not_sent
    }
    val text = stringResource(description)
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        modifier = Modifier.semantics { contentDescription = text },
    )
}

@Composable
private fun ListChoiceScreen(
    state: WearShoppingState,
    onListClick: (Int) -> Unit,
) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    val haptic = LocalHapticFeedback.current

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            items(state.lists, key = { it.id }) { list ->
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onListClick(list.id)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                    colors = if (list.id == state.listId) {
                        ButtonDefaults.buttonColors()
                    } else {
                        ButtonDefaults.filledTonalButtonColors()
                    },
                    label = { Text(list.name) },
                )
            }
        }
    }
}
