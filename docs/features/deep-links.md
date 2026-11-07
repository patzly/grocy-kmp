# Deep links and NFC

`grocy://<page>?<parameter>=<id>` opens a page of the Android app from an NFC tag, a QR code, a
browser or another app (`DeepLinks`, apps:shared). Links only open a page: whatever it books the
user still confirms, so a link from anywhere cannot change data. Only while logged in; a link
before the login is ignored.

- Pages (case does not matter): `start`, `stockOverview`, `scan`, `purchase`, `consume`,
  `transfer`, `inventory` (each `?product=<id>`), `stockEntries`, `stockJournal` (each
  `?product=<id>`), `locationContentSheet`, `spendings`, `shoppingList` (`?list=<id>`),
  `shoppingMode`, `recipes`, `recipe?id=<id>`, `mealPlan`, `chores`, `choreTracking`
  (`?chore=<id>`), `tasks`, `task` (`?id=<id>`, without one a new task), `batteries`,
  `batteryTracking` (`?battery=<id>`), `equipment` (`?id=<id>`), `calendar`, `settings`.
  Examples: `grocy://consume?product=12` on a tag at the pantry,
  `grocy://choreTracking?chore=3` at the washing machine.
- A parameter that is no positive id spoils the link (nothing opens), parameters a page does
  not know are left out; forms opened by a link are on their own (`inList = false`), so back
  returns to where the user was.
- Manifest: `VIEW` with `BROWSABLE` for scheme `grocy` and `NDEF_DISCOVERED` for NFC tags with
  such a URI record, which Android opens without asking which app; `MainActivity` is
  `singleTask`, so a link from another app brings the app forward instead of a second copy. The
  activity hands `intent.dataString` to `AppGraph.openDeepLink`, which shows the page on top like
  a widget's link (`openedRoute`).
- Tags are written with any NFC app as URI record. The web app has its own URLs (route paths)
  and no `grocy://` scheme.
