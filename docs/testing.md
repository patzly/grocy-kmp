# Testing details

Test setup, fakes and platform caveats. Moved from CLAUDE.md.

- JVM tests of Android code run against stubbed Android classes: avoid Android graphics types
  (e.g. outlines with a `Path`) and test plain data instead (`RoundRect.toCssBorderRadius`).
- Every module gets `kotlin-test`, `kotlinx-coroutines-test` and Turbine via the convention
  plugin; `ExperimentalCoroutinesApi` is opted in for test source sets.
- `core:testing` provides:
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
- Fuzzy search (`FuzzySearch`) and number formatting (`formatDecimal`, `parseDecimal`,
  `platformDecimalSeparator`) live in `core:model`, so they are tested on JVM and Wasm.
- Network code is tested against `MockEngine` via `createGrocyHttpClient(engine)`, which uses the
  same configuration as production.
- Protocols with sockets are tested through a small message interface, e.g. the Home Assistant
  handshake with scripted `WebSocketMessages` (`IngressSessionHandshakeTest`).
- Decoders are tested with real codes: `ZxingBarcodeDecoderTest` renders QR and EAN codes with
  ZXing's writer (white quiet zone around them, linear codes need it) and decodes them, also
  turned by 90 degrees. Camera, permissions and ML Kit itself need a device and are not tested.
