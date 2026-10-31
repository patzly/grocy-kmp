# Shopping list

Grocy's shopping list, including offline check offs. Moved from CLAUDE.md.

Shopping list (`feature:shoppinglist`, `ShoppingListRepository` of `core:data`):
- `ShoppingListRoute(listId)` is the list pane of a list-detail scene of its own (scene key
  "shoppinglist"), the item form `ShoppingListItemRoute(id, listId, productId)` and the list form
  `ShoppingListFormRoute(id)` its details (`DetailRoute`s, also on their own as quick add); both
  forms close after creating (`ObjectFormViewModel.closesWhenCreated`), a new item without list
  goes to Grocy's first list by name, a list created in its form becomes the shown one
  (`ShoppingListRepository.selectedListId`, kept for the process). The item form is titled
  "Add item" / "Edit item", Grocy's titles do not fit the app bar.
- The app bar's title names the shown list; with several lists it is Grocy's list selector
  (`SegmentedListScreen(onTitleClick)`, drop down arrow): `ShoppingListChoiceDialog`, single
  choice like the language dialog, "Name (count)" like Grocy's options.
- Rows: a checkbox checks off (in `LeadingTrailingContentWrapper`, so it keeps its distance from
  the top edge of items aligned to the top; done items struck through), a tap opens the item form, swipe
  towards the end checks off, towards the start "Add this item to stock"; the menu has Product
  overview, done/edit, add to stock and delete (no question, like Grocy). The amount chip is
  blue below the min. stock amount. Chips: status with counts, group by (none, product group,
  default store) and the shown fields (amount, product group, last price per unit and total,
  default store, barcodes, the userfields Grocy shows as columns), kept in `SettingsStore`
  (`ShoppingListOptions`, `ShoppingListOptionsRepository`; the status only for the process,
  `ShoppingListOptionsRepository.status`, shared with the shopping mode). The total value counts
  only the shown list; "Ungrouped" and "Not set" groups come last. `shoppingListState` builds
  the state of both screens.
- The overflow menu has Grocy's list actions (new, edit, delete list, notes edited as plain text
  in a dialog), clear list (asks) and clear done items, the stock actions (add products below
  min. stock amount, overdue/expired ones, "Add all list items to stock") and Print (only
  Grocy's thermal printer, `ServerConfig.thermalPrinter`). Left out on purpose: the month
  calendar, browser printing, collapsing groups, sorting by other columns than the name, and
  "create or assign product" for unknown barcodes (a scan only picks known products).
- "Add this item to stock" opens `PurchaseRoute(productId, amount, quId, shoppingListItemId)`,
  whose booking removes the item (`ShoppingListRepository.deleteItem`); "Add all list items to
  stock" opens one purchase after the other: when the list shows again, a removed item opens the
  next one, an item still there asks Grocy's "Adding shopping list item %s of %s" (saying that
  the item was not added to stock) with Skip or Cancel. The list counts as shown again only once
  the back stack has it on top (`isShown`): a back gesture shows it while it may still be
  cancelled, which flashed the question. The purchases get their place in the workflow
  (`PurchaseProgress`), which the purchase page shows.
- Checking off works offline (product requirement): `setDone` writes the local `done` and a
  queued entry (`pending_shopping_list_done`, the item id as key) in one transaction and sends
  it in the app scope; the replay PUTs only `{"done":0|1}`, drops the entry on a 4xx (the item is
  gone or refused, it is never recreated), keeps it on a 5xx or offline, and removes it only
  while its value is unchanged. Downloads put queued check offs on top of the server's state. The
  queue is sent on the next check off, on refresh, at app start (`AppGraph` init), whenever the
  device gets a network (`PlatformDependencies.networkAvailable`: Android's default network
  callback with `ACCESS_NETWORK_STATE`, the browser's `online` event) and, while an attempt
  failed (offline, 5xx), by itself after 15 s, doubled up to 5 min
  (`ShoppingListRepository.FIRST_RETRY_DELAY`, `firstRetryDelay = null` in tests that run until
  idle). `syncStates` tells per item whether its check off is being sent (`SENDING`) or its last
  attempt failed (`FAILED`, kept in memory, so after a restart it is sending until tried); the
  row shows it after the title like the stock overview's cart (18.dp, 6.dp gap): `sync` in
  onSurfaceVariant only after 500 ms (an answer right away flashes nothing), `sync_problem` in
  `error` at once, both read as text.
- The list's notes work offline too (`ShoppingListRepository.saveNotes`): stored at once with a
  queued entry (`pending_shopping_list_notes`, the list id as key), replayed like the check offs
  (PUT of only `description`, dropped on a 4xx, kept on a 5xx or offline, removed only while
  unchanged, the same retries); downloads and saved lists keep queued notes on top. The notes
  item shows `notesSyncStates` like a row its check off.
- Shopping mode (`ShoppingModeRoute`, `ShoppingModeScreen`, `ShoppingModeViewModel`), opened by
  the start screen's `storefront` button: a screen of its own with a close button, the list's
  name as title (Grocy's list selector with several lists), no subtitle, search or toolbar. Its
  overflow menu holds "Reload" and the checkable "Auto sync" in one group, then "Shopping mode
  settings". The shopping list's chips (status and table options are the same, not separate
  settings) and rows in large type (`LocalChipSize` large: title medium chips; titles in
  headline medium, wrapping; group titles in headline small emphasized), the total value below
  on the background in headline small with prices. A tap anywhere on a row checks it off (no
  swiping, no menu), sent at once where possible; done items move to the end of their group
  with the setting (`buildShoppingList(doneLast)`). Pull to refresh; with auto sync
  `syncWhileShown` downloads only the items (`ShoppingListRepository.refreshItemsOnly`, queued
  changes first) every interval while the screen is started, quietly offline, so several people
  can shop together. The screen stays on with the setting (`KeepScreenOn`).
- "Add to shopping list" and the item form share their fields (`ShoppingListItemInputs`,
  `ShoppingListItemFields`); `ShoppingListRepository.addProduct` keeps Grocy's merge of
  add-product.
