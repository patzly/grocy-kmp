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

- `WearViewModel`: the first list by name or the chosen one, what is left at the top. Several lists
  make the title a button to choose (`ListChoiceScreen`).
- Offline first with the phone's `ShoppingListRepository`: a check off is stored and queued, replayed
  per item; a row shows the `sync` or `sync_problem` icon until the server has it.
- Coming to the front and the edge button download the list. Products and units only when an
  item names one that is missing (`refreshProductsAndUnits`), the rest of the master data never.
- Compose for Wear OS Material 3: `AppScaffold`, `SwipeDismissableNavHost`, `ScreenScaffold` with
  `EdgeButton`, `TransformingLazyColumn`, `CheckboxButton`; rotary input comes with the list.
