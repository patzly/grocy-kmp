# Calendar

Grocy's calendar page (`/calendar`), `feature:calendar`. Grocy shows it with FullCalendar, this is
its own Material 3 Expressive version that does the same.

- Route: `CalendarRoute(view, day, productId)` (`feature:calendar:api`), a screen of its own that
  uses the whole window like the meal plan. Without arguments it opens today in the view the user
  chose last; the widgets open it in a view on a day, a product's event with its overview. The
  start screen opens it from its section "Calendar" after the equipment (with
  `FEATURE_FLAG_CALENDAR`), which tells how many events today has.
- Events (`calendarEventsOf`, `core:model`, Grocy's `CalendarService`): products in stock on their
  due date (with due date tracking), tasks to do, chores (with their time unless they track the date
  only; "assigned to" a user), batteries of active ones, and the meal plan's recipes, notes and
  products (the section's name and time). Each knows its object (`objectId`). Grocy offers them
  only in its translated iCal file or inside its page, so `CalendarRepository` (`core:data`)
  computes them from what the other repositories stored; the calendar therefore works offline.
  Whole days first, then by time, kind and name.
- Form: the established form of Google Calendar, whose references the project owner chose, in
  Material 3 Expressive. The title is always "Calendar", the subtitle the period short ("Nov 2026",
  "Oct – Nov 2026", the list's at its top) and with Grocy's `CALENDAR_SHOW_WEEK_OF_YEAR` the week's
  number. The screen lays itself out (`SegmentedListScreen`'s `body`): what heads a view stays,
  only below it scrolls, with the scrollbar. Hour labels and weekdays stand directly on the
  background, the project owner's exception like Google's Android app.
- Views (`CalendarView`): Grocy's day, week, month and list (going on like Google's schedule). The
  chosen one is kept across app starts (`CalendarOptionsRepository`, `SettingsStore.calendarView`),
  the list without a choice. Weeks start at `CALENDAR_FIRST_DAY_OF_WEEK`, else the language's first
  day (`weekOfYear` counts them).
- Paging: day, week and month are a `HorizontalPager` each (`PagedView`), pages counted from a fixed
  day (`dayOfPage`, `pageOfDay`); swiping or the floating toolbar's arrows turn them with the pager's
  animation, a settled page moves the calendar to its day, a day chosen elsewhere turns the pages
  there. The list has no periods, its arrows stay disabled, so toolbar and FAB keep their place. The toolbar's view button opens its menu right
  above the toolbar (`FloatingToolbarMenu`, 8.dp apart), the current view selected.
- Today: the FAB ("Today", in the day "Now") turns to today and scrolls to the current time (time
  grid) or today's row (list), animated.
- Changing views: a `SharedTransitionLayout` around an `AnimatedContent` of the views; events
  (`CalendarEvent.key`) and days' numbers and weekdays (`dayKey`, `weekdayKey`) share their bounds,
  so they move and grow on the theme's spatial spring into their place in the new view
  (`sharedCalendarBounds`), the rest fades with its effects spring.
- Grocy's title (`calendarEventTitle`: "Product due: Milk") shows where it leaves the name whole: in
  the list, which wraps it to two lines, and on chips at least `ChipKindMinWidth` wide; narrower
  chips show the name and their icon. Screen readers always get the whole title.
- Events are one line chips (`EventChip`) like Google's tasks, because they have no duration: at their
  time in the time grid, those at nearly the same time below each other (`chipTops`), and on top without
  a time, Grocy's 23:59:59 counting as none. The icon says what it is about where it fits, screen readers
  get what Grocy's title says. Colors: `customStatusColorOf` harmonizes the user's color with the theme's
  primary and derives its filled roles (`color`, `onColor`) like Material's primary ones, in the
  theme's contrast level (`docs/ui.md`), so all follow the theme; the color dialogs show them like
  that too.
- Day (`TimeGridPage`): Google's day, its weekday and number (`DayBadge`) at the start of the hours'
  column, what has no time beside them (three rows, "3 more" expands); below the hours as segmented
  rows, 56.dp per hour, the line at the current time on today. Week: the days' badges side by side,
  a tap on a number shows that day, the columns themselves do not react. Every page of both views shares one scroll
  state, so paging and switching keep the time; it opens around now on today, else at 7:00.
- Month (`MonthPage`): the weekdays stay, a segmented row per week shares the height that is left
  (`monthRowHeight`), four places per day with the names only (a tap on "3 more" or the
  number shows the day, the cell itself does not react).
- List (`CalendarList`, `listRowsOf`): every day with events and today, below the headings of their
  weeks ("Nov 9 – 15"), within the width of the screen's lists. Each day's badge at the start (a tap
  shows the day, the number moves to the day's heading), its events as Google's schedule chips with
  icon, time, section and assignee. The badge stays at the top of the list while the day's events
  scroll past, until the next day's badge pushes it away (`stuckBadgeShift`); today has the line of the current time after what took place
  (`nowIndexOf`). It opens at the shown day (`listIndexOf`).
- A tap on an event opens what it is about, unlike Grocy, whose links lead to whole lists
  (`routeOf`): the task's form, the chore's or battery's tracking, the recipe, a product's overview
  over the calendar (`ProductOverviewHost`); a meal plan note opens the meal plan's week.
- "Share/Integrate calendar (iCal)" of the app bar's menu: asks the server for the public link
  (`calendar/ical/sharing-link`, which creates its key), shows it with a QR code and copies it. The
  link holds a key, so it is kept in the ViewModel only while the dialog is open, never stored.
- "Configure colors" of the app bar's menu: Grocy's user settings `calendar_color_*` of the features
  the server has; a color of the palette (Grocy's) or any typed as "#rrggbb", saved at once and online
  only like Grocy's, a failure shows a message. The dialog closes with "Close".
