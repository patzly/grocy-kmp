# Batteries

Grocy's batteries overview (`/batteriesoverview`), battery tracking (`/batterytracking`) and
batteries journal (`/batteriesjournal`), `feature:batteries` with `BatteriesRepository` of
`core:data`. The battery form and list stay in `feature:masterdata`, the batteries settings in
`feature:settings`.

- Routes (`feature:batteries:api`): `BatteriesOverviewRoute` is the list pane of a list-detail scene
  of its own (scene key "batteries"), `BatteryTrackingRoute(batteryId, inList)` and
  `BatteriesJournalRoute(batteryId)` its details. The start screen opens the overview from its
  section and the tracking from the section's only button (`docs/features/start-screen.md`): in wide
  windows the overview with the tracking beside it, in narrow ones the tracking alone
  (`inList = false`). The overview's placeholder is `batteries_detail_hint`.
- Data (database version 13): `batteries_current` (`/batteries`, Grocy's view; batteries without an
  interval answer "2999-12-31", which means no next charge cycle, as does a battery never charged),
  `battery_charge_cycles` (the journal; a download of a date range replaces that range),
  `battery_details` (`/batteries/{id}`, per battery). A refresh also stores the batteries themselves
  (`objects/batteries`), the overview has no names of its own. Tracking and undoing are online only
  and download what the server computed afterwards (`afterChange`, a failed download never fails the
  change); nothing about the next charge cycle is computed locally.
- Overview (`BatteriesOverviewScreen`, `BatteriesOverviewViewModel`, pure `buildBatteriesOverview`):
  the number of batteries below the title. Chips: status with the numbers of Grocy's colored messages
  in its menu ("Due soon" includes what is due today like Grocy's hidden column, and hides with 0
  due soon days), sort (name and shown fields), group by (used in, Grocy's `allow-grouping` column)
  and, for both, the userfield columns except files and pictures, and the shown fields, kept like
  Grocy's table options (`BatteriesOverviewOptions`, `BatteriesOverviewOptionsRepository`); filter and
  search only for the screen. Grocy sorts by next charge cycle; batteries without one sort last in
  both directions (Grocy's text sort is no help there). The toolbar has the journal and the search,
  "Battery tracking" as FAB; the menu links to the battery list and the settings. The list downloads
  again when it shows again (`isShown`).
- Rows (`BatteryListItem`): the name, the fields as chips (`FieldChip`): used in, last charged, next
  charge cycle relative in Grocy's row colors (red overdue, blue today, yellow soon), userfields. A
  tap opens the battery overview. Grocy's "Track charge cycle" stays on the row; swiping towards the
  end does the same (also in the accessibility actions). The menu follows Grocy's groups: battery
  overview, journal, edit; grocycode download and print.
- Actions (`BatteryActions`, `BatteryActionsHost`, shared by the list and the overview dialog): the
  button tracks now without a question like Grocy's; messages carry "Undo", also on the overview,
  where Grocy has none: a swipe is easily done by mistake. Failures say so in a snackbar, where Grocy
  only logs them.
- Battery overview (`BatteryOverviewHost`, `BatteryOverviewViewModel`): Grocy's "Battery overview" as
  a dialog like the product overview, no page: opened by a tap on a row of the overview or the journal,
  their menu entry and the info button of the tracking toolbar. The description, then one group with
  icons: name, used in, charge cycles count, last charged ("never"), next charge cycle, which Grocy's
  popup leaves to its row. The stored values show at once, every opening downloads them again. Buttons
  as on Grocy's card: journal and form; the menu has tracking and the grocycode.
- Tracking (`BatteryTrackingScreen`, `BatteryTrackingViewModel`): battery (active ones; typed, chosen
  or scanned, `grcy:b:<id>`), tracked time (now; Grocy's "This can only be before now" is enforced,
  `FieldError.NotBeforeNow`), userfields of `battery_charge_cycles`. "OK" as FAB, in the toolbar the
  battery overview (the chosen or last tracked battery) and back to the top. Like Grocy's page the
  form starts over once tracked. Focus: `docs/focus.md`; `ScannerForm.BATTERY_TRACKING`.
- Journal (`BatteriesJournalScreen`, `BatteriesJournalViewModel`, pure `buildBatteriesJournal`):
  Grocy's battery and date range filters (default two years, Grocy's default here) and search; rows:
  battery, tracked time, userfields, "Undone on"; undone ones muted and struck through. "Undo charge
  cycle" on the row. A tap opens the battery overview. Table options
  (`BatteriesJournalOptions`): fields, grouping by battery, sort by every shown column.
- Notifications: `NotificationKind.BATTERIES`, see `docs/features/notifications.md`.
