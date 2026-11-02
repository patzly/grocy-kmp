# Chores

Grocy's chores overview (`/choresoverview`), chore tracking (`/choretracking`) and chores journal
(`/choresjournal`), `feature:chores` with `ChoresRepository` of `core:data`. The chore form and
list stay in `feature:masterdata`, the chores settings in `feature:settings`.

- Routes (`feature:chores:api`): `ChoresOverviewRoute` is the list pane of a list-detail scene of
  its own (scene key "chores"), `ChoreTrackingRoute(choreId, inList)` and
  `ChoresJournalRoute(choreId)` its details. The start screen opens the overview from its section
  and the tracking from the section's only button (`docs/features/start-screen.md`): in wide
  windows the overview with the tracking beside it, in narrow ones the tracking alone
  (`inList = false`), which stays a screen of its own when the window grows. The overview's
  placeholder is `chores_detail_hint`. The chore form and the settings are scenes of their own.
- Data (`ChoresRepository`, database version 11): `chores_current` (`/chores`, Grocy's view with
  everything the server computes: next execution, assignment, rescheduled, reassigned; chores
  without a schedule have no next execution, older servers answer "2999-12-31"), `chores_log`
  (the journal; a download of a date range replaces that range), `chore_details` (`/chores/{id}`,
  what the card shows, per chore) and the logged in user (`server_values`, for "assigned to me" and
  "Done by"). Tracking, undoing and rescheduling are online only and download what the server
  computed afterwards (`afterChange`, a failed download never fails the change); nothing about
  the next execution is computed locally.
- Overview (`ChoresOverviewScreen`, `ChoresOverviewViewModel`, pure `buildChoresOverview`):
  the number of chores below the title. Chips: status with the numbers of Grocy's colored messages
  in its menu ("Due soon" includes what is due today like Grocy's hidden column, and hides with 0
  due soon days), assignment ("Assigned to me" with its number first; only with
  `FEATURE_FLAG_CHORES_ASSIGNMENTS` and readable users), sort (name and shown fields), group by
  (assigned to) and, for both, the userfield columns except files and pictures (`TableColumn`,
  numbers order by value, rows without a value last, a hidden or deleted column falls back to the
  default), and the shown fields (Grocy's columns and its userfield columns), kept like Grocy's
  table options (`ChoresOverviewOptions`, `ChoresOverviewOptionsRepository`); filters and search
  only for the screen. Grocy sorts by next estimated tracking; chores without a schedule sort last
  in both directions (Grocy's text sort puts its "-" first, which pushes everything due down). The
  toolbar has the journal and the search, "Chore tracking" as FAB; the menu links to the chore list
  and the settings. The list downloads again when it shows again (`isShown`).
- Rows (`ChoreListItem`): the name, the fields as chips (`FieldChip`): next tracking relative
  ("In 3 days", with its time unless the chore tracks only dates) in Grocy's row colors (red
  overdue, blue today, yellow soon; `event_repeat` while rescheduled), last tracked, assigned to
  (`swap_horiz` while reassigned), userfields. A tap opens the chore overview. Grocy's main button stays on
  the row (the next schedule, now with Grocy's `chores_overview_swap_tracking_buttons`); swiping
  towards the end does the same, towards the start skips (off for chores without a schedule); both
  are also in the menu and accessibility actions. The menu follows Grocy's groups: the other way
  to track, skip, reschedule; chore overview, journal, edit; grocycode download and print.
- Actions (`ChoreActions`, `ChoreActionsHost`, shared by the list and the chore overview): Grocy's buttons send
  no user, so the logged in one counts. Messages carry "Undo", also on the overview, where Grocy has
  none: a swipe is easily done by mistake. "Reschedule next execution" is Grocy's dialog (date as
  it tracks, assigned to; "Reset" only while something was set); it PUTs `rescheduled_date` and
  `rescheduled_next_execution_assigned_to_user_id`, then lets Grocy assign again. Grocy's feedback
  "This can only be in the future" is not enforced by Grocy either, past dates pass.
- Chore overview (`ChoreOverviewHost`, `ChoreOverviewViewModel`): Grocy's "Chore overview" as a
  dialog like the product overview, no page: opened by a tap on a row of the overview or the journal,
  their menu entry and the info button of the tracking toolbar. The description, then one group
  with icons like the product overview's: chore name, next tracking and assignment, which Grocy's popup leaves to its row, tracked count,
  average frequency (humanized: `formatDuration`, hours below 22 like moment.js), last tracked
  ("never"), last done by ("Unknown"). The stored values show at once, every opening downloads them again. Buttons as
  on Grocy's card: journal and form, which open instead of the dialog (in the journal it filters
  that page); the menu has tracking both ways and skipping, which close the dialog for their
  message with "Undo", rescheduling and the grocycode.
- Tracking (`ChoreTrackingScreen`, `ChoreTrackingViewModel`): chore (active ones; typed, chosen or
  scanned, `grcy:c:<id>`), tracked time (now, a date only for chores that track no time), done by
  (the logged in user, with assignments), userfields of `chores_log`. "OK" as FAB, in the toolbar
  "Skip" (off for chores without a schedule), the chore overview (the chosen or last tracked
  chore, like the product overview on Purchase) and back to the top. Like Grocy's page the form
  starts over once tracked. Focus: `docs/focus.md`; `ScannerForm.CHORE_TRACKING`.
- Journal (`ChoresJournalScreen`, `ChoresJournalViewModel`, pure `buildChoresJournal`): Grocy's
  chore and date range filters (default one year) and search; rows: tracked time (red when tracked
  after its schedule), "Skipped", scheduled time, done by, userfields, "Undone on"; undone ones
  muted and struck through. "Undo chore execution" on the row. A tap opens the chore overview.
- The chore form shows Grocy's grocycode once saved (`GrocycodeItem`).
- Journal table options (`ChoresJournalOptions`, kept by `ChoresJournalOptionsRepository`): fields
  with Grocy's "Time of tracking" (`row_created_timestamp`) hidden by default, grouping by chore
  or done by (an unknown user last), sort by every shown column. Done by and its grouping need
  `FEATURE_FLAG_CHORES_ASSIGNMENTS`, rows without a user say "Unknown" like Grocy's.
