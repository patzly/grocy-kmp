# Wear OS app

`apps:wear`: shows and checks off the shopping list. Screens and `WearGraph` there; logic in
`core:wear`, tested in `allTests`.

## Login

- Zero click: `PlayApplication` keeps the phone's login in a data item of the Wearable Data Layer
  (`PhoneConnectionPublisher`, path `/grocy/connection`), urgent, removed on logout. Phone and watch
  share application ID and signature, which is what lets the watch read it. The watch takes it
  from `PhoneConnectionService`, also while closed, and at start (`DataLayerPhoneLink`).
- A watch that finds nothing sends `/grocy/request-connection`, whose `WatchRequestService` only
  has to start the phone's process, which publishes at every start.
- `WearConnectionSync`: another server clears the watch's data, the same one keeps it. Logging out
  on the phone logs out the watch only if its login came from there (`PhoneLoginMarker`). Unreadable
  data and addresses `ServerUrl.normalize` would change are ignored.
- Fallback for the FOSS app and phones without Play Services: "Enter server" opens the system's
  text input for address and API key (`RemoteInput`), which on a watch offers the keyboard of the
  paired phone. Verified with `AuthRepository.verify` before it is stored. Home Assistant only
  arrives from the phone. "Demo server" needs no typing.
- Stored with the same `AndroidCredentialStore` as the phone; the app has no backups.

## List

- `WearShoppingList` (app and tile) builds what `WearViewModel` shows: Grocy's first list or the
  chosen one (kept across processes in `WearGraph`), like the phone's shopping mode with group
  titles (`ListSubHeader`), done items in a last group and amounts in the item's unit. Several
  lists make the title a button to choose (`ListChoiceScreen`).
- Options: the edge button at the end of the list opens `WearOptionsScreen`, one scrolling list
  because Wear OS has no menus: reload, auto sync, done items last, group by, status, sync
  interval. Stored on the watch with the phone's `ShoppingListOptions` and `ShoppingModeSettings`
  and their defaults. Left out: the shown fields (one line of details fits), keeping the screen
  on (battery), the status below min. stock amount (would need the whole stock).
- Offline first with the phone's `ShoppingListRepository`: a check off is stored and queued, replayed
  per item; a row shows the `sync` or `sync_problem` icon until the server has it.
- Coming to the front downloads the list, auto sync (on by default, every 10 s) the items while
  the app is visible (`syncWhileShown`). Master data never as a whole: what names, groups and
  counts items (`refreshShoppingListNames`) once per process and when an item names something
  missing.
- Compose for Wear OS Material 3: `AppScaffold`, `SwipeDismissableNavHost`, `ScreenScaffold` with
  `EdgeButton`, `TransformingLazyColumn`, `CheckboxButton`; rotary input comes with the list.
- `GrocyWearTheme`: the watch face's colors where the system offers them (`dynamicColorScheme`,
  API 35 and newer with the setting on), otherwise `WearPalette`, the phone's green in Wear's tones
  on black. Google Sans Flex from `core:resources`.

## Tile

`ShoppingListTileService` (`Material3TileService`, Wear OS Tiles): list name, a ring of what is
done, how many items are left and the next three, a tap on the card or the edge button opens the
app. Answers from the database; `refreshForTile` then downloads at most once a minute and
`WearGraph` requests an update whenever `tileSummary` changes, so check offs show up at once.
Freshness 15 min for changes made elsewhere. The system's dynamic theme or `WearPalette`; tiles
cannot load fonts, so the text asks the system font for full roundness (Wear OS 6 and newer). The
preview in the tile picker is `drawable-nodpi/tile_shopping_list_preview.png`, update it when the
layout changes.
