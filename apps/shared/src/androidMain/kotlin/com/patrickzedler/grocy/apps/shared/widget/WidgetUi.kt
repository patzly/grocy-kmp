package com.patrickzedler.grocy.apps.shared.widget

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.runtime.collectAsState
import com.patrickzedler.grocy.apps.shared.AndroidAppLanguageStore
import com.patrickzedler.grocy.apps.shared.AppGraphOwner
import com.patrickzedler.grocy.apps.shared.MainActivity
import com.patrickzedler.grocy.apps.shared.R
import com.patrickzedler.grocy.core.design.appColorRoleSpecs
import com.patrickzedler.grocy.core.design.appColorSchemes
import com.patrickzedler.grocy.core.model.AppearanceSettings
import com.patrickzedler.grocy.core.model.calendar.CalendarEvent
import com.patrickzedler.grocy.core.model.calendar.CalendarEventKind
import com.patrickzedler.grocy.core.model.calendar.calendarEventTitle
import com.patrickzedler.grocy.core.model.text.todayEpochDay
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.calendar_assigned_to
import com.patrickzedler.grocy.core.resources.calendar_battery_due
import com.patrickzedler.grocy.core.resources.calendar_chore_due
import com.patrickzedler.grocy.core.resources.calendar_meal_plan_note
import com.patrickzedler.grocy.core.resources.calendar_meal_plan_product
import com.patrickzedler.grocy.core.resources.calendar_meal_plan_recipe
import com.patrickzedler.grocy.core.resources.calendar_next
import com.patrickzedler.grocy.core.resources.calendar_nothing_today
import com.patrickzedler.grocy.core.resources.calendar_previous
import com.patrickzedler.grocy.core.resources.calendar_product_due
import com.patrickzedler.grocy.core.resources.calendar_task_due
import com.patrickzedler.grocy.core.resources.calendar_title
import com.patrickzedler.grocy.core.resources.shopping_list_add_item
import com.patrickzedler.grocy.core.resources.shopping_list_empty
import com.patrickzedler.grocy.core.resources.shopping_list_list
import com.patrickzedler.grocy.core.resources.task_create
import com.patrickzedler.grocy.core.resources.tasks_title
import com.patrickzedler.grocy.core.resources.widget_log_in
import com.patrickzedler.grocy.core.resources.widget_no_tasks
import com.patrickzedler.grocy.core.resources.widget_not_available
import com.patrickzedler.grocy.core.resources.widget_shopping_choose_hint
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.getString

/**
 * The base of the widgets: they draw what the database holds, also offline, and follow it while
 * Glance keeps them composed. Every size the launcher asks for is drawn exactly, so they fit
 * whatever size the user gives them.
 */
internal abstract class GrocyWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // The texts' language before Android 13: no activity set it in a process just started
        AndroidAppLanguageStore.wrap(context)
        val graph = (context.applicationContext as AppGraphOwner).graph
        WidgetState.today.value = todayEpochDay()
        val data = widgetDataOf(graph.widgetDependencies)
        // Read before composing, so the first drawing already shows it
        val initial = data.first()
        val texts = widgetTexts()
        provideContent {
            val current by data.collectAsState(initial)
            val colors = remember(current.appearance) {
                widgetColorsOf(context, current.appearance)
            }
            Content(current, colors, texts)
        }
    }

    /**
     * Android 15's picker shows the widget itself with [sampleWidgetData] (published by
     * [WidgetUpdates]), so it looks exactly like it will in the app's colors and language.
     */
    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val localized = AndroidAppLanguageStore.wrap(context)
        val graph = (context.applicationContext as AppGraphOwner).graph
        val appearance = graph.widgetDependencies.appearance.first()
        val data = sampleWidgetData(todayEpochDay(), sampleNamesOf(localized), appearance)
        val colors = widgetColorsOf(context, appearance)
        val texts = widgetTexts()
        provideContent { PreviewContent(data, colors, texts) }
    }

    @Composable
    protected abstract fun Content(data: WidgetData, colors: WidgetColors, texts: WidgetTexts)

    /** What the picker shows, the content without the state of a placed widget. */
    @Composable
    protected open fun PreviewContent(data: WidgetData, colors: WidgetColors, texts: WidgetTexts) {
        Content(data, colors, texts)
    }
}

/** The picker's names in the app's language, the same its sketches of older Androids show. */
private fun sampleNamesOf(context: Context) = SampleNames(
    mowLawn = context.getString(R.string.widget_sample_mow_lawn),
    buySeeds = context.getString(R.string.widget_sample_buy_seeds),
    waterPlants = context.getString(R.string.widget_sample_water_plants),
    descale = context.getString(R.string.widget_sample_descale),
    milk = context.getString(R.string.widget_sample_milk),
    vacuum = context.getString(R.string.widget_sample_vacuum),
    spaghetti = context.getString(R.string.widget_sample_spaghetti),
    lunch = context.getString(R.string.widget_sample_lunch),
    smokeDetector = context.getString(R.string.widget_sample_smoke_detector),
    groceries = context.getString(R.string.widget_sample_groceries),
    bread = context.getString(R.string.widget_sample_bread),
    eggs = context.getString(R.string.widget_sample_eggs),
    apples = context.getString(R.string.widget_sample_apples),
)

private fun widgetColorsOf(context: Context, appearance: AppearanceSettings): WidgetColors {
    val (light, dark) = appColorSchemes(context, appearance)
    val (lightSpec, darkSpec) = appColorRoleSpecs(context, appearance)
    return WidgetColors(light, dark, lightSpec, darkSpec)
}

/** The widgets' texts, read before they are drawn because Glance cannot read Compose resources. */
internal class WidgetTexts(
    val tasks: String,
    val calendar: String,
    val logIn: String,
    val noTasks: String,
    val notAvailable: String,
    val nothingToday: String,
    val previous: String,
    val next: String,
    val createTask: String,
    val shoppingList: String,
    val addItem: String,
    val shoppingListEmpty: String,
    val chooseShoppingList: String,
    private val kinds: Map<CalendarEventKind, String>,
    private val assignedTo: String,
) {
    /** Grocy's title of [event]. */
    fun title(event: CalendarEvent): String =
        calendarEventTitle(kinds[event.kind].orEmpty(), event.name)

    /** What Grocy's title says of [event], what the app's calendar tells screen readers. */
    fun description(event: CalendarEvent): String = listOfNotNull(
        event.time,
        kinds[event.kind],
        event.section,
        event.name,
        event.assignedTo?.let { assignedTo.replace(NAME_PLACEHOLDER, it) },
    ).joinToString(", ")
}

private suspend fun widgetTexts() = WidgetTexts(
    tasks = getString(Res.string.tasks_title),
    calendar = getString(Res.string.calendar_title),
    logIn = getString(Res.string.widget_log_in),
    noTasks = getString(Res.string.widget_no_tasks),
    notAvailable = getString(Res.string.widget_not_available),
    nothingToday = getString(Res.string.calendar_nothing_today),
    previous = getString(Res.string.calendar_previous),
    next = getString(Res.string.calendar_next),
    createTask = getString(Res.string.task_create),
    shoppingList = getString(Res.string.shopping_list_list),
    addItem = getString(Res.string.shopping_list_add_item),
    shoppingListEmpty = getString(Res.string.shopping_list_empty),
    chooseShoppingList = getString(Res.string.widget_shopping_choose_hint),
    kinds = mapOf(
        CalendarEventKind.PRODUCT to getString(Res.string.calendar_product_due),
        CalendarEventKind.TASK to getString(Res.string.calendar_task_due),
        CalendarEventKind.CHORE to getString(Res.string.calendar_chore_due),
        CalendarEventKind.BATTERY to getString(Res.string.calendar_battery_due),
        CalendarEventKind.MEAL_PLAN_RECIPE to getString(Res.string.calendar_meal_plan_recipe),
        CalendarEventKind.MEAL_PLAN_NOTE to getString(Res.string.calendar_meal_plan_note),
        CalendarEventKind.MEAL_PLAN_PRODUCT to getString(Res.string.calendar_meal_plan_product),
    ),
    assignedTo = getString(Res.string.calendar_assigned_to, NAME_PLACEHOLDER),
)

private const val NAME_PLACEHOLDER = "\u0000name\u0000"

/** Opens the app at [link] of [com.patrickzedler.grocy.apps.shared.WidgetLinks], else as it is. */
@Composable
internal fun openApp(link: String?): Action {
    val context = LocalContext.current
    val intent = Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    link?.let { intent.putExtra(WIDGET_LINK_EXTRA, it) }
    return actionStartActivity(intent)
}

/** The extra of the intent a widget opens the app with, a link of its WidgetLinks. */
const val WIDGET_LINK_EXTRA = "com.patrickzedler.grocy.WIDGET_LINK"

/** The shapes of the widgets, as drawables for the corners before Android 12. */
internal enum class WidgetShape(val radius: Dp, @param:DrawableRes val drawable: Int) {
    SMALL(4.dp, R.drawable.widget_shape_small),
    MEDIUM(16.dp, R.drawable.widget_shape_medium),
    FULL(18.dp, R.drawable.widget_shape_full),
}

/** A background of [color] with rounded corners, on every Android version. */
internal fun GlanceModifier.rounded(color: ColorProvider, shape: WidgetShape): GlanceModifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        background(color).cornerRadius(shape.radius)
    } else {
        background(ImageProvider(shape.drawable), colorFilter = ColorFilter.tint(color))
    }

/** The widget's own surface, with the launcher's corners from Android 12 on. */
@Composable
internal fun WidgetSurface(colors: WidgetColors, content: @Composable () -> Unit) {
    val surface = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        GlanceModifier.background(colors.background)
            .cornerRadius(android.R.dimen.system_app_widget_background_radius)
    } else {
        GlanceModifier.background(
            ImageProvider(R.drawable.widget_shape_large),
            colorFilter = ColorFilter.tint(colors.background),
        )
    }
    Box(GlanceModifier.fillMaxSize().appWidgetBackground().then(surface)) { content() }
}

@Composable
internal fun WidgetIcon(
    @DrawableRes icon: Int,
    tint: ColorProvider,
    size: Dp,
    modifier: GlanceModifier = GlanceModifier,
) {
    Image(
        provider = ImageProvider(icon),
        contentDescription = null,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier.size(size),
    )
}

/** The title row of a widget with its "+" like Google's widgets. */
@Composable
internal fun WidgetHeader(
    title: String,
    colors: WidgetColors,
    onTitleClick: Action,
    addLabel: String?,
    onAddClick: Action?,
    start: Dp = WidgetInset,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = GlanceModifier.fillMaxWidth().height(HeaderHeight).padding(end = 4.dp),
    ) {
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = GlanceModifier.defaultWeight().height(HeaderHeight)
                .padding(start = start, end = 4.dp)
                .clickable(onTitleClick),
        ) {
            Text(
                text = title,
                maxLines = 1,
                style = TextStyle(
                    color = colors.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = TitleFont,
                ),
            )
        }
        trailing()
        if (addLabel != null && onAddClick != null) AddButton(addLabel, colors, onAddClick)
    }
}

/** Google's filled "+" of its widgets, with a touch target of 48.dp. */
@Composable
private fun AddButton(label: String, colors: WidgetColors, onClick: Action) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = GlanceModifier.size(64.dp, 48.dp)
            .clickable(onClick)
            .semantics { contentDescription = label },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = GlanceModifier.size(56.dp, 36.dp).rounded(colors.primary, WidgetShape.FULL),
        ) {
            WidgetIcon(R.drawable.widget_ic_add, colors.onPrimary, 24.dp)
        }
    }
}

/** A small icon button of a widget's title row, e.g. the month's "Previous". */
@Composable
internal fun HeaderIconButton(
    @DrawableRes icon: Int,
    label: String,
    colors: WidgetColors,
    onClick: Action,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = GlanceModifier.size(40.dp, 48.dp)
            .clickable(onClick)
            .semantics { contentDescription = label },
    ) {
        WidgetIcon(icon, colors.onBackgroundVariant, 24.dp)
    }
}

/** A message in a tile, e.g. Google's "Nothing scheduled". */
@Composable
internal fun MessageTile(text: String, colors: WidgetColors, onClick: Action) {
    Box(GlanceModifier.fillMaxWidth().padding(horizontal = ChipMargin, vertical = 4.dp)) {
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = GlanceModifier.fillMaxWidth()
                .rounded(colors.tile, WidgetShape.MEDIUM)
                .padding(horizontal = ChipPadding, vertical = 12.dp)
                .clickable(onClick),
        ) {
            Text(
                text = text,
                style = TextStyle(
                    color = colors.onTile,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = TitleFont,
                ),
            )
        }
    }
}

/** A widget that cannot show its content: logged out or the server lacks the feature. */
@Composable
internal fun WidgetMessage(title: String, text: String, colors: WidgetColors) {
    val open = openApp(null)
    Column(GlanceModifier.fillMaxSize()) {
        WidgetHeader(title, colors, open, addLabel = null, onAddClick = null)
        MessageTile(text, colors, open)
    }
}

/** The message a widget shows instead of its content, null when it can show it. */
internal fun unavailableText(data: WidgetData, texts: WidgetTexts, isAvailable: Boolean): String? =
    when {
        !data.isLoggedIn -> texts.logIn
        !isAvailable -> texts.notAvailable
        else -> null
    }

// Google's fonts of its widgets where the device has them, else the system's
internal val TitleFont = FontFamily("google-sans")
internal val BodyFont = FontFamily("google-sans-text")

internal val HeaderHeight = 56.dp

// A chip's distance from the widget's edge and its text's from the chip's, so the texts of chips
// and headings start in one line like in Google's calendar widget
internal val ChipMargin = 12.dp
internal val ChipPadding = 10.dp

/** Where texts start from the widget's edge: in line with the text of chips. */
internal val WidgetInset = ChipMargin + ChipPadding

internal val CalendarEventKind.icon: Int
    @DrawableRes get() = when (this) {
        CalendarEventKind.PRODUCT -> R.drawable.widget_ic_kitchen
        CalendarEventKind.TASK -> R.drawable.widget_ic_checklist
        CalendarEventKind.CHORE -> R.drawable.widget_ic_cleaning_services
        CalendarEventKind.BATTERY -> R.drawable.widget_ic_battery_charging_full
        CalendarEventKind.MEAL_PLAN_RECIPE -> R.drawable.widget_ic_menu_book
        CalendarEventKind.MEAL_PLAN_NOTE -> R.drawable.widget_ic_edit_note
        CalendarEventKind.MEAL_PLAN_PRODUCT -> R.drawable.widget_ic_restaurant
    }
