# Tasks

Grocy's tasks page (`/tasks`) and task form (`/task/{id}`), `feature:tasks` with `TasksRepository`
of `core:data`. Task categories stay in `feature:masterdata`, the tasks settings in
`feature:settings`.

- Routes (`feature:tasks:api`): `TasksRoute` is the list pane of a list-detail scene of its own
  (scene key "tasks"), `TaskRoute(id, inList)` the form beside it. The start screen opens the list
  from its section and a new task from the section's only button (`docs/features/start-screen.md`):
  in wide windows the list with the form beside it, in narrow ones the form alone
  (`inList = false`), which stays a screen of its own when the window grows. The placeholder is
  `tasks_detail_hint`. Everything hides with `FEATURE_FLAG_TASKS`.
- Data (`TasksRepository`, database version 12): `tasks` from `objects/tasks`, also the done ones
  Grocy's `/tasks` leaves out, so "Show done tasks" needs no download and works offline. A refresh
  also downloads the task categories and the logged in user. Saving, completing and undoing are
  online only and store the task as the server answers (a failed download never fails the change);
  saving never sends `done`, which only Grocy's own endpoints change. Deleting goes through
  `MasterDataRepository.deleteObject(TASKS, id)` and `ObjectActionsViewModel`, the same question
  and errors as every object.
- List (`TasksScreen`, `TasksViewModel`, pure `buildTasksOverview`): the number of tasks to do below
  the title. Chips: status with the numbers of Grocy's colored messages in its menu ("Due soon"
  includes what is due today like Grocy's hidden column, and hides with 0 due soon days), category
  (with "Uncategorized"), assignment, "Show done tasks", sort, group by (due date, category, assigned
  to) and the shown fields (Grocy's columns and its userfield columns), kept like Grocy's table
  options (`TasksOverviewOptions`, `TasksOverviewOptionsRepository`); filters and search only for
  the screen. "Clear filter" also ends "Show done tasks", which Grocy's button means to do but does
  not. Grocy sorts by due date; tasks without one sort last in both directions (Grocy's text sort
  puts them first, which pushes everything due down). The toolbar has "Create task" as FAB and the
  search; the menu links to the task categories and the settings. The list downloads again when it
  shows again (`isShown`). Sort and group by also offer the userfield columns (`TableColumn`), except files and
  pictures, which show no text: numbers order by value, rows without a value last; a hidden or
  deleted column falls back to the default.
- Rows (`TaskListItem`): the name (struck through and muted once done, with a state description),
  the fields as chips (`FieldChip`): due date relative in Grocy's row colors (red overdue, blue
  today, yellow soon; done tasks keep them like Grocy's rows), category ("Uncategorized" like
  Grocy), assigned to, userfields. A tap opens the form. Grocy's "Mark task as completed" or
  "Undo task" stays on the row; swiping towards the end does the same, towards the start asks to
  delete; both are also in the menu or accessibility actions.
- Actions (`TaskActions`, `TaskActionsHost`): Grocy's message "Marked task %s as completed on %s"
  carries "Undo", where Grocy has none: a swipe is easily done by mistake. Failures say so in a
  snackbar, where Grocy only logs them.
- Form (`TaskScreen`, `TaskFormViewModel`, on `ObjectFormViewModel`): Grocy's fields in its order:
  name (required, focused for a new task), description, due date (optional, date only), category
  (active ones and the one the task has), assigned to (the logged in user for a new task; hidden
  without readable users), userfields. A new task closes like Grocy's "Save & close"; "Save & add
  another task" in the toolbar saves and starts over with an empty form and the focus in the name
  (`generation`). The overflow menu deletes a saved task, deleting one shown beside the list closes
  the form.
