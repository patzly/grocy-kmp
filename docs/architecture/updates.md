# Updates

How every platform checks for and installs updates. Moved from CLAUDE.md.

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
