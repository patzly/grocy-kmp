# CLAUDE.md

Grocy KMP is a client for [Grocy](https://grocy.info), the self-hosted groceries and household
management server. It is a full rewrite of the old Grocy Android app in Kotlin Multiplatform with
Compose Multiplatform. Targets: Android (phone, Wear OS) and Web (Kotlin/Wasm). No iOS or desktop.

Current state: login (demo, QR code, manual, Home Assistant ingress on Android), the barcode
scanner, update checks, the start screen, About, the settings (General, Server, Grocy settings),
Grocy's stock overview, master data with all their forms, the stock bookings and the shopping list.

## Details

Read the matching file before working on one of these areas; CLAUDE.md keeps only what most
tasks need.

- `docs/features/start-screen.md`: the start screen and the full list of what is built.
- `docs/features/stock-bookings.md`: Purchase, Consume, Transfer, Inventory (`booking/`).
- `docs/features/shopping-list.md`: the shopping list and its offline check offs.
- `docs/features/stock-overview.md`: Grocy's stock overview page.
- `docs/features/master-data.md`: master data lists and forms, list-detail scenes, menus, pull to
  refresh, products, product overview, price chart, barcodes, grocycodes, stock pages.
- `docs/features/settings.md`: settings categories, Grocy settings, server, appearance, language.
- `docs/architecture/web-navigation.md`: browser history, entry decorators, navigationevent.
- `docs/architecture/network.md`: API key placement, Home Assistant ingress.
- `docs/architecture/database.md`: schema history, the web worker database.
- `docs/architecture/updates.md`: update checks per platform, the web service worker.
- `docs/architecture/scanner.md`: barcode scanner engines, camera and permissions.
- `docs/ui.md`: logo, scrollbars, button groups, tooltips, pointer kind, web status bar, keyboard.

## Product requirements (binding)

These rules come from the project owner and apply to every change.

### Parity with the official Grocy web UI

- grocy-kmp is a native client for Grocy. Forms, lists, fields, columns, data and their order
  must match what the official Grocy web frontend shows, so users find their way immediately
  because everything works the same.
- Only the presentation differs: more intuitive and polished, with real Material 3 Expressive
  (e.g. segmented expressive lists as in `AboutScreen`).
- Exception: barcode scanning must work much better natively than in the web app.
- To find out how something works, read the official Grocy repository
  (https://github.com/grocy/grocy: OpenAPI spec, controllers, views, JS) and design the best
  client implementation from that. Do not rebuild or port the old grocy-android app.

### Composable quality

Composables must be finished work that leaves nothing to improve:

- Accessibility: semantics, content descriptions, headings, live regions, minimum touch targets,
  sensible focus and keyboard traversal order.
- Correct, current API usage and official formatting.
- Material 3 Expressive components, shapes, motion and typography.
- Haptic feedback on Android for every tap on a button, list item, toggle or menu item (not on
  text fields), usually `Confirm` via `haptic.confirm()`; use another `HapticFeedbackType` where
  it fits better. The shared components of `core:design` (`AppBarIconButton`,
  `ConnectedButtonGroup`, `VerticalButtonGroup`, `FormattedText` links, ...) vibrate themselves;
  wrappers around raw Material components must do it too. Long presses get `LongPress` from
  `combinedClickable` already. The web app has no haptics at all: browsers only offer
  `navigator.vibrate` at full strength without amplitude control, which feels like a cheap buzz
  (Compose's `Confirm` even vibrates twice there), so `apps/web` provides a silent
  `LocalHapticFeedback`.
- Text input never hides behind the keyboard, on every platform and in every window size: the
  scrolling container shrinks with the keyboard (padding from `WindowInsets.keyboard`, not
  content padding), so Compose keeps the focused field in view; on Android the change is
  animated in sync with the keyboard. `SegmentedListScreen` and `ScrollableAlertDialog` do this
  already.

### Screen and dialog design

Every screen should look as polished as `AboutScreen` (with its own content). Reference
implementations: `AboutScreen` and the login screens for screens, `FeedbackDialog` for dialogs.

Build screens with the shared components of `core:design`, which already apply every rule below:
`SegmentedListScreen` (scaffold, colored large app bar, `InsetLazyColumn`), `segmentedGroup`
(group with optional title as `StringResource`), `SegmentedContainer` (non-clickable item for
text fields, buttons, text or a scanner), `SegmentedListDefaults.colors()`/`.shape(index, count)`
for `SegmentedListItem`s, and `AppBarIconButton` for app bar actions.

- Screen background is always `surfaceContainer` (`Scaffold(containerColor = ...)`).
- Top app bar: `LargeFlexibleTopAppBar` (Material 3 Expressive) with the expanded title at the
  start and the collapsed one centered. Compose aligns both rows alike, so `SegmentedListScreen`
  centers them and lets the expanded row's title fill its width (text at its start), which puts
  it exactly where a start aligned title would be; the row is told apart by its text style.
- Top app bar: `surfaceContainer` while not scrolled, `surfaceContainerHighest` when scrolled
  (`TopAppBarDefaults.topAppBarColors(containerColor = surfaceContainer,
  scrolledContainerColor = surfaceContainerHighest)` plus a scroll behavior connected via
  `nestedScroll`).
- App bar actions (navigation icon included) are `FilledIconButton`s with `contentColor =
  onSurface` inside a `HapticTooltipBox`: `AppBarIconButton`, which `NavigateBackButton` and
  `AppBarOverflowMenu` use. Their container is always `surfaceContainerHighest`: it stands out
  on the `surfaceContainer` bar (`surfaceContainerHigh` was a single tone step above it, too
  faint) and becomes one with the scrolled bar.
- Nothing except group titles sits directly on the `surfaceContainer` background. Exceptions: the
  header of `LoginChoiceScreen` (logo, welcome title, subtitle, centered above the choices) and
  the primary action button that ends a form (`LoginManualScreen`'s Connect, full list width in
  an `insetItem`). A heading or explanation that belongs to one control goes inside its item as
  `SegmentedTexts` (list item headline/supporting styles), not as group title. All other
  content (text, values, text fields, buttons, switches, data, ...) lives inside Material 3
  Expressive segmented list items, logically grouped:
  - `InsetLazyColumn` with `insetItem` per group, `consumeWindowInsets(padding)` and content
    padding of the scaffold padding + 16.dp top and bottom; 16.dp between groups. Screens with
    text input also need the keyboard handling of `SegmentedListScreen`.
  - A group is a `Column` with `Arrangement.spacedBy(ListItemDefaults.SegmentedGap)`.
  - Items use `SegmentedListItem` (clickable) or `ListItem` with the content-lambda overload
    and `shapes` (non-clickable), shapes from `ListItemDefaults.segmentedShapes(index, count)`, colors from
    `SegmentedListDefaults.colors()`, which is Material's default `segmentedColors()` with the
    `surface` container (`SegmentedListDefaults.containerColor` for custom items).
  - Leading icons go in `LeadingContentWrapper`.
  - Group titles: `titleSmall`, color `secondary`, `semantics { heading() }`, 8.dp below.
- Dialogs, except the plain text dialog (`TextDialog`), are segmented lists too: build them with
  `ScrollableAlertDialog` + `ScrollableAlertDialogContent` from `core:design`, a short
  `bodyMedium`/`onSurfaceVariant` message on top if needed, then groups of segmented items in
  `surface` with 16.dp between groups, like `FeedbackDialog`.
- Always prefer the project's own components from `core:design` (`ScrollableAlertDialog`,
  `InsetLazyColumn`, `RoundedTextField`, `ConnectedButtonGroup`, `NavigateBackButton`,
  `HapticTooltipBox`, `LeadingContentWrapper`, ...) over raw Material components when they fit.

### Adaptive layouts and platforms

- Android apps must be fully optimized for phones in every format (portrait, landscape, split
  screen), foldables, tablets in every format with freely resizable windows, and desktops
  (Chromebook, Googlebook). Follow all of Google's adaptive and large screen best practices
  (window size classes, no orientation locks, keyboard, mouse and trackpad support, state kept
  across configuration changes) and all Material 3 / Material 3 Expressive rules for each form
  factor, for navigation, design and usability.
- Use Navigation 3 scene strategies for automatic list-detail layouts.
- The Wear OS app only shows and checks off the shopping list, but must follow all Material 3
  Expressive for Wear OS standards just as strictly.
- The web app must behave like the Android app wherever the browser allows it, ideally including
  login and scanning.

### Permissions

Request a permission only at the moment it is needed (e.g. notifications when the user enables
them in settings, camera when the scanner opens), never on first launch. Handle grant, denial
and permanent denial correctly on Android and on the web.

### Security

- The app talks to the internet, so security must be state of the art: no new vulnerabilities,
  secrets encrypted at rest and never logged, safe defaults everywhere.
- Use stable dependency versions only. The exception is the design stack: Compose
  Multiplatform and the Material 3 Expressive libraries use the newest pre-release.

### Data and offline behaviour (Android and Wear OS)

- Database: Room 3 (`androidx.room3`, Kotlin Multiplatform), stable since 3.0.0. Use the
  latest stable 3.0.x, not the 3.1 alphas. It also ships Wasm artifacts.
- Follow Google's offline-first architecture: the local database is the source of truth for the
  UI, the network syncs into it.
- Transactions that change server state through calculations (consume, purchase, open, transfer,
  inventory, ...) are online-only. When offline, show an offline error. Never calculate the new
  server state locally.
- Exception: checking off shopping list items must work offline. Persist the local state and
  queue the actions, then run them against the server as soon as the device is online again.
- Syncing must not overwrite changes made by others in the meantime (items added, removed or
  changed by someone else). Replay the user's own per-item actions against the current server
  state and refresh afterwards, so the result makes sense for everyone involved.

### Testing

- The project must always be thoroughly tested, so every error shows up immediately.
- Every change comes with its tests: new code gets tests, changed behaviour gets updated tests,
  fixed bugs get a test that would have caught them. All tests must pass before a step is done.
- Choose the most useful and efficient kind of test per case; there is no coverage target.
  Unit tests of logic, ViewModels, repositories, parsers and the network layer are the baseline.
  Screenshot and UI tests are overkill for now.
- Code that is hard to test gets refactored to be testable (inject dispatchers, time sources,
  storage, HTTP engines) instead of being left untested.
- Placeholders get no tests; their tests come with the real implementation.

### Wear OS login

- Nobody wants to type an API key on a watch. Primary path: automatic zero-click sync of the
  server connection from the phone app to the watch in the background (Wearable Data Layer).
- Fallback, when the phone has no Play Services or automatic pairing fails: entry through the
  phone's system keyboard (remote input on the phone).
- Wear OS always has Google Play Services, so the watch app may depend on them. Requiring the
  Play phone app as companion for the automatic sync is acceptable, since Wear OS users need
  Google's Wear OS app on their phone anyway.

## Build and run

Any recent JDK can launch the wrapper; Gradle downloads JDK 21 for the daemon itself via foojay
(`gradle/gradle-daemon-jvm.properties`). Android modules need an Android SDK (`sdk.dir` in the
git-ignored `local.properties` or `ANDROID_HOME`); AGP downloads missing platforms and build tools
once the SDK licenses are accepted. On Linux the Node.js that Kotlin/Wasm downloads needs
`libatomic`.

```bash
./gradlew :apps:android-foss:assembleDebug        # FOSS phone APK
./gradlew :apps:android-play:assembleDebug        # Play phone APK
./gradlew :apps:wear:assembleDebug                # Wear OS app
./gradlew :apps:web:wasmJsBrowserDevelopmentRun   # web app dev server
./gradlew :apps:web:wasmJsBrowserDistribution     # web bundle -> apps/web/build/dist/wasmJs/productionExecutable
./gradlew :apps:android-foss:assembleNightly      # nightly build type (.nightly suffix, own icon/label)
./gradlew :core:network:compileKotlinWasmJs       # quick compile check of one module for Wasm
./gradlew allTests                                # all tests on JVM and Wasm (what CI runs)
./gradlew :core:network:allTests                  # all tests of one module
./gradlew :core:network:testAndroidHostTest       # JVM only, fastest feedback
./gradlew :core:network:wasmJsNodeTest            # Wasm only
```

Test reports: `<module>/build/reports/tests/`. There is no lint/detekt/ktlint setup yet. Before a
step is done, run `allTests` and build the apps whose code changed.

- Configuration cache, build cache and parallel builds are on (`gradle.properties`). Keep build
  logic configuration-cache compatible (use `providers`, no `project` access at execution time).
- `kotlin-js-store/wasm/yarn.lock` updates itself locally; with `CI` set an outdated lock fails
  the build, so npm dependencies only change through a reviewed commit. Commit lock file changes
  after changing npm dependencies. The local lock must equal the one a clean CI checkout computes:
  every workspace `package.json` is regenerated on each build (the Compose convention plugin
  disables Wasm test tasks but keeps the `PackageJson` tasks, stale ones kept old versions in the
  lock). On a failed check `checks.yml` prints the lock diff to commit.
- Node.js, Yarn and Binaryen downloads come only from the repositories in `settings.gradle.kts`
  (`downloadBaseUrl = null` in the root build and `useDeclaredWasmToolingRepositories()` in the
  convention plugins), so Kotlin adds no project repositories. The remaining Gradle deprecation
  warning (`Configuration.setVisible`, removal in Gradle 11) comes from AGP itself.
- App version lives only in root `gradle.properties` (`grocy.versionName`, `grocy.versionCode`),
  shared by all apps. The web app gets it through a generated `BuildInfo.kt`
  (`apps/web/build.gradle.kts`). Wear adds `1_000_000` to the version code.

## Module layout

```
apps/
  shared/        KMP: GrocyApp (root composable), AppGraph (manual DI), PlatformDependencies,
                 Android MainActivity, launcher icons, themes, splash, backup + network config
  android-foss/  Android app, FossApplication -> AppGraph(isPlayVariant = false)
  android-play/  Android app, PlayApplication -> AppGraph(isPlayVariant = true), <queries> for unlock app
  wear/          standalone Wear OS app (Android only, not KMP, own placeholder UI)
  web/           Kotlin/Wasm browser app, main() + index.html loading screen
core/
  model/         plain models: ServerConnection, User, SystemInfo, UnlockState, theme enums
  network/       Ktor client, GrocyApi, GrocyApiException, ServerUrl.normalize, DTOs
  data/          AuthRepository, UnlockRepository, CredentialStore, SettingsStore (+ platform impls),
                 MasterDataRepository
  database/      Room 3 GrocyDatabase (copy of the server's data), DAOs, entity mappings,
                 web-worker/ with the SQLite worker of the browser
  resources/     Compose Resources: strings, drawables, fonts, text files; public Res class
  design/        GrocyTheme (Material 3 Expressive, MaterialKolor), typography, shapes, components
  navigation/    Navigator, Route, GrocyNavDisplay (Navigation 3)
  ui/            cross-feature UI: AppDialog/AppDialogHost, feedback/help/unlock dialogs, share
  forms/         form and list building blocks of the features: TextItem, DropdownItem,
                 userfields, ObjectFormViewModel/Screen, object actions, filter chips, ChangeError
  scanner/       BarcodeScanner composable, BarcodeDecoder interface, ZXing on Android + Web
  scanner-mlkit/ ML Kit implementation of BarcodeDecoder, closed source, only for android-play
  update-play/   Google Play in-app updates (PlayAppUpdater), closed source, only for android-play
  testing/       test fakes (FakeGrocyServer, FakeCredentialStore, ...), only a commonTest dependency
feature/<name>/
  api/           @Serializable routes + <name>RouteSerializers (no UI)
  impl/          screens, ViewModels, <name>Dependencies interface, <name>Entries/Entry function
```

Dependency direction: `apps` -> `feature:*:impl` -> `feature:*:api` / `core:*`.
`impl` modules never depend on other `impl` modules; to navigate to another feature, depend on
its `api` module (e.g. `feature:start:impl` uses `AboutRoute` from `feature:about:api`).

### Convention plugins (`build-logic/convention`)

Apply these instead of raw plugins; IDs are aliased in `gradle/libs.versions.toml`:

| Alias | Use for | Does |
|---|---|---|
| `grocy.kmp.library` | non-UI KMP modules | KMP + `com.android.kotlin.multiplatform.library`, Android + `wasmJs { browser() }`, JVM 21, tests on JVM and Wasm |
| `grocy.kmp.compose` | KMP modules with Compose | above + Compose MPP + compiler, adds runtime/foundation/ui/preview, tests on JVM only |
| `grocy.android.application` | Android apps | AGP 9 built-in Kotlin, applicationId, signing, release/nightly types |
| `grocy.web.application` | the web app | wasmJs executable, output `grocy.js` |

- Android namespace is derived from the Gradle path: `:core:design` ->
  `com.patrickzedler.grocy.core.design` (`-` becomes `.`). Kotlin packages follow the same scheme.
- All Android apps share `applicationId = "com.patrickzedler.grocy"` (phone and watch must match
  for the Wear Data Layer). FOSS and Play are separate apps with the same ID, not flavors.
- Modules that ship Android resources/assets (`apps:shared`, `core:resources`) need
  `androidResources.enable = true` in the `kotlin { android { } }` block.
- Use `api(...)` only when a type of the dependency appears in the module's public API; such
  lines carry a comment explaining why. Everything else is `implementation(...)`.
- New modules must be added to `settings.gradle.kts`; reference them via type-safe accessors
  (`projects.core.data`). All versions go into `gradle/libs.versions.toml`.

## Architecture

### Dependency injection

Manual, no DI framework. `AppGraph` (`apps/shared/.../AppGraph.kt`) creates every shared object
lazily, once per process (Android `Application`, web `main()`). Each feature `impl` declares a
small `XxxDependencies` interface listing what it needs; `AppGraph` implements it as an anonymous
object. Platform specifics come in through `PlatformDependencies` (`AndroidPlatformDependencies`,
`WebPlatformDependencies`).

To add a feature: create `feature/<name>/api` + `impl`, add routes and a `SerializersModule`,
add a `Dependencies` interface if needed, wire it in `AppGraph`, register the serializers in
`routeSerializers` and the entries in `GrocyNavigation` (`apps/shared/.../GrocyApp.kt`), add
both modules to `apps/shared/build.gradle.kts`, and add the new routes to `allRoutes` in
`RouteSerializersTest` (`apps/shared/src/commonTest`).

### Navigation (Navigation 3)

- Routes are `@Serializable @SerialName("<url path>") data object/class XxxRoute : Route` in the
  feature `api` module. They end up in the browser history and URL, so never put secrets in them.
- Every route must be registered in the feature's `SerializersModule` under
  `polymorphic(NavKey::class)`. Wasm has no reflection, so a missing registration breaks state
  saving at runtime, not at compile time.
- Features only use `Navigator` (`navigate`, `goBack`, `replaceAll`, `openDetail`,
  `goBack(from)`, `currentRoute`), never the back stack. List-detail: see
  `docs/features/master-data.md`; web history: `docs/architecture/web-navigation.md`.
- Each feature exposes `fun EntryProviderScope<NavKey>.xxxEntries(dependencies, navigator)`.
  ViewModels are created there with `viewModel { XxxViewModel(...) }` and are scoped to the entry
  by `GrocyNavDisplay`.
- Login/logout is driven by `AuthRepository.authState`: saving or clearing credentials makes
  `GrocyApp` call `navigator.replaceAll(StartRoute | LoginChoiceRoute)`. Login screens therefore
  never navigate after success; they just call `authRepository.saveLogin(...)`.

### Screens and state

- Pattern: `XxxScreen(viewModel, callbacks)` collects state and delegates to a private stateless
  `XxxContent(...)`, which `@Preview` functions call inside `GrocyTheme`.
- ViewModels are `internal`, extend `androidx.lifecycle.ViewModel`, and expose either a
  `StateFlow<UiState>` (`LoginChoiceViewModel`) or Compose state with `private set` +
  `TextFieldState` for form input (`LoginManualViewModel`).
- Always rethrow `CancellationException` before catching generic exceptions in coroutines.
- Map `GrocyApiException` to UI error enums (`LoginError`) and resolve messages via string
  resources. Errors shown in UI get `liveRegion = LiveRegionMode.Polite`.
- Loading states use `withMinimumDuration(MINIMUM_LOADING_DURATION)` (`core:data` `util`) so
  indicators do not flicker.
- Navigation must be smooth on every platform: a screen shows its real values in its first
  frame, never a default that jumps to the stored value (sliders, button groups, items).
  Settings and state that screens show are read once at app start into an eager `StateFlow`
  of a repository (`stateIn(appScope, SharingStarted.Eagerly, null)`, e.g.
  `AppearanceRepository`, `UnlockRepository.unlockState`); ViewModels expose that flow
  directly instead of their own `stateIn(WhileSubscribed, null)`. Data from the network shows a
  loading state instead.

### Network

- One `HttpClient` per process (`createGrocyHttpClient()`), OkHttp on Android, Js engine on Wasm,
  JSON with `ignoreUnknownKeys = true`.
- `GrocyApi` methods take the `ServerConnection` per call. `ServerConnection.serverUrl` is the
  base URL without `/api` and without trailing slash; build it with `ServerUrl.normalize`, which
  only accepts http(s) URLs with a real host and no whitespace.
- Error handling: transport errors -> `Unreachable` (catches `Throwable`, Wasm fetch failures
  arrive as `Error`), 401 -> `Unauthorized`, other non-2xx -> `HttpError`, parse failures ->
  `NotGrocyServer`; Home Assistant: `HomeAssistantTokenRejected`, `AddonNotFound`,
  `NotHomeAssistant`.
- DTOs are `internal` in `core.network.dto` with a `toModel()` mapping into `core:model`.
- Android allows cleartext HTTP and user CAs on purpose (self-hosted servers), see
  `network_security_config.xml`.

### Local database (`core:database`, Room 3)

- Offline first: screens read `GrocyDatabase` through repositories (`MasterDataRepository` in
  `core:data`), `refresh()` downloads everything a screen shows in parallel and replaces the
  tables in one `withWriteTransaction`, so readers never see half an update. Changes are online
  only (`saveProduct`, `deleteProduct`, `saveLocation`, `saveChore`, ...: one `saveObject` per
  type posts or puts, sends changed userfields, then stores the object as the server returns it).
  `refresh()` downloads all master data at once; users need a permission of their own, a 403
  keeps the stored ones instead of failing the refresh.
- `AuthRepository` clears the database before a login and on logout (`clearLocalData`), so no
  screen shows the data of another server. The database file is excluded from backups.
- Server configuration and user settings live as JSON in `server_values`; only the known feature
  flags are kept (`ServerConfig`), the rest of `/system/config` (e.g. webhooks) never is stored.
- What only the server computes (product overviews: averages, spoil rate, price history) is
  stored as the last downloaded JSON per object (`product_overviews`, version 3), never derived
  from the other tables; a refresh deletes the ones of removed products.
- Room 3.0.x via the `grocy.room` convention plugin (KSP per target, schemas exported to
  `core/database/schemas`). Every schema change needs a new version and a migration; check it
  against the exported schema.
- Grocy sends numbers as numbers or strings depending on version: DTOs use `FlexInt`,
  `FlexDouble`, `FlexBoolean`, `FlexString`. Writes send JSON with exactly `application/json`
  (Grocy compares the header as string and rejects a charset) and get `GrocyApiException.
  HttpError.serverMessage` from Grocy's `error_message`. Browsers can write too: the preflight
  carries the API key in the query and Grocy answers it.

### Storage

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

### FOSS vs Play variant

Behaviour differences are expressed through nullability, not build flags: `unlockChecker` is
non-null only in the Play app, which makes `UnlockState` non-null and shows the unlock key
section, Play links, and "Recommend" pointing to Google Play. FOSS and web hide all of that.
`UnlockState.isUnlocked` (key installed or the check turned off) unlocks the extras, currently
the ML Kit scanner. Closed-source code only enters through `android-play`: it passes
`additionalBarcodeDecoders = mapOf(ML_KIT to MlKitBarcodeDecoderFactory)` to
`AndroidPlatformDependencies`; `apps:shared` never depends on `core:scanner-mlkit`. The same
goes for updates: `PlayApplication` wraps the GitHub updater in `PlayAppUpdater`
(`core:update-play`) through the `appUpdater` parameter.

### Barcode scanner (`core:scanner`)

- Always use `BarcodeScanner` as an item of a group; details in `docs/architecture/scanner.md`.

### Theming and UI

- `GrocyTheme(appearance)` wraps `MaterialExpressiveTheme`; colors come from MaterialKolor with a
  seed hue (`AppearanceSettings.DEFAULT_HUE = 154`), optional dynamic color on Android 12+,
  contrast levels.
  Compose Multiplatform and `compose-material3` are alphas on purpose, always the newest pair
  (newest Compose and Material 3 Expressive design state); many APIs need
  `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`, tooltips `ExperimentalMaterial3Api`
  (kept out of public APIs: `HapticTooltipBox` takes its own `TooltipPosition`). Compose 1.13
  needs compileSdk 37.1 or newer (`android-compileSdkMinor`). Material's adaptive libraries
  (`adaptive-navigation3`, list-detail scenes) follow the same rule: newest alpha.
- Colors of states outside the color scheme (Grocy's highlighted rows) come from
  `rememberStatusColors` (`core:design`): info (blue), warning (yellow), critical (red), fixed
  hues harmonized with primary and turned into container, content and color tones like
  Material's custom colors (90/10/40 light, 30/90/80 dark; `color` for small marks such as
  dots), always together with a text. Gestures on list items
  use `SwipeActionBox` (Gmail's look, the action runs on letting go beyond the threshold, the
  item springs back); every swipe action must also be in the item's menu and its accessibility
  actions.
- Version numbers (app, Grocy) and other numbers read as columns use tabular figures
  (`TextStyle.withTabularNumbers()`), every digit equally wide.
- Reuse components from `core:design` (`SegmentedListScreen`, `segmentedGroup`,
  `SegmentedContainer`, `AppBarIconButton`, `InsetLazyColumn` + `insetItem` with max width
  640.dp, `RoundedTextField`, `ConnectedButtonGroup`, `NavigateBackButton`, `HapticTooltipBox`,
  `TextDialog`, `FormattedText`, `FloatingToolbarWithFab` + `FloatingToolbarIconButton` for the
  floatingToolbar slot of `SegmentedListScreen` (snackbars never cover it: they lie above it
  and follow it while it slides out and back in, `snackbarLift`), `SegmentedSwitchItem`,
  `SupportingLines` for every multi-line supporting content in lazy lists (Material sizes items
  by the slot's baselines, a reused column kept those of its former line count, so items stayed
  too high), `SupportingBulletList` for a text with a bulleted list in that slot (wrapping
  lines; `FormattedText`'s lists bring paragraph spacing a list item does not want), `SearchableDropdownField` for every choice of objects, a combobox like Grocy's pickers: the
  text is the value (a text naming an option chooses it, an empty one chooses nothing, other
  text shows "Choose an entry from the list" once no suggestion is open and is reported through
  `onUnmatchedChange`, so forms refuse to save); the menu only suggests (fuzzy, no check marks,
  optional fields offer "None"), opens through its arrow or while typing and closes on a choice
  or once the text names an option completely;
  a tap on the field or its trailing action (scan) only focuses, the arrow never does (choosing
  needs no keyboard); its menu is one expressive menu group in a `DropdownMenuPopup`, not
  `ExposedDropdownMenu`, whose old menu padding sits outside the scrolling and cuts the items
  off, and no `ExposedDropdownMenuBox` either: it focuses the field whenever its menu opens), units of text fields go in the
  `suffix` slot of `RoundedTextField`. `SegmentedSwitchItem` is Material's checkable segmented item: while on it takes the selected
  shape and color (`secondaryContainer`) of the segmented defaults. Clicks trigger `haptic.confirm()`. `HapticTooltipBox`
  puts its modifier on an outer `Box`, because Material's `TooltipBox` loses parent data such as `align`.
  Tooltips show on long press, mouse hover and keyboard focus, never after a plain tap or click:
  Compose on the web focuses a clicked button, so `TooltipPressTracker` tells a pointer focus
  (`TooltipTrigger.PointerFocus`) from keyboard focus and long press. Focus counts as keyboard
  focus only while the keyboard is in use (`rememberKeyboardInput`: Android's input mode, on the
  web the last of `keydown` and `pointerdown`), because a screen opened by a click focuses its
  first button (the back button) by itself.
- Web pages show the hand cursor over clickable things, Compose does not: every button, item,
  chip, toggle, slider and link applies `Modifier.clickCursor(enabled)` (`core:design`, a no-op
  on Android, whose convention, also with a mouse on ChromeOS, keeps the arrow on buttons).
  New interactive components must apply it too.
- `UriHandler.tryOpenUri` instead of `openUri` when the target app might be missing.
- Platform differences use `expect`/`actual` with files named `Xxx.android.kt` / `Xxx.wasmJs.kt`.
  Wasm JS interop uses `@JsFun` with `@OptIn(ExperimentalWasmJsInterop::class)`.

### Resources and strings

- All UI strings, icons (`ic_rounded_*`), fonts and text files live in `core:resources`
  (`composeResources/`), accessed via `com.patrickzedler.grocy.core.resources.Res`. Do not add
  resources to feature modules.
- Icons are Material Symbols Rounded in their outlined form only (`FILL 0`, the `<name>_24px.svg`
  of `google/material-design-icons`, not `_fill1`), never filled variants; vectors use the
  960 viewport with y shifted by 960 like the existing files.
- Translations: English (`values/`) plus the languages of grocy-android's app resources (ca, cs,
  de, el, es, et, fr, he, hu, it, ja, nb, nl, pl, pt-BR, pt-PT, ru, sk, sv, tr, uk, zh-CN,
  zh-TW; regions as `values-pt-rBR`). Hebrew exists twice, `values-he` (browsers) and an
  identical `values-iw` (Android still reports the old code, Compose Resources match exactly);
  the Android `localeFilters` include `iw` too. Android lays RTL languages out right to left by
  itself, the web app sets `LocalLayoutDirection` and `<html dir>` from `isRightToLeft`
  (core:model). Claude writes the translations, they are
  uploaded to Transifex as "to review" and native speakers correct them there; corrections come
  back into the repository. Rules for writing one:
  - Use the terms of Grocy's own web frontend, from its translation in the Grocy repository
    (`localization/<language>/*.po`, e.g. "API-Schlüssel verwalten", "Bestand",
    "Hausarbeiten", "Einkaufszettel"), so the app and the web UI say the same.
  - Address the user like Grocy does in that language (German "du", French "vous", ...).
    The slogan "ERP beyond your fridge" stays English, so does Home Assistant's "Long-lived
    access token" (German "Long-Lived-Access-Token"). German "theme" is "Farbschema".
  - Keep every placeholder (`%1$s`); leave out `translatable="false"` strings.
  - A new language: `values-<tag>/strings.xml`, its tag in `AppLanguages` and in
    `locales_config.xml`; `LocalesConfigTest` fails until all three match.
  - Long texts in `files/` (changelog, licenses) have no language qualifiers and stay English.
- Mark URLs and names `translatable="false"`.
- Long texts (changelog, licenses) are files in `composeResources/files/`, rendered by
  `FormattedText`, which has its own markup: paragraphs split by blank lines, `#` headlines
  (`#_` for tabular numbers), `__` subtitles, `**bold**` / `<b>`, `=> ` / `> ` link lines,
  `? ` note and `! ` warning boxes in the size of the surrounding paragraphs (`bodyMedium` in
  dialogs), `?- ` / `!- ` boxes with small text (`bodySmall`).
- Android-only resources (launcher icon, splash, themes, `app_name_nightly`) are in
  `apps/shared/src/androidMain/res`.

## Testing

- Tests live in `src/commonTest/kotlin/` next to the code they test, same package. Platform-only
  code is tested in `wasmJsTest` (or `androidHostTest`) of its module.
- Where tests run: modules on `grocy.kmp.library` run `commonTest` on the JVM (Android host test)
  and on Wasm in Node.js, which catches platform differences (regex, exceptions, number
  formatting). Compose modules (`grocy.kmp.compose`) run tests on the JVM only, because Compose
  on Wasm needs Skiko in a browser; their Wasm test tasks are disabled entirely (linking those
  binaries costs minutes and gigabytes of memory). Keep platform-sensitive logic out of Compose
  modules when it can live elsewhere.
- JVM tests of Android code run against stubbed Android classes: avoid Android graphics types
  (e.g. outlines with a `Path`) and test plain data instead (`RoundRect.toCssBorderRadius`).
- Every module gets `kotlin-test`, `kotlinx-coroutines-test` and Turbine via the convention
  plugin; `ExperimentalCoroutinesApi` is opted in for test source sets. Add `projects.core.testing`
  to `commonTest` when fakes are needed.
- Fakes over mocks, there is no mocking library. `core:testing` provides:
  - `FakeGrocyServer`: Ktor `MockEngine` behind the real `GrocyApi`, answers like a healthy Grocy
    (shapes from the official `grocy.openapi.json`). Change answers with `respond(path, status,
    body)`, simulate network loss with `unreachable = true` or per scheme with
    `unreachableSchemes`, Home Assistant errors with `ingressFailure`; inspect
    `requests`/`requestedPaths`. Home Assistant connections get a fake ingress session and reach
    the same responses below `INGRESS_PATH`. It sends the key as header on every platform.
  - `FakeCredentialStore`, `FakeSettingsStore`, `FakeUnlockChecker`, `FakeAppUpdater`,
    `FakeAppLanguageStore`.
  - `FakeGrocyServer` also serves the master data of `MasterDataJson` (Milk, Bread, a disabled
    product, a sub product, units, barcode, stock, userfield, config, settings, the product
    overview of Milk with two prices, chores Vacuum and a disabled one, a battery, a task
    category, a userentity, users, a chore userfield, the shopping lists 1 "Shopping list" with
    notes and 2 "Hardware store", item 5 (Milk, 6 pieces shown as a Pack, note "Low fat") and the
    done note item 6 "Batteries"), answers POST of the chores' next assignments, of the shopping
    list endpoints (new items and lists get ids) and
    `respond(..., method = HttpMethod.Post)` per method, accepts PUT and DELETE with 204 and lists
    them with `requestsTo(path, method)`.
- Room needs a real SQLite: tests with a database live in `androidHostTest` (Node has no web
  worker) and use `createInMemoryGrocyDatabase(queryContext = testDispatcher)`; modules running
  them apply `grocy.sqlite.host.tests`, which loads the desktop SQLite library for the JVM.
  `RepositoryTestEnvironment` (`core:testing`, Android only) puts the repositories of the screens
  on `FakeGrocyServer` and such a database.
- `snapshotFlow` over a `TextFieldState` in a ViewModel needs `Snapshot.sendApplyNotifications()`
  in tests, which the frame clock does in the app.
- Dates, durations, currencies and percents of the app language are formatted by the platform
  in `core:model` `text` (`formatDate`, `formatRelativeTime`, `formatDuration`,
  `formatCurrency`, `formatPercent`: ICU and `java.text` on Android, `Intl` in browsers); the
  rounding (`timeSpanBetween`, `durationOf`, moment.js's thresholds like Grocy) and `epochDayOf`
  are common code. Android's ICU is stubbed in JVM tests, so tests never call the relative and
  duration formatters. Prices use the server's `CURRENCY` (`ServerConfig.currency`) and the
  user setting `stock_decimal_places_prices_display`.
- ViewModels get the app language as parameter (`appLanguage()` of `MasterDataEntries`,
  `LocalLocale`; a change restarts the app), composables read `LocalLocale`: Compose
  deprecated `Locale.current`, which no state backs. Builds have no compiler warnings of the
  project's own code.
- Fuzzy search (`FuzzySearch`) and number formatting (`formatDecimal`, `parseDecimal`,
  `platformDecimalSeparator`) live in `core:model`, so they are tested on JVM and Wasm.
- Network code is tested against `MockEngine` via `createGrocyHttpClient(engine)`, which uses the
  same configuration as production.
- Coroutines: wrap tests in `runTest`. Inject dispatchers (`UnlockRepository(checkDispatcher)`)
  and time sources (`withMinimumDuration(timeSource)`) instead of using globals; pass
  `testTimeSource` / test dispatchers in tests. Assert flows with Turbine (`flow.test {}`).
- ViewModels: `Dispatchers.setMain(StandardTestDispatcher())` in `@BeforeTest`, `resetMain()` in
  `@AfterTest`, create `FakeGrocyServer(requestDispatcher = dispatcher)` so requests finish in
  virtual time, then `advanceUntilIdle()`. `stateIn(WhileSubscribed)` state needs a collector:
  `backgroundScope.launch { viewModel.state.collect {} }`. Repositories with an eager app-scope
  flow get `backgroundScope + UnconfinedTestDispatcher(testScheduler)` as scope, so their state
  is there right away like at app start (work in plain `backgroundScope` does not run under
  `advanceUntilIdle`). Compose state and `TextFieldState`
  work directly in tests (`setTextAndPlaceCursorAtEnd`).
- Logic inside composables (parsing, mapping) is extracted into plain `internal` functions and
  tested there, e.g. `parseFormattedText` for `FormattedText`.
- Protocols with sockets are tested through a small message interface, e.g. the Home Assistant
  handshake with scripted `WebSocketMessages` (`IngressSessionHandshakeTest`).
- Decoders are tested with real codes: `ZxingBarcodeDecoderTest` renders QR and EAN codes with
  ZXing's writer (white quiet zone around them, linear codes need it) and decodes them, also
  turned by 90 degrees. Camera, permissions and ML Kit itself need a device and are not tested.
- Test names describe behaviour (`rejectedKeyShowsErrorAndAllowsRetry`); one behaviour per test;
  given/when/then separated by blank lines.
- Persisted formats get a test that decodes a fixed literal (see `ServerConnectionCodecTest`), so
  stored data of existing installations stays readable.

## Code style

- Always follow the official formatting standards: JetBrains Kotlin coding conventions and
  Google's Android Kotlin style guide.
- `.editorconfig`: 4 spaces, max line length 100, trailing commas allowed, no wildcard imports.
  A few Android-only files and some `build.gradle.kts` files still use 2 spaces; reformat them to
  4 spaces when touching them.
- Comments explain *why* in one short sentence, KDoc on public APIs. Keep this density.
- Sources go under `src/<sourceSet>/kotlin/` in nested directories matching the package
  (`com/patrickzedler/grocy/core/ui/`, not `com/patrickzedler/grocy.core.ui/`). Files outside
  `kotlin/` are silently not compiled.
- `internal` by default for screens, ViewModels, DTOs and helpers; public only what other modules
  use.

## CI and releases (`.github/workflows`)

- `checks.yml` (every push to `main`, every pull request): `./gradlew allTests`, uploads test
  reports on failure.
- `nightly.yml` (daily 02:00 UTC, skipped without new commits): signed nightly APKs for FOSS and
  Play, web app deployed to GitHub Pages (build id = commit SHA, see `docs/architecture/updates.md`), a reproducibility check that builds the unsigned FOSS
  release twice and compares bytes, then replaces the `nightly` GitHub release.
- `release.yml` (tag `v*`, must equal `v` + `grocy.versionName`): runs `allTests`, then signed
  FOSS APK + SHA256SUMS on the GitHub release, Play and Wear AABs as workflow artifacts.
- Signing reads `RELEASE_*` / `NIGHTLY_*` env vars (`KEYSTORE_PATH`, `KEYSTORE_PASSWORD`,
  `KEY_ALIAS`, `KEY_PASSWORD`); without them builds are unsigned, never debug-signed.
- Reproducible FOSS builds matter (F-Droid/IzzyOnDroid): `vcsInfo` and the dependency info block
  are disabled. Do not add build inputs that vary per machine, path or time.
- Actions are pinned by commit SHA; Dependabot updates them weekly.
