# Calendar

Grocy's calendar page (`/calendar`), `feature:calendar`. Grocy shows it with FullCalendar, this is
its own Material 3 Expressive version that does the same.

- Route: `CalendarRoute(view, day)` (`feature:calendar:api`), a screen of its own that uses the
  whole window like the meal plan; the widgets open it in a view on a day, otherwise it opens today
  in the window's view. The start screen opens it from its section "Calendar" after the equipment (with
  `FEATURE_FLAG_CALENDAR`), which tells how many events today has.
- Events (`calendarEventsOf`, `core:model`, Grocy's `CalendarService`): products in stock on their
  due date (with due date tracking), tasks to do, chores (with their time unless they track the date
  only; "assigned to" a user), batteries of active ones, and the meal plan's recipes, notes and
  products (the section's name and time). Grocy offers them only in its translated iCal file or
  inside its page, so `CalendarRepository` (`core:data`) computes them from what the other
  repositories stored; the calendar therefore works offline. Whole days first, then by time, kind
  and name.
- Form: the established form of Google Calendar, whose references the project owner chose, in
  Material 3 Expressive. The title is the period ("November 2026", "Oct – Nov 2026"), with
  Grocy's `CALENDAR_SHOW_WEEK_OF_YEAR` the week's number below it. The floating toolbar holds previous,
  the view's menu and next, its FAB goes to today. Hour labels and weekdays stand directly on the
  background, the project owner's exception like Google's Android app.
- Views (`CalendarView`): Grocy's day, week, month and list (the schedule, going on from the shown day). Without a choice small windows (below 768.dp, Grocy's limit)
  start with the day, others with the month (`defaultCalendarView`). Weeks start at
  `CALENDAR_FIRST_DAY_OF_WEEK`, else the language's first day (`weekOfYear` counts them).
- Events are one line chips (`EventChip`) like Google's tasks, because they have no duration: at their
  time in the time grid, those at nearly the same time below each other (`chipTops`), and on top without
  a time, Grocy's 23:59:59 counting as none. The icon says what it is about where it fits, screen readers
  get what Grocy's title says. Colors: `customStatusColorOf` harmonizes the user's color with the theme's
  primary and derives the filled role (tone 40 and 100 in light, 80 and 20 in dark), so all follow
  the theme; the color dialogs show them like that too.
- Time grid (`CalendarTimeGrid`, day, week): days with what has no time as sticky header (three
  rows, "3 more" expands), below them the hours as segmented rows, 56.dp per hour, the line at the current
  time on today. It opens around now or at 7:00. A tap on a day's heading shows the day.
- Month (`monthGrid`): a segmented row per week sharing the window's height (`monthRowHeight`), four
  places per day ("3 more" and a tap on the day show the day). Schedule (`schedule`): a segmented row per day with events.
- A tap on an event goes where Grocy links it (`routeOf`): stock overview, tasks, chores overview,
  batteries overview, the recipe, or the meal plan's week of that day.
- "Share/Integrate calendar (iCal)" of the app bar's menu: asks the server for the public link
  (`calendar/ical/sharing-link`, which creates its key), shows it with a QR code and copies it. The
  link holds a key, so it is kept in the ViewModel only while the dialog is open, never stored.
- "Configure colors" of the app bar's menu: Grocy's user settings `calendar_color_*` of the features
  the server has; a color of the palette (Grocy's) or any typed as "#rrggbb", saved at once and online
  only like Grocy's, a failure shows a message.
