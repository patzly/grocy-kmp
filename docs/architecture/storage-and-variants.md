# Storage and variants

Credential and settings stores, and the FOSS vs Play variant. Moved from CLAUDE.md.

## Storage

- `CredentialStore`: Android uses DataStore with values AES-GCM encrypted by an Android Keystore
  key (`KeystoreCipher`); the credentials file is excluded from backups. Web uses `localStorage`
  (no secure storage available). Both serialize via `ServerConnectionCodec` (JSON, polymorphic
  `ServerConnection` with `@SerialName` discriminators "demo" / "api_key" / "home_assistant").
- `SettingsStore`: DataStore on Android, `localStorage` keys prefixed `grocy.` on web. Holds
  `checkUnlockKey`, `scannerEngine` (stored by enum name, unknown values fall back to ZXing),
  `scannerForms` (the forms that open with their scanner, enum names joined with commas),
  the appearance, the stock overview's table options (`StockOverviewOptions.of`: unknown
  values of a newer version fall back to the defaults) and the shopping list's
  (`shopping_list_fields`/`shopping_list_grouping`, on the web `grocy.shoppingListFields`/
  `grocy.shoppingListGrouping`), the notification settings and what was delivered
  (`NotificationSettings`, `NotificationDeliveries`).
- Sessions (`CredentialStore.sessions`, `SavedSession`, most recently used first; Android key
  `saved_sessions`, encrypted like the connection; web `grocy.savedSessions`): a logout keeps the
  login so it can be opened again without typing, only the bin of the sessions dialog
  (`AuthRepository.removeSession`) deletes the credentials. An account is unique per server and
  user, every demo counts as one (`SavedSession.isSameAs`). The watch keeps none
  (`keepsSessions = false`), its logout really forgets. Switching logs in anew: the database holds
  one server's data at a time, so it is cleared and downloaded again.
- Web stores access the browser only through `KeyValueStorage` (`BrowserLocalStorage` in
  production, an in-memory one in tests). Blocked or full browser storage must not crash the app:
  values are then kept for the session only.
- `ServerConnection.ApiKey`/`HomeAssistant.toString()` mask the secrets; keep secrets out of
  `toString`/logs.

## FOSS vs Play variant

Behaviour differences are expressed through nullability, not build flags: `unlockChecker` is
non-null only in the Play app, which makes `UnlockState` non-null and shows the unlock key
section, Play links, and "Recommend" pointing to Google Play. FOSS and web hide all of that.
`UnlockState.isUnlocked` (key installed or the check turned off) unlocks the extras, currently
the ML Kit scanner. Closed-source code only enters through `android-play`: it passes
`additionalBarcodeDecoders = mapOf(ML_KIT to MlKitBarcodeDecoderFactory)` to
`AndroidPlatformDependencies`; `apps:shared` never depends on `core:scanner-mlkit`. The same
goes for updates: `PlayApplication` wraps the GitHub updater in `PlayAppUpdater`
(`core:update-play`) through the `appUpdater` parameter. And for the watch: `PlayApplication`
publishes the login with `PhoneConnectionPublisher` (`core:wear`, `docs/features/wear.md`), the
FOSS app does not, its users log in on the watch through the phone's keyboard.
