# Widgets

Home screen widgets of the Android phone apps, built with Glance (`apps/shared`, package
`widget`). Grocy's web frontend has none; they take the form of Google's Tasks and Calendar
widgets, whose look the project owner chose, with Grocy's data and the app's colors.

- Task list (`TaskListWidget`): Grocy's tasks to do by due date, those without one last
  (`widgetTasksOf`), the date with a clock, in the error color when overdue. The circle is Grocy's
  "Mark task as completed" (`CompleteTaskAction`, online only like in the app: it shows checked
  while the server completes it, a failure shows a toast); a tap on the task opens its form, on the
  title the tasks page, "+" a new task. Google's list dropdown has no counterpart and is left out.
- Calendar month view (`MonthWidget`): the month like the app's (`monthCellsOf`, the weeks start at
  `CALENDAR_FIRST_DAY_OF_WEEK`), as many one line chips per day as fit (`monthChipPlaces`), dots for
  the rest; an icon where a day is wide enough. Previous and next step through the months
  (`MonthStepAction`, Glance's state of the widget), for the rest of the day (`shownMonthOf`). A tap
  on a day opens the calendar's day, on the title its month.
- Calendar schedule (`ScheduleWidget`): today, also without events ("Nothing due today"), then
  every day with events (`scheduleRowsOf`), each event like a task of Google's widget with time
  and section below. A tap on an event goes where Grocy links it (`routeOf` of the calendar), on a
  day to the calendar's schedule. Narrow it writes the date short like Google ("5 Thu").
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
  something since or the day changed (`CalendarRepository.refreshIfChanged`): a check without news
  is one small request. Using the API does not change that time (Grocy keeps it when it records
  `last_used`).
- A new language restarts the app; the first start in it redraws them.

## Testing

`WidgetModelTest` (tasks, schedule, month, stepping, places, midnight, manifest),
`WidgetColorsTest`, `WidgetLinksTest`, `CalendarRepositoryTest` (refresh only on changes). How they
look and behave on a launcher needs a device.
