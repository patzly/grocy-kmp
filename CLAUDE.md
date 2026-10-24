# CLAUDE.md

Grocy KMP is a client for [Grocy](https://grocy.info), the self-hosted groceries and household
management server. It is a full rewrite of the old Grocy Android app in Kotlin Multiplatform with
Compose Multiplatform. Targets: Android (phone, Wear OS) and Web (Kotlin/Wasm). No iOS or desktop.

Current state: login (demo server, QR code of Grocy's API key page, manual Grocy URL + API key,
Home Assistant ingress on Android), the reusable barcode scanner (`core:scanner`), update checks
on every platform, the start screen (Master data item, the app menu), an About
screen, the Settings overview (`feature:settings`) and the master data
(`feature:masterdata`): the overview of Grocy's master data categories, the list and form of
every category (products with their overview dialog, locations, stores, quantity units,
product groups, chores, batteries, task categories, userfields, userentities). Barcodes, QU
conversions (of products and the default conversions of units), Stock entries, Stock journal
and "Presets for new products" are placeholders without action.

Master data (`feature:masterdata`), built after Grocy's web frontend:
- `MasterDataScreen` lists the categories of Grocy's "Manage master data" menu in its order and
  hides the ones whose feature flag is off (`MasterDataCategory.isAvailable`), from the eager
  `ServerRepository.serverConfig` (`MasterDataDependencies.serverConfig`), so they show in the
  first frame: on the web a refresh's write transaction keeps the single database connection
  busy for seconds while animations run, and reads wait behind it; Products shows
  the number of products once it is known (stored products or a finished download).
- Lists and their forms are list-detail scenes of Navigation 3 (`listPaneMetadata` /
  `detailPaneMetadata` of `core:navigation`, Material's `ListDetailSceneStrategy` in
  `GrocyNavDisplay`): from Material's expanded window width on (840.dp, the default pane
  directive) the form opens beside its list, before a choice `DetailPlaceholder` (`core:design`)
  fills the detail pane with `objects_select_hint`; narrower windows show list and form one after
  the other. Both panes start half and half with Material's `VerticalDragHandle` between them
  (`PaneAnchors` 1/2, 1/3, 2/3: drag, the screen reader's action or the arrow keys, a tick per
  snap); the pane expansion overrides the hinge avoidance, the middle lies on a book posture's
  fold anyway. Beside its list a form has no back button (the list's leaves both) and the list's
  small app bar in its color (`SegmentedListScreen(isBesideList)`), so both bars line up. Forms are `DetailRoute`s opened with `Navigator.openDetail` (replaces the form beside
  the list), lists leave with `goBack(from = route)`. A form with unsaved changes asks
  (`UnsavedChangesDialog`) also when another item of the list beside it is chosen or the list is
  left: `rememberLeaveGuard(route)` + `LeaveGuard.onLeaveRequest`. Beside a form
  (`showsDetailBesideList`) list items are single choice items marking the shown object
  (`ListDetailItem`), otherwise plain clickable items (no radio semantics in one pane).
- Overflow menus act on objects like Grocy's buttons and table menus (`ObjectActionsViewModel`,
  one per list and per form, `objectActionsOf`): every object can be deleted (Grocy's question,
  with its warning for products), products copied (`stock/products/{id}/copy`, the copy opens)
  and merged, chores merged (Grocy's merge dialog, the object preset as the one to keep, a
  refresh afterwards). Every list item has the narrow three dots button (`ListItemOverflowMenu`
  of `core:design`), products with "Product overview" as first group; forms put the actions as a
  group before Help and Send feedback (`SegmentedListScreen(actionMenuItems)`, the app menu
  without Settings and About: `rememberAppMenu()` without both). A deleted or merged away object
  closes its form. Forms get menu, leave guard, back and actions as one `FormFrame`.
- Date fields (date and datetime userfields, a chore's start date) have a calendar button
  (`DatePickerButton`): Material's date picker, for date and time then its time picker (dial or
  text input); the pure `withPickedDate`/`withPickedTime` write Grocy's format, typing still
  works.
- The other categories share one list (`objects/`): `ObjectListKind` describes each like its
  Grocy table (columns in Grocy's order to sort by, first column as default sort, e.g. Entity for
  userfields, "Show disabled" where objects can be disabled, the entity of "Configure
  userfields"), `ObjectListViewModel` turns the database into `ObjectRow`s (shown texts and
  numeric sort values per `ObjectColumn`, enum labels through `ListTexts`, product count of
  groups, charge cycle of batteries) and the pure `buildObjectList` filters, sorts (empty values
  last, ties by name) and searches every column. Userfields filter by entity (Grocy's entity list:
  OpenAPI's exposed entities, `userentity-<name>`, users; `UserfieldsRoute(entity)` from every
  "Configure userfields"), userentities link to their fields. The filter chips
  (`FilterChipRow`, `DropdownFilterChip`, `ShowDisabledChip`, `SortChip`, `ClearFilterChip`), the
  search item and the empty states (`ListParts.kt`) are shared with the product list.
- Forms of the other categories build on `ObjectFormViewModel` (load, refresh for unknown ids,
  validation, unsaved changes, userfields of `userfieldEntity`, `ObjectFormEvent`s) and
  `ObjectFormScreen` (save FAB, Scroll to top, userfield group, leave guard). Fields and rules
  follow Grocy's forms, each with pure `XxxValues.validate` and conversion tested:
  `SimpleObjectViewModel` for locations (freezer only with freezing), stores, quantity units
  (plural forms only for languages with more than two, `grocyPluralFormCount` of the app
  language; default conversions as placeholder), product groups, batteries and task categories;
  `ChoreViewModel` (Grocy's first period and assignment type for new chores, start now, hidden
  period fields set like Grocy's JS, assignment only with `choresAssignments`, consumed product
  with stock; saving lets Grocy calculate the next assignment), `UserfieldViewModel` (entity and
  type start empty, options for preset types, "Now / today" for dates) and
  `UserentityViewModel`. Saved objects close the form, created ones become their edit form.
- List pictures are stored for offline use: `MasterDataRepository.productThumbnail` keeps them
  in `product_thumbnails` (Grocy gives every new picture a new file name, so they never get
  outdated), the product list downloads the missing ones after each refresh
  (`downloadMissingThumbnails`), and a refresh deletes those no product shows anymore.
- Every screen except About and the settings has the app menu in its overflow menu: Settings,
  About, Help and Send feedback (`rememberAppMenu` + `AppMenuDialogs` of `core:ui`, the items go
  to `SegmentedListScreen(menuItems)`, which puts "Reload" as a group of its own before them).
  The feedback dialog reads the unlock state from `LocalUnlockState`, provided by `GrocyApp`.
  Ripples of grouped menus follow the group's corners through
  `menuItemShape(index, count, groupIndex, groupCount)`.
- Every list screen (start, master data, products) has pull to refresh through
  `SegmentedListScreen(isRefreshing, onRefresh)`: Material's own
  `PullToRefreshDefaults.LoadingIndicator`, sliding out from below the app bar, and "Reload" in
  the app bar's overflow menu (`AppBarOverflowMenu`, narrow three dots button) for keyboards and
  screen readers. With a mouse or trackpad (`LocalPointerKind`, below) pulling is off (a wheel
  only showed the indicator without refreshing) and "Reload" is a round icon button of the bar
  instead (only the three dots are narrow). These screens use the small centered app bar with `pinnedScrollBehavior` (no
  collapsing, which would fight with pulling), always in its scrolled color
  (`surfaceContainerHighest`), so the indicator visibly comes from below it; the others keep
  the large one, except while a mouse is used: its wheel has no gesture end the large bar could
  settle on, so every screen gets the small centered bar then, changing only its color.
  Single items of a menu group use `menuItemShape(0, 1)`: Material's standalone item shape has
  the small corners of middle items, so its ripple would not fill the rounded group. ViewModels run downloads through
  `RefreshController` (`core:data`: one at a time, offline/failed errors, `loadedOnce`);
  `RefreshErrorEffect` (`core:ui`) shows a failed download as snackbar.
- `ProductsScreen`: one segmented item per product (`itemSpacing = SegmentedGap`, a product with
  picture shows it at the item's end before the overflow menu button, `ProductThumbnail`, 56.dp with small corners, without
  `LeadingTrailingContentWrapper`, whose padding only icons and switches need), first the
  filter row directly on the background
  (`edgeToEdgeItem` of `InsetLazyColumn`: it scrolls sideways up to the screen's edges, the
  padding that lines it up with the items scrolls along) and, while open, the search as its own
  segmented item below it (full width, opens with Material's spatial spring from its top edge,
  so it can be focused, keyboard included, right away without scrolling the list; closing
  clears the focus at its start).
  `ProductFilterBar` has outlined Material 3 chips with a `surfaceContainerLow` container (they
  lie on the background) and no elevation in any state, one per filter: product group and status
  as dropdown filter chips that are selected and show the value while not "All", "Show
  disabled" as toggle chip, a sort chip, and "Clear filter" as assist chip with a reset icon
  at its start while a filter (not the search) is active; chips use the standard overloads with Material's 8.dp rounded rectangle, not the
  morphing `shapes` overloads whose tokens are full round). Back closes an open search first
  (`NavigationBackHandler`, also the browser's back) and stays on the list
  and a floating toolbar (Add as FAB, Configure userfields (the product userfields), Presets for
  new products, Scroll to
  top, Search last). Filtering, sorting and the fuzzy search are the pure `buildProductList`:
  in stock means `amount_aggregated > 0` of an active product, like Grocy's `stock_current`.
- `ProductOverviewDialog` (`overview/`) is Grocy's "Product overview" card, opened from the
  first group of each active product item's overflow menu (Grocy computes no overview for
  disabled products). The title is "Product overview"; on top of the scrolling
  content the card header's buttons as an expressive `ButtonGroup` of wide filled (primary)
  icon buttons with `animateWidth` and its default 12.dp spacing, the last ones in its overflow
  menu when the dialog is too narrow, so they are ordered by importance (unlike Grocy): Edit
  product, Add to shopping list (only with the shopping list feature), Stock entries and Stock
  journal; only Edit leads somewhere so far. Then every
  value of the card in its order, all as items with the label as text and the value as
  supporting text, the product name first in the stock group (with the loading indicator of a
  download over stored values), hidden like in Grocy by feature flag (prices: stock value, last/average price and
  price history; location: default location; best before dates: average shelf life),
  description (three lines, "Show more"), picture (1200 px, offline the stored list picture)
  and the price history. `ProductOverviewViewModel` is one per product list (`show`/`hide`, kept
  across rotations), every opening downloads `/stock/products/{id}` plus its price history
  (`MasterDataRepository.refreshProductOverview`, stored as JSON in `product_overviews`, so the
  last overview shows offline). The pure `buildProductOverview` resolves units (prices per price
  unit, per stock unit too when they differ), stores and the chart series.
- `PriceHistoryChart`: Canvas line chart after the dataviz rules: one 2.dp line per store with
  surface-ringed points, Grocy's dotted least squares trend, y axis from zero in round steps
  (`priceAxis`), first and last date below, legend from two series or with a trend. Colors come
  from `rememberChartColors` (`core:design`): a validated color blind safe categorical palette
  in fixed order with light and dark steps, harmonized with the theme's primary color by the
  strongest hue rotation (15 degrees like Material's harmonization, down to none) that keeps
  neighbours apart with normal vision and simulated protanopia/deuteranopia
  (`isDistinguishable`, the dataviz validator's OKLab checks); stores keep their color by id,
  stores beyond eight are drawn together as "Other stores". Tap, sideways drag, mouse hover and
  the arrow keys select a price (date, store, price as tooltip); screen readers get all prices.
- `ProductScreen` edits or creates (`ProductRoute(productId = null)`) a product with every field
  of Grocy's form, in its order, grouped, fields of disabled features hidden
  (`ProductFormFeatures`). The logic is pure and tested: `ProductFormValues.validate` (Grocy's
  required fields and number ranges), `toProduct` (hidden fields keep their values, an
  unchanged description keeps its HTML), `newProductValues` (presets), `linkedQuantityUnit`
  (the other units follow the stock unit). Saving is online only: an edited product closes the
  form like "Save & return", a created one turns the form into its edit form. Userfields are
  edited by type (`UserfieldInput`, only changed ones are sent; files and images read only),
  the picture through `ProductPictureDialog` (`rememberImagePicker` of `core:ui`), the parent
  product can also be scanned (barcode or grocycode `grcy:p:<id>`): the scanner opens inside the
  field's item below it (`DropdownItem(below)`, small corners, text field color). Pickers whose
  text names no option (`ProductFormState.unmatchedChoices`, `UserfieldInput.isUnmatched`) block
  saving. Leaving with unsaved changes (`ProductViewModel.hasUnsavedChanges`: form values,
  userfields, picture or unmatched text differ from the loaded or last saved state; back button,
  back gesture and browser back) asks in `UnsavedChangesDialog` (saveable, survives rotation):
  save (and leave once saved), discard, or cancel.

Settings plan: the overview lists the categories General (language, theme, contrast), Server
(server URL, Grocy version, reload configuration, logout), User settings (loaded from the
server: stock, shopping list), Network (loading timeout, Tor with Orbot, HTTP proxy), Behavior
(navigation, date and keyboard settings, Open Food Facts / Open Beauty Facts), Scanner (engine,
front camera, 2D formats, barcode formats, external scanners) and Notifications (stock, chores)
as one group (`SettingsCategory`), then "Backup and restore" and "Reset all" as a second group.
Built so far: the overview, General (language, theme, contrast) and Server. The other category
pages, backup and reset follow; `settingsEntries` wires their clicks to nothing until then.

- Server (`ServerSettingsScreen`): on the demo server first a red `errorContainer` `Card` (a
  plain card, not a list item, no icon: a `titleMedium` title and `bodyMedium` text) warning
  that everything entered is public until the demo resets its changes once a day; then server URL and Grocy version, then "Reload configuration" and
  "Logout". `ServerRepository` (`core:data`, eager `StateFlow` in the app scope) combines the
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
  tone as the theme's seed. Below it a button group chooses MaterialKolor's `PaletteStyle`
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
  `Intl.DisplayNames`); a regional system language resolves to its base translation
  (`resolveAppLanguage`, "de-CH" uses "de"). Compose Multiplatform takes the text language from
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
  content padding), so Compose keeps the focused field in view, and the change is animated in
  sync with the keyboard. `SegmentedListScreen` and `ScrollableAlertDialog` do this already.

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
- App bar actions (navigation icon included) are `FilledIconButton`s with
  `containerColor = surfaceContainerHighest`, `contentColor = onSurface`, inside a
  `HapticTooltipBox`; `NavigateBackButton` and the menu button in `AboutScreen` show how. When the
  bar scrolls it takes the same color, so actions and bar become one.
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
  `goBack(from)`, `currentRoute`), never the back stack. List-detail: see "Master data".
- Each feature exposes `fun EntryProviderScope<NavKey>.xxxEntries(dependencies, navigator)`.
  ViewModels are created there with `viewModel { XxxViewModel(...) }` and are scoped to the entry
  by `GrocyNavDisplay`.
- Web: `BrowserHistoryInput` (a `NavigationEventInput`, installed by `GrocyNavDisplay` through
  `BrowserBackIntegration`) mirrors the back stack into the browser history, one entry per
  screen (`SceneInfo`) plus one for an open dialog or popup. Browser back and the Android back
  gesture of the installed web app therefore go back inside the app and only leave it from the
  first screen. Forward moves are reverted. Every entry stores `{ index, stack }` (the back stack
  as JSON, `BackStackCodec`) and the URL shows the screen as hash: the route's `@SerialName` is
  its path (`#/settings/general`, the first screen keeps the plain address; `RouteSerializersTest`
  checks paths are URL-like and unique). A reload restores the entry's back stack
  (`rememberNavigator`, only if it starts with the start route of the current login state) and
  stays on its entry; on a dialog's entry it returns to the screen below. A pasted URL without
  stored back stack starts on the first screen. When the first screen changes in place (login),
  no handler reports it, so `onBackStackChanged` rewrites the entry. A back the
  browser animated itself (`hasUAVisualTransition`) skips the app's pop transition. Browsers
  expose no gesture progress, so the web cannot run Compose's predictive back animation.
  From `beforeunload` until the next `pageshow` `WindowBrowserHistory` moves nothing: closing a
  dialog while the page reloads (language change) would otherwise cancel the reload.
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
- API key placement (`sendApiKeyAsQueryParameter`, expect/actual): the `GROCY-API-KEY` header
  on Android, a query parameter of the same name in browsers. Grocy checks the key before its
  CORS middleware, so the preflight for a custom header fails with a 401 without CORS headers;
  with the query parameter GET requests need no preflight. Consequence in browsers: a wrong key
  looks like an unreachable server, because the 401 carries no CORS headers either.
- Home Assistant ingress (`ServerConnection.HomeAssistant`, Android only,
  `isHomeAssistantIngressSupported`): `WebSocketIngressSessionSource` authenticates on
  `/api/websocket` with the long-lived token, reads `ingress_entry` of the add-on
  (`supervisor/api` `/addons/<slug>/info`, default slug `a0d7b954_grocy`) and creates a session
  (`/ingress/session`); the REST proxy does not allow these endpoints. Requests then go to
  `<HA URL><ingress_entry>/api/...` with the cookie `ingress_session` plus the Grocy API key.
  `HomeAssistantIngress` reuses a session for 10 minutes after its last use (the supervisor ends
  it after 15) and `GrocyApi` retries a 401 once with a fresh session. Slug and ingress path are
  validated, so a server answer can never redirect the API key to another host.
- Browsers cannot use ingress at all (no CORS on ingress, session only as cookie, `Cookie` is a
  forbidden header); the web login explains the add-on's network port as alternative.
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
  keeps the stored ones instead of failing the refresh. Version 4 added chores, batteries, task
  categories, userentities, users and the userfield values of the simple objects.
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
- Android: `BundledSQLiteDriver`, file `grocy.db`. Web: `WebWorkerSQLiteDriver` with the worker
  `core/database/web-worker/worker.js` (npm package `grocy-sqlite-worker` on top of
  `@sqlite.org/sqlite-wasm`, androidx's message protocol). It stores the database in OPFS with
  the "opfs-sahpool" VFS, which needs no COOP/COEP headers (GitHub Pages); a second tab or a
  browser without OPFS gets an in-memory database that the app fills from the server again.
  Webpack bundles worker and `sqlite3.wasm`, `precache.json` lists them for offline use.
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
  `checkUnlockKey` and `scannerEngine` (stored by enum name, unknown values fall back to ZXing).
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

### Updates

- `AppUpdater` (`core:data` `update`) per platform, created by
  `PlatformDependencies.createAppUpdater`; `UpdateRepository` in `AppGraph` runs the checks.
  Results are `UpdateCheck` (`UpToDate`, `Available(versionName?)`, `Failed`).
- About has a "Check for updates" item below the version on every platform: it checks on tap and
  shows `UpdateCheckDialog` (`core:ui`): the update dialog, or a `TextDialog` for up to date or a
  failed check. Only the web app also checks by itself (`checksAutomatically`): at start, when it
  returns to the foreground and every 4 hours; `GrocyApp` shows `UpdateAvailableDialog` for a
  found update. "Later" (or dismissing) postpones that version for the session. Never reload or
  install without the user's tap.
- Android outside Google Play (FOSS, nightly, debug, downloaded APKs): `GitHubAppUpdater` asks
  the public GitHub API (`GitHubReleases` in `core:network`, no credentials, release pages must
  stay below `https://github.com/patzly/grocy-kmp/`). Regular builds compare their version with
  the latest release (`isNewerVersion`), nightly builds (`-nightly+<short SHA>`) their commit
  with the `nightly` release. Installing opens the release page; the app never installs APKs
  itself (no `REQUEST_INSTALL_PACKAGES`).
- Play app installed by the Play Store: `PlayAppUpdater` uses the in-app update API (immediate
  flow from the resumed activity, `ResumedActivityTracker`), Play gives no version name. Other
  installs of the Play app fall back to the GitHub updater.
- Web: `ServiceWorkerAppUpdater` with `apps/web/src/wasmJsMain/resources/sw.js`. Every deployment
  has a build id (`-Pgrocy.webBuildId`, the commit SHA in `nightly.yml`; version suffix through
  `-Pgrocy.webVersionSuffix`), compiled into `BuildInfo.kt` and written with the file list into
  `precache.json` by `wasmJsBrowserDistribution`. The app registers `sw.js?build=<id>`; that
  worker caches exactly the files of its build (refusing to install if the server has another
  one), serves them cache first (also offline) and deletes older caches when it activates. A
  check fetches `precache.json` with `no-store`; for a new build it registers that build's worker,
  which downloads the version in the background, and offers the update once installed. Updating
  sends `skipWaiting` and reloads on `controllerchange`. Requests to other origins (Grocy
  servers) are never cached. Builds without id (`dev`, also the dev server) register no worker
  and do not check automatically, so local builds always run fresh code.

### Barcode scanner (`core:scanner`)

- One component for the whole app: `BarcodeScanner(onBarcodeScanned, formats, shape, paused)`.
  Pass the segmented item shape (`SegmentedListDefaults.shape(index, count)`) and place it as an
  item of a group, never directly on the background. It is a landscape rectangle (16:10, at most
  240.dp high, so wide windows such as desktop browsers get a strip) and
  draws its own `surface` background and permission/error states inside. The torch button sits
  in the bottom end corner with 12.dp margin.
- Engines implement `BarcodeDecoder` (`decode(CameraFrame, formats): List<Barcode>`):
  ZXing via `BarcodeDecoderFactory.Zxing` (zxing-core on Android, zxing-wasm in browsers) and
  ML Kit via `MlKitBarcodeDecoderFactory` (`core:scanner-mlkit`). `CameraFrame` is an
  expect class (ImageProxy / ImageData). All engines share `ScanResultFilter` (linear codes need
  two identical reads in a row, a code in view is reported once) and the `BarcodeFormat` set, so
  they behave the same. ZXing on Android decodes the upright image and a 90 degree turn for
  linear codes, like ML Kit's orientation-independent detection.
- Engine selection: `ScannerRepository.activeEngine` = setting if the platform has it and (for
  ML Kit) `isUnlocked`, else ZXing. ZXing is always the default; unlocking (key installed, or the
  key check turned off by 10 long presses on the key item in About) never switches the engine,
  the user selects ML Kit themselves. `GrocyApp` provides the matching factory through
  `LocalBarcodeDecoderFactory`, so screens only call `BarcodeScanner`. There is no settings
  screen yet to change the engine.
- Permission: asked only when the user taps the scanner's button (`rememberCameraAccess`);
  granted cameras start right away. Android detects "denied for good" and opens the app settings;
  browsers get a hint for the site settings. The camera stops when the scanner leaves the
  composition or the lifecycle stops.
- Android: CameraX `CameraXViewfinder` in `ImplementationMode.EMBEDDED` (TextureView, so the
  rounded clip works), `ImageAnalysis` 1280x720 with KEEP_ONLY_LATEST, decoding on
  `Dispatchers.Default`, torch button if the camera has a flash. The CAMERA permission and an
  optional camera feature are declared in the module manifest.
- Web: `HtmlElementView` with a `<video>` from `getUserMedia` (needs HTTPS); the element lies
  above the Compose canvas, so its corners are rounded with CSS from the shape
  (`toCssBorderRadius`) and Compose overlays cannot cover it. Frames are decoded every 120 ms.
  `zxing_reader.wasm` is copied from node_modules into the web app (`copyZxingWasm` in
  `apps/web`) and loaded from there instead of zxing-wasm's default CDN.

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
- Font is Google Sans Flex from Compose Resources. On web fonts load async;
  `rememberResourcesReady()` (also every string and icon: browsers load them asynchronously, so a new screen would fill in its texts while sliding in) gates `onContentReady`, which removes the Android splash / the HTML
  `#loading` element.
- Reuse components from `core:design` (`SegmentedListScreen`, `segmentedGroup`,
  `SegmentedContainer`, `AppBarIconButton`, `InsetLazyColumn` + `insetItem` with max width
  640.dp, `RoundedTextField`, `ConnectedButtonGroup`, `NavigateBackButton`, `HapticTooltipBox`,
  `TextDialog`, `FormattedText`, `FloatingToolbarWithFab` + `FloatingToolbarIconButton` for the
  floatingToolbar slot of `SegmentedListScreen`, `SegmentedSwitchItem`,
  `SupportingLines` for every multi-line supporting content in lazy lists (Material sizes items
  by the slot's baselines, a reused column kept those of its former line count, so items stayed
  too high), `SearchableDropdownField` for every choice of objects, a combobox like Grocy's pickers: the
  text is the value (a text naming an option chooses it, an empty one chooses nothing, other
  text shows "Choose an entry from the list" once no suggestion is open and is reported through
  `onUnmatchedChange`, so forms refuse to save); the menu only suggests (fuzzy, no check marks,
  optional fields offer "None"), opens through its arrow or while typing and closes on a choice
  or once the text names an option completely;
  a tap on the field or its trailing action (scan) only focuses; its menu is one expressive menu
  group in a `DropdownMenuPopup`, not `ExposedDropdownMenu`, whose old menu padding sits outside
  the scrolling and cuts the items off), units of text fields go in the
  `suffix` slot of `RoundedTextField`. `SegmentedSwitchItem` is Material's checkable segmented item: while on it takes the selected
  shape and color (`secondaryContainer`) of the segmented defaults. Clicks trigger `haptic.confirm()`. `HapticTooltipBox`
  puts its modifier on an outer `Box`, because Material's `TooltipBox` loses parent data such as `align`.
  Tooltips show on long press, mouse hover and keyboard focus, never after a plain tap or click:
  Compose on the web focuses a clicked button, so `TooltipPressTracker` tells a pointer focus
  (`TooltipTrigger.PointerFocus`) from keyboard focus and long press.
- `LocalPointerKind` (`core:design`, provided and tracked by `GrocyApp` with
  `trackPointerKind`): whether the user works with a mouse or trackpad right now. It starts with
  the device (`(pointer: fine)` in browsers, `FEATURE_PC` on Android) and follows the pointer of
  the last press or wheel turn; hover moves do not count, browsers emulate them after a tap.
- Web pages show the hand cursor over clickable things, Compose does not: every button, item,
  chip, toggle, slider and link applies `Modifier.clickCursor(enabled)` (`core:design`, a no-op
  on Android, whose convention, also with a mouse on ChromeOS, keeps the arrow on buttons).
  New interactive components must apply it too.
- `ScrollableAlertDialog` closes on a tap beside it through `detectTapGestures`, not
  `clickable`, which would merge the dialog's pane title into one node (the web throws).
- "Recommend" (`rememberRecommendAction`) opens the system share sheet; where there is none
  (desktop browsers on Linux, `isTextSharingAvailable`) it shows `AppDialog.RECOMMEND` with the
  text and "Copy to clipboard" (`plainTextClipEntry` of `core:ui`).
- `GrocyApp` draws a `surfaceContainer` background below all screens, so fading transitions never
  show the window or page behind them. On the web `SystemBarAppearance` also sets the page
  background to `surfaceContainer`.
- System bars of the installed web app (Chrome on Android): a page cannot draw behind them, not
  even with `viewport-fit=cover`. Chrome paints the status bar in the page's `theme-color`, so
  `SegmentedListScreen` reports its app bar color (`StatusBarColorEffect`, the same
  `lerp(surfaceContainer, surfaceContainerHighest, FastOutLinearIn(collapsedFraction))` the large
  app bar uses) and `StatusBarColorRequests` writes the newest screen's color into the meta tags
  (`surfaceContainer` without app bar). The gesture bar takes the manifest's `theme_color`, which
  has no dark variant; `manifest.webmanifest` therefore has none, so the bar follows the
  system's light or dark mode. Never add `theme_color` back.
- Keyboard: `WindowInsets.keyboard` (`core:design`) is `WindowInsets.ime` on Android (edge-to-edge
  plus `adjustResize`, animated frame by frame). On the web `observeBrowserKeyboard` (called in
  `main()`) measures it: Chromium's VirtualKeyboard API with `overlaysContent = true`, otherwise
  the visual viewport; the app animates the overlap with Android's IME curve (285 ms,
  cubic-bezier(0.2, 0, 0, 1)). `SegmentedListScreen` pads the list by the keyboard and puts only
  the uncovered part of the navigation bar below the content (`ScreenContentPadding`, read
  during layout, so animations relayout without recomposing).
- Web page (`index.html`): `viewport-fit=cover` and `interactive-widget=resizes-content`; the
  page fills the screen (behind the gesture bar of an installed app) while `#composeApp` is
  fixed inside `env(safe-area-inset-*)`, because Compose on the web knows no window insets.
  Layout and colors of screens and dialogs: see "Screen and dialog design" above.
- `UriHandler.tryOpenUri` instead of `openUri` when the target app might be missing.
- Platform differences use `expect`/`actual` with files named `Xxx.android.kt` / `Xxx.wasmJs.kt`.
  Wasm JS interop uses `@JsFun` with `@OptIn(ExperimentalWasmJsInterop::class)`.

### Resources and strings

- All UI strings, icons (`ic_rounded_*`), fonts and text files live in `core:resources`
  (`composeResources/`), accessed via `com.patrickzedler.grocy.core.resources.Res`. Do not add
  resources to feature modules.
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
    category, a userentity, users, a chore userfield), answers POST of the chores' next
    assignments and
    `respond(..., method = HttpMethod.Post)` per method, accepts PUT and DELETE with 204 and lists
    them with `requestsTo(path, method)`.
- Room needs a real SQLite: tests with a database live in `androidHostTest` (Node has no web
  worker) and use `createInMemoryGrocyDatabase(queryContext = testDispatcher)`; modules running
  them apply `grocy.sqlite.host.tests`, which loads the desktop SQLite library for the JVM.
- `snapshotFlow` over a `TextFieldState` in a ViewModel needs `Snapshot.sendApplyNotifications()`
  in tests, which the frame clock does in the app.
- Dates, durations, currencies and percents of the app language are formatted by the platform
  in `core:model` `text` (`formatDate`, `formatRelativeTime`, `formatDuration`,
  `formatCurrency`, `formatPercent`: ICU and `java.text` on Android, `Intl` in browsers); the
  rounding (`timeSpanBetween`, `durationOf`, moment.js's thresholds like Grocy) and `epochDayOf`
  are common code. Android's ICU is stubbed in JVM tests, so tests never call the relative and
  duration formatters. Prices use the server's `CURRENCY` (`ServerConfig.currency`) and the
  user setting `stock_decimal_places_prices_display`.
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
  Play, web app deployed to GitHub Pages (build id = commit SHA, see "Updates"), a reproducibility check that builds the unsigned FOSS
  release twice and compares bytes, then replaces the `nightly` GitHub release.
- `release.yml` (tag `v*`, must equal `v` + `grocy.versionName`): runs `allTests`, then signed
  FOSS APK + SHA256SUMS on the GitHub release, Play and Wear AABs as workflow artifacts.
- Signing reads `RELEASE_*` / `NIGHTLY_*` env vars (`KEYSTORE_PATH`, `KEYSTORE_PASSWORD`,
  `KEY_ALIAS`, `KEY_PASSWORD`); without them builds are unsigned, never debug-signed.
- Reproducible FOSS builds matter (F-Droid/IzzyOnDroid): `vcsInfo` and the dependency info block
  are disabled. Do not add build inputs that vary per machine, path or time.
- Actions are pinned by commit SHA; Dependabot updates them weekly.
