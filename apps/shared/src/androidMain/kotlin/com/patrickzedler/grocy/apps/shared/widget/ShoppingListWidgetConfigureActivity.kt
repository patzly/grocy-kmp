package com.patrickzedler.grocy.apps.shared.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import com.patrickzedler.grocy.apps.shared.AndroidAppLanguageStore
import com.patrickzedler.grocy.apps.shared.AppGraphOwner
import com.patrickzedler.grocy.apps.shared.WidgetDependencies
import com.patrickzedler.grocy.core.design.GrocyTheme
import com.patrickzedler.grocy.core.design.component.LeadingTrailingContentWrapper
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialog
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialogContent
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.dialogListScrollbar
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingList
import com.patrickzedler.grocy.core.network.GrocyApiException
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_close
import com.patrickzedler.grocy.core.resources.widget_log_in
import com.patrickzedler.grocy.core.resources.widget_not_available
import com.patrickzedler.grocy.core.resources.widget_shopping_choose
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** What the choice of a widget's list can offer. */
internal sealed interface ShoppingListChoice {
    /** Every list; empty while they are still being downloaded. */
    data class Lists(val lists: List<ShoppingList>) : ShoppingListChoice

    data object LoggedOut : ShoppingListChoice

    data object NotAvailable : ShoppingListChoice
}

internal fun shoppingListChoiceOf(
    isLoggedIn: Boolean,
    isAvailable: Boolean,
    lists: List<ShoppingList>,
): ShoppingListChoice = when {
    !isLoggedIn -> ShoppingListChoice.LoggedOut
    !isAvailable -> ShoppingListChoice.NotAvailable
    else -> ShoppingListChoice.Lists(lists)
}

/**
 * Floats over the home screen: the launcher starts it when a shopping list widget is placed, which
 * needs a choice, and from the widget's settings. Without a choice a new widget is not placed.
 */
class ShoppingListWidgetConfigureActivity : ComponentActivity() {

    private var isDownloading by mutableStateOf(true)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AndroidAppLanguageStore.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        // Canceled unless a list is chosen; another app cannot reach other widgets through this
        setResult(Activity.RESULT_CANCELED, resultOf(widgetId))
        if (!isShoppingListWidget(widgetId)) {
            finish()
            return
        }
        val dependencies = (application as AppGraphOwner).graph.widgetDependencies
        downloadLists(dependencies)
        val choice = choiceOf(dependencies)
        setContent {
            val appearance by dependencies.appearance.collectAsState(null)
            val current by choice.collectAsState(null)
            val selectedId by selectedIdOf(widgetId)
            val theme = appearance ?: return@setContent
            val shown = current ?: return@setContent
            GrocyTheme(theme) {
                ChoiceDialog(
                    choice = shown,
                    selectedId = selectedId,
                    isDownloading = isDownloading,
                    onListClick = { choose(widgetId, it.id) },
                    onDismissRequest = ::finish,
                )
            }
        }
    }

    private fun resultOf(widgetId: Int) =
        Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)

    private fun isShoppingListWidget(widgetId: Int): Boolean =
        AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId)?.provider ==
            ComponentName(this, ShoppingListWidgetReceiver::class.java)

    private fun choiceOf(dependencies: WidgetDependencies): Flow<ShoppingListChoice> = combine(
        dependencies.connection,
        dependencies.masterDataRepository.serverConfig,
        dependencies.shoppingListRepository.lists,
    ) { connection, config, lists ->
        shoppingListChoiceOf(connection != null, config.shoppingList, lists)
    }

    // The lists may have changed since the app last ran
    private fun downloadLists(dependencies: WidgetDependencies) {
        lifecycleScope.launch {
            try {
                dependencies.shoppingListRepository.refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (_: GrocyApiException) {
                // Offline it offers what is stored
            } finally {
                isDownloading = false
            }
        }
    }

    @Composable
    private fun selectedIdOf(widgetId: Int) = produceState<Int?>(null) {
        val glanceId = GlanceAppWidgetManager(this@ShoppingListWidgetConfigureActivity)
            .getGlanceIdBy(widgetId)
        value = getAppWidgetState<Preferences>(
            this@ShoppingListWidgetConfigureActivity,
            PreferencesGlanceStateDefinition,
            glanceId,
        )[ShoppingListIdKey]
    }

    private fun choose(widgetId: Int, listId: Int) {
        lifecycleScope.launch {
            val glanceId = GlanceAppWidgetManager(this@ShoppingListWidgetConfigureActivity)
                .getGlanceIdBy(widgetId)
            updateAppWidgetState(this@ShoppingListWidgetConfigureActivity, glanceId) {
                it[ShoppingListIdKey] = listId
            }
            ShoppingListWidget().update(this@ShoppingListWidgetConfigureActivity, glanceId)
            setResult(Activity.RESULT_OK, resultOf(widgetId))
            finish()
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ChoiceDialog(
    choice: ShoppingListChoice,
    selectedId: Int?,
    isDownloading: Boolean,
    onListClick: (ShoppingList) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    ScrollableAlertDialog(onDismissRequest = onDismissRequest) {
        ScrollableAlertDialogContent(
            title = { Text(stringResource(Res.string.widget_shopping_choose)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptic.confirm()
                        onDismissRequest()
                    },
                    modifier = Modifier.clickCursor(),
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(Res.string.action_close))
                }
            },
            isScrollableControlledByContent = choice is ShoppingListChoice.Lists,
        ) {
            when (choice) {
                ShoppingListChoice.LoggedOut -> Message(Res.string.widget_log_in)
                ShoppingListChoice.NotAvailable -> Message(Res.string.widget_not_available)
                is ShoppingListChoice.Lists -> when {
                    choice.lists.isNotEmpty() -> Lists(choice.lists, selectedId) {
                        haptic.confirm()
                        onListClick(it)
                    }
                    isDownloading -> Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    ) {
                        LoadingIndicator()
                    }
                    else -> Message(Res.string.widget_not_available)
                }
            }
        }
    }
}

@Composable
private fun Message(text: StringResource) {
    Text(stringResource(text))
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Lists(
    lists: List<ShoppingList>,
    selectedId: Int?,
    onClick: (ShoppingList) -> Unit,
) {
    val listState = rememberLazyListState()
    LazyColumn(
        modifier = Modifier.fillMaxWidth().dialogListScrollbar(listState),
        state = listState,
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        itemsIndexed(lists, key = { _, list -> list.id }) { index, list ->
            val selected = list.id == selectedId
            SegmentedListItem(
                selected = selected,
                onClick = { onClick(list) },
                shapes = ListItemDefaults.segmentedShapes(index = index, count = lists.size),
                colors = SegmentedListDefaults.colors(),
                modifier = Modifier.fillMaxWidth().clickCursor(),
                verticalAlignment = Alignment.CenterVertically,
                leadingContent = {
                    LeadingTrailingContentWrapper {
                        // The item handles the click, the radio button only shows the choice
                        RadioButton(selected = selected, onClick = null)
                    }
                },
                content = { Text(list.name) },
            )
        }
    }
}
