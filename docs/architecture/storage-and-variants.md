# Storage and variants

Credential and settings stores, and the FOSS vs Play variant. Moved from CLAUDE.md.

## Storage

- `CredentialStore`: Android uses DataStore with values AES-GCM encrypted by an Android Keystore
  key (`KeystoreCipher`); the credentials file is excluded from backups. Web uses `localStorage`
  (no secure storage available). Both serialize via `ServerConnectionCodec` (JSON, polymorphic
  `ServerConnection` with `@SerialName` discriminators "demo" / "api_key" / "home_assistant").
- `SettingsStore`: DataStore on Android, `localStorage` keys prefixed `grocy.` on web. Holds
  `checkUnlockKey` and `scannerEngine` (stored by enum name, unknown values fall back to ZXing),
  the appearance, the stock overview's table options (`StockOverviewOptions.of`: unknown
  values of a newer version fall back to the defaults) and the shopping list's
  (`shopping_list_fields`/`shopping_list_grouping`, on the web `grocy.shoppingListFields`/
  `grocy.shoppingListGrouping`).
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
(`core:update-play`) through the `appUpdater` parameter.
