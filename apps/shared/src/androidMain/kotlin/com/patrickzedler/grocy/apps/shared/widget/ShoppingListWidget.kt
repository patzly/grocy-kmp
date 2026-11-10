package com.patrickzedler.grocy.apps.shared.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalGlanceId
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.CheckboxDefaults
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.patrickzedler.grocy.apps.shared.AppGraphOwner
import com.patrickzedler.grocy.apps.shared.WidgetLinks
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingList
import com.patrickzedler.grocy.core.model.text.platformDecimalSeparator
import java.util.Locale

/** The list a widget shows, chosen in [ShoppingListWidgetConfigureActivity]. */
internal val ShoppingListIdKey = intPreferencesKey("shopping_list_id")

/**
 * What is left to buy on the list the user chose for this widget: a tap on the title starts the
 * shopping mode with it, on an item opens its form, its checkbox checks it off, "+" adds an item.
 */
internal class ShoppingListWidget : GrocyWidget() {

    override val previewSizeMode = SizeMode.Responsive(setOf(DpSize(200.dp, 260.dp)))

    @Composable
    override fun Content(data: WidgetData, colors: WidgetColors, texts: WidgetTexts) {
        Shown(data, currentState<Preferences>()[ShoppingListIdKey], colors, texts)
    }

    @Composable
    override fun PreviewContent(data: WidgetData, colors: WidgetColors, texts: WidgetTexts) {
        Shown(data, SAMPLE_SHOPPING_LIST_ID, colors, texts)
    }
}

class ShoppingListWidgetReceiver : GrocyWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ShoppingListWidget()
}

@Composable
private fun Shown(data: WidgetData, listId: Int?, colors: WidgetColors, texts: WidgetTexts) {
    WidgetSurface(colors) {
        val message = unavailableText(data, texts, isAvailable = data.config.shoppingList)
        val list = data.shopping.lists.find { it.id == listId }
        when {
            message != null -> WidgetMessage(texts.shoppingList, message, colors)
            list != null -> ListItems(list, data, colors, texts)
            // Nothing chosen yet, the list was deleted or another account logged in
            else -> ChooseList(colors, texts)
        }
    }
}

@Composable
private fun ChooseList(colors: WidgetColors, texts: WidgetTexts) {
    val manager = GlanceAppWidgetManager(LocalContext.current)
    val intent = Intent(LocalContext.current, ShoppingListWidgetConfigureActivity::class.java)
        .putExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            manager.getAppWidgetId(LocalGlanceId.current),
        )
    val choose: Action = actionStartActivity(intent)
    Column(GlanceModifier.fillMaxSize()) {
        WidgetHeader(texts.shoppingList, colors, choose, addLabel = null, onAddClick = null)
        MessageTile(texts.chooseShoppingList, colors, choose)
    }
}

@Composable
private fun ListItems(
    list: ShoppingList,
    data: WidgetData,
    colors: WidgetColors,
    texts: WidgetTexts,
) {
    val items = remember(data.shopping, data.settings, list.id) {
        widgetShoppingItemsOf(
            shopping = data.shopping,
            listId = list.id,
            settings = data.settings,
            language = Locale.getDefault().language,
            decimalSeparator = platformDecimalSeparator(),
        )
    }
    val shop = openApp(WidgetLinks.shoppingMode(list.id))
    val add = openApp(WidgetLinks.newShoppingItem(list.id))
    // The title scrolls with the items like the task list's
    LazyColumn(GlanceModifier.fillMaxSize()) {
        item { WidgetHeader(list.name, colors, shop, texts.addItem, add) }
        if (items.isEmpty()) item { MessageTile(texts.shoppingListEmpty, colors, shop) }
        items(items, itemId = { it.id.toLong() }) { item ->
            ItemRow(item, colors, texts.checkOff)
        }
        item { Spacer(GlanceModifier.height(12.dp)) }
    }
}

/**
 * An item with its amount and note below it, in line with the title like a task; a tap opens its
 * form, the checkbox at its end checks it off like in the app, offline too.
 */
@Composable
private fun ItemRow(item: WidgetShoppingItem, colors: WidgetColors, checkOffLabel: String) {
    Row(
        GlanceModifier.fillMaxWidth().padding(end = CheckBoxEndPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            GlanceModifier
                .defaultWeight()
                .padding(start = WidgetInset, top = 10.dp, bottom = 10.dp)
                .clickable(openApp(WidgetLinks.shoppingItem(item.id))),
        ) {
            Text(
                text = item.name,
                maxLines = 3,
                style = TextStyle(
                    color = colors.onBackground,
                    fontSize = 16.sp,
                    fontFamily = BodyFont,
                ),
            )
            item.details?.let {
                Text(
                    text = it,
                    maxLines = 2,
                    style = TextStyle(
                        color = colors.onBackgroundVariant,
                        fontSize = 14.sp,
                        fontFamily = BodyFont,
                    ),
                )
            }
        }
        CheckBox(
            checked = false,
            onCheckedChange = actionRunCallback<CheckOffShoppingItemAction>(
                actionParametersOf(ShoppingItemIdKey to item.id),
            ),
            colors = CheckboxDefaults.colors(
                checkedColor = colors.primary,
                uncheckedColor = colors.onBackgroundVariant,
            ),
            modifier = GlanceModifier.semantics { contentDescription = checkOffLabel },
        )
    }
}

// The checkbox brings its own touch target, so the row ends where the add button ends
private val CheckBoxEndPadding = 4.dp

/** The widget's checkbox: the item is done for Grocy, sent in the background or queued. */
class CheckOffShoppingItemAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val id = parameters[ShoppingItemIdKey] ?: return
        val graph = (context.applicationContext as? AppGraphOwner)?.graph ?: return
        // The widget follows the stored items, the done one disappears from it
        graph.widgetDependencies.shoppingListRepository.setDone(id, done = true)
    }
}

internal val ShoppingItemIdKey = ActionParameters.Key<Int>("shopping_item_id")
