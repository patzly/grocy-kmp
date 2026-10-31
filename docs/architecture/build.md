# Build details

Build logic, convention plugins and adding a feature. Moved from CLAUDE.md.

## Build

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

## Convention plugins (`build-logic/convention`)

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

## Adding a feature

To add a feature: create `feature/<name>/api` + `impl`, add routes and a `SerializersModule`,
add a `Dependencies` interface if needed, wire it in `AppGraph`, register the serializers in
`routeSerializers` and the entries in `GrocyNavigation` (`apps/shared/.../GrocyApp.kt`), add
both modules to `apps/shared/build.gradle.kts`, and add the new routes to `allRoutes` in
`RouteSerializersTest` (`apps/shared/src/commonTest`).
