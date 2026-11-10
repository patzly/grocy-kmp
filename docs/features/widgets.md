# Widgets

Home screen widgets of the Android phone apps, built with Glance (`apps/shared`, package
`widget`). Grocy's web frontend has none; they take the form of Google's Tasks and Calendar
widgets, whose look the project owner chose, with Grocy's data and the app's colors.

- Task list (`TaskListWidget`): Grocy's tasks to do by due date, those without one last
  (`widgetTasksOf`), the date with a clock, in the error color when overdue; no circles, in line
  with the title (the project owner's choice: a list of tasks needs no marks). A tap on the task
  opens its form, on the title the tasks page, "+" a new task. Google's list dropdown has no
  counterpart and is left out.
- Calendar month view (`MonthWidget`): the month like the app's (`monthCellsOf`, the weeks start at
  `CALENDAR_FIRST_DAY_OF_WEEK`), as many one line chips per day as fit (`monthChipPlaces`), dots for
  the rest; an icon where a day is wide enough. Previous and next step through the months
  (`MonthStepAction`, Glance's state of the widget), for the rest of the day (`shownMonthOf`). A tap
  on a day opens the calendar's day, on the title its month.
- Event titles in the calendar list: from three columns (`NarrowWidth`) Grocy's whole title
  ("Product due: Milk"), narrower only the name with the icon, so the kind never takes its place;
  both wrap, a widget's list rows may differ in height.
- Calendar list (`ScheduleWidget`, the class keeps its first name because the system knows placed
  widgets by it): today, also without events ("Nothing due today"), then every day with events
  (`scheduleRowsOf`), each event like a task of Google's widget with time and section below. Like
  Google's widget the dates start exactly where the chips' texts do (`ChipMargin` + `ChipPadding`
  = `WidgetInset`), so do the titles and tasks of all widgets. A tap on an event opens what it is
  about (`routeOf` of the calendar), on a day the calendar's list. Narrow it writes the date short
  like Google ("5 Thu").
- Shopping list (`ShoppingListWidget`): what is left to buy on one list by name
  (`widgetShoppingItemsOf`), amount and note below. The title is the list's name and starts the
  shopping mode with it, a tap on an item opens its form, the checkbox at its end checks it off
  (`CheckOffShoppingItemAction`, `ShoppingListRepository.setDone`: offline too, queued like the
  app's, the item then leaves the widget), "+" adds to the list. Which list is the
  user's choice for each widget (`ShoppingListIdKey`, Glance's state of the widget):
  `ShoppingListWidgetConfigureActivity` floats over the home screen when it is placed (without a
  choice it is not placed) and from the widget's settings (`reconfigurable`). The activity is
  exported for the launcher and only accepts this kind of widget. A list that no longer exists
  (deleted, another account) makes the widget ask again.
- Previews: from Android 15 on the picker shows the widgets themselves (`providePreview`, Glance's
  generated previews) with `sampleWidgetData`: tasks and events of every kind around today with
  Grocy's colors, the names translated Android resources (`widget_sample_*`), in the app's colors
  and language. `WidgetUpdates` publishes them at start when the app's version, language or
  appearance changed since (the system limits how often). Android 12 to 14 show the layouts
  `widget_preview_*` instead: the same with those names in the wallpaper's colors.
- Scrollbars stay the launcher's: a widget's list is the system's view, whose scrollbar no app can
  shape.
- Without a login or the server's feature (`FEATURE_FLAG_TASKS`, `_CALENDAR`) they say so and open
  the app. All texts come from `core:resources` (`WidgetTexts`, read before drawing); the picker's
  names and descriptions are Android resources of `apps/shared` in every language.
- Links: a tap opens `MainActivity` with a text of `WidgetLinks` (`WIDGET_LINK_EXTRA`), which
  `GrocyApp` opens on top like a notification. Only those few links exist, so another app cannot
  open anything else through it. `CalendarRoute` takes the view and day for them.

## Colors

`appColorSchemes` (`core:design`) gives the light and dark scheme of the app's appearance (the
wallpaper's with dynamic color). Every color is a day/night pair (`WidgetColors`), so the system
switches with the device's dark theme by itself from Android 12 on, before that the next redraw
does. The background is the secondary color at tone 95 and 20 like Google's widgets, "+" primary.
Events use `customStatusColorOf` like the app's calendar, harmonized for light and dark
(`eventColorOf`). Corners: the launcher's radius and `cornerRadius` from Android 12 on, tinted shape
drawables before. Google's system fonts where the device has them (`google-sans`), else the
system's.

## Staying current

They draw what the database holds, so they work offline, and nothing runs without a widget
(`WidgetUpdates.sync`, called when the first is added and the last removed, at start and after a
restart):

- While the app runs, `WidgetUpdates.start` follows the data (`widgetDataOf`) and redraws when it
  changes, e.g. after the user changed something in the app; Glance keeps a drawn widget composed
  for about 45 seconds and follows the same data then.
- At midnight `WidgetDayReceiver` moves them to the new day: an `RTC` alarm that does not wake the
  device, so it costs nothing and shows the new day once the screen is on. It also runs after a
  restart, an update of the app and a changed time, zone or language.
- `WidgetRefreshWorker` (WorkManager, every 30 minutes, only with a network, batched by the system
  in Doze) asks Grocy's `system/db-changed-time` and downloads only when the server changed
  something since or the day changed (`WidgetDataRefresher`): a check without news is one small
  request, shared by all widgets, and a change only downloads what placed widgets show. The
  shopping list widget therefore adds no timer or wake up of its own: changes made elsewhere reach
  it within about 30 minutes, changes in the app at once. Using the API does not change that time (Grocy keeps it when it records
  `last_used`).
- A new language restarts the app; the first start in it redraws them.

## Testing

`WidgetModelTest` (tasks, shopping list, list, month, stepping, places, samples, midnight, manifest),
`WidgetColorsTest`, `WidgetLinksTest`, `WidgetDataRefresherTest` (refresh only on changes). How they
look and behave on a launcher needs a device.
