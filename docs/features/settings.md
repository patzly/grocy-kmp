# Settings

The settings categories and pages, appearance and language. Moved from CLAUDE.md.

Settings plan: the overview lists the categories General (language, theme, contrast), Server
(server URL, Grocy version, reload configuration, logout), Grocy settings (loaded from the
server, Grocy's "Settings" menu), Network (loading timeout, Tor with Orbot, HTTP proxy), Behavior
(navigation, date and keyboard settings, Open Food Facts / Open Beauty Facts), Scanner (engine,
front camera, 2D formats, barcode formats, external scanners), Notifications (stock, chores)
and Shopping mode settings (`storefront`) as one group (`SettingsCategory`), then "Backup and restore" and "Reset all" as a second group.
Built so far: the overview, General (language, theme, contrast), Server, Shopping mode settings
(`ShoppingModeSettingsRoute`, also opened by the shopping mode's menu as a screen of its own
with `inSettings = false`: auto sync with its interval in seconds, at least
`ShoppingModeSettings.MIN_SYNC_INTERVAL_SECONDS`, saved once typing pauses and only when valid,
"Keep screen on" and "Show checked items at the bottom"; kept on the device in `SettingsStore`
(DataStore keys `shopping_mode_*`, `grocy.shoppingMode*` on the web), read at app start by
`ShoppingModeSettingsRepository`), Grocy settings
(`GrocySettingsRoute`, the wrench icon of Grocy's menu: a note that Grocy keeps them for the user
on the server, then the pages of Grocy's settings menu with settings of the user in its order:
stock settings (always, like Grocy), shopping list, recipes, chores, tasks and batteries
settings (hidden like Grocy's entries by feature flag), then Grocy's "User settings" as a group
of its own) and the stock settings (`StockSettingsRoute`, also
opened by "Presets for new products": in wide windows the products open the categories, Grocy's
settings and the stock settings at once (`Navigator.navigateAll`), so they look like reached
through the settings, and back (`StockSettingsRoute.backLeavesSettings`, a
`detailPaneMetadata(backLeavesScene)` that `NestedDetailSceneStrategy` lets leave the whole
scene, and the page's back button) returns to the products in one step). The other category pages, backup and reset follow;
`settingsEntries` wires their clicks to nothing until then.
- Wide windows show the categories as list-detail scene like the master data: the detail pane
  says to choose a category (`settings_select_hint`) until a page is open; the pages are
  `DetailRoute`s opened with `openDetail`, the categories then single choice items marking the
  shown page (`settingsCategoryOf`). A page beside them has neither back button nor menu (the
  categories' bar has both). A page opened from a page (`StockSettingsRoute(inSettings =
  true)`) takes its place beside the categories with a back button to it; opened from the
  products (`inSettings = false`) it is a screen of its own.
- Stock settings (`stock/`): every setting of Grocy's stock settings page in its groups, hidden
  by feature flag; each change is saved at once (`MasterDataRepository.saveUserSetting`,
  `UserSettingValue`), typed numbers once typing pauses and only when valid (Grocy's ranges,
  `StockNumberSetting`); a failed save returns to the stored value with a message
  (`UserSettingSaver`, `UserSettingEventEffect`, shared by every page of Grocy's settings).
- The other pages (`grocy/`, `GrocySettingsPageRoute(GrocySettingsPage)`): one generic page
  described as data (`GrocySettingsPage.groups`: Grocy's headings, checkboxes as switches with
  Grocy's help texts, "Due soon days" from 0 as number picker with its help below the buttons,
  the shopping list for products below their min. stock amount only with several lists and
  enabled like Grocy's choice), saved like the stock settings.
- Grocy's "User settings" (`user/`, `UserSettingsRoute`): the user's language in Grocy
  (`locale`, Grocy's codes such as "pt_BR" of its `localization` folders, `GrocyLanguages`,
  empty for its default, the browser's language), chosen in the language dialog of General
  (`GrocyLanguageDialog`, names in their language); disabled on demos, which always show the
  language of their address.

- Server (`ServerSettingsScreen`): on the demo server first a group of its own with one
  non-clickable item in `errorContainer`/`onErrorContainer` (warning icon, "Public demo server",
  supporting text that everything entered is public until the demo resets its changes once a
  day); then server
  URL and Grocy version, then "Reload configuration" and "Logout". On a demo the URL opens
  `DemoServerDialog`: all of Grocy's demos (`DemoServers` of `core:model`, the language menu the
  demos show themselves, e.g. `https://de.demo.grocy.info`; `demoServerFor` maps app tags such as
  "nb" and "iw"), named like the language dialog. `DemoServerSwitcher` checks the chosen demo
  (`AuthRepository.verify`, a demo that is down never replaces a working one), logs in to it
  (clears the data) and downloads its configuration. The version checks Grocy's newest GitHub
  release (`ServerRepository.checkGrocyVersion`: the server's version downloaded anew, the
  stored one offline, compared with `releases/latest` of `grocy/grocy`): up to date or a newer
  version with a link to its release as `TextDialog`, a failed check as snackbar. `ServerRepository` (`core:data`, eager `StateFlow` in the app scope) combines the
  stored connection with the stored `SystemInfo` (`server_values` key `system_info`, written by
  every `MasterDataRepository.refresh`); opening the screen downloads the version again quietly
  (an update of the server changes it). Reloading downloads version, `/system/config` and user
  settings (`MasterDataRepository.reloadConfiguration`) and then asks whether to restart now
  (`PlatformDependencies.restartApp`: a new process on Android, a page reload on the web, which
  keeps the back stack); failures show as snackbar. Logout asks first (logging in again needs
  address and API key) and goes through `AuthRepository.logout`; the screen keeps showing the
  server while it slides out.

- Every settings screen uses `SettingsScaffold` (`feature:settings:impl`): `SegmentedListScreen`
  with back button and the same overflow menu (About, Help, Send feedback, Show logcat / Show
  log) plus its dialogs. The menu item shapes (`itemShape(i, 4)` in the first group,
  `itemShape(1, 2)` for the single log item) look wrong but are the only way the ripples of the
  expressive menu groups come out right; keep them.
- The log item opens `LogRoute`, named by `LogReader.source`, with reload and copy. Android
  ("Logcat"): `LogcatReader` reads the newest 300 error lines of the app's own process
  (`logcat *:E -t 300`). Web ("Log"): browsers do not let a page read its console, so
  `BrowserErrorLog` (`core:data`, installed by `WebPlatformDependencies` at start) records
  `console.error`, uncaught errors and unhandled rejections, logcat-like
  (`MM-dd HH:mm:ss.SSS E ...`), the newest 300 lines in `sessionStorage` (survives a reload, not
  closing the tab or app). `redactSecrets` removes API keys (the web sends them in the URL),
  access tokens, Bearer tokens and ingress sessions before anything is stored.
- Appearance (`AppearanceSettings`: theme, static/dynamic color, hue, palette style, contrast)
  lives in `SettingsStore` (DataStore keys `theme`, `color`, `hue`, `style`, `contrast`;
  `grocy.theme` etc. on the web), read once at app start by `AppearanceRepository` (eager `StateFlow` in the app scope of
  `AppGraph`), which `GrocyApp` passes to `GrocyTheme(appearance)` and the General screen shows;
  nothing is drawn until it is read, so no default flashes up. Dynamic color shows only where
  `isDynamicColorAvailable` (Android 12+); its contrast follows the system from Android 14 on
  (`dynamicColorFollowsSystemContrast`), so the contrast buttons are disabled with dynamic
  color. `HueSlider` (`core:design`) is the rainbow slider, colored with the same chroma and
  tone as the theme's seed; it vibrates exactly once per step (`SliderStepTracker`: drags
  report a step on every move) and ignores the stored hue while it is dragged or pressed (the
  value arrives late and would move the thumb back, which ticked twice). Below it a button group chooses MaterialKolor's `PaletteStyle`
  (`AppPaletteStyle`: tonal spot, neutral, vibrant, rainbow, monochrome; the ones that do not
  fit go into its overflow menu; stored styles that were removed fall back to tonal spot). Changing light/dark, color source, style or contrast fades the whole
  app into the new colors (`animateColorScheme`: one animation blends every color of the
  scheme); hue changes apply at once, so dragging the slider never piles up animations.
- Language (`AppLanguageStore`, null = system language). The translations are the tags in
  `AppLanguages` (core:model, English first as the base in `values/`); `locales_config.xml` of
  apps:shared lists the same and the Android build reads its `localeFilters` from it;
  `LocalesConfigTest` checks the lists, that every language has its `values-<tag>/strings.xml`
  and that translations keep the placeholders of English. The dialog shows only the names, from
  the platform in the language itself (`languageName`: `Locale.getDisplayName` /
  `Intl.DisplayNames`), sorted by the collation of the app language
  (`languageNameComparator`: `Collator` / `localeCompare`, so "Čeština" follows "Català"); it
  opens scrolled to the chosen language, `DemoServerDialog` to the current demo; a regional system language resolves to its base translation
  (`resolveAppLanguage`, "de-CH" uses "de"). Choosing a language in the app while logged in to
  a demo first asks whether to switch to the demo in that language too (only if one exists and
  it is not the current demo; "System default" means the language the app will show, from
  `systemLanguageTag`: Android's system resources, on the web the browser language that
  `index.html` keeps in `window.grocySystemLanguage` before overriding it). The question comes
  before the language is applied, which restarts the screen; switching applies the language
  only once the demo answered, "Keep current" or dismissing applies it right away. Changes from
  Android's per-app language settings never ask. Compose Multiplatform takes the text language from
  `Locale.getDefault()` on Android, so `AndroidAppLanguageStore` works like Tack: from Android 13
  on `LocaleManager` (system settings show it, the system recreates the activity); before, the
  tag is stored in a plain preference, `MainActivity.attachBaseContext` wraps the context and
  sets `Locale.setDefault`, and a change recreates the activity. No AppCompat. Compose reads the
  text language once, so the web keeps it in `localStorage` (`grocy.language`), `index.html`
  overrides `navigator.language(s)` with it before the app starts, and a change reloads the
  page (the `languagechange` event alone does not update Compose's texts).
- Android only lists the app in the system's per-app language settings when the app manifests
  (`android-foss`, `android-play`) declare `android:localeConfig`: the manifest merger rejects
  the attribute from libraries such as `apps:shared`. `LocalesConfigTest` checks both manifests.
