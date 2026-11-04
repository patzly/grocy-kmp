# Equipment

Grocy's equipment page (`/equipment`) and form (`/equipment/{id}`), `feature:equipment` with
`EquipmentRepository` of `core:data`. Everything hides with `FEATURE_FLAG_EQUIPMENT`.

- Routes (`feature:equipment:api`): `EquipmentRoute` is the list pane of a list-detail scene of its
  own (scene key "equipment"), `EquipmentDetailRoute(id, inList)` and `EquipmentFormRoute(id)` its
  details. `inList = false` is Grocy's "Expand to fullscreen", a screen of its own. Grocy selects the
  first row, the app shows the placeholder (`equipment_detail_hint`) like its other scenes: opening a
  manual downloads a file nobody asked for.
- Data (database version 13): `equipment` from `objects/equipment`. Saving is online only and stores
  the piece as the server answers. Manuals (file group `equipmentmanuals`) and file userfields
  (`userfiles`, the field's value `base64(stored)_base64(name)` as path) are downloaded when their tab
  shows and kept while the screen lives, not in the database: they can be large, so offline a tab
  says so with "Retry".
- List (`EquipmentScreen`, `EquipmentViewModel`, pure `buildEquipmentList`): the number of pieces below
  the title. Chips only with userfield columns (Grocy's table has only the name otherwise): sort, group
  by and shown fields, kept like Grocy's table options (`EquipmentOptions`,
  `EquipmentOptionsRepository`); file userfields are tabs, no columns, like Grocy. "Add" as FAB, the
  search in the toolbar. Rows: name, userfields as chips; Grocy's "Edit this item" on the row, "Delete
  this item" in the menu, swiping towards the start asks to delete as well (also in the accessibility
  actions). Deleting goes through `ObjectActionsViewModel`, the same question and errors as every
  object, and closes the piece shown beside the list.
- Details (`EquipmentDetailScreen`, `EquipmentDetailViewModel`): Grocy's tabs as connected buttons:
  "Instruction manual", "Notes", one per file userfield; it opens with the manual, without one with
  the notes (`firstTabOf`), like Grocy. Notes are HTML, shown as text (`htmlToPlainText`). Files go
  through `PdfDocumentView` (`docs/ui.md`); only what starts like a PDF is shown, whatever it is called
  (`isPdf`), everything else and every browser without a viewer says it can be downloaded. The app bar
  has Grocy's "Download file", "Expand to fullscreen" while beside the list, and the form.
- Form (`EquipmentFormScreen`, `EquipmentFormViewModel`, on `ObjectFormViewModel`): name (required,
  focused for a new piece), notes (plain text, unchanged notes keep their HTML), userfields, instruction
  manual: a tap chooses a PDF (`rememberFilePicker`), the item's menu deletes the current one; both
  happen on save like Grocy's ("The current file will be deleted on save"). A new file is uploaded
  first under a new name (Grocy never overwrites), so the piece never names a file the server does not
  have; a replaced or deleted file goes last and a failure there does not fail the save. File
  userfields stay read only in forms like everywhere in the app.
